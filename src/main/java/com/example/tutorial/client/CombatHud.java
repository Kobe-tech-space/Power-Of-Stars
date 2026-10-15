package com.example.tutorial.client;

import com.example.tutorial.Config;
import com.example.tutorial.config.ClientHudConfig;
import com.example.tutorial.config.ClientHudConfig.Widget;
import com.example.tutorial.enchantment.ModEnchantments;
import com.example.tutorial.event.AstralResonance;
import com.example.tutorial.event.WeaponMastery;
import com.example.tutorial.item.AstralGreatswordItem;
import com.example.tutorial.item.ModItems;
import com.example.tutorial.item.StarArmorItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.Locale;

/** Independently registered overlays, shared with the live HUD editor preview. */
public final class CombatHud
{
    public record Bounds(float left, float top, float width, float height)
    {
        public float centerX() { return left + width / 2; }
        public float centerY() { return top + height / 2; }
        public boolean contains(double x, double y)
        {
            return x >= left && x <= left + width && y >= top && y <= top + height;
        }
    }

    private CombatHud() {}

    public static Bounds bounds(Widget widget, int width, int height)
    {
        float w = widget.width * widget.scale.get().floatValue();
        float h = widget.height * widget.scale.get().floatValue();
        float x = clampCenter((float)(widget.x.get() * width), w, width);
        float y = clampCenter((float)(widget.y.get() * height), h, height);
        if (widget == Widget.PANEL && Math.abs(widget.x.get() - widget.defaultX) < .00001
                && Math.abs(widget.y.get() - widget.defaultY) < .00001)
        {
            x = clampCenter(6 + w / 2, w, width);
            y = clampCenter(height - 58 - h / 2, h, height);
        }
        return new Bounds(x - w / 2, y - h / 2, w, h);
    }

    private static float clampCenter(float center, float size, int extent)
    {
        if (size >= extent) return extent / 2.0F;
        return Math.max(size / 2, Math.min(extent - size / 2, center));
    }

