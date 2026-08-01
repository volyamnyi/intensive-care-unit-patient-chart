$files = Get-ChildItem -Path "$PSScriptRoot\specs" -Recurse -Filter "*.ts" | Select-Object -ExpandProperty FullName
$changed = 0
foreach ($f in $files) {
    $c = Get-Content $f -Raw
    $o = $c

    # \/doctor\/episode\/ -> \/icu\/doctor\/episode\/
    $c = $c -replace [regex]::Escape('\/doctor\/episode\/'), '\/icu\/doctor\/episode\/'
    # \/nurse\/episode\/ -> \/icu\/nurse\/episode\/
    $c = $c -replace [regex]::Escape('\/nurse\/episode\/'), '\/icu\/nurse\/episode\/''
    # '/doctor/create-card' -> '/icu/doctor/create-card'
    $c = $c -replace [regex]::Escape("'/doctor/create-card'"), "'/icu/doctor/create-card'"
    # \/prescriptions\/icu (not needed anymore, but keep for safety)
    $c = $c -replace '\\/prescriptions\\/icu\\/', '\/icu\/'

    if ($c -ne $o) {
        Set-Content -Path $f -Value $c -NoNewline
        Write-Output "Updated $([System.IO.Path]::GetFileName($f))"
        $changed++
    }
}
Write-Output "Updated $changed files"
