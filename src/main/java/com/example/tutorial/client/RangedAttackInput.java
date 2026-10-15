package com.example.tutorial.client;

import com.example.tutorial.TutorialMod;
import com.example.tutorial.item.AstralGreatswordItem;
import com.example.tutorial.item.ModItems;
import com.example.tutorial.network.ModNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import org.lwjgl.glfw.GLFW;
import com.mojang.blaze3d.platform.InputConstants;

@Mod.EventBusSubscriber(modid = TutorialMod.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class RangedAttackInput
{
    private static final AttackInputGate GATE = new AttackInputGate();
    private static final HeldAttackState HELD = new HeldAttackState();
    private static InputConstants.Key heldBinding;
    private static int heldKeyboardKey = GLFW.GLFW_KEY_UNKNOWN;
    private RangedAttackInput()
    {
    }

    /** Runs before vanilla/Better Combat consume mouse clicks; only takes over ranged mode. */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onMouse(InputEvent.MouseButton.Pre event)
    {
        Minecraft minecraft = Minecraft.getInstance();
        if (!ready(minecraft) || !minecraft.options.keyAttack.matchesMouse(event.getButton())) return;
        // Suppress both press and release so a held attack cannot start a parallel melee combo.
        event.setCanceled(true);
        clearVanillaAttack(minecraft);
        HELD.update(event.getAction() == GLFW.GLFW_PRESS);
        heldBinding = minecraft.options.keyAttack.getKey();
        if (event.getAction() == GLFW.GLFW_PRESS) request(minecraft);
    }

    /** Keyboard bindings must use the user's attack key, not a hardcoded mouse button. */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onKey(InputEvent.Key event)
    {
        Minecraft minecraft = Minecraft.getInstance();
        if (!ready(minecraft) || !minecraft.options.keyAttack.matches(event.getKey(), event.getScanCode())) return;
        clearVanillaAttack(minecraft);
        if (event.getAction() == GLFW.GLFW_REPEAT) return;
        HELD.update(event.getAction() == GLFW.GLFW_PRESS);
        heldBinding = minecraft.options.keyAttack.getKey();
        heldKeyboardKey = event.getKey();
        if (event.getAction() == GLFW.GLFW_PRESS) request(minecraft);
    }

    /** Fallback for controllers/mods that inject KeyMapping clicks instead of raw GLFW events. */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onTick(TickEvent.ClientTickEvent event)
    {
        if (event.phase != TickEvent.Phase.START) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (!ready(minecraft) || !minecraft.isWindowActive()) {
            resetHeld();
            return;
        }
        if (HELD.isHeld() && !physicallyHeld(minecraft)) resetHeld();
        boolean clicked = false;
        while (minecraft.options.keyAttack.consumeClick()) clicked = true;
        minecraft.options.keyAttack.setDown(false);
        if (clicked || HELD.isHeld()) request(minecraft);
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        GATE.reset();
        resetHeld();
    }

    private static void resetHeld() {
        HELD.update(false);
        heldBinding = null;
        heldKeyboardKey = GLFW.GLFW_KEY_UNKNOWN;
    }

    /** Poll physical input as well, since GUI/focus changes can swallow release events. */
    private static boolean physicallyHeld(Minecraft minecraft) {
        if (heldBinding == null || !heldBinding.equals(minecraft.options.keyAttack.getKey())) return false;
        long window = minecraft.getWindow().getWindow();
        if (heldBinding.getType() == InputConstants.Type.MOUSE)
            return GLFW.glfwGetMouseButton(window, heldBinding.getValue()) == GLFW.GLFW_PRESS;
        int key = heldBinding.getType() == InputConstants.Type.KEYSYM ? heldBinding.getValue() : heldKeyboardKey;
        return key != GLFW.GLFW_KEY_UNKNOWN && GLFW.glfwGetKey(window, key) == GLFW.GLFW_PRESS;
    }

    private static boolean ready(Minecraft minecraft)
    {
        if (minecraft.player == null || minecraft.level == null || minecraft.screen != null
                || minecraft.isPaused() || !minecraft.isWindowActive() || !minecraft.mouseHandler.isMouseGrabbed()
                || !minecraft.player.isAlive() || minecraft.player.isSpectator()) return false;
        ItemStack weapon = minecraft.player.getMainHandItem();
        return weapon.is(ModItems.ICE_SWORD.get()) && AstralGreatswordItem.isRanged(weapon);
    }

    private static void clearVanillaAttack(Minecraft minecraft)
    {
        while (minecraft.options.keyAttack.consumeClick()) { /* Drain only while using ranged sword. */ }
        minecraft.options.keyAttack.setDown(false);
    }

    private static void request(Minecraft minecraft)
    {
        if (!GATE.accept(minecraft.level, minecraft.level.getGameTime())) return;
        minecraft.player.swing(InteractionHand.MAIN_HAND);
        ModNetwork.sendToServer(new com.example.tutorial.network.FireAstralSlashPacket());
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST, receiveCanceled = true)
    public static void onInteractionKey(InputEvent.InteractionKeyMappingTriggered event)
    {
        if (!event.isAttack())
        {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (!ready(minecraft))
        {
            return;
        }

        event.setCanceled(true);
        event.setSwingHand(false);
        clearVanillaAttack(minecraft);
        request(minecraft);
    }
}
