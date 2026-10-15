package com.example.tutorial.enchantment;

import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.TridentItem;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentCategory;

/**
 * 本模组附魔的公共基类，集中处理所有附魔都要做的事：
 * <ul>
 *   <li>名字逐字上色（含彩虹 / 闪烁）</li>
 *   <li>适用范围：任意武器（剑 / 斧 / 三叉戟 / 弓 / 弩）</li>
 *   <li>附魔台的等级代价</li>
 * </ul>
 *
 * <p>子类只需要负责自己的等级与数值，并覆盖 {@link #getMaxLevel()}。</p>
 */
public abstract class TutorialEnchantment extends Enchantment
{
    /**
     * 决定附魔名在某个字符位置、某一瞬间的颜色（0xRRGGBB）。
     *
     * <p>颜色允许随时间变化 —— 物品提示框每帧都会重建一次
     * （AbstractContainerScreen.renderTooltip → getTooltipFromContainerItem →
     * ItemStack.getTooltipLines → appendEnchantmentNames → getFullname），
     * 所以只要颜色取决于当前时间，就会自动产生流动 / 闪烁效果，
     * 不需要任何 mixin 或自定义渲染。</p>
     */
    @FunctionalInterface
    public interface NameColor
    {
        int rgbAt(int charIndex, long timeMillis);
    }

    // 1 级在附魔台出现所需的最低等级，更高等级在此基础上递增
    private final int baseCost;
    // 附魔名的颜色
    private final NameColor nameColor;

    protected TutorialEnchantment(Rarity rarity, int baseCost, NameColor nameColor)
    {
        // WEAPON 分类默认只认剑，实际的「任意武器」判定由下面的 canEnchant 负责
        super(rarity, EnchantmentCategory.WEAPON, new EquipmentSlot[]{ EquipmentSlot.MAINHAND });
        this.baseCost = baseCost;
        this.nameColor = nameColor;
    }

    @Override
    public int getMinLevel()
    {
        return 1;
    }

    @Override
    public int getMinCost(int level)
    {
        return this.baseCost + (level - 1) * 6;
    }

    @Override
    public int getMaxCost(int level)
    {
        return this.getMinCost(level) + 25;
    }

    /**
     * 重写原版的附魔名渲染。原版在这里硬编码成灰色（诅咒为红色），
     * 我们改成逐字符上色，颜色由构造时传入的 {@link NameColor} 决定。
     */
    @Override
    public Component getFullname(int level)
    {
        // 先按原版的样子拼出「名字 + 罗马数字等级」
        MutableComponent plain = Component.translatable(this.getDescriptionId());
        if (level != 1 || this.getMaxLevel() != 1)
        {
            plain.append(CommonComponents.SPACE).append(Component.translatable("enchantment.level." + level));
        }

        String text = plain.getString();
        long now = System.currentTimeMillis();

        // 逐字符构造，才能做到「同一时刻每个字颜色不同」的真正彩虹
        MutableComponent styled = Component.empty();
        for (int i = 0; i < text.length(); i++)
        {
            styled.append(Component.literal(String.valueOf(text.charAt(i)))
                    .withStyle(Style.EMPTY.withColor(TextColor.fromRgb(this.nameColor.rgbAt(i, now)))));
        }
        return styled;
    }

    // 附魔台在筛选可用附魔时调用的是这个方法（按 ItemStack 判定）
    @Override
    public boolean canApplyAtEnchantingTable(ItemStack stack)
    {
        return isWeapon(stack);
    }

    // 铁砧、/enchant 命令等通用路径走这个
    @Override
    public boolean canEnchant(ItemStack stack)
    {
        return isWeapon(stack);
    }

    public static boolean isWeapon(ItemStack stack)
    {
        Item item = stack.getItem();
        return item instanceof SwordItem
                || item instanceof AxeItem
                || item instanceof TridentItem
                || item instanceof BowItem
                || item instanceof CrossbowItem;
    }
}
