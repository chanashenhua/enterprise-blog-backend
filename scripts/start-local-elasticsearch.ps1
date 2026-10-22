#Requires -Version 7.4
[CmdletBinding()]
param(
    [string]$ElasticsearchHome = 'D:\Elasticsearch\elasticsearch-8.15.3',
    [string]$RuntimeDirectory = 'D:\Elasticsearch\local-blog',
    [ValidateRange(256, 4096)][int]$HeapMb = 512
)

$ErrorActionPreference = 'Stop'
$installRoot = [IO.Path]::GetFullPath($ElasticsearchHome)
$runtimeRoot = [IO.Path]::GetFullPath($RuntimeDirectory)
foreach ($path in @($installRoot, $runtimeRoot)) {
    if (-not $path.StartsWith('D:\', [StringComparison]::OrdinalIgnoreCase)) {
        throw 'Elasticsearch installation, data, logs and temporary files must stay on D:.'
    }
}
$launcher = Join-Path $installRoot 'bin/elasticsearch.bat'
if (-not (Test-Path -LiteralPath $launcher)) { throw "Extract the official Windows ZIP to $installRoot first." }
$listener = Get-NetTCPConnection -State Listen -LocalPort 9200 -ErrorAction SilentlyContinue
if ($listener) {
    $existing = Invoke-RestMethod 'http://127.0.0.1:9200' -NoProxy -TimeoutSec 5
    if ($existing.cluster_name -ne 'enterprise-blog-local') { throw 'Port 9200 belongs to a different instance.' }
    Write-Output 'The enterprise-blog-local Elasticsearch instance is already running on 127.0.0.1:9200.'
    return
}
$configRoot = Join-Path $runtimeRoot 'config'
foreach ($name in @('config', 'data', 'logs', 'temp')) {
    $null = New-Item -ItemType Directory -Path (Join-Path $runtimeRoot $name) -Force
}
$configFile = Join-Path $configRoot 'elasticsearch.yml'
$marker = '# Managed by enterprise-blog start-local-elasticsearch.ps1'
if ((Test-Path -LiteralPath $configFile) -and (Get-Content -LiteralPath $configFile -TotalCount 1) -ne $marker) {
    throw "An unmanaged configuration exists at $configFile; it was not changed."
}
foreach ($name in @('jvm.options', 'log4j2.properties')) {
    $destination = Join-Path $configRoot $name
    if (-not (Test-Path -LiteralPath $destination)) { Copy-Item -LiteralPath (Join-Path $installRoot "config/$name") -Destination $destination }
}
$dataPath = (Join-Path $runtimeRoot 'data').Replace('\', '/')
$logsPath = (Join-Path $runtimeRoot 'logs').Replace('\', '/')
@"
$marker
cluster.name: enterprise-blog-local
node.name: local-windows
network.host: 127.0.0.1
http.host: 127.0.0.1
http.port: 9200
discovery.type: single-node
xpack.security.enabled: false
xpack.security.autoconfiguration.enabled: false
xpack.ml.enabled: false
path.data: '$dataPath'
path.logs: '$logsPath'
"@ | Set-Content -LiteralPath $configFile -Encoding utf8
$tempRoot = Join-Path $runtimeRoot 'temp'
$environment = @{ ES_PATH_CONF = $configRoot; ES_JAVA_OPTS = "-Xms$($HeapMb)m -Xmx$($HeapMb)m"; ES_TMPDIR = $tempRoot; TEMP = $tempRoot; TMP = $tempRoot }
$process = Start-Process -FilePath $launcher -WorkingDirectory $runtimeRoot -Environment $environment -WindowStyle Hidden -PassThru `
    -RedirectStandardOutput (Join-Path $runtimeRoot 'elasticsearch.out.log') -RedirectStandardError (Join-Path $runtimeRoot 'elasticsearch.err.log')
$deadline = [DateTime]::UtcNow.AddSeconds(90)
while ([DateTime]::UtcNow -lt $deadline) {
    try {
        $health = Invoke-RestMethod 'http://127.0.0.1:9200/_cluster/health' -NoProxy -TimeoutSec 2
        if ($health.status -in @('green', 'yellow')) {
            $connection = Get-NetTCPConnection -State Listen -LocalPort 9200 | Select-Object -First 1
            $jvm = Get-Process -Id $connection.OwningProcess
            @{ pid = $jvm.Id; launcherPid = $process.Id; root = $installRoot; config = $configRoot; startedAt = $jvm.StartTime.ToUniversalTime().ToString('o') } |
                ConvertTo-Json | Set-Content -LiteralPath (Join-Path $runtimeRoot 'process.json')
            Write-Output "Elasticsearch: $($health.status), http://127.0.0.1:9200 (PID $($jvm.Id)); data and logs: $runtimeRoot"
            return
        }
    } catch { }
    $process.Refresh()
    if ($process.HasExited) { break }
    Start-Sleep -Milliseconds 500
}
throw "Elasticsearch did not become healthy; inspect $runtimeRoot/elasticsearch.err.log and logs."
