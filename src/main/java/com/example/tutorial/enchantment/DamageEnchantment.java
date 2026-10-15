package com.example.tutorial.enchantment;

/**
 * 「按百分比放大最终伤害」的附魔。
 *
 * <p>倍率是等级的**纯函数**（不再是从前那种 3 个元素的死数组），
 * 所以如果别的模组把附魔等级上限提到 3 以上，伤害会自动跟着等级一起涨。</p>
 *
 * <p>实际倍率由 {@code event/CombatEventHandler} 在 LivingHurtEvent 里施加到最终伤害上，
 * 因此不受 ATTACK_DAMAGE 属性 0~2048 上限的夹取，也不会出现在物品提示框的攻击力那一行。</p>
 */
public class DamageEnchantment extends TutorialEnchantment
{
    /** 加成叠加到最终伤害上的方式。 */
    public enum Kind
    {
        /** 与其他同类附魔【加算】的百分比区：因子 = 1 + 每级 × 等级 */
        ADDITIVE_PERCENT,
        /** 独立乘区：每级翻一倍，即因子 = 2^等级 */
        FINAL_MULTIPLIER
    }

    private final Kind kind;
    // 仅 ADDITIVE_PERCENT 使用：每级增加的百分比
    private final double perLevel;

    public DamageEnchantment(Rarity rarity, Kind kind, double perLevel, int baseCost, NameColor nameColor)
    {
        super(rarity, baseCost, nameColor);
        this.kind = kind;
        this.perLevel = perLevel;
    }

    public Kind getKind()
    {
        return this.kind;
    }

    /** 该附魔在指定等级下的伤害因子（1.0 = 无加成）。等级不设上限。 */
    public double getFactor(int level)
    {
        if (level <= 0)
        {
            return 1.0D;
        }
        return this.kind == Kind.FINAL_MULTIPLIER
                ? Math.pow(2.0D, level)
                : 1.0D + this.perLevel * level;
    }

    @Override
    public int getMaxLevel()
    {
        return 3;
    }
}
