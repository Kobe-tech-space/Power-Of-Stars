package com.example.tutorial.enchantment;

/**
 * 「越战越勇」：连续命中会累积连击数，按连击档位放大最终伤害；一段时间没造成伤害就清零。
 *
 * <p>这是唯一一个只有 1 级的附魔 —— 强度完全由当前连击数决定，与附魔等级无关。
 * 连击的统计与清零在 {@code event/ComboTracker}。</p>
 */
public class ComboEnchantment extends TutorialEnchantment
{
    // 连击阈值
    private static final int[] THRESHOLDS = { 50, 100, 150 };
    // 达到对应阈值时的伤害加成（0.5 = +50%）
    private static final double[] BONUSES = { 0.50D, 1.00D, 1.50D };

    public ComboEnchantment(Rarity rarity, int baseCost, NameColor nameColor)
    {
        super(rarity, baseCost, nameColor);
    }

    /** 按当前连击数返回伤害加成；未达最低阈值返回 0，超过最高阈值封顶。 */
    public double getBonusForCombo(int combo)
    {
        double bonus = 0.0D;
        for (int i = 0; i < THRESHOLDS.length; i++)
        {
            if (combo >= THRESHOLDS[i])
            {
                bonus = BONUSES[i];
            }
        }
        return bonus;
    }

    @Override
    public int getMaxLevel()
    {
        return 1;
    }
}
