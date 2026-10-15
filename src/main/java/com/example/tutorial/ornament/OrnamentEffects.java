package com.example.tutorial.ornament;
import com.example.tutorial.TutorialMod;
import com.example.tutorial.config.ModSettings;
import com.example.tutorial.event.AstralSafety;
import com.example.tutorial.item.StellarOrnamentItem;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.effect.*;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.*;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.*;
import java.util.*;
import java.nio.charset.StandardCharsets;

@Mod.EventBusSubscriber(modid=TutorialMod.MOD_ID)
public final class OrnamentEffects {
    public static Set<OrnamentKind> equipped(ServerPlayer p){
        if(!ModSettings.enabled("ornaments.enabled")||!p.isAlive()||p.isSpectator())return EnumSet.noneOf(OrnamentKind.class);
        if(ModList.get().isLoaded("curios"))return com.example.tutorial.compat.CuriosOrnaments.equipped(p);
        return p.getOffhandItem().getItem() instanceof StellarOrnamentItem item?EnumSet.of(item.kind()):EnumSet.noneOf(OrnamentKind.class);
    }
    public static boolean has(ServerPlayer p,OrnamentKind kind){return equipped(p).contains(kind);}
    public static double damageBonus(ServerPlayer p){return has(p,OrnamentKind.ORBIT_RING)?.05:0;}
    public static int extraEnergy(ServerPlayer p,int base){
        if(base<=0||!has(p,OrnamentKind.STARFALL_AMULET))return 0;
        var data=p.getPersistentData();double fraction=data.getDouble("tutorial_mod:ornament_energy_fraction");
        if(!Double.isFinite(fraction)||fraction<0||fraction>=1)fraction=0;
        double total=Math.min(1000000,base)*.10+fraction;
        int extra=(int)Math.floor(total+1.0E-9);
        data.putDouble("tutorial_mod:ornament_energy_fraction",Math.max(0,Math.min(.999999999,total-extra)));
        return extra;
    }
    private static void modifier(ServerPlayer p,Attribute type,String name,double amount,AttributeModifier.Operation operation){
        var attribute=p.getAttribute(type);if(attribute==null)return;
        var id=UUID.nameUUIDFromBytes(("tutorial_mod:ornament:"+name).getBytes(StandardCharsets.UTF_8));
        var existing=attribute.getModifier(id);
        if(existing!=null&&existing.getAmount()==amount&&existing.getOperation()==operation)return;
        if(existing!=null)attribute.removeModifier(id);
        if(amount!=0)attribute.addTransientModifier(new AttributeModifier(id,name,amount,operation));
    }
    public static void update(ServerPlayer p){
        var kinds=equipped(p);
        modifier(p,Attributes.MAX_HEALTH,"crown",kinds.contains(OrnamentKind.DAWN_CROWN)?2:0,AttributeModifier.Operation.ADDITION);
        modifier(p,Attributes.ATTACK_SPEED,"bracelet",kinds.contains(OrnamentKind.CONSTELLATION_BRACELET)?.05:0,AttributeModifier.Operation.MULTIPLY_BASE);
        modifier(p,Attributes.KNOCKBACK_RESISTANCE,"badge",kinds.contains(OrnamentKind.ASTRAL_BADGE)?.05:0,AttributeModifier.Operation.ADDITION);
        if(p.getHealth()>p.getMaxHealth())p.setHealth(p.getMaxHealth());
    }
    @SubscribeEvent public static void tick(TickEvent.PlayerTickEvent e){
        if(e.phase!=TickEvent.Phase.END||!(e.player instanceof ServerPlayer p)||!AstralSafety.valid(p)||p.tickCount%20!=0)return;
        update(p);var kinds=equipped(p);long now=p.server.overworld().getGameTime();
        if(kinds.contains(OrnamentKind.CRESCENT_EARRING)&&p.tickCount%100==0){
            var existing=p.getEffect(MobEffects.NIGHT_VISION);
            if(existing==null||existing.getDuration()<120)p.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION,220,0,true,false,true));
        }
        if(kinds.contains(OrnamentKind.TWILIGHT_TALISMAN)&&p.tickCount%200==0&&p.getFoodData().getFoodLevel()>=16
                &&now-p.getPersistentData().getLong("tutorial_mod:ornament_last_combat")>=100)p.heal(1);
    }
    @SubscribeEvent public static void equipment(LivingEquipmentChangeEvent e){if(e.getEntity() instanceof ServerPlayer p)update(p);}
    @SubscribeEvent public static void respawn(PlayerEvent.PlayerRespawnEvent e){if(e.getEntity() instanceof ServerPlayer p)update(p);}
    @SubscribeEvent public static void login(PlayerEvent.PlayerLoggedInEvent e){if(e.getEntity() instanceof ServerPlayer p)update(p);}
    @SubscribeEvent public static void fall(LivingFallEvent e){
        if(e.getEntity() instanceof ServerPlayer p&&has(p,OrnamentKind.WING_BROOCH))e.setDamageMultiplier(e.getDamageMultiplier()*.8F);
    }
    @SubscribeEvent public static void mining(PlayerEvent.BreakSpeed e){
        if(e.getEntity() instanceof ServerPlayer p&&has(p,OrnamentKind.ASTRAL_COMPASS)&&e.getState().is(net.minecraftforge.common.Tags.Blocks.ORES)
                &&Float.isFinite(e.getNewSpeed())&&e.getNewSpeed()>0)e.setNewSpeed(Math.min(100000,e.getNewSpeed()*1.10F));
    }
    @SubscribeEvent(priority=EventPriority.LOWEST) public static void combat(LivingDamageEvent e){
        if(e.isCanceled()||e.getAmount()<=0||!Float.isFinite(e.getAmount()))return;
        if(e.getEntity() instanceof ServerPlayer p)p.getPersistentData().putLong("tutorial_mod:ornament_last_combat",p.server.overworld().getGameTime());
        if(e.getSource().getEntity() instanceof ServerPlayer p)p.getPersistentData().putLong("tutorial_mod:ornament_last_combat",p.server.overworld().getGameTime());
    }
    @SubscribeEvent(priority=EventPriority.LOWEST) public static void kill(LivingDeathEvent e){
        if(e.isCanceled()||!(e.getEntity() instanceof Monster)||!(e.getSource().getEntity() instanceof ServerPlayer p)
                ||!AstralSafety.valid(p)||!has(p,OrnamentKind.METEOR_PENDANT)||!ModSettings.enabled("repair.enabled"))return;
        var data=p.getPersistentData();long now=p.server.overworld().getGameTime();
        if(data.contains("tutorial_mod:meteor_repair")&&now>=data.getLong("tutorial_mod:meteor_repair")&&now-data.getLong("tutorial_mod:meteor_repair")<200)return;
        var stack=p.getMainHandItem();
        if(!stack.isDamageableItem()||!stack.isDamaged()||(!ModSettings.enabled("repair.stackWithMending")&&stack.getEnchantmentLevel(Enchantments.MENDING)>0))return;
        data.putLong("tutorial_mod:meteor_repair",now);
        if(p.getRandom().nextFloat()<.20F)stack.setDamageValue(Math.max(0,stack.getDamageValue()-2));
    }
    @SubscribeEvent public static void clone(PlayerEvent.Clone e){
        for(String key:new String[]{"tutorial_mod:meteor_repair","tutorial_mod:ornament_last_combat","tutorial_mod:ornament_energy_fraction"}){
            var tag=e.getOriginal().getPersistentData().get(key);if(tag!=null)e.getEntity().getPersistentData().put(key,tag.copy());
        }
    }
    private OrnamentEffects(){}
}
