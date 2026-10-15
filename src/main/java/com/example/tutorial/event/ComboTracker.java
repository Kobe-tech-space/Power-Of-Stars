package com.example.tutorial.event;

import net.minecraft.world.entity.player.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 「越战越勇」的连击计数器：每个玩家一份，命中时 +1，
 * 超过 {@link #RESET_MILLIS} 没有造成伤害就归零。
 *
 * <p>只在玩家武器上带着该附魔时才会被调用，所以不持有附魔的玩家不会产生任何状态。</p>
 */
public final class ComboTracker
{
    /** 超过这个时间没有造成伤害，连击清零（用户定的 30 秒）。 */
    public static final long RESET_MILLIS = 30_000L;

    private static final Map<UUID, State> STATES = new ConcurrentHashMap<>();

    private ComboTracker()
    {
    }

    /** 记录一次命中，返回命中之后的连击数。 */
    public static int onHit(Player player)
    {
        long now = System.currentTimeMillis();
        State previous = STATES.get(player.getUUID());
        int combo = (previous == null || now - previous.lastHitMillis() > RESET_MILLIS)
                ? 1
                : Math.min(9_999, previous.combo() + 1);
        STATES.put(player.getUUID(), new State(combo, now));
        if(player instanceof net.minecraft.server.level.ServerPlayer server) {
            AstralStatistics.maximum(server,"highest_combo",combo);
            if(combo>=150) AstralProgress.award(server,"combo_150");
        }
        return combo;
    }

    /** Returns the combo that the next successful hit would produce without mutating state. */
    public static int previewNextHit(Player player)
    {
        long now = System.currentTimeMillis();
        State previous = STATES.get(player.getUUID());
        return (previous == null || now - previous.lastHitMillis() > RESET_MILLIS)
                ? 1
                : Math.min(9_999, previous.combo() + 1);
    }

    /** 玩家退出时清掉状态，避免残留。 */
    public static void clear(Player player)
    {
        STATES.remove(player.getUUID());
    }

    private record State(int combo, long lastHitMillis)
    {
    }
}
