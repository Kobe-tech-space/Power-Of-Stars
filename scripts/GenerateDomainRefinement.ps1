$ErrorActionPreference='Stop'
$projectRoot=Split-Path $PSScriptRoot -Parent
$resources=Join-Path $projectRoot 'src/main/resources'
$assetRoot=Join-Path $resources 'assets/tutorial_mod'
$encoding=[Text.UTF8Encoding]::new($false)
$spriteDir=Join-Path $assetRoot 'textures/mob_effect'
New-Item -ItemType Directory -Force -Path $spriteDir|Out-Null
Copy-Item -LiteralPath (Join-Path $assetRoot 'textures/item/star_singularity.png') -Destination (Join-Path $spriteDir 'army_breaker.png')
$gold=Join-Path $projectRoot 'docs/art/dominion_generated.png'
if(!(Test-Path -LiteralPath $gold)) {
 Copy-Item -LiteralPath 'C:\Users\Zx\.codex\generated_images\01a12e36-3e91-71a0-b418-02ff22ce2f15\exec-afc26d8f-81d0-4b48-baef-931932699f4b.png' -Destination $gold
}
Add-Type -AssemblyName System.Drawing
$sourceImage=[Drawing.Bitmap]::new($gold)
$target=[Drawing.Bitmap]::new(64,64,[Drawing.Imaging.PixelFormat]::Format32bppArgb)
$graphics=[Drawing.Graphics]::FromImage($target)
$graphics.InterpolationMode=[Drawing.Drawing2D.InterpolationMode]::NearestNeighbor
$graphics.PixelOffsetMode=[Drawing.Drawing2D.PixelOffsetMode]::Half
$graphics.CompositingMode=[Drawing.Drawing2D.CompositingMode]::SourceCopy
$graphics.DrawImage($sourceImage,[Drawing.Rectangle]::new(0,0,64,64),0,0,$sourceImage.Width,$sourceImage.Height,[Drawing.GraphicsUnit]::Pixel)
$target.Save((Join-Path $spriteDir 'dominion.png'),[Drawing.Imaging.ImageFormat]::Png)
$graphics.Dispose();$target.Dispose();$sourceImage.Dispose()
$translations=@{
 'effect.tutorial_mod.army_breaker'=@('破军','Army Breaker')
 'effect.tutorial_mod.dominion'=@('统御','Dominion')
 'hud.tutorial_mod.panel_title'=@('星环装备','Astral Gear')
 'hud.tutorial_mod.compact_energy'=@('星能','ENERGY')
 'hud.tutorial_mod.compact_shield'=@('星能盾','SHIELD')
 'hud.tutorial_mod.compact_barrier'=@('守护屏障','BARRIER')
 'hud.tutorial_mod.compact_mode_melee'=@('近战 · 破军','Melee · Breaker')
 'hud.tutorial_mod.compact_mode_ranged'=@('剑气 · 统御','Wave · Dominion')
 'hud_editor.tutorial_mod.widget_panel'=@('统一状态面板','Unified status panel')
 'hud.tutorial_mod.domain_no_energy'=@('%s · 需要1000星能','%s · needs 1000 energy')
 'tooltip.tutorial_mod.domain'=@('J：高阶全套消耗1000星能释放破军／统御','J: ascended full set spends 1000 energy on a domain')
 'message.tutorial_mod.break_domain'=@('破军领域展开','Army Breaker activated')
 'message.tutorial_mod.control_domain'=@('统御领域展开','Dominion activated')
}
foreach($language in @('zh_cn','en_us')) {
 $index=if($language -eq 'zh_cn'){0}else{1}
 $langFile=Join-Path $assetRoot "lang/$language.json"
 $lang=Get-Content -Raw $langFile|ConvertFrom-Json -AsHashtable
 foreach($key in $translations.Keys){$lang[$key]=$translations[$key][$index]}
 [IO.File]::WriteAllText($langFile,($lang|ConvertTo-Json -Depth 15),$encoding)
 $bookRoot=Join-Path $assetRoot "patchouli_books/astral_guide/$language/entries"
 $domainFile=Join-Path $bookRoot 'combat/domains.json'
 $domain=Get-Content -Raw $domainFile|ConvertFrom-Json -AsHashtable
 foreach($page in $domain.pages) {
  if($page.text){$page.text=$page.text -replace '消耗当前全部星能，至少需要1点','固定消耗1000星能' -replace 'spends all current energy \(at least 1\)','spends exactly 1000 energy' -replace 'HARMFUL类负面状态','负面与中性不利状态（含黑暗、失明、凶兆）' -replace 'removes/denies harmful status effects','removes/denies harmful and neutral effects, including Darkness, Blindness and Bad Omen' -replace '\$\(br2\)持续状态显示为药水图标.*$','' -replace '\$\(br2\)Active domains use potion icons.*$',''}
 }
 $domain.pages=@($domain.pages|Where-Object {$_.title -notin @('状态与兼容','Status and Compatibility')})
 $domain.pages+=@{type='patchouli:text';title=@('状态与兼容','Status and Compatibility')[$index];text=@('持续状态显示为药水图标：破军为蓝紫奇点，统御为金色奇点；HUD不显示领域时间倒计时。$(br2)负面免疫仅在统御10秒生效期间有效，并非盔甲永久免疫。领域结束后，监守者仍能重新赋予黑暗。喝奶或脱装会结束领域，冷却不会清除。$(br2)检测到Apothic Attributes后，关闭本模组的超额护甲补偿，避免重复减伤；实际伤害使用它的护甲/保护公式。',
 'Domains use potion icons: blue singularity for Army Breaker, gold for Dominion. No separate HUD duration timer. Dominion immunity only lasts during its 10-second status, not permanently. Darkness can return afterwards. Milk/unequipping ends the domain but never resets cooldowns. Apothic Attributes disables our legacy extra-armor compensation; its armor/protection formulas determine actual damage.')[$index]}
 [IO.File]::WriteAllText($domainFile,($domain|ConvertTo-Json -Depth 20),$encoding)
 $eliteFile=Join-Path $bookRoot 'world/elites.json'
 $elite=Get-Content -Raw $eliteFile|ConvertFrom-Json -AsHashtable
 $elite.pages=@(@{type='patchouli:text';text=@('敌对生物保留3%的精英概率，蚀星之夜提高概率。自动强化跳过已有命名怪及生命≥200的Boss。精英生命最高增至6倍（基础总量上限500），额外12护甲、8韧性、35%击退抗性；攻击力与移动速度强化。$(br2)红色狂暴：半血时力量与速度爆发。青色星盾：80吸收盾，脱战5秒后每6秒回10。金色迅捷：每4秒碰撞检测突进。紫色引力：10格内牵引，每6秒伤害与缓速脉冲。击杀掉落3～6星锭。',
 'Hostiles retain a 3% elite chance (higher during Eclipse). Existing named mobs and bosses with health >=200 are excluded. Elites gain up to x6 health (500 baseline total cap), +12 armor, +8 toughness, +35% knockback resistance and offense/speed boosts. Berserker bursts below half health; Shielded has 80 absorption and out-of-combat regeneration; Swift lunges every 4s with collision checks; Gravity pulls within 10 blocks and pulses every 6s. Drops: 3-6 star ingots.')[$index]})
 [IO.File]::WriteAllText($eliteFile,($elite|ConvertTo-Json -Depth 20),$encoding)
 $controlsFile=Join-Path $bookRoot 'reference/controls.json'
 $controls=Get-Content -Raw $controlsFile|ConvertFrom-Json -AsHashtable
 $controls.pages[0].text=@('J：高阶全套消耗1000星能施放领域。G：150星能重铸50星能盾。K：编辑紧凑HUD。右键重剑切换破军近战／统御剑气。$(br2)默认HUD统一为左下小面板，显示模式、星能、星能盾、守护屏障和重剑等级。领域状态使用原版药水图标，不显示单独倒计时。暴击保留伤害与资源计算，关闭本模组的暴击浮字、粒子、音效和特殊剑气外观。',
 'J: ascended full set spends 1000 energy on a domain. G: 150 energy for 50 shield. K: edit compact HUD. Right-click switches sword mode. Default HUD: one small bottom-left panel with mode, energy, shields and mastery level. Domains use potion icons without a separate timer. Critical calculations remain; custom crit numbers, sounds, particles and special wave appearance are disabled.')[$index]
 [IO.File]::WriteAllText($controlsFile,($controls|ConvertTo-Json -Depth 20),$encoding)
}
Write-Output 'Domain status icons, compact HUD text and guide refinements updated.'
if (Test-Path (Join-Path $PSScriptRoot 'GenerateFeatures21To32.ps1')) { & (Join-Path $PSScriptRoot 'GenerateFeatures21To32.ps1') }
