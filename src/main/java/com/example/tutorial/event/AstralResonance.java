package com.example.tutorial.event;

import com.example.tutorial.item.ModItems;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;

public final class AstralResonance
{
    private AstralResonance() {}

    public static int level(Player player)
    {
        int level = player.getMainHandItem().is(ModItems.ICE_SWORD.get()) ? 1 : 0;
        for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET})
            if (player.getItemBySlot(slot).getItem() instanceof com.example.tutorial.item.StarArmorItem) level++;
        return level;
    }
}
