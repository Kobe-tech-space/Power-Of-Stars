# 星辰升阶与领域

适用 Minecraft 1.20.1 / Forge 47.4.10。安装本模组和 Patchouli 1.20.1-84.1-FORGE；无需额外安装熔岩明视模组。

## 两阶装备

| 属性 | 初阶单件 | 初阶全套合计 | 进阶单件 | 进阶全套合计 |
| --- | ---: | ---: | ---: | ---: |
| 护甲 | 20 | 100 | 50 | 250 |
| 韧性（保留原设定） | 50 | 200 | 50 | 200 |
| 伤害免疫 | 5% | 30% | 10% | 60% |
| 百分比攻速 | 2% | 13% | 5% | 30% |
| 加算增伤 | 2% | 13% | 5% | 30% |
| 星能容量 | 无 | 250 | 无 | 1000 |

星能容量 1000 为高阶暂定值。蓝紫 original 模型对应初阶，银蓝 textured 对应进阶；提供的两个 bbmodel 几何结构均为 68 个方块，大小相同，使用各自的 UV、贴图与旋转数据。物品栏图标使用提供的八张 64×64 PNG。

套装要求四件同阶。混穿只有单件属性，不能获取、储存星能，套装效果也会移除。初阶全套拥有摔落缓冲 X（额外 80% 摔落减伤）、抗性提升 I、力量 II、急迫 II、夜视、熔岩明视。进阶全套拥有摔落免疫、抗火、力量 X、急迫 X、夜视、熔岩明视。两阶全套均保留头后星环。

装备增伤加到超级/究极伤害附魔的加算区，再应用至尊等独立乘区。装备攻速使用 MULTIPLY_BASE，与其他同类百分比攻速加算。免伤加到现有免伤属性，再与其他减伤机制相乘。未安装 Apothic Attributes 时保留超出 20 点护甲的可配置补偿；检测到 attributeslib 后关闭补偿，避免与其替换后的护甲公式重复减伤。因此表中的免伤不是包含护甲、抗性等机制之后的最终减伤率。

