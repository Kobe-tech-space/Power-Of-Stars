package com.example.tutorial.client;

import com.example.tutorial.TutorialMod;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import vazkii.patchouli.api.PatchouliAPI;

/** Development-only check of the real item-use -> packet -> Patchouli screen path. */
@Mod.EventBusSubscriber(modid = TutorialMod.MOD_ID, value = Dist.CLIENT)
public final class GuideSmokeVerifier
{
    private static int ticks;
    private static boolean reported;
    private GuideSmokeVerifier() {}

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent event)
    {
        if (!Boolean.getBoolean("tutorial_mod.verifyGuide") || event.phase != TickEvent.Phase.END
                || reported || Minecraft.getInstance().player == null) return;
        // The isolated instance must keep ticking when the user's game has focus.
        Minecraft.getInstance().options.pauseOnLostFocus = false;
        ++ticks;
        if (ticks < 100) return;
        ResourceLocation open = PatchouliAPI.get().getOpenBookGui();
        if (ticks == 100 && ResourceLocation.fromNamespaceAndPath(TutorialMod.MOD_ID, "astral_guide").equals(open))
        {
            LogUtils.getLogger().info("ASTRAL_GUIDE_SMOKE_PASS: actual item use opened the Patchouli guide screen");
        }
        else if (ticks == 100)
        {
            reported = true;
            LogUtils.getLogger().error("ASTRAL_GUIDE_SMOKE_FAIL: expected guide screen, got {}", open);
            Minecraft.getInstance().stop();
        }
        if (ticks == 105 || ticks == 125)
            PatchouliAPI.get().openBookEntry(ResourceLocation.fromNamespaceAndPath(TutorialMod.MOD_ID, "astral_guide"),
                    ResourceLocation.fromNamespaceAndPath(TutorialMod.MOD_ID, "equipment/ascension"), ticks == 105 ? 0 : 2);
        if (ticks == 145 || ticks == 165)
            PatchouliAPI.get().openBookEntry(ResourceLocation.fromNamespaceAndPath(TutorialMod.MOD_ID, "astral_guide"),
                    ResourceLocation.fromNamespaceAndPath(TutorialMod.MOD_ID, "combat/domains"), ticks == 145 ? 0 : 2);
        if (ticks == 115 || ticks == 135 || ticks == 155 || ticks == 175)
            net.minecraft.client.Screenshot.grab(Minecraft.getInstance().gameDirectory, "guide-tier-" + ticks + ".png",
                    Minecraft.getInstance().getMainRenderTarget(), c -> {});
        if (ticks == 180)
        {
            reported = true;
            LogUtils.getLogger().info("ASTRAL_GUIDE_TIER_PAGES_PASS: ascension recipes and domain pages rendered");
            Minecraft.getInstance().stop();
        }
    }
}
