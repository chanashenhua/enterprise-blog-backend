#Requires -Version 7.4
[CmdletBinding()]
param(
    [string]$JavaHome = $env:JAVA_HOME,
    [ValidateSet('all', 'foundation', 'business', 'gateway')]
    [string]$Group = 'all',
    [string[]]$ServiceNames = @(),
    [switch]$IncludeSearch,
    [string]$LogDirectory = (Join-Path $PSScriptRoot '../target/local-services')
)

$ErrorActionPreference = 'Stop'
$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
if (-not $JavaHome) { throw 'Set JAVA_HOME to a JDK 17 installation.' }
$java = Join-Path $JavaHome 'bin/java.exe'
if (-not (Test-Path -LiteralPath $java)) { throw "Java executable not found: $java" }
$services = @(
    @{ Name = 'eureka-server'; Port = 8761; Group = 'foundation'; Profile = 'local' },
    @{ Name = 'config-server'; Port = 8888; Group = 'foundation'; Profile = 'native,local' },
    @{ Name = 'org-service'; Port = 8082; Group = 'business'; Profile = 'local' },
    @{ Name = 'permission-service'; Port = 8083; Group = 'business'; Profile = 'local' },
    @{ Name = 'audit-service'; Port = 8092; Group = 'business'; Profile = 'local' },
    @{ Name = 'tag-service'; Port = 8085; Group = 'business'; Profile = 'local' },
    @{ Name = 'review-service'; Port = 8086; Group = 'business'; Profile = 'local' },
    @{ Name = 'comment-service'; Port = 8089; Group = 'business'; Profile = 'local' },
    @{ Name = 'stats-service'; Port = 8090; Group = 'business'; Profile = 'local' },
    @{ Name = 'notification-service'; Port = 8091; Group = 'business'; Profile = 'local' },
    @{ Name = 'search-service'; Port = 8088; Group = 'business'; Profile = 'local' },
    @{ Name = 'article-service'; Port = 8084; Group = 'business'; Profile = 'local' },
    @{ Name = 'gateway-service'; Port = 8080; Group = 'gateway'; Profile = 'dev,local' }
) | Where-Object { ($Group -eq 'all' -or $_.Group -eq $Group) -and (-not $ServiceNames -or $_.Name -in $ServiceNames) -and
    ($_.Name -ne 'search-service' -or $IncludeSearch -or 'search-service' -in $ServiceNames) }
foreach ($name in $ServiceNames) {
    if ($name -notin $services.Name) { throw "Unknown service or service outside selected group: $name" }
}
$databaseServices = @('org-service', 'audit-service', 'tag-service', 'review-service', 'comment-service', 'stats-service', 'notification-service', 'article-service')
if (($services | Where-Object { $_.Name -in $databaseServices }) -and -not $env:POSTGRES_PASSWORD) {
    throw 'Set POSTGRES_PASSWORD in this terminal before starting database-backed services.'
}

# Check all selected services before launching anything; never replace an existing process.
foreach ($service in $services) {
    $service.Jar = Join-Path $repoRoot "$($service.Name)/target/$($service.Name)-0.1.0-SNAPSHOT.jar"
    if (-not (Test-Path -LiteralPath $service.Jar)) {
        throw "Build the executable JAR first: $($service.Jar)"
    }
    $listener = [System.Net.Sockets.TcpListener]::new([System.Net.IPAddress]::Loopback, $service.Port)
    try { $listener.Start() }
    catch { throw "Port $($service.Port) is already in use; stop or reuse the existing service first." }
    finally { $listener.Stop() }
}
$logRoot = [System.IO.Path]::GetFullPath($LogDirectory, $repoRoot)
$null = New-Item -ItemType Directory -Path $logRoot -Force
$stateFile = Join-Path $logRoot 'processes.jsonl'

foreach ($service in $services) {
    $environment = @{
        SPRING_APPLICATION_NAME = $service.Name
        SPRING_PROFILES_ACTIVE = $service.Profile
        SERVER_ADDRESS = '127.0.0.1'
        SERVER_PORT = "$($service.Port)"
        EUREKA_INSTANCE_PREFER_IP_ADDRESS = 'true'
        EUREKA_INSTANCE_IP_ADDRESS = '127.0.0.1'
    }
    $arguments = @('-Xms64m', '-Xmx256m', '-jar', "`"$($service.Jar)`"",
        '--eureka.client.service-url.defaultZone=http://localhost:8761/eureka/')
    if ($service.Name -eq 'eureka-server') {
        $environment.EUREKA_CLIENT_REGISTER_WITH_EUREKA = 'false'
        $environment.EUREKA_CLIENT_FETCH_REGISTRY = 'false'
    } elseif ($service.Name -eq 'config-server') {
        $environment.SPRING_CLOUD_CONFIG_SERVER_NATIVE_SEARCH_LOCATIONS = ([System.Uri](Join-Path $repoRoot 'config-repo/')).AbsoluteUri
    } else {
        $environment.SPRING_CONFIG_IMPORT = 'configserver:http://localhost:8888'
        $environment.SPRING_CLOUD_CONFIG_FAIL_FAST = 'true'
    }
    if ($service.Name -eq 'gateway-service') {
        $environment.GATEWAY_DEV_MOCK_TOKEN = if ($env:GATEWAY_DEV_MOCK_TOKEN) { $env:GATEWAY_DEV_MOCK_TOKEN } else { 'local-dev-token' }
    }
    if ($service.Name -eq 'article-service' -and $IncludeSearch) { $environment.BLOG_OUTBOX_SEARCH_ENABLED = 'true' }
    $process = Start-Process -FilePath $java -ArgumentList $arguments -WorkingDirectory $repoRoot `
        -Environment $environment -WindowStyle Hidden -PassThru `
        -RedirectStandardOutput (Join-Path $logRoot "$($service.Name).out.log") `
        -RedirectStandardError (Join-Path $logRoot "$($service.Name).err.log")
    @{ name = $service.Name; pid = $process.Id; port = $service.Port; jar = $service.Jar; startedAt = $process.StartTime.ToUniversalTime().ToString('o') } |
        ConvertTo-Json -Compress | Add-Content -LiteralPath $stateFile
    Write-Output "Starting $($service.Name) on 127.0.0.1:$($service.Port) (PID $($process.Id))"
    $deadline = [DateTime]::UtcNow.AddSeconds(90)
    $healthy = $false
    while ([DateTime]::UtcNow -lt $deadline -and -not $process.HasExited) {
        try {
            $health = Invoke-RestMethod -Uri "http://127.0.0.1:$($service.Port)/actuator/health" -TimeoutSec 2 -NoProxy
            if ($health.status -eq 'UP') { $healthy = $true; break }
        } catch { }
        Start-Sleep -Milliseconds 500
        $process.Refresh()
    }
    if (-not $healthy) { throw "$($service.Name) did not become healthy; inspect logs in $logRoot" }
    Write-Output "$($service.Name): UP"
}
Write-Output "Process IDs and logs: $logRoot"
