#Requires -Version 7.4
[CmdletBinding(SupportsShouldProcess)]
param([string]$RuntimeDirectory = 'D:\Elasticsearch\local-blog')
$ErrorActionPreference = 'Stop'
$manifest = Join-Path ([IO.Path]::GetFullPath($RuntimeDirectory)) 'process.json'
if (-not (Test-Path -LiteralPath $manifest)) { throw "Process manifest not found: $manifest" }
$record = Get-Content -LiteralPath $manifest -Raw | ConvertFrom-Json
$process = Get-Process -Id ([int]$record.pid) -ErrorAction SilentlyContinue
if (-not $process) { Write-Output 'Recorded Elasticsearch process is not running.'; return }
if ($process.StartTime.ToUniversalTime() -ne ([DateTime]$record.startedAt).ToUniversalTime()) {
    Write-Output 'The PID was reused; no process was stopped.'; return
}
$processInfo = Get-CimInstance Win32_Process -Filter "ProcessId = $($process.Id)"
$commandLine = $processInfo.CommandLine
# ES passes its configuration directory through the environment, not JVM arguments.
$expectedJava = Join-Path ([string]$record.root) 'jdk/bin/java.exe'
$expectedTemp = Join-Path ([IO.Path]::GetFullPath($RuntimeDirectory)) 'temp'
$tempArgument = [regex]::Escape("-Djava.io.tmpdir=$expectedTemp")
if ($process.ProcessName -ne 'java' -or $processInfo.ExecutablePath -ne [IO.Path]::GetFullPath($expectedJava) -or
    $commandLine -notmatch 'org\.elasticsearch\.bootstrap\.Elasticsearch' -or
    $commandLine -notmatch "(?i)(?:^|\s)`"?$tempArgument`"?(?:\s|$)") {
    throw 'The recorded process does not match the managed Elasticsearch instance; it was not stopped.'
}
if ($PSCmdlet.ShouldProcess("Elasticsearch (PID $($process.Id))", 'Stop local instance')) { Stop-Process -Id $process.Id }
