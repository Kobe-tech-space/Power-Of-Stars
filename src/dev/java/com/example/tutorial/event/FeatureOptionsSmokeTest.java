package com.example.tutorial.event;
import com.example.tutorial.config.ModSettings;
import com.example.tutorial.item.ModItems;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.ForgeConfigSpec;
import java.util.Map;
public final class FeatureOptionsSmokeTest {
    @SuppressWarnings("unchecked") private static void flag(String key,boolean value) {
        try {var field=ModSettings.class.getDeclaredField("BOOLS");field.setAccessible(true);((Map<String,ForgeConfigSpec.BooleanValue>)field.get(null)).get(key).set(value);}catch(ReflectiveOperationException ex) {throw new IllegalStateException(ex);}
    }
    private static void check(boolean value,String text) {if(!value) throw new IllegalStateException("FEATURE_OPTIONS: "+text);}
    public static void verify(ServerPlayer p) {
        long day=p.level().getDayTime();float fall=p.fallDistance;var held=p.getMainHandItem().copy();
        try {
            flag("environment.nightEnergy",true);p.serverLevel().setDayTime(18000);p.getPersistentData().remove("tutorial_mod:night_energy_fraction");check(AstralEnvironment.energyBonus(p,100)==20,"night energy option");
            int smallGains=0;for(int i=0;i<5;i++) smallGains+=AstralEnvironment.energyBonus(p,1);check(smallGains==1,"night bonus is20 percent even for small gains");
            flag("environment.fullMoonDamage",true);check(Math.abs(AstralEnvironment.damageFactor(p)-1.1)<.0001,"moon snapshot factor");
            flag("environment.fullMoonDamage",false);flag("environment.fallingSlash",true);p.fallDistance=30;check(Math.abs(AstralEnvironment.damageFactor(p)-1.5)<.0001,"fall damage cap");
            flag("audio.enabled",false);p.getPersistentData().remove("tutorial_mod:sound_options_test");AstralSounds.play(p,"options_test",net.minecraft.sounds.SoundEvents.AMETHYST_BLOCK_CHIME,0,1);check(!p.getPersistentData().contains("tutorial_mod:sound_options_test"),"audio off gate");
            int allowed=AstralSafety.particles(p.serverLevel(),com.example.tutorial.particle.ModParticles.STAR.get(),1024,201,1024,1000,0,0,0,0);
            check(allowed==160 && AstralSafety.particles(p.serverLevel(),com.example.tutorial.particle.ModParticles.STAR.get(),1024,201,1024,1000,0,0,0,0)==0,"shared chunk particle budget");
            flag("effects.lowEffects",true);check(AstralSafety.particles(p.serverLevel(),com.example.tutorial.particle.ModParticles.STAR.get(),1040,201,1040,40,0,0,0,0)==10,"low-effects count");
            flag("energy.keepOnDeath",true);StarEnergy.set(p,123);
            var clone=new ServerPlayer(p.server,p.serverLevel(),new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"EnergyClone"));clone.connection=p.connection;
            StarArmorEventHandler.onPlayerClone(new net.minecraftforge.event.entity.player.PlayerEvent.Clone(clone,p,true));
            StarArmorEventHandler.updateFullSetBonus(new net.minecraftforge.event.TickEvent.PlayerTickEvent(net.minecraftforge.event.TickEvent.Phase.END,clone));
            check(clone.getPersistentData().getInt("tutorial_mod:pending_death_energy")==123,"unequipped death energy bank");
            for(var slot:new net.minecraft.world.entity.EquipmentSlot[]{net.minecraft.world.entity.EquipmentSlot.HEAD,net.minecraft.world.entity.EquipmentSlot.CHEST,net.minecraft.world.entity.EquipmentSlot.LEGS,net.minecraft.world.entity.EquipmentSlot.FEET}) clone.setItemSlot(slot,p.getItemBySlot(slot).copy());
            StarArmorEventHandler.updateFullSetBonus(new net.minecraftforge.event.TickEvent.PlayerTickEvent(net.minecraftforge.event.TickEvent.Phase.END,clone));
            check(StarEnergy.get(clone)==123 && !clone.getPersistentData().contains("tutorial_mod:pending_death_energy"),"retained energy restored exactly once");
            flag("environment.thunderChain",true);p.serverLevel().setWeatherParameters(0,6000,true,true);p.serverLevel().setRainLevel(1);p.serverLevel().setThunderLevel(1);
            check(p.level().isThundering(),"thunder test weather");
            p.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND,new net.minecraft.world.item.ItemStack(ModItems.ICE_SWORD.get()));
            var targets=new java.util.ArrayList<net.minecraft.world.entity.LivingEntity>();
            for(int i=0;i<3;i++) {
                var zombie=net.minecraft.world.entity.EntityType.ZOMBIE.create(p.serverLevel());zombie.setNoAi(true);zombie.getPersistentData().putBoolean("tutorial_mod:elite_rolled",true);
                zombie.moveTo(p.getX()+i+2,p.getY(),p.getZ()+4,0,0);p.serverLevel().addFreshEntity(zombie);targets.add(zombie);
            }
            int hits=AstralStatistics.value(p,"slash_hits");var storm=new FeaturesSmokeTest.Wave(p,6);storm.hit(targets.get(0));
            check(AstralStatistics.value(p,"slash_hits")==hits+3 && targets.get(1).getHealth()<20 && targets.get(2).getHealth()<20,"thunder chain affects exactly two additional enemies");
            targets.forEach(net.minecraft.world.entity.Entity::discard);storm.discard();
            flag("audio.enabled",true);flag("environment.amethystResonance",true);p.getPersistentData().remove("tutorial_mod:sound_resonance");var crystal=p.blockPosition().offset(2,0,2);p.serverLevel().setBlockAndUpdate(crystal,net.minecraft.world.level.block.Blocks.AMETHYST_CLUSTER.defaultBlockState());
            int previousTicks=p.tickCount;p.tickCount=100;AstralEnvironment.tick(new net.minecraftforge.event.TickEvent.PlayerTickEvent(net.minecraftforge.event.TickEvent.Phase.END,p));
            check(p.getPersistentData().contains("tutorial_mod:sound_resonance"),"nearby amethyst resonance");
            flag("environment.beaconShield",true);StarArmorEventHandler.setShield(p,10);AstralEnvironment.tick(new net.minecraftforge.event.TickEvent.PlayerTickEvent(net.minecraftforge.event.TickEvent.Phase.END,p));
            check(StarArmorEventHandler.getStellarShield(p)==11,"active beacon repairs unbroken shield");
            StarArmorEventHandler.setShield(p,0);AstralEnvironment.tick(new net.minecraftforge.event.TickEvent.PlayerTickEvent(net.minecraftforge.event.TickEvent.Phase.END,p));check(StarArmorEventHandler.getStellarShield(p)==0,"beacon cannot grant a free broken-shield reforge");
            p.tickCount=previousTicks;p.serverLevel().removeBlock(crystal,false);
        } finally {
            for(String key:new String[]{"environment.nightEnergy","environment.fullMoonDamage","environment.fallingSlash","environment.thunderChain","environment.amethystResonance","environment.beaconShield","effects.lowEffects","energy.keepOnDeath"}) flag(key,false);
            flag("audio.enabled",true);p.serverLevel().setDayTime(day);p.serverLevel().setWeatherParameters(6000,0,false,false);p.serverLevel().setRainLevel(0);p.serverLevel().setThunderLevel(0);p.fallDistance=fall;p.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND,held);StarEnergy.sync(p);
        }
        com.mojang.logging.LogUtils.getLogger().info("FEATURE_OPTIONS_PASS: fractional night bonus, moon/fall switches, thunder chain, amethyst, active beacon, audio gate, particle budget, low effects, death clone bank");
    }
}
