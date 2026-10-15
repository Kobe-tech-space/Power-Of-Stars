package com.example.tutorial.client;
import com.example.tutorial.TutorialMod;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import vazkii.patchouli.api.PatchouliAPI;
@Mod.EventBusSubscriber(modid=TutorialMod.MOD_ID,value=Dist.CLIENT)
public final class FeaturesSmokeVerifier {
    private static int ticks;
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent e) {
        var mc=Minecraft.getInstance();if(!Boolean.getBoolean("tutorial_mod.verifyFeatures") || e.phase!=TickEvent.Phase.END || mc.player==null) return;
        mc.options.pauseOnLostFocus=false;++ticks;
        if(ticks==95) {
            var sword=new net.minecraft.world.item.ItemStack(com.example.tutorial.item.ModItems.ICE_SWORD.get());sword.getOrCreateTag().putInt(com.example.tutorial.event.AstralEquipment.LEVEL,4);
            var armor=new net.minecraft.world.item.ItemStack(com.example.tutorial.item.ModItems.STAR_HELMET.get());armor.getOrCreateTag().putInt(com.example.tutorial.event.AstralEquipment.LEVEL,4);
            var swordLines=sword.getTooltipLines(mc.player,net.minecraft.world.item.TooltipFlag.Default.ADVANCED);
            var armorLines=armor.getTooltipLines(mc.player,net.minecraft.world.item.TooltipFlag.Default.ADVANCED);
            if(swordLines.stream().noneMatch(c->c.getString().contains("穿透 3")) || armorLines.stream().anyMatch(c->c.getString().contains("穿透")) || armorLines.stream().noneMatch(c->c.getString().contains("技能减耗贡献 3.2%")) || swordLines.stream().noneMatch(c->c.getString().contains("经验25级"))) throw new IllegalStateException("FEATURE_TOOLTIP: actual reinforcement stats disagree with tooltip");
            LogUtils.getLogger().info("FEATURE_TOOLTIP_PASS: weapon-only piercing, correct efficiency and next-level costs");
        }
        if(ticks==100) {mc.getToasts().clear();PatchouliAPI.get().openBookEntry(ResourceLocation.fromNamespaceAndPath(TutorialMod.MOD_ID,"astral_guide"),ResourceLocation.fromNamespaceAndPath(TutorialMod.MOD_ID,"reference/feature_24"),0);}
        if(ticks==115) Screenshot.grab(mc.gameDirectory,"reinforcement-guide.png",mc.getMainRenderTarget(),c->LogUtils.getLogger().info("FEATURE_SCREENSHOT: {}",c.getString()));
        if(ticks==130) {mc.setScreen(new net.minecraft.client.gui.screens.achievement.StatsScreen(null,mc.player.getStats()));}
        if(ticks==140) for(var child:mc.screen.children()) if(child instanceof net.minecraft.client.gui.components.AbstractSelectionList<?> list) {
            for(int i=0;i<list.children().size();i++) {
                Object entry=list.children().get(i);
                for(var field:entry.getClass().getDeclaredFields()) if(field.getType()==net.minecraft.stats.Stat.class) {
                    try {field.setAccessible(true);var stat=(net.minecraft.stats.Stat<?>)field.get(entry);
                        if(stat.getValue() instanceof ResourceLocation id && id.getNamespace().equals(TutorialMod.MOD_ID)) {list.setScrollAmount(Math.max(0,(i-1)*10));return;}
                    } catch(ReflectiveOperationException ex) {throw new IllegalStateException(ex);}
                }
            }
        }
        if(ticks==145) {Screenshot.grab(mc.gameDirectory,"astral-statistics.png",mc.getMainRenderTarget(),c->LogUtils.getLogger().info("FEATURE_SCREENSHOT: {}",c.getString()));LogUtils.getLogger().info("FEATURE_RENDER_PASS: new guide entry and original statistics UI");}
        if(ticks==155) mc.setScreen(new net.minecraft.client.gui.screens.advancements.AdvancementsScreen(mc.player.connection.getAdvancements()));
        if(ticks==165) Screenshot.grab(mc.gameDirectory,"astral-advancements.png",mc.getMainRenderTarget(),c->LogUtils.getLogger().info("FEATURE_ADVANCEMENT_SCREENSHOT: {}",c.getString()));
        if(ticks==185) mc.stop();
    }
}
