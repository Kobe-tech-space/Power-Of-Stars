package com.example.tutorial.event;
import com.example.tutorial.TutorialMod;
import com.example.tutorial.config.ModSettings;
import com.example.tutorial.item.StarArmorItem;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
@Mod.EventBusSubscriber(modid=TutorialMod.MOD_ID)
public final class AstralEnvironment {
    private static boolean night(ServerPlayer p) {long clock=Math.floorMod(p.level().getDayTime(),24000L);return clock>=13000 && clock<23000;}
    public static int energyBonus(ServerPlayer p,int amount) {
        if(amount<=0 || !ModSettings.enabled("environment.nightEnergy") || p.level().dimension()!=Level.OVERWORLD || !night(p)) return 0;
        String key="tutorial_mod:night_energy_fraction";
        double fraction=p.getPersistentData().getDouble(key);
        if(!Double.isFinite(fraction) || fraction<0 || fraction>=1) fraction=0;
        double total=fraction+amount*.20;
        int bonus=(int)Math.floor(total+1.0E-9);
        p.getPersistentData().putDouble(key,Math.max(0,total-bonus));
        return bonus;
    }
    public static double damageFactor(ServerPlayer p) {
        double f=ModSettings.enabled("environment.fullMoonDamage") && p.level().dimension()==Level.OVERWORLD && night(p) && p.level().getMoonPhase()==0?1.1:1;
        if(ModSettings.enabled("environment.fallingSlash") && p.fallDistance>3) f*=1+Math.min(.5,(p.fallDistance-3)*.025);
        return f;
    }
    @SubscribeEvent public static void tick(TickEvent.PlayerTickEvent e) {
        if(e.phase!=TickEvent.Phase.END || !(e.player instanceof ServerPlayer p) || !StarArmorItem.hasFullSet(p) || p.tickCount%100!=0) return;
        if(ModSettings.enabled("environment.amethystResonance")) {
            for(BlockPos pos:BlockPos.betweenClosed(p.blockPosition().offset(-3,-2,-3),p.blockPosition().offset(3,2,3))) {
                if(p.serverLevel().hasChunkAt(pos) && p.level().getBlockState(pos).is(Blocks.AMETHYST_CLUSTER)) {AstralSounds.play(p,"resonance",net.minecraft.sounds.SoundEvents.AMETHYST_BLOCK_RESONATE,.35F,1.4F);break;}
            }
        }
        if(ModSettings.enabled("environment.beaconShield")) {
            BlockPos pos=p.blockPosition();
            for(int depth=1;depth<=32 && pos.getY()-depth>=p.level().getMinBuildHeight();depth++) {
                var state=p.level().getBlockState(pos.below(depth));
                if(state.is(Blocks.BEACON)) {
                    if(p.level().getBlockEntity(pos.below(depth)) instanceof net.minecraft.world.level.block.entity.BeaconBlockEntity beacon && !beacon.getBeamSections().isEmpty()) {
                        float shield=StarArmorEventHandler.getStellarShield(p); if(shield>0 && shield<StarArmorEventHandler.maximumShield()) StarArmorEventHandler.setShield(p,shield+1);
                    }
                    break;
                }
                if(state.canOcclude()) break;
            }
        }
    }
    private AstralEnvironment() {}
}
