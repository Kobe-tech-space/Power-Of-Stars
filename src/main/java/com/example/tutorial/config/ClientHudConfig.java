package com.example.tutorial.config;

import net.minecraftforge.common.ForgeConfigSpec;

/** Pure configuration: safe to register on both distributions, loaded only on clients. */
public final class ClientHudConfig
{
    public enum Widget
    {
        ENERGY("energy", 0.23D, 0.64D, 204, 38),
        SHIELD("shield", 0.23D, 0.79D, 174, 28),
        RESONANCE("resonance", 0.23D, 0.52D, 110, 16),
        MODE("mode", 0.23D, 0.89D, 150, 16),
        COMBO("combo", 0.50D, 0.73D, 230, 42),
        MASTERY("mastery", 0.77D, 0.66D, 260, 16),
        HINTS("hints", 0.77D, 0.83D, 280, 18),
        CRITICAL("critical", 0.50D, 0.40D, 240, 52),
        BARRIER("barrier", 0.77D, 0.53D, 200, 28),
        DOMAIN("domain", 0.50D, 0.20D, 260, 32),
        PANEL("panel", 0.145D, 0.76D, 148, 62);

        public final String id;
        public final double defaultX, defaultY;
        public final int width, height;
        public ForgeConfigSpec.DoubleValue x, y, scale, opacity;
        public ForgeConfigSpec.BooleanValue enabled;

        Widget(String id, double x, double y, int width, int height)
        {
            this.id = id; this.defaultX = x; this.defaultY = y;
            this.width = width; this.height = height;
        }

        public void reset()
        {
            x.set(defaultX); y.set(defaultY); scale.set(0.8D); opacity.set(1.0D); enabled.set(this == PANEL);
        }
    }

    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.BooleanValue ENABLED, EFFECTS;
    public static final ForgeConfigSpec.IntValue THEME;
    public static final ForgeConfigSpec.IntValue LAYOUT_VERSION;

    static
    {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        LAYOUT_VERSION = builder.defineInRange("layoutVersion", 0, 0, 100);
        ENABLED = builder.define("enabled", true);
        EFFECTS = builder.define("screenEffects", false);
        THEME = builder.comment("0: cyan, 1: violet, 2: gold").defineInRange("theme", 0, 0, 2);
        for (Widget widget : Widget.values())
        {
            builder.push(widget.id);
            widget.enabled = builder.define("enabled", widget == Widget.PANEL);
            widget.x = builder.comment("Horizontal center, as a fraction of screen width").defineInRange("x", widget.defaultX, 0.0D, 1.0D);
            widget.y = builder.comment("Vertical center, as a fraction of screen height").defineInRange("y", widget.defaultY, 0.0D, 1.0D);
            widget.scale = builder.defineInRange("scale", 0.8D, 0.5D, 2.0D);
            widget.opacity = builder.defineInRange("opacity", 1.0D, 0.2D, 1.0D);
            builder.pop();
        }
        SPEC = builder.build();
    }

    private ClientHudConfig() {}

    public static int accent()
    {
        return switch (THEME.get()) { case 1 -> 0xC28CFF; case 2 -> 0xFFD16A; default -> 0x47D9FF; };
    }

    public static void resetAll()
    {
        for (Widget widget : Widget.values()) widget.reset();
        ENABLED.set(true); EFFECTS.set(false); THEME.set(0);
        LAYOUT_VERSION.set(3);
    }
    public static Widget[] editableWidgets() { return new Widget[]{Widget.PANEL, Widget.COMBO}; }
}
