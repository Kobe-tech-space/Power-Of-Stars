$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path $PSScriptRoot -Parent
$resources = Join-Path $projectRoot 'src/main/resources'
$encoding = [Text.UTF8Encoding]::new($false)
function Write-Resource([string]$relative, $data) {
    $target = Join-Path $resources $relative
    New-Item -ItemType Directory -Force -Path (Split-Path $target) | Out-Null
    [IO.File]::WriteAllText($target, ($data|ConvertTo-Json -Depth 25), $encoding)
}
function Recipe([string]$name, [string[]]$pattern, $key, [string]$result, [int]$count=1) {
    Write-Resource "data/tutorial_mod/recipes/$name.json" @{type='minecraft:crafting_shaped';pattern=$pattern;key=$key;result=@{item=$result;count=$count}}
}
Recipe 'star_ingot_block' @('SSS','SSS','SSS') @{S=@{item='tutorial_mod:star_ingot'}} 'tutorial_mod:star_ingot_block'
Write-Resource 'data/tutorial_mod/recipes/star_ingots_from_block.json' @{type='minecraft:crafting_shapeless';ingredients=@(@{item='tutorial_mod:star_ingot_block'});result=@{item='tutorial_mod:star_ingot';count=9}}
Recipe 'star_singularity' @('NDN','BSB','NDN') @{N=@{item='minecraft:netherite_ingot'};D=@{item='minecraft:diamond_block'};B=@{item='tutorial_mod:star_ingot_block'};S=@{item='minecraft:nether_star'}} 'tutorial_mod:star_singularity'
Recipe 'star_upgrade_template' @('DND','BTB','DND') @{N=@{item='minecraft:netherite_ingot'};D=@{item='minecraft:diamond_block'};B=@{item='tutorial_mod:star_ingot_block'};T=@{item='minecraft:netherite_upgrade_smithing_template'}} 'tutorial_mod:star_upgrade_template'
foreach ($part in @('helmet','chestplate','leggings','boots')) {
    Write-Resource "data/tutorial_mod/recipes/cosmic_$part.json" @{type='minecraft:smithing_transform';template=@{item='tutorial_mod:star_upgrade_template'};base=@{item="tutorial_mod:star_$part"};addition=@{item='tutorial_mod:star_singularity'};result=@{item="tutorial_mod:cosmic_$part"}}
}
Write-Resource 'assets/tutorial_mod/blockstates/star_ingot_block.json' @{variants=@{''=@{model='tutorial_mod:block/star_ingot_block'}}}
Write-Resource 'assets/tutorial_mod/models/block/star_ingot_block.json' @{parent='minecraft:block/cube_all';textures=@{all='minecraft:block/amethyst_block'}}
Write-Resource 'assets/tutorial_mod/models/item/star_ingot_block.json' @{parent='tutorial_mod:block/star_ingot_block'}
Write-Resource 'data/tutorial_mod/loot_tables/blocks/star_ingot_block.json' @{type='minecraft:block';pools=@(@{rolls=1;entries=@(@{type='minecraft:item';name='tutorial_mod:star_ingot_block'});conditions=@(@{condition='minecraft:survives_explosion'})})}
foreach ($path in @('mineable/pickaxe','needs_iron_tool')) {
    $file = Join-Path $resources "data/minecraft/tags/blocks/$path.json"
    $tag = Get-Content -Raw $file|ConvertFrom-Json -AsHashtable
    if ($tag.values -notcontains 'tutorial_mod:star_ingot_block') { $tag.values += 'tutorial_mod:star_ingot_block' }
    Write-Resource "data/minecraft/tags/blocks/$path.json" $tag
}
$translations = [ordered]@{
 'item.tutorial_mod.star_helmet'=@('星环头盔·初阶','Star Helmet · Lesser')
 'item.tutorial_mod.star_chestplate'=@('星环胸甲·初阶','Star Chestplate · Lesser')
 'item.tutorial_mod.star_leggings'=@('星环护腿·初阶','Star Leggings · Lesser')
 'item.tutorial_mod.star_boots'=@('星环战靴·初阶','Star Boots · Lesser')
 'item.tutorial_mod.cosmic_helmet'=@('星辰头盔·进阶','Cosmic Helmet · Ascended')
 'item.tutorial_mod.cosmic_chestplate'=@('星辰胸甲·进阶','Cosmic Chestplate · Ascended')
 'item.tutorial_mod.cosmic_leggings'=@('星辰护腿·进阶','Cosmic Leggings · Ascended')
 'item.tutorial_mod.cosmic_boots'=@('星辰战靴·进阶','Cosmic Boots · Ascended')
 'item.tutorial_mod.star_singularity'=@('星辰奇点','Star Singularity')
 'item.tutorial_mod.star_upgrade_template'=@('星辰升级模板','Star Upgrade Smithing Template')
 'block.tutorial_mod.star_ingot_block'=@('星锭块','Star Ingot Block')
 'template.tutorial_mod.applies'=@('初阶星环盔甲','Lesser star armor')
 'template.tutorial_mod.upgrade'=@('星辰升阶','Cosmic Ascension')
 'template.tutorial_mod.base'=@('放入一件初阶星环盔甲','Add one lesser star armor piece')
 'template.tutorial_mod.addition'=@('放入星辰奇点','Add a star singularity')
 'tooltip.tutorial_mod.low_set'=@('同阶全套：护甲100、免伤30%、攻速/增伤+13%','Matching set: 100 armor, 30% immunity, +13% speed/damage')
 'tooltip.tutorial_mod.high_set'=@('同阶全套：护甲250、免伤60%、攻速/增伤+30%','Matching set: 250 armor, 60% immunity, +30% speed/damage')
 'tooltip.tutorial_mod.piece_offense'=@('单件：攻速 +%s、加算增伤 +%s','Per piece: +%s attack speed, +%s additive damage')
 'tooltip.tutorial_mod.star_shield'=@('全套解锁星能；G：150星能重铸50点星能护盾','Full set unlocks energy; G: 150 energy for 50 energy shield')
 'tooltip.tutorial_mod.armor_skill'=@('高阶全套：J释放与重剑模式对应的领域','Ascended full set: J casts the domain of your sword mode')
 'tooltip.tutorial_mod.energy_gain'=@('仅同阶全套获取星能：命中+5，击杀+15，暴击+3','Matching full set gains energy: hit +5, kill +15, crit +3')
 'tooltip.tutorial_mod.domain'=@('J：高阶全套消耗全部星能释放破军／统御','J: ascended full set spends all energy on a domain')
 'key.tutorial_mod.domain'=@('星辰领域（破军／统御）','Star Domain (Break / Control)')
 'hud.tutorial_mod.mode_melee'=@('破军 · 近战挥砍','Army Breaker · Melee')
 'hud.tutorial_mod.mode_ranged'=@('统御 · 远程剑气','Dominion · Sword Wave')
 'tooltip.tutorial_mod.mode_melee'=@('攻击形式：破军（近战挥砍）','Attack mode: Army Breaker (melee)')
 'tooltip.tutorial_mod.mode_ranged'=@('攻击形式：统御（远程剑气）','Attack mode: Dominion (sword wave)')
 'message.tutorial_mod.mode_melee'=@('已切换为破军：近战挥砍','Switched to Army Breaker: melee')
 'message.tutorial_mod.mode_ranged'=@('已切换为统御：远程剑气','Switched to Dominion: sword wave')
 'domain.tutorial_mod.break'=@('破军','Army Breaker')
 'domain.tutorial_mod.control'=@('统御','Dominion')
 'message.tutorial_mod.domain_requires_set'=@('需要穿齐高阶星辰盔甲并主手持有星环重剑','Equip a full ascended set and hold the astral greatsword')
 'message.tutorial_mod.domain_cooldown'=@('领域冷却剩余 %s 秒','Domain ready in %s seconds')
 'message.tutorial_mod.break_domain'=@('破军领域展开：10秒独立增伤与攻速强化','Army Breaker: independent combat multipliers for 10 seconds')
 'message.tutorial_mod.control_domain'=@('统御领域展开：10秒庇护，1000点守护屏障','Dominion: 10 seconds of protection and a 1000-point barrier')
 'hud.tutorial_mod.guardian_barrier'=@('守护屏障 %s / 1000','Guardian Barrier %s / 1000')
 'hud.tutorial_mod.domain_active'=@('%s领域 · 剩余 %ss','%s Domain · %ss remaining')
 'hud.tutorial_mod.domain_cooldown'=@('%s · 冷却 %ss','%s · cooldown %ss')
 'hud.tutorial_mod.domain_no_energy'=@('%s · 至少需要1点星能','%s · needs at least 1 energy')
 'hud.tutorial_mod.domain_ready'=@('%s · [%3$s] 展开领域','%s · [%3$s] cast domain')
 'hud.tutorial_mod.star_energy_hint'=@('全套获取星能 · J领域 / G盾','Full set energy · J / G skills')
 'hud.tutorial_mod.armor_skill_ready'=@('[G] 星能护盾可重铸 · 消耗150','[G] Reforge energy shield · costs 150')
 'hud_editor.tutorial_mod.widget_barrier'=@('守护屏障','Guardian barrier')
 'hud_editor.tutorial_mod.widget_domain'=@('领域状态','Domain status')
 'attribute.name.tutorial_mod.damage_bonus'=@('加算伤害提升','Additive Damage Bonus')
 'attribute.name.tutorial_mod.domain'=@('星辰领域','Star Domain')
 'tooltip.tutorial_mod.branch_star_core'=@('路线：星核 · 星能获取额外+1（容量由套装决定）','Branch: Star Core · +1 energy gained (capacity is set-based)')
 'message.tutorial_mod.branch_star_core'=@('路线切换：星核（额外星能获取+1）','Branch: Star Core (+1 energy gained)')
}
foreach ($language in @('zh_cn','en_us')) {
    $index = if ($language -eq 'zh_cn') {0} else {1}
    $lang = Get-Content -Raw (Join-Path $resources "assets/tutorial_mod/lang/$language.json")|ConvertFrom-Json -AsHashtable
    foreach ($key in $translations.Keys) { $lang[$key]=$translations[$key][$index] }
    # The old paginated booklet is no longer used; remove stale gameplay descriptions.
    foreach ($key in @($lang.Keys)) { if ($key -match '^guide\.tutorial_mod\.page_') { $lang.Remove($key) } }
    $lang['tooltip.tutorial_mod.piece_offense'] = if ($index -eq 0) {'单件：攻速与加算增伤均 +%s'} else {'Per piece: +%s attack speed and additive damage'}
    $lang['hud.tutorial_mod.domain_ready'] = if ($index -eq 0) {'%1$s · [%3$s] 展开领域'} else {'%1$s · [%3$s] cast domain'}
    Write-Resource "assets/tutorial_mod/lang/$language.json" $lang
    $bookRoot = "assets/tutorial_mod/patchouli_books/astral_guide/$language/entries"
    $armorText = if ($index -eq 0) {
        @('初阶：每件20护甲、50韧性、5%免伤、2%攻速、2%加算增伤。全套额外20护甲、10%免伤、5%攻速与增伤，合计100护甲、30%免伤、13%攻速/增伤。$(br2)全套获得摔落缓冲X（80%摔落减伤）、抗性提升I、力量II、急迫II、夜视与熔岩明视。熔岩明视并不防火。',
          '进阶：每件50护甲、50韧性、10%免伤、5%攻速和加算增伤。全套额外50护甲、20%免伤、10%攻速与增伤，合计250护甲、60%免伤、30%攻速/增伤。$(br2)全套免疫摔落，获得抗火、力量X、急迫X、夜视、熔岩明视。混穿无套装效果；两阶均保留头后星环。')
    } else {
        @('Lesser pieces: 20 armor, 50 toughness, 5% immunity, 2% speed and additive damage each. Matching set adds 20 armor, 10% immunity and 5% speed/damage: totals 100 armor, 30% immunity, 13% speed/damage. Full set: 80% fall reduction, Resistance I, Strength II, Haste II, night and lava vision. Lava vision is not fire resistance.',
          'Ascended pieces: 50 armor, 50 toughness, 10% immunity, 5% speed/additive damage each. Matching set adds 50 armor, 20% immunity, 10% speed/damage: totals 250 armor, 60% immunity, 30% speed/damage. Full set: no fall damage, Fire Resistance, Strength X, Haste X, night/lava vision. Mixed tiers only retain individual attributes.')
    }
    Write-Resource "$bookRoot/equipment/armor.json" @{name=@('两阶星环盔甲','Two Armor Tiers')[$index];icon='tutorial_mod:cosmic_chestplate';category='tutorial_mod:equipment';sortnum=4;pages=@(@{type='patchouli:text';text=$armorText[0]},@{type='patchouli:text';text=$armorText[1]})}
    $energyText = @('只有同阶四件套能获取与储存星能。初阶容量250，进阶容量1000；混穿、卸下一件后星能清零。命中+5、击杀+15、暴击+3，星核路线额外+1。$(br2)脱战10秒后每秒衰减1点，相关获取与衰减可配置。共鸣不再增加星能容量。祭坛仅为完整套装补满星能，不再免费补盾。',
                   'Only a matching four-piece set can gain/store energy. Lesser capacity: 250; ascended: 1000. Removing a piece or mixing tiers clears energy. Hit +5, kill +15, crit +3; Star Core adds +1. Decay: 1/second after 10 seconds out of combat. Resonance no longer raises capacity. The altar fills set energy but does not grant a free shield.')[$index]
    Write-Resource "$bookRoot/combat/energy.json" @{name=@('套装星能','Set Energy')[$index];icon='minecraft:experience_bottle';category='tutorial_mod:combat';sortnum=10;pages=@(@{type='patchouli:text';text=$energyText})}
    $shieldText = @('穿齐任一同阶套装，按G消耗150星能将星能护盾重铸为50点，不叠加。护盾不自动回复，脱装或死亡清除。$(br2)统御领域另提供1000点守护屏障；领域结束、换装和离线都不会清除剩余屏障，只有耗尽或死亡才清除。再次释放统御刷新为1000，不叠加。先扣守护屏障，再扣星能护盾，溢出伤害正常扣血。两者不占金苹果吸收生命值。',
                   'Matching full set: G spends 150 energy to reforge a 50-point energy shield (no stacking or passive refill). Removing the set/death clears it. Dominion grants a separate 1000-point guardian barrier. It survives the domain ending, unequipping and logout until depleted or death. Recasting refreshes, never stacks. Barrier absorbs before energy shield; excess damage hits health. Golden-apple absorption remains separate.')[$index]
    Write-Resource "$bookRoot/equipment/shield.json" @{name=@('双层护盾','Two Shield Pools')[$index];icon='minecraft:golden_apple';category='tutorial_mod:equipment';sortnum=6;pages=@(@{type='patchouli:text';text=$shieldText})}
    $domainTexts = if ($index -eq 0) {
        @('高阶四件套且主手持重剑，按J消耗当前全部星能，至少需要1点。创造不消耗资源且无冷却。右键切换模式决定领域；领域锁定发射时模式，换成另一模式不会改变正在生效的领域。$(br2)领域持续10秒，换下高阶全套立即结束，脚下展开放大的星环。两种冷却分别计时，死亡或换维度不能重置。',
          '近战·破军：伤害×1.5、攻速×3、攻击力×3，三项均为独立乘区。持续10秒，冷却300秒。新发射的剑气保存发射时领域与装备加成，领域结束不影响已发射的剑气。',
          '剑气·统御：免疫单次原始伤害大于1的伤害，清除并阻止HARMFUL类负面状态；1点或以下伤害仍可发生，但先由护盾吸收。立即获得1000守护屏障。领域持续10秒，冷却300秒；屏障不会随领域结束而消失。')
    } else {
        @('Wear all ascended pieces and hold the greatsword; J spends all current energy (at least 1). Creative is free and has no cooldown. Right-click selects the domain through sword mode. An active domain keeps its cast-time identity. It lasts 10 seconds; unequipping the ascended set ends it. A large halo appears at your feet. Separate cooldowns survive death/dimension changes.',
          'Melee / Army Breaker: damage x1.5, attack speed x3, attack damage x3, each an independent multiplier. Duration 10 seconds; cooldown 300 seconds. New waves snapshot these launch-time buffs, retaining them after the domain expires.',
          'Ranged / Dominion: rejects incoming raw damage above 1 and removes/denies harmful status effects. Damage of 1 or less still reaches shields. Grants a separate 1000-point guardian barrier. Duration 10 seconds, cooldown 300 seconds; the barrier survives the domain ending.')
    }
    Write-Resource "$bookRoot/combat/domains.json" @{name=@('星辰领域：破军与统御','Star Domains')[$index];icon='tutorial_mod:cosmic_helmet';category='tutorial_mod:combat';sortnum=14;pages=@($domainTexts|ForEach-Object {@{type='patchouli:text';text=$_}})}
    $upgradeTexts = @('锻造台左：星辰升级模板；中：初阶盔甲；右：星辰奇点。每件升阶消耗一个模板和一个奇点，保留附魔、自定义名称与耐久损耗。$(br2)模板不提供廉价复制配方，避免终局装备量产。全套合计消耗4个下界之星、24块下界合金锭、24个钻石块、16个星锭块与4个原版下界合金升级模板。星锭块由9星锭压缩，可还原。',
                     'Smithing: star upgrade template (left), lesser armor (middle), star singularity (right). Each upgrade consumes one of each, preserving enchantments, custom name and damage. No cheap template duplication. A full set costs 4 nether stars, 24 netherite ingots, 24 diamond blocks, 16 star ingot blocks and 4 vanilla netherite upgrade templates. Nine star ingots compress into a reversible block.')[$index]
    Write-Resource "$bookRoot/equipment/ascension.json" @{name=@('昂贵的星辰升阶','Cosmic Ascension')[$index];icon='tutorial_mod:star_singularity';category='tutorial_mod:equipment';sortnum=10;pages=@(@{type='patchouli:text';text=$upgradeTexts},@{type='patchouli:crafting';recipe='tutorial_mod:star_singularity';recipe2='tutorial_mod:star_upgrade_template'},@{type='patchouli:smithing';recipe='tutorial_mod:cosmic_helmet';recipe2='tutorial_mod:cosmic_chestplate'},@{type='patchouli:smithing';recipe='tutorial_mod:cosmic_leggings';recipe2='tutorial_mod:cosmic_boots'})}
    Write-Resource "$bookRoot/equipment/pieces.json" @{name=@('套装效果规则','Set Effect Rules')[$index];icon='tutorial_mod:star_boots';category='tutorial_mod:equipment';sortnum=5;pages=@(@{type='patchouli:text';text=@('单件仅提供护甲、韧性、免伤与攻速/增伤属性。夜视、熔岩明视、力量、急迫、摔落保护等都要求同阶全套。切换阶级自动移除旧套装专属短时效果。低阶没有抗火，熔岩里仍会受伤。装备的免伤在同一属性内加算，随后与护甲、抗性等系统相乘。', 'Individual pieces provide attributes only. Night/lava vision, strength, haste and fall protection require a matching full set. Tier changes remove short-lived effects of the previous set. Lesser lava vision does not prevent lava damage. Immunity adds within one attribute before multiplication with armor/resistance.')[$index]})}
    # Correct old descriptions that granted capacity through resonance or Star Core.
    foreach ($entry in @('equipment/resonance','equipment/branches')) {
        $bookFile = Join-Path $resources "$bookRoot/$entry.json"
        $data = Get-Content -Raw $bookFile|ConvertFrom-Json -AsHashtable
        foreach ($page in $data.pages) {
            if ($page.text) { $page.text = $page.text -replace '基础上限\+20','额外星能获取+1' -replace '星能上限\+20','额外星能获取+1' -replace '星能容量\+20，','' -replace '\+20 energy cap','+1 energy gained' -replace '\+20 capacity and ',' ' -replace '每级\+10星能上限','星能容量由完整套装决定' -replace '每级增加10点星能上限与2%重剑伤害','每级增加2%重剑伤害，星能容量由完整同阶套装决定' -replace '\+10 energy capacity per level','capacity determined by full set' -replace '10 energy capacity and 2%','2%' }
        }
        Write-Resource "$bookRoot/$entry.json" $data
    }
    $controlsFile = Join-Path $resources "$bookRoot/reference/controls.json"
    $controls = Get-Content -Raw $controlsFile|ConvertFrom-Json -AsHashtable
    $domainNote = if ($index -eq 0) {'$(br2)J：星辰领域（高阶全套）；G：150星能重铸50点护盾。K编辑HUD，也可拖动领域和守护屏障提示。'} else {'$(br2)J: Star Domain (ascended full set); G: 150 energy for 50 shield. K edits HUD, including domain/barrier widgets.'}
    if ($controls.pages[0].text -notmatch 'J[:：]') { $controls.pages[0].text += $domainNote }
    Write-Resource "$bookRoot/reference/controls.json" $controls
    $altar = Get-Content -Raw (Join-Path $resources "$bookRoot/world/altar.json")|ConvertFrom-Json -AsHashtable
    $altar.pages[0].text = $altar.pages[0].text -replace '，全套时重铸星盾','（需要同阶全套）' -replace ', restores the full-set shield',' (requires a matching full set)'
    Write-Resource "$bookRoot/world/altar.json" $altar
    $shield = Get-Content -Raw (Join-Path $resources "$bookRoot/equipment/shield.json")|ConvertFrom-Json -AsHashtable
    $shield.pages[0].text = $shield.pages[0].text -replace '只有耗尽或死亡才清除','只有完全耗尽才清除（异常死亡后仍保留剩余屏障）' -replace 'until depleted or death','until completely depleted (also carried across abnormal deaths)'
    Write-Resource "$bookRoot/equipment/shield.json" $shield
}
Write-Output 'Tier recipes, smithing upgrades, translations and guide entries generated.'
if (Test-Path (Join-Path $PSScriptRoot 'GenerateDomainRefinement.ps1')) {
    & (Join-Path $PSScriptRoot 'GenerateDomainRefinement.ps1')
}
