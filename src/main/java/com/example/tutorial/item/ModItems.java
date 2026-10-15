package com.example.tutorial.item;

import com.example.tutorial.TutorialMod; // 导入你自己的主类！
import com.example.tutorial.block.ModBlocks;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Tiers;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.registries.ForgeRegistries;

public class ModItems {
    // 使用 TutorialMod.MOD_ID
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS.getRegistryKey(), TutorialMod.MOD_ID);

    /** 星环重剑的耐久。 */
    public static final int ICE_SWORD_DURABILITY = 25_000;

    // 星环重剑：钻石材质，面板显示 攻击力 30 / 攻速 2.5
    //
    // SwordItem 的第二个参数不是最终攻击力，而是「材质加成之前」的值，且玩家自身也有基础值：
    //     最终攻击力 = 1.0(玩家基础) + 参数 + 3(钻石材质加成)    → 1 + 26 + 3 = 30
    //     最终攻速   = 4.0(玩家基础) + 参数                     → 4.0 - 1.5 = 2.5
    //
    // 耐久必须写在 Properties 上：
    //   TieredItem 构造时会调 pProperties.defaultDurability(tier.getUses())，
    //   而 defaultDurability 的实现在 maxDamage != 0 时是空操作
    //   （`return this.maxDamage == 0 ? this.durability(pMaxDamage) : this;`），
    //   所以这里先设好 25000，材质那 1561 就覆盖不掉它。
    //
    // 外观用的是 3D 模型 models/item/ice_sword.json（星环重剑），不是平面贴图。
    public static final RegistryObject<Item> ICE_SWORD =
            ITEMS.register("ice_sword", () -> new AstralGreatswordItem(
                    Tiers.DIAMOND, 26, -1.5F,
                    new Item.Properties().durability(ICE_SWORD_DURABILITY)));

    public static final RegistryObject<Item> STAR_ORE = ITEMS.register("star_ore",
            () -> new BlockItem(ModBlocks.STAR_ORE.get(), new Item.Properties()));

    public static final RegistryObject<Item> STAR_INGOT = ITEMS.register("star_ingot",
            () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> STAR_INGOT_BLOCK = ITEMS.register("star_ingot_block",
            () -> new BlockItem(ModBlocks.STAR_INGOT_BLOCK.get(), new Item.Properties()));

    public static final RegistryObject<Item> ASTRAL_GUIDE = ITEMS.register("astral_guide",
            () -> new AstralGuideBookItem(new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> STAR_HELMET = ITEMS.register("star_helmet",
            () -> new StarArmorItem(ArmorItem.Type.HELMET, new Item.Properties().fireResistant()));
    public static final RegistryObject<Item> STAR_CHESTPLATE = ITEMS.register("star_chestplate",
            () -> new StarArmorItem(ArmorItem.Type.CHESTPLATE, new Item.Properties().fireResistant()));
    public static final RegistryObject<Item> STAR_LEGGINGS = ITEMS.register("star_leggings",
            () -> new StarArmorItem(ArmorItem.Type.LEGGINGS, new Item.Properties().fireResistant()));
    public static final RegistryObject<Item> STAR_BOOTS = ITEMS.register("star_boots",
            () -> new StarArmorItem(ArmorItem.Type.BOOTS, new Item.Properties().fireResistant()));

    public static final RegistryObject<Item> COSMIC_HELMET = ITEMS.register("cosmic_helmet",
            () -> new StarArmorItem(ArmorItem.Type.HELMET, new Item.Properties().fireResistant(), true));
    public static final RegistryObject<Item> COSMIC_CHESTPLATE = ITEMS.register("cosmic_chestplate",
            () -> new StarArmorItem(ArmorItem.Type.CHESTPLATE, new Item.Properties().fireResistant(), true));
    public static final RegistryObject<Item> COSMIC_LEGGINGS = ITEMS.register("cosmic_leggings",
            () -> new StarArmorItem(ArmorItem.Type.LEGGINGS, new Item.Properties().fireResistant(), true));
    public static final RegistryObject<Item> COSMIC_BOOTS = ITEMS.register("cosmic_boots",
            () -> new StarArmorItem(ArmorItem.Type.BOOTS, new Item.Properties().fireResistant(), true));
    public static final RegistryObject<Item> STAR_SINGULARITY = ITEMS.register("star_singularity",
            () -> new Item(new Item.Properties().fireResistant().rarity(net.minecraft.world.item.Rarity.EPIC)));
    public static final RegistryObject<Item> STAR_UPGRADE_TEMPLATE = ITEMS.register("star_upgrade_template",
            () -> new net.minecraft.world.item.SmithingTemplateItem(
                    net.minecraft.network.chat.Component.translatable("template.tutorial_mod.applies"),
                    net.minecraft.network.chat.Component.translatable("item.tutorial_mod.star_singularity"),
                    net.minecraft.network.chat.Component.translatable("template.tutorial_mod.upgrade"),
                    net.minecraft.network.chat.Component.translatable("template.tutorial_mod.base"),
                    net.minecraft.network.chat.Component.translatable("template.tutorial_mod.addition"),
                    java.util.List.of(net.minecraft.resources.ResourceLocation.withDefaultNamespace("item/empty_armor_slot_helmet"),
                            net.minecraft.resources.ResourceLocation.withDefaultNamespace("item/empty_armor_slot_chestplate"),
                            net.minecraft.resources.ResourceLocation.withDefaultNamespace("item/empty_armor_slot_leggings"),
                            net.minecraft.resources.ResourceLocation.withDefaultNamespace("item/empty_armor_slot_boots")),
                    java.util.List.of(net.minecraft.resources.ResourceLocation.withDefaultNamespace("item/empty_slot_ingot"))));

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
