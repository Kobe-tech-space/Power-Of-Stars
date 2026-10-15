param([string]$Source = 'C:\Users\Zx\Downloads\Cosmic_Armor_Inventory_Icons_64')
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path $PSScriptRoot -Parent
$assetRoot = Join-Path $projectRoot 'src/main/resources/assets/tutorial_mod'
$culture = [System.Globalization.CultureInfo]::InvariantCulture
foreach ($tier in @('low', 'high')) {
    $variant = if ($tier -eq 'low') { 'original' } else { 'textured' }
    $fileName = if ($tier -eq 'low') { 'Cosmic_Dread_Armor_Forge_1.20.1.bbmodel' } else { 'Cosmic_Dread_Armor_Textured_Forge_1.20.1.bbmodel' }
    $sourceModel = Get-Content -LiteralPath (Join-Path $Source $fileName) -Raw | ConvertFrom-Json
    $groupByCube = @{}
    foreach ($group in $sourceModel.groups) {
        $tree = $sourceModel.outliner | Where-Object uuid -eq $group.uuid
        foreach ($id in $tree.children) { $groupByCube[$id] = $group.name }
    }
    $cubes = foreach ($cube in $sourceModel.elements) {
        $group = $groupByCube[$cube.uuid]
        if (!$group) { throw "Unassigned cube $($cube.name)" }
        $slot = if ($group -eq 'head') { 'HEAD' } elseif ($cube.name -match 'boot|toe|heel') { 'FEET' }
            elseif ($group -match 'leg' -or $cube.name -match 'hip|tasset|waist|belt') { 'LEGS' } else { 'CHEST' }
        $faces = [ordered]@{}
        foreach ($side in @('north','south','east','west','up','down')) {
            $face = $cube.faces.$side
            if ($face.rotation -and $face.rotation -ne 0) { throw "Unsupported face rotation in $($cube.name)" }
            $uv = @($face.uv)
            $faces[$side] = @(([double]$uv[0]/$sourceModel.resolution.width), ([double]$uv[1]/$sourceModel.resolution.height),
                             ([double]$uv[2]/$sourceModel.resolution.width), ([double]$uv[3]/$sourceModel.resolution.height))
        }
        [ordered]@{name=$cube.name;group=$group.ToUpperInvariant();slot=$slot;from=$cube.from;to=$cube.to;
            origin=$cube.origin;rotation=$cube.rotation;faces=$faces;glow=($cube.name -match 'gem|crystal|inlay|core_crystal|edge|fin')}
    }
    $modelDir = Join-Path $assetRoot 'armor_models'
    $textureDir = Join-Path $assetRoot 'textures/models/armor'
    New-Item -ItemType Directory -Force -Path $modelDir,$textureDir | Out-Null
    [IO.File]::WriteAllText((Join-Path $modelDir "cosmic_$tier.json"), ($cubes | ConvertTo-Json -Depth 12), [Text.UTF8Encoding]::new($false))
    $encoded = $sourceModel.textures[0].source
    if ($encoded -notmatch '^data:image/png;base64,') { throw 'Missing embedded texture' }
    [IO.File]::WriteAllBytes((Join-Path $textureDir "cosmic_$tier.png"), [Convert]::FromBase64String($encoded.Substring($encoded.IndexOf(',')+1)))
    Copy-Item -LiteralPath (Join-Path $Source $fileName) -Destination (Join-Path $projectRoot "docs/art/cosmic_$tier.bbmodel")
    foreach ($part in @('helmet','chestplate','leggings','boots')) {
        $item = if ($tier -eq 'low') { "star_$part" } else { "cosmic_$part" }
        Copy-Item -LiteralPath (Join-Path $Source "assets/cosmic_armor/textures/item/cosmic_dread_${variant}_$part.png") -Destination (Join-Path $assetRoot "textures/item/$item.png")
        $itemModel = [ordered]@{parent='minecraft:item/generated';textures=@{layer0="tutorial_mod:item/$item"}}
        [IO.File]::WriteAllText((Join-Path $assetRoot "models/item/$item.json"),($itemModel|ConvertTo-Json -Depth 4),[Text.UTF8Encoding]::new($false))
    }
    Write-Output "Imported $tier : $($cubes.Count) cubes; texture $($sourceModel.resolution.width) px"
}

# Mechanical nearest-neighbor resizing preserves generated PNG alpha; originals remain archived.
Add-Type -AssemblyName System.Drawing
$generatedDir = 'C:\Users\Zx\.codex\generated_images\01a12e36-3e91-71a0-b418-02ff22ce2f15'
$images = @{
    star_singularity = 'exec-c0b1a222-5ce5-4a76-b7ab-33b4d272fcd9.png'
    star_upgrade_template = 'exec-f548118a-b3d7-41f8-bfd0-bb15501b582b.png'
}
foreach ($item in $images.Keys) {
    $imageFile = Join-Path $generatedDir $images[$item]
    Copy-Item -LiteralPath $imageFile -Destination (Join-Path $projectRoot "docs/art/${item}_generated.png")
    $sourceImage = [Drawing.Bitmap]::new($imageFile)
    $target = [Drawing.Bitmap]::new(64,64,[Drawing.Imaging.PixelFormat]::Format32bppArgb)
    $graphics = [Drawing.Graphics]::FromImage($target)
    $graphics.InterpolationMode = [Drawing.Drawing2D.InterpolationMode]::NearestNeighbor
    $graphics.PixelOffsetMode = [Drawing.Drawing2D.PixelOffsetMode]::Half
    $graphics.CompositingMode = [Drawing.Drawing2D.CompositingMode]::SourceCopy
    $graphics.DrawImage($sourceImage,[Drawing.Rectangle]::new(0,0,64,64),0,0,$sourceImage.Width,$sourceImage.Height,[Drawing.GraphicsUnit]::Pixel)
    $target.Save((Join-Path $assetRoot "textures/item/$item.png"),[Drawing.Imaging.ImageFormat]::Png)
    $graphics.Dispose(); $target.Dispose(); $sourceImage.Dispose()
    [IO.File]::WriteAllText((Join-Path $assetRoot "models/item/$item.json"), (@{parent='minecraft:item/generated';textures=@{layer0="tutorial_mod:item/$item"}}|ConvertTo-Json -Depth 4),[Text.UTF8Encoding]::new($false))
}
