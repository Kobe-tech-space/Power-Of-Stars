package com.example.tutorial.client;
import com.example.tutorial.TutorialMod;
import com.example.tutorial.effect.ModEffects;
import com.example.tutorial.item.StarArmorItem;
import net.minecraft.client.Minecraft;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Eliminate client-only darkness/negative-effect visuals while the server-synced status is active. */
@Mod.EventBusSubscriber(modid=TutorialMod.MOD_ID, value=Dist.CLIENT)
public final class DominionEffectGuard
{
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event)
    {
        var player=Minecraft.getInstance().player;
        if (event.phase != TickEvent.Phase.END || player == null || StarArmorItem.fullSetTier(player) != 2
                || !player.hasEffect(ModEffects.DOMINION.get())) return;
        for (var effect : java.util.List.copyOf(player.getActiveEffects()))
            if (effect.getEffect().getCategory() != MobEffectCategory.BENEFICIAL) player.removeEffect(effect.getEffect());
    }
}
