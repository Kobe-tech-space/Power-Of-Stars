package com.example.tutorial.client;
import com.example.tutorial.TutorialMod;
import com.example.tutorial.item.StarArmorItem;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.material.FogType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.client.event.RenderBlockScreenEffectEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = TutorialMod.MOD_ID, value = Dist.CLIENT)
public final class AstralVision
{
    private static boolean enabled()
    {
        var player = Minecraft.getInstance().player;
        return player != null && StarArmorItem.hasFullSet(player);
    }
    @SubscribeEvent public static void fog(ViewportEvent.RenderFog event)
    {
        if (event.getCamera().getFluidInCamera() != FogType.LAVA || !enabled()) return;
        event.setNearPlaneDistance(.25F);
        event.setFarPlaneDistance(Math.min(64, Minecraft.getInstance().options.getEffectiveRenderDistance() * 16F));
        event.setCanceled(true);
    }
    @SubscribeEvent public static void color(ViewportEvent.ComputeFogColor event)
    {
        if (event.getCamera().getFluidInCamera() != FogType.LAVA || !enabled()) return;
        event.setRed(.10F); event.setGreen(.055F); event.setBlue(.035F);
    }
    @SubscribeEvent public static void fire(RenderBlockScreenEffectEvent event)
    {
        if (event.getOverlayType() == RenderBlockScreenEffectEvent.OverlayType.FIRE
                && event.getPlayer().isInLava() && enabled()) event.setCanceled(true);
    }
}
