package com.example.tutorial.event;

import com.example.tutorial.TutorialMod;
import com.example.tutorial.item.AstralGreatswordItem;
import com.example.tutorial.item.ModItems;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = TutorialMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class RangedModeGuard
{
    private RangedModeGuard()
    {
    }

    @SubscribeEvent
    public static void onAttackEntity(AttackEntityEvent event)
    {
        if (isRangedSword(event.getEntity().getMainHandItem()))
        {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event)
    {
        if (isRangedSword(event.getEntity().getMainHandItem()))
        {
            event.setCanceled(true);
        }
    }

    private static boolean isRangedSword(ItemStack stack)
    {
        return stack.is(ModItems.ICE_SWORD.get()) && AstralGreatswordItem.isRanged(stack);
    }
}
