package com.example.tutorial.event;
import com.example.tutorial.TutorialMod;
import com.example.tutorial.bounty.*;
import com.example.tutorial.entity.AstralSlashEntity;
import com.example.tutorial.item.ModItems;
import com.mojang.authlib.GameProfile;
import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.util.UUID;
@Mod.EventBusSubscriber(modid=TutorialMod.MOD_ID)
public final class BountySmokeTest {
    private static int ticks;
    private static void check(boolean v,String s) { if(!v) throw new IllegalStateException("BOUNTY_TEST: "+s); }
    @SubscribeEvent public static void tick(TickEvent.PlayerTickEvent e) {
        if(!Boolean.getBoolean("tutorial_mod.verifyBounties") || e.phase!=TickEvent.Phase.END || !(e.player instanceof ServerPlayer p)) return;
        ++ticks;
        if(ticks==10) {
            p.getPersistentData().remove(BountySystem.KEY); p.getInventory().clearContent(); p.setGameMode(GameType.SURVIVAL); p.removeAllEffects();
            p.serverLevel().getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false,p.server); p.serverLevel().setDayTime(18000);
            for(BlockPos pos:BlockPos.betweenClosed(-6,200,-6,6,200,12)) p.serverLevel().setBlockAndUpdate(pos,Blocks.STONE.defaultBlockState());
            p.connection.teleport(.5,201,.5,0,0);
        }
        if(ticks==30) {
            check(BountyCatalog.QUESTS.size()==10,"ten quests"); BountySystem.record(p,"kill","minecraft:zombie"); check(BountySystem.progress(p,0)==0,"accept first");
            BountySystem.action(p,-5,1); BountySystem.action(p,50,2); BountySystem.action(p,0,9);
            for(int i=0;i<10;i++) BountySystem.action(p,i,1);
            BountySystem.action(p,0,2); check(BountySystem.state(p,0)==1,"early claim denied");
            for(int i=0;i<22;i++) BountySystem.record(p,"kill","minecraft:zombie");
            check(BountySystem.progress(p,0)==20 && BountySystem.progress(p,1)==0,"clamp and independent progress");
            int before=p.getInventory().countItem(Items.IRON_INGOT); BountySystem.action(p,0,2); BountySystem.action(p,0,2);
            check(p.getInventory().countItem(Items.IRON_INGOT)==before+16 && BountySystem.state(p,0)==2,"no duplicate reward");
            p.setGameMode(GameType.CREATIVE); BountySystem.record(p,"kill","minecraft:skeleton"); check(BountySystem.progress(p,1)==0,"creative ignored"); p.setGameMode(GameType.SURVIVAL);
            var elite=EntityType.ZOMBIE.create(p.serverLevel()); elite.getPersistentData().putBoolean("tutorial_mod:astral_elite",true);
            BountySystem.death(new LivingDeathEvent(elite,p.damageSources().playerAttack(p))); check(BountySystem.progress(p,6)==1,"elite kill hook");
            var wave=new AstralSlashEntity(p.serverLevel(),p,18,false,false); var skeleton=EntityType.SKELETON.create(p.serverLevel());
            BountySystem.death(new LivingDeathEvent(skeleton,com.example.tutorial.damage.ModDamageTypes.astralSlash(wave,p))); check(BountySystem.progress(p,1)==1,"wave owner credit");
            var canceled=new LivingDeathEvent(skeleton,p.damageSources().playerAttack(p)); canceled.setCanceled(true); BountySystem.death(canceled); check(BountySystem.progress(p,1)==1,"canceled ignored");
            for(int i=0;i<3;i++) BountySystem.record(p,"trial","");
            before=p.getInventory().countItem(ModItems.STAR_INGOT.get()); BountySystem.action(p,8,2); BountySystem.action(p,8,2);
            check(p.getInventory().countItem(ModItems.STAR_INGOT.get())==before+32 && p.getInventory().countItem(Items.ENCHANTED_BOOK)==1,"mythic single reward");
            var clone=new ServerPlayer(p.server,p.serverLevel(),new GameProfile(UUID.randomUUID(),"BountyClone")); BountySystem.clone(new PlayerEvent.Clone(clone,p,true));
            check(BountySystem.state(clone,0)==2 && BountySystem.progress(clone,1)==1,"death clone persistence");
            var saved=p.getPersistentData().getCompound(BountySystem.KEY).copy(); p.getPersistentData().remove(BountySystem.KEY); p.getPersistentData().put(BountySystem.KEY,saved); check(BountySystem.state(p,8)==2,"NBT restore");
            for(int i=0;i<10;i++) if(!BountyCatalog.QUESTS.get(i).tier().equals("神话")) for(var r:BountyCatalog.rewards(i)) check(net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(r.getItem()).getNamespace().equals("minecraft"),"vanilla rewards");
            LogUtils.getLogger().info("BOUNTY_SMOKE_PASS: ten tasks, acceptance, kill credit, bounds, duplicate claims, rewards, persistence");
            p.getPersistentData().getCompound(BountySystem.KEY).putInt("s9",0);
            p.getPersistentData().getCompound(BountySystem.KEY).putInt("p7",2);
        }
        if(ticks==55) BountySystem.sync(p,true);
        if(ticks==120) {
            check(BountySystem.state(p,9)==1,"actual UI accept packet");
            check(BountySystem.state(p,7)==2 && p.getInventory().countItem(Items.NETHERITE_INGOT)==2,"UI claim and replay denied");
            LogUtils.getLogger().info("BOUNTY_UI_ACTION_PASS: UI accept, claim and packet replay");
        }
        if(ticks>=125 && ticks<=145 && ticks%5==0) {
            var slash=new AstralSlashEntity(p.serverLevel(),p,18,false,false,new Vec3(.2,0,1),.13F); slash.setPos(p.getX(),p.getEyeY(),p.getZ()+2); p.serverLevel().addFreshEntity(slash);
        }
        if(ticks==145) p.serverLevel().sendParticles(com.example.tutorial.particle.ModParticles.STAR.get(),p.getX()+.5,p.getEyeY(),p.getZ()+3,35,.4,.4,.4,.10);
    }
}
