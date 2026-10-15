package com.example.tutorial.client;

import com.example.tutorial.item.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

public final class ClientGuideOpener
{
    private ClientGuideOpener() {}

    public static void open(InteractionHand hand)
    {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) return;
        ItemStack stack = minecraft.player.getItemInHand(hand);
        if (stack.is(ModItems.ASTRAL_GUIDE.get()))
            vazkii.patchouli.api.PatchouliAPI.get().openBookGUI(
                    net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("tutorial_mod", "astral_guide"));
    }
}
