package com.example.tutorial.event;

import com.example.tutorial.TutorialMod;
import com.example.tutorial.item.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Vector3f;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = TutorialMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class AstralEliteHandler
{
    private static final String ROLLED = "tutorial_mod:elite_rolled";
    private static final String ELITE = "tutorial_mod:astral_elite";
    private static final String TYPE = "tutorial_mod:elite_type";
    private static final String VERSION = "tutorial_mod:elite_version";
    private static final String LAST_HIT = "tutorial_mod:elite_last_hit";
    private static final DustParticleOptions[] COLORS = {
            new DustParticleOptions(new Vector3f(1,.25F,.15F),1),
            new DustParticleOptions(new Vector3f(.25F,.85F,1),1),
            new DustParticleOptions(new Vector3f(1,.78F,.2F),1),
            new DustParticleOptions(new Vector3f(.65F,.3F,1),1)
    };
    private static final DustParticleOptions PARTICLE = new DustParticleOptions(new Vector3f(0.45F, 0.2F, 1.0F), 1.0F);

    private AstralEliteHandler() {}

    @SubscribeEvent
    public static void onJoin(EntityJoinLevelEvent event)
    {
        if (event.getLevel().isClientSide() || !(event.getEntity() instanceof Mob mob) || !(mob instanceof Enemy)) return;
        var data = mob.getPersistentData();
        if (data.getBoolean(ELITE)) {
            if (data.getInt(VERSION) < 2) empower(mob, data.getInt(TYPE));
            return;
        }
        if (data.getBoolean(ROLLED)) return;
        data.putBoolean(ROLLED, true);
        if (mob.hasCustomName() || mob.getMaxHealth() >= 200) return;
        double chance = event.getLevel() instanceof ServerLevel level ? AstralWorldEvents.eliteChance(level) : 0.03D;
        if (mob.getRandom().nextDouble() >= chance) return;
        int type = mob.getRandom().nextInt(4);
        empower(mob, type);
    }

    public static void empower(Mob mob, int type)
    {
        var data = mob.getPersistentData();
        if (data.getBoolean(ELITE) && data.getInt(VERSION) >= 2) return;
        type = net.minecraft.util.Mth.clamp(type, 0, 3);
        data.putBoolean(ROLLED, true);
        data.putInt(VERSION, 2);
        data.putBoolean(ELITE, true);
        data.putInt(TYPE, type);
        var health = mob.getAttribute(Attributes.MAX_HEALTH);
        if (health != null) health.removeModifier(id("health"));
        double baseline = Math.max(1, mob.getMaxHealth());
        // Use the already-modified baseline once; a flat bonus would be multiplied by other mods again.
        add(mob, Attributes.MAX_HEALTH, "health", Math.max(0, Math.min(5, 500 / baseline - 1)), AttributeModifier.Operation.MULTIPLY_TOTAL);
        add(mob, Attributes.ARMOR, "armor", 12, AttributeModifier.Operation.ADDITION);
        add(mob, Attributes.ARMOR_TOUGHNESS, "toughness", 8, AttributeModifier.Operation.ADDITION);
        add(mob, Attributes.ATTACK_DAMAGE, "damage", type == 0 ? 2 : 1, AttributeModifier.Operation.MULTIPLY_BASE);
        add(mob, Attributes.MOVEMENT_SPEED, "speed", type == 2 ? .65 : .2, AttributeModifier.Operation.MULTIPLY_BASE);
        add(mob, Attributes.KNOCKBACK_RESISTANCE, "knockback", .35, AttributeModifier.Operation.ADDITION);
        mob.setHealth(mob.getMaxHealth());
        if (type == 1) mob.setAbsorptionAmount(80.0F);
        mob.setCustomName(Component.translatable("elite.tutorial_mod.type_" + type, mob.getType().getDescription())
                .withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD));
        mob.setCustomNameVisible(true);
        mob.setGlowingTag(true);
    }

    @SubscribeEvent
    public static void onTick(LivingEvent.LivingTickEvent event)
    {
        LivingEntity entity = event.getEntity();
        if (!entity.getPersistentData().getBoolean(ELITE) || entity.tickCount % 10 != 0
                || !(entity.level() instanceof ServerLevel level)) return;
        int type = net.minecraft.util.Mth.clamp(entity.getPersistentData().getInt(TYPE),0,3);
        com.example.tutorial.event.AstralSafety.particles(level,COLORS[type], entity.getX(), entity.getY(0.65D), entity.getZ(),
                3, 0.35D, 0.5D, 0.35D, 0.01D);
        if (!(entity instanceof Mob mob)) return;
        if (type == 1 && entity.tickCount % 120 == 0
                && level.getGameTime()-entity.getPersistentData().getLong(LAST_HIT) >= 100)
            entity.setAbsorptionAmount(Math.min(80, entity.getAbsorptionAmount()+10));
        var target = mob.getTarget();
        if (target == null || !target.isAlive() || !mob.hasLineOfSight(target)
                || target.isSpectator() || target instanceof net.minecraft.world.entity.player.Player p && p.getAbilities().instabuild) return;
        if (type == 0 && entity.getHealth() <= entity.getMaxHealth() * .5 && entity.tickCount % 60 == 0)
        {
            mob.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.DAMAGE_BOOST,80,1,false,false));
            mob.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.MOVEMENT_SPEED,80,0,false,false));
            pulse(level,mob,type,12);
        }
        if (type == 2 && entity.tickCount % 80 == 0 && entity.distanceToSqr(target) > 4 && entity.distanceToSqr(target) < 100)
        {
            var direction = target.position().subtract(entity.position()).multiply(1,0,1).normalize().scale(.6);
            if (level.noCollision(mob, mob.getBoundingBox().move(direction)))
                mob.setDeltaMovement(direction.add(0,.15,0));
            pulse(level,mob,type,10);
        }
        if (type == 3 && entity.distanceToSqr(target) < 100)
        {
            if (StarDomains.kind(target) != 2)
            {
                target.setDeltaMovement(target.getDeltaMovement().add(
                        entity.position().subtract(target.position()).multiply(1,0,1).normalize().scale(.08)));
                target.hurtMarked = true;
            }
            if (entity.tickCount % 120 == 0)
            {
                target.hurt(level.damageSources().mobAttack(mob),6);
                target.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN,60,1,false,false),mob);
                pulse(level,mob,type,18);
            }
        }
    }
    private static void pulse(ServerLevel level, Mob mob, int type, int count)
    {
        com.example.tutorial.event.AstralSafety.particles(level,COLORS[type],mob.getX(),mob.getY(.5),mob.getZ(),count,.8,.5,.8,.02);
        com.example.tutorial.event.AstralSafety.sound(level, mob.blockPosition(),net.minecraft.sounds.SoundEvents.AMETHYST_BLOCK_CHIME,
                net.minecraft.sounds.SoundSource.HOSTILE,.55F,.7F+type*.2F);
    }
    @SubscribeEvent public static void onHit(net.minecraftforge.event.entity.living.LivingDamageEvent event)
    {
        if (event.getAmount() > 0 && event.getEntity().getPersistentData().getBoolean(ELITE))
            event.getEntity().getPersistentData().putLong(LAST_HIT,event.getEntity().level().getGameTime());
    }

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event)
    {
        if (!event.getEntity().getPersistentData().getBoolean(ELITE)) return;
        event.getEntity().spawnAtLocation(new net.minecraft.world.item.ItemStack(
                ModItems.STAR_INGOT.get(), 3 + event.getEntity().getRandom().nextInt(4)));
        if (event.getEntity().level() instanceof ServerLevel level)
            com.example.tutorial.event.AstralSafety.particles(level,net.minecraft.core.particles.ParticleTypes.FIREWORK,event.getEntity().getX(),event.getEntity().getY(.5),event.getEntity().getZ(),24,.5,.6,.5,.08);
    }

    private static void add(Mob mob, net.minecraft.world.entity.ai.attributes.Attribute attribute,
                            String name, double amount, AttributeModifier.Operation operation)
    {
        var instance = mob.getAttribute(attribute);
        if (instance == null) return;
        UUID id = id(name);
        instance.removeModifier(id);
        instance.addPermanentModifier(new AttributeModifier(id, "tutorial_mod:elite_" + name, amount, operation));
    }
    private static UUID id(String name) { return UUID.nameUUIDFromBytes((TutorialMod.MOD_ID + ":elite_" + name).getBytes(StandardCharsets.UTF_8)); }
}
