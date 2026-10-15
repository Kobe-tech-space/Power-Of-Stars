package com.example.tutorial.client.model;

import com.example.tutorial.TutorialMod;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

/**
 * A two-sided emissive disc used by {@link StarHaloLayer}. Keeping the halo as
 * a textured plane preserves transparent star rays and fine concentric rings.
 */
public class StarHaloModel<T extends Entity> extends EntityModel<T>
{
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(TutorialMod.MOD_ID, "star_halo"), "main");

    public StarHaloModel(ModelPart ignored) {}

    public static LayerDefinition createBodyLayer()
    {
        return LayerDefinition.create(new MeshDefinition(), 16, 16);
    }

    @Override public void setupAnim(T entity, float a, float b, float c, float d, float e) {}

    @Override
    public void renderToBuffer(PoseStack pose, VertexConsumer buffer, int light, int overlay,
                               float red, float green, float blue, float alpha)
    {
        PoseStack.Pose current = pose.last();
        Matrix4f position = current.pose();
        Matrix3f normal = current.normal();
        float radius = 0.92F;

        // Front face (the player-facing side).
        vertex(buffer, position, normal, -radius, -radius, 0.0F, 0.0F, 1.0F,
                light, overlay, red, green, blue, alpha, -1.0F);
        vertex(buffer, position, normal, radius, -radius, 0.0F, 1.0F, 1.0F,
                light, overlay, red, green, blue, alpha, -1.0F);
        vertex(buffer, position, normal, radius, radius, 0.0F, 1.0F, 0.0F,
                light, overlay, red, green, blue, alpha, -1.0F);
        vertex(buffer, position, normal, -radius, radius, 0.0F, 0.0F, 0.0F,
                light, overlay, red, green, blue, alpha, -1.0F);

        // Back face so other players can also see it from behind.
        vertex(buffer, position, normal, radius, -radius, 0.002F, 1.0F, 1.0F,
                light, overlay, red, green, blue, alpha, 1.0F);
        vertex(buffer, position, normal, -radius, -radius, 0.002F, 0.0F, 1.0F,
                light, overlay, red, green, blue, alpha, 1.0F);
        vertex(buffer, position, normal, -radius, radius, 0.002F, 0.0F, 0.0F,
                light, overlay, red, green, blue, alpha, 1.0F);
        vertex(buffer, position, normal, radius, radius, 0.002F, 1.0F, 0.0F,
                light, overlay, red, green, blue, alpha, 1.0F);
    }

    private static void vertex(VertexConsumer consumer, Matrix4f position, Matrix3f normal,
                               float x, float y, float z, float u, float v,
                               int light, int overlay, float red, float green, float blue,
                               float alpha, float normalZ)
    {
        consumer.vertex(position, x, y, z)
                .color(red, green, blue, alpha)
                .uv(u, v)
                .overlayCoords(overlay)
                .uv2(light)
                .normal(normal, 0.0F, 0.0F, normalZ)
                .endVertex();
    }
}
