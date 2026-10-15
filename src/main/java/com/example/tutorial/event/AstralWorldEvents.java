package com.example.tutorial.event;

import com.example.tutorial.TutorialMod;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = TutorialMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class AstralWorldEvents
{
    public enum Type { NONE, STARFALL, RADIANCE, ECLIPSE }
    private AstralWorldEvents() {}

    public static Type current(ServerLevel level)
    {
        Data data = data(level);
        return level.getGameTime() < data.endsAt ? Type.values()[data.type] : Type.NONE;
    }

    public static int energyMultiplier(ServerLevel level) { return current(level) == Type.RADIANCE ? 2 : 1; }
    public static double eliteChance(ServerLevel level) { return com.example.tutorial.config.ModSettings.d(current(level) == Type.ECLIPSE ? "elites.eclipseChance" : "elites.spawnChance"); }

    @SubscribeEvent
    public static void tick(TickEvent.LevelTickEvent event)
    {
        if (event.phase != TickEvent.Phase.END || !(event.level instanceof ServerLevel level)
                || level.dimension() != net.minecraft.world.level.Level.OVERWORLD || level.getGameTime() % 200L != 0L) return;
        Data data = data(level);
        long day = level.getDayTime() / 24000L;
        if (day > data.lastRollDay)
        {
            data.lastRollDay = day;
            if (level.random.nextDouble() < 0.28D)
            {
                data.type = 1 + level.random.nextInt(3);
                data.endsAt = level.getGameTime() + 12000L;
                data.setDirty();
                Component message = Component.translatable("event.tutorial_mod.start_" + Type.values()[data.type].name().toLowerCase());
                level.getServer().getPlayerList().broadcastSystemMessage(message, false);
            }
            else data.setDirty();
        }
        if (data.type != 0 && level.getGameTime() >= data.endsAt)
        {
            data.type = 0;
            data.setDirty();
            level.getServer().getPlayerList().broadcastSystemMessage(Component.translatable("event.tutorial_mod.end"), false);
        }
    }

    private static Data data(ServerLevel level)
    {
        ServerLevel overworld = level.getServer().overworld();
        return overworld.getDataStorage().computeIfAbsent(Data::load, Data::new, "tutorial_mod_astral_events");
    }

    public static final class Data extends SavedData
    {
        private long lastRollDay = -1L;
        private long endsAt;
        private int type;
        public static Data load(CompoundTag tag)
        {
            Data data = new Data();
            data.lastRollDay = tag.getLong("LastRollDay"); data.endsAt = tag.getLong("EndsAt"); data.type = tag.getInt("Type");
            return data;
        }
        @Override public CompoundTag save(CompoundTag tag)
        {
            tag.putLong("LastRollDay", lastRollDay); tag.putLong("EndsAt", endsAt); tag.putInt("Type", type); return tag;
        }
    }
}
