package com.example.tutorial.client;

import com.example.tutorial.TutorialMod;
import com.example.tutorial.network.ActivateStarSkillPacket;
import com.example.tutorial.network.ModNetwork;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

public final class StarSkillInput
{
    private static final String CATEGORY = "key.categories.tutorial_mod";
    public static final KeyMapping GREAT_SWORD_SKILL = new KeyMapping(
            "key.tutorial_mod.great_sword_skill", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_V, CATEGORY);
    public static final KeyMapping ARMOR_SKILL = new KeyMapping(
            "key.tutorial_mod.armor_skill", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_G, CATEGORY);
    public static final KeyMapping PARRY = new KeyMapping(
            "key.tutorial_mod.parry", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_B, CATEGORY);

    private StarSkillInput() {}
    public static final KeyMapping SCAN = new KeyMapping("key.tutorial_mod.scan", GLFW.GLFW_KEY_N, CATEGORY);
    public static final KeyMapping DASH = new KeyMapping("key.tutorial_mod.dash", GLFW.GLFW_KEY_R, CATEGORY);
    public static final KeyMapping BLINK = new KeyMapping("key.tutorial_mod.blink", GLFW.GLFW_KEY_H, CATEGORY);
    public static final KeyMapping HUD_EDITOR = new KeyMapping("key.tutorial_mod.hud_editor", GLFW.GLFW_KEY_K, CATEGORY);
    public static final KeyMapping DOMAIN = new KeyMapping("key.tutorial_mod.domain", GLFW.GLFW_KEY_J, CATEGORY);
    public static final KeyMapping BOUNTIES = new KeyMapping("key.tutorial_mod.bounties", GLFW.GLFW_KEY_L, CATEGORY);
    public static final KeyMapping TALENTS = new KeyMapping("key.tutorial_mod.talents", GLFW.GLFW_KEY_O, CATEGORY);

    @Mod.EventBusSubscriber(modid = TutorialMod.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static final class Registration
    {
        @SubscribeEvent
        public static void registerKeys(RegisterKeyMappingsEvent event)
        {
            event.register(GREAT_SWORD_SKILL);
            event.register(ARMOR_SKILL);
            event.register(PARRY);
            event.register(SCAN); event.register(DASH); event.register(BLINK);
            event.register(HUD_EDITOR);
            event.register(DOMAIN);
            event.register(BOUNTIES);
            event.register(TALENTS);
        }
    }

    @Mod.EventBusSubscriber(modid = TutorialMod.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static final class Input
    {
        @SubscribeEvent
        public static void onKey(InputEvent.Key event)
        {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.player != null && minecraft.screen == null && HUD_EDITOR.consumeClick())
            {
                minecraft.setScreen(new HudEditorScreen(null));
                return;
            }
            if (minecraft.player == null || minecraft.screen != null) return;
            while (BOUNTIES.consumeClick()) ModNetwork.sendToServer(new com.example.tutorial.network.BountyActionPacket(-1, 0));
            while (TALENTS.consumeClick()) ModNetwork.sendToServer(new com.example.tutorial.network.TalentActionPacket(0, -1));
            while (GREAT_SWORD_SKILL.consumeClick())
                ModNetwork.sendToServer(new ActivateStarSkillPacket(ActivateStarSkillPacket.Skill.GREAT_SWORD));
            while (DOMAIN.consumeClick()) ModNetwork.sendToServer(new ActivateStarSkillPacket(ActivateStarSkillPacket.Skill.DOMAIN));
            while (ARMOR_SKILL.consumeClick())
                ModNetwork.sendToServer(new ActivateStarSkillPacket(ActivateStarSkillPacket.Skill.ARMOR_SET));
            while (PARRY.consumeClick())
                ModNetwork.sendToServer(new ActivateStarSkillPacket(ActivateStarSkillPacket.Skill.PARRY));
            while (SCAN.consumeClick()) ModNetwork.sendToServer(new ActivateStarSkillPacket(ActivateStarSkillPacket.Skill.SCAN));
            while (DASH.consumeClick()) ModNetwork.sendToServer(new ActivateStarSkillPacket(ActivateStarSkillPacket.Skill.DASH));
            while (BLINK.consumeClick()) ModNetwork.sendToServer(new ActivateStarSkillPacket(ActivateStarSkillPacket.Skill.BLINK));
        }
    }
}
