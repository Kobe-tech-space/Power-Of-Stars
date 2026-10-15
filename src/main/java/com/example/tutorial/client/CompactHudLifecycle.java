package com.example.tutorial.client;
import com.example.tutorial.TutorialMod;
import com.example.tutorial.config.ClientHudConfig;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;

@Mod.EventBusSubscriber(modid=TutorialMod.MOD_ID, bus=Mod.EventBusSubscriber.Bus.MOD, value=Dist.CLIENT)
public final class CompactHudLifecycle
{
    @SubscribeEvent public static void loaded(ModConfigEvent.Loading event)
    {
        if (event.getConfig().getSpec() != ClientHudConfig.SPEC) return;
        Minecraft.getInstance().execute(() -> {
            if (ClientHudConfig.LAYOUT_VERSION.get() < 3) { ClientHudConfig.resetAll(); ClientHudConfig.SPEC.save(); }
        });
    }
}