    public static void renderWidget(GuiGraphics graphics, int width, int height, Widget widget, boolean preview)
    {
        Minecraft minecraft = Minecraft.getInstance();
        if (widget != Widget.PANEL && widget != Widget.COMBO) return;
        if (!preview && (minecraft.options.hideGui || minecraft.player == null
                || minecraft.screen instanceof HudEditorScreen || !ClientHudConfig.ENABLED.get()
                || !widget.enabled.get())) return;
        ItemStack held = minecraft.player == null ? ItemStack.EMPTY : minecraft.player.getMainHandItem();
        boolean sword = held.is(ModItems.ICE_SWORD.get());
        boolean fullArmor = minecraft.player != null && StarArmorItem.hasFullSet(minecraft.player);
        int resonance = minecraft.player == null ? 0 : AstralResonance.level(minecraft.player);
        boolean equipped = sword || resonance > 0;
        int combo = ClientCombatState.combo();
        if (!preview)
        {
            boolean visible = switch (widget)
            {
                case PANEL -> equipped || ClientCombatState.guardianBarrier() > 0;
                case ENERGY, RESONANCE, HINTS -> equipped;
                case SHIELD -> fullArmor;
                case BARRIER -> ClientCombatState.guardianBarrier() > 0;
                case DOMAIN -> sword && StarArmorItem.fullSetTier(minecraft.player) == 2 || ClientCombatState.domain() > 0;
                case MODE, MASTERY -> sword;
                case COMBO -> ModEnchantments.hasCombo(held) && combo > 0;
                case CRITICAL -> ClientCombatState.showCritical();
            };
            if (!visible) return;
        }
        Bounds bounds = bounds(widget, width, height);
        float opacity = widget.opacity.get().floatValue() * (preview && !widget.enabled.get() ? 0.3F : 1.0F);
        graphics.pose().pushPose();
        graphics.pose().translate(bounds.centerX(), bounds.centerY(), 0);
        float scale = widget.scale.get().floatValue();
        graphics.pose().scale(scale, scale, 1);
        int accent = ClientHudConfig.accent();
        switch (widget)
        {
            case PANEL -> compactPanel(graphics, held, sword, preview, accent, opacity);
            case ENERGY -> {
                int energy = preview ? 78 : ClientCombatState.starEnergy();
                int max = preview ? 250 : ClientCombatState.maximumStarEnergy();
                text(graphics, Component.translatable("hud.tutorial_mod.star_energy", energy, max), -16, accent, opacity);
                bar(graphics, 148, -3, energy / (float)Math.max(1, max), energy >= max ? 0xF588FF : accent, opacity);
                Component skill = fullArmor && energy >= 150
                        ? Component.translatable("hud.tutorial_mod.armor_skill_ready")
                        : (sword || preview) && energy >= Config.swordSkillCost
                        ? Component.translatable("hud.tutorial_mod.sword_skill_ready")
                        : Component.translatable("hud.tutorial_mod.star_energy_hint");
                text(graphics, skill, 9, 0xC7E6FF, opacity);
            }
            case SHIELD -> {
                float shield = preview ? 35 : ClientCombatState.stellarShield();
                float max = preview ? 50 : ClientCombatState.maximumStellarShield();
                text(graphics, Component.translatable("hud.tutorial_mod.stellar_shield", Math.round(shield), Math.round(max)), -12, 0xDFB9FF, opacity);
                bar(graphics, 148, 2, shield / Math.max(1, max), shield <= max * 0.25F ? 0xFF7777 : 0xC978FF, opacity);
            }
            case BARRIER -> {
                float shield = preview ? 850 : ClientCombatState.guardianBarrier();
                text(graphics, Component.translatable("hud.tutorial_mod.guardian_barrier", Math.round(shield)), -12, 0xAABFFF, opacity);
                bar(graphics, 148, 2, shield / 1000, 0x6688FF, opacity);
            }
            case DOMAIN -> {
                boolean ranged = AstralGreatswordItem.isRanged(held);
                int active = preview ? 1 : ClientCombatState.domain();
                int remaining = active > 0 ? preview ? 10 : ClientCombatState.domainSeconds() : ClientCombatState.domainCooldown(ranged);
                Component name = Component.translatable((active > 0 ? active == 2 : ranged)
                        ? "domain.tutorial_mod.control" : "domain.tutorial_mod.break");
                Component label = Component.translatable(active > 0 ? "hud.tutorial_mod.domain_active"
                        : remaining > 0 ? "hud.tutorial_mod.domain_cooldown"
                        : !preview && ClientCombatState.starEnergy() <= 0 && !minecraft.player.getAbilities().instabuild
                        ? "hud.tutorial_mod.domain_no_energy" : "hud.tutorial_mod.domain_ready",
                        name, remaining, StarSkillInput.DOMAIN.getTranslatedKeyMessage());
                text(graphics, label, -4, active > 0 ? 0xFFD580 : accent, opacity);
            }
            case RESONANCE -> text(graphics, Component.translatable("hud.tutorial_mod.resonance", preview ? 5 : resonance), -4, accent, opacity);
            case MODE -> text(graphics, Component.translatable(AstralGreatswordItem.isRanged(held)
                    ? "hud.tutorial_mod.mode_ranged" : "hud.tutorial_mod.mode_melee"), -4, accent, opacity);
            case MASTERY -> text(graphics, Component.translatable("tooltip.tutorial_mod.mastery",
                    preview ? 8 : WeaponMastery.level(held), 20, preview ? 36 : WeaponMastery.progress(held)), -4, 0xFFD080, opacity);
            case HINTS -> text(graphics, Component.translatable("hud.tutorial_mod.exploration_keys",
                    StarSkillInput.SCAN.getTranslatedKeyMessage(), StarSkillInput.DASH.getTranslatedKeyMessage(),
                    StarSkillInput.BLINK.getTranslatedKeyMessage()), -4, 0xD9C9FF, opacity);
            case COMBO -> {
                int value = preview ? 42 : combo;
                text(graphics, Component.translatable("hud.tutorial_mod.combo", value), -7, accent, opacity);
                bar(graphics, 120, 6, preview ? 0.75F : ClientCombatState.remainingRatio(), accent, opacity);
                if (ClientCombatState.milestone() > 0)
                    text(graphics, Component.translatable("hud.tutorial_mod.milestone", ClientCombatState.milestone()), -21, 0xFFCC55, opacity);
            }
            case CRITICAL -> {
                float damage = preview ? 156.7F : ClientCombatState.criticalDamage();
                String number = Float.isFinite(damage) ? String.format(Locale.ROOT, "%.1f", damage) : "∞";
                float progress = preview ? 0 : ClientCombatState.criticalAnimationProgress();
                graphics.pose().translate(0, -progress * 12, 0);
                graphics.pose().scale(1.4F - progress * 0.15F, 1.4F - progress * 0.15F, 1);
                text(graphics, Component.translatable("hud.tutorial_mod.critical_damage", number), -4, 0xFF6666, opacity);
            }
        }
        graphics.pose().popPose();
    }

