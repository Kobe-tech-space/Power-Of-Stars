package com.example.tutorial.client.model;

import com.example.tutorial.entity.AstralSlashEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

public class AstralSlashRenderer extends EntityRenderer<AstralSlashEntity>
{
    // The lightning shader is untextured, additive, depth-tested and independent of ambient light.
    private static final ResourceLocation UNUSED_TEXTURE = ResourceLocation.fromNamespaceAndPath("minecraft", "textures/misc/white.png");
    private static final int SEGMENTS = 40;

    public AstralSlashRenderer(EntityRendererProvider.Context context)
    {
        super(context);
        this.shadowRadius = 0.0F;
    }

    @Override
    public void render(AstralSlashEntity entity, float yaw, float partialTick, PoseStack pose,
                       MultiBufferSource buffers, int packedLight)
    {
        pose.pushPose();
        Vec3 velocity = entity.getDeltaMovement();
        if (velocity.lengthSqr() > 0.000001D)
        {
            float heading = (float) Math.toDegrees(Math.atan2(velocity.x, velocity.z));
            float pitch = (float) Math.toDegrees(Math.atan2(
                    velocity.y, Math.sqrt(velocity.x * velocity.x + velocity.z * velocity.z)));
            pose.mulPose(Axis.YP.rotationDegrees(heading));
            pose.mulPose(Axis.XP.rotationDegrees(-pitch));
        }
        // A tilted sweep reads as a sword slash, including when viewed from behind the shooter.
        pose.mulPose(Axis.ZP.rotationDegrees(-28.0F));
        float size = 1.65F;
        pose.scale(size, size, size);
        VertexConsumer vertices = buffers.getBuffer(AstralSlashRenderType.ENERGY);
        Matrix4f matrix = pose.last().pose();
        float red = 0.30F;
        float green = 0.055F;
        float blue = 0.58F;
        // Three translucent echoes stretch backwards and fade towards the tail.
        for (int echo = 3; echo >= 1; echo--)
            arc(vertices, matrix, 0.28F + echo * 0.035F, -echo * 0.40F,
                    red, green, blue, 0.10F / echo, 0.0F);
        arc(vertices, matrix, 0.38F, 0.0F, red, green, blue, 0.16F, 0.0F);
        arc(vertices, matrix, 0.19F, 0.0F, red, green, blue, 0.80F, 0.0F);
        arc(vertices, matrix, 0.035F, 0.01F, 0.65F, 0.32F, 0.93F, 0.90F, 0.0F);
        // A thin second face gives the energy blade thickness without any cubes or texture.
        arc(vertices, matrix, 0.15F, 0.0F, red, green, blue, 0.25F, 0.035F);
        arc(vertices, matrix, 0.15F, 0.0F, red, green, blue, 0.25F, -0.035F);
        pose.popPose();
        super.render(entity, yaw, partialTick, pose, buffers, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(AstralSlashEntity entity)
    {
        return UNUSED_TEXTURE;
    }

    private static void arc(VertexConsumer out, Matrix4f matrix, float width, float tail,
                            float red, float green, float blue, float alpha, float y)
    {
        for (int segment = 0; segment < SEGMENTS; segment++)
        {
            float a = -1.45F + 2.9F * segment / SEGMENTS;
            float b = -1.45F + 2.9F * (segment + 1) / SEGMENTS;
            float taperA = (float) Math.pow(Math.max(0.0D, Math.cos(a / 1.45F * Math.PI / 2.0D)), 0.7D);
            float taperB = (float) Math.pow(Math.max(0.0D, Math.cos(b / 1.45F * Math.PI / 2.0D)), 0.7D);
            float xa = (float) Math.sin(a) * 1.55F, xb = (float) Math.sin(b) * 1.55F;
            float za = (float) Math.cos(a) * 0.85F + tail;
            float zb = (float) Math.cos(b) * 0.85F + tail;
            vertex(out, matrix, xa, y, za, red, green, blue, alpha * taperA);
            vertex(out, matrix, xb, y, zb, red, green, blue, alpha * taperB);
            vertex(out, matrix, xb, y, zb - width * taperB, red, green, blue, 0.0F);
            vertex(out, matrix, xa, y, za - width * taperA, red, green, blue, 0.0F);
        }
    }

    private static void vertex(VertexConsumer out, Matrix4f matrix, float x, float y, float z,
                               float red, float green, float blue, float alpha)
    {
        out.vertex(matrix, x, y, z).color(red, green, blue, alpha).endVertex();
    }
}
