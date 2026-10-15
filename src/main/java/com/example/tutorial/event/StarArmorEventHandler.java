package com.example.tutorial.event;

import com.example.tutorial.TutorialMod;
import com.example.tutorial.Config;
import com.example.tutorial.attribute.ModAttributes;
import com.example.tutorial.item.StarArmorItem;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.event.ItemAttributeModifierEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityAttributeModificationEvent;
import net.minecraftforge.event.entity.living.*;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = TutorialMod.MOD_ID)
public final class StarArmorEventHandler
{
    public static final float ENERGY_SHIELD_MAX = 50;
    private static final String SHIELD = "tutorial_mod:energy_shield";
    private static final String TIER = "tutorial_mod:equipped_star_tier";
    private StarArmorEventHandler() {}

    @SubscribeEvent public static void addArmorPieceImmunity(ItemAttributeModifierEvent event)
    {
        if (!(event.getItemStack().getItem() instanceof StarArmorItem armor)
                || event.getSlotType() != armor.getType().getSlot()) return;
        String slot = armor.getType().getName();
        String tier=armor.isAdvanced()?"high":"low";
        for(var attribute:new Attribute[]{Attributes.ARMOR,Attributes.ARMOR_TOUGHNESS}) {
            for(var original:armor.getDefaultAttributeModifiers(event.getSlotType()).get(attribute)) {
                event.removeModifier(attribute,original);
                double value=attribute==Attributes.ARMOR?com.example.tutorial.config.ModSettings.i("armor."+tier+"."+slot):com.example.tutorial.config.ModSettings.d("armor.toughness");
                event.addModifier(attribute,new AttributeModifier(original.getId(),original.getName(),value,original.getOperation()));
            }
        }
        addPiece(event, ModAttributes.DAMAGE_IMMUNITY.get(), "immunity_" + slot,
                com.example.tutorial.config.ModSettings.d("armor."+tier+".pieceImmunity"), AttributeModifier.Operation.ADDITION);
        addPiece(event, ModAttributes.DAMAGE_BONUS.get(), "damage_" + slot,
                armor.isAdvanced() ? .05 : .02, AttributeModifier.Operation.ADDITION);
        addPiece(event, Attributes.ATTACK_SPEED, "speed_" + slot,
                armor.isAdvanced() ? .05 : .02, AttributeModifier.Operation.MULTIPLY_BASE);
    }
    private static void addPiece(ItemAttributeModifierEvent event, Attribute attribute,
                                 String name, double amount, AttributeModifier.Operation operation)
    {
        event.addModifier(attribute, new AttributeModifier(uuid(name), "tutorial_mod:" + name, amount, operation));
    }

