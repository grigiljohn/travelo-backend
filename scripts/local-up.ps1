<#
.SYNOPSIS
    Boots the full Travelo backend on Windows Docker Desktop.

.DESCRIPTION
    Wraps the common `docker compose` invocations so you don't have to remember
    the flag soup. Run from the repo root.

    Examples:
      # First-time build, start everything in the background:
      .\scripts\local-up.ps1 -Build

      # Just start (reuse cached images):
      .\scripts\local-up.ps1

      # Tear everything down (keeps volumes):
      .\scripts\local-up.ps1 -Down

      # Full reset (removes DB/Redis/uploads volumes):
      .\scripts\local-up.ps1 -Down -Purge

      # Tail logs for one or more services:
      .\scripts\local-up.ps1 -Logs api-gateway,social-service,media-service
#>
[CmdletBinding()]
param(
    [switch] $Build,
    [switch] $Down,
    [switch] $Purge,
    [switch] $Tools,
    [string[]] $Logs
)

$ErrorActionPreference = "Stop"
Set-Location -Path (Split-Path -Parent $PSScriptRoot)

if (-not (Test-Path ".env")) {
    Write-Host "No .env found — copying .env.example → .env (edit it for prod creds)." -ForegroundColor Yellow
    Copy-Item ".env.example" ".env"
}

$profiles = @()
if ($Tools) { $profiles += "--profile"; $profiles += "tools" }

if ($Down) {
    if ($Purge) {
        docker compose @profiles down -v --remove-orphans
    } else {
        docker compose @profiles down --remove-orphans
    }
    exit $LASTEXITCODE
}

if ($Logs) {
    docker compose logs -f --tail=200 @Logs
    exit $LASTEXITCODE
}

$upArgs = @("up", "-d")
if ($Build) { $upArgs += "--build" }

Write-Host "Bringing up Travelo backend…" -ForegroundColor Cyan
docker compose @profiles @upArgs
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

Write-Host ""
Write-Host "Stack is starting. Useful URLs:" -ForegroundColor Green
Write-Host "  API Gateway ........ http://localhost:8080"
Write-Host "  Identity ........... http://localhost:8081/swagger-ui.html"
Write-Host "  Social ............. http://localhost:8096/swagger-ui.html"
Write-Host "  Media .............. http://localhost:8084/swagger-ui.html"
Write-Host "  Discovery (search).. http://localhost:8088/actuator/health"
Write-Host "  Realtime ........... http://localhost:8098/actuator/health"
Write-Host "  Commerce ........... http://localhost:8097/swagger-ui.html"
Write-Host "  Config server ...... http://localhost:8888/actuator/health"
Write-Host "  Eureka ............. http://localhost:8761"
Write-Host "  Kafka bootstrap .... localhost:9092 (host) / kafka:29092 (in-network)"
Write-Host "  Redis .............. localhost:6379"
Write-Host "  Postgres ........... localhost:5432 (travelo/travelo)"
if ($Tools) { Write-Host "  pgAdmin ............ http://localhost:5050 (admin@travelo.local / admin)" }
Write-Host ""
Write-Host "Tail everything with: docker compose logs -f" -ForegroundColor DarkGray
