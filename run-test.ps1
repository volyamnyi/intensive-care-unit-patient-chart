# Start servers and run test
$ErrorActionPreference = 'Continue'

# Ensure application databases exist
Write-Host "Checking application databases..."
$psql = "C:\Program Files\PostgreSQL\18\bin\psql.exe"
$env:PGPASSWORD = 'admin'
foreach ($db in @('my_fullstack_core','my_fullstack_icu','my_fullstack_med','my_fullstack_prosth')) {
    $exists = (& $psql -U postgres -h localhost -d postgres -tAc "SELECT 1 FROM pg_database WHERE datname='$db'") -join ''
    if ($exists -ne '1') {
        Write-Host "Creating database $db..."
        & $psql -U postgres -h localhost -d postgres -c "CREATE DATABASE $db"
    }
}

# Cleanup
Get-Process -Name node -ErrorAction SilentlyContinue | Stop-Process -Force -ErrorAction SilentlyContinue
Get-Process -Name java -ErrorAction SilentlyContinue | Stop-Process -Force -ErrorAction SilentlyContinue
Start-Sleep -Seconds 3

# Start backend
Write-Host "Starting backend..."
$backend = Start-Process -FilePath "cmd.exe" -ArgumentList "/c cd /d C:\projects\intensive-care-unit-patient-chart\backend\common && mvn spring-boot:run -DskipTests" -PassThru -NoNewWindow -RedirectStandardOutput "C:\projects\intensive-care-unit-patient-chart\backend-run.log" -RedirectStandardError "C:\projects\intensive-care-unit-patient-chart\backend-error.log"

# Wait for backend
Write-Host "Waiting for backend to start..."
Start-Sleep -Seconds 60

# Start frontend
Write-Host "Starting frontend..."
$frontend = Start-Process -FilePath "cmd.exe" -ArgumentList "/c cd /d C:\projects\intensive-care-unit-patient-chart\frontend && npm run dev" -PassThru -NoNewWindow -RedirectStandardOutput "C:\projects\intensive-care-unit-patient-chart\frontend-run.log" -RedirectStandardError "C:\projects\intensive-care-unit-patient-chart\frontend-error.log"

# Wait for frontend
Write-Host "Waiting for frontend to start..."
Start-Sleep -Seconds 15

# Check servers
Write-Host "`n=== Checking servers ==="
try {
    $r = Invoke-WebRequest -Uri "http://localhost:8085/api/prosthesis-manufacturing/patients?query=test" -UseBasicParsing -TimeoutSec 5
    Write-Host "Backend: OK"
} catch {
    Write-Host "Backend: $($_.Exception.Message)"
}

try {
    $r = Invoke-WebRequest -Uri "http://localhost:5173/" -UseBasicParsing -TimeoutSec 5
    Write-Host "Frontend: OK"
} catch {
    Write-Host "Frontend: $($_.Exception.Message)"
}

# Run test
Write-Host "`n=== Running test ==="
Set-Location C:\projects\intensive-care-unit-patient-chart\tests
npx playwright test --config=playwright-spec-verification.config.ts --timeout=300000 2>&1

Write-Host "`n=== Test complete ==="
