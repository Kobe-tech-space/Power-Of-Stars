package com.example.tutorial;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;

/** Server-authoritative gameplay settings intended for modpacks. */
@Mod.EventBusSubscriber(modid = TutorialMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class Config
{
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    private static final ForgeConfigSpec.IntValue ENERGY_PER_HIT = integer("starEnergy.perHit", 5, 0, 100);
    private static final ForgeConfigSpec.IntValue ENERGY_PER_KILL = integer("starEnergy.perKill", 15, 0, 500);
    private static final ForgeConfigSpec.IntValue ENERGY_PER_CRIT = integer("starEnergy.perCritical", 3, 0, 100);
    private static final ForgeConfigSpec.IntValue ENERGY_DECAY_DELAY = integer("starEnergy.decayDelayTicks", 200, 0, 72000);
    private static final ForgeConfigSpec.IntValue ENERGY_DECAY = integer("starEnergy.decayPerSecond", 1, 0, 100);
    private static final ForgeConfigSpec.IntValue SWORD_SKILL_COST = integer("skills.tripleSlashCost", 50, 0, 1000);
    private static final ForgeConfigSpec.IntValue RADIAL_SKILL_COST = integer("skills.radialSlashCost", 75, 0, 1000);
    private static final ForgeConfigSpec.IntValue PARRY_COST = integer("skills.parryCost", 15, 0, 1000);
    private static final ForgeConfigSpec.IntValue MAX_SLASHES = integer("astralSlash.maxPerPlayer", 16, 1, 128);
    private static final ForgeConfigSpec.IntValue SLASH_LIFETIME = integer("astralSlash.maxLifetimeTicks", 100, 10, 1200);
    private static final ForgeConfigSpec.DoubleValue SLASH_RANGE = decimal("astralSlash.maxRange", 96.0D, 8.0D, 512.0D);
    /** 原版护甲减伤上限（CombatRules.MAX_ARMOR = 20），超出这个点数的护甲才享受额外减伤。 */
    private static final ForgeConfigSpec.IntValue ARMOR_BONUS_THRESHOLD = integer("armor.bonusThreshold", 20, 0, 200);
    /** 超出部分每 1 点护甲换算成多少额外减伤（0.002 = 每点 0.2%）。 */
    private static final ForgeConfigSpec.DoubleValue ARMOR_BONUS_PER_POINT = decimal("armor.bonusPerPoint", 0.002D, 0.0D, 0.05D);
    /** 额外减伤的上限，防止叠成无敌。 */
    private static final ForgeConfigSpec.DoubleValue ARMOR_BONUS_CAP = decimal("armor.bonusCap", 0.90D, 0.0D, 0.99D);
    private static final ForgeConfigSpec.BooleanValue SCREEN_EFFECTS = BUILDER.define("clientEffects.enabled", true);

    public static final ForgeConfigSpec SPEC = BUILDER.build();
    public static int energyPerHit = 5, energyPerKill = 15, energyPerCritical = 3;
    public static int energyDecayDelayTicks = 200, energyDecayPerSecond = 1;
    public static int swordSkillCost = 50, radialSkillCost = 75, parryCost = 15;
    public static int maxSlashes = 16, slashLifetimeTicks = 100;
    public static double slashRange = 96.0D;
    public static int armorBonusThreshold = 20;
    public static double armorBonusPerPoint = 0.002D;
    public static double armorBonusCap = 0.90D;
    public static boolean screenEffects = true;

    private Config() {}
    private static ForgeConfigSpec.IntValue integer(String path, int value, int min, int max) { return BUILDER.defineInRange(path, value, min, max); }
    private static ForgeConfigSpec.DoubleValue decimal(String path, double value, double min, double max) { return BUILDER.defineInRange(path, value, min, max); }

    @SubscribeEvent
    static void onLoad(ModConfigEvent event)
    {
        if (event.getConfig().getSpec() != SPEC) return;
        energyPerHit = ENERGY_PER_HIT.get(); energyPerKill = ENERGY_PER_KILL.get();
        energyPerCritical = ENERGY_PER_CRIT.get(); energyDecayDelayTicks = ENERGY_DECAY_DELAY.get();
        energyDecayPerSecond = ENERGY_DECAY.get(); swordSkillCost = SWORD_SKILL_COST.get();
        radialSkillCost = RADIAL_SKILL_COST.get(); parryCost = PARRY_COST.get();
        maxSlashes = Math.min(16,MAX_SLASHES.get()); slashLifetimeTicks = Math.min(100,SLASH_LIFETIME.get()); slashRange = SLASH_RANGE.get();
        screenEffects = SCREEN_EFFECTS.get();
        armorBonusThreshold = ARMOR_BONUS_THRESHOLD.get(); armorBonusPerPoint = ARMOR_BONUS_PER_POINT.get();
        armorBonusCap = ARMOR_BONUS_CAP.get();
    }
}
