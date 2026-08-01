$files = @(
    "C:\projects\kilo\intensive-care-unit-patient-chart\tests\specs\doctor\exploratory.spec.ts",
    "C:\projects\kilo\intensive-care-unit-patient-chart\tests\specs\doctor\sidebar-resize.spec.ts",
    "C:\projects\kilo\intensive-care-unit-patient-chart\tests\specs\nurse\signoff-full-chain.spec.ts"
)
$bt = [char]96
$changes = 0
foreach ($f in $files) {
    $c = Get-Content $f -Raw
    $o = $c
    $c = $c.Replace("$bt/doctor/episode/", "$bt/icu/doctor/episode/")
    $c = $c.Replace("$bt/nurse/episode/", "$bt/icu/nurse/episode/")
    $c = $c.Replace("$bt/doctor/create-card", "$bt/icu/doctor/create-card")
    if ($c -ne $o) {
        Set-Content -Path $f -Value $c -NoNewline
        $changes++
    }
}
Write-Output "Fixed template-literal routes in $changes files"
