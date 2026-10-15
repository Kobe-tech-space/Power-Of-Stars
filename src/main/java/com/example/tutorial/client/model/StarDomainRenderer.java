package com.example.tutorial.client.model;
import com.example.tutorial.TutorialMod;
import com.example.tutorial.attribute.ModAttributes;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** World-space halo, visible in first and third person. */
@Mod.EventBusSubscriber(modid = TutorialMod.MOD_ID, value = Dist.CLIENT)
public final class StarDomainRenderer
{
    private static final StarHaloModel<Entity> DISC = new StarHaloModel<>(null);
    private static final RenderType TYPE = RenderType.entityTranslucentEmissive(
            ResourceLocation.fromNamespaceAndPath(TutorialMod.MOD_ID, "textures/entity/star_halo.png"));
    @SubscribeEvent public static void render(RenderLevelStageEvent event)
    {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) return;
        var minecraft = Minecraft.getInstance();
        if (minecraft.level == null) return;
        var camera = event.getCamera().getPosition();
        var buffers = minecraft.renderBuffers().bufferSource();
        boolean drew = false;
        for (var player : minecraft.level.players())
        {
            var attribute = player.getAttribute(ModAttributes.DOMAIN.get());
            int kind = attribute == null ? 0 : (int)attribute.getValue();
            if (kind == 0 || player.isInvisible() || player.distanceToSqr(camera) > 96 * 96) continue;
            float partial = event.getPartialTick();
            var position = player.getPosition(partial).subtract(camera);
            var pose = event.getPoseStack();
            pose.pushPose();
            pose.translate(position.x, position.y + .035, position.z);
            pose.mulPose(Axis.XP.rotationDegrees(90));
            float time = player.tickCount + partial;
            pose.mulPose(Axis.ZP.rotationDegrees(time * .55F));
            float radius = 3.5F + (float)Math.sin(time * .08F) * .08F;
            if(com.example.tutorial.config.ModSettings.enabled("environment.endHalo") && player.level().dimension()==net.minecraft.world.level.Level.END) radius*=1.25F;
            pose.scale(radius, radius, radius);
            DISC.renderToBuffer(pose, buffers.getBuffer(TYPE), LightTexture.FULL_BRIGHT,
                    OverlayTexture.NO_OVERLAY, kind == 1 ? 1 : .55F, kind == 1 ? .68F : .8F, 1, .85F);
            pose.popPose();
            drew = true;
        }
        if (drew) buffers.endBatch(TYPE);
    }
}
