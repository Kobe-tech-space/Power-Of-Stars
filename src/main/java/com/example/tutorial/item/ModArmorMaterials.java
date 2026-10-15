package com.example.tutorial.item;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.function.Supplier;

public enum ModArmorMaterials implements ArmorMaterial
{
    STAR("star", 25, SoundEvents.ARMOR_EQUIP_NETHERITE,
            50.0F, 0.25F, () -> Ingredient.of(ModItems.STAR_INGOT.get())),
    COSMIC("cosmic", 25, SoundEvents.ARMOR_EQUIP_NETHERITE,
            50.0F, 0.25F, () -> Ingredient.of(ModItems.STAR_INGOT.get()));

    /** 四件套统一的耐久 —— 头盔、胸甲、护腿、靴子都是这个值。 */
    private static final int STAR_DURABILITY = 25_000;

    private final String name;
    private final int enchantmentValue;
    private final SoundEvent equipSound;
    private final float toughness;
    private final float knockbackResistance;
    private final Supplier<Ingredient> repairIngredient;

    ModArmorMaterials(String name, int enchantmentValue,
                      SoundEvent equipSound, float toughness, float knockbackResistance,
                      Supplier<Ingredient> repairIngredient)
    {
        this.name = name;
        this.enchantmentValue = enchantmentValue;
        this.equipSound = equipSound;
        this.toughness = toughness;
        this.knockbackResistance = knockbackResistance;
        this.repairIngredient = repairIngredient;
    }

    // 四件都给同一个耐久值（不再按部位用不同基数 × 倍率）。
    // ArmorItem 的构造会调 pProperties.defaultDurability(getDurabilityForType(type))，
    // 而 defaultDurability 在 Properties 已设过耐久时是空操作 —— 这里没设，所以这个值直接生效。
    @Override public int getDurabilityForType(ArmorItem.Type type) { return STAR_DURABILITY; }
    @Override public int getDefenseForType(ArmorItem.Type type) { return this == STAR ? 20 : 50; }
    @Override public int getEnchantmentValue() { return this.enchantmentValue; }
    @Override public SoundEvent getEquipSound() { return this.equipSound; }
    @Override public Ingredient getRepairIngredient() { return this.repairIngredient.get(); }
    @Override public String getName() { return "tutorial_mod:" + this.name; }
    @Override public float getToughness() { return this.toughness; }
    @Override public float getKnockbackResistance() { return this.knockbackResistance; }
}
