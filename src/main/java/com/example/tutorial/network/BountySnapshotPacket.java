package com.example.tutorial.network;
import com.example.tutorial.client.BountyScreen;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;
public record BountySnapshotPacket(boolean open, int[] states, int[] progress) {
    public static void encode(BountySnapshotPacket p, FriendlyByteBuf b) {
        b.writeBoolean(p.open);
        for (int i = 0; i < 10; i++) { b.writeVarInt(p.states[i]); b.writeVarInt(p.progress[i]); }
    }
    public static BountySnapshotPacket decode(FriendlyByteBuf b) {
        boolean open = b.readBoolean(); int[] s = new int[10], p = new int[10];
        for (int i = 0; i < 10; i++) { s[i] = b.readVarInt(); p[i] = b.readVarInt(); }
        return new BountySnapshotPacket(open, s, p);
    }
    public static void handle(BountySnapshotPacket p, Supplier<NetworkEvent.Context> supplier) {
        var ctx = supplier.get();
        ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> BountyScreen.receive(p)));
        ctx.setPacketHandled(true);
    }
}
