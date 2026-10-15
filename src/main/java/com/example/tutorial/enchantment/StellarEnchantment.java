package com.example.tutorial.enchantment;
import com.example.tutorial.item.AstralGreatswordItem;
import com.example.tutorial.item.StarArmorItem;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentCategory;
public final class StellarEnchantment extends Enchantment {
    private final EquipmentSlot slot;
    public StellarEnchantment(EquipmentSlot slot) {
        super(Rarity.RARE, slot == EquipmentSlot.MAINHAND ? EnchantmentCategory.WEAPON : EnchantmentCategory.ARMOR, new EquipmentSlot[]{slot});
        this.slot = slot;
    }
    @Override public boolean canEnchant(ItemStack stack) {
        return slot == EquipmentSlot.MAINHAND ? stack.getItem() instanceof AstralGreatswordItem
                : stack.getItem() instanceof StarArmorItem armor && armor.getType().getSlot() == slot;
    }
    @Override public boolean canApplyAtEnchantingTable(ItemStack stack) { return canEnchant(stack); }
    @Override public int getMaxLevel() { return 3; }
    @Override public int getMinCost(int level) { return 15 + (level - 1) * 10; }
    @Override public int getMaxCost(int level) { return getMinCost(level) + 30; }
}
