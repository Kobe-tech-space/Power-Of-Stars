package com.example.tutorial.event;

import com.example.tutorial.TutorialMod;
import com.example.tutorial.enchantment.ModEnchantments;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

/**
 * 把「暴击率 / 暴击伤害」显示在物品提示框末尾。
 *
 * <p>伤害类附魔走的是 LivingHurtEvent（不走属性），所以提示框里看不到它们的数字；
 * 暴击这两个是唯一会被展示出来的 —— 因为玩家否则完全无从得知自己的暴击率。</p>
 */
@Mod.EventBusSubscriber(modid = TutorialMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class TooltipEventHandler
{
    @SubscribeEvent
    public static void onItemTooltip(ItemTooltipEvent event)
    {
        ItemStack stack = event.getItemStack();
        double critChance = ModEnchantments.getCritChance(stack);
        double critDamage = ModEnchantments.getCritDamageBonus(stack);

        if (critChance <= 0.0D && critDamage <= 0.0D)
        {
            return;
        }

        List<Component> tooltip = event.getToolTip();

        if (critChance > 0.0D)
        {
            tooltip.add(Component.translatable("tooltip.tutorial_mod.crit_chance", toPercent(critChance))
                    .withStyle(ChatFormatting.YELLOW));
        }
        if (critDamage > 0.0D)
        {
            tooltip.add(Component.translatable("tooltip.tutorial_mod.crit_damage", toPercent(critDamage))
                    .withStyle(ChatFormatting.RED));
        }
    }

    /** 0.3 -> "30%" */
    private static String toPercent(double ratio)
    {
        return Math.round(ratio * 100.0D) + "%";
    }
}
