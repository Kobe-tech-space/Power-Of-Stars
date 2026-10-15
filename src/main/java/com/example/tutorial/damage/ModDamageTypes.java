package com.example.tutorial.damage;

import com.example.tutorial.TutorialMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;

import javax.annotation.Nullable;

public final class ModDamageTypes
{
    public static final ResourceKey<DamageType> ASTRAL_SLASH = ResourceKey.create(
            Registries.DAMAGE_TYPE, ResourceLocation.fromNamespaceAndPath(TutorialMod.MOD_ID, "astral_slash"));
    public static final ResourceKey<DamageType> STAR_BURST = ResourceKey.create(
            Registries.DAMAGE_TYPE, ResourceLocation.fromNamespaceAndPath(TutorialMod.MOD_ID, "star_burst"));
    public static final ResourceKey<DamageType> ASTRAL_SLASH_MAGIC = ResourceKey.create(
            Registries.DAMAGE_TYPE, ResourceLocation.fromNamespaceAndPath(TutorialMod.MOD_ID, "astral_slash_magic"));
    public static boolean isAstralSlash(DamageSource source) {
        return source.is(ASTRAL_SLASH) || source.is(ASTRAL_SLASH_MAGIC);
    }
    public static DamageSource astralSlashMagic(Entity projectile, @Nullable Entity owner) {
        return new DamageSource(projectile.level().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE)
                .getHolderOrThrow(ASTRAL_SLASH_MAGIC), projectile, owner);
    }

    private ModDamageTypes() {}

    public static DamageSource astralSlash(Entity projectile, @Nullable Entity owner)
    {
        return new DamageSource(projectile.level().registryAccess()
                .registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(ASTRAL_SLASH), projectile, owner);
    }

    public static DamageSource starBurst(Entity owner)
    {
        return new DamageSource(owner.level().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE)
                .getHolderOrThrow(STAR_BURST), owner);
    }

    /** Only vanilla direct melee attacks may read the attacker's held weapon. */
    public static boolean isDirectMelee(DamageSource source)
    {
        return source.getEntity() != null && source.getDirectEntity() == source.getEntity()
                && (source.is(DamageTypes.PLAYER_ATTACK) || source.is(DamageTypes.MOB_ATTACK)
                || source.is(DamageTypes.MOB_ATTACK_NO_AGGRO));
    }
}
