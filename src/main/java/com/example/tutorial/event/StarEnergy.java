package com.example.tutorial.event;

import com.example.tutorial.Config;
import com.example.tutorial.TutorialMod;
import com.example.tutorial.network.ModNetwork;
import com.example.tutorial.network.StarEnergySyncPacket;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import com.example.tutorial.event.WeaponMastery.Branch;

/** Server-owned persistent resource shared by the astral weapon and armor set. */
public final class StarEnergy
{
    private static final String ENERGY_TAG = "tutorial_mod:star_energy";
    private static final String LAST_COMBAT_TAG = "tutorial_mod:last_star_combat_tick";

    private StarEnergy() {}

    public static int get(ServerPlayer player)
    {
        return Math.max(0, Math.min(maximum(player), player.getPersistentData().getInt(ENERGY_TAG)));
    }

    public static int maximum(ServerPlayer player)
    {
        int tier = com.example.tutorial.item.StarArmorItem.fullSetTier(player);
        return tier == 2 ? com.example.tutorial.config.ModSettings.i("energy.highCapacity") : tier == 1 ? com.example.tutorial.config.ModSettings.i("energy.lowCapacity") : 0;
    }

    public static void add(ServerPlayer player, int amount)
    {
        if (maximum(player) == 0 || amount <= 0) return;
        int before = get(player);
        player.getPersistentData().putLong(LAST_COMBAT_TAG, player.level().getGameTime());
        int bonus = WeaponMastery.branch(player.getMainHandItem()) == Branch.STAR_CORE ? 1 : 0;
        amount = (int)Math.min(1000000L, Math.ceil(amount * (1 + .10 * com.example.tutorial.talent.StellarTalents.rank(player, 8)))
                + com.example.tutorial.ornament.OrnamentEffects.extraEnergy(player,amount));
        int multiplier = player.level() instanceof ServerLevel level ? AstralWorldEvents.energyMultiplier(level) : 1;
        set(player, before + Math.max(0, (amount + bonus + AstralEnvironment.energyBonus(player,amount)) * multiplier));
        int after = get(player);
        AstralStatistics.add(player,"energy_gained",after-before);
        AstralSounds.threshold(player,before,after);
        if (before < maximum(player) && after >= maximum(player))
            com.example.tutorial.event.AstralSafety.notifySound(player,SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 0.75F, 1.75F);
    }

    public static boolean consume(ServerPlayer player, int amount)
    {
        int current = get(player);
        if (amount < 0 || current < amount) return false;
        set(player, current - amount);
        AstralStatistics.add(player,"energy_spent",amount);
        return true;
    }

    public static void set(ServerPlayer player, int value)
    {
        int clamped = Math.max(0, Math.min(maximum(player), value));
        player.getPersistentData().putInt(ENERGY_TAG, clamped);
        sync(player);
    }

    public static void sync(ServerPlayer player)
    {
        ModNetwork.sendToPlayer(player, new StarEnergySyncPacket(get(player), maximum(player),
                StarArmorEventHandler.getStellarShield(player), StarArmorEventHandler.maximumShield(),
                StarDomains.barrier(player), StarDomains.kind(player), StarDomains.remaining(player),
                StarDomains.cooldown(player, false), StarDomains.cooldown(player, true)));
    }

    @Mod.EventBusSubscriber(modid = TutorialMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static final class Events
    {
        @SubscribeEvent
        public static void tick(TickEvent.PlayerTickEvent event)
        {
            if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)) return;
            long time = player.level().getGameTime();
            int energy = get(player);
            if (energy > 0 && Config.energyDecayPerSecond > 0 && time % 20L == 0L
                    && time - player.getPersistentData().getLong(LAST_COMBAT_TAG) >= Config.energyDecayDelayTicks)
                set(player, energy - Config.energyDecayPerSecond * (com.example.tutorial.config.ModSettings.enabled("environment.netherDecay") && player.level().dimension()==net.minecraft.world.level.Level.NETHER ? 2 : 1));
            if (Config.screenEffects && maximum(player) > 0 && energy >= maximum(player) && time % 10L == 0L
                    && player.level() instanceof ServerLevel level)
                com.example.tutorial.event.AstralSafety.particles(level,ParticleTypes.END_ROD, player.getX(), player.getY(0.65D), player.getZ(),
                        2, 0.45D, 0.7D, 0.45D, 0.01D);
        }
    }
}
