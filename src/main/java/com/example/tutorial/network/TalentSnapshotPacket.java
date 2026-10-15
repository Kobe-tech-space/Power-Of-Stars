package com.example.tutorial.network;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import java.util.function.Supplier;
public record TalentSnapshotPacket(boolean open, int experience, int[] ranks, boolean enabled, int resetLevels, int resetIngots) {
    public static void encode(TalentSnapshotPacket p, FriendlyByteBuf b) {
        b.writeBoolean(p.open); b.writeVarInt(p.experience);
        for(int i=0;i<12;i++) b.writeVarInt(p.ranks[i]);
        b.writeBoolean(p.enabled); b.writeVarInt(p.resetLevels); b.writeVarInt(p.resetIngots);
    }
    public static TalentSnapshotPacket decode(FriendlyByteBuf b) {
        boolean open=b.readBoolean(); int experience=b.readVarInt(); int[] ranks=new int[12];
        for(int i=0;i<12;i++) ranks[i]=b.readVarInt();
        return new TalentSnapshotPacket(open,experience,ranks,b.readBoolean(),b.readVarInt(),b.readVarInt());
    }
    public static void handle(TalentSnapshotPacket p, Supplier<NetworkEvent.Context> supplier) {
        var ctx=supplier.get(); ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> com.example.tutorial.client.TalentScreen.receive(p))); ctx.setPacketHandled(true);
    }
}
