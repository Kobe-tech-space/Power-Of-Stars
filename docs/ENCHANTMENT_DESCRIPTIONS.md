# 1.1.1：附魔描述与独立分类

全部八种附魔已提供 `enchantment.tutorial_mod.<id>.desc` 中英文说明，供 Enchantment Descriptions 直接读取；无需额外接口或强制依赖。

Enchantment Descriptions 1.20.1 通过附魔注册对象获取说明，再匹配附魔名称组件。本模组现有静态彩色名称保留，不改变伤害数值。

如果仅附魔书显示说明、武器不显示，检查 `config/enchdesc.json` 的 `onlyDisplayOnBooks`。如果要求按键才显示，按住 Shift，或调整 `requireKeybindPress`。还需安装匹配版本的 Bookshelf。

创造模式新增“星环装备”分类（重剑图标），包含全部15种物品及所有正常等级的附魔书。移除模组物品及附魔书在原版分类中的重复项；全局搜索仍可搜索它们。JEI 的搜索与分类由 JEI 自己控制，不修改原版配方类型。

本次只补充说明和创造分类，不调整附魔、套装、升阶或强化数值。