熔岩明视将岩浆雾视距提高到最多 64 格，并在岩浆中隐藏火焰遮挡。低阶仅改善视野，不防火。效果参考 [Lava Clear View](https://www.curseforge.com/minecraft/mc-mods/lava-clear-view)，本模组自己实现套装条件，没有加入新的依赖。

## 按键

- 右键重剑：切换近战/剑气模式。
- J：高阶全套的星辰领域，固定消耗 1000 星能，余额不足不释放。
- G：同阶全套消耗 150 星能，把星能护盾重铸为 50，不叠加。
- V / 潜行 V：保留三重剑气/八向剑气技能。
- K：HUD 编辑器；统一为可拖动的小面板，连击条可选，默认关闭。

近战模式的「破军」持续 10 秒，冷却 300 秒。攻击力 ×3、攻速 ×3、增伤 ×1.5，三个独立乘区；剑气会保存发射瞬间的全部加成。剑气模式的「统御」持续 10 秒，冷却 30 秒，免疫单次原始伤害大于 1 的伤害，清除并拒绝负面和中性不利状态，立即获得 1000 点守护屏障。1 点及以下伤害仍可发生，但先由护盾吸收。

领域以真实药水状态同步：破军使用星辰奇点图标，统御使用金色奇点图标，HUD 不再显示领域时长倒计时。统御能拦截监守者范围黑暗、失明、凶兆等；服务端最终拦截与客户端视觉清理共同生效。免疫仅在统御状态存在的 10 秒内有效，切换到剑气模式不等于施放领域。喝奶、脱装或领域结束后，负面效果可以正常施加。

暴击伤害和暴击星能保留，取消本模组暴击浮字、专属粒子、音效及紫色强化剑气外观。其他模组和原版跳劈产生的暴击表现不修改。

两种领域分别冷却，切换模式不改变正在生效的领域。卸下高阶全套结束领域，但不清除守护屏障。创造模式不要求星能且无领域冷却。冷却以服务器主世界游戏时钟计时，换维度、换装、死亡不重置。

守护屏障在领域结束、换装、离线以及异常死亡后保留剩余数值，只有完全击碎才消失。再次统御刷新为 1000，不叠加。星能护盾在卸装或死亡后清除。两者由独立数值记录，不混入金苹果的吸收生命值；守护屏障先吸收，星能护盾后吸收，超出的伤害正常扣血。祭坛只能补星能与修复装备，不再免费重铸护盾。

## 昂贵升阶链

9 星锭 ↔ 1 星锭块（为避免新增第三张自制贴图，方块复用原版紫水晶块外观）。

星辰奇点配方：

```text
下界合金锭  钻石块      下界合金锭
星锭块      下界之星    星锭块
下界合金锭  钻石块      下界合金锭
```

星辰升级模板配方：

```text
钻石块      下界合金锭          钻石块
星锭块      下界合金升级模板    星锭块
钻石块      下界合金锭          钻石块
```

锻造台按「星辰升级模板 / 初阶盔甲 / 星辰奇点」升阶。保留附魔、名称与耐久损耗。四件分别消耗 1 个奇点和 1 个模板；没有廉价复制配方。

全套材料合计：4 下界之星、24 下界合金锭、24 钻石块（216 钻石）、16 星锭块（144 星锭）、4 原版下界合金升级模板，以及原有初阶四件套。以凋灵、远古残骸和堡垒探索作为终局门槛；大量星锭也给现有矿物、精英与试炼奖励提供用途。

旧版 star_* 物品 ID 保留并改为初阶，cosmic_* 是新进阶物品。旧版自动赠送的 250 点吸收盾只迁移清理一次，不再重新赠送。替换旧 jar 前建议备份存档；服务端和客户端必须一起更新（网络协议已升级到 9）。

## 自制贴图

使用内置图像生成工具生成，保留透明通道，最近邻缩放至 64×64。原图存于 docs/art，游戏文件：

- src/main/resources/assets/tutorial_mod/textures/item/star_singularity.png
- src/main/resources/assets/tutorial_mod/textures/item/star_upgrade_template.png

提示词原文（内置工具，不使用 CLI）：

**Star Singularity**

Use case: stylized-concept. Asset type: Minecraft 1.20.1 inventory item sprite, Star Singularity. A single original pixel-art cosmic singularity: tiny obsidian black diamond core surrounded by a bright cyan-white four-point star and two angular violet orbital rings, dark navy outline, restrained gold flecks. True transparent background, no text, no frame, no shadow or checkerboard, no other objects. Centered square, silhouette fills 80 percent. Crisp coarse pixel clusters suitable for nearest-neighbor reduction to 64x64; flat 2D item icon, not photorealistic.

**Star Upgrade Template**

Use case: stylized-concept. Asset type: Minecraft inventory sprite Star Upgrade Smithing Template. Single original flat 2D pixel-art smithing template: chunky beveled midnight-blue square tablet viewed frontally, silver angular border, inset bright cyan four-point star with a short violet upward chevron beneath, subtle gold corner fasteners. Visually recognisable as a costly netherite-like forging template, fits blue-purple cosmic armor. Genuine transparent background, centered square 80 percent fill. Crisp coarse pixel clusters suitable for nearest-neighbor reduction to 64x64. No text, no scenery, no shadow, no checkerboard, only one item.

## 验证方式

`scripts/ImportCosmicTiers.ps1` 从所提供文件机械导出模型数据、贴图、物品图标；`scripts/GenerateTierProgression.ps1` 生成配方、双语与手册数据；`scripts/ValidatePatchouliGuide.ps1` 校验手册引用。开发测试通过 `-Pverify_tiers -Prun_directory=run-patchouli-check` 在独立测试存档执行，测试代码不进入正式 jar。

验证结果：两阶属性、同阶限制、套装药水效果、摔落保护、护盾消耗、锻造保留附魔/名称/损耗、独立领域倍率、冷却阻止与创造免冷却、统御伤害阈值和负面状态拦截、屏障持续与溢出伤害均通过游戏内断言。26 条中英文手册均通过引用校验；真实右键打开手册、升阶配方页与领域页通过游戏内检查。记录保存在 docs/art/tier-progress-check.log、guide-tier-check.log，并保留了原生游戏截图。

测试环境为 Forge + Patchouli 的独立本地测试存档；没有修改或进入你的正式存档，也未对你的整合包、联机服务器或具体光影做兼容性保证。熔岩明视使用 Forge 的雾和火焰覆盖事件，最终效果可能受第三方光影影响。
