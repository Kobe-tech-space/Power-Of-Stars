package com.example.tutorial.event;
import com.example.tutorial.TutorialMod;
import com.example.tutorial.entity.AstralSlashEntity;
import com.example.tutorial.damage.ModDamageTypes;
import com.example.tutorial.item.ModItems;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.*;
import net.minecraftforge.registries.*;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.*;
import net.minecraftforge.eventbus.api.*;
import net.minecraftforge.fml.common.Mod;
import java.util.*;
@Mod.EventBusSubscriber(modid=TutorialMod.MOD_ID)
public final class AstralStatistics {
    public static final DeferredRegister<ResourceLocation> STATS=DeferredRegister.create(Registries.CUSTOM_STAT,TutorialMod.MOD_ID);
    public static final List<String> KEYS=List.of("slashes_fired","slash_hits","slash_kills","melee_kills","highest_combo","max_critical_damage","energy_gained","energy_spent","shield_absorbed","skills_used","perfect_parries");
    private static final Map<String,RegistryObject<ResourceLocation>> VALUES=new LinkedHashMap<>();
    static {for(String key:KEYS) VALUES.put(key,STATS.register(key,()->ResourceLocation.fromNamespaceAndPath(TutorialMod.MOD_ID,key)));}
    public static void initialize() {for(var v:VALUES.values()) Stats.CUSTOM.get(v.get(),StatFormatter.DEFAULT);}
    public static int value(ServerPlayer p,String key) {return p.getStats().getValue(Stats.CUSTOM,VALUES.get(key).get());}
    public static void add(ServerPlayer p,String key,int amount) {if(amount>0) p.awardStat(VALUES.get(key).get(),amount);}
    public static void maximum(ServerPlayer p,String key,int value) {if(value>value(p,key)) p.getStats().setValue(p,Stats.CUSTOM.get(VALUES.get(key).get()),value);}
    public static void skill(ServerPlayer p,String id) {add(p,"skills_used",1);AstralProgress.award(p,id);}
    @SubscribeEvent public static void join(EntityJoinLevelEvent e) {
        if(!e.getLevel().isClientSide() && e.getEntity() instanceof AstralSlashEntity s && s.getOwner() instanceof ServerPlayer p && !s.getPersistentData().getBoolean("tutorial_mod:counted")) {s.getPersistentData().putBoolean("tutorial_mod:counted",true);add(p,"slashes_fired",1);}
    }
    @SubscribeEvent(priority=EventPriority.LOWEST) public static void damage(LivingDamageEvent e) {
        if(e.getAmount()<=0 || !(e.getSource().getEntity() instanceof ServerPlayer p)) return;
        if(ModDamageTypes.isAstralSlash(e.getSource()) && e.getSource().getDirectEntity() instanceof AstralSlashEntity wave) {
            if(wave.claimStatisticsHit(e.getEntity().getUUID())) {add(p,"slash_hits",1); AstralProgress.award(p,"slash_hit");}
            float total = wave.addActualDamage(e.getEntity(),e.getAmount());
            if(wave.isCritical()) {
                maximum(p,"max_critical_damage",(int)Math.min(Integer.MAX_VALUE,Math.ceil(total)));
                AstralProgress.award(p,"critical_slash");
            }
        }
    }
    public static void critical(ServerPlayer p,LivingDamageEvent e) {maximum(p,"max_critical_damage",(int)Math.min(Integer.MAX_VALUE,Math.ceil(Math.min(e.getAmount(),e.getEntity().getHealth()))));if(e.getSource().getDirectEntity() instanceof AstralSlashEntity) AstralProgress.award(p,"critical_slash");}
    public static void absorb(ServerPlayer p,float amount) {
        if(!Float.isFinite(amount) || amount<=0) return;
        String key="tutorial_mod:shield_absorb_fraction";
        double total=Math.max(0,p.getPersistentData().getDouble(key))+amount;
        int whole=(int)Math.min(Integer.MAX_VALUE,Math.floor(total));p.getPersistentData().putDouble(key,total-whole);add(p,"shield_absorbed",whole);
    }
    @SubscribeEvent(priority=EventPriority.LOWEST) public static void death(LivingDeathEvent e) {
        if(e.isCanceled() || !(e.getSource().getEntity() instanceof ServerPlayer p)) return;
        if(e.getSource().getDirectEntity() instanceof AstralSlashEntity wave) {add(p,"slash_kills",1);wave.recordKill(p);}
        else if(ModDamageTypes.isDirectMelee(e.getSource()) && p.getMainHandItem().is(ModItems.ICE_SWORD.get())) add(p,"melee_kills",1);
        if(p.getHealth()<=1 && (e.getSource().getDirectEntity() instanceof AstralSlashEntity || p.getMainHandItem().is(ModItems.ICE_SWORD.get()))) AstralProgress.award(p,"last_stand");
        if(e.getEntity().getPersistentData().getBoolean("tutorial_mod:astral_elite")) AstralProgress.award(p,"elite");
    }
    private AstralStatistics() {}
}
