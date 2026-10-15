package com.example.tutorial.client;
import com.example.tutorial.TutorialMod;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
@Mod.EventBusSubscriber(modid=TutorialMod.MOD_ID,value=Dist.CLIENT)
public final class ClientResourceLifecycle {
    @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggingOut e) {ClientCombatState.reset();}
}
