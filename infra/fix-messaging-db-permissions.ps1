# PowerShell script to fix PostgreSQL schema permissions for travelo_messaging database
# Run this script to fix the "permission denied for schema public" error

param(
    [string]$Host = "localhost",
    [int]$Port = 5432,
    [string]$SuperUser = "postgres",
    [string]$SuperPassword = "postgres",
    [string]$AppUser = "travelo",
    [string]$Database = "travelo_messaging",
    [string]$ContainerName = "infra-postgres-1"
)

$ErrorActionPreference = "Stop"

Write-Host "Fixing PostgreSQL schema permissions for $Database database..." -ForegroundColor Green

# Check if running via Docker or direct PostgreSQL connection
$isDocker = $false
try {
    $containerCheck = docker ps --filter "name=$ContainerName" --format "{{.Names}}" 2>&1
    if ($containerCheck -and $containerCheck -eq $ContainerName) {
        $isDocker = $true
        Write-Host "Using Docker container: $ContainerName" -ForegroundColor Yellow
    }
} catch {
    Write-Host "Docker not available, using direct PostgreSQL connection" -ForegroundColor Yellow
}

if ($isDocker) {
    # Using Docker
    Write-Host "Connecting to Docker container..." -ForegroundColor Yellow
    
    $sql = @"
\connect $Database
GRANT USAGE ON SCHEMA public TO $AppUser;
GRANT CREATE ON SCHEMA public TO $AppUser;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO $AppUser;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO $AppUser;
GRANT ALL PRIVILEGES ON ALL FUNCTIONS IN SCHEMA public TO $AppUser;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO $AppUser;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON SEQUENCES TO $AppUser;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON FUNCTIONS TO $AppUser;
SELECT 'Permissions granted successfully' AS status;
"@
    
    $sql | docker exec -i $ContainerName psql -U $SuperUser -d postgres
    
    if ($LASTEXITCODE -eq 0) {
        Write-Host "✓ Permissions granted successfully for $Database" -ForegroundColor Green
    } else {
        Write-Host "✗ Failed to grant permissions. Error code: $LASTEXITCODE" -ForegroundColor Red
        Write-Host "Make sure the database exists. You may need to create it first." -ForegroundColor Yellow
        exit 1
    }
} else {
    # Direct PostgreSQL connection
    Write-Host "Connecting directly to PostgreSQL..." -ForegroundColor Yellow
    $env:PGPASSWORD = $SuperPassword
    
    $sql = @"
GRANT USAGE ON SCHEMA public TO $AppUser;
GRANT CREATE ON SCHEMA public TO $AppUser;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO $AppUser;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO $AppUser;
GRANT ALL PRIVILEGES ON ALL FUNCTIONS IN SCHEMA public TO $AppUser;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO $AppUser;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON SEQUENCES TO $AppUser;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON FUNCTIONS TO $AppUser;
SELECT 'Permissions granted successfully' AS status;
"@
    
    $sql | psql -h $Host -p $Port -U $SuperUser -d $Database
    
    if ($LASTEXITCODE -eq 0) {
        Write-Host "✓ Permissions granted successfully for $Database" -ForegroundColor Green
    } else {
        Write-Host "✗ Failed to grant permissions. Error code: $LASTEXITCODE" -ForegroundColor Red
        exit 1
    }
    
    Remove-Item Env:\PGPASSWORD
}

Write-Host "`nPermission fix completed! Please restart the messaging-service." -ForegroundColor Green
Write-Host "You can restart it with: docker compose restart messaging-service" -ForegroundColor Cyan

