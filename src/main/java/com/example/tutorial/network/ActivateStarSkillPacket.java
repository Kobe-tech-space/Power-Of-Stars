package com.example.tutorial.network;

import com.example.tutorial.enchantment.ModEnchantments;
import com.example.tutorial.Config;
import com.example.tutorial.entity.AstralSlashEntity;
import com.example.tutorial.event.StarArmorEventHandler;
import com.example.tutorial.event.StarEnergy;
import com.example.tutorial.item.ModItems;
import com.example.tutorial.item.StarArmorItem;
import com.example.tutorial.event.WeaponMastery;
import com.example.tutorial.event.AstralResonance;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record ActivateStarSkillPacket(Skill skill)
{
    public enum Skill { GREAT_SWORD, ARMOR_SET, PARRY, SCAN, DASH, BLINK, DOMAIN }

    public static void encode(ActivateStarSkillPacket message, FriendlyByteBuf buffer)
    {
        buffer.writeEnum(message.skill);
    }

    public static ActivateStarSkillPacket decode(FriendlyByteBuf buffer)
    {
        return new ActivateStarSkillPacket(buffer.readEnum(Skill.class));
    }

    public static void handle(ActivateStarSkillPacket message, Supplier<NetworkEvent.Context> contextSupplier)
    {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {if(com.example.tutorial.event.AstralSafety.allowPacket(context.getSender(),false)) activate(context.getSender(),message.skill);});
        context.setPacketHandled(true);
    }

    public static void activate(ServerPlayer player, Skill skill)
    {
        if (!com.example.tutorial.event.AstralSafety.valid(player)) return;
        if (skill == Skill.DOMAIN) com.example.tutorial.event.StarDomains.activate(player);
        else if (skill == Skill.GREAT_SWORD) activateGreatSword(player);
        else if (skill == Skill.ARMOR_SET) activateArmor(player);
        else if (skill == Skill.PARRY) activateParry(player);
        else com.example.tutorial.event.StarExploration.activate(player, skill.ordinal() - Skill.SCAN.ordinal());
    }

    private static void activateGreatSword(ServerPlayer player)
    {
        ItemStack weapon = player.getMainHandItem();
        int active = player.level().getEntitiesOfClass(AstralSlashEntity.class,
                player.getBoundingBox().inflate(Config.slashRange + 4.0D),
                slash -> slash.getOwner() == player).size();
        int cost = com.example.tutorial.event.AstralEquipment.skillCost(player,player.isShiftKeyDown() ? Config.radialSkillCost : Config.swordSkillCost);
        int count = player.isShiftKeyDown() ? 8 : 3;
        if (!weapon.is(ModItems.ICE_SWORD.get()) || active > Config.maxSlashes - count
                || !StarEnergy.consume(player, cost))
        {
            fail(player, cost);
            return;
        }

        double factor = ModEnchantments.getDamageFactor(weapon,
                com.example.tutorial.event.AstralSafety.damageBonus(player));
        factor *= com.example.tutorial.event.StarDomains.damageFactor(player) * com.example.tutorial.event.AstralEnvironment.damageFactor(player);
        factor *= WeaponMastery.masteryDamage(weapon) * (1.0D + AstralResonance.level(player) * 0.02D);
        if (WeaponMastery.branch(weapon) == WeaponMastery.Branch.MOON_TRACE) factor *= 1.15D;
        double critChance = Math.min(1.0D, ModEnchantments.getCritChance(weapon));
        boolean critical = critChance > 0.0D && player.getRandom().nextDouble() < critChance;
        if (critical) factor *= ModEnchantments.BASE_CRIT_MULTIPLIER + ModEnchantments.getCritDamageBonus(weapon);
        float damage = (float) (player.getAttributeValue(Attributes.ATTACK_DAMAGE) * com.example.tutorial.config.ModSettings.d("slash.skillDamage") * factor);
        boolean momentum = ModEnchantments.hasCombo(weapon);
        Vec3 look = player.getLookAngle();
        float[] angles = player.isShiftKeyDown()
                ? new float[]{0.0F, 0.785F, 1.571F, 2.356F, 3.142F, 3.927F, 4.712F, 5.498F}
                : new float[]{-0.16F, 0.0F, 0.16F};
        Vec3 base = player.isShiftKeyDown() ? new Vec3(0.0D, 0.0D, 1.0D) : look;
        int voice=0;
        for (float angle : angles)
        {
            player.level().addFreshEntity(new AstralSlashEntity(
                    player.level(), player, damage, critical, momentum, base.yRot(angle), (float)com.example.tutorial.config.ModSettings.d("slash.skillSpeed")));
            if(voice<3) com.example.tutorial.event.AstralSounds.play(player,"triple_"+voice,SoundEvents.PLAYER_ATTACK_SWEEP,.35F,.75F+voice*.14F);
            voice++;
        }
        com.example.tutorial.event.AstralStatistics.skill(player,"triple_slash");
        weapon.hurtAndBreak(3, player, broken -> broken.broadcastBreakEvent(InteractionHand.MAIN_HAND));
        player.swing(InteractionHand.MAIN_HAND, true);
        if (player.level() instanceof ServerLevel level)
        {
            com.example.tutorial.event.AstralSafety.particles(level,ParticleTypes.END_ROD, player.getX(), player.getEyeY(), player.getZ(),
                    42, 0.65D, 0.65D, 0.65D, 0.2D);
            com.example.tutorial.event.AstralSafety.sound(level, player.blockPosition(), SoundEvents.BEACON_POWER_SELECT,
                    SoundSource.PLAYERS, 1.2F, 1.65F);
            com.example.tutorial.event.AstralSafety.sound(level, player.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP,
                    SoundSource.PLAYERS, 1.3F, 0.62F);
        }
    }

    private static void activateArmor(ServerPlayer player)
    {
        int cost=com.example.tutorial.event.AstralEquipment.skillCost(player,com.example.tutorial.config.ModSettings.i("shield.reforgeCost"));
        if (!StarArmorItem.hasFullSet(player)) {
            player.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.tutorial_mod.skill_requires_set"), true);
            return;
        }
        if (!StarArmorEventHandler.needsShieldRestore(player)) {
            player.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.tutorial_mod.shield_already_full"), true);
            return;
        }
        if (!StarEnergy.consume(player, cost))
        {
            fail(player, cost);
            return;
        }
        StarArmorEventHandler.restoreStellarShield(player);
        com.example.tutorial.event.AstralEquipment.wearArmor(player);
        com.example.tutorial.event.AstralStatistics.skill(player,"reforge");
        com.example.tutorial.event.AstralSounds.play(player,"reforge",SoundEvents.IRON_GOLEM_REPAIR,.8F,1.35F);
        if (player.level() instanceof ServerLevel level)
        {
            com.example.tutorial.event.AstralSafety.particles(level,ParticleTypes.TOTEM_OF_UNDYING, player.getX(), player.getY(0.55D), player.getZ(),
                    70, 0.85D, 1.0D, 0.85D, 0.2D);
            com.example.tutorial.event.AstralSafety.particles(level,ParticleTypes.END_ROD, player.getX(), player.getY(0.65D), player.getZ(),
                    55, 0.75D, 0.9D, 0.75D, 0.16D);
            com.example.tutorial.event.AstralSafety.sound(level, player.blockPosition(), SoundEvents.TOTEM_USE,
                    SoundSource.PLAYERS, 1.0F, 1.35F);
        }
    }

    private static void activateParry(ServerPlayer player)
    {
        int cost=com.example.tutorial.event.AstralEquipment.skillCost(player,Config.parryCost);
        if (!player.getMainHandItem().is(ModItems.ICE_SWORD.get())
                || !StarEnergy.consume(player, cost))
        {
            fail(player, cost);
            return;
        }
        player.getPersistentData().putLong("tutorial_mod:parry_until", player.level().getGameTime() + 8L);
        com.example.tutorial.event.AstralStatistics.skill(player,"parry");
        com.example.tutorial.event.AstralSafety.notifySound(player,SoundEvents.ARMOR_EQUIP_NETHERITE, SoundSource.PLAYERS, 0.75F, 1.45F);
    }

    private static void fail(ServerPlayer player, int cost)
    {
        StarEnergy.sync(player);
        player.displayClientMessage(net.minecraft.network.chat.Component.translatable(
                "message.tutorial_mod.not_enough_star_energy", cost), true);
        com.example.tutorial.event.AstralSounds.play(player,"failure",SoundEvents.NOTE_BLOCK_BASS.get(),.5F,.65F);
    }
}
