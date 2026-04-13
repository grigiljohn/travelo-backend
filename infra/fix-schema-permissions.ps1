# PowerShell script to fix PostgreSQL schema permissions
# This script grants necessary permissions on the public schema for all Travelo databases

param(
    [string]$Host = "localhost",
    [int]$Port = 5432,
    [string]$SuperUser = "postgres",
    [string]$SuperPassword = "postgres",
    [string]$AppUser = "travelo"
)

$ErrorActionPreference = "Stop"

Write-Host "Fixing PostgreSQL schema permissions for Travelo databases..." -ForegroundColor Green

# Database list
$databases = @(
    "travelo_posts",
    "travelo_ads",
    "travelo_media",
    "travelo_notifications",
    "travelo_messaging",
    "travelo_stories",
    "travelo_reels",
    "travelo_users",
    "travelo_admin",
    "travelo_auth"
)

# Set PGPASSWORD environment variable
$env:PGPASSWORD = $SuperPassword

foreach ($db in $databases) {
    Write-Host "Fixing permissions for database: $db" -ForegroundColor Yellow
    
    # Check if database exists
    $dbExists = & psql -h $Host -p $Port -U $SuperUser -d postgres -tAc "SELECT 1 FROM pg_database WHERE datname='$db'" 2>&1
    
    if ($dbExists -match "1") {
        # Grant permissions
        $sql = @"
GRANT USAGE ON SCHEMA public TO $AppUser;
GRANT CREATE ON SCHEMA public TO $AppUser;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO $AppUser;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO $AppUser;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO $AppUser;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON SEQUENCES TO $AppUser;
"@
        
        $sql | & psql -h $Host -p $Port -U $SuperUser -d $db -q
        
        if ($LASTEXITCODE -eq 0) {
            Write-Host "  ✓ Permissions granted for $db" -ForegroundColor Green
        } else {
            Write-Host "  ✗ Failed to grant permissions for $db" -ForegroundColor Red
        }
    } else {
        Write-Host "  ⚠ Database $db does not exist, skipping..." -ForegroundColor Yellow
    }
}

Write-Host "`nSchema permissions fix completed!" -ForegroundColor Green

