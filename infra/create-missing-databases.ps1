# PowerShell script to create missing databases in existing PostgreSQL instance

Write-Host "Creating missing databases..." -ForegroundColor Green

Get-Content create-missing-databases.sql | docker exec -i infra-postgres-1 psql -U travelo -d postgres

Write-Host "Done! Databases created." -ForegroundColor Green

