$process = Start-Process -FilePath "node" -ArgumentList "node_modules/vite/bin/vite.js", "--host" -WorkingDirectory "C:\projects\intensive-care-unit-patient-chart\frontend" -PassThru -WindowStyle Hidden
Write-Host "Vite started with PID: $($process.Id)"
