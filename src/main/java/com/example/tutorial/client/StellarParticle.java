package com.example.tutorial.client;
import com.example.tutorial.TutorialMod;
import com.example.tutorial.particle.ModParticles;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.client.Camera;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.util.Mth;
import org.joml.Vector3f;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Vanilla star-shaped sprite animation, tinted violet/gold, with a pulsing emissive core. */
public final class StellarParticle extends TextureSheetParticle {
    private final SpriteSet sprites;
    private final float baseSize;
    private final Vector3f corner = new Vector3f();
    private StellarParticle(ClientLevel level, double x, double y, double z, double vx, double vy, double vz, SpriteSet sprites) {
        super(level, x, y, z, 0, 0, 0);
        this.sprites = sprites; xd = vx; yd = vy; zd = vz;
        hasPhysics = false; gravity = 0; friction = .90F;
        lifetime = 14 + random.nextInt(13); baseSize = .045F + random.nextFloat() * .045F;
        quadSize = baseSize;
        if (random.nextInt(5) == 0) setColor(1F, .78F, .40F);
        else if (random.nextInt(3) == 0) setColor(.86F, .72F, 1F);
        else setColor(.57F, .24F, .92F);
        setSpriteFromAge(sprites);
    }
    @Override public void tick() {
        super.tick(); setSpriteFromAge(sprites);
        alpha = Math.max(0F, 1F - (float)age / lifetime);
        quadSize = baseSize * (.75F + .25F * (float)Math.sin(age * .9F));
    }
    @Override protected int getLightColor(float partialTick) { return 0xF000F0; }
    @Override public void render(VertexConsumer out, Camera camera, float partial) {
        var eye = camera.getPosition();
        float x = (float)(Mth.lerp(partial, xo, this.x) - eye.x);
        float y = (float)(Mth.lerp(partial, yo, this.y) - eye.y);
        float z = (float)(Mth.lerp(partial, zo, this.z) - eye.z);
        // Two slim diamond sprites form four pointed rays, without a new texture or draw call.
        ray(out, camera, x, y, z, quadSize, quadSize * .24F);
        ray(out, camera, x, y, z, quadSize * .24F, quadSize);
    }
    private void ray(VertexConsumer out, Camera camera, float x, float y, float z, float halfWidth, float halfHeight) {
        for (int i = 0; i < 4; i++) {
            corner.set(i < 2 ? -halfWidth : halfWidth, i == 1 || i == 2 ? halfHeight : -halfHeight, 0)
                    .rotate(camera.rotation()).add(x, y, z);
            out.vertex(corner.x(), corner.y(), corner.z()).uv(i < 2 ? getU1() : getU0(), i == 1 || i == 2 ? getV0() : getV1())
                    .color(rCol, gCol, bCol, alpha).uv2(0xF000F0).endVertex();
        }
    }
    @Override public ParticleRenderType getRenderType() { return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT; }
    public record Provider(SpriteSet sprites) implements ParticleProvider<SimpleParticleType> {
        @Override public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z, double vx, double vy, double vz) {
            return new StellarParticle(level, x, y, z, vx, vy, vz, sprites);
        }
    }
    @Mod.EventBusSubscriber(modid = TutorialMod.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static class Registration {
        @SubscribeEvent public static void register(RegisterParticleProvidersEvent e) { e.registerSpriteSet(ModParticles.STAR.get(), Provider::new); }
    }
}
