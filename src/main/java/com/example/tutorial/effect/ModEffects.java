package com.example.tutorial.effect;

import com.example.tutorial.TutorialMod;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/** Real, synchronized status effects; only a server-authorized domain supplies their powers. */
public final class ModEffects
{
    public static final DeferredRegister<MobEffect> EFFECTS = DeferredRegister.create(ForgeRegistries.MOB_EFFECTS, TutorialMod.MOD_ID);
    public static final RegistryObject<MobEffect> ARMY_BREAKER = EFFECTS.register("army_breaker", () -> new DomainEffect(0x69BFFF));
    public static final RegistryObject<MobEffect> DOMINION = EFFECTS.register("dominion", () -> new DomainEffect(0xFFD56B));
    private static final class DomainEffect extends MobEffect
    {
        DomainEffect(int color) { super(MobEffectCategory.BENEFICIAL, color); }
    }
    private ModEffects() {}
}
