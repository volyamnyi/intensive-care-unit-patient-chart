# Start servers and run test
$ErrorActionPreference = 'Continue'

# Cleanup
Get-Process -Name node -ErrorAction SilentlyContinue | Stop-Process -Force -ErrorAction SilentlyContinue
Get-Process -Name java -ErrorAction SilentlyContinue | Stop-Process -Force -ErrorAction SilentlyContinue
Start-Sleep -Seconds 3

# Start backend
Write-Host "Starting backend..."
$backend = Start-Process -FilePath "cmd.exe" -ArgumentList "/c cd /d C:\projects\intensive-care-unit-patient-chart\backend\icu-chart && mvn spring-boot:run -DskipTests" -PassThru -NoNewWindow -RedirectStandardOutput "C:\projects\intensive-care-unit-patient-chart\backend-run.log" -RedirectStandardError "C:\projects\intensive-care-unit-patient-chart\backend-error.log"

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
