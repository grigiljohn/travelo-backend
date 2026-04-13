# PostgreSQL Connection Test Script
# This script helps you find the correct PostgreSQL credentials

Write-Host "Testing PostgreSQL Connection on port 5433..." -ForegroundColor Cyan
Write-Host ""

# Common usernames to try
$usernames = @("postgres", "admin", "root", $env:USERNAME)

# Common passwords to try
$passwords = @("", "postgres", "admin", "root", "password", "travelo", "123456")

$host = "localhost"
$port = "5433"
$database = "postgres"  # Try connecting to default postgres database first

Write-Host "Attempting to find correct credentials..." -ForegroundColor Yellow
Write-Host ""

foreach ($username in $usernames) {
    foreach ($password in $passwords) {
        try {
            # Try to connect using .NET PostgreSQL driver
            $connectionString = "Host=$host;Port=$port;Database=$database;Username=$username;Password=$password"
            
            # Note: This requires Npgsql NuGet package, so we'll use a different approach
            Write-Host "Trying: Username=$username, Password=[hidden]" -ForegroundColor Gray
            
            # Alternative: Try using psql if available
            if (Get-Command psql -ErrorAction SilentlyContinue) {
                $env:PGPASSWORD = $password
                $result = echo "SELECT 1;" | psql -h $host -p $port -U $username -d $database -t 2>&1
                if ($LASTEXITCODE -eq 0) {
                    Write-Host ""
                    Write-Host "✓ SUCCESS! Found working credentials:" -ForegroundColor Green
                    Write-Host "  Username: $username" -ForegroundColor Green
                    Write-Host "  Password: $password" -ForegroundColor Green
                    Write-Host "  Port: $port" -ForegroundColor Green
                    Write-Host ""
                    Write-Host "Update your application.yml with these values!" -ForegroundColor Yellow
                    $env:PGPASSWORD = ""
                    exit 0
                }
            }
        } catch {
            # Continue trying
        }
    }
}

Write-Host ""
Write-Host "Could not automatically find credentials." -ForegroundColor Red
Write-Host ""
Write-Host "Manual steps:" -ForegroundColor Yellow
Write-Host "1. Check your PostgreSQL installation directory for configuration files"
Write-Host "2. Check if you saved credentials during installation"
Write-Host "3. Try connecting with pgAdmin or another GUI tool"
Write-Host "4. If you have psql installed, try: psql -h localhost -p 5433 -U postgres"
Write-Host ""
Write-Host "Common default combinations to try manually:" -ForegroundColor Cyan
Write-Host "  - Username: postgres, Password: (empty)"
Write-Host "  - Username: postgres, Password: postgres"
Write-Host "  - Username: postgres, Password: admin"
Write-Host ""

