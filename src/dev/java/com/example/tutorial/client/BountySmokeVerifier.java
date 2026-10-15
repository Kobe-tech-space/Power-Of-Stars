package com.example.tutorial.client;
import com.example.tutorial.TutorialMod;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
@Mod.EventBusSubscriber(modid=TutorialMod.MOD_ID,value=Dist.CLIENT)
public final class BountySmokeVerifier {
    private static int ticks;
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent e) {
        var mc=Minecraft.getInstance();
        if(!Boolean.getBoolean("tutorial_mod.verifyBounties") || e.phase!=TickEvent.Phase.END || mc.player==null) return;
        mc.options.pauseOnLostFocus=false; ++ticks;
        if(ticks==10) {mc.options.setCameraType(net.minecraft.client.CameraType.FIRST_PERSON); mc.player.setXRot(15);}
        if(ticks==80) mc.getToasts().clear();
        if(ticks==85) { if(!(mc.screen instanceof BountyScreen)) throw new IllegalStateException("Bounty network screen missing"); capture(mc,"bounty-board.png"); }
        if(ticks==95 && mc.screen instanceof BountyScreen screen) {
            int w=mc.getWindow().getGuiScaledWidth(), h=mc.getWindow().getGuiScaledHeight(), ph=Math.min(290,h-16);
            screen.mouseClicked((w-Math.min(440,w-16))/2+25,(h-ph)/2+55+9*Math.max(12,(ph-76)/10)+2,0);
        }
        if(ticks==105) capture(mc,"bounty-mythic.png");
        if(ticks==108 && mc.screen instanceof BountyScreen screen) {
            for (var child:screen.children()) if(child instanceof net.minecraft.client.gui.components.Button b && b.getMessage().getString().equals("接取悬赏")) b.onPress();
        }
        if(ticks==111 && mc.screen instanceof BountyScreen screen) {
            int w=mc.getWindow().getGuiScaledWidth(), h=mc.getWindow().getGuiScaledHeight(), ph=Math.min(290,h-16);
            screen.mouseClicked((w-Math.min(440,w-16))/2+25,(h-ph)/2+55+7*Math.max(12,(ph-76)/10)+2,0);
        }
        if(ticks==112 && mc.screen instanceof BountyScreen screen) {
            for (var child:screen.children()) if(child instanceof net.minecraft.client.gui.components.Button b && b.getMessage().getString().equals("领取奖励")) b.onPress();
        }
        if(ticks==113) com.example.tutorial.network.ModNetwork.sendToServer(new com.example.tutorial.network.BountyActionPacket(7,2));
        if(ticks==115) mc.setScreen(null);
        if(ticks==147) {capture(mc,"dark-purple-slash.png"); LogUtils.getLogger().info("BOUNTY_RENDER_PASS: network screen, mythic preview, slash and stars");}
        if(ticks==175) mc.stop();
    }
    private static void capture(Minecraft mc,String name) {Screenshot.grab(mc.gameDirectory,name,mc.getMainRenderTarget(),c->LogUtils.getLogger().info("BOUNTY_SCREENSHOT: {}",c.getString()));}
}
