package com.example.tutorial.event;

import com.example.tutorial.item.ModItems;
import net.minecraft.world.item.ItemStack;

public final class WeaponMastery
{
    public static final int MAX_LEVEL = 20;
    private static final String XP = "tutorial_mod:mastery_xp";
    private static final String BRANCH = "tutorial_mod:mastery_branch";

    public enum Branch { NONE, STAR_BLADE, MOON_TRACE, STAR_CORE }

    private WeaponMastery() {}

    public static int xp(ItemStack stack) { return stack.hasTag()?Math.max(0,Math.min(MAX_LEVEL*100,stack.getTag().getInt(XP))):0; }
    public static int level(ItemStack stack) { return Math.min(MAX_LEVEL, xp(stack) / 100); }
    public static int progress(ItemStack stack) { return level(stack)>=MAX_LEVEL?100:xp(stack)%100; }

    public static void add(ItemStack stack, int amount)
    {
        if (!stack.is(ModItems.ICE_SWORD.get())) return;
        stack.getOrCreateTag().putInt(XP, Math.min(MAX_LEVEL * 100, xp(stack) + Math.max(0, amount)));
    }

    public static Branch branch(ItemStack stack)
    {
        int ordinal = stack.hasTag()?stack.getTag().getInt(BRANCH):0;
        return ordinal >= 0 && ordinal < Branch.values().length ? Branch.values()[ordinal] : Branch.NONE;
    }

    public static Branch cycleBranch(ItemStack stack)
    {
        Branch next = switch (branch(stack))
        {
            case NONE, STAR_CORE -> Branch.STAR_BLADE;
            case STAR_BLADE -> Branch.MOON_TRACE;
            case MOON_TRACE -> Branch.STAR_CORE;
        };
        stack.getOrCreateTag().putInt(BRANCH, next.ordinal());
        return next;
    }

    public static double masteryDamage(ItemStack stack) { return 1.0D + level(stack) * 0.01D; }
}
