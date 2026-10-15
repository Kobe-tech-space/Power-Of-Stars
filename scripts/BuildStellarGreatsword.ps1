param([Parameter(Mandatory=$true)][string]$TexturePath)
$ErrorActionPreference='Stop'
$root=Split-Path -Parent $PSScriptRoot
$output=Join-Path $root 'docs/art/stellar_greatsword_v2'
[IO.Directory]::CreateDirectory($output)|Out-Null
$atlas=Join-Path $output 'stellar_greatsword_atlas.png'
Copy-Item -LiteralPath $TexturePath -Destination $atlas -Force
Add-Type -AssemblyName System.Drawing
$original=[Drawing.Bitmap]::new($atlas)
# Mechanical nearest-neighbour conversion to a Minecraft-friendly power-of-two atlas.
$bitmap=[Drawing.Bitmap]::new(128,128)
$resize=[Drawing.Graphics]::FromImage($bitmap)
$resize.InterpolationMode='NearestNeighbor';$resize.PixelOffsetMode='Half'
$resize.DrawImage($original,[Drawing.Rectangle]::new(0,0,128,128),0,0,$original.Width,$original.Height,[Drawing.GraphicsUnit]::Pixel)
$resize.Dispose();$original.Dispose()
$bitmap.Save($atlas,[Drawing.Imaging.ImageFormat]::Png)
$size=$bitmap.Width
if($size -ne $bitmap.Height){throw 'Atlas must be square'}
$elements=[Collections.Generic.List[object]]::new()
$editor=[Collections.Generic.List[object]]::new()
$groups=[ordered]@{}
function Cube([string]$name,[double[]]$from,[double[]]$to,[int]$tile,[string]$group,[double]$angle=0){
 $column=$tile%4;$row=[math]::Floor($tile/4)
 # Sample safely inside the material cell to avoid generated boundary bleed.
 $uv=@(($column*4+.65),($row*4+.65),($column*4+3.35),($row*4+3.35))
 $faces=[ordered]@{};$bbFaces=[ordered]@{}
 foreach($side in @('north','south','east','west','up','down')){
  $faces[$side]=@{uv=$uv;texture='#0'}
  $bbFaces[$side]=@{uv=@($uv|ForEach-Object{$_*$size/16});texture=0}
 }
 $uuid=[guid]::NewGuid().ToString()
 $center=@((($from[0]+$to[0])/2),(($from[1]+$to[1])/2),(($from[2]+$to[2])/2))
 $cube=[ordered]@{name=$name;from=$from;to=$to;faces=$faces}
 if($angle -ne 0){$cube.rotation=@{origin=$center;axis='z';angle=$angle;rescale=$false}}
 $elements.Add($cube)
 $editor.Add([ordered]@{name=$name;type='cube';uuid=$uuid;from=$from;to=$to;origin=$center;rotation=@(0,0,$angle);inflate=0;shade=$true;box_uv=$false;faces=$bbFaces})
 if(!$groups.Contains($group)){$groups[$group]=[Collections.Generic.List[string]]::new()}
 $groups[$group].Add($uuid)
}
# Broad blade: restrained stepped taper and a strong silver silhouette.
Cube 'Blade / silver body' @(5.5,11,7.45) @(10.5,26.5,8.55) 1 'Blade'
Cube 'Blade / upper taper' @(6,26.5,7.5) @(10,28.5,8.5) 0 'Blade'
Cube 'Blade / point bevel' @(6.7,28.5,7.6) @(9.3,30,8.4) 0 'Blade'
Cube 'Blade / tip' @(7.4,30,7.7) @(8.6,31.4,8.3) 5 'Blade'
Cube 'Blade / left edge' @(5.1,11,7.65) @(5.5,26.5,8.35) 0 'Blade'
Cube 'Blade / right edge' @(10.5,11,7.65) @(10.9,26.5,8.35) 0 'Blade'
Cube 'Blade / shoulder' @(5.8,9.8,7.4) @(10.2,11,8.6) 2 'Blade'
foreach($front in @($true,$false)){
 $z=if($front){8.55}else{7.3}
 Cube "Spine / $front" @(7.45,11.4,$z) @(8.55,27.8,($z+.15)) 2 'Inlays'
 $inlayZ=if($front){$z+.16}else{$z-.06}
 Cube "Energy line / $front" @(7.89,14,$inlayZ) @(8.11,27.1,($inlayZ+.04)) 4 'Inlays'
 Cube "Star facet / $front" @(7.68,12.1,$inlayZ) @(8.32,12.74,($inlayZ+.06)) 5 'Inlays' 45
}
# A single floating-looking octagonal halo at the blade root, not stacked rings.
for($i=0;$i -lt 8;$i++){
 $radians=$i*[math]::PI/4
 $x=8+2.3*[math]::Cos($radians);$y=9.5+2.3*[math]::Sin($radians)
 $angle=if($i%2){if($i -eq 1 -or $i -eq 5){-45}else{45}}else{0}
 $horizontal=($i%2 -eq 1 -or $i -eq 2 -or $i -eq 6)
 $dx=if($horizontal){.95}else{.14};$dy=if($horizontal){.14}else{.95}
 Cube "Halo / segment $i" @(($x-$dx),($y-$dy),8.85) @(($x+$dx),($y+$dy),9.13) 10 'Halo' $angle
}
Cube 'Crossguard / navy' @(2.5,6.7,7.35) @(13.5,7.5,8.65) 2 'Guard'
Cube 'Crossguard / silver upper lip' @(2.5,7.5,7.6) @(13.5,7.72,8.4) 0 'Guard'
Cube 'Crossguard / left end' @(2.1,6.55,7.4) @(2.7,7.8,8.6) 1 'Guard'
Cube 'Crossguard / right end' @(13.3,6.55,7.4) @(13.9,7.8,8.6) 1 'Guard'
Cube 'Core / silver housing' @(7.15,8.65,8.9) @(8.85,10.35,9.2) 1 'Halo' 45
Cube 'Core / cyan diamond' @(7.48,8.98,9.21) @(8.52,10.02,9.38) 4 'Halo' 45
Cube 'Core / white glint' @(7.82,9.32,9.39) @(8.18,9.68,9.44) 5 'Halo' 45
Cube 'Grip / leather' @(7.35,1.65,7.35) @(8.65,6.7,8.65) 8 'Hilt'
foreach($y in @(2.2,3.3,4.4,5.5)){Cube "Grip binding / $y" @(7.29,$y,7.29) @(8.71,($y+.16),8.71) 9 'Hilt'}
Cube 'Pommel / silver' @(6.95,.45,7) @(9.05,1.65,9) 1 'Hilt'
Cube 'Pommel / star inset' @(7.65,.8,9.01) @(8.35,1.5,9.15) 4 'Hilt' 45
$display=@{
 thirdperson_righthand=@{rotation=@(0,-90,55);translation=@(0,4,.5);scale=@(.7,.7,.7)}
 thirdperson_lefthand=@{rotation=@(0,90,-55);translation=@(0,4,.5);scale=@(.7,.7,.7)}
 firstperson_righthand=@{rotation=@(0,-90,25);translation=@(1.13,2.7,1.13);scale=@(.58,.58,.58)}
 firstperson_lefthand=@{rotation=@(0,90,-25);translation=@(1.13,2.7,1.13);scale=@(.58,.58,.58)}
 gui=@{rotation=@(0,0,-35);translation=@(0,-2,0);scale=@(.49,.49,.49)}
 ground=@{rotation=@(0,0,0);translation=@(0,2,0);scale=@(.4,.4,.4)}
 fixed=@{rotation=@(0,180,0);translation=@(0,-2,0);scale=@(.5,.5,.5)}
}
$model=[ordered]@{credit='Stellar Greatsword v2 - minimal astral silhouette';textures=@{'0'='tutorial_mod:item/stellar_greatsword_v2';particle='tutorial_mod:item/stellar_greatsword_v2'};elements=@($elements.ToArray());display=$display;gui_light='front'}
$outliner=@(foreach($name in $groups.Keys){@{name=$name;uuid=[guid]::NewGuid().ToString();origin=@(8,8,8);children=@($groups[$name].ToArray());export=$true;isOpen=$true}})
$bb=[ordered]@{meta=@{format_version='4.10';model_format='java_block';box_uv=$false};name='Stellar Greatsword v2';model_identifier='ice_sword';visible_box=@(1,2,1);resolution=@{width=$size;height=$size};elements=@($editor.ToArray());outliner=$outliner;display=$display;textures=@(@{name='stellar_greatsword_v2.png';uuid=[guid]::NewGuid().ToString();id='0';width=$size;height=$size;uv_width=$size;uv_height=$size;namespace='tutorial_mod';folder='item';mode='bitmap';saved=$true;source='data:image/png;base64,'+[Convert]::ToBase64String([IO.File]::ReadAllBytes($atlas))})}
$model|ConvertTo-Json -Depth 30|Set-Content -LiteralPath (Join-Path $output 'ice_sword.json') -Encoding utf8
$bb|ConvertTo-Json -Depth 30|Set-Content -LiteralPath (Join-Path $output 'stellar_greatsword_v2.bbmodel') -Encoding utf8
$liveModel=Join-Path $root 'src/main/resources/assets/tutorial_mod/models/item/ice_sword.json'
$backup=Join-Path $output 'ice_sword_previous.json'
if(!(Test-Path $backup)){Copy-Item -LiteralPath $liveModel -Destination $backup}
Copy-Item -LiteralPath (Join-Path $output 'ice_sword.json') -Destination $liveModel -Force
Copy-Item -LiteralPath $atlas -Destination (Join-Path $root 'src/main/resources/assets/tutorial_mod/textures/item/stellar_greatsword_v2.png') -Force
# Front-view render of actual model geometry; no concept-art substitute.
$preview=[Drawing.Bitmap]::new(900,1000);$g=[Drawing.Graphics]::FromImage($preview)
$g.Clear([Drawing.Color]::FromArgb(16,21,34));$g.SmoothingMode='AntiAlias'
foreach($cube in ($elements|Sort-Object {$_.from[2]})){
 $tileUv=$cube.faces.south.uv
 $c=$bitmap.GetPixel([int](($tileUv[0]+$tileUv[2])*$size/32),[int](($tileUv[1]+$tileUv[3])*$size/32))
 $x1=$cube.from[0];$x2=$cube.to[0];$y1=$cube.from[1];$y2=$cube.to[1];$cx=($x1+$x2)/2;$cy=($y1+$y2)/2
 $angle=if($cube.Contains('rotation')){$cube.rotation.angle*[math]::PI/180}else{0}
 $points=[Collections.Generic.List[Drawing.PointF]]::new()
 foreach($point in @(@($x1,$y1),@($x2,$y1),@($x2,$y2),@($x1,$y2))){
  $dx=$point[0]-$cx;$dy=$point[1]-$cy
  $px=$cx+$dx*[math]::Cos($angle)-$dy*[math]::Sin($angle)
  $py=$cy+$dx*[math]::Sin($angle)+$dy*[math]::Cos($angle)
  $points.Add([Drawing.PointF]::new([single](450+($px-8)*25),[single](940-$py*27)))
 }
 $brush=[Drawing.SolidBrush]::new($c);$g.FillPolygon($brush,$points.ToArray());$brush.Dispose()
}
$g.Dispose();$preview.Save((Join-Path $output 'model_front_preview.png'),[Drawing.Imaging.ImageFormat]::Png);$preview.Dispose();$bitmap.Dispose()
Write-Output "Built $($elements.Count) cubes, embedded atlas, $($groups.Count) editing groups: $output"
