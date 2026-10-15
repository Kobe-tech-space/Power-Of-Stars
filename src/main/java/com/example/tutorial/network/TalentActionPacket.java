package com.example.tutorial.network;
import com.example.tutorial.event.AstralSafety;
import com.example.tutorial.talent.StellarTalents;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;
public record TalentActionPacket(int action, int node) {
    public static void encode(TalentActionPacket p, FriendlyByteBuf b) { b.writeVarInt(p.action); b.writeVarInt(p.node); }
    public static TalentActionPacket decode(FriendlyByteBuf b) { return new TalentActionPacket(b.readVarInt(), b.readVarInt()); }
    public static void handle(TalentActionPacket p, Supplier<NetworkEvent.Context> supplier) {
        var ctx = supplier.get();
        ctx.enqueueWork(() -> {
            var player = ctx.getSender();
            if (p.action < 0 || p.action > 2 || !AstralSafety.allowPacket(player, false)) return;
            StellarTalents.action(player, p.action, p.node);
        }); ctx.setPacketHandled(true);
    }
}
