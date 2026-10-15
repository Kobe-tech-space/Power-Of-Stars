# 星环装备指南

安装 Forge 1.20.1 对应的本模组和 `Patchouli-1.20.1-84.1-FORGE.jar`，客户端与服务器均需要安装。
如果整合包已有满足版本要求的 Forge 帕秋莉，只保留一份；不要添加 Fabric 版本或重复的帕秋莉。

现有 `tutorial_mod:astral_guide` 物品保持兼容，书与星锭仍可无序合成。右键打开帕秋莉界面。
手册提供六个分类、38个条目、中英文内容、搜索、书签、章节链接、装备合成图、烧炼页与可投影祭坛结构；参考分类包含第21–32类功能说明。
书封采用深蓝、青色星环和紫色书签。封面可运行 `scripts/GenerateGuideCover.ps1` 重建。

手册定义位于 `src/main/resources/data/tutorial_mod/patchouli_books/astral_guide/book.json`。
内容位于 `src/main/resources/assets/tutorial_mod/patchouli_books/astral_guide/{en_us,zh_cn}`。
执行 `scripts/ValidatePatchouliGuide.ps1` 检查分类、内部链接、配方和多方块映射。

剑气使用直接绘制的月牙弧面、亮色核心和渐隐尾迹，无须外部模型或剑气贴图。
剑气统一为暗紫色，搭配四芒星粒子；保留暴击计算但不单独显示暴击特效。最多16道、最多5秒；强化+2/+4分别可穿透2/3个目标。发射时保留伤害与原版附魔快照。

测试实例可使用 `gradlew runClient -Prun_directory=run-patchouli-check -Pverify_guide`。
此实例仅在测试目录有 `saves/PatchouliSmokeTest/level.dat` 时可快速进入对应测试世界。

接入依据：[帕秋莉书籍配置](https://vazkiimods.github.io/Patchouli/docs/reference/book-json/)、
[页面类型](https://vazkiimods.github.io/Patchouli/docs/patchouli-basics/page-types/)。
