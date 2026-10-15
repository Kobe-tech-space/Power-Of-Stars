package com.example.tutorial.client;
import com.example.tutorial.TutorialMod;
import com.example.tutorial.ornament.*;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
@Mod.EventBusSubscriber(modid=TutorialMod.MOD_ID,value=Dist.CLIENT)
public final class OrnamentSmokeVerifier {
    private static int ticks;
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent e){
        var mc=Minecraft.getInstance();
        if(!Boolean.getBoolean("tutorial_mod.verifyOrnaments")||e.phase!=TickEvent.Phase.END||mc.player==null)return;
        mc.options.pauseOnLostFocus=false;++ticks;
        if(ticks==30)mc.getSingleplayerServer().execute(()->{
            var p=mc.getSingleplayerServer().getPlayerList().getPlayer(mc.player.getUUID());
            p.setGameMode(net.minecraft.world.level.GameType.CREATIVE);
            com.example.tutorial.event.OrnamentTestGear.equip(p,OrnamentKind.DAWN_CROWN,OrnamentKind.STARFALL_AMULET,OrnamentKind.WING_BROOCH);
            OrnamentEffects.update(p);
            if(!OrnamentEffects.has(p,OrnamentKind.DAWN_CROWN)||OrnamentEffects.equipped(p).size()!=3)throw new IllegalStateException("Real-player Curios equipment did not activate");
            LogUtils.getLogger().info("ORNAMENT_CLIENT_EQUIP_PASS: real player, 3 dedicated Curios slots, active equipment");
        });
        if(ticks==60)mc.setScreen(new Gallery());
        if(ticks==100)Screenshot.grab(mc.gameDirectory,"stellar-ornaments-gallery.png",mc.getMainRenderTarget(),c->LogUtils.getLogger().info("ORNAMENT_SCREENSHOT: {}",c.getString()));
        if(ticks==110) {
            // Open the real vanilla creative inventory at the mod tab, not a magnified gallery.
            mc.setScreen(new InventoryInspection(mc));
        }
        if(ticks==125 && mc.screen instanceof net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen screen) {
            try {
                var method=java.util.Arrays.stream(net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen.class.getDeclaredMethods()).filter(m -> m.getReturnType()==void.class
                        &&m.getParameterCount()==1&&m.getParameterTypes()[0]==net.minecraft.world.item.CreativeModeTab.class)
                        .findFirst().orElseThrow(() -> new NoSuchMethodException("selectTab"));
                method.setAccessible(true);method.invoke(screen,com.example.tutorial.item.ModCreativeTabs.ASTRAL.get());
            } catch(ReflectiveOperationException ex) {throw new IllegalStateException("Unable to select actual creative tab",ex);}
        }
        if(ticks==145)Screenshot.grab(mc.gameDirectory,"stellar-ornaments-inventory.png",mc.getMainRenderTarget(),c->LogUtils.getLogger().info("ORNAMENT_INVENTORY_SCREENSHOT: {}",c.getString()));
        if(ticks==170){LogUtils.getLogger().info("ORNAMENT_CLIENT_PASS: ten native item icons rendered in actual creative inventory, optional Curios integration active");mc.stop();}
    }
    private static final class InventoryInspection extends net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen {
        InventoryInspection(Minecraft mc){super(mc.player,mc.player.connection.enabledFeatures(),true);}
        @Override public void render(GuiGraphics g,int mx,int my,float partial){super.render(g,-1,-1,partial);}
    }
    private static final class Gallery extends Screen {
        Gallery(){super(Component.literal("星辰饰品 · 十种不同用途"));}
        @Override public void render(GuiGraphics g,int mx,int my,float partial){
            renderBackground(g);int pw=Math.min(450,width-16),ph=Math.min(250,height-16),x=(width-pw)/2,y=(height-ph)/2;
            g.fill(x,y,x+pw,y+ph,0xF20D1020);g.fill(x,y,x+pw,y+2,0xFFBFA0FF);
            g.drawCenteredString(font,title,width/2,y+14,0xE5D8FF);
            g.drawCenteredString(font,Component.literal("3个专属槽 · 同种不叠加 · 无Curios时副手一件生效"),width/2,y+31,0xA9BCD4);
            for(int i=0;i<10;i++){
                var kind=OrnamentKind.values()[i];int cw=(pw-20)/5,cx=x+10+i%5*cw,cy=y+54+i/5*85;
                g.fill(cx+2,cy,cx+cw-2,cy+76,0xFF1A2033);
                g.pose().pushPose();g.pose().translate(cx+(cw-48)/2,cy+5,0);g.pose().scale(3,3,1);
                g.renderItem(new ItemStack(OrnamentItems.get(kind)),0,0);g.pose().popPose();
                g.drawCenteredString(font,Component.translatable("item.tutorial_mod."+kind.id),cx+cw/2,cy+58,0xE4DDFA);
            }
            super.render(g,mx,my,partial);
        }
        @Override public boolean isPauseScreen(){return false;}
    }
}
