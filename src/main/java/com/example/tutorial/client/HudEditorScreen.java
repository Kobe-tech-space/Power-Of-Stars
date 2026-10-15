package com.example.tutorial.client;

import com.example.tutorial.config.ClientHudConfig;
import com.example.tutorial.config.ClientHudConfig.Widget;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

/** Drag-and-drop editor; positions are saved as resolution-independent screen fractions. */
public final class HudEditorScreen extends Screen
{
    private final Screen parent;
    private Widget selected = Widget.PANEL;
    private boolean dragging;
    private boolean settingsVisible;
    private double grabX, grabY;

    public HudEditorScreen(Screen parent)
    {
        super(Component.translatable("hud_editor.tutorial_mod.title"));
        this.parent = parent;
    }

    private int canvasHeight() { return height; }

    @Override
    protected void init()
    {
        addRenderableWidget(Button.builder(Component.translatable("hud_editor.tutorial_mod.options"), b -> {
            settingsVisible = !settingsVisible; rebuildWidgets();
        }).bounds(width - 68, 3, 62, 20).build());
        if (!settingsVisible) return;
        int controls = height - 86;
        addRenderableWidget(Button.builder(Component.literal("‹"), b -> select(-1)).bounds(6, controls, 24, 20).build());
        addRenderableWidget(Button.builder(Component.literal("›"), b -> select(1)).bounds(32, controls, 24, 20).build());
        addRenderableWidget(Button.builder(toggleLabel("hud_editor.tutorial_mod.element", selected.enabled.get()), b -> {
            selected.enabled.set(!selected.enabled.get()); rebuildWidgets();
        }).bounds(width - 104, controls, 98, 20).build());
        addRenderableWidget(new SettingSlider(6, height - 62, width / 2 - 9, true));
        addRenderableWidget(new SettingSlider(width / 2 + 3, height - 62, width / 2 - 9, false));
        int buttonWidth = (width - 36) / 5;
        addRenderableWidget(Button.builder(toggleLabel("hud_editor.tutorial_mod.global", ClientHudConfig.ENABLED.get()), b -> {
            ClientHudConfig.ENABLED.set(!ClientHudConfig.ENABLED.get()); rebuildWidgets();
        }).bounds(6, height - 30, buttonWidth, 20).build());
        addRenderableWidget(Button.builder(toggleLabel("hud_editor.tutorial_mod.effects", ClientHudConfig.EFFECTS.get()), b -> {
            ClientHudConfig.EFFECTS.set(!ClientHudConfig.EFFECTS.get()); rebuildWidgets();
        }).bounds(12 + buttonWidth, height - 30, buttonWidth, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("hud_editor.tutorial_mod.theme_" + ClientHudConfig.THEME.get()), b -> {
            ClientHudConfig.THEME.set((ClientHudConfig.THEME.get() + 1) % 3); rebuildWidgets();
        }).bounds(18 + buttonWidth * 2, height - 30, buttonWidth, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("hud_editor.tutorial_mod.reset"), b -> {
            ClientHudConfig.resetAll(); rebuildWidgets();
        }).bounds(24 + buttonWidth * 3, height - 30, buttonWidth, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("hud_editor.tutorial_mod.save"), b -> onClose())
                .bounds(30 + buttonWidth * 4, height - 30, buttonWidth, 20).build());
    }

    private static Component toggleLabel(String key, boolean enabled)
    {
        return Component.translatable(key, Component.translatable(enabled ? "options.on" : "options.off"));
    }

    private void select(int direction)
    {
        Widget[] widgets = ClientHudConfig.editableWidgets();
        selected = widgets[Math.floorMod(selected.ordinal() + direction, widgets.length)];
        rebuildWidgets();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick)
    {
        renderBackground(graphics);
        graphics.fill(0, 0, width, height, 0x30081422);
        graphics.pose().pushPose();
        for (Widget widget : ClientHudConfig.editableWidgets())
        {
            CombatHud.renderWidget(graphics, width, canvasHeight(), widget, true);
            CombatHud.Bounds box = CombatHud.bounds(widget, width, canvasHeight());
            int color = widget == selected ? 0xFF66DDFF : widget.enabled.get() ? 0x556688AA : 0x88777777;
            int left = Math.round(box.left()), top = Math.round(box.top());
            int right = Math.round(box.left() + box.width()), bottom = Math.round(box.top() + box.height());
            graphics.fill(left, top, right, top + 1, color); graphics.fill(left, bottom - 1, right, bottom, color);
            graphics.fill(left, top, left + 1, bottom, color); graphics.fill(right - 1, top, right, bottom, color);
        }
        graphics.pose().popPose();
        // Finish the preview buffers before drawing the foreground controls.
        graphics.flush();
        graphics.pose().pushPose();
        graphics.pose().translate(0, 0, 200);
        graphics.drawCenteredString(font, title, width / 2, 5, 0xAADDFF);
        if (settingsVisible)
        {
            graphics.fill(0, height - 96, width, height, 0xEE081422);
            graphics.drawString(font, Component.translatable("hud_editor.tutorial_mod.widget_" + selected.id), 62, height - 80, 0xFFFFFF);
        }
        else
        {
            graphics.drawCenteredString(font, Component.translatable("hud_editor.tutorial_mod.drag_hint"), width / 2, 28, 0xBBCDDD);
            graphics.drawString(font, Component.translatable("hud_editor.tutorial_mod.widget_" + selected.id), 6, height - 12, 0xFFFFFF);
        }
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.pose().popPose();
        graphics.flush();
    }

