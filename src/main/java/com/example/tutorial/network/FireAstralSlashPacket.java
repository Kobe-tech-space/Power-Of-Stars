package com.example.tutorial.network;

import com.example.tutorial.Config;
import com.example.tutorial.entity.AstralSlashEntity;
import com.example.tutorial.enchantment.ModEnchantments;
import com.example.tutorial.item.AstralGreatswordItem;
import com.example.tutorial.item.ModItems;
import com.example.tutorial.event.WeaponMastery;
import com.example.tutorial.event.AstralResonance;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record FireAstralSlashPacket()
{
    private static final String LAST_FIRE_TICK = "tutorial_mod:last_slash_tick";
    public static void encode(FireAstralSlashPacket message, FriendlyByteBuf buffer)
    {
    }

    public static FireAstralSlashPacket decode(FriendlyByteBuf buffer)
    {
        return new FireAstralSlashPacket();
    }

    public static void handle(FireAstralSlashPacket message, Supplier<NetworkEvent.Context> contextSupplier)
    {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {if(com.example.tutorial.event.AstralSafety.allowPacket(context.getSender(),true)) fire(context.getSender());});
        context.setPacketHandled(true);
    }

    private static void fire(ServerPlayer player)
    {
        if (!com.example.tutorial.event.AstralSafety.valid(player))
        {
            return;
        }
        ItemStack weapon = player.getMainHandItem();
        if (!weapon.is(ModItems.ICE_SWORD.get()) || !AstralGreatswordItem.isRanged(weapon))
        {
            return;
        }

        long gameTime = player.level().getGameTime();
        if (player.getPersistentData().getLong(LAST_FIRE_TICK) == gameTime)
        {
            return;
        }
        player.getPersistentData().putLong(LAST_FIRE_TICK, gameTime);

        int active = player.level().getEntitiesOfClass(AstralSlashEntity.class,
                player.getBoundingBox().inflate(Config.slashRange + 4.0D),
                slash -> slash.getOwner() == player).size();
        if (active >= Config.maxSlashes)
        {
            return;
        }

        double factor = ModEnchantments.getDamageFactor(weapon,
                com.example.tutorial.event.AstralSafety.damageBonus(player));
        factor *= com.example.tutorial.event.StarDomains.damageFactor(player) * com.example.tutorial.event.AstralEnvironment.damageFactor(player);
        factor *= WeaponMastery.masteryDamage(weapon) * (1.0D + AstralResonance.level(player) * 0.02D);
        if (WeaponMastery.branch(weapon) == WeaponMastery.Branch.MOON_TRACE) factor *= 1.15D;
        double critChance = Math.min(1.0D, ModEnchantments.getCritChance(weapon));
        boolean critical = critChance > 0.0D && player.getRandom().nextDouble() < critChance;
        if (critical)
        {
            factor *= ModEnchantments.BASE_CRIT_MULTIPLIER + ModEnchantments.getCritDamageBonus(weapon);
        }
        // Snapshot the complete attack attribute too, so Strength and attribute buffs
        // cannot be gained or lost by swapping state while the wave is in flight.
        float snapshotBaseDamage = (float) (player.getAttributeValue(Attributes.ATTACK_DAMAGE) * 0.60D);
        float snapshotDamage = (float) (snapshotBaseDamage * factor);
        boolean momentum = ModEnchantments.hasCombo(weapon);

        AstralSlashEntity slash = new AstralSlashEntity(
                player.level(), player, snapshotDamage, critical, momentum);
        player.level().addFreshEntity(slash);
        weapon.hurtAndBreak(1, player, broken -> broken.broadcastBreakEvent(InteractionHand.MAIN_HAND));
        player.swing(InteractionHand.MAIN_HAND, true);
        // Firing/missing sound. A successful impact has its own distinct sound in the entity.
        com.example.tutorial.event.AstralSafety.sound(player.level(), player.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP,
                SoundSource.PLAYERS, 0.65F, 0.72F + player.getRandom().nextFloat() * 0.08F);
    }
}
