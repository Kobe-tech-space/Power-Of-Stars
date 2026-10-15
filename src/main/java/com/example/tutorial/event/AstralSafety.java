package com.example.tutorial.event;
import com.example.tutorial.TutorialMod;
import com.example.tutorial.config.ModSettings;
import com.example.tutorial.entity.AstralSlashEntity;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.util.*;
@Mod.EventBusSubscriber(modid=TutorialMod.MOD_ID)
public final class AstralSafety {
    public static boolean valid(ServerPlayer p) {return p!=null && p.isAlive() && !p.isSpectator() && !(p instanceof FakePlayer) && p.serverLevel().hasChunkAt(p.blockPosition());}
    public static double damageBonus(LivingEntity e) {
        var attribute=e.getAttribute(com.example.tutorial.attribute.ModAttributes.DAMAGE_BONUS.get());
        double base = attribute==null || !Double.isFinite(attribute.getValue())?0:Math.max(0,Math.min(100,attribute.getValue()));
        return base + (e instanceof ServerPlayer p && p.getMainHandItem().getItem() instanceof com.example.tutorial.item.AstralGreatswordItem
                ? com.example.tutorial.talent.StellarTalents.damageBonus(p) + com.example.tutorial.ornament.OrnamentEffects.damageBonus(p) : 0);
    }
    public static boolean allowPacket(ServerPlayer p,boolean slash) {
        if(!valid(p)) return false;
        var data=p.getPersistentData(); String key="tutorial_mod:packet_"+(slash?"slash":"skill");
        long now=p.server.overworld().getGameTime(), start=data.getLong(key+"_start");
        if(now<start || now-start>=20) {data.putLong(key+"_start",now);data.putInt(key+"_count",0);}
        int used=data.getInt(key+"_count"), limit=ModSettings.i(slash?"network.slashPacketsPerSecond":"network.skillPacketsPerSecond");
        if(used>=limit) return false; data.putInt(key+"_count",used+1); return true;
    }
    public static boolean canHit(Entity owner,LivingEntity target) {
        if(owner!=null && (!owner.isAlive() || owner instanceof Player p && p.isSpectator())) return false;
        if(!target.isAlive() || target==owner || target instanceof net.minecraft.world.entity.decoration.ArmorStand || target instanceof FakePlayer) return false;
        if(target instanceof Player p && (p.isCreative() || p.isSpectator())) return false;
        if(target instanceof TamableAnimal pet && owner!=null && owner.getUUID().equals(pet.getOwnerUUID())) return false;
        if(owner instanceof Player p && target instanceof Player victim && !p.canHarmPlayer(victim)) return false;
        return owner==null || !owner.isAlliedTo(target) || ModSettings.enabled("combat.friendlyFire");
    }
    private record Budget(long tick,int used) {}
    private static final WeakHashMap<ServerLevel,Map<String,Long>> SOUNDS=new WeakHashMap<>();
    public static void sound(net.minecraft.world.level.Level raw,net.minecraft.core.BlockPos pos,net.minecraft.sounds.SoundEvent sound,net.minecraft.sounds.SoundSource source,float volume,float pitch) {
        if(!(raw instanceof ServerLevel level) || !ModSettings.enabled("audio.enabled")) return;
        long now=level.getGameTime();var map=SOUNDS.computeIfAbsent(level,k->new HashMap<>());
        map.entrySet().removeIf(e->now<e.getValue() || now-e.getValue()>100);
        String key=net.minecraft.world.level.ChunkPos.asLong(pos.getX()>>4,pos.getZ()>>4)+":"+sound.getLocation();
        if(map.containsKey(key) && now-map.get(key)<ModSettings.i("audio.minimumIntervalTicks")) return;
        map.put(key,now);level.playSound(null,pos,sound,source,Math.min(1.5F,volume),Math.max(.5F,Math.min(2,pitch)));
    }
    public static void notifySound(net.minecraft.world.entity.player.Player p,net.minecraft.sounds.SoundEvent sound,net.minecraft.sounds.SoundSource source,float volume,float pitch) {
        if(p instanceof ServerPlayer server) AstralSounds.play(server,sound.getLocation().getPath(),sound,volume,pitch);
    }
    private static final WeakHashMap<ServerLevel,Map<Long,Budget>> BUDGETS=new WeakHashMap<>();
    public static int particles(ServerLevel level,ParticleOptions type,double x,double y,double z,int count,double dx,double dy,double dz,double speed) {
        if(!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z) || count<=0) return 0;
        long tick=level.getGameTime(); var map=BUDGETS.computeIfAbsent(level,k->new HashMap<>());
        map.entrySet().removeIf(e->e.getValue().tick()!=tick);
        long chunk=net.minecraft.world.level.ChunkPos.asLong((int)Math.floor(x)>>4,(int)Math.floor(z)>>4);
        int used=map.getOrDefault(chunk,new Budget(tick,0)).used();
        int maximum=ModSettings.i("effects.particleBudgetPerChunkPerTick");
        int n=Math.min(Math.max(0,count),Math.max(0,maximum-used));
        if(ModSettings.enabled("effects.lowEffects")) n=Math.min(n,Math.max(1,count/4));
        if(n==0) return 0; map.put(chunk,new Budget(tick,used+n)); level.sendParticles(type,x,y,z,n,dx,dy,dz,speed);return n;
    }
    private static void cleanup(ServerPlayer p) {
        for(ServerLevel level:p.server.getAllLevels()) for(AstralSlashEntity wave:level.getEntitiesOfClass(AstralSlashEntity.class,p.getBoundingBox().inflate(520),w->p.getUUID().equals(w.getOwner()==null?null:w.getOwner().getUUID()))) wave.discard();
        ComboTracker.clear(p); p.getPersistentData().remove("tutorial_mod:parry_until");
        p.getPersistentData().remove("tutorial_mod:packet_skill_count"); p.getPersistentData().remove("tutorial_mod:packet_slash_count");
    }
    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent e) {if(e.getEntity() instanceof ServerPlayer p) cleanup(p);}
    @SubscribeEvent public static void dimension(PlayerEvent.PlayerChangedDimensionEvent e) {if(e.getEntity() instanceof ServerPlayer p) {cleanup(p);StarEnergy.sync(p);}}
    @SubscribeEvent public static void respawn(PlayerEvent.PlayerRespawnEvent e) {if(e.getEntity() instanceof ServerPlayer p) StarEnergy.sync(p);}
    @SubscribeEvent public static void stopped(net.minecraftforge.event.server.ServerStoppedEvent e) {BUDGETS.clear();SOUNDS.clear();}
    private AstralSafety() {}
}
