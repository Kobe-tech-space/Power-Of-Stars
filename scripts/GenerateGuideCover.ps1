param([string]$ProjectRoot = (Split-Path -Parent $PSScriptRoot))
$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing
$bitmap = [System.Drawing.Bitmap]::new(64, 64)
$graphics = [System.Drawing.Graphics]::FromImage($bitmap)
$graphics.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::None
$graphics.Clear([System.Drawing.Color]::Transparent)
function Fill-Polygon($hex, $coordinates) {
    $brush = [System.Drawing.SolidBrush]::new([System.Drawing.ColorTranslator]::FromHtml($hex))
    $points = [System.Drawing.Point[]]@($coordinates | ForEach-Object { [System.Drawing.Point]::new($_[0], $_[1]) })
    $graphics.FillPolygon($brush, $points)
    $brush.Dispose()
}
Fill-Polygon '#09121F' @(@(10,12),@(17,6),@(53,12),@(53,56),@(47,61),@(10,54))
Fill-Polygon '#A8B4BB' @(@(13,15),@(49,20),@(49,55),@(45,58),@(13,52))
Fill-Polygon '#E4E8E7' @(@(15,14),@(50,19),@(50,53),@(46,56),@(15,50))
Fill-Polygon '#24465C' @(@(12,12),@(18,7),@(18,49),@(12,53))
Fill-Polygon '#12263C' @(@(18,7),@(52,13),@(52,54),@(18,48))
Fill-Polygon '#19334D' @(@(20,10),@(49,15),@(49,50),@(20,45))
$edge = [System.Drawing.Pen]::new([System.Drawing.ColorTranslator]::FromHtml('#64BFD6'), 1)
$graphics.DrawLine($edge, 22, 14, 45, 18)
$graphics.DrawLine($edge, 22, 42, 45, 46)
$graphics.DrawEllipse($edge, 25, 22, 19, 16)
$edge.Dispose()
Fill-Polygon '#DBF5FF' @(@(35,22),@(37,28),@(43,31),@(37,33),@(35,39),@(33,33),@(27,31),@(33,29))
Fill-Polygon '#68C9E5' @(@(35,27),@(38,31),@(35,34),@(32,31))
Fill-Polygon '#A893D3' @(@(42,48),@(47,49),@(47,59),@(44,56),@(42,57))
$destination = Join-Path $ProjectRoot 'src/main/resources/assets/tutorial_mod/textures/item/astral_guide.png'
$bitmap.Save($destination, [System.Drawing.Imaging.ImageFormat]::Png)
$preview = [System.Drawing.Bitmap]::new(512,512)
$previewGraphics = [System.Drawing.Graphics]::FromImage($preview)
$previewGraphics.Clear([System.Drawing.ColorTranslator]::FromHtml('#101A28'))
$previewGraphics.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::NearestNeighbor
$previewGraphics.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::Half
$previewGraphics.DrawImage($bitmap, [System.Drawing.Rectangle]::new(0,0,512,512))
$preview.Save((Join-Path $ProjectRoot 'docs/art/astral_guide_cover_preview.png'), [System.Drawing.Imaging.ImageFormat]::Png)
$previewGraphics.Dispose(); $preview.Dispose(); $graphics.Dispose(); $bitmap.Dispose()
Write-Output 'Generated minimal navy/cyan astral guide cover.'
