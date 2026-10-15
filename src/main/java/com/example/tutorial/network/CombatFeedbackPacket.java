package com.example.tutorial.network;

import com.example.tutorial.client.ClientCombatState;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Server-to-client snapshot used only for combat HUD feedback. */
public record CombatFeedbackPacket(int combo, boolean critical, int milestone, float criticalDamage)
{
    public static void encode(CombatFeedbackPacket message, FriendlyByteBuf buffer)
    {
        buffer.writeVarInt(message.combo);
        buffer.writeBoolean(message.critical);
        buffer.writeVarInt(message.milestone);
        buffer.writeFloat(message.criticalDamage);
    }

    public static CombatFeedbackPacket decode(FriendlyByteBuf buffer)
    {
        return new CombatFeedbackPacket(
                buffer.readVarInt(), buffer.readBoolean(), buffer.readVarInt(), buffer.readFloat());
    }

    public static void handle(CombatFeedbackPacket message, Supplier<NetworkEvent.Context> contextSupplier)
    {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> ClientCombatState.accept(message));
        context.setPacketHandled(true);
    }
}
