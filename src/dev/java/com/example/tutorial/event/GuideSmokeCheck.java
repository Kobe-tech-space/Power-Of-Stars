package com.example.tutorial.event;

import com.example.tutorial.TutorialMod;
import com.example.tutorial.item.ModItems;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Development-only check in an isolated test world. */
@Mod.EventBusSubscriber(modid = TutorialMod.MOD_ID)
public final class GuideSmokeCheck
{
    private GuideSmokeCheck() {}

    @SubscribeEvent
    public static void tick(TickEvent.PlayerTickEvent event)
    {
        if (!Boolean.getBoolean("tutorial_mod.verifyGuide") || event.phase != TickEvent.Phase.END
                || !(event.player instanceof ServerPlayer player)) return;
        if (player.tickCount == 40)
        {
            player.setItemSlot(EquipmentSlot.MAINHAND, ModItems.ASTRAL_GUIDE.get().getDefaultInstance());
            player.containerMenu.broadcastChanges();
        }
        if (player.tickCount == 50)
            player.getMainHandItem().use(player.level(), player, InteractionHand.MAIN_HAND);
    }
}