    @Override
    public boolean mouseClicked(double x, double y, int button)
    {
        if (super.mouseClicked(x, y, button)) return true;
        if (settingsVisible && y >= height - 96) return false;
        if (button == 0 && y >= 46 && y <= height)
        {
            Widget[] widgets = ClientHudConfig.editableWidgets();
            for (int i = widgets.length - 1; i >= 0; i--)
            {
                CombatHud.Bounds box = CombatHud.bounds(widgets[i], width, canvasHeight());
                if (!box.contains(x, y)) continue;
                selected = widgets[i]; dragging = true;
                grabX = x - box.centerX(); grabY = y - box.centerY();
                rebuildWidgets();
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseDragged(double x, double y, int button, double dx, double dy)
    {
        if (dragging && button == 0)
        {
            selected.x.set(clamp((x - grabX) / width));
            selected.y.set(clamp((y - grabY) / canvasHeight()));
            return true;
        }
        return super.mouseDragged(x, y, button, dx, dy);
    }

    @Override
    public boolean mouseReleased(double x, double y, int button)
    {
        dragging = false;
        return super.mouseReleased(x, y, button);
    }

    @Override
    public boolean mouseScrolled(double x, double y, double delta)
    {
        if (y >= 46 && (!settingsVisible || y < height - 96))
        {
            selected.scale.set(Math.max(0.5D, Math.min(2.0D, selected.scale.get() + delta * 0.05D)));
            rebuildWidgets(); return true;
        }
        return super.mouseScrolled(x, y, delta);
    }

    @Override
    public boolean keyPressed(int key, int scanCode, int modifiers)
    {
        if (key == GLFW.GLFW_KEY_TAB)
        {
            settingsVisible = !settingsVisible; rebuildWidgets(); return true;
        }
        int step = hasShiftDown() ? 10 : 1;
        if (key == GLFW.GLFW_KEY_LEFT || key == GLFW.GLFW_KEY_RIGHT)
        {
            selected.x.set(clamp(selected.x.get() + (key == GLFW.GLFW_KEY_LEFT ? -step : step) / (double)width)); return true;
        }
        if (key == GLFW.GLFW_KEY_UP || key == GLFW.GLFW_KEY_DOWN)
        {
            selected.y.set(clamp(selected.y.get() + (key == GLFW.GLFW_KEY_UP ? -step : step) / (double)canvasHeight())); return true;
        }
        return super.keyPressed(key, scanCode, modifiers);
    }

    private static double clamp(double value) { return Math.max(0, Math.min(1, value)); }

    @Override
    public void onClose()
    {
        ClientHudConfig.SPEC.save();
        minecraft.setScreen(parent);
    }

    private final class SettingSlider extends AbstractSliderButton
    {
        private final boolean size;
        SettingSlider(int x, int y, int width, boolean size)
        {
            super(x, y, width, 20, Component.empty(), size
                    ? (selected.scale.get() - 0.5D) / 1.5D : (selected.opacity.get() - 0.2D) / 0.8D);
            this.size = size; updateMessage();
        }
        @Override protected void updateMessage()
        {
            setMessage(Component.translatable(size ? "hud_editor.tutorial_mod.scale" : "hud_editor.tutorial_mod.opacity",
                    Math.round((size ? 0.5D + value * 1.5D : 0.2D + value * 0.8D) * 100)));
        }
        @Override protected void applyValue()
        {
            if (size) selected.scale.set(0.5D + value * 1.5D);
            else selected.opacity.set(0.2D + value * 0.8D);
        }
    }
}
