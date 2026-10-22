#Requires -Version 7.4
[CmdletBinding()]
param(
    [string]$GatewayUrl = 'http://127.0.0.1:8080',
    [string]$MockToken = 'local-dev-token',
    [string]$ElasticsearchHome = 'D:\Elasticsearch\elasticsearch-8.15.3',
    [string]$RuntimeDirectory = 'D:\Elasticsearch\local-blog'
)

# Opt-in outage test: stops only the ES process recorded by start-local-elasticsearch.ps1.
# Run against a dedicated local development stack, without concurrent integration suites.
$ErrorActionPreference = 'Stop'
if (([Uri]$GatewayUrl).Host -notin @('127.0.0.1', 'localhost')) { throw 'This test only supports a local Gateway.' }
$prefix = 'SEARCHOUTAGE-' + [Guid]::NewGuid().ToString('N')
$owned = [Collections.Generic.List[string]]::new()
function Invoke-Blog([string]$Method, [string]$Path, $Body = $null, [switch]$Admin) {
    $headers = @{
        'X-Mock-Token' = $MockToken; 'X-Mock-User' = $(if ($Admin) { 'u-admin' } else { 'u-author' })
        'X-Mock-Roles' = $(if ($Admin) { 'ADMIN,REVIEWER,AUTHOR,READER' } else { 'AUTHOR,READER' })
        'X-Mock-Departments' = 'd-platform'; 'X-Mock-Teams' = 't-search'
    }
    $arguments = @{ Method = $Method; Uri = "$GatewayUrl$Path"; Headers = $headers; NoProxy = $true; TimeoutSec = 15 }
    if ($null -ne $Body) { $arguments.Body = $Body | ConvertTo-Json -Depth 8 -Compress; $arguments.ContentType = 'application/json' }
    Invoke-RestMethod @arguments
}
function Wait-For([scriptblock]$Condition, [string]$Message, [int]$Seconds = 75) {
    $deadline = [DateTime]::UtcNow.AddSeconds($Seconds)
    while ([DateTime]::UtcNow -lt $deadline) {
        try { if (& $Condition) { return } } catch { }
        Start-Sleep -Milliseconds 750
    }
    throw "Timed out: $Message"
}
function New-TestArticle([string]$Suffix) {
    $draft = Invoke-Blog POST '/api/articles/drafts' @{
        title = "$prefix $Suffix"; contentJson = (@{ type = 'markdown'; version = 1; source = 'Redis outage recovery' } | ConvertTo-Json -Compress)
        tagIds = @('redis'); categoryId = 'engineering'; clientDraftId = [Guid]::NewGuid().ToString(); autosave = $true
    }
    $owned.Add($draft.id)
    Invoke-Blog POST "/api/articles/$($draft.id)/submit-publish" @{ visibilityType = 'COMPANY'; targetOrgIds = @(); expectedRevision = $draft.revision }
}
function Get-OwnedFailed {
    # Assign REST arrays before piping: Invoke-RestMethod may emit a whole array as one pipeline object.
    $tasks = Invoke-Blog GET '/api/admin/search/tasks?status=FAILED' -Admin
    foreach ($task in $tasks) { if ($task.articleId -in $owned) { $task } }
}
$searchPath = '/api/search/articles?q=' + [Uri]::EscapeDataString($prefix)
$completed = $false
try {
    & (Join-Path $PSScriptRoot 'start-local-elasticsearch.ps1') -ElasticsearchHome $ElasticsearchHome -RuntimeDirectory $RuntimeDirectory
    $first = New-TestArticle 'before-outage'
    Wait-For { (Invoke-Blog GET $searchPath).total -eq 1 } 'initial publication indexed'
    & (Join-Path $PSScriptRoot 'stop-local-elasticsearch.ps1') -RuntimeDirectory $RuntimeDirectory
    Wait-For { -not (Get-NetTCPConnection -State Listen -LocalPort 9200 -ErrorAction SilentlyContinue) } 'Elasticsearch stopped' 15
    $status = 0
    try { $null = Invoke-Blog GET $searchPath } catch { $status = [int]$_.Exception.Response.StatusCode }
    if ($status -ne 503) { throw "Expected search to fail closed with 503; received $status" }
    Write-Output 'PASS: Elasticsearch offline returns 503, without stale search results.'
    $null = Invoke-Blog POST "/api/articles/$($first.id)/withdraw"
    $null = Invoke-Blog DELETE "/api/articles/$($first.id)"
    $second = New-TestArticle 'published-offline'
    Wait-For {
        $failed = @(Get-OwnedFailed)
        $failed.Count -eq 3 -and @($failed | Where-Object retryCount -ne 5).Count -eq 0
    } 'publish, withdraw and delete reach FAILED after five attempts'
    $failed = @(Get-OwnedFailed)
    $types = @($failed.eventType | Sort-Object)
    if (($types -join ',') -ne 'ArticleDeleted,ArticlePublished,ArticleWithdrawn') { throw 'Unexpected failure task types.' }
    Write-Output 'PASS: publish, withdraw and delete each stop at five failures.'
    & (Join-Path $PSScriptRoot 'start-local-elasticsearch.ps1') -ElasticsearchHome $ElasticsearchHome -RuntimeDirectory $RuntimeDirectory
    if ((Invoke-Blog GET $searchPath).total -ne 0) { throw 'Withdrawn/deleted content leaked from the recovered old index.' }
    Write-Output 'PASS: recovered stale index remains unreadable before task retries.'
    foreach ($task in $failed) { $null = Invoke-Blog POST "/api/admin/search/tasks/$($task.id)/retry" -Admin }
    Wait-For { (Invoke-Blog GET $searchPath).total -eq 1 } 'retried publication becomes searchable'
    $result = Invoke-Blog GET $searchPath
    if ($result.items[0].articleId -ne $second.id) { throw 'Unexpected article returned after recovery.' }
    Wait-For {
        $document = Invoke-RestMethod "http://127.0.0.1:9200/articles-v1/_doc/$($first.id)" -NoProxy
        $document._source.status -eq 'DELETED' -and @($document._source.PSObject.Properties).Count -eq 3
    } 'deleted article becomes a content-free revision marker'
    Write-Output 'PASS: retries restore current published content and erase deleted content.'
    $completed = $true
} finally {
    & (Join-Path $PSScriptRoot 'start-local-elasticsearch.ps1') -ElasticsearchHome $ElasticsearchHome -RuntimeDirectory $RuntimeDirectory
    foreach ($id in $owned) {
        try { $null = Invoke-Blog DELETE "/api/articles/$id" } catch { Write-Warning "Cleanup failed for test article $id : $($_.Exception.Message)" }
    }
    foreach ($task in @(Get-OwnedFailed)) { $null = Invoke-Blog POST "/api/admin/search/tasks/$($task.id)/retry" -Admin }
    if ($owned.Count) { Wait-For { (Invoke-Blog GET $searchPath).total -eq 0 } 'test articles hidden after cleanup' }
    Write-Output "Test prefix: $prefix; articles: $($owned -join ','); completed: $completed"
}
