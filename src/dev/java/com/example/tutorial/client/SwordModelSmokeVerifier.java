package com.example.tutorial.client;

import com.example.tutorial.TutorialMod;
import com.example.tutorial.item.ModItems;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Asset-only smoke check, exclusively in the disposable development world. */
@Mod.EventBusSubscriber(modid=TutorialMod.MOD_ID,value=Dist.CLIENT)
public final class SwordModelSmokeVerifier {
    private static int ticks;
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent e) {
        var mc=Minecraft.getInstance();
        if(!Boolean.getBoolean("tutorial_mod.verifySword") || e.phase!=TickEvent.Phase.END || mc.player==null) return;
        mc.options.pauseOnLostFocus=false;
        ++ticks;
        if(ticks==20) {
            mc.getSingleplayerServer().execute(() -> {
                var p=mc.getSingleplayerServer().getPlayerList().getPlayer(mc.player.getUUID());
                if(p==null) throw new IllegalStateException("Missing test player");
                p.setGameMode(GameType.CREATIVE);
                p.getInventory().selected=0;
                p.getInventory().setItem(0,new ItemStack(ModItems.ICE_SWORD.get()));
                p.connection.teleport(0,201,0,0,0);
                p.serverLevel().setDayTime(6000);
            });
            mc.player.getInventory().selected=0;
        }
        if(ticks==60) {
            var baked=mc.getItemRenderer().getModel(new ItemStack(ModItems.ICE_SWORD.get()),mc.level,mc.player,0);
            if(baked==mc.getModelManager().getMissingModel()) throw new IllegalStateException("Sword model missing");
            Screenshot.grab(mc.gameDirectory,"stellar-sword-firstperson.png",mc.getMainRenderTarget(),c->LogUtils.getLogger().info("SWORD_SCREENSHOT: {}",c.getString()));
        }
        if(ticks==70) mc.setScreen(new InventoryScreen(mc.player));
        if(ticks==85) {
            Screenshot.grab(mc.gameDirectory,"stellar-sword-inventory.png",mc.getMainRenderTarget(),c->LogUtils.getLogger().info("SWORD_SCREENSHOT: {}",c.getString()));
            LogUtils.getLogger().info("SWORD_MODEL_PASS: baked item, first-person and inventory render");
        }
        if(ticks==90) mc.setScreen(new net.minecraft.client.gui.screens.Screen(net.minecraft.network.chat.Component.literal("Sword detail")) {
            @Override public void render(net.minecraft.client.gui.GuiGraphics g,int x,int y,float partial) {
                renderBackground(g);
                float scale=Math.min(width,height)*.85F/16;
                g.pose().pushPose();g.pose().translate((width-16*scale)/2,(height-16*scale)/2,0);g.pose().scale(scale,scale,scale);
                g.renderItem(new ItemStack(ModItems.ICE_SWORD.get()),0,0);g.pose().popPose();
                super.render(g,x,y,partial);
            }
            @Override public boolean isPauseScreen(){return false;}
        });
        if(ticks==100) Screenshot.grab(mc.gameDirectory,"stellar-sword-model-detail.png",mc.getMainRenderTarget(),c->LogUtils.getLogger().info("SWORD_DETAIL_SCREENSHOT: {}",c.getString()));
        if(ticks==110) mc.stop();
    }
}
