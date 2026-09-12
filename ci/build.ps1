#Requires -Version 5.1
<#
 ci/build.ps1 — compile the MCSM extras and assemble the jar ON YOUR PC.

 You already have everything this needs: Windows 11, a JDK 25 (your Azul
 Zulu 25 works — the same one Minecraft uses), and internet. PowerShell ships
 with Windows, so there is nothing to install.

   1. Open PowerShell.
   2. cd into your clone of the repo  (cd C:\...\Lowuuuuuu)
   3. powershell -ExecutionPolicy Bypass -File ci\build.ps1
        (optionally:  -Version 1.9.98  to pin a version)

 Output lands in  .\out\dabywitherstormmod-<version>-26.2-beta-mcsm.jar
 Put that ONE jar in your mods folder, replacing older builds.

 This is the same recipe as ci/build.sh (the one GitHub Actions runs once the
 workflow file is installed — see ci\README.md).
#>

param([string]$Version = "")

$ErrorActionPreference = "Stop"
$root = (Get-Location).Path
if ($Version -eq "") { $Version = (Get-Content "$root\VERSION").Trim() }
$jarId = "$Version-26.2-beta-mcsm"
Write-Host "[build] MCSM $jarId"

# --- newest delivery jar as the base -------------------------------------
$base = Get-ChildItem "$root\delivery\dabywitherstormmod-*-26.2-beta-mcsm.jar" |
        Sort-Object { [version]($_.Name -replace '.*?(\d+\.\d+\.\d+)-.*','$1') } |
        Select-Object -Last 1
Write-Host "[build] base jar: $($base.Name)"

# --- dependencies (only downloaded once, cached in .\ci\.\cache) -----------
$cache = "$root\ci\cache"
New-Item -ItemType Directory -Force -Path $cache | Out-Null

# Resolve the Minecraft client jar from the LIVE version manifest — a pinned
# object hash goes stale and 404s. Falls back to the pinned hash offline.
$clientUrl = "https://piston-data.mojang.com/v1/objects/2dc72797acbc1b63fc16a11c4ac393605f453754/client.jar"
try {
  $manifest = Invoke-RestMethod -Uri "https://piston-meta.mojang.com/mc/game/version_manifest_v2.json" -TimeoutSec 30
  $v = $manifest.versions | Where-Object { $_.id -eq "26.2" } | Select-Object -First 1
  if ($v) {
    $vmeta = Invoke-RestMethod -Uri $v.url -TimeoutSec 30
    if ($vmeta.downloads.client.url) { $clientUrl = $vmeta.downloads.client.url }
  }
} catch { Write-Host "[deps] manifest unreachable — using pinned client hash" }

$deps = @(
  @{ url = $clientUrl; file = "client.jar" },
  @{ url = "https://repo1.maven.org/maven2/net/fabricmc/sponge-mixin/0.15.4+mixin.0.8.7/sponge-mixin-0.15.4+mixin.0.8.7.jar"; file = "mixin.jar" },
  @{ url = "https://repo1.maven.org/maven2/org/jspecify/jspecify/1.0.0/jspecify-1.0.0.jar"; file = "jspecify.jar" },
  @{ url = "https://repo1.maven.org/maven2/it/unimi/dsi/fastutil/8.5.15/fastutil-8.5.15.jar"; file = "fastutil.jar" },
  @{ url = "https://libraries.minecraft.net/com/mojang/datafixerupper/8.0.16/datafixerupper-8.0.16.jar"; file = "dfu.jar" },
  @{ url = "https://libraries.minecraft.net/org/joml/joml/1.10.8/joml-1.10.8.jar"; file = "joml.jar" }
)
foreach ($d in $deps) {
  $out = Join-Path $cache $d.file
  if (-not (Test-Path $out)) {
    Write-Host "[deps] downloading $($d.file) ..."
    Invoke-WebRequest -Uri $d.url -OutFile $out
  }
}

# --- find javac -------------------------------------------------------------
$javac = (Get-Command javac -ErrorAction SilentlyContinue).Source
if (-not $javac) { $javac = "$env:JAVA_HOME\bin\javac.exe" }
if (-not (Test-Path $javac)) { throw "javac not found — install/point JAVA_HOME at your JDK 25 (the Azul one you play with works)." }
$javaBin = Split-Path $javac           # all JDK tools live beside javac
$jarTool = Join-Path $javaBin "jar.exe"
& $javac -version

