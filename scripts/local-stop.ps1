$ErrorActionPreference = "Stop"

$BackendPort = 8080
$FrontendPort = 5173

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

function Stop-ProcessTree {
    param([int]$ProcessId)

    if ($ProcessId -eq $PID) {
        throw "Refusing to stop the current PowerShell process."
    }

    $children = @(Get-CimInstance Win32_Process -Filter "ParentProcessId = $ProcessId" -ErrorAction SilentlyContinue)
    foreach ($child in $children) {
        Stop-ProcessTree -ProcessId ([int]$child.ProcessId)
    }

    $process = Get-Process -Id $ProcessId -ErrorAction SilentlyContinue
    if ($null -ne $process) {
        Stop-Process -Id $ProcessId -Force -ErrorAction SilentlyContinue
    }
}

function Wait-PortClosed {
    param(
        [int]$Port,
        [int]$TimeoutSeconds
    )

    $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
    while ((Get-Date) -lt $deadline) {
        if ((Get-PortOwners $Port).Count -eq 0) {
            return $true
        }
        Start-Sleep -Seconds 1
    }
    return $false
}

function Stop-ManagedService {
    param(
        [string]$Name,
        [string]$PidPath,
        [int]$Port
    )

    $pidValue = Read-PidFile $PidPath
    if ($null -eq $pidValue) {
        Write-Host "${Name}: no PID file found. To avoid accidental kills, no process was stopped."
        $owners = Get-PortOwners $Port
        if ($owners.Count -gt 0) {
            Write-Host "${Name}: port $Port is still occupied by PID(s): $($owners -join ', ')."
        }
        return
    }

    $process = Get-Process -Id $pidValue -ErrorAction SilentlyContinue
    if ($null -eq $process) {
        Write-Host "${Name}: PID file was stale. Removed: $PidPath"
        Remove-Item -LiteralPath $PidPath -Force -ErrorAction SilentlyContinue
        return
    }

    Write-Host "${Name}: stopping PID $pidValue and its child processes..."
    Stop-ProcessTree -ProcessId $pidValue
    Remove-Item -LiteralPath $PidPath -Force -ErrorAction SilentlyContinue

    if (Wait-PortClosed $Port 20) {
        Write-Host "${Name}: stopped. Port $Port is free."
    } else {
        $owners = Get-PortOwners $Port
        Write-Host "${Name}: stop command completed, but port $Port is still occupied by PID(s): $($owners -join ', ')."
    }
}

Stop-ManagedService "Frontend" $FrontendPidFile $FrontendPort
Stop-ManagedService "Backend" $BackendPidFile $BackendPort

Write-Host "Local stop finished."
