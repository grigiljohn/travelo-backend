# Prints the fix for: FATAL: remaining connection slots are reserved for ... SUPERUSER
# That message means the PostgreSQL server has hit (max_connections - superuser_reserved_connections)
# for non-superuser roles. Raising max_connections and restarting the server is the real fix.
#
# Usage: open "SQL Shell (psql)" or psql, connect as user POSTGRES (superuser), then paste the
#   ALTER and restart steps shown below. Do not run this as your app user (travelo).

$target = 200
Write-Host ""
Write-Host "Run these as PostgreSQL superuser (typically user 'postgres'):" -ForegroundColor Cyan
Write-Host @"

SHOW max_connections;
SHOW superuser_reserved_connections;
SELECT count(*)::int AS open_backends
FROM pg_stat_activity;

-- Persist after restart (PostgreSQL 9.4+)
ALTER SYSTEM SET max_connections = '$target';

"@
Write-Host "Then restart the Windows service (e.g. 'postgresql-x64-16' in services.msc), then:" -ForegroundColor Yellow
Write-Host "SHOW max_connections;  -- expect $target"
Write-Host ""
Write-Host "Optional: free non-superuser sessions before ALTER (idle only, superuser only):"
Write-Host "SELECT usename, state, count(*) FROM pg_stat_activity GROUP BY 1,2 ORDER BY 1,2;"
Write-Host ""
$psql = "psql"
if (-not (Get-Command $psql -ErrorAction SilentlyContinue)) {
    $found = Get-ChildItem "C:\Program Files\PostgreSQL" -Recurse -Filter "psql.exe" -ErrorAction SilentlyContinue | Select-Object -First 1
    if ($found) { $psql = $found.FullName }
}
Write-Host "psql is usually: $psql" -ForegroundColor DarkGray
Write-Host ""