# --- compile ----------------------------------------------------------------
$buildDir = "$root\ci\out-classes"
if (Test-Path $buildDir) { Remove-Item -Recurse -Force $buildDir }
New-Item -ItemType Directory -Force -Path $buildDir | Out-Null
New-Item -ItemType Directory -Force -Path "$root\out" | Out-Null
$sources = Get-ChildItem -Recurse "$root\mcsm-extras\java" -Filter *.java | ForEach-Object { $_.FullName }
$cp = @( "$cache\client.jar"; $base.FullName; "$cache\mixin.jar"; "$cache\jspecify.jar"; "$cache\fastutil.jar"; "$cache\dfu.jar"; "$cache\joml.jar" ) -join ";"
# Survivable: on failure the full log is saved next to the output jar and the
# jar is still assembled from the old classes + current shaders.
& $javac -nowarn --release 25 -proc:none -cp "$cp" -d "$buildDir" $sources 2>&1 | Tee-Object -FilePath "$root\ci\javac-last.log"
$javacRc = $LASTEXITCODE
$nClasses = @(Get-ChildItem -Recurse $buildDir -Filter *.class -ErrorAction SilentlyContinue).Count
if ($javacRc -ne 0) {
  Write-Host "[javac] FAILED (exit $javacRc) — continuing with a shaders-only jar." -ForegroundColor Red
  Write-Host "[javac] The jar will keep the OLD Java classes. Full log: ci\javac-last.log" -ForegroundColor Red
  Copy-Item "$root\ci\javac-last.log" "$root\out\JAVAC_FAILED.txt" -Force -ErrorAction SilentlyContinue
} else {
  Write-Host "[javac] OK: $nClasses classes"
}

# --- assemble the new jar ---------------------------------------------------
$fx = "$root\ci\out-unzip"
if (Test-Path $fx) { Remove-Item -Recurse -Force $fx }
New-Item -ItemType Directory -Force -Path $fx | Out-Null
Write-Host "[assemble] extracting base..."
Push-Location $fx
try { & $jarTool -xf $base.FullName } finally { Pop-Location }

Copy-Item -Recurse -Force "$root\mcsm-core-shaders\*" "$fx\assets\minecraft\shaders\"
Copy-Item -Recurse -Force "$root\jar-overrides\*" "$fx\"
# only overlay fresh classes when the compile produced some (empty dir would
# otherwise abort the script — that bug used to eat the whole jar)
if ($nClasses -gt 0) {
  Copy-Item -Recurse -Force "$buildDir\*" "$fx\"
}
# Native SkyRenderer is authoritative; remove legacy texture-pack sky paths
# even when the pinned base jar still carries them.
Remove-Item -Recurse -Force "$fx\assets\fabricskyboxes", "$fx\assets\dabywitherstormmod\textures\sky", "$fx\assets\dabywitherstormmod\textures\mcsm_atmosphere\sky" -ErrorAction SilentlyContinue
Get-ChildItem "$fx\assets\dabywitherstormmod\textures\environment" -Filter "storymode_sky_*.png" -ErrorAction SilentlyContinue | Remove-Item -Force -ErrorAction SilentlyContinue
Remove-Item -Force "$fx\net\mcsm\extras\client\McsmBlobOval.class", "$fx\net\mcsm\extras\client\McsmBlobShape.class", "$fx\net\mcsm\extras\client\McsmSkyDome.class", "$fx\net\mcsm\extras\client\McsmStormSkyLayer.class", "$fx\net\dabicco\witherstormmod\mixin\StormSkyGradientMixin.class", "$fx\net\dabicco\witherstormmod\mixin\StoryModeSkyDomeMixin.class" -ErrorAction SilentlyContinue
Get-ChildItem "$fx\net\mcsm\extras\client" -Filter "McsmBlobOval`$*.class" -ErrorAction SilentlyContinue | Remove-Item -Force -ErrorAction SilentlyContinue
Get-ChildItem "$fx\net\mcsm\extras\client" -Filter "McsmBlobShape`$*.class" -ErrorAction SilentlyContinue | Remove-Item -Force -ErrorAction SilentlyContinue

$fmj = "$fx\fabric.mod.json"
(Get-Content $fmj -Raw) -replace '"version": "[0-9.]+-26\.2-beta-mcsm"', """version"": ""$jarId""" | Set-Content $fmj -Encoding ASCII

$outDir = "$root\out"
New-Item -ItemType Directory -Force -Path $outDir | Out-Null
$outJar = "$outDir\dabywitherstormmod-$jarId.jar"
if (Test-Path $outJar) { Remove-Item -Force $outJar }
Push-Location $fx
try {
  & $jarTool -cf $outJar .
  if ($LASTEXITCODE -ne 0) { throw "jar packaging failed" }
} finally { Pop-Location }

$hash = (Get-FileHash $outJar -Algorithm SHA256).Hash.ToLower()
"$hash  $(Split-Path $outJar -Leaf)" | Set-Content "$outJar.sha256" -Encoding ASCII
Write-Host "[done] $outJar"
Write-Host "[sha256] $hash"
Write-Host "Put this ONE jar in your mods folder. Remove older dabywitherstormmod jars first."
