# 星环重剑 · 星辰简约版

这是一套实际接入模组的Minecraft Java方块模型，不是只有概念图。宽银蓝剑刃、深蓝中央剑脊、单条青蓝能量线、单层八边星环与菱形星核。35个方块构件，5个编辑分组；旧模型有919个构件。

## 文件

- `stellar_greatsword_v2.bbmodel`：Blockbench原生可编辑工程，Java Block/Item格式，贴图内嵌。
- `stellar_greatsword_atlas.png`：128×128材质图集。
- `ice_sword.json`：Minecraft物品模型，资源路径已设置。
- `model_front_preview.png`：根据实际几何生成的正面示意，不是游戏截图。
- `stellar-sword-firstperson.png`、`stellar-sword-inventory.png`：独立Forge测试存档中的实际显示。
- `IMAGE_PROMPT.txt`：内置图像生成工具的原始材质提示词。

## Blockbench使用

在Blockbench选择“文件→打开模型”，打开`.bbmodel`。贴图已经内嵌，无需寻找外部旧路径。模型包含Blade、Inlays、Halo、Guard、Hilt五组，可分别修改几何及UV。显示设置包括左右手第一／第三人称、背包、掉落和展示框。

修改后通过Minecraft Java模型导出功能导出JSON；贴图另存为PNG。游戏资源分别是：

`assets/tutorial_mod/models/item/ice_sword.json`

`assets/tutorial_mod/textures/item/stellar_greatsword_v2.png`

JSON引用` tutorial_mod:item/stellar_greatsword_v2 `，不要误改为盔甲包的命名空间。

## 验证与范围

游戏内已加载模型并查看第一人称与背包截图；结构检查包含元素尺寸、坐标边界、合法旋转角、纹理尺寸与内嵌数据。没有在本机Blockbench界面中打开验证，因此不同Blockbench版本如提示导入转换，请使用Minecraft Java方块／物品格式。

材质由内置image_gen工具生成，随后机械转换为128×128最近邻纹理；几何与UV由项目生成脚本构建。原始素材未删除。`ice_sword_previous.json`是旧游戏模型备份，不进入新版设计压缩包。

新版模组1.1.6沿用原物品ID、配方、伤害、技能与NBT数据，只替换外观；保留剑气混合伤害、300秒统御冷却及每日悬赏。不会自动修改用户整合包或正式存档。
