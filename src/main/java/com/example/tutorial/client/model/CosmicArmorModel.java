package com.example.tutorial.client.model;

import com.example.tutorial.TutorialMod;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

import java.util.List;

/**
 * Runtime version of the supplied Blockbench model. The original project uses
 * per-face UV rectangles, so the boxes are emitted directly instead of using
 * vanilla's box-UV layout.
 */
public class CosmicArmorModel<T extends LivingEntity> extends HumanoidModel<T>
{
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(TutorialMod.MOD_ID, "cosmic_armor"), "main");
    private List<ArmorCube> cubes;
    private boolean glowOnly;

    public void prepareForSlot(EquipmentSlot slot, boolean advanced, boolean glowOnly)
    {
        this.renderingSlot = slot;
        this.cubes = advanced ? HIGH_CUBES.get() : LOW_CUBES.get();
        this.glowOnly = glowOnly;
    }

    private EquipmentSlot renderingSlot = EquipmentSlot.CHEST;

    public CosmicArmorModel(ModelPart root)
    {
        super(root);
        this.cubes = LOW_CUBES.get();
    }

    public static LayerDefinition createBodyLayer()
    {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        CubeListBuilder empty = CubeListBuilder.create();
        root.addOrReplaceChild("head", empty, PartPose.ZERO);
        root.addOrReplaceChild("hat", empty, PartPose.ZERO);
        root.addOrReplaceChild("body", empty, PartPose.ZERO);
        root.addOrReplaceChild("right_arm", empty, PartPose.offset(-5.0F, 2.0F, 0.0F));
        root.addOrReplaceChild("left_arm", empty, PartPose.offset(5.0F, 2.0F, 0.0F));
        root.addOrReplaceChild("right_leg", empty, PartPose.offset(-1.9F, 12.0F, 0.0F));
        root.addOrReplaceChild("left_leg", empty, PartPose.offset(1.9F, 12.0F, 0.0F));
        return LayerDefinition.create(mesh, 128, 128);
    }

    public void prepareForSlot(EquipmentSlot slot)
    {
        prepareForSlot(slot, false, false);
    }

    @Override
    public void renderToBuffer(PoseStack pose, VertexConsumer buffer, int light, int overlay,
                               float red, float green, float blue, float alpha)
    {
        renderGroup(pose, buffer, light, overlay, red, green, blue, alpha, Group.HEAD, this.head);
        renderGroup(pose, buffer, light, overlay, red, green, blue, alpha, Group.BODY, this.body);
        renderGroup(pose, buffer, light, overlay, red, green, blue, alpha, Group.RIGHT_ARM, this.rightArm);
        renderGroup(pose, buffer, light, overlay, red, green, blue, alpha, Group.LEFT_ARM, this.leftArm);
        renderGroup(pose, buffer, light, overlay, red, green, blue, alpha, Group.RIGHT_LEG, this.rightLeg);
        renderGroup(pose, buffer, light, overlay, red, green, blue, alpha, Group.LEFT_LEG, this.leftLeg);
    }

    private void renderGroup(PoseStack pose, VertexConsumer buffer, int light, int overlay,
                             float red, float green, float blue, float alpha,
                             Group group, ModelPart animatedPart)
    {
        pose.pushPose();
        animatedPart.translateAndRotate(pose);
        for (ArmorCube cube : this.cubes)
        {
            if (cube.group == group && cube.slot == this.renderingSlot && (!glowOnly || cube.glow))
            {
                cube.render(pose, buffer, light, overlay, red, green, blue, alpha);
            }
        }
        pose.popPose();
    }

    private enum Group
    {
        HEAD(0.0F, 24.0F, 0.0F), BODY(0.0F, 24.0F, 0.0F),
        RIGHT_ARM(-5.0F, 22.0F, 0.0F), LEFT_ARM(5.0F, 22.0F, 0.0F),
        RIGHT_LEG(-1.9F, 12.0F, 0.0F), LEFT_LEG(1.9F, 12.0F, 0.0F);

        final float x;
        final float y;
        final float z;

        Group(float x, float y, float z)
        {
            this.x = x;
            this.y = y;
            this.z = z;
        }
    }

    private record UV(float u0, float v0, float u1, float v1) {}

    private record ArmorCube(Group group, EquipmentSlot slot,
                             float fromX, float fromY, float fromZ,
                             float toX, float toY, float toZ,
                             UV north, UV south, UV east, UV west, UV up, UV down,
                             float ox, float oy, float oz, float rx, float ry, float rz, boolean glow)
    {
        void render(PoseStack pose, VertexConsumer buffer, int light, int overlay,
                    float red, float green, float blue, float alpha)
        {
            pose.pushPose();
            float pivotX = (ox - group.x) / 16F, pivotY = (group.y - oy) / 16F, pivotZ = (oz - group.z) / 16F;
            pose.translate(pivotX, pivotY, pivotZ);
            pose.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(-rz));
            pose.mulPose(com.mojang.math.Axis.YP.rotationDegrees(ry));
            pose.mulPose(com.mojang.math.Axis.XP.rotationDegrees(-rx));
            pose.translate(-pivotX, -pivotY, -pivotZ);
            float x0 = (fromX - group.x) / 16.0F;
            float x1 = (toX - group.x) / 16.0F;
            float y0 = (group.y - toY) / 16.0F;
            float y1 = (group.y - fromY) / 16.0F;
            float z0 = (fromZ - group.z) / 16.0F;
            float z1 = (toZ - group.z) / 16.0F;
            PoseStack.Pose current = pose.last();
            Matrix4f matrix = current.pose();
            Matrix3f normals = current.normal();

            quad(buffer, matrix, normals, light, overlay, red, green, blue, alpha, north,
                    x0, y0, z0, x1, y0, z0, x1, y1, z0, x0, y1, z0, 0, 0, -1);
            quad(buffer, matrix, normals, light, overlay, red, green, blue, alpha, south,
                    x1, y0, z1, x0, y0, z1, x0, y1, z1, x1, y1, z1, 0, 0, 1);
            quad(buffer, matrix, normals, light, overlay, red, green, blue, alpha, east,
                    x1, y0, z0, x1, y0, z1, x1, y1, z1, x1, y1, z0, 1, 0, 0);
            quad(buffer, matrix, normals, light, overlay, red, green, blue, alpha, west,
                    x0, y0, z1, x0, y0, z0, x0, y1, z0, x0, y1, z1, -1, 0, 0);
            quad(buffer, matrix, normals, light, overlay, red, green, blue, alpha, up,
                    x0, y0, z1, x1, y0, z1, x1, y0, z0, x0, y0, z0, 0, -1, 0);
            quad(buffer, matrix, normals, light, overlay, red, green, blue, alpha, down,
                    x0, y1, z0, x1, y1, z0, x1, y1, z1, x0, y1, z1, 0, 1, 0);
            pose.popPose();
        }
    }

    private static void quad(VertexConsumer buffer, Matrix4f matrix, Matrix3f normals,
                             int light, int overlay, float red, float green, float blue, float alpha, UV uv,
                             float x0, float y0, float z0, float x1, float y1, float z1,
                             float x2, float y2, float z2, float x3, float y3, float z3,
                             float nx, float ny, float nz)
    {
        vertex(buffer, matrix, normals, x0, y0, z0, uv.u0, uv.v0,
                light, overlay, red, green, blue, alpha, nx, ny, nz);
        vertex(buffer, matrix, normals, x1, y1, z1, uv.u1, uv.v0,
                light, overlay, red, green, blue, alpha, nx, ny, nz);
        vertex(buffer, matrix, normals, x2, y2, z2, uv.u1, uv.v1,
                light, overlay, red, green, blue, alpha, nx, ny, nz);
        vertex(buffer, matrix, normals, x3, y3, z3, uv.u0, uv.v1,
                light, overlay, red, green, blue, alpha, nx, ny, nz);
    }

    private static void vertex(VertexConsumer buffer, Matrix4f matrix, Matrix3f normals,
                               float x, float y, float z, float u, float v,
                               int light, int overlay, float red, float green, float blue, float alpha,
                               float nx, float ny, float nz)
    {
        buffer.vertex(matrix, x, y, z).color(red, green, blue, alpha).uv(u, v)
                .overlayCoords(overlay).uv2(light).normal(normals, nx, ny, nz).endVertex();
    }

    private static UV uv(float u0, float v0, float u1, float v1)
    {
        return new UV(u0, v0, u1, v1);
    }

    // Layer registration runs before packs exist. Load only when a renderer is actually built.
    private static final java.util.function.Supplier<List<ArmorCube>> LOW_CUBES =
            com.google.common.base.Suppliers.memoize(() -> load("cosmic_low"));
    private static final java.util.function.Supplier<List<ArmorCube>> HIGH_CUBES =
            com.google.common.base.Suppliers.memoize(() -> load("cosmic_high"));

    private static List<ArmorCube> load(String name)
    {
        var location = ResourceLocation.fromNamespaceAndPath(TutorialMod.MOD_ID, "armor_models/" + name + ".json");
        try (var reader = net.minecraft.client.Minecraft.getInstance().getResourceManager().openAsReader(location))
        {
            var result = new java.util.ArrayList<ArmorCube>();
            for (var element : com.google.gson.JsonParser.parseReader(reader).getAsJsonArray())
            {
                var cube = element.getAsJsonObject();
                var from = cube.getAsJsonArray("from");
                var to = cube.getAsJsonArray("to");
                var origin = cube.getAsJsonArray("origin");
                var rotation = cube.getAsJsonArray("rotation");
                var faces = cube.getAsJsonObject("faces");
                result.add(new ArmorCube(Group.valueOf(cube.get("group").getAsString()),
                        EquipmentSlot.valueOf(cube.get("slot").getAsString()),
                        from.get(0).getAsFloat(), from.get(1).getAsFloat(), from.get(2).getAsFloat(),
                        to.get(0).getAsFloat(), to.get(1).getAsFloat(), to.get(2).getAsFloat(),
                        face(faces, "north"), face(faces, "south"), face(faces, "east"),
                        face(faces, "west"), face(faces, "up"), face(faces, "down"),
                        origin.get(0).getAsFloat(), origin.get(1).getAsFloat(), origin.get(2).getAsFloat(),
                        rotation.get(0).getAsFloat(), rotation.get(1).getAsFloat(), rotation.get(2).getAsFloat(),
                        cube.get("glow").getAsBoolean()));
            }
            return List.copyOf(result);
        }
        catch (java.io.IOException e) { throw new IllegalStateException("Missing cosmic armor model " + location, e); }
    }

    private static UV face(com.google.gson.JsonObject faces, String side)
    {
        var uv = faces.getAsJsonArray(side);
        return new UV(uv.get(0).getAsFloat(), uv.get(1).getAsFloat(), uv.get(2).getAsFloat(), uv.get(3).getAsFloat());
    }
}

