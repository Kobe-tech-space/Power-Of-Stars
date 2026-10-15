package com.example.tutorial.event;

import com.example.tutorial.TutorialMod;
import com.example.tutorial.attribute.ModAttributes;
import com.example.tutorial.item.AstralGreatswordItem;
import com.example.tutorial.item.ModItems;
import com.example.tutorial.item.StarArmorItem;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.network.chat.Component;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.MobEffectEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Server-owned domains. Cooldowns use the overworld clock, so dimension changes cannot reset them. */
@Mod.EventBusSubscriber(modid = TutorialMod.MOD_ID)
public final class StarDomains
{
    public static final String BARRIER = "tutorial_mod:guardian_barrier";
    public static final String END = "tutorial_mod:domain_end";
    public static final String KIND = "tutorial_mod:domain_kind";
    public static final String BREAK_CD = "tutorial_mod:break_domain_ready";
    public static final String CONTROL_CD = "tutorial_mod:control_domain_ready";
    public static final float BARRIER_MAX = 1000;
    public static final int ENERGY_COST = 1000;
    private StarDomains() {}
    private static long now(ServerPlayer player) { return player.server.overworld().getGameTime(); }
    public static int kind(LivingEntity entity)
    {
        if (!(entity instanceof ServerPlayer player))
        {
            var attribute = entity.getAttribute(ModAttributes.DOMAIN.get());
            return attribute == null ? 0 : (int)attribute.getValue();
        }
        int mode = player.getPersistentData().getInt(KIND);
        boolean effect = mode == 1 ? player.hasEffect(com.example.tutorial.effect.ModEffects.ARMY_BREAKER.get())
                : mode == 2 && player.hasEffect(com.example.tutorial.effect.ModEffects.DOMINION.get());
        return effect && StarArmorItem.fullSetTier(player) == 2 && player.getPersistentData().getLong(END) > now(player) ? mode : 0;
    }
    public static double damageFactor(LivingEntity entity) { return kind(entity) == 1 ? 1.5 : 1; }
    public static int remaining(ServerPlayer player)
    {
        return kind(player) == 0 ? 0 : (int)Math.max(0, player.getPersistentData().getLong(END) - now(player));
    }
    public static int cooldown(ServerPlayer player, boolean ranged)
    {
        if (player.getAbilities().instabuild) return 0;
        return (int)Math.max(0, Math.min(6000, player.getPersistentData().getLong(ranged ? CONTROL_CD : BREAK_CD) - now(player)));
    }
    public static float barrier(Player player)
    {
        return Math.max(0, Math.min(BARRIER_MAX, player.getPersistentData().getFloat(BARRIER)));
    }
    public static boolean activate(ServerPlayer player)
    {
        if (!AstralSafety.valid(player) || !player.getMainHandItem().is(ModItems.ICE_SWORD.get())
                || StarArmorItem.fullSetTier(player) != 2)
        {
            player.displayClientMessage(Component.translatable("message.tutorial_mod.domain_requires_set"), true);
            return false;
        }
        boolean ranged = AstralGreatswordItem.isRanged(player.getMainHandItem());
        int cooldown = cooldown(player, ranged);
        if (cooldown > 0)
        {
            player.displayClientMessage(Component.translatable("message.tutorial_mod.domain_cooldown", (cooldown + 19) / 20), true);
            return false;
        }
        var data = player.getPersistentData();
        long time = now(player);
        if (data.getLong("tutorial_mod:last_domain_cast") == time) return false;
        int energy = StarEnergy.get(player);
        int cost=com.example.tutorial.config.ModSettings.i("skills.domainCost");
        if (!player.getAbilities().instabuild && energy < cost)
        {
            player.displayClientMessage(Component.translatable("message.tutorial_mod.not_enough_star_energy", cost), true);
            AstralSounds.play(player,"failure",SoundEvents.NOTE_BLOCK_BASS.get(),.5F,.65F);
            return false;
        }
        data.putLong("tutorial_mod:last_domain_cast", time);
        data.putInt(KIND, ranged ? 2 : 1);
        data.putLong(END, time + 200);
        data.putLong(ranged ? CONTROL_CD : BREAK_CD, player.getAbilities().instabuild ? 0 : time + 6000);
        player.removeEffect(com.example.tutorial.effect.ModEffects.ARMY_BREAKER.get());
        player.removeEffect(com.example.tutorial.effect.ModEffects.DOMINION.get());
        player.addEffect(new net.minecraft.world.effect.MobEffectInstance(ranged
                ? com.example.tutorial.effect.ModEffects.DOMINION.get() : com.example.tutorial.effect.ModEffects.ARMY_BREAKER.get(),
                200, 0, false, false, true));
        if (ranged)
        {
            data.putFloat(BARRIER, BARRIER_MAX);
            purgeNegative(player);
        }
        updateAttributes(player);
        if (!player.getAbilities().instabuild) StarEnergy.consume(player, cost);
        else StarEnergy.sync(player);
        AstralStatistics.skill(player,"domain");AstralEquipment.wearArmor(player);
        player.displayClientMessage(Component.translatable(ranged ? "message.tutorial_mod.control_domain" : "message.tutorial_mod.break_domain"), true);
        // Creative casts have no gameplay cooldown; visual spam is bounded separately.
        if (time - data.getLong("tutorial_mod:last_domain_visual") >= 10)
        {
            data.putLong("tutorial_mod:last_domain_visual", time);
            if (com.example.tutorial.Config.screenEffects)
                com.example.tutorial.event.AstralSafety.particles(player.serverLevel(),ParticleTypes.END_ROD, player.getX(), player.getY(.25), player.getZ(), 80, 2, .15, 2, .12);
            com.example.tutorial.event.AstralSafety.sound(player.serverLevel(), player.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.5F, ranged ? .6F : 1.2F);
        }
        return true;
    }
    private static void updateAttributes(ServerPlayer player)
    {
        int active = kind(player);
        double boost = active == 1 ? 2 : 0;
        StarArmorEventHandler.modifier(player, Attributes.ATTACK_DAMAGE, "domain_attack", boost, AttributeModifier.Operation.MULTIPLY_TOTAL);
        StarArmorEventHandler.modifier(player, Attributes.ATTACK_SPEED, "domain_speed", boost, AttributeModifier.Operation.MULTIPLY_TOTAL);
        var visible = player.getAttribute(ModAttributes.DOMAIN.get());
        if (visible != null && visible.getBaseValue() != active) visible.setBaseValue(active);
    }
    @SubscribeEvent public static void tick(TickEvent.PlayerTickEvent event)
    {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)) return;
        int active = kind(player);
        updateAttributes(player);
        if (active == 0 && player.getPersistentData().getInt(KIND) != 0)
        {
            player.removeEffect(com.example.tutorial.effect.ModEffects.ARMY_BREAKER.get());
            player.removeEffect(com.example.tutorial.effect.ModEffects.DOMINION.get());
            player.getPersistentData().putInt(KIND, 0);
            player.getPersistentData().putLong(END, 0);
            // The independent guardian barrier intentionally outlives the domain and equipment changes.
            StarEnergy.sync(player);
        }
        if (active == 2) purgeNegative(player);
        if (player.tickCount % 20 == 0) StarEnergy.sync(player);
    }
    private static void purgeNegative(ServerPlayer player)
    {
        var effects = java.util.List.copyOf(player.getActiveEffects());
        for (var effect : effects) if (blocked(effect.getEffect()))
            player.removeEffect(effect.getEffect());
    }
    private static boolean blocked(net.minecraft.world.effect.MobEffect effect)
    {
        return effect.getCategory() != MobEffectCategory.BENEFICIAL
                || effect == net.minecraft.world.effect.MobEffects.DARKNESS || effect == net.minecraft.world.effect.MobEffects.BLINDNESS;
    }
    @SubscribeEvent(priority = EventPriority.LOWEST) public static void denyNegative(MobEffectEvent.Applicable event)
    {
        if (kind(event.getEntity()) == 2 && blocked(event.getEffectInstance().getEffect()))
            event.setResult(Event.Result.DENY);
    }
    @SubscribeEvent(priority = EventPriority.HIGHEST) public static void preventAttack(LivingAttackEvent event)
    {
        if (kind(event.getEntity()) == 2 && event.getAmount() > 1) event.setCanceled(true);
    }
    @SubscribeEvent(priority = EventPriority.HIGHEST) public static void preventDamage(LivingHurtEvent event)
    {
        if (kind(event.getEntity()) == 2 && event.getAmount() > 1) event.setCanceled(true);
    }
    @SubscribeEvent(priority = EventPriority.LOW) public static void preventAmplifiedDamage(LivingHurtEvent event)
    {
        // Catch additive/critical damage raised by other normal-priority hurt handlers.
        if (kind(event.getEntity()) == 2 && event.getAmount() > 1) event.setCanceled(true);
    }
    @SubscribeEvent(priority = EventPriority.HIGHEST) public static void preventFinalDamage(
            net.minecraftforge.event.entity.living.LivingDamageEvent event)
    {
        // Also cover mods that post a final damage event directly without an attack/hurt event.
        if (kind(event.getEntity()) == 2 && event.getAmount() > 1) event.setCanceled(true);
    }
    public static float absorbBarrier(ServerPlayer player, float amount)
    {
        float current = barrier(player), used = Math.min(current, amount);
        if (used <= 0) return amount;
        player.getPersistentData().putFloat(BARRIER, current - used);
        if(current-used<=.01F) AstralSounds.play(player,"barrier_break",SoundEvents.GLASS_BREAK,.8F,.55F);
        if (current - used <= .01F) StarArmorEventHandler.shatter(player);
        StarEnergy.sync(player);
        return Math.max(0, amount - used);
    }
}
