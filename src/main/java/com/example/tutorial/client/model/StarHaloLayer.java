package com.example.tutorial.client.model;

import com.example.tutorial.TutorialMod;
import com.example.tutorial.item.StarArmorItem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.Map;

/** Renders a vertical, animated astral mandala behind the wearer's head. */
public class StarHaloLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>>
{
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(TutorialMod.MOD_ID, "textures/entity/star_halo.png");
    private static final DustParticleOptions CYAN_SPARK =
            new DustParticleOptions(new Vector3f(0.20F, 0.78F, 1.0F), 0.75F);
    private static final int FULL_BRIGHT = LightTexture.FULL_BRIGHT;

    private final StarHaloModel<AbstractClientPlayer> model;
    private final Map<Integer, Integer> lastParticleTick = new HashMap<>();

    public StarHaloLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent,
                         StarHaloModel<AbstractClientPlayer> model)
    {
        super(parent);
        this.model = model;
    }

    @Override
    public void render(PoseStack pose, MultiBufferSource buffers, int packedLight,
                       AbstractClientPlayer player, float limbSwing, float limbSwingAmount,
                       float partialTick, float ageInTicks, float netHeadYaw, float headPitch)
    {
        if (!StarArmorItem.hasFullSet(player) || player.isInvisible()) return;

        pose.pushPose();
        this.getParentModel().head.translateAndRotate(pose);

        // A vanilla head extends to +0.25 on its back. This keeps the vertical
        // disc behind the helmet while the outer rays frame the wearer's face.
        pose.translate(0.0D, -0.25D, 0.34D);
        float pulse = 1.0F + Mth.sin(ageInTicks * 0.12F) * 0.035F;

        // Large soft aura, counter-rotating to give the halo depth.
        pose.pushPose();
        pose.mulPose(Axis.ZP.rotationDegrees(-ageInTicks * 0.22F));
        pose.scale(1.16F * pulse, 1.16F * pulse, 1.0F);
        this.model.renderToBuffer(pose,
                buffers.getBuffer(RenderType.entityTranslucentEmissive(TEXTURE)),
                FULL_BRIGHT, OverlayTexture.NO_OVERLAY,
                0.20F, 0.52F, 1.0F, 0.24F);
        pose.popPose();

        // Main high-definition ring.
        pose.pushPose();
        pose.mulPose(Axis.ZP.rotationDegrees(ageInTicks * 0.36F));
        pose.scale(pulse, pulse, 1.0F);
        this.model.renderToBuffer(pose,
                buffers.getBuffer(RenderType.entityTranslucentEmissive(TEXTURE)),
                FULL_BRIGHT, OverlayTexture.NO_OVERLAY,
                0.92F, 0.96F, 1.0F, 0.94F);
        pose.popPose();

        // A smaller violet inner star wheel turns in the opposite direction.
        pose.pushPose();
        pose.translate(0.0D, 0.0D, -0.006D);
        pose.mulPose(Axis.ZP.rotationDegrees(-ageInTicks * 0.62F + 22.5F));
        pose.scale(0.66F, 0.66F, 1.0F);
        this.model.renderToBuffer(pose,
                buffers.getBuffer(RenderType.entityTranslucentEmissive(TEXTURE)),
                FULL_BRIGHT, OverlayTexture.NO_OVERLAY,
                0.55F, 0.32F, 1.0F, 0.46F);
        pose.popPose();

        pose.popPose();
        spawnOrbitSpark(player, partialTick);
    }

    /** One client-only spark every three ticks; bounded so the effect stays cheap. */
    private void spawnOrbitSpark(AbstractClientPlayer player, float partialTick)
    {
        int tick = player.tickCount;
        Integer previousTick = lastParticleTick.put(player.getId(), tick);
        if (tick % 3 != 0 || previousTick != null && previousTick == tick) return;

        Vec3 forward = player.getViewVector(partialTick).normalize();
        Vec3 right = forward.cross(new Vec3(0.0D, 1.0D, 0.0D));
        if (right.lengthSqr() < 1.0E-4D) right = new Vec3(1.0D, 0.0D, 0.0D);
        right = right.normalize();
        Vec3 up = right.cross(forward).normalize();
        Vec3 center = player.getEyePosition(partialTick)
                .subtract(forward.scale(0.38D))
                .add(0.0D, -0.02D, 0.0D);

        double angle = tick * 0.19D + player.getId() * 0.73D;
        double radius = 0.72D + 0.08D * Math.sin(tick * 0.11D);
        Vec3 point = center.add(right.scale(Math.cos(angle) * radius))
                .add(up.scale(Math.sin(angle) * radius));
        player.level().addParticle(CYAN_SPARK, point.x, point.y, point.z,
                0.0D, 0.006D, 0.0D);
    }
}
