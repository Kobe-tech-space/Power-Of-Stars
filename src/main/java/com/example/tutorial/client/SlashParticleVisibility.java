package com.example.tutorial.client;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
/** Client camera access stays outside the common projectile class. */
public final class SlashParticleVisibility {
    public static boolean visible(Entity wave) {
        var player=Minecraft.getInstance().player;
        return player!=null && wave.distanceToSqr(player)<=2304;
    }
    private SlashParticleVisibility() {}
}
