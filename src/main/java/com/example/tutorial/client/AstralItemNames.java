package com.example.tutorial.client;
import com.example.tutorial.TutorialMod;
import com.example.tutorial.config.ModSettings;
import com.example.tutorial.event.*;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.*;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
@Mod.EventBusSubscriber(modid=TutorialMod.MOD_ID,value=Dist.CLIENT)
public final class AstralItemNames {
    @SubscribeEvent public static void tooltip(ItemTooltipEvent e) {
        var s=e.getItemStack(); if(!AstralEquipment.isEquipment(s) || e.getToolTip().isEmpty()) return;
        int level=AstralEquipment.level(s);
        var viewer=Minecraft.getInstance().player;
        boolean details=e.getFlags().isAdvanced() || viewer!=null && viewer.isCreative();
        if(level>0 || details) {
            e.getToolTip().add(Component.literal("强化 +"+level+" / "+ModSettings.i("reinforcement.maxLevel")+" · 耐久上限 +"+(level*10)+"%").withStyle(net.minecraft.ChatFormatting.GRAY));
            e.getToolTip().add(Component.literal("技能减耗贡献 "+String.format(java.util.Locale.ROOT,"%.1f",level*.8)+"% · 总上限20%，领域除外").withStyle(net.minecraft.ChatFormatting.GRAY));
            if(s.is(com.example.tutorial.item.ModItems.ICE_SWORD.get())) e.getToolTip().add(Component.literal("剑气最多穿透 "+(1+level/2)+" 个目标").withStyle(net.minecraft.ChatFormatting.LIGHT_PURPLE));
            if(details && level<ModSettings.i("reinforcement.maxLevel")) {
                int next=level+1;
                e.getToolTip().add(Component.literal("下一级：星锭×"+(1<<next)+"、钻石×"+(next*2)+"、经验"+(next*5)+"级"+(next>=4?"、奇点×1":"")).withStyle(net.minecraft.ChatFormatting.DARK_GRAY));
            }
        }
        if(!ModSettings.enabled("names.dynamic") || s.hasCustomHoverName()) return;
        int energy=ClientCombatState.starEnergy(), max=ClientCombatState.maximumStarEnergy(),color=0x6FA9EC;
        if(energy>=50) color=0xB58ADD;
        if(max>0 && energy>=max) {double wave=(Math.sin(net.minecraft.Util.getMillis()/900.0)+1)/2;color=((int)(110+60*wave)<<16)|((int)(140-35*wave)<<8)|235;}
        if(s.getItem() instanceof com.example.tutorial.item.StarArmorItem && ClientCombatState.shieldFlash() && ClientCombatState.stellarShield()==0) color=0x55536B;
        var name=e.getToolTip().get(0).copy();
        if(s.is(com.example.tutorial.item.ModItems.ICE_SWORD.get()) && WeaponMastery.level(s)>=WeaponMastery.MAX_LEVEL) name=Component.literal("星主 · ").append(name);
        e.getToolTip().set(0,name.withStyle(Style.EMPTY.withColor(color)));
    }
    private AstralItemNames() {}
}
