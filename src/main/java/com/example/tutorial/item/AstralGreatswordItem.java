package com.example.tutorial.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;
import com.example.tutorial.event.WeaponMastery;

public class AstralGreatswordItem extends SwordItem
{
    private static final String RANGED_MODE_TAG = "tutorial_mod:ranged_mode";

    public AstralGreatswordItem(Tier tier, int attackDamageModifier, float attackSpeedModifier, Properties properties)
    {
        super(tier, attackDamageModifier, attackSpeedModifier, properties);
    }

    public static boolean isRanged(ItemStack stack)
    {
        return stack.hasTag() && stack.getTag().getBoolean(RANGED_MODE_TAG);
    }
    @Override public boolean isValidRepairItem(ItemStack tool,ItemStack material) {return material.is(ModItems.STAR_INGOT.get());}
    @Override public int getMaxDamage(ItemStack stack) {return com.example.tutorial.event.AstralEquipment.maxDamage(stack,super.getMaxDamage(stack));}

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand)
    {
        ItemStack stack = player.getItemInHand(hand);
        if (player.isShiftKeyDown())
        {
            WeaponMastery.Branch branch = WeaponMastery.cycleBranch(stack);
            player.displayClientMessage(Component.translatable("message.tutorial_mod.branch_" +
                    branch.name().toLowerCase(java.util.Locale.ROOT)), true);
            if (!level.isClientSide()) com.example.tutorial.event.AstralSafety.sound(level, player.blockPosition(), SoundEvents.ENCHANTMENT_TABLE_USE,
                    SoundSource.PLAYERS, 0.65F, 1.1F + branch.ordinal() * 0.15F);
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
        }
        boolean ranged = !isRanged(stack);
        stack.getOrCreateTag().putBoolean(RANGED_MODE_TAG, ranged);

        player.swing(hand, true);
        if (!level.isClientSide())
        {
            if(ranged && player instanceof net.minecraft.server.level.ServerPlayer server) com.example.tutorial.event.AstralProgress.award(server,"ranged");
            player.displayClientMessage(Component.translatable(ranged
                    ? "message.tutorial_mod.mode_ranged"
                    : "message.tutorial_mod.mode_melee"), true);
            com.example.tutorial.event.AstralSafety.sound(level, player.blockPosition(),
                    ranged ? SoundEvents.BEACON_ACTIVATE : SoundEvents.IRON_GOLEM_REPAIR,
                    SoundSource.PLAYERS, 0.65F, ranged ? 1.45F : 1.15F);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag)
    {
        tooltip.add(Component.translatable(isRanged(stack)
                ? "tooltip.tutorial_mod.mode_ranged"
                : "tooltip.tutorial_mod.mode_melee").withStyle(
                isRanged(stack) ? ChatFormatting.AQUA : ChatFormatting.GOLD));
        tooltip.add(Component.translatable("tooltip.tutorial_mod.switch_mode").withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.translatable("tooltip.tutorial_mod.sword_skill").withStyle(ChatFormatting.LIGHT_PURPLE));
        tooltip.add(Component.translatable("tooltip.tutorial_mod.radial_skill").withStyle(ChatFormatting.LIGHT_PURPLE));
        tooltip.add(Component.translatable("tooltip.tutorial_mod.parry").withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.translatable("tooltip.tutorial_mod.energy_gain").withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.translatable("tooltip.tutorial_mod.domain").withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.translatable("tooltip.tutorial_mod.mastery", WeaponMastery.level(stack),
                WeaponMastery.MAX_LEVEL, WeaponMastery.progress(stack)).withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.translatable("tooltip.tutorial_mod.branch_" +
                WeaponMastery.branch(stack).name().toLowerCase(java.util.Locale.ROOT)).withStyle(ChatFormatting.LIGHT_PURPLE));
        tooltip.add(Component.translatable("tooltip.tutorial_mod.switch_branch").withStyle(ChatFormatting.DARK_GRAY));
        super.appendHoverText(stack, level, tooltip, flag);
    }
}
