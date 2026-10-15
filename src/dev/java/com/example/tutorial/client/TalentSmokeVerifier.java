package com.example.tutorial.client;
import com.example.tutorial.TutorialMod;
import com.example.tutorial.network.*;
import com.example.tutorial.talent.StellarTalents;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
@Mod.EventBusSubscriber(modid=TutorialMod.MOD_ID,value=Dist.CLIENT)
public final class TalentSmokeVerifier {
    private static int ticks;
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event) {
        var mc=Minecraft.getInstance();
        if(!Boolean.getBoolean("tutorial_mod.verifyTalents") || event.phase!=TickEvent.Phase.END || mc.player==null)return;
        mc.options.pauseOnLostFocus=false;++ticks;
        if(ticks==30)mc.getSingleplayerServer().execute(()->{
            var p=mc.getSingleplayerServer().getPlayerList().getPlayer(mc.player.getUUID());
            p.setGameMode(net.minecraft.world.level.GameType.CREATIVE);
            p.getPersistentData().remove("tutorial_mod:stellar_talents");
            StellarTalents.award(p,9300);
        });
        if(ticks==50)ModNetwork.sendToServer(new TalentActionPacket(0,-1));
        if(ticks==70) {
            if(!(mc.screen instanceof TalentScreen s))throw new IllegalStateException("Talent screen did not open through network");
            for(var child:s.children())if(child instanceof Button b&&b.getMessage().getString().equals(Component.translatable("talent.tutorial_mod.learn").getString()))b.onPress();
        }
        if(ticks==90) {
            Screenshot.grab(mc.gameDirectory,"stellar-talent-tree.png",mc.getMainRenderTarget(),c->LogUtils.getLogger().info("TALENT_SCREENSHOT: {}",c.getString()));
            mc.getSingleplayerServer().execute(()->{
                var p=mc.getSingleplayerServer().getPlayerList().getPlayer(mc.player.getUUID());
                if(StellarTalents.rank(p,0)!=1)throw new IllegalStateException("Network learning failed");
                LogUtils.getLogger().info("TALENT_CLIENT_PASS: screen opens, network purchase confirmed, constellation layout rendered");
            });
        }
        if(ticks==110&&mc.screen instanceof TalentScreen s) {
            int w=mc.getWindow().getGuiScaledWidth(),h=mc.getWindow().getGuiScaledHeight(),pw=Math.min(480,w-12),ph=Math.min(310,h-12);
            s.mouseClicked((w-pw)/2+12+(pw-24)/3*2+25,(h-ph)/2+64+2*Math.max(22,(ph-140)/4)+5,0);
        }
        if(ticks==125)Screenshot.grab(mc.gameDirectory,"stellar-talent-details.png",mc.getMainRenderTarget(),c->LogUtils.getLogger().info("TALENT_SCREENSHOT: {}",c.getString()));
        if(ticks==140)mc.stop();
    }
}
