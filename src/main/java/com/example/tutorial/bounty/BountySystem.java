package com.example.tutorial.bounty;

import com.example.tutorial.TutorialMod;
import com.example.tutorial.network.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

@Mod.EventBusSubscriber(modid = TutorialMod.MOD_ID)
public final class BountySystem {
    public static final String KEY = "tutorial_mod:bounties_v1";
    private BountySystem() {}
    private static CompoundTag data(ServerPlayer p) {
        if (!p.getPersistentData().contains(KEY, 10)) p.getPersistentData().put(KEY, new CompoundTag());
        CompoundTag tag = p.getPersistentData().getCompound(KEY);
        long day = Math.max(0, p.server.overworld().getDayTime() / 24000L);
        if (!tag.contains("day", 4)) tag.putLong("day", day);
        else if (DailyBountyCycle.shouldRefresh(tag.getLong("day"), day)) {
            for (int i = 0; i < 10; i++) { tag.remove("s" + i); tag.remove("p" + i); }
            tag.putLong("day", day);
        }
        return tag;
    }
    public static int state(ServerPlayer p, int id) { return data(p).getInt("s" + id); }
    public static int progress(ServerPlayer p, int id) { return data(p).getInt("p" + id); }
    public static void sync(ServerPlayer p, boolean open) {
        int[] states = new int[10], progress = new int[10];
        for (int i = 0; i < 10; i++) { states[i] = state(p, i); progress[i] = progress(p, i); }
        ModNetwork.sendToPlayer(p, new BountySnapshotPacket(open, states, progress));
    }
    /** 0 = available, 1 = accepted, 2 = claimed. All mutations run on the server thread. */
    public static void action(ServerPlayer p, int id, int action) {
        if (action == 0) { sync(p, true); return; }
        if (action != 1 && action != 2) return;
        if (id < 0 || id >= BountyCatalog.QUESTS.size()) return;
        CompoundTag tag = data(p);
        if (action == 1 && state(p, id) == 0) tag.putInt("s" + id, 1);
        else if (action == 2 && state(p, id) == 1 && progress(p, id) >= BountyCatalog.QUESTS.get(id).goal()) {
            // Commit first: replayed packets cannot grant the same reward twice.
            tag.putInt("s" + id, 2);
            for (var reward : BountyCatalog.rewards(id)) p.getInventory().placeItemBackInInventory(reward.copy());
            p.giveExperiencePoints(BountyCatalog.QUESTS.get(id).xp());
            com.example.tutorial.event.AstralSafety.sound(p.serverLevel(), p.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, .5F, 1.4F);
        }
        sync(p, false);
    }
    public static void record(ServerPlayer p, String trigger, String target) {
        if (p.isCreative() || p.isSpectator()) return;
        boolean changed = false;
        for (int i = 0; i < BountyCatalog.QUESTS.size(); i++) {
            var quest = BountyCatalog.QUESTS.get(i);
            int previous = progress(p, i);
            if (state(p, i) != 1 || previous >= quest.goal() || !quest.trigger().equals(trigger)
                || (!quest.target().isEmpty() && !quest.target().equals(target))) continue;
            int next = Math.min(quest.goal(), previous + 1);
            data(p).putInt("p" + i, next); changed = true;
            if (next == quest.goal()) p.displayClientMessage(Component.literal("悬赏完成：" + quest.title() + " · 按悬赏键领取奖励"), false);
        }
        if (changed) sync(p, false);
    }
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void death(LivingDeathEvent e) {
        if (e.isCanceled() || !(e.getSource().getEntity() instanceof ServerPlayer p)) return;
        record(p, "kill", String.valueOf(ForgeRegistries.ENTITY_TYPES.getKey(e.getEntity().getType())));
        if (e.getEntity().getPersistentData().getBoolean("tutorial_mod:astral_elite")) record(p, "elite", "");
    }
    @SubscribeEvent public static void clone(PlayerEvent.Clone e) {
        if (e.getOriginal().getPersistentData().contains(KEY)) e.getEntity().getPersistentData().put(KEY, e.getOriginal().getPersistentData().getCompound(KEY).copy());
    }
    @SubscribeEvent public static void login(PlayerEvent.PlayerLoggedInEvent e) { if (e.getEntity() instanceof ServerPlayer p) sync(p, false); }
    @SubscribeEvent public static void respawn(PlayerEvent.PlayerRespawnEvent e) { if (e.getEntity() instanceof ServerPlayer p) sync(p, false); }
    @SubscribeEvent public static void tick(net.minecraftforge.event.TickEvent.PlayerTickEvent e) {
        if (e.phase != net.minecraftforge.event.TickEvent.Phase.END || !(e.player instanceof ServerPlayer p)
                || p.tickCount % 20 != 0) return;
        CompoundTag previous = p.getPersistentData().getCompound(KEY);
        long oldDay = previous.contains("day", 4) ? previous.getLong("day") : Long.MIN_VALUE;
        if (data(p).getLong("day") != oldDay) sync(p, false);
    }
}
