package com.example.tutorial.client.model;

import com.example.tutorial.TutorialMod;
import com.example.tutorial.item.StarArmorItem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;

/** Full-bright second pass on crystal/trim cubes using each tier's own UV atlas. */
public class CosmicArmorGlowLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>>
{
    private static final EquipmentSlot[] SLOTS = {
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
    };

    private final CosmicArmorModel<AbstractClientPlayer> model;

    public CosmicArmorGlowLayer(
            RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent,
            CosmicArmorModel<AbstractClientPlayer> model)
    {
        super(parent);
        this.model = model;
    }

    @Override
    public void render(PoseStack pose, MultiBufferSource buffers, int packedLight,
                       AbstractClientPlayer player, float limbSwing, float limbSwingAmount,
                       float partialTick, float ageInTicks, float netHeadYaw, float headPitch)
    {
        this.getParentModel().copyPropertiesTo(this.model);
        for (EquipmentSlot slot : SLOTS)
        {
            if (!(player.getItemBySlot(slot).getItem() instanceof StarArmorItem armor)) continue;
            this.model.prepareForSlot(slot, armor.isAdvanced(), true);
            var texture = ResourceLocation.fromNamespaceAndPath(TutorialMod.MOD_ID,
                    "textures/models/armor/" + (armor.isAdvanced() ? "cosmic_high" : "cosmic_low") + ".png");
            this.model.renderToBuffer(pose,
                    buffers.getBuffer(RenderType.entityTranslucentEmissive(texture)),
                    LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY,
                    1.0F, 1.0F, 1.0F, 0.92F);
        }
    }
}
