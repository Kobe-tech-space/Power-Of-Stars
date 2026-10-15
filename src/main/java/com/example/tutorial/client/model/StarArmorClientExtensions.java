package com.example.tutorial.client.model;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;

/** Supplies the Blockbench armor model to Forge's normal armor renderer. */
public final class StarArmorClientExtensions implements IClientItemExtensions
{
    public static final StarArmorClientExtensions INSTANCE = new StarArmorClientExtensions();
    private CosmicArmorModel<LivingEntity> model;

    private StarArmorClientExtensions() {}

    @Override
    public HumanoidModel<?> getHumanoidArmorModel(LivingEntity entity, ItemStack stack,
                                                   EquipmentSlot slot, HumanoidModel<?> original)
    {
        if (this.model == null)
        {
            this.model = new CosmicArmorModel<>(Minecraft.getInstance().getEntityModels()
                    .bakeLayer(CosmicArmorModel.LAYER));
        }
        copyPose(original, this.model);
        this.model.prepareForSlot(slot,
                stack.getItem() instanceof com.example.tutorial.item.StarArmorItem armor && armor.isAdvanced(), false);
        return this.model;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void copyPose(HumanoidModel<?> source, HumanoidModel<?> target)
    {
        ((HumanoidModel) source).copyPropertiesTo((HumanoidModel) target);
    }
}
