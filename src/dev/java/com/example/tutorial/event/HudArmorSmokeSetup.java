package com.example.tutorial.event;

import com.example.tutorial.TutorialMod;
import com.example.tutorial.item.ModItems;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = TutorialMod.MOD_ID)
public final class HudArmorSmokeSetup
{
    @SubscribeEvent
    public static void tick(TickEvent.PlayerTickEvent event)
    {
        if (!Boolean.getBoolean("tutorial_mod.verifyHud") || event.phase != TickEvent.Phase.END
                || !(event.player instanceof ServerPlayer player) || player.tickCount != 30) return;
        player.setItemSlot(EquipmentSlot.HEAD, ModItems.STAR_HELMET.get().getDefaultInstance());
        player.setItemSlot(EquipmentSlot.CHEST, ModItems.STAR_CHESTPLATE.get().getDefaultInstance());
        player.setItemSlot(EquipmentSlot.LEGS, ModItems.STAR_LEGGINGS.get().getDefaultInstance());
        player.setItemSlot(EquipmentSlot.FEET, ModItems.STAR_BOOTS.get().getDefaultInstance());
        player.setItemSlot(EquipmentSlot.MAINHAND, ModItems.ICE_SWORD.get().getDefaultInstance());
        player.containerMenu.broadcastChanges();
        StarEnergy.set(player, 78);
    }
}
