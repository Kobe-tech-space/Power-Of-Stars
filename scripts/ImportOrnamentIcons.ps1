param([string]$ManifestPath = 'docs/art/ornament_icons_v2/manifest.json', [int]$Size = 32)
$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing
Add-Type -ReferencedAssemblies @([Drawing.Bitmap].Assembly.Location,[Drawing.Rectangle].Assembly.Location) -TypeDefinition @'
using System.Drawing;
public static class OrnamentSpriteBounds {
    public static Rectangle Find(Bitmap image) {
        int minX=image.Width,minY=image.Height,maxX=-1,maxY=-1;
        for(int y=0;y<image.Height;y++) for(int x=0;x<image.Width;x++) {
            if(image.GetPixel(x,y).A<=32)continue;
            if(x<minX)minX=x;if(x>maxX)maxX=x;if(y<minY)minY=y;if(y>maxY)maxY=y;
        }
        return maxX<minX?Rectangle.Empty:new Rectangle(minX,minY,maxX-minX+1,maxY-minY+1);
    }
}
'@
$taskRoot = Split-Path -Parent $PSScriptRoot
$manifestFile = Join-Path $taskRoot $ManifestPath
$manifest = Get-Content -LiteralPath $manifestFile -Raw | ConvertFrom-Json
$archiveDirectory = Join-Path $taskRoot 'docs/art/ornament_icons_v2/original_hd'
$mastersDirectory = Join-Path $taskRoot 'docs/art/ornament_icons_v2/generated_masters'
$textureDirectory = Join-Path $taskRoot 'src/main/resources/assets/tutorial_mod/textures/item'
New-Item -ItemType Directory -Path $archiveDirectory,$mastersDirectory -Force | Out-Null
foreach ($entry in $manifest) {
    if ($entry.id -notmatch '^[a-z_]+$') { throw 'Invalid item ID' }
    $texturePath = Join-Path $textureDirectory ($entry.id + '.png')
    $backupPath = Join-Path $archiveDirectory ($entry.id + '.png')
    if (!(Test-Path -LiteralPath $backupPath)) { Copy-Item -LiteralPath $texturePath -Destination $backupPath }
    $masterPath = Join-Path $mastersDirectory ($entry.id + '.png')
    Copy-Item -LiteralPath $entry.source -Destination $masterPath -Force
    $source = [Drawing.Bitmap]::FromFile($masterPath)
    try {
        if ($source.GetPixel(0,0).A -gt 16) { throw "Opaque background: $($entry.id)" }
        $bounds = [OrnamentSpriteBounds]::Find($source)
        if ($bounds.IsEmpty) { throw "Empty generated sprite: $($entry.id)" }
        $factor = ($Size-2) / [double][Math]::Max($bounds.Width,$bounds.Height)
        $width = [Math]::Max(1,[int][Math]::Round($bounds.Width*$factor))
        $height = [Math]::Max(1,[int][Math]::Round($bounds.Height*$factor))
        $destination = [Drawing.Rectangle]::new([int][Math]::Floor(($Size-$width)/2),[int][Math]::Floor(($Size-$height)/2),$width,$height)
        $sprite = [Drawing.Bitmap]::new($Size,$Size,[Drawing.Imaging.PixelFormat]::Format32bppArgb)
        $graphics = [Drawing.Graphics]::FromImage($sprite)
        try {
            $graphics.Clear([Drawing.Color]::Transparent)
            $graphics.CompositingMode = [Drawing.Drawing2D.CompositingMode]::SourceCopy
            $graphics.InterpolationMode = [Drawing.Drawing2D.InterpolationMode]::NearestNeighbor
            $graphics.PixelOffsetMode = [Drawing.Drawing2D.PixelOffsetMode]::Half
            $graphics.DrawImage($source,$destination,$bounds,[Drawing.GraphicsUnit]::Pixel)
            $sprite.Save($texturePath,[Drawing.Imaging.ImageFormat]::Png)
        } finally { $graphics.Dispose(); $sprite.Dispose() }
        Write-Output "$($entry.id): native ${Size}x${Size}, transparent, centered"
    } finally { $source.Dispose() }
}
