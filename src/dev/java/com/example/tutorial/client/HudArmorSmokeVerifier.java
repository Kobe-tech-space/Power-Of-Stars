package com.example.tutorial.client;

import com.example.tutorial.TutorialMod;
import com.example.tutorial.config.ClientHudConfig;
import com.mojang.logging.LogUtils;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Native-game render, drag, save and screenshot check, never included in release builds. */
@Mod.EventBusSubscriber(modid = TutorialMod.MOD_ID, value = Dist.CLIENT)
public final class HudArmorSmokeVerifier
{
    private static int ticks;
    private static HudEditorScreen editor;

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent event)
    {
        Minecraft mc = Minecraft.getInstance();
        if (!Boolean.getBoolean("tutorial_mod.verifyHud") || event.phase != TickEvent.Phase.END || mc.player == null) return;
        mc.options.pauseOnLostFocus = false;
        ++ticks;
        if (ticks == 60) mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT);
        if (ticks == 85) Screenshot.grab(mc.gameDirectory, "armor-updated.png", mc.getMainRenderTarget(), c -> LogUtils.getLogger().info("HUD_ARMOR_SCREENSHOT: {}", c.getString()));
        if (ticks == 95)
        {
            ClientHudConfig.resetAll();
            editor = new HudEditorScreen(null);
            mc.setScreen(editor);
        }
        if (ticks == 115) Screenshot.grab(mc.gameDirectory, "hud-editor.png", mc.getMainRenderTarget(), c -> LogUtils.getLogger().info("HUD_EDITOR_SCREENSHOT: {}", c.getString()));
        if (ticks == 118) editor.keyPressed(org.lwjgl.glfw.GLFW.GLFW_KEY_TAB, 0, 0);
        if (ticks == 122) Screenshot.grab(mc.gameDirectory, "hud-options.png", mc.getMainRenderTarget(), c -> {});
        if (ticks == 124) editor.keyPressed(org.lwjgl.glfw.GLFW.GLFW_KEY_TAB, 0, 0);
        if (ticks == 130)
        {
            var widget = ClientHudConfig.Widget.PANEL;
            var box = CombatHud.bounds(widget, editor.width, editor.height);
            editor.mouseClicked(box.centerX(), box.centerY(), 0);
            editor.mouseDragged(editor.width * 0.65D, editor.height * 0.25D, 0, 10, 10);
            editor.mouseReleased(0, 0, 0);
            if (Math.abs(widget.x.get() - 0.65D) > 0.05D)
                throw new IllegalStateException("HUD drag did not update the selected widget");
            editor.onClose();
            ClientHudConfig.SPEC.save();
            LogUtils.getLogger().info("HUD_EDITOR_SMOKE_PASS: editor rendered, widget dragged and client configuration saved");
        }
        if (ticks == 150) mc.stop();
    }
}
