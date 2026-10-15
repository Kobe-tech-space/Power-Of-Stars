# power of stars · Forge 1.20.1

星辰主题装备、剑气、星能、天赋与饰品模组。版本保持 `1.0.0-1.20.1`。

[下载可安装的模组 JAR](https://github.com/Kobe-tech-space/Power-Of-Stars/raw/refs/heads/main/distribution/power_of_stars-1.0.0-1.20.1.jar)

为兼容已有存档，内部注册 ID 保留 `tutorial_mod`；安装新版时替换旧 JAR，不要同时安装两份。

## 构建

需要 Java 17，运行 `gradlew.bat build`（Windows）或 `./gradlew build`。产物为 `build/libs/power_of_stars-1.0.0-1.20.1.jar`。依赖由 Gradle 从配置的 Maven 仓库获取；游戏运行需要 Forge 与 Patchouli，Curios 为可选依赖。

仓库不包含本地游戏存档、运行日志、开发缓存、第三方 JAR 和美术生成过程中的原始备份。

饰品清晰度修正：十件饰品重绘为面向真实物品栏尺寸的32×32像素图标，减少细链和碎光；原高清图另存保留，功能与配方不变。

新增十种星辰饰品：Curios可选适配、3个专属槽，同种不叠加；未安装Curios时副手一件生效。每件都有独立功能、配方及帕秋莉条目，详见[饰品说明](docs/STELLAR_ORNAMENTS.md)。

移动修正：H瞬移、R冲刺无技能冷却；R低头仍水平前进；G满盾和移动受阻分别显示明确提示。见[移动与重铸说明](docs/MOVEMENT_AND_SHIELD.md)。

新增内置星辰天赋树（O键或 `/astral talents`）及6种装备专属附魔，无新增前置依赖。详见 [天赋与专属附魔](docs/STELLAR_TALENTS.md)。版本仍为1.0.0。

当前未发布版本统一为：1.0.0-1.20.1。功能保留最新实现，不回退历史版本。以星环重剑、暗紫剑气、两阶盔甲、星能领域与终局成长为核心，包含专属附魔、精英、信标试炼、悬赏、可配置HUD和帕秋莉手册。

本次补充魔法／物理伤害Forge分类，并增强剑气尺寸与可见性，见[伤害与视觉说明](docs/MAGIC_AND_SLASH_VISIBILITY.md)。

最新修正：按用户要求恢复原剑气角度，仅整体放大；剑尖改为连续对称斜刃网格，见[锋利剑尖工程](docs/art/stellar_sharp_tip/README.md)。

远程普攻支持按住连发，松开停止；保留原有武器、盔甲和精英强化系统。

1.1.8使用原版持剑方向和左右手显示设置，重剑贴图升级为1024×1024。

1.1.7重新设计对称星辰君临重剑，见[Blockbench工程说明](docs/art/stellar_greatsword_v3/README.md)。

1.1.6更新简约星辰重剑模型，附[Blockbench工程与设计说明](docs/art/stellar_greatsword_v2/README.md)。

1.1.5悬赏按游戏日刷新并降低奖励，详见[每日悬赏](docs/DAILY_BOUNTIES.md)。

1.1.4将统御领域冷却改为300秒，与破军一致；持续10秒，创造无冷却。

1.1.2加入独立攻击输入兼容，1.1.3将剑气改为半物理半魔法；详见[混合伤害说明](docs/MIXED_SLASH_DAMAGE.md)。完整说明正文的数值基线仍为1.1.1。

## 玩家说明

[阅读中文完整说明](docs/星环模组完整说明.md)：安装、材料配方、装备数值、按键技能、领域护盾、附魔、升阶与强化、维修回收、全部悬赏、配置与常见问题。

Minecraft1.20.1，Forge47.4.10或更新47.x，必装兼容Forge版Patchouli。客户端与服务器同时更新，不要保留重复旧版JAR。

构建后的当前模组文件：`build/libs/tutorial_mod-1.0.0-1.20.1.jar`。

## 开发与记录

- [21～32类实现记录](docs/FEATURES_21_32.md)
- [附魔描述与创造分类](docs/ENCHANTMENT_DESCRIPTIONS.md)
- [交付验证记录](docs/RELEASE_VERIFICATION.md)

历史阶段文档可能保留旧版本信息，请优先参照完整说明及当前配置。
