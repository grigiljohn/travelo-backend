# Creates PostgreSQL databases required by realtime-service (messaging + notifications).
# Run once on your dev machine if you see: FATAL: database "travelo_notifications" does not exist
#
# Usage (from repo root):
#   .\infra\create-realtime-databases.ps1
#   $env:PGPASSWORD='...'; .\infra\create-realtime-databases.ps1 -DbUser postgres
#
param(
    [string]$DbHost = "localhost",
    [string]$DbPort = "5432",
    [string]$DbUser = "travelo",
    [string]$AppRole = "travelo"
)

function Find-Psql {
    $cmd = Get-Command psql.exe -ErrorAction SilentlyContinue
    if ($cmd) { return $cmd.Source }
    foreach ($ver in 18, 17, 16, 15, 14) {
        $p = "${env:ProgramFiles}\PostgreSQL\$ver\bin\psql.exe"
        if (Test-Path $p) { return $p }
    }
    return $null
}

$psql = Find-Psql
if (-not $psql) {
    Write-Error "psql.exe not found. Add PostgreSQL bin to PATH or install PostgreSQL."
    exit 1
}

$dbs = @("travelo_messaging", "travelo_notifications")
foreach ($db in $dbs) {
    $sql = "SELECT 1 FROM pg_database WHERE datname = '$db'"
    $exists = & $psql -h $DbHost -p $DbPort -U $DbUser -d postgres -tAc $sql 2>$null
    if ($exists -match "1") {
        Write-Host "OK: database $db already exists"
        continue
    }
    Write-Host "Creating database $db ..."
    & $psql -h $DbHost -p $DbPort -U $DbUser -d postgres -v ON_ERROR_STOP=1 -c "CREATE DATABASE $db OWNER $AppRole;"
    if ($LASTEXITCODE -ne 0) {
        Write-Host "Hint: if permission denied, run with a superuser, e.g. -DbUser postgres (set PGPASSWORD first)."
        exit $LASTEXITCODE
    }
}
Write-Host "Done."
