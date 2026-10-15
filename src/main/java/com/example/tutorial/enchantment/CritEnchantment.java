package com.example.tutorial.enchantment;

/**
 * 暴击相关附魔：一个提供暴击概率，一个提供暴击伤害。
 *
 * <p>原版没有「暴击率」属性（{@code Attributes} 与 {@code ForgeMod} 里都没有），
 * 原版暴击是「下落途中攻击」的位置判定。所以这套暴击是模组自建的独立系统：
 * 每次命中掷一次骰，命中则把最终伤害乘以「基础倍率 + 爆伤加成」。
 * 两者都与原版跳劈暴击互不干涉，可能同时触发。</p>
 *
 * <p>数值是等级的纯函数，附魔上限被别的模组抬高时会自动跟着涨。</p>
 */
public class CritEnchantment extends TutorialEnchantment
{
    /** 这个附魔提供的是哪一种暴击属性。 */
    public enum Stat
    {
        /** 暴击概率（0.1 = +10%） */
        CRIT_CHANCE,
        /** 暴击伤害倍率的加成（0.2 = 暴击倍率 +0.2） */
        CRIT_DAMAGE
    }

    private final Stat stat;
    // 每级增加的数值
    private final double perLevel;

    public CritEnchantment(Rarity rarity, Stat stat, double perLevel, int baseCost, NameColor nameColor)
    {
        super(rarity, baseCost, nameColor);
        this.stat = stat;
        this.perLevel = perLevel;
    }

    public Stat getStat()
    {
        return this.stat;
    }

    /** 该等级提供的数值 = 每级数值 × 等级。等级不设上限。 */
    public double getBonus(int level)
    {
        return this.perLevel * Math.max(level, 0);
    }

    @Override
    public int getMaxLevel()
    {
        return 3;
    }
}
