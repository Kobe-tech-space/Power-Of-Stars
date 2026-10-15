$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
$jarPath = Join-Path $projectRoot 'build/libs/tutorial_mod-1.1.0-1.20.1.jar'
$dependency = Join-Path $projectRoot 'build/libs/dependencies/Patchouli-1.20.1-84.1-FORGE.jar'
if (!(Test-Path $jarPath) -or !(Test-Path $dependency)) { throw 'Build release and provide mandatory Patchouli dependency before packaging.' }
Add-Type -AssemblyName System.IO.Compression.FileSystem
$archive = [IO.Compression.ZipFile]::OpenRead($jarPath)
try {
    $entries = @($archive.Entries.FullName)
    if ($entries | Where-Object { $_ -match '(Smoke|GameTest|TierProgression|FeatureOptions)' -and $_ -like '*.class' }) { throw 'Development classes present in production JAR.' }
    foreach ($required in @('com/example/tutorial/bounty/BountySystem.class','com/example/tutorial/client/BountyScreen.class','com/example/tutorial/recipe/AstralRecyclingRecipe.class','com/example/tutorial/event/AstralStatistics.class','com/example/tutorial/config/ModSettings.class','assets/tutorial_mod/particles/stellar_spark.json','data/tutorial_mod/advancements/astral/multi_kill.json')) { if ($required -notin $entries) { throw "Missing release resource: $required" } }
    foreach ($lang in @('zh_cn','en_us')) {
        $pattern = "assets/tutorial_mod/patchouli_books/astral_guide/$lang/entries/*"
        if (@($entries | Where-Object { $_ -like $pattern -and $_ -like '*.json' }).Count -ne 38) { throw "Incorrect guide entry count for $lang" }
    }
    if (@($entries | Where-Object { $_ -like 'data/tutorial_mod/recipes/recycle_*.json' }).Count -ne 9) { throw 'Missing recycling recipes' }
    $metadataReader = [IO.StreamReader]::new($archive.GetEntry('META-INF/mods.toml').Open())
    try { $metadata = $metadataReader.ReadToEnd() } finally { $metadataReader.Dispose() }
    if (!$metadata.Contains('1.1.0-1.20.1') -or !$metadata.Contains('[47.4.10,48)') -or !$metadata.Contains('[1.20.1]')) { throw 'Release version constraints are not resolved.' }
} finally { $archive.Dispose() }
$delivery = Join-Path $projectRoot 'build/delivery/astral-complete-1.1.0-1.20.1'
$mods = Join-Path $delivery 'mods'
[IO.Directory]::CreateDirectory($mods) | Out-Null
Copy-Item -LiteralPath $jarPath -Destination $mods
Copy-Item -LiteralPath $dependency -Destination $mods
foreach ($file in @('INSTALL_COMPLETE.md','FEATURES_21_32.md','BOUNTIES.md','RELEASE_VERIFICATION.md')) { Copy-Item -LiteralPath (Join-Path $projectRoot "docs/$file") -Destination $delivery }
$zipPath = Join-Path $projectRoot 'build/delivery/astral-complete-1.1.0-1.20.1.zip'
Compress-Archive -LiteralPath (Join-Path $delivery 'mods'),(Join-Path $delivery 'INSTALL_COMPLETE.md'),(Join-Path $delivery 'FEATURES_21_32.md'),(Join-Path $delivery 'BOUNTIES.md'),(Join-Path $delivery 'RELEASE_VERIFICATION.md') -DestinationPath $zipPath -Force
$checkZip = [IO.Compression.ZipFile]::OpenRead($zipPath)
try {
    foreach ($entry in @('mods/tutorial_mod-1.1.0-1.20.1.jar','mods/Patchouli-1.20.1-84.1-FORGE.jar','INSTALL_COMPLETE.md')) { if (!$checkZip.GetEntry($entry)) { throw "Delivery ZIP missing $entry" } }
} finally { $checkZip.Dispose() }
Get-Item $jarPath,$zipPath | Select-Object FullName,Length,LastWriteTime
Get-FileHash -LiteralPath $jarPath,$zipPath -Algorithm SHA256 | Select-Object Path,Hash
