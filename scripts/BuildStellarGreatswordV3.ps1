param([Parameter(Mandatory=$true)][string]$TexturePath)
$ErrorActionPreference='Stop'
Add-Type -AssemblyName System.Drawing
$root=Split-Path -Parent $PSScriptRoot
$out=Join-Path $root 'docs/art/stellar_greatsword_v3'
[IO.Directory]::CreateDirectory($out)|Out-Null
Copy-Item -LiteralPath $TexturePath -Destination (Join-Path $out 'original_generated_texture.png') -Force
$source=[Drawing.Bitmap]::new($TexturePath)
$n=256;$textureSize=1024;$step=32.0/$n
$texture=[Drawing.Bitmap]::new($textureSize,$textureSize,[Drawing.Imaging.PixelFormat]::Format32bppArgb)
$g=[Drawing.Graphics]::FromImage($texture)
$g.InterpolationMode='NearestNeighbor';$g.PixelOffsetMode='Half';$g.CompositingMode='SourceCopy'
$g.DrawImage($source,[Drawing.Rectangle]::new(0,0,$textureSize,$textureSize),0,0,$source.Width,$source.Height,[Drawing.GraphicsUnit]::Pixel)
$g.Dispose();$source.Dispose()
$png=Join-Path $out 'stellar_greatsword_v3.png'
$texture.Save($png,[Drawing.Imaging.ImageFormat]::Png)
# Construct only the left half and mirror its geometry AND UV mapping to the right.
# This guarantees exact structural and visible symmetry without painting over the AI texture.
$mask=[int[,]]::new($n,$n/2);$used=[bool[,]]::new($n,$n/2)
$solid=0
for($y=0;$y -lt $n;$y++){for($x=0;$x -lt $n/2;$x++){
 if($texture.GetPixel([int](($x+.5)*$textureSize/$n),[int](($y+.5)*$textureSize/$n)).A -lt 180){continue}
 $solid++
 $mask[$y,$x]=if($y -lt 174){if($x -gt 120){3}else{1}}elseif($y -lt 211){if($x -gt 113){4}else{2}}else{3}
}}
if($solid -lt 500){throw 'Generated texture has insufficient opaque geometry'}
$modelCubes=[Collections.Generic.List[object]]::new();$bbCubes=[Collections.Generic.List[object]]::new();$groups=[ordered]@{}
$pair=0
for($y=0;$y -lt $n;$y++){for($x=0;$x -lt $n/2;$x++){
 $depthKey=$mask[$y,$x];if($depthKey -eq 0 -or $used[$y,$x]){continue}
 $w=1;while($x+$w -lt $n/2 -and !$used[$y,($x+$w)] -and $mask[$y,($x+$w)] -eq $depthKey){$w++}
 $h=1
 :grow while($y+$h -lt $n){for($k=0;$k -lt $w;$k++){if($used[($y+$h),($x+$k)] -or $mask[($y+$h),($x+$k)] -ne $depthKey){break grow}};$h++}
 for($j=0;$j -lt $h;$j++){for($k=0;$k -lt $w;$k++){$used[($y+$j),($x+$k)]=$true}}
 $depth=@{1=.45;2=.65;3=.85;4=1.2}[$depthKey]
 $group=if($y -lt 174){'Blade & star inlays'}elseif($y -lt 211){'Twin star wings & orbits'}else{'Grip & celestial pommel'}
 if(!$groups.Contains($group)){$groups[$group]=[Collections.Generic.List[string]]::new()}
 foreach($mirror in @($false,$true)){
  $worldX=if($mirror){$n-$x-$w}else{$x}
  $from=@((8+($worldX-$n/2)*$step),(31.5-($y+$h)*$step),(8-$depth/2))
  $to=@((8+($worldX+$w-$n/2)*$step),(31.5-$y*$step),(8+$depth/2))
  # Center and normalize the 32-unit vertical authoring canvas to vanilla's
  # 16-unit diagonal sword canvas before applying the shared -45 degree axis.
  foreach($point in @($from,$to)) {
   $point[0]=8+($point[0]-8)*.66
   $point[1]=8+($point[1]-15.5)*.66
   $point[2]=8+($point[2]-8)*.66
  }
  $uv=if($mirror){@((($x+$w)*16/$n),($y*16/$n),($x*16/$n),(($y+$h)*16/$n))}else{@(($x*16/$n),($y*16/$n),(($x+$w)*16/$n),(($y+$h)*16/$n))}
  $north=@($uv[2],$uv[1],$uv[0],$uv[3])
  $edgeUv=@(($x*16/$n),($y*16/$n),(($x+1)*16/$n),(($y+1)*16/$n))
  $faces=[ordered]@{};$bbFaces=[ordered]@{}
  foreach($side in @('north','south','east','west','up','down')){
   $sideUv=if($side -eq 'south'){$uv}elseif($side -eq 'north'){$north}else{$edgeUv}
   $faces[$side]=@{uv=$sideUv;texture='#0'}
   $bbFaces[$side]=@{uv=@($sideUv|ForEach-Object{$_*$textureSize/16});texture=0}
  }
  $name="Pair $pair / "+$(if($mirror){'R'}else{'L'})
  $uuid=[guid]::NewGuid().ToString()
  # Vanilla sword artwork runs bottom-left to top-right. Match that local axis,
  # then use the unmodified vanilla handheld transforms below.
  $modelCubes.Add(@{name=$name;from=$from;to=$to;faces=$faces;shade=$true;rotation=@{origin=@(8,8,8);axis='z';angle=-45;rescale=$false}})
  $bbCubes.Add(@{name=$name;type='cube';uuid=$uuid;from=$from;to=$to;origin=@(8,8,8);rotation=@(0,0,-45);inflate=0;box_uv=$false;shade=$true;faces=$bbFaces})
  $groups[$group].Add($uuid)
 }
 $pair++
}}
$display=@{
 thirdperson_righthand=@{rotation=@(0,-90,55);translation=@(0,4,.5);scale=@(.85,.85,.85)}
 thirdperson_lefthand=@{rotation=@(0,90,-55);translation=@(0,4,.5);scale=@(.85,.85,.85)}
 firstperson_righthand=@{rotation=@(0,-90,25);translation=@(1.13,3.2,1.13);scale=@(.68,.68,.68)}
 firstperson_lefthand=@{rotation=@(0,90,-25);translation=@(1.13,3.2,1.13);scale=@(.68,.68,.68)}
 gui=@{rotation=@(0,0,10);translation=@(0,0,0);scale=@(.75,.75,.75)}
 ground=@{rotation=@(0,0,0);translation=@(0,2,0);scale=@(.4,.4,.4)}
 fixed=@{rotation=@(0,180,0);translation=@(0,-1,0);scale=@(.5,.5,.5)}
}
$model=@{credit='Stellar Sovereign / exact mirrored geometry and UV';textures=@{'0'='tutorial_mod:item/stellar_greatsword_v3';particle='tutorial_mod:item/stellar_greatsword_v3'};elements=@($modelCubes.ToArray());display=$display;gui_light='front'}
$outliner=@(foreach($group in $groups.Keys){@{name=$group;uuid=[guid]::NewGuid().ToString();origin=@(8,8,8);children=@($groups[$group].ToArray());export=$true;isOpen=$true}})
$bb=@{meta=@{format_version='4.10';model_format='java_block';box_uv=$false};name='Stellar Sovereign Greatsword';model_identifier='ice_sword';visible_box=@(2,2,2);resolution=@{width=$textureSize;height=$textureSize};elements=@($bbCubes.ToArray());outliner=$outliner;display=$display;textures=@(@{name='stellar_greatsword_v3.png';uuid=[guid]::NewGuid().ToString();id='0';width=$textureSize;height=$textureSize;uv_width=$textureSize;uv_height=$textureSize;namespace='tutorial_mod';folder='item';mode='bitmap';saved=$true;source='data:image/png;base64,'+[Convert]::ToBase64String([IO.File]::ReadAllBytes($png))})}
$model|ConvertTo-Json -Depth 30|Set-Content -LiteralPath (Join-Path $out 'ice_sword.json') -Encoding utf8
$bb|ConvertTo-Json -Depth 30|Set-Content -LiteralPath (Join-Path $out 'stellar_sovereign.bbmodel') -Encoding utf8
Copy-Item -LiteralPath (Join-Path $out 'ice_sword.json') -Destination (Join-Path $root 'src/main/resources/assets/tutorial_mod/models/item/ice_sword.json') -Force
Copy-Item -LiteralPath $png -Destination (Join-Path $root 'src/main/resources/assets/tutorial_mod/textures/item/stellar_greatsword_v3.png') -Force
# Render the actual generated front geometry with its exact mirrored UVs.
$preview=[Drawing.Bitmap]::new(1024,1152)
$painter=[Drawing.Graphics]::FromImage($preview)
$painter.Clear([Drawing.Color]::FromArgb(14,19,33))
$painter.InterpolationMode='NearestNeighbor';$painter.PixelOffsetMode='Half'
foreach($cube in $modelCubes){
 $uv=$cube.faces.south.uv
 $x1=512+($cube.from[0]-8)*32;$x2=512+($cube.to[0]-8)*32
 $top=1088-$cube.to[1]*32;$bottom=1088-$cube.from[1]*32
 $points=if($uv[0] -gt $uv[2]){[Drawing.PointF[]]@([Drawing.PointF]::new($x2,$top),[Drawing.PointF]::new($x1,$top),[Drawing.PointF]::new($x2,$bottom))}else{[Drawing.PointF[]]@([Drawing.PointF]::new($x1,$top),[Drawing.PointF]::new($x2,$top),[Drawing.PointF]::new($x1,$bottom))}
 $sx=[math]::Min($uv[0],$uv[2])*$textureSize/16;$sy=$uv[1]*$textureSize/16
 $sw=[math]::Abs($uv[2]-$uv[0])*$textureSize/16;$sh=($uv[3]-$uv[1])*$textureSize/16
 $painter.DrawImage($texture,[Drawing.PointF[]]$points,[Drawing.RectangleF]::new($sx,$sy,$sw,$sh),[Drawing.GraphicsUnit]::Pixel)
}
$painter.Dispose();$preview.Save((Join-Path $out 'model_front_render.png'),[Drawing.Imaging.ImageFormat]::Png);$preview.Dispose()
$texture.Dispose()
Write-Output "Symmetric model: $($modelCubes.Count) cubes / $pair exact mirror pairs; atlas ${textureSize}x${textureSize}; $out"
if(Test-Path -LiteralPath (Join-Path $PSScriptRoot 'RefineStellarSwordTip.ps1')) {
 & (Join-Path $PSScriptRoot 'RefineStellarSwordTip.ps1')
}
