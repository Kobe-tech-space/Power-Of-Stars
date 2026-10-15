package com.example.tutorial.network;
import com.example.tutorial.bounty.BountySystem;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;
public record BountyActionPacket(int id, int action) {
    public static void encode(BountyActionPacket p, FriendlyByteBuf b) { b.writeVarInt(p.id); b.writeVarInt(p.action); }
    public static BountyActionPacket decode(FriendlyByteBuf b) { return new BountyActionPacket(b.readVarInt(), b.readVarInt()); }
    public static void handle(BountyActionPacket p, Supplier<NetworkEvent.Context> supplier) {
        var ctx = supplier.get();
        ctx.enqueueWork(() -> {
            var player = ctx.getSender();
            if (player == null) return;
            long now = player.server.overworld().getGameTime();
            var data = player.getPersistentData();
            String key = "tutorial_mod:bounty_packet_tick";
            if (p.action == 0) {
                if (data.contains(key) && now >= data.getLong(key) && now - data.getLong(key) < 10) return;
                data.putLong(key, now);
            }
            BountySystem.action(player, p.id, p.action);
        });
        ctx.setPacketHandled(true);
    }
}
