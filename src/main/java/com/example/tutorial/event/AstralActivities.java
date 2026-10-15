package com.example.tutorial.event;

import com.example.tutorial.TutorialMod;
import com.example.tutorial.block.ModBlocks;
import com.example.tutorial.item.ModItems;
import com.example.tutorial.item.StarArmorItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

@Mod.EventBusSubscriber(modid = TutorialMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class AstralActivities
{
    private static final String ACTIVE = "tutorial_mod:trial_active";
    private static final String TRIAL_OWNER = "tutorial_mod:trial_owner";
    private AstralActivities() {}

    @SubscribeEvent
    public static void useBeacon(PlayerInteractEvent.RightClickBlock event)
    {
        if (event.getHand() != InteractionHand.MAIN_HAND || !event.getLevel().getBlockState(event.getPos()).is(Blocks.BEACON)
                || !event.getItemStack().is(ModItems.STAR_INGOT.get()) || !(event.getEntity() instanceof ServerPlayer player)) return;
        event.setCanceled(true);
        if (player.isShiftKeyDown()) activateAltar(player, event.getPos(), event.getItemStack());
        else startTrial(player, event.getPos(), event.getItemStack());
    }

    private static void startTrial(ServerPlayer player, BlockPos center, ItemStack payment)
    {
        var data = player.getPersistentData();
        if (data.getBoolean(ACTIVE)) { player.displayClientMessage(Component.translatable("trial.tutorial_mod.already_active"), true); return; }
        consume(player, payment);
        data.putBoolean(ACTIVE, true); data.putInt("tutorial_mod:trial_wave", 0);
        data.putLong("tutorial_mod:trial_next", player.level().getGameTime() + 40L);
        data.putLong("tutorial_mod:trial_x", center.getX()); data.putLong("tutorial_mod:trial_y", center.getY()); data.putLong("tutorial_mod:trial_z", center.getZ());
        player.displayClientMessage(Component.translatable("trial.tutorial_mod.started"), false);
    }

    private static void activateAltar(ServerPlayer player, BlockPos center, ItemStack payment)
    {
        ServerLevel level = player.serverLevel();
        if (!validAltar(level, center)) { player.displayClientMessage(Component.translatable("altar.tutorial_mod.invalid"), true); return; }
        consume(player, payment);
        StarEnergy.set(player, StarEnergy.maximum(player));
        // Shield reforging is paid separately (150 energy); the altar cannot grant it for one ingot.
        for (ItemStack armor : player.getArmorSlots())
            if (armor.isDamaged()) armor.setDamageValue(Math.max(0, armor.getDamageValue() - 500));
        for (ItemStack inventoryStack : player.getInventory().items)
            if (inventoryStack.is(ModItems.ICE_SWORD.get()) && inventoryStack.isDamaged())
            {
                inventoryStack.setDamageValue(Math.max(0, inventoryStack.getDamageValue() - 1000));
                break;
            }
        com.example.tutorial.event.AstralSafety.particles(level,ParticleTypes.END_ROD, center.getX() + 0.5D, center.getY() + 1.2D, center.getZ() + 0.5D,
                90, 1.5D, 1.0D, 1.5D, 0.2D);
        com.example.tutorial.event.AstralSafety.sound(level, center, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 1.4F, 1.6F);
        player.displayClientMessage(Component.translatable("altar.tutorial_mod.activated"), false);
    }

    private static boolean validAltar(ServerLevel level, BlockPos p)
    {
        return level.getBlockState(p.north()).is(Blocks.AMETHYST_BLOCK) && level.getBlockState(p.south()).is(Blocks.AMETHYST_BLOCK)
                && level.getBlockState(p.east()).is(Blocks.AMETHYST_BLOCK) && level.getBlockState(p.west()).is(Blocks.AMETHYST_BLOCK)
                && level.getBlockState(p.north().east()).is(Blocks.CRYING_OBSIDIAN)
                && level.getBlockState(p.north().west()).is(Blocks.CRYING_OBSIDIAN)
                && level.getBlockState(p.south().east()).is(Blocks.CRYING_OBSIDIAN)
                && level.getBlockState(p.south().west()).is(Blocks.CRYING_OBSIDIAN);
    }

    @SubscribeEvent
    public static void playerTick(TickEvent.PlayerTickEvent event)
    {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player) || player.tickCount % 20 != 0) return;
        var data = player.getPersistentData();
        if (!data.getBoolean(ACTIVE)) return;
        BlockPos center = new BlockPos((int)data.getLong("tutorial_mod:trial_x"), (int)data.getLong("tutorial_mod:trial_y"), (int)data.getLong("tutorial_mod:trial_z"));
        if (player.blockPosition().distSqr(center) > 4096.0D) { data.putBoolean(ACTIVE, false); player.displayClientMessage(Component.translatable("trial.tutorial_mod.failed"), false); return; }
        List<Mob> alive = player.serverLevel().getEntitiesOfClass(Mob.class, new net.minecraft.world.phys.AABB(center).inflate(48.0D),
                mob -> player.getStringUUID().equals(mob.getPersistentData().getString(TRIAL_OWNER)));
        if (!alive.isEmpty() || player.level().getGameTime() < data.getLong("tutorial_mod:trial_next")) return;
        int wave = data.getInt("tutorial_mod:trial_wave") + 1;
        if (wave > 3)
        {
            data.putBoolean(ACTIVE, false);
            player.getInventory().placeItemBackInInventory(new ItemStack(ModItems.STAR_INGOT.get(), 4));
            player.giveExperiencePoints(300);
            player.displayClientMessage(Component.translatable("trial.tutorial_mod.complete"), false);
            com.example.tutorial.bounty.BountySystem.record(player, "trial", "");
            AstralProgress.award(player,"trial");
            return;
        }
        data.putInt("tutorial_mod:trial_wave", wave); data.putLong("tutorial_mod:trial_next", player.level().getGameTime() + 60L);
        spawnWave(player, center, wave);
    }

    private static void spawnWave(ServerPlayer player, BlockPos center, int wave)
    {
        ServerLevel level = player.serverLevel();
        int count = 3 + wave * 2;
        for (int i = 0; i < count; i++)
        {
            EntityType<? extends Mob> type = i % 3 == 0 ? EntityType.SKELETON : i % 3 == 1 ? EntityType.ZOMBIE : EntityType.SPIDER;
            Mob mob = type.create(level); if (mob == null) continue;
            double angle = Math.PI * 2.0D * i / count; int x = center.getX() + (int)Math.round(Math.cos(angle) * 7.0D); int z = center.getZ() + (int)Math.round(Math.sin(angle) * 7.0D);
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            mob.moveTo(x + 0.5D, y, z + 0.5D, player.getYRot(), 0.0F); mob.getPersistentData().putString(TRIAL_OWNER, player.getStringUUID()); mob.setTarget(player);
            level.addFreshEntity(mob);
            if (wave == 3 && i == count - 1) AstralEliteHandler.empower(mob, level.random.nextInt(4));
        }
        player.displayClientMessage(Component.translatable("trial.tutorial_mod.wave", wave, 3), false);
        com.example.tutorial.event.AstralSafety.sound(level, center, SoundEvents.END_PORTAL_SPAWN, SoundSource.HOSTILE, 0.7F, 1.4F);
    }

    @SubscribeEvent
    public static void breakOre(BlockEvent.BreakEvent event)
    {
        if (!(event.getPlayer() instanceof ServerPlayer player) || !event.getState().is(ModBlocks.STAR_ORE.get())
                || AstralWorldEvents.current(player.serverLevel()) != AstralWorldEvents.Type.STARFALL) return;
        if (player.getRandom().nextBoolean()) player.spawnAtLocation(new ItemStack(ModItems.STAR_INGOT.get()));
    }

    private static void consume(ServerPlayer player, ItemStack stack) { if (!player.getAbilities().instabuild) stack.shrink(1); }
}
