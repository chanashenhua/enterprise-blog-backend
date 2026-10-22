#Requires -Version 7.4
[CmdletBinding(SupportsShouldProcess)]
param(
    [string]$LogDirectory = (Join-Path $PSScriptRoot '../target/local-services'),
    [string[]]$ServiceNames = @()
)

$ErrorActionPreference = 'Stop'
$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$logRoot = [System.IO.Path]::GetFullPath($LogDirectory, $repoRoot)
$stateFile = Join-Path $logRoot 'processes.jsonl'
if (-not (Test-Path -LiteralPath $stateFile)) { throw "Process manifest not found: $stateFile" }
$records = @(Get-Content -LiteralPath $stateFile | ForEach-Object { $_ | ConvertFrom-Json } |
    Where-Object { -not $ServiceNames -or $_.name -in $ServiceNames })
[array]::Reverse($records)
foreach ($record in $records) {
    $process = Get-Process -Id ([int]$record.pid) -ErrorAction SilentlyContinue
    if (-not $process) { continue }
    # A PID may have been reused after a previous run. Verify both its start time and JAR.
    $startedAt = ([DateTime]$record.startedAt).ToUniversalTime()
    if ($process.StartTime.ToUniversalTime() -ne $startedAt) { continue }
    $expectedJar = Join-Path $repoRoot "$($record.name)/target/$($record.name)-0.1.0-SNAPSHOT.jar"
    $jarPath = [System.IO.Path]::GetFullPath([string]$record.jar)
    if (-not $jarPath.StartsWith($repoRoot + [System.IO.Path]::DirectorySeparatorChar, [StringComparison]::OrdinalIgnoreCase) -or
        $jarPath -ne [System.IO.Path]::GetFullPath($expectedJar)) { throw 'Unexpected JAR path in process manifest.' }
    $commandLine = (Get-CimInstance Win32_Process -Filter "ProcessId = $($process.Id)").CommandLine
    $escapedJar = [regex]::Escape($jarPath)
    if ($process.ProcessName -ne 'java' -or $commandLine -notmatch "(?i)(?:^|\s)-jar\s+(?:`"$escapedJar`"|$escapedJar)(?:\s|$)") {
        throw "PID $($process.Id) does not match the recorded service; it was not stopped."
    }
    if ($PSCmdlet.ShouldProcess("$($record.name) (PID $($process.Id))", 'Stop local Java service')) {
        Stop-Process -Id $process.Id
    }
}
