# 1.1.0 完整交付验证记录

## 测试环境与范围

Minecraft1.20.1 / Forge47.4.10 / Java17 / Patchouli84.1。新增功能测试及回归测试使用 Apothic Attributes1.3.7 + Placebo8.6.3；该兼容组合不是本模组的新增硬依赖。

测试均在项目独立目录运行，没有修改实际 Minecraft 安装、整合包或个人存档。

|测试|覆盖内容|记录|
|---|---|---|
|第21–32类新增功能|固定强化成本与上限、NBT保持、动态耐久、回收配方与附魔拒绝、经验修补隔离、星能统计、技能包限频、矿物分布配置|docs/art/features-final-check.log|
|剑气兼容|发射附魔快照、抢夺事件、火焰附加、击退、穿透去重、多杀进度、目标保护、持久化|同上|
|配置与环境|小额夜晚增益累计、满月/下落上限、雷雨连锁、紫水晶共鸣、有效信标恢复、禁止信标免费重铸、音效关闭、粒子预算、低特效、死亡克隆保留与重新装备恢复|同上|
|原有装备回归|初/高阶护甲、混穿、星能上限、盾消耗、四件升阶保留NBT、两领域数值、黑暗等负面免疫、屏障持久与溢出、创造无CD、精英能力、Apothic补偿隔离|docs/art/features-final-tier-regression.log|
|独立悬赏回归|十项数量、先接取、击杀归属、目标进度封顶、普通/神话奖励、死亡数据保留、实际界面接取与领奖、重复网络包防重领|docs/art/features-final-bounty-regression.log|
|无客户端服务端检查|实际GameTestServer启动，强制执行一项测试，配方/进度加载、公共剑气类加载、无主剑气清除、异常伤害与空物品快照|docs/art/features-dedicated-check.log|
|资源检查|38个中英文手册条目、分类、内部链接、合成引用、祭坛投影、全部JSON解析|scripts/ValidatePatchouliGuide.ps1|

## 原生界面检查

- `docs/art/reinforcement-guide.png`：帕秋莉强化章节，百分号格式错误已修复。
- `docs/art/astral-statistics.png`：原版统计通用分类显示星环统计。
- `docs/art/astral-advancements.png`：原版进度界面显示星环之路及分支。
- 实际物品提示验证：只有重剑显示剑气穿透，护甲显示真实技能效率；创造/高级提示包含下一级星锭、钻石、经验和奇点成本。
- 独立悬赏与暗紫剑气截图保留于 `docs/art/bounty-*.png`、`docs/art/dark-purple-slash.png`。

## 发布检查

正式构建不携带verify参数，发布JAR内不得出现任何SmokeTest、SmokeVerifier、GameTest或TierProgression开发测试类；必须包含自定义配方、完整进度树、38个手册条目、悬赏界面和粒子资源。运行 `scripts/PackageCompleteDelivery.ps1` 校验这些要求、生成完整交付压缩包，并输出SHA256。

这是上述指定环境的验证结果，不承诺任意第三方模组组合均兼容或具有同样的战斗平衡。对实际整合包建议先备份存档再替换，并确保双方版本一致。
