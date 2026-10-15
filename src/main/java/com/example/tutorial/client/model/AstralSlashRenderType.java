package com.example.tutorial.client.model;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.RenderType;

/** Additive energy with no back-face culling and no transparent depth occlusion. */
public final class AstralSlashRenderType extends RenderType
{
    public static final RenderType ENERGY = create("tutorial_mod_astral_energy",
            DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.QUADS, 2048, false, true,
            CompositeState.builder().setShaderState(RENDERTYPE_LIGHTNING_SHADER)
                    .setTransparencyState(LIGHTNING_TRANSPARENCY).setCullState(NO_CULL)
                    .setWriteMaskState(COLOR_WRITE).setOutputState(WEATHER_TARGET)
                    .createCompositeState(false));

    private AstralSlashRenderType()
    {
        super("tutorial_mod_astral_energy", DefaultVertexFormat.POSITION_COLOR,
                VertexFormat.Mode.QUADS, 2048, false, true, () -> {}, () -> {});
    }
}
