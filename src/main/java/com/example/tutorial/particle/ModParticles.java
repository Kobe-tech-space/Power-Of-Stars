package com.example.tutorial.particle;
import com.example.tutorial.TutorialMod;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraftforge.registries.*;
public final class ModParticles {
    public static final DeferredRegister<net.minecraft.core.particles.ParticleType<?>> PARTICLES = DeferredRegister.create(ForgeRegistries.PARTICLE_TYPES, TutorialMod.MOD_ID);
    public static final RegistryObject<SimpleParticleType> STAR = PARTICLES.register("stellar_spark", () -> new SimpleParticleType(false));
    private ModParticles() {}
}
