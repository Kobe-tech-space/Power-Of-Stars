package com.example.tutorial.item;

import com.example.tutorial.TutorialMod;
import com.example.tutorial.enchantment.ModEnchantments;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

/** A single home for all mod items, including every supported enchantment level. */
public final class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, TutorialMod.MOD_ID);
    public static final RegistryObject<CreativeModeTab> ASTRAL = TABS.register("astral", () ->
            CreativeModeTab.builder().title(Component.translatable("itemGroup.tutorial_mod.astral"))
                    .icon(() -> new ItemStack(ModItems.ICE_SWORD.get()))
                    .displayItems((parameters, output) -> {
                        ModItems.ITEMS.getEntries().forEach(item -> output.accept(item.get()));
                        ModEnchantments.ENCHANTMENTS.getEntries().forEach(entry -> {
                            var enchantment = entry.get();
                            for (int level = enchantment.getMinLevel(); level <= enchantment.getMaxLevel(); level++)
                                output.accept(EnchantedBookItem.createForEnchantment(new EnchantmentInstance(enchantment, level)));
                        });
                    }).build());
    private ModCreativeTabs() {}

    public static void removeFromVanillaTabs(net.minecraftforge.event.BuildCreativeModeTabContentsEvent event) {
        // Keep global search intact, but do not let vanilla's automatic book population
        // duplicate our enchantments in Tools/Combat or Ingredients.
        if (!event.getTabKey().location().getNamespace().equals("minecraft")
                || event.getTabKey().equals(net.minecraft.world.item.CreativeModeTabs.SEARCH)) return;
        var iterator = event.getEntries().iterator();
        while (iterator.hasNext()) {
            ItemStack stack = iterator.next().getKey();
            var id = net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(stack.getItem());
            boolean ownItem = id != null && id.getNamespace().equals(TutorialMod.MOD_ID);
            boolean ownBook = stack.getItem() instanceof EnchantedBookItem
                    && net.minecraft.world.item.enchantment.EnchantmentHelper.getEnchantments(stack)
                    .keySet().stream().anyMatch(enchantment -> {
                        var enchantmentId = net.minecraftforge.registries.ForgeRegistries.ENCHANTMENTS.getKey(enchantment);
                        return enchantmentId != null && enchantmentId.getNamespace().equals(TutorialMod.MOD_ID);
                    });
            if (ownItem || ownBook) iterator.remove();
        }
    }
}
