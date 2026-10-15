package com.example.tutorial;

import com.example.tutorial.enchantment.ModEnchantments;
import com.example.tutorial.block.ModBlocks;
import com.example.tutorial.attribute.ModAttributes;
import com.example.tutorial.entity.ModEntities;
import com.example.tutorial.item.ModItems;
import com.example.tutorial.network.ModNetwork;
import com.example.tutorial.client.model.AstralSlashRenderer;
import com.example.tutorial.client.model.CosmicArmorGlowLayer;
import com.example.tutorial.client.model.CosmicArmorModel;
import com.example.tutorial.client.model.StarHaloLayer;
import com.example.tutorial.client.model.StarHaloModel;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(TutorialMod.MOD_ID)
public class TutorialMod
{
    // Define mod id in a common place for everything to reference
    public static final String MOD_ID = "tutorial_mod";
    // Directly reference a slf4j logger
    private static final Logger LOGGER = LogUtils.getLogger();
    // 构造函数注入的加载上下文。
    // ModLoadingContext.get() 在 Forge 1.20.1 已标记 @Deprecated(forRemoval)，
    // 官方建议改用注入的 context —— 客户端扩展点注册需要它，所以在这里存下来给 ClientModEvents 用。
    private static FMLJavaModLoadingContext loadingContext;

    public TutorialMod(FMLJavaModLoadingContext context)
    {
        loadingContext = context;
        IEventBus modEventBus = context.getModEventBus();

        // Register the commonSetup method for modloading
        modEventBus.addListener(this::commonSetup);

        // Register ourselves for server and other game events we are interested in
        MinecraftForge.EVENT_BUS.register(this);

        // Register the item to a creative tab
        com.example.tutorial.item.ModCreativeTabs.TABS.register(modEventBus);
        modEventBus.addListener(com.example.tutorial.item.ModCreativeTabs::removeFromVanillaTabs);

        ModBlocks.register(modEventBus);
        ModAttributes.register(modEventBus);
        com.example.tutorial.effect.ModEffects.EFFECTS.register(modEventBus);

        // Register the DeferredRegister of items to the mod event bus
        com.example.tutorial.ornament.OrnamentItems.register();
        ModItems.register(modEventBus);
        ModEntities.register(modEventBus);
        com.example.tutorial.event.AstralStatistics.STATS.register(modEventBus);
        com.example.tutorial.recipe.AstralRecyclingRecipe.SERIALIZERS.register(modEventBus);
        com.example.tutorial.worldgen.ConfigurableStarOre.FEATURES.register(modEventBus);
        com.example.tutorial.worldgen.ConfigurableStarOre.PLACEMENTS.register(modEventBus);
        com.example.tutorial.particle.ModParticles.PARTICLES.register(modEventBus);

        // Register the DeferredRegister of enchantments to the mod event bus
        ModEnchantments.register(modEventBus);

        // Register combat feedback packets used by the client HUD.
        ModNetwork.register();

        // Register our mod's ForgeConfigSpec so that Forge can create and load the config file for us
        context.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
        context.registerConfig(ModConfig.Type.SERVER, com.example.tutorial.config.ModSettings.SPEC, "tutorial_mod-gameplay.toml");
        context.registerConfig(ModConfig.Type.CLIENT,
                com.example.tutorial.config.ClientHudConfig.SPEC, "tutorial_mod-hud.toml");
    }

    private void commonSetup(final FMLCommonSetupEvent event)
    {
        // Some common setup code
        LOGGER.info("HELLO FROM COMMON SETUP");

        event.enqueueWork(() -> {
            if (net.minecraftforge.fml.ModList.get().isLoaded("curios")) com.example.tutorial.compat.CuriosOrnaments.initialize();
            com.example.tutorial.event.AstralStatistics.initialize();
            ((RangedAttribute) Attributes.ARMOR).maxValue = Math.max(500.0D, ((RangedAttribute) Attributes.ARMOR).maxValue);
            ((RangedAttribute) Attributes.ARMOR_TOUGHNESS).maxValue = Math.max(500.0D, ((RangedAttribute) Attributes.ARMOR_TOUGHNESS).maxValue);
        });
    }

    // You can use SubscribeEvent and let the Event Bus discover methods to call
    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event)
    {
        // Do something when the server starts
        LOGGER.info("HELLO from server starting");
    }

    // You can use EventBusSubscriber to automatically register all static methods in the class annotated with @SubscribeEvent
    @Mod.EventBusSubscriber(modid = MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents
    {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event)
        {
            loadingContext.registerExtensionPoint(
                    net.minecraftforge.client.ConfigScreenHandler.ConfigScreenFactory.class,
                    () -> new net.minecraftforge.client.ConfigScreenHandler.ConfigScreenFactory(
                            (minecraft, parent) -> new com.example.tutorial.client.HudEditorScreen(parent)));
            // Some client setup code
            LOGGER.info("HELLO FROM CLIENT SETUP");
            LOGGER.info("MINECRAFT NAME >> {}", Minecraft.getInstance().getUser().getName());
        }

        @SubscribeEvent
        public static void registerHud(net.minecraftforge.client.event.RegisterGuiOverlaysEvent event)
        {
            for (com.example.tutorial.config.ClientHudConfig.Widget widget
                    : com.example.tutorial.config.ClientHudConfig.Widget.values())
                event.registerAboveAll("astral_" + widget.id,
                        (gui, graphics, partialTick, width, height) ->
                                com.example.tutorial.client.CombatHud.renderWidget(graphics, width, height, widget, false));
            event.registerAboveAll("astral_screen_effects",
                    (gui, graphics, partialTick, width, height) ->
                            com.example.tutorial.client.CombatHud.renderEffects(graphics, width, height));
        }

        @SubscribeEvent
        public static void registerLayers(EntityRenderersEvent.RegisterLayerDefinitions event)
        {
            event.registerLayerDefinition(StarHaloModel.LAYER, StarHaloModel::createBodyLayer);
            event.registerLayerDefinition(CosmicArmorModel.LAYER, CosmicArmorModel::createBodyLayer);
        }

        @SubscribeEvent
        public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event)
        {
            event.registerEntityRenderer(ModEntities.ASTRAL_SLASH.get(), AstralSlashRenderer::new);
        }

        @SubscribeEvent
        public static void addPlayerLayers(EntityRenderersEvent.AddLayers event)
        {
            for (String skin : event.getSkins())
            {
                net.minecraft.client.renderer.entity.player.PlayerRenderer renderer = event.getSkin(skin);
                if (renderer != null)
                {
                    renderer.addLayer(new CosmicArmorGlowLayer(renderer,
                            new CosmicArmorModel<>(event.getEntityModels().bakeLayer(CosmicArmorModel.LAYER))));
                    renderer.addLayer(new StarHaloLayer(renderer,
                            new StarHaloModel<>(event.getEntityModels().bakeLayer(StarHaloModel.LAYER))));
                }
            }
        }
    }
}
