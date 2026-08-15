@echo off
echo Starting servers...

REM Ensure application databases exist
echo Checking application databases...
set PGPASSWORD=admin
for %%D in (my_fullstack_core my_fullstack_icu my_fullstack_med my_fullstack_prosth) do (
    "C:\Program Files\PostgreSQL\18\bin\psql.exe" -U postgres -h localhost -d postgres -tAc "SELECT 1 FROM pg_database WHERE datname='%%D'" | findstr /C:"1" >nul
    if errorlevel 1 (
        echo Creating database %%D...
        "C:\Program Files\PostgreSQL\18\bin\psql.exe" -U postgres -h localhost -d postgres -c "CREATE DATABASE %%D"
    )
)

REM Kill existing processes
taskkill /F /IM java.exe 2>nul
taskkill /F /IM node.exe 2>nul
timeout /t 3 /nobreak >nul

REM Start backend
echo Starting backend...
start "Backend" cmd /k "cd /d C:\projects\intensive-care-unit-patient-chart\backend\common && mvn spring-boot:run -DskipTests"

REM Wait for backend
echo Waiting for backend (60s)...
timeout /t 60 /nobreak >nul

REM Start frontend
echo Starting frontend...
start "Frontend" cmd /k "cd /d C:\projects\intensive-care-unit-patient-chart\frontend && npm run dev"

REM Wait for frontend
echo Waiting for frontend (20s)...
timeout /t 20 /nobreak >nul

REM Run test
echo Running test...
cd /d C:\projects\intensive-care-unit-patient-chart\tests
npx playwright test --config=playwright-spec-verification.config.ts --headed --timeout=300000

echo Test complete!
pause
