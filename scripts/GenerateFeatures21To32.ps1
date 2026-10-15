$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
$resources = Join-Path $projectRoot 'src/main/resources'
$encoding = [Text.UTF8Encoding]::new($false)
function Write-Json($path, $data) {
    $target = Join-Path $resources $path
    [IO.Directory]::CreateDirectory((Split-Path -Parent $target)) | Out-Null
    [IO.File]::WriteAllText($target,($data | ConvertTo-Json -Depth 40),$encoding)
}
$nodes = @(
    @('root','星环之路','获得星辰装备，开启星环之路','tutorial_mod:star_ingot',''),
    @('ore','寻星者','获得第一块星矿','tutorial_mod:star_ore','root'),
    @('ingot','凝星成锭','获得星锭','tutorial_mod:star_ingot','ore'),
    @('sword','执掌星环','获得星环重剑','tutorial_mod:ice_sword','ingot'),
    @('ranged','统御之姿','切换为剑气模式','minecraft:arrow','sword'),
    @('slash_hit','月牙初现','剑气首次造成有效伤害','minecraft:iron_sword','ranged'),
    @('critical_slash','星芒一瞬','剑气触发暴击并造成伤害','minecraft:diamond_sword','slash_hit'),
    @('full_set','星环加冕','穿齐任意同阶星环套装','tutorial_mod:star_helmet','ingot'),
    @('energy_100','星能盈溢','拥有至少100点星能','minecraft:amethyst_shard','full_set'),
    @('triple_slash','三重星环斩','成功释放重剑专属技能','tutorial_mod:ice_sword','energy_100'),
    @('shield_full','星盾成形','拥有满额星能盾（默认50点）','minecraft:shield','full_set'),
    @('reforge','星盾重铸','消耗星能重铸星能盾','minecraft:anvil','shield_full'),
    @('elite','星蚀终结者','击败本模组星辰精英怪','minecraft:netherite_sword','slash_hit'),
    @('trial','试炼征服者','完成信标星辰试炼','minecraft:beacon','elite'),
    @('mastery','星主','重剑熟练度达到20级','minecraft:experience_bottle','sword'),
    @('combo_150','连星不绝','越战越勇达到150连击','minecraft:blaze_powder','mastery'),
    @('multi_kill','贯穿星河','同一道剑气击杀至少两个目标（强化+2起可穿透）','minecraft:crossbow','elite'),
    @('last_stand','逆境星光','生命不超过1点时完成星环击杀','minecraft:totem_of_undying','trial')
)
foreach($node in $nodes) {
    $display = @{title=$node[1];description=$node[2];icon=@{item=$node[3]};frame='task';show_toast=$true;announce_to_chat=$true;hidden=$false}
    if($node[0] -eq 'root') {$display.background='minecraft:textures/gui/advancements/backgrounds/stone.png'}
    if($node[0] -in @('mastery','combo_150','multi_kill','last_stand')) {$display.frame='challenge'}
    $json=@{display=$display;criteria=@{complete=@{trigger='minecraft:impossible'}}}
    if($node[4]) {$json.parent='tutorial_mod:astral/'+$node[4]}
    Write-Json ('data/tutorial_mod/advancements/astral/'+$node[0]+'.json') $json
}
Write-Json 'data/tutorial_mod/advancements/astral/guide_gift.json' @{criteria=@{complete=@{trigger='minecraft:impossible'}}}
foreach($name in @('ice_sword','star_helmet','star_chestplate','star_leggings','star_boots','cosmic_helmet','cosmic_chestplate','cosmic_leggings','cosmic_boots')) {
    $maximum=if($name -match 'chestplate') {2} else {1}
    Write-Json ('data/tutorial_mod/recipes/recycle_'+$name+'.json') @{type='tutorial_mod:astral_recycling';ingredient=@{item='tutorial_mod:'+$name};maximum=$maximum}
}
foreach($name in @('star_ore','star_ore_end')) {
    $path='data/tutorial_mod/worldgen/configured_feature/'+$name+'.json'
    $json=Get-Content -Raw (Join-Path $resources $path)|ConvertFrom-Json -AsHashtable
    $json.type='tutorial_mod:configurable_star_ore';Write-Json $path $json
    Write-Json ('data/tutorial_mod/worldgen/placed_feature/'+$name+'.json') @{feature='tutorial_mod:'+$name;placement=@(@{type='tutorial_mod:star_ore_distribution';end=($name -eq 'star_ore_end')},@{type='minecraft:biome'})}
}
$keys=@('slashes_fired','slash_hits','slash_kills','melee_kills','highest_combo','max_critical_damage','energy_gained','energy_spent','shield_absorbed','skills_used','perfect_parries')
$zh=@('发射剑气总数','剑气有效命中次数','剑气击杀数','重剑近战击杀数','最高连击','最高真实暴击伤害','累计获得星能','累计消耗星能','星盾与屏障累计吸收伤害','技能成功使用次数','完美防御次数')
$en=@('Astral slashes fired','Astral slash hits','Astral slash kills','Astral melee kills','Highest astral combo','Highest actual critical damage','Star energy gained','Star energy spent','Astral shield damage absorbed','Astral skills used','Perfect astral parries')
$chapters=@(
    @('21','音效逻辑','audio','minecraft:note_block','星能越过50与100时提示；失败、星盾受击、低值、破碎、重铸有不同声音。三重斩分三层音高，连击阶段音调递增。服务端限频，可关闭audio.enabled。后续要求取消暴击反馈，因此不添加暴击高音。'),
    @('22','装备维修','repair','minecraft:anvil','星锭可在铁砧维修重剑与两阶盔甲。满星能每10秒为手中重剑恢复10耐久；击杀星辰精英有25%概率修复100耐久。自动维修默认不与经验修补叠加。套装重铸与领域每次消耗每件1耐久，创造模式免消耗。均可配置。'),
    @('23','高炉回收','recycling','minecraft:blast_furnace','将未附魔星环装备放进高炉，提供燃料。胸甲最多回收2星锭，其他装备最多1锭；损坏装备按剩余耐久比例计算，至少需剩余耐久才能回收。带附魔装备必须先过砂轮。不会返还强化材料、星辰奇点或模板，也不会返还附魔经验。'),
    @('24','固定成本强化','reinforcement','minecraft:smithing_table','潜行，主手持星环装备，右键锻造台。升阶仍使用原本锻造槽配方；强化是独立的交互。最高+5，每级提高10%最大耐久；全身与主手强化提高技能效率，最多减耗20%，领域不减耗。$(br2)升到等级N：星锭2^N、钻石2N、经验5N级；+4和+5各额外消耗1星辰奇点。材料不足不消耗，强化不失败、不销毁装备。+2剑气穿透2目标，+4起穿透3目标，发射时快照。'),
    @('25','星环进度树','advancements','minecraft:book','原版进度界面新增星环之路：矿石、星锭、重剑、远程、剑气命中、剑气暴击、完整套装、100星能、技能、满星盾、重铸、精英、试炼、满级熟练度、150连击、单道多杀、残血反杀。满星盾遵守后续50点设定，不恢复旧250点自动护盾。暴击进度和统计不显示暴击特效。'),
    @('26','入门与帮助','onboarding','tutorial_mod:astral_guide','首次获得星锭会解锁重剑与手册配方。首次持有或制作重剑会赠送本手册，每个玩家世界内仅一次。重登或死亡不会重复发书。guide.autoGift可关闭。$(br2)所有按键可在按键设置中更改。/astral help查看简明操作；/astral stats查看统计。沿用现有帕秋莉，不退回原版翻页书。'),
    @('27','整合包配置','configuration','minecraft:comparator','世界serverconfig/tutorial_mod-gameplay.toml提供护甲、韧性、免伤、两阶容量、护盾、领域消耗、剑气速度、技能倍率、精英概率、维修、强化、回收、世界生成、友伤、死亡保留、特效预算和环境开关。客户端同步服务端配置。$(br2)既有获取量、射程和数量在config/tutorial_mod-common.toml；HUD位置大小在config/tutorial_mod-hud.toml。矿物生成仅影响新生成区域。'),
    @('28','安全与低特效','safety','minecraft:barrier','技能验证服务端玩家存活、非旁观、非假玩家、所在区块加载；技能包与普通剑气包分别限频。保留每人剑气数量、射程和生命期保护。退出与换维度清理剑气/格挡，重生与维度变化同步资源。粒子按区块每刻预算，低特效模式减少粒子，48格以外不生成剑气装饰星点。'),
    @('29','原版附魔兼容','vanilla','minecraft:enchanted_book','剑气遵循护甲、保护、弹射物保护、抗性。发射时快照锋利/亡灵杀手/节肢杀手、抢夺、火焰附加、击退与强化等级；换武器不能改变已发出的剑气。火焰附加点燃命中目标，击退沿飞行方向。$(br2)不触发横扫之刃与荆棘反伤：剑气视为投射物而非直接近战。不攻击创造/旁观玩家、盔甲架、假玩家、自己的宠物，遵守队伍友伤。'),
    @('30','动态装备名称','names','tutorial_mod:ice_sword','物品提示名称随本地星能从蓝色变为淡紫，满能量时缓慢蓝紫过渡；满级重剑增加星主前缀。星盾破碎时短暂暗色。尊重自定义名称，不改写物品名称NBT；强化数值在提示框展示。names.dynamic可关闭。暴击实体仍不加高亮反馈。'),
    @('31','环境互动','environment','minecraft:amethyst_cluster','全部默认关闭，可在environment中逐项启用：夜晚星能获取+20%；满月剑气伤害+10%；雷雨剑气最多连锁两名附近敌人，造成25%伤害；末地领域视觉半径+25%；下界衰减翻倍；信标光柱缓慢修复未破碎星盾；附近紫水晶簇低频共鸣；下落增加技能剑气伤害最多50%。不跨维度，不加载远处区块。'),
    @('32','战斗统计','statistics','minecraft:paper','原版统计界面的通用分类新增11项星环统计：发射、命中、剑气与近战击杀、最高连击、真实暴击、星能收入与消耗、护盾吸收、技能次数、完美防御。/astral stats也可查看。统计存储在原版玩家统计中，重登死亡保留；客户端不能提交统计数值。')
)
$englishChapters=@(
    'Energy crossings at50 and100 have distinct cues. Failed skills, shield impacts, low shield, break and reforge use different vanilla sounds. Triple waves have three pitches and combo milestones rise in pitch. Server-side rate limits prevent dense audio. audio.enabled disables mod sounds; critical sound effects remain removed.',
    'Repair both sword and armor with star ingots in an anvil. Full energy repairs10 sword durability every200 ticks. Elite kills have a25 percent chance to repair100 durability. Auto-repair skips Mending by default. Reforge and domains wear each armor piece by1, except in Creative. All amounts can be configured.',
    'Put unenchanted astral equipment and fuel into a blast furnace. Chestplates return at most2 ingots; other equipment returns at most1. Remaining durability scales the amount, rounded up; depleted items cannot recycle. Use a grindstone first to remove enchantments. Ascension and reinforcement materials are never returned.',
    'Sneak with equipment in your main hand and right-click a smithing table. Ordinary ascension still uses the three smithing slots. Reinforcement has fixed costs and caps at+5, preserving names, enchantments and damage.$(br2)Each level adds10 percent maximum durability. Total equipped reinforcement lowers ordinary skill costs by up to20 percent; domains are not discounted. LevelN costs2^N star ingots,2N diamonds and5N XP levels. Levels4 and5 each also cost1 singularity. +2 waves pierce2 targets; +4 waves pierce3.',
    'A native advancement tree tracks ore, ingots, sword, ranged mode, wave hits and criticals, full armor,100 energy, triple slash, full shield, reforge, elites, trials, maximum mastery,150 combo, single-wave multi-kills and a kill at1 health or less. The full-shield task follows the later50-point shield design, not the obsolete250-point shield.',
    'Your first star ingot unlocks sword and guide recipes plus clickable chat help. Obtaining the sword grants a Patchouli guide once per player/world. Death and reconnects do not duplicate gifts. guide.autoGift disables gifts.$(br2)Use /astral help for controls or /astral stats for counters. All keys can be rebound. The current Patchouli guide is retained instead of restoring the old written book.',
    'World serverconfig/tutorial_mod-gameplay.toml controls armor pieces, toughness, immunity, energy, shield, domains, wave speed/multiplier, elite chance, ore generation, maintenance, recycling, reinforcement, friendly fire, death retention, budgets and environment. Forge synchronizes these settings.$(br2)Existing gain/decay/range/cap settings remain in config/tutorial_mod-common.toml; HUD settings remain in config/tutorial_mod-hud.toml. Generation only affects new chunks. Restart the world after changes.',
    'Server-side checks reject dead, spectator, fake or unloaded players. Skill and ordinary-wave packets have separate budgets. Wave caps, lifetime and range remain bounded. Disconnect and dimension changes clear waves/parry state; respawn resynchronizes resources. Particle budgets apply per chunk/tick, low-effects mode reduces counts, and decorative wave stars are skipped beyond48 blocks.',
    'Waves follow armor, Protection, Projectile Protection and Resistance. Launch snapshots preserve Sharpness/Smite/Bane, Looting, Fire Aspect, Knockback and reinforcement. Switching weapons cannot alter an existing wave.$(br2)Fire Aspect burns for4 seconds/level; Knockback pushes along flight direction. Waves do not run Sweeping Edge or melee Thorns callbacks. Creative/spectator players, armor stands, fake players and owned pets are protected. Team friendly-fire rules remain authoritative.',
    'Client tooltip names shift from blue to violet at50 energy and slowly blend when full. Maximum mastery adds a Star Sovereign prefix. Shield break briefly darkens armor names. Custom item names are respected; no name NBT is rewritten every tick. Disable names.dynamic to retain normal names. Critical highlighting remains removed.',
    'All environment bonuses are optional and off by default: night energy+20 percent; full-moon wave damage+10 percent; thunder chains to at most2 nearby enemies for25 percent damage; End domain visuals+25 percent; doubled Nether decay; beacon beam repairs an unbroken shield; quiet nearby amethyst resonance; falling wave attacks gain at most50 percent damage. No dimension travel or remote chunk loading is added.',
    'Eleven counters appear in native General statistics and /astral stats: waves fired/hit/kills, melee kills, highest combo, actual critical damage, energy gained/spent, shield and barrier absorption, skills used and perfect parries. Vanilla player statistics persist across death and reconnects. Clients cannot submit counter values. Counters also unlock advancement milestones.'
)
$configuration=@('audio.enabled / audio.minimumIntervalTicks','repair.* / skills.armorWear','recycling.enabled','reinforcement.enabled / reinforcement.maxLevel','Esc → 进度 / Advancements','guide.autoGift','serverconfig/tutorial_mod-gameplay.toml','network.* / effects.*','combat.friendlyFire / combat.breakFragileBlocks','names.dynamic','environment.*','Esc → 统计信息 / /astral stats')
foreach($language in @('zh_cn','en_us')) {
    $langPath='assets/tutorial_mod/lang/'+$language+'.json'
    $lang=Get-Content -Raw (Join-Path $resources $langPath)|ConvertFrom-Json -AsHashtable
    for($i=0;$i -lt $keys.Count;$i++) {$lang['stat.tutorial_mod.'+$keys[$i]]=if($language -eq 'zh_cn') {$zh[$i]} else {$en[$i]}}
    $lang['tooltip.tutorial_mod.configured_set']=if($language -eq 'zh_cn') {'同阶全套：护甲%s、免伤%s%%、攻速/增伤+%s%%'} else {'Matching set: %s armor, %s%% immunity, +%s%% speed/damage'}
    Write-Json $langPath $lang
    foreach($chapter in $chapters) {
        $title=$chapter[0]+'. '+$chapter[1]
        $text=$chapter[4].Replace('%','％')
        $index=[int]$chapter[0]-21
        if($language -eq 'en_us') {$title=$chapter[0]+'. '+$chapter[2];$text=$englishChapters[$index]}
        $parts=$text -split '\$\(br2\)',2
        $page2=if($parts.Count -gt 1) {$parts[1]+'$(br2)'+$configuration[$index]} else {'$(bold)'+$configuration[$index]+'$()$(br2)'+$(if($language -eq 'zh_cn') {'使用 /astral help 查看快捷操作，使用 /astral stats 查看战斗统计。配置修改建议重启世界后生效。'} else {'Use /astral help for controls and /astral stats for counters. Restart the world after configuration changes.'})}
        Write-Json ('assets/tutorial_mod/patchouli_books/astral_guide/'+$language+'/entries/reference/feature_'+$chapter[0]+'.json') @{name=$title;icon=$chapter[3];category='tutorial_mod:reference';sortnum=[int]$chapter[0];pages=@(@{type='patchouli:text';text=$parts[0]},@{type='patchouli:text';text=$page2})}
    }
}
Get-ChildItem (Join-Path $resources 'assets/tutorial_mod/patchouli_books/astral_guide') -Recurse -Filter *.json | ForEach-Object {
    $before=[IO.File]::ReadAllText($_.FullName);$after=$before.Replace('%','％')
    if($before -ne $after) {[IO.File]::WriteAllText($_.FullName,$after,$encoding)}
}
'Generated 18 visible advances, hidden gift marker, 9 recycling recipes, configured ore assets, statistics labels and 12 bilingual guide entries.'
