package com.example.tutorial.client;

import com.example.tutorial.network.CombatFeedbackPacket;
import net.minecraft.Util;

/** Short-lived client-only state populated by combat feedback packets. */
public final class ClientCombatState
{
    private static final long COMBO_DURATION_MILLIS = 30_000L;
    private static final long CRIT_DURATION_MILLIS = 700L;
    private static final long MILESTONE_DURATION_MILLIS = 1_500L;

    private static int combo;
    private static int milestone;
    private static long comboExpiresAt;
    private static long critExpiresAt;
    private static long critStartedAt;
    private static long milestoneExpiresAt;
    private static float criticalDamage;
    private static int starEnergy;
    private static int maximumStarEnergy;
    private static float stellarShield;
    private static float maximumStellarShield = 50.0F;
    private static long shieldFlashUntil;
    private static long energyFlashUntil;
    private static float guardianBarrier;
    private static int domain;
    private static long domainUntil, breakReady, controlReady;

    public static void setDomains(float barrier, int kind, int ticks, int breakTicks, int controlTicks)
    {
        long now = Util.getMillis();
        if (barrier < guardianBarrier) shieldFlashUntil = now + 450;
        guardianBarrier = Math.max(0, Math.min(1000, barrier));
        domain = kind;
        long time = gameTime();
        domainUntil = time + ticks;
        breakReady = time + breakTicks;
        controlReady = time + controlTicks;
    }
    public static float guardianBarrier() { return guardianBarrier; }
    private static long gameTime()
    {
        var level = net.minecraft.client.Minecraft.getInstance().level;
        return level == null ? 0 : level.getGameTime();
    }
    public static int domain() { return gameTime() < domainUntil ? domain : 0; }
    public static int domainSeconds() { return (int)Math.max(0, (domainUntil - gameTime() + 19) / 20); }
    public static int domainCooldown(boolean ranged)
    {
        return (int)Math.max(0, ((ranged ? controlReady : breakReady) - gameTime() + 19) / 20);
    }

    private ClientCombatState()
    {
    }
    public static void reset() {
        combo=0;milestone=0;comboExpiresAt=0;critExpiresAt=0;critStartedAt=0;milestoneExpiresAt=0;criticalDamage=0;
        starEnergy=0;maximumStarEnergy=0;stellarShield=0;maximumStellarShield=50;shieldFlashUntil=0;energyFlashUntil=0;
        guardianBarrier=0;domain=0;domainUntil=0;breakReady=0;controlReady=0;
    }

    public static void accept(CombatFeedbackPacket packet)
    {
        long now = Util.getMillis();
        if (packet.combo() > 0)
        {
            combo = packet.combo();
            comboExpiresAt = now + COMBO_DURATION_MILLIS;
        }
        if (packet.milestone() > 0)
        {
            milestone = packet.milestone();
            milestoneExpiresAt = now + MILESTONE_DURATION_MILLIS;
        }
    }

    public static int combo()
    {
        return Util.getMillis() < comboExpiresAt ? combo : 0;
    }

    public static float remainingRatio()
    {
        return Math.max(0.0F, Math.min(1.0F,
                (comboExpiresAt - Util.getMillis()) / (float) COMBO_DURATION_MILLIS));
    }

    public static boolean showCritical()
    {
        return false;
    }

    public static float criticalDamage()
    {
        return criticalDamage;
    }

    /** 0 at the instant of the hit and 1 when the floating number disappears. */
    public static float criticalAnimationProgress()
    {
        if (!showCritical())
        {
            return 1.0F;
        }
        return Math.max(0.0F, Math.min(1.0F,
                (Util.getMillis() - critStartedAt) / (float) CRIT_DURATION_MILLIS));
    }

    public static int milestone()
    {
        return Util.getMillis() < milestoneExpiresAt ? milestone : 0;
    }

    public static void setAstralResources(int energy, int maximum, float shield, float maximumShield)
    {
        if (shield < stellarShield) shieldFlashUntil = Util.getMillis() + 450;
        if (energy < starEnergy) energyFlashUntil = Util.getMillis() + 250;
        maximumStarEnergy = Math.max(0, maximum);
        starEnergy = Math.max(0, Math.min(maximumStarEnergy, energy));
        maximumStellarShield = Math.max(0.0F, maximumShield);
        stellarShield = Math.max(0.0F, Math.min(maximumStellarShield, shield));
    }

    public static int starEnergy()
    {
        return starEnergy;
    }

    public static int maximumStarEnergy() { return maximumStarEnergy; }
    public static float stellarShield() { return stellarShield; }
    public static float maximumStellarShield() { return maximumStellarShield; }
    public static boolean shieldFlash() { return Util.getMillis() < shieldFlashUntil; }
    public static boolean energyFlash() { return Util.getMillis() < energyFlashUntil; }
}
