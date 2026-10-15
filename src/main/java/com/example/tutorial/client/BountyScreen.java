package com.example.tutorial.client;

import com.example.tutorial.bounty.BountyCatalog;
import com.example.tutorial.network.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Separate screen, not a HUD or a Patchouli page; rewards use actual item tooltips. */
public final class BountyScreen extends Screen {
    private static int[] states = new int[10], progress = new int[10];
    private int selected, left, top, panelWidth, panelHeight, listWidth, rowHeight;
    private Button action;
    private boolean pending;
    private final java.util.List<java.util.List<net.minecraft.world.item.ItemStack>> previews =
            java.util.stream.IntStream.range(0, 10).mapToObj(BountyCatalog::rewards).toList();
    public BountyScreen() { super(Component.literal("星辰悬赏")); }
    public static void receive(BountySnapshotPacket p) {
        states = p.states().clone(); progress = p.progress().clone();
        Minecraft mc = Minecraft.getInstance();
        if (p.open() && mc.player != null && (mc.screen == null || mc.screen instanceof BountyScreen)) mc.setScreen(new BountyScreen());
        if (mc.screen instanceof BountyScreen screen) { screen.pending = false; screen.refresh(); }
    }
    @Override protected void init() {
        panelWidth = Math.min(440, width - 16); panelHeight = Math.min(290, height - 16);
        left = (width - panelWidth) / 2; top = (height - panelHeight) / 2;
        listWidth = Math.min(158, panelWidth / 2 - 12); rowHeight = Math.max(12, (panelHeight - 76) / 10);
        action = addRenderableWidget(Button.builder(Component.empty(), b -> {
            if (pending) return;
            pending = true; refresh();
            ModNetwork.sendToServer(new BountyActionPacket(selected, states[selected] == 0 ? 1 : 2));
        }).bounds(left + listWidth + 24, top + panelHeight - 35, panelWidth - listWidth - 40, 20).build());
        addRenderableWidget(Button.builder(Component.literal("×"), b -> onClose()).bounds(left + panelWidth - 24, top + 9, 16, 16).build());
        refresh();
    }
    private void refresh() {
        if (action == null) return;
        int s = states[selected];
        boolean ready = progress[selected] >= BountyCatalog.QUESTS.get(selected).goal();
        action.setMessage(Component.literal(pending ? "正在同步…" : s == 0 ? "接取悬赏" : s == 2 ? "奖励已领取" : ready ? "领取奖励" : "进行中"));
        action.active = !pending && (s == 0 || (s == 1 && ready));
    }
    @Override public void render(GuiGraphics g, int mx, int my, float partial) {
        renderBackground(g);
        g.fill(left, top, left + panelWidth, top + panelHeight, 0xF010111D);
        g.fill(left, top, left + panelWidth, top + 2, 0xFF9663DA);
        g.drawString(font, "星辰悬赏", left + 14, top + 13, 0xE3D7F8, false);
        int claimed = 0; for (int s : states) if (s == 2) claimed++;
        g.drawString(font, "游戏日刷新 · 今日已领 " + claimed + "/10", left + 14, top + 31, 0x9997AA, false);
        g.fill(left + listWidth + 15, top + 54, left + listWidth + 16, top + panelHeight - 13, 0xFF343044);
        for (int i = 0; i < 10; i++) {
            int y = top + 55 + i * rowHeight; var q = BountyCatalog.QUESTS.get(i);
            g.fill(left + 9, y, left + listWidth + 7, y + rowHeight - 1, i == selected ? 0xFF332541 : 0xFF181824);
            g.fill(left + 9, y, left + 11, y + rowHeight - 1, 0xFF000000 | q.color());
            String mark = states[i] == 2 ? "✓" : states[i] == 1 && progress[i] >= q.goal() ? "!" : states[i] == 1 ? "·" : "";
            g.drawString(font, q.title(), left + 17, y + (rowHeight - 8) / 2, i == selected ? 0xEEE4FA : 0xB5B2C5, false);
            g.drawString(font, mark, left + listWidth - 6, y + (rowHeight - 8) / 2, q.color(), false);
        }
        var q = BountyCatalog.QUESTS.get(selected);
        int x = left + listWidth + 27, y = top + 59, detailWidth = panelWidth - listWidth - 42;
        g.drawString(font, q.tier() + " / " + q.title(), x, y, q.color(), false);
        g.drawWordWrap(font, Component.literal(q.objective()), x, y + 22, detailWidth, 0xDBD8E5);
        int py = top + panelHeight - 122;
        g.drawString(font, "进度  " + progress[selected] + " / " + q.goal(), x, py, 0xC1B7D5, false);
        g.fill(x, py + 13, x + detailWidth, py + 16, 0xFF30283E);
        g.fill(x, py + 13, x + detailWidth * Math.min(progress[selected], q.goal()) / q.goal(), py + 16, 0xFF000000 | q.color());
        g.drawString(font, "奖励  ·  " + q.xp() + " 经验", x, py + 25, 0xAAA2BC, false);
        var rewards = previews.get(selected);
        for (int i = 0; i < rewards.size(); i++) {
            int ix = x + i * 35, iy = py + 42;
            g.fill(ix - 3, iy - 3, ix + 23, iy + 23, 0xFF252131);
            g.renderItem(rewards.get(i), ix, iy); g.renderItemDecorations(font, rewards.get(i), ix, iy);
        }
        super.render(g, mx, my, partial);
        for (int i = 0; i < rewards.size(); i++) {
            int ix = x + i * 35, iy = py + 42;
            if (mx >= ix - 3 && mx < ix + 23 && my >= iy - 3 && my < iy + 23) g.renderTooltip(font, rewards.get(i), mx, my);
        }
    }
    @Override public boolean mouseClicked(double x, double y, int button) {
        if (button == 0 && x >= left + 9 && x < left + listWidth + 7 && y >= top + 55) {
            int i = (int)(y - top - 55) / rowHeight;
            if (i >= 0 && i < 10) { selected = i; refresh(); return true; }
        }
        return super.mouseClicked(x, y, button);
    }
    @Override public boolean isPauseScreen() { return false; }
    @Override public boolean keyPressed(int key, int scanCode, int modifiers) {
        if (key == org.lwjgl.glfw.GLFW.GLFW_KEY_UP || key == org.lwjgl.glfw.GLFW.GLFW_KEY_DOWN) {
            selected = Math.floorMod(selected + (key == org.lwjgl.glfw.GLFW.GLFW_KEY_UP ? -1 : 1), 10);
            refresh(); return true;
        }
        return super.keyPressed(key, scanCode, modifiers);
    }
}