    public static void renderEffects(GuiGraphics graphics, int width, int height)
    {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.options.hideGui || minecraft.screen instanceof HudEditorScreen
                || !ClientHudConfig.ENABLED.get() || !ClientHudConfig.EFFECTS.get() || !Config.screenEffects) return;
        int color = ClientCombatState.shieldFlash() ? 0x559755FF : ClientCombatState.energyFlash() ? 0x3345DDFF : 0;
        if (color != 0)
        {
            graphics.fill(0, 0, width, 5, color); graphics.fill(0, height - 5, width, height, color);
            graphics.fill(0, 0, 5, height, color); graphics.fill(width - 5, 0, width, height, color);
        }
        if (minecraft.player.getHealth() <= minecraft.player.getMaxHealth() * 0.25F)
            graphics.fill(0, height - 3, width, height, 0x99FF4444);
    }

    private static void text(GuiGraphics graphics, Component text, int y, int color, float opacity)
    {
        graphics.drawCenteredString(Minecraft.getInstance().font, text, 0, y, rgba(color, opacity));
    }
    private static void compactPanel(GuiGraphics graphics, ItemStack held, boolean sword, boolean preview, int accent, float opacity)
    {
        graphics.fill(-74, -31, 74, 31, rgba(0x09121D, opacity * .80F));
        graphics.fill(-74, -31, -72, 31, rgba(accent, opacity));
        var font = Minecraft.getInstance().font;
        var title = Component.translatable(!sword && !preview ? "hud.tutorial_mod.panel_title"
                : AstralGreatswordItem.isRanged(held) ? "hud.tutorial_mod.compact_mode_ranged" : "hud.tutorial_mod.compact_mode_melee");
        graphics.drawString(font, title, -65, -26, rgba(accent, opacity), false);
        if (sword || preview) {
            var level = Component.literal("Lv." + (preview ? 8 : WeaponMastery.level(held)));
            graphics.drawString(font, level, 65-font.width(level), -26, rgba(0x93A9BB, opacity), false);
        }
        resourceRow(graphics, "hud.tutorial_mod.compact_energy", preview ? 1000 : ClientCombatState.starEnergy(),
                preview ? 1000 : ClientCombatState.maximumStarEnergy(), -13, accent, opacity);
        resourceRow(graphics, "hud.tutorial_mod.compact_shield", preview ? 50 : ClientCombatState.stellarShield(), preview ? 50 : (int)ClientCombatState.maximumStellarShield(), 1, accent, opacity);
        resourceRow(graphics, "hud.tutorial_mod.compact_barrier", preview ? 850 : ClientCombatState.guardianBarrier(), 1000, 15, accent, opacity);
    }
    private static void resourceRow(GuiGraphics graphics, String key, float current, int maximum, int y, int accent, float opacity)
    {
        var font = Minecraft.getInstance().font;
        int color = current > 0 ? accent : 0x839AAE;
        graphics.drawString(font, Component.translatable(key), -65, y, rgba(color, opacity), false);
        String amount = Math.round(current) + "/" + maximum;
        graphics.drawString(font, amount, 65-font.width(amount), y, rgba(0xB4C7D7, opacity), false);
        graphics.fill(-65, y+10, 65, y+12, rgba(0x233344, opacity));
        int filled = Math.round(130 * Math.max(0, Math.min(1, current / Math.max(1, maximum))));
        graphics.fill(-65, y+10, -65+filled, y+12, rgba(accent, opacity));
    }

    private static void bar(GuiGraphics graphics, int width, int y, float ratio, int color, float opacity)
    {
        int left = -width / 2;
        graphics.fill(left, y, left + width, y + 8, rgba(0x071421, opacity * 0.65F));
        int filled = Math.round((width - 2) * Math.max(0, Math.min(1, ratio)));
        graphics.fill(left + 1, y + 1, left + 1 + filled, y + 7, rgba(color, opacity));
    }

    private static int rgba(int color, float opacity)
    {
        return Math.max(1, Math.round(opacity * 255)) << 24 | color & 0xFFFFFF;
    }
}
