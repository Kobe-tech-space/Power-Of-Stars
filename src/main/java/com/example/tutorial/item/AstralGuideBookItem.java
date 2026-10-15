package com.example.tutorial.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import com.example.tutorial.network.ModNetwork;
import com.example.tutorial.network.OpenAstralGuidePacket;

import javax.annotation.Nullable;
import java.util.List;

/** Keeps the original item ID while opening the categorized Patchouli guide. */
public final class AstralGuideBookItem extends Item
{

    public AstralGuideBookItem(Properties properties)
    {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand)
    {
        ItemStack stack = player.getItemInHand(hand);
        if (player instanceof ServerPlayer serverPlayer)
            ModNetwork.sendToPlayer(serverPlayer, new OpenAstralGuidePacket(hand));
        player.awardStat(Stats.ITEM_USED.get(this));
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag)
    {
        tooltip.add(Component.translatable("tooltip.tutorial_mod.astral_guide"));
    }

}
