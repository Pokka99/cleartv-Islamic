# Windows version of theme.sh: push a theme folder (config.json and optionally wallpaper.jpg) to the
# launcher on the TV and restart Home. See themes/README.md for the keys.
#
# Usage: tools\theme.ps1 -TvHost 192.168.1.50 themes\aurora
#        tools\theme.ps1 -TvHost 192.168.1.50 -Reset
param(
    [Parameter(Position = 0)][string]$Theme,
    [switch]$Reset,
    [string]$TvHost = $env:TV_HOST
)
$ErrorActionPreference = 'Stop'
$OutputEncoding = New-Object System.Text.UTF8Encoding $false
if (-not $TvHost) { throw 'pass -TvHost <tv ip> or set $env:TV_HOST' }
$env:TV_HOST = $TvHost
# Resolve the theme folder against the caller's directory before moving into tools\.
if ($Theme) { $Theme = (Resolve-Path $Theme).Path }
Set-Location $PSScriptRoot
$py = 'venv\Scripts\python.exe'
if (-not (Test-Path $py)) { throw 'run: python -m venv tools\venv; tools\venv\Scripts\python.exe -m pip install -r tools\requirements.txt' }
$pkg = 'com.gathin.tvhome'
$dir = "/sdcard/Android/data/$pkg/files"

if ($Reset) {
    & $py -I tv.py "rm -f $dir/config.json $dir/wallpaper.jpg $dir/azan.mp3 $dir/azan-subuh.mp3; rm -rf $dir/masjid; am force-stop $pkg; am start -n $pkg/.MainActivity >/dev/null; echo reset"
    exit 0
}
if (-not $Theme) { throw 'usage: theme.ps1 <theme folder> | -Reset' }
$config = Join-Path $Theme 'config.json'
if (-not (Test-Path $config)) { throw "no config.json in $Theme" }
try { Get-Content $config -Raw | ConvertFrom-Json | Out-Null } catch { throw "$config is not valid JSON" }

& $py -I tv.py "mkdir -p $dir" | Out-Null
& $py -I tv.py --push $config "$dir/config.json"
$wall = Join-Path $Theme 'wallpaper.jpg'
if (Test-Path $wall) { & $py -I b64push.py $wall "$dir/wallpaper.jpg" } else { & $py -I tv.py "rm -f $dir/wallpaper.jpg" | Out-Null }
# Mosque photos behind the Islamic screensaver (tools/masjid-photos.py).
& $py -I tv.py "rm -rf $dir/masjid" | Out-Null
$masjid = Join-Path $Theme 'masjid'
if (Test-Path $masjid) {
    & $py -I tv.py "mkdir -p $dir/masjid" | Out-Null
    Get-ChildItem $masjid -File | Where-Object { $_.Extension -eq '.jpg' -or $_.Name -eq 'captions.json' } | ForEach-Object {
        & $py -I b64push.py $_.FullName "$dir/masjid/$($_.Name)"
    }
}
# The azan's sound (prayer_zone): azan.mp3, and azan-subuh.mp3 for Subuh.
foreach ($f in 'azan.mp3', 'azan-subuh.mp3') {
    $src = Join-Path $Theme $f
    if (Test-Path $src) { & $py -I b64push.py $src "$dir/$f" } else { & $py -I tv.py "rm -f $dir/$f" | Out-Null }
}
& $py -I tv.py "am force-stop $pkg; am start -n $pkg/.MainActivity >/dev/null; echo applied"
Write-Host "Theme $(Split-Path $Theme -Leaf) is on the TV. Undo with: tools\theme.ps1 -TvHost $TvHost -Reset"
