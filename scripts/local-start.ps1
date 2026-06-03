param(
    [switch]$NoBrowser
)

$ErrorActionPreference = "Stop"

$BackendPort = 8080
$FrontendPort = 5173
$BackendReadyUrl = "http://localhost:$BackendPort/api/health"
$FrontendReadyUrl = "http://localhost:$FrontendPort/coffee"
$FrontendOpenUrl = "http://localhost:$FrontendPort/coffee"

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
$BackendDir = Join-Path $RootDir "backend"
$FrontendDir = Join-Path $RootDir "frontend"
$UploadsDir = Join-Path $RootDir "uploads"
$LogDir = Join-Path $RootDir "logs"
$PidDir = Join-Path $LogDir "pids"
$BackendLog = Join-Path $LogDir "backend.log"
$FrontendLog = Join-Path $LogDir "frontend.log"
$BackendPidFile = Join-Path $PidDir "backend.pid"
$FrontendPidFile = Join-Path $PidDir "frontend.pid"

function Ensure-Directory {
    param([string]$Path)
    if (-not (Test-Path -LiteralPath $Path)) {
        New-Item -ItemType Directory -Path $Path -Force | Out-Null
    }
}

function Get-CommandPath {
    param([string[]]$Names)
    foreach ($name in $Names) {
        $command = Get-Command $name -ErrorAction SilentlyContinue | Select-Object -First 1
        if ($null -ne $command) {
            return $command.Source
        }
    }
    return $null
}

function Quote-Cmd {
    param([string]$Value)
    return '"' + ($Value -replace '"', '""') + '"'
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

function Test-BackendReady {
    try {
        $response = Invoke-RestMethod -Uri $BackendReadyUrl -TimeoutSec 5 -ErrorAction Stop
        return (
            $response.code -eq 0 -and
            $response.data.status -eq "ok" -and
            $response.data.database -eq "ok"
        )
    } catch {
        return $false
    }
}

function Wait-ForService {
    param(
        [string]$Name,
        [scriptblock]$ReadyCheck,
        [int]$TimeoutSeconds,
        [string]$LogPath
    )

    $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
    while ((Get-Date) -lt $deadline) {
        if (& $ReadyCheck) {
            Write-Host "$Name is ready."
            return $true
        }
        Start-Sleep -Seconds 2
    }

    Write-Host "$Name did not become ready within $TimeoutSeconds seconds. Check log: $LogPath"
    return $false
}

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

function Clear-StalePidFile {
    param([string]$Path)
    $pidValue = Read-PidFile $Path
    if ($null -eq $pidValue) {
        if (Test-Path -LiteralPath $Path) {
            Remove-Item -LiteralPath $Path -Force
        }
        return
    }

    $process = Get-Process -Id $pidValue -ErrorAction SilentlyContinue
    if ($null -eq $process) {
        Remove-Item -LiteralPath $Path -Force
    }
}

function Start-ManagedProcess {
    param(
        [string]$Name,
        [string]$WorkingDirectory,
        [string]$Executable,
        [string[]]$Arguments,
        [string]$LogPath,
        [string]$PidPath
    )

    Add-Content -LiteralPath $LogPath -Value ""
    Add-Content -LiteralPath $LogPath -Value "==== $Name start $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss') ===="

    $quotedExecutable = Quote-Cmd $Executable
    $quotedWorkingDirectory = Quote-Cmd $WorkingDirectory
    $quotedLogPath = Quote-Cmd $LogPath
    $argumentText = ($Arguments | ForEach-Object { Quote-Cmd $_ }) -join " "
    $command = "cd /d $quotedWorkingDirectory && $quotedExecutable $argumentText >> $quotedLogPath 2>&1"

    $process = Start-Process -FilePath "cmd.exe" `
        -ArgumentList @("/d", "/s", "/c", $command) `
        -WindowStyle Hidden `
        -PassThru

    Set-Content -LiteralPath $PidPath -Value $process.Id
    Write-Host "$Name start command launched. PID: $($process.Id). Log: $LogPath"
    return $process
}

function Assert-PortFreeOrReusable {
    param(
        [string]$Name,
        [int]$Port,
        [scriptblock]$ReadyCheck
    )

    if (& $ReadyCheck) {
        Write-Host "$Name already appears to be running on port $Port. Reusing it and not starting another instance."
        return $true
    }

    $owners = Get-PortOwners $Port
    if ($owners.Count -gt 0) {
        $ownerText = ($owners -join ", ")
        throw "$Name port $Port is occupied, but the expected service is not ready. Owning PID(s): $ownerText. Stop that process or check its logs first."
    }

    return $false
}

Ensure-Directory $LogDir
Ensure-Directory $PidDir
Ensure-Directory $UploadsDir

Clear-StalePidFile $BackendPidFile
Clear-StalePidFile $FrontendPidFile

if ([string]::IsNullOrWhiteSpace($env:FILE_UPLOAD_PATH)) {
    $env:FILE_UPLOAD_PATH = $UploadsDir
}

$backendWasReady = Assert-PortFreeOrReusable "Backend" $BackendPort ${function:Test-BackendReady}
$frontendWasReady = Assert-PortFreeOrReusable "Frontend" $FrontendPort { Test-HttpOk $FrontendReadyUrl }

if (-not $backendWasReady) {
    $mvnPath = Get-CommandPath @("mvn.cmd", "mvn")
    if ($null -eq $mvnPath) {
        throw "Maven command not found. Install Maven and make sure mvn is available in PATH."
    }

    Start-ManagedProcess "Backend" $BackendDir $mvnPath @("spring-boot:run") $BackendLog $BackendPidFile | Out-Null
}

if (-not (Wait-ForService "Backend" ${function:Test-BackendReady} 120 $BackendLog)) {
    throw "Backend startup failed or MySQL is not ready. See: $BackendLog"
}

if (-not $frontendWasReady) {
    $npmPath = Get-CommandPath @("npm.cmd", "npm")
    if ($null -eq $npmPath) {
        throw "npm command not found. Install Node.js/npm and make sure npm is available in PATH."
    }
    if (-not (Test-Path -LiteralPath (Join-Path $FrontendDir "node_modules"))) {
        throw "Frontend dependencies are missing. Run 'npm install' in the frontend directory first."
    }

    Start-ManagedProcess "Frontend" $FrontendDir $npmPath @("run", "dev") $FrontendLog $FrontendPidFile | Out-Null
}

if (-not (Wait-ForService "Frontend" { Test-HttpOk $FrontendReadyUrl } 90 $FrontendLog)) {
    throw "Frontend startup failed. See: $FrontendLog"
}

if (-not $NoBrowser) {
    Start-Process $FrontendOpenUrl
    Write-Host "Browser opened: $FrontendOpenUrl"
} else {
    Write-Host "Browser opening skipped. Open manually: $FrontendOpenUrl"
}

Write-Host "Coffee Manager local services are ready."
