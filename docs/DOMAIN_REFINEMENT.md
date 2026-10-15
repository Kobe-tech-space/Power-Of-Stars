# 领域、紧凑 HUD、精英与 Apothic 兼容

## 本次修改

- 生存模式领域固定消耗1000星能，999也不能释放；创造保持免费、无冷却。
- 领域仍持续10秒，破军300秒冷却、统御30秒冷却。领域用真实药水状态同步，蓝紫奇点表示破军，金色奇点表示统御。没有独立HUD倒计时。
- 领域能力以服务器合法施法状态为准，直接用指令给予图标并不能绕过资源、装备或冷却条件。喝奶、脱装、到期都会结束领域；屏障仍保留。
- 默认HUD统一为左下148×62基准像素、80%缩放的小面板。模式、等级、星能和两层护盾使用相同配色、细条、窄边。领域状态交给原版药水栏，连击条默认关闭。K可调整面板与可选连击条。
- 本模组的暴击浮字、粒子、音效和特殊剑气外观取消，但暴击倍率、暴击星能照常计算。原版跳劈与其他模组的暴击特效不改动。

## 黑暗为什么还会出现

原版黑暗属于HARMFUL，但旧逻辑只在服务器判定统御的10秒内拦截。单纯切换到剑气模式、穿着高阶套装、或仍有守护屏障，并不等于统御领域还在生效。原来的HUD模式名称容易让这几种状态混淆。

新版以药水图标标明真正生效状态，拦截在Applicable事件的最后优先级执行，并每tick清除残留状态；客户端额外清理黑暗等视觉效果。HARMFUL与NEUTRAL类别都阻止，覆盖黑暗、失明、凶兆、发光标记等。BENEFICIAL状态不阻止。

测试覆盖正常添加、forceAddEffect、监守者同款MobEffectUtil.addEffectToPlayersAround范围添加、统御到期后正常恢复黑暗施加。免疫仅在统御生效期间有效，不是永久免疫，也不声称能识别绕开Minecraft状态系统的任意第三方自定义机制。

## 精英强化

随机概率保持默认3%，特殊世界事件保留原有提升。自动强化跳过已有命名怪和生命≥200的Boss，避免粗暴叠加其他模组的命名Boss。存量本模组精英按版本只升级一次；重复加载、重复强化不会继续叠加。

基础强化：生命根据施加时的已有最大生命独立乘算，最多六倍、施加结果封顶500；+12护甲、+8韧性、+35%击退抗性；一般攻击力+100%、速度+20%，狂暴攻击力+200%，迅捷速度+65%。攻击/速度与其他同运算属性相加，不改写其他模组基础值，生命不会把已含其他加成的基数作为固定值再乘一次。

| 类型 | 技能 |
| --- | --- |
| 狂暴（红） | 半血时，每3秒刷新力量II和速度I的4秒爆发 |
| 星盾（青） | 初始80吸收盾；5秒未受伤后，每6秒恢复10，上限80 |
| 迅捷（金） | 每4秒向2～10格内目标突进，检查碰撞，不穿墙传送 |
| 引力（紫） | 10格内有视线的目标受牵引，每6秒产生6点伤害与缓速II脉冲 |

技能要求有效目标和视线，不针对创造或旁观玩家。统御同时阻挡本模组引力精英的牵引。视觉按每10tick最多3粒基础粒子、脉冲10～18粒限量发送；不额外召唤大量实体。击杀奖励3～6星锭与碎裂反馈。生命封顶按施加当时的已有属性计算，已经超过500的最大生命不会强行降低；其他模组后续变化仍可能提高实际最大生命。

## Apothic Attributes

检测的modid是`attributeslib`，没有新增生产环境强制依赖。已在Forge 47.4.10 + Patchouli + Apothic Attributes 1.3.7 + Placebo 8.6.3的独立测试存档联测。

Apothic Attributes会替换护甲和保护减伤公式，并把韧性主要用于抵御穿透/削甲。所以即使面板护甲、韧性不变，实际承伤也不一定等同未安装时。官方说明：[Armor Calculations](https://www.curseforge.com/minecraft/mc-mods/apothic-attributes)。

旧版额外处理“超出20点护甲的补偿减伤”，与Apothic的可扩展护甲公式一起使用会重复计算。新版检测到Apothic后自动关闭此补偿，保留装备护甲、韧性、免伤、攻速、加算增伤；属性上限只提高到至少500，不再把其他模组已扩大的上限降低为500。

属性数值未被反复放大或覆盖，但实际平衡会改变。Apothic或其他模组提供的暴击、穿透、生命偷取等仍按它们的规则参与战斗，不能保证任意整合包的所有搭配都具有相同伤害结果。自定义免伤/增伤属性的0.6、0.3分别表示60%、30%，不是只增加0.6或0.3个百分点。

## 图标

使用内置图像生成工具编辑金色版本，透明通道保留，最近邻缩至64×64。破军直接复用星辰奇点原图。

- `src/main/resources/assets/tutorial_mod/textures/mob_effect/army_breaker.png`
- `src/main/resources/assets/tutorial_mod/textures/mob_effect/dominion.png`
- 金色原图归档：`docs/art/dominion_generated.png`

编辑提示词：

Edit target: supplied cosmic singularity pixel-art sprite. Change ONLY its color theme to luminous golden yellow, amber, cream-white and dark bronze. Preserve exactly the centered four-point star silhouette, central dark diamond, orbit loops, spark placements, coarse pixel-art shapes and transparent negative spaces. Replace cyan and blue with gold and cream; replace purple with amber and bronze. Actual transparent background. No text, no new shapes, no border or frame. This will be a Minecraft potion-status icon matching the original blue singularity.

## 验证

游戏内断言覆盖1000星能门槛、领域倍率、状态同步、黑暗与中性负面效果、屏障持续、两阶属性、锻造NBT保留，以及四类精英的爆发/回盾/突进/牵引行为和防重复叠加。Apothic联测记录：`docs/art/apothic-refinement-check.log`。HUD及药水图标由原生游戏截图验证。

升级后联机双方同时替换jar；协议为9。先备份正式存档并删除旧jar，不要同时装两份同ID模组。
