package com.example.tutorial.bounty;

import com.example.tutorial.item.ModItems;
import com.example.tutorial.enchantment.ModEnchantments;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import java.util.List;

/** Stable indexes are persisted: append entries, never reorder them. */
public final class BountyCatalog {
    public record Quest(String title, String objective, String trigger, String target, int goal, String tier, int color, int xp) {}
    public static final List<Quest> QUESTS = List.of(
        new Quest("亡灵清剿", "击败 20 只僵尸", "kill", "minecraft:zombie", 20, "普通", 0xBAC5D4, 20),
        new Quest("断弦行动", "击败 16 只骷髅", "kill", "minecraft:skeleton", 16, "普通", 0xBAC5D4, 25),
        new Quest("拆弹专家", "击败 12 只苦力怕", "kill", "minecraft:creeper", 12, "普通", 0xBAC5D4, 30),
        new Quest("末影追猎", "击败 10 只末影人", "kill", "minecraft:enderman", 10, "稀有", 0x73B8F4, 45),
        new Quest("烈焰远征", "击败 16 只烈焰人", "kill", "minecraft:blaze", 16, "稀有", 0x73B8F4, 50),
        new Quest("深海巡猎", "击败 16 只守卫者", "kill", "minecraft:guardian", 16, "稀有", 0x73B8F4, 60),
        new Quest("精英克星", "击败 12 只本模组星辰精英怪", "elite", "", 12, "史诗", 0xB18CF2, 100),
        new Quest("凋零猎手", "击败 2 只凋零", "kill", "minecraft:wither", 2, "史诗", 0xB18CF2, 150),
        new Quest("星辰试炼", "完成 3 次信标星辰试炼", "trial", "", 3, "神话", 0xE6BA70, 200),
        new Quest("终末契约", "击败 1 只末影龙", "kill", "minecraft:ender_dragon", 1, "神话", 0xE6BA70, 300)
    );
    private BountyCatalog() {}
    public static List<ItemStack> rewards(int id) {
        return switch (id) {
            case 0 -> List.of(new ItemStack(Items.IRON_INGOT, 4), new ItemStack(Items.BREAD, 4));
            case 1 -> List.of(new ItemStack(Items.ARROW, 16), new ItemStack(Items.GOLD_INGOT, 2));
            case 2 -> List.of(new ItemStack(Items.TNT, 2), new ItemStack(Items.EMERALD, 2));
            case 3 -> List.of(new ItemStack(Items.ENDER_PEARL, 2), new ItemStack(Items.EXPERIENCE_BOTTLE, 4));
            case 4 -> List.of(new ItemStack(Items.BLAZE_ROD, 3), new ItemStack(Items.GOLDEN_APPLE, 1));
            case 5 -> List.of(new ItemStack(Items.PRISMARINE_CRYSTALS, 8), new ItemStack(Items.DIAMOND));
            case 6 -> List.of(new ItemStack(Items.DIAMOND, 3), new ItemStack(Items.EMERALD, 4));
            case 7 -> List.of(new ItemStack(Items.NETHERITE_INGOT, 1), new ItemStack(Items.DIAMOND, 4));
            case 8 -> List.of(new ItemStack(ModItems.STAR_INGOT.get(), 8), EnchantedBookItem.createForEnchantment(new EnchantmentInstance(ModEnchantments.ULTIMATE_SHARPNESS.get(), 1)));
            case 9 -> List.of(new ItemStack(ModItems.STAR_INGOT.get(), 16), EnchantedBookItem.createForEnchantment(new EnchantmentInstance(ModEnchantments.SUPREME_SHARPNESS.get(), 1)));
            default -> List.of();
        };
    }
}
