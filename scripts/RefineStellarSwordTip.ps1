$ErrorActionPreference='Stop'
$root=Split-Path -Parent $PSScriptRoot
$base=Join-Path $root 'docs/art/stellar_greatsword_v3'
$out=Join-Path $root 'docs/art/stellar_sharp_tip'
[IO.Directory]::CreateDirectory($out)|Out-Null
$original=Get-Content -LiteralPath (Join-Path $base 'ice_sword.json') -Raw|ConvertFrom-Json -AsHashtable
$bb=Get-Content -LiteralPath (Join-Path $base 'stellar_sovereign.bbmodel') -Raw|ConvertFrom-Json -AsHashtable
$cutRow=96;$cut=18.56-$cutRow*.0825
$body=[Collections.Generic.List[object]]::new();$editor=[Collections.Generic.List[object]]::new()
for($i=0;$i -lt $original.elements.Count;$i++){
 $cube=$original.elements[$i];$edit=$bb.elements[$i]
 if($cube.from[1] -ge $cut){continue}
 if($cube.to[1] -gt $cut){
  $cube.to[1]=$cut;$edit.to[1]=$cut
  foreach($side in @('north','south')){$cube.faces[$side].uv[1]=$cutRow/16.0;$edit.faces[$side].uv[1]=$cutRow*4}
 }
 $body.Add($cube);$editor.Add($edit)
}
# Continuous four-facet diamond-section blade, ending in ONE exact shared point.
# Its front spine retains the original blue/star texture, while its edges use silver.
$vertices=[ordered]@{};$faces=[Collections.Generic.List[object]]::new()
function V([string]$key,[double]$x,[double]$y,[double]$z){$vertices[$key]=@((($x-128)*.0825),(10.56-$y*.0825),($z*.66))}
V 'point' 128 0 0
foreach($station in @(@('a',32,14,.42),@('b',40,12,.42),@('c',96,14,.42))){
 $prefix=$station[0];$y=$station[1];$w=$station[2];$depth=$station[3]
 V ($prefix+'L') (128-$w) $y 0;V ($prefix+'F') 128 $y $depth
 V ($prefix+'R') (128+$w) $y 0;V ($prefix+'B') 128 $y (-$depth)
}
function Face([string[]]$keys,[string]$material){$faces.Add(@{vertices=$keys;material=$material})}
foreach($edge in @(@('L','F'),@('F','R'),@('R','B'),@('B','L'))){Face @('point',('a'+$edge[0]),('a'+$edge[1])) 'silver'}
foreach($pair in @(@('a','b'),@('b','c'))){foreach($edge in @(@('L','F'),@('F','R'),@('R','B'),@('B','L'))){Face @(($pair[0]+$edge[0]),($pair[1]+$edge[0]),($pair[1]+$edge[1]),($pair[0]+$edge[1])) 'blade'}}
Face @('cL','cB','cR','cF') 'silver'
# A narrow blue inlay on each front/back facet follows the tapered point.
foreach($back in @($false,$true)){
 $sign=if($back){-1}else{1};$tag=if($back){'back'}else{'front'}
 V ($tag+'Top') 128 13 ($sign*.18)
 V ($tag+'Left') 124.5 96 ($sign*.335)
 V ($tag+'Right') 131.5 96 ($sign*.335)
 Face @(($tag+'Top'),($tag+'Left'),($tag+'Right')) 'inlay'
}
$obj=[Collections.Generic.List[string]]::new();$obj.Add('mtllib stellar_sharp_tip.mtl');$obj.Add('o continuous_sharp_tip')
$indices=@{};$index=0
foreach($key in $vertices.Keys){
 $index++;$indices[$key]=$index;$p=$vertices[$key]
 # OBJ coordinates are measured in blocks. Apply the same vanilla diagonal axis.
 $x=8+($p[0]+$p[1])/[math]::Sqrt(2);$y=8+(-$p[0]+$p[1])/[math]::Sqrt(2);$z=8+$p[2]
 $obj.Add(('v {0} {1} {2}' -f ($x/16).ToString('R',[cultureinfo]::InvariantCulture),($y/16).ToString('R',[cultureinfo]::InvariantCulture),($z/16).ToString('R',[cultureinfo]::InvariantCulture)))
}
$meshFaces=[ordered]@{};$texIndex=0;$faceIndex=0
foreach($face in $faces){
 $obj.Add('usemtl '+$face.material);$uv=[ordered]@{};$refs=[Collections.Generic.List[string]]::new()
 foreach($key in $face.vertices){
  $p=$vertices[$key];$px=128+$p[0]/.0825;$py=(10.56-$p[1])/.0825
  if($face.material -eq 'silver'){$u=1.8/16;$v=1.8/16;$uv[$key]=@(57.6,57.6)}
  else{$u=[math]::Min($px,256-$px)/256;$v=$py/256;$uv[$key]=@(($u*1024),($v*1024))}
  $texIndex++;$obj.Add(('vt {0} {1}' -f $u.ToString('R',[cultureinfo]::InvariantCulture),(1-$v).ToString('R',[cultureinfo]::InvariantCulture)))
  $refs.Add("$($indices[$key])/$texIndex")
 }
 $obj.Add('f '+($refs -join ' '))
 # Reverse face too: preserves exact sharp edges without dependence on winding.
 $reverse=@($refs.ToArray());[array]::Reverse($reverse);$obj.Add('f '+($reverse -join ' '))
 $meshFaces["face$faceIndex"]=@{vertices=$face.vertices;uv=$uv;texture=$(if($face.material -eq 'silver'){1}else{0})};$faceIndex++
}
$obj|Set-Content -LiteralPath (Join-Path $out 'stellar_sharp_tip.obj') -Encoding utf8
@('newmtl silver','Kd 1 1 1','map_Kd #1','newmtl blade','Kd 1 1 1','map_Kd #0','newmtl inlay','Kd 1 1 1','map_Kd #0')|Set-Content -LiteralPath (Join-Path $out 'stellar_sharp_tip.mtl') -Encoding utf8
$textures=@{'0'='tutorial_mod:item/stellar_greatsword_v3';'1'='tutorial_mod:item/stellar_greatsword_v2';particle='tutorial_mod:item/stellar_greatsword_v3'}
$model=@{loader='forge:composite';textures=$textures;gui_light='front';display=$original.display;children=[ordered]@{body=@{textures=$textures;elements=@($body.ToArray());display=$original.display;gui_light='front'};tip=@{loader='forge:obj';model='tutorial_mod:models/item/stellar_sharp_tip.obj';flip_v=$true;automatic_culling=$false;shade_quads=$true;textures=$textures;display=$original.display;gui_light='front'}}}
$model|ConvertTo-Json -Depth 40|Set-Content -LiteralPath (Join-Path $out 'ice_sword.json') -Encoding utf8
$meshId=[guid]::NewGuid().ToString()
$editor.Add(@{type='mesh';name='Continuous sharp diamond tip';uuid=$meshId;origin=@(8,8,8);rotation=@(0,0,-45);vertices=$vertices;faces=$meshFaces})
$bb.meta.model_format='free';$bb.elements=@($editor.ToArray());$alive=@($editor|ForEach-Object {$_.uuid})
foreach($group in $bb.outliner){$group.children=@($group.children|Where-Object {$_ -in $alive})}
$bb.outliner+=@{name='Sharp tip mesh';uuid=[guid]::NewGuid().ToString();origin=@(8,8,8);children=@($meshId);export=$true}
$atlas=Join-Path $root 'src/main/resources/assets/tutorial_mod/textures/item/stellar_greatsword_v2.png'
$bb.textures+=@{name='stellar_greatsword_v2.png';uuid=[guid]::NewGuid().ToString();id='1';width=128;height=128;uv_width=1024;uv_height=1024;mode='bitmap';source='data:image/png;base64,'+[Convert]::ToBase64String([IO.File]::ReadAllBytes($atlas))}
$bb|ConvertTo-Json -Depth 40|Set-Content -LiteralPath (Join-Path $out 'stellar_sovereign_sharp.bbmodel') -Encoding utf8
$live=Join-Path $root 'src/main/resources/assets/tutorial_mod/models/item'
Copy-Item -LiteralPath (Join-Path $out 'ice_sword.json'),(Join-Path $out 'stellar_sharp_tip.obj'),(Join-Path $out 'stellar_sharp_tip.mtl') -Destination $live -Force
Write-Output "Sharp tip: $($body.Count) retained body cubes, $($vertices.Count) continuous mesh vertices / $($faces.Count) faces"
