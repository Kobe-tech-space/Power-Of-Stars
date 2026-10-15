package com.example.tutorial.item;

import com.example.tutorial.client.model.StarArmorClientExtensions;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;

import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Consumer;

public class StarArmorItem extends ArmorItem
{
    private final boolean advanced;
    public StarArmorItem(Type type, Properties properties)
    {
        this(type, properties, false);
    }

    public StarArmorItem(Type type, Properties properties, boolean advanced)
    {
        super(advanced ? ModArmorMaterials.COSMIC : ModArmorMaterials.STAR, type, properties);
        this.advanced = advanced;
    }

    public boolean isAdvanced() { return this.advanced; }
    @Override public int getMaxDamage(ItemStack stack) {return com.example.tutorial.event.AstralEquipment.maxDamage(stack,super.getMaxDamage(stack));}

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer)
    {
        consumer.accept(StarArmorClientExtensions.INSTANCE);
    }

    @Override
    public String getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot, String type)
    {
        return "tutorial_mod:textures/models/armor/" + (advanced ? "cosmic_high" : "cosmic_low") + ".png";
    }

    public static boolean hasFullSet(LivingEntity entity)
    {
        return fullSetTier(entity) > 0;
    }

    /** Mixed tiers retain individual attributes but never activate a set bonus. */
    public static int fullSetTier(LivingEntity entity)
    {
        int tier = 0;
        for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET})
        {
            if (!(entity.getItemBySlot(slot).getItem() instanceof StarArmorItem armor)
                    || armor.getType().getSlot() != slot) return 0;
            int current = armor.advanced ? 2 : 1;
            if (tier != 0 && current != tier) return 0;
            tier = current;
        }
        return tier;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag)
    {
        String tier=advanced?"high":"low";
        double immunity=com.example.tutorial.config.ModSettings.d("armor."+tier+".pieceImmunity");
        int totalArmor=com.example.tutorial.config.ModSettings.i("armor."+tier+".setBonus");
        for(String part:java.util.List.of("helmet","chestplate","leggings","boots")) totalArmor+=com.example.tutorial.config.ModSettings.i("armor."+tier+"."+part);
        tooltip.add(Component.translatable("tooltip.tutorial_mod.damage_immunity", Math.round(immunity*100)+"%")
                .withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.translatable("tooltip.tutorial_mod.configured_set",totalArmor,Math.round(Math.min(.95,immunity*4+com.example.tutorial.config.ModSettings.d("armor."+tier+".setImmunity"))*100),advanced?30:13)
                .withStyle(ChatFormatting.LIGHT_PURPLE));
        tooltip.add(Component.translatable("tooltip.tutorial_mod.piece_offense", advanced ? "5%" : "2%")
                .withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.translatable("tooltip.tutorial_mod.star_shield")
                .withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.translatable("tooltip.tutorial_mod.armor_skill")
                .withStyle(ChatFormatting.AQUA));
        super.appendHoverText(stack, level, tooltip, flag);
    }
}
