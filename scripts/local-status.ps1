$ErrorActionPreference = "Stop"

$BackendPort = 8080
$FrontendPort = 5173
$BackendReadyUrl = "http://localhost:$BackendPort/api/health"
$FrontendReadyUrl = "http://localhost:$FrontendPort/coffee"

function Test-ProjectRoot {
    param([string]$Path)
    return (
        -not [string]::IsNullOrWhiteSpace($Path) -and
        (Test-Path -LiteralPath (Join-Path $Path "backend\pom.xml")) -and
        (Test-Path -LiteralPath (Join-Path $Path "frontend\package.json"))
    )
}

function Resolve-ProjectRoot {
    $basePaths = @()
    if (-not [string]::IsNullOrWhiteSpace($PSScriptRoot)) {
        $basePaths += $PSScriptRoot
    }
    try {
        $basePaths += Split-Path -Parent ([System.Diagnostics.Process]::GetCurrentProcess().MainModule.FileName)
    } catch {
        # Ignore process path lookup failures.
    }
    $basePaths += (Get-Location).Path

    foreach ($basePath in ($basePaths | Where-Object { -not [string]::IsNullOrWhiteSpace($_) } | Select-Object -Unique)) {
        $candidatePaths = @($basePath, (Join-Path $basePath ".."))
        foreach ($candidatePath in $candidatePaths) {
            if (Test-ProjectRoot $candidatePath) {
                return (Resolve-Path -LiteralPath $candidatePath).Path
            }
        }
    }

    throw "Cannot locate Coffee Manager project root. Run this script from the project root, keep it in scripts, or put the generated exe in the project root or one folder below it."
}

$RootDir = Resolve-ProjectRoot
$LogDir = Join-Path $RootDir "logs"
$PidDir = Join-Path $LogDir "pids"
$BackendPidFile = Join-Path $PidDir "backend.pid"
$FrontendPidFile = Join-Path $PidDir "frontend.pid"

function Read-PidFile {
    param([string]$Path)
    if (-not (Test-Path -LiteralPath $Path)) {
        return $null
    }

    $raw = (Get-Content -LiteralPath $Path -ErrorAction SilentlyContinue | Select-Object -First 1)
    $pidValue = 0
    if ([int]::TryParse($raw, [ref]$pidValue)) {
        return $pidValue
    }
    return $null
}

function Get-PortOwners {
    param([int]$Port)
    try {
        return @(Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction Stop |
            Select-Object -ExpandProperty OwningProcess -Unique)
    } catch {
        return @()
    }
}

function Test-HttpOk {
    param([string]$Url)
    try {
        $response = Invoke-WebRequest -Uri $Url -UseBasicParsing -TimeoutSec 5 -ErrorAction Stop
        return ($response.StatusCode -ge 200 -and $response.StatusCode -lt 400)
    } catch {
        return $false
    }
}

function Get-BackendStatus {
    if ((Get-PortOwners $BackendPort).Count -eq 0) {
        return "not running"
    }

    try {
        $response = Invoke-RestMethod -Uri $BackendReadyUrl -TimeoutSec 5 -ErrorAction Stop
        if ($response.code -eq 0 -and $response.data.status -eq "ok" -and $response.data.database -eq "ok") {
            return "reachable (health ok, database ok)"
        }
        return "reachable but health payload is unexpected"
    } catch {
        return "port is listening, but /api/health is not healthy"
    }
}

function Get-FrontendStatus {
    if ((Get-PortOwners $FrontendPort).Count -eq 0) {
        return "not running"
    }

    if (Test-HttpOk $FrontendReadyUrl) {
        return "reachable"
    }
    return "port is listening, but /coffee is not reachable"
}

function Format-PidStatus {
    param([string]$PidPath)
    $pidValue = Read-PidFile $PidPath
    if ($null -eq $pidValue) {
        return "no local PID file"
    }

    $process = Get-Process -Id $pidValue -ErrorAction SilentlyContinue
    if ($null -eq $process) {
        return "stale PID file ($pidValue)"
    }

    return "PID $pidValue"
}

$backendStatus = Get-BackendStatus
$frontendStatus = Get-FrontendStatus
$backendPidStatus = Format-PidStatus $BackendPidFile
$frontendPidStatus = Format-PidStatus $FrontendPidFile
$backendOwners = Get-PortOwners $BackendPort
$frontendOwners = Get-PortOwners $FrontendPort

Write-Host "Coffee Manager local status"
Write-Host "Backend  : $backendStatus; $backendPidStatus; port $BackendPort owner(s): $(if ($backendOwners.Count -gt 0) { $backendOwners -join ', ' } else { 'none' })"
Write-Host "Frontend : $frontendStatus; $frontendPidStatus; port $FrontendPort owner(s): $(if ($frontendOwners.Count -gt 0) { $frontendOwners -join ', ' } else { 'none' })"
Write-Host "Logs     : $LogDir"

if ($backendStatus -eq "not running" -or $frontendStatus -eq "not running") {
    Write-Host "Next step: run .\scripts\local-start.ps1 from the project root."
}
