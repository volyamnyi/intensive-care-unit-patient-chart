$files = Get-ChildItem -Path "$PSScriptRoot\specs" -Recurse -Filter "*.ts" | Select-Object -ExpandProperty FullName
$changed = 0
foreach ($f in $files) {
    $c = Get-Content $f -Raw
    $o = $c

    # \/doctor\/episode\/ -> \/prescriptions\/icu\/doctor\/episode\/
    $c = $c -replace [regex]::Escape('\/doctor\/episode\/'), '\/prescriptions\/icu\/doctor\/episode\/'
    # \/nurse\/episode\/ -> \/prescriptions\/icu\/nurse\/episode\/
    $c = $c -replace [regex]::Escape('\/nurse\/episode\/'), '\/prescriptions\/icu\/nurse\/episode\/
    # '/doctor/create-card' -> '/prescriptions/icu/doctor/create-card'
    $c = $c -replace [regex]::Escape("'/doctor/create-card'"), "'/prescriptions/icu/doctor/create-card'"
    # \/prescriptions\/nurse (not followed by icu) -> \/prescriptions\/icu\/nurse
    $c = $c -replace '\\/prescriptions\\/(?!icu\\/)nurse', '\/prescriptions\/icu\/nurse'

    if ($c -ne $o) {
        Set-Content -Path $f -Value $c -NoNewline
        Write-Output "Updated $([System.IO.Path]::GetFileName($f))"
        $changed++
    }
}
Write-Output "Updated $changed files"