    @SubscribeEvent public static void updateFullSetBonus(TickEvent.PlayerTickEvent event)
    {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)) return;
        if(!AstralSafety.valid(player)) return;
        int tier = StarArmorItem.fullSetTier(player);
        modifier(player, Attributes.ARMOR, "full_set_armor", tier == 0 ? 0 : com.example.tutorial.config.ModSettings.i("armor."+(tier==2?"high":"low")+".setBonus"), AttributeModifier.Operation.ADDITION);
        modifier(player, ModAttributes.DAMAGE_IMMUNITY.get(), "full_set_immunity", tier == 0 ? 0 : com.example.tutorial.config.ModSettings.d("armor."+(tier==2?"high":"low")+".setImmunity"), AttributeModifier.Operation.ADDITION);
        modifier(player, ModAttributes.DAMAGE_BONUS.get(), "full_set_damage", tier == 2 ? .10 : tier == 1 ? .05 : 0, AttributeModifier.Operation.ADDITION);
        modifier(player, Attributes.ATTACK_SPEED, "full_set_speed", tier == 2 ? .10 : tier == 1 ? .05 : 0, AttributeModifier.Operation.MULTIPLY_BASE);
        var data = player.getPersistentData();
        if(tier>0 && data.contains("tutorial_mod:pending_death_energy")) {
            int retained=data.getInt("tutorial_mod:pending_death_energy");data.remove("tutorial_mod:pending_death_energy");StarEnergy.set(player,retained);
        }
        if (tier == 0 && data.getInt("tutorial_mod:star_energy") > 0) StarEnergy.set(player, 0);
        // Remove the previous auto-granted shield once; don't change other absorption after migration.
        if (!data.getBoolean("tutorial_mod:shield_v2_migrated"))
        {
            float old = Math.max(0, data.getFloat("tutorial_mod:stellar_shield_remaining"));
            player.setAbsorptionAmount(Math.max(0, player.getAbsorptionAmount() - old));
            data.putFloat("tutorial_mod:stellar_shield_remaining", 0);
            data.putBoolean("tutorial_mod:shield_v2_migrated", true);
        }
        boolean tierChanged = data.getInt(TIER) != tier;
        if (tierChanged)
        {
            clearSetEffects(player, data.getInt(TIER));
            data.putInt(TIER, tier);
            if (tier == 0) data.putFloat(SHIELD, 0);
            StarEnergy.set(player, StarEnergy.get(player));
        }
        if (tier > 0 && (tierChanged || player.tickCount % 20 == 0))
        {
            setEffect(player, MobEffects.NIGHT_VISION, 240, 0);
            setEffect(player, MobEffects.DAMAGE_BOOST, 60, tier == 2 ? 9 : 1);
            setEffect(player, MobEffects.DIG_SPEED, 60, tier == 2 ? 9 : 1);
            setEffect(player, tier == 2 ? MobEffects.FIRE_RESISTANCE : MobEffects.DAMAGE_RESISTANCE, 60, 0);
            if (player.isSprinting() && player.tickCount % 100 == 0) StarEnergy.add(player, 1);
        }
    }
    private static void setEffect(Player player, net.minecraft.world.effect.MobEffect effect, int duration, int level)
    {
        player.addEffect(new MobEffectInstance(effect, duration, level, true, false, true));
    }
    private static void clearSetEffects(Player player, int previousTier)
    {
        if (previousTier == 0) return;
        for (var effect : new net.minecraft.world.effect.MobEffect[]{MobEffects.NIGHT_VISION,
                MobEffects.DAMAGE_BOOST, MobEffects.DIG_SPEED,
                previousTier == 2 ? MobEffects.FIRE_RESISTANCE : MobEffects.DAMAGE_RESISTANCE})
        {
            var active = player.getEffect(effect);
            int amplifier = effect == MobEffects.DAMAGE_BOOST || effect == MobEffects.DIG_SPEED ? previousTier == 2 ? 9 : 1 : 0;
            if (active != null && active.isAmbient() && !active.isVisible() && active.getAmplifier() == amplifier
                    && active.getDuration() <= (effect == MobEffects.NIGHT_VISION ? 240 : 60)) player.removeEffect(effect);
        }
    }
    public static void modifier(Player player, Attribute attribute, String name, double amount, AttributeModifier.Operation operation)
    {
        var instance = player.getAttribute(attribute);
        if (instance == null) return;
        UUID id = uuid(name);
        var old = instance.getModifier(id);
        if (old != null && old.getAmount() == amount && old.getOperation() == operation) return;
        if (old != null) instance.removeModifier(id);
        if (amount != 0) instance.addTransientModifier(new AttributeModifier(id, "tutorial_mod:" + name, amount, operation));
    }
    public static void restoreStellarShield(Player player)
    {
        if (!StarArmorItem.hasFullSet(player)) return;
        player.getPersistentData().putFloat(SHIELD, maximumShield());
        if (player instanceof ServerPlayer server) StarEnergy.sync(server);
    }
    public static float maximumShield() {return com.example.tutorial.config.ModSettings.i("shield.capacity");}
    public static void setShield(ServerPlayer p,float value) {p.getPersistentData().putFloat(SHIELD,Math.max(0,Math.min(maximumShield(),value)));StarEnergy.sync(p);}
    public static boolean needsShieldRestore(Player player) { return getStellarShield(player) < maximumShield() - .01F; }
    public static float getStellarShield(Player player)
    {
        return Math.max(0, Math.min(maximumShield(), player.getPersistentData().getFloat(SHIELD)));
    }
    @SubscribeEvent(priority = EventPriority.LOW) public static void absorbShield(LivingDamageEvent event)
    {
        if (!(event.getEntity() instanceof ServerPlayer player) || event.getAmount() <= 0) return;
        float damage = StarDomains.absorbBarrier(player, event.getAmount());
        AstralStatistics.absorb(player,event.getAmount()-damage);
        float shield = getStellarShield(player), absorbed = Math.min(shield, damage);
        if (absorbed > 0)
        {
            player.getPersistentData().putFloat(SHIELD, shield - absorbed);
            damage -= absorbed;
            AstralStatistics.absorb(player,absorbed);
            AstralSounds.shield(player,shield,shield-absorbed);
            if (shield - absorbed <= .01F) shatter(player);
            StarEnergy.sync(player);
        }
        event.setAmount(Math.max(0, damage));
    }
    public static void shatter(ServerPlayer player)
    {
        ServerLevel level = player.serverLevel();
        com.example.tutorial.event.AstralSafety.particles(level,ParticleTypes.FIREWORK, player.getX(), player.getY(.65), player.getZ(), 42, .65, .8, .65, .2);
    }
    @SubscribeEvent(priority = EventPriority.LOWEST) public static void applyDamageImmunity(LivingHurtEvent event)
    {
        var attribute = event.getEntity().getAttribute(ModAttributes.DAMAGE_IMMUNITY.get());
        if (attribute == null) return;
        double immunity = Double.isFinite(attribute.getValue()) ? Math.min(.95, Math.max(0, attribute.getValue())) : 0;
        // Preserve the configurable extra protection for armor beyond vanilla's hard-coded 20.
        double armor = event.getEntity().getAttributeValue(Attributes.ARMOR);
        double armorBonus = event.getSource().is(net.minecraft.tags.DamageTypeTags.BYPASSES_ARMOR)
                ? 0 : AttributeCompatibility.extraArmorReduction(armor);
        event.setAmount((float)(event.getAmount() * (1 - immunity) * (1 - armorBonus)));
        if (event.getEntity() instanceof ServerPlayer player && StarArmorItem.hasFullSet(player)) StarEnergy.add(player, 1);
    }
    @SubscribeEvent public static void reduceFallDamage(LivingFallEvent event)
    {
        int tier = StarArmorItem.fullSetTier(event.getEntity());
        if (tier == 2) event.setCanceled(true);
        else if (tier == 1) event.setDamageMultiplier(event.getDamageMultiplier() * .20F);
    }
    @SubscribeEvent public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event)
    {
        if (event.getEntity() instanceof ServerPlayer player) StarEnergy.sync(player);
    }
    @SubscribeEvent public static void onPlayerClone(PlayerEvent.Clone event)
    {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        player.getPersistentData().putFloat(StarDomains.BARRIER, StarDomains.barrier(event.getOriginal()));
        if (!event.isWasDeath())
        {
            for (String key : new String[]{"tutorial_mod:star_energy", SHIELD, StarDomains.BARRIER,
                    StarDomains.END, StarDomains.KIND, StarDomains.BREAK_CD, StarDomains.CONTROL_CD})
            {
                var tag = event.getOriginal().getPersistentData().get(key);
                if (tag != null) player.getPersistentData().put(key, tag.copy());
            }
        }
        else for (String key : new String[]{StarDomains.BREAK_CD, StarDomains.CONTROL_CD})
            player.getPersistentData().putLong(key, event.getOriginal().getPersistentData().getLong(key));
        if(event.isWasDeath() && com.example.tutorial.config.ModSettings.enabled("energy.keepOnDeath")) player.getPersistentData().putInt("tutorial_mod:pending_death_energy",event.getOriginal().getPersistentData().getInt("tutorial_mod:star_energy"));
        player.getPersistentData().putBoolean("tutorial_mod:shield_v2_migrated", true);
        StarEnergy.sync(player);
    }
    private static UUID uuid(String path)
    {
        return UUID.nameUUIDFromBytes((TutorialMod.MOD_ID + ":" + path).getBytes(StandardCharsets.UTF_8));
    }
    @Mod.EventBusSubscriber(modid = TutorialMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static final class AttributeRegistration
    {
        @SubscribeEvent public static void addPlayerAttributes(EntityAttributeModificationEvent event)
        {
            event.add(EntityType.PLAYER, ModAttributes.DAMAGE_IMMUNITY.get());
            event.add(EntityType.PLAYER, ModAttributes.DAMAGE_BONUS.get());
            event.add(EntityType.PLAYER, ModAttributes.DOMAIN.get());
        }
    }
}
