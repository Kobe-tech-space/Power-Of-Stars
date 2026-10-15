package com.example.tutorial.event;

import net.minecraftforge.fml.ModList;

public final class AttributeCompatibility
{
    private AttributeCompatibility() {}
    public static boolean hasApothicAttributes() { return ModList.get().isLoaded("attributeslib"); }
    /** Apothic already replaces vanilla's armor ceiling with its own scalable formula. */
    public static double extraArmorReduction(double armor)
    {
        if (hasApothicAttributes()) return 0;
        return Math.min(com.example.tutorial.Config.armorBonusCap,
                Math.max(0, armor - com.example.tutorial.Config.armorBonusThreshold) * com.example.tutorial.Config.armorBonusPerPoint);
    }
}
