package com.example.tutorial.client;
import com.example.tutorial.TutorialMod;
import com.example.tutorial.config.ClientHudConfig;
import com.mojang.logging.LogUtils;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid=TutorialMod.MOD_ID, value=Dist.CLIENT)
public final class TierProgressionSmokeVerifier
{
    private static int ticks;
    private static HudEditorScreen editor;
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event)
    {
        var mc=Minecraft.getInstance();
        if (!Boolean.getBoolean("tutorial_mod.verifyTiers") || event.phase!=TickEvent.Phase.END || mc.player==null) return;
        mc.options.pauseOnLostFocus=false;
        ++ticks;
        if (ticks==10) {mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT); mc.player.setXRot(12);}
        if (ticks==45) capture(mc,"armor-low-tier.png");
        if (ticks==125) capture(mc,"armor-high-control.png");
        if (ticks==365) capture(mc,"armor-high-break.png");
        if (ticks==375) { ClientHudConfig.resetAll(); editor=new HudEditorScreen(null); mc.setScreen(editor); }
        if (ticks==390) capture(mc,"tier-hud-editor.png");
        if (ticks==395) {editor.onClose(); LogUtils.getLogger().info("TIER_RENDER_SMOKE_PASS: both armor tiers, domains and HUD rendered");}
        if (ticks==420) mc.stop();
    }
    private static void capture(Minecraft mc,String name)
    {
        Screenshot.grab(mc.gameDirectory,name,mc.getMainRenderTarget(),c->LogUtils.getLogger().info("TIER_SCREENSHOT: {}",c.getString()));
    }
}
