# 锋利剑尖与原角度剑气

模组版本仍统一1.0.0-1.20.1，保留全部最新玩法。剑气恢复改动前角度、颜色、透明度和拖尾，仅放大1.65倍，伤害及命中范围不变。

剑尖不再用逐行挤出的方块轮廓。现以连续菱形截面网格构建：一个共同尖点、左右对称斜刃与中间剑脊，19个顶点、15个面，接入Forge自带OBJ加载器。剩余剑身、星纹、护手与持握设置保留。

## 文件与使用

- `stellar_sovereign_sharp.bbmodel`：Blockbench通用工程，包含原方块剑身和新的网格剑尖，内嵌两张现有贴图。
- `ice_sword.json`：Forge组合物品模型，包含原剑身及OBJ剑尖。
- `stellar_sharp_tip.obj` / `.mtl`：连续剑尖网格及材质定义。
- `stellar-sword-firstperson.png`、`stellar-sword-inventory.png`：独立Forge测试世界截图。

Blockbench“文件→打开模型”可编辑工程，尖端在“Sharp tip mesh”分组。此工程包含网格，不能直接当作纯原版Java方块模型导出；修改尖端需导出OBJ并保留Forge组合JSON。游戏只需要Forge，不增加第三方模型库。

组合模型引用两张已存在的材质：`tutorial_mod:item/stellar_greatsword_v3`与`tutorial_mod:item/stellar_greatsword_v2`。用于Blockbench的内嵌贴图与游戏资源保持一致；通用工程的材质UV单位与Java方块工程存在格式差异，继续导出前应检查UV。

旧方块模型和设计包保留在`docs/art/stellar_greatsword_v3`，便于比较。没有修改用户实际整合包或正式存档。
