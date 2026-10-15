package com.example.tutorial.event;

import com.example.tutorial.block.ModBlocks;
import com.example.tutorial.item.ModItems;
import com.example.tutorial.item.StarArmorItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.phys.Vec3;

/** Bounded scans and collision-checked movement, decided entirely on the server. */
public final class StarExploration {
    private StarExploration() {}
    public static void activate(ServerPlayer player, int ability) {
        if (!AstralSafety.valid(player) || ability < 0 || ability > 2) return;
        boolean equipped = StarArmorItem.hasFullSet(player);
        if (!equipped) {
            player.displayClientMessage(Component.translatable("message.tutorial_mod.skill_requires_set"), true);
            return;
        }
        if (player.isPassenger()) {
            player.displayClientMessage(Component.translatable("message.tutorial_mod.movement_riding"), true);
            return;
        }
        String key = "tutorial_mod:exploration_" + ability;
        long now = player.level().getGameTime();
        // Scan retains its interval. Movement ignores legacy cooldown timestamps.
        if (ability == 0 && now < player.getPersistentData().getLong(key)) return;
        if (ability != 0 && player.getPersistentData().contains("tutorial_mod:last_star_move_tick")
                && now == player.getPersistentData().getLong("tutorial_mod:last_star_move_tick")) return;
        int cost = AstralEquipment.skillCost(player,ability == 0 ? 10 : ability == 1 ? 20 : 40);
        if (StarEnergy.get(player) < cost) {
            player.displayClientMessage(Component.translatable("message.tutorial_mod.not_enough_star_energy", cost), true);
            return;
        }
        if (ability == 0) {
            BlockPos origin = player.blockPosition(), nearest = null;
            double distance = Double.MAX_VALUE;
            for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-12,-12,-12), origin.offset(12,12,12))) {
                if (!player.serverLevel().hasChunkAt(pos) || !player.level().getBlockState(pos).is(ModBlocks.STAR_ORE.get())) continue;
                double next = origin.distSqr(pos);
                if (next < distance) { distance = next; nearest = pos.immutable(); }
            }
            StarEnergy.consume(player, cost);
            AstralStatistics.skill(player,"scan");
            player.getPersistentData().putLong(key, now + 100);
            if (nearest == null) player.displayClientMessage(Component.translatable("exploration.tutorial_mod.no_ore"), true);
            else {
                Vec3 delta = Vec3.atCenterOf(nearest).subtract(player.position());
                String direction = Math.abs(delta.x) > Math.abs(delta.z) ? delta.x > 0 ? "east" : "west" : delta.z > 0 ? "south" : "north";
                player.displayClientMessage(Component.translatable("exploration.tutorial_mod.ore", Component.translatable("exploration.tutorial_mod."+direction), Math.round(Math.sqrt(distance))), true);
            }
            com.example.tutorial.event.AstralSafety.notifySound(player,SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.8F, 1.5F);
            return;
        }
        Vec3 start = player.position(), direction = player.getLookAngle();
        // Ground dash must not drive the player's feet into the floor when looking down.
        if (ability == 1) {
            direction = new Vec3(direction.x, 0, direction.z);
            direction = direction.lengthSqr() < 1.0E-6 ? Vec3.directionFromRotation(0, player.getYRot()) : direction.normalize();
        }
        Vec3 destination = start;
        double range = ability == 1 ? 3.5 : 8.0;
        for (double step = 0.25; step <= range; step += 0.25) {
            Vec3 candidate = start.add(direction.scale(step));
            BlockPos pos = BlockPos.containing(candidate);
            if (!player.serverLevel().hasChunkAt(pos) || !player.level().getWorldBorder().isWithinBounds(pos)
                    || candidate.y < player.level().getMinBuildHeight() || candidate.y + player.getBbHeight() >= player.level().getMaxBuildHeight()
                    || !player.level().noCollision(player, player.getBoundingBox().move(candidate.subtract(start)))
                    || player.level().containsAnyLiquid(player.getBoundingBox().move(candidate.subtract(start)))) break;
            destination = candidate;
        }
        if (destination.distanceToSqr(start) < 0.25) {
            player.displayClientMessage(Component.translatable("message.tutorial_mod.movement_blocked"), true);
            return;
        }
        StarEnergy.consume(player, cost);
        AstralStatistics.skill(player,ability==1?"dash":"blink");
        player.getPersistentData().remove(key);
        player.getPersistentData().putLong("tutorial_mod:last_star_move_tick", now);
        player.connection.teleport(destination.x, destination.y, destination.z, player.getYRot(), player.getXRot());
        player.fallDistance = 0;
        for (int i=0; i<=12; i++) {
            Vec3 point = start.lerp(destination, i/12.0).add(0,0.8,0);
            com.example.tutorial.event.AstralSafety.particles(player.serverLevel(),ParticleTypes.END_ROD,point.x,point.y,point.z,1,0.1,0.2,0.1,0.01);
        }
        com.example.tutorial.event.AstralSafety.sound(player.level(), player.blockPosition(),SoundEvents.ENDERMAN_TELEPORT,SoundSource.PLAYERS,0.65F,1.5F);
    }
}
