package com.example.tutorial.client;
import com.example.tutorial.TutorialMod;
import com.example.tutorial.entity.AstralSlashEntity;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
@Mod.EventBusSubscriber(modid=TutorialMod.MOD_ID,value=Dist.CLIENT)
public final class SlashVisibilityVerifier {
    private static int ticks;
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent e) {
        var mc=Minecraft.getInstance();
        if(!Boolean.getBoolean("tutorial_mod.verifySlash") || e.phase!=TickEvent.Phase.END || mc.player==null)return;
        mc.options.pauseOnLostFocus=false;++ticks;
        if(ticks==20)mc.getSingleplayerServer().execute(()->{
            var p=mc.getSingleplayerServer().getPlayerList().getPlayer(mc.player.getUUID());
            p.setGameMode(GameType.CREATIVE);p.connection.teleport(0,201,0,0,0);p.serverLevel().setDayTime(6000);
        });
        if(ticks==40 || ticks==85)mc.getSingleplayerServer().execute(()->{
            var p=mc.getSingleplayerServer().getPlayerList().getPlayer(mc.player.getUUID());
            p.serverLevel().setDayTime(ticks<85?6000:18000);
            var wave=new AstralSlashEntity(p.serverLevel(),p,1,false,false,new Vec3(0,0,1),.005F);
            wave.setPos(p.getX(),p.getEyeY(),p.getZ()+8);p.serverLevel().addFreshEntity(wave);
        });
        if(ticks==60 || ticks==105) Screenshot.grab(mc.gameDirectory,ticks==60?"slash-visible-day.png":"slash-visible-night.png",mc.getMainRenderTarget(),c->LogUtils.getLogger().info("SLASH_VISIBLE_SCREENSHOT: {}",c.getString()));
        if(ticks==120){LogUtils.getLogger().info("SLASH_VISIBILITY_PASS: bright daylight and night render");mc.stop();}
    }
}
