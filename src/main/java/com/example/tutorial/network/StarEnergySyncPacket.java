package com.example.tutorial.network;

import com.example.tutorial.client.ClientCombatState;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record StarEnergySyncPacket(int energy, int maximum, float shield, float maximumShield,
                                  float barrier, int domain, int domainTicks, int breakCooldown, int controlCooldown)
{
    public static void encode(StarEnergySyncPacket message, FriendlyByteBuf buffer)
    {
        buffer.writeVarInt(message.energy);
        buffer.writeVarInt(message.maximum);
        buffer.writeFloat(message.shield);
        buffer.writeFloat(message.maximumShield);
        buffer.writeFloat(message.barrier);
        buffer.writeVarInt(message.domain);
        buffer.writeVarInt(message.domainTicks);
        buffer.writeVarInt(message.breakCooldown);
        buffer.writeVarInt(message.controlCooldown);
    }

    public static StarEnergySyncPacket decode(FriendlyByteBuf buffer)
    {
        return new StarEnergySyncPacket(buffer.readVarInt(), buffer.readVarInt(), buffer.readFloat(), buffer.readFloat(),
                buffer.readFloat(), buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt());
    }

    public static void handle(StarEnergySyncPacket message, Supplier<NetworkEvent.Context> contextSupplier)
    {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ClientCombatState.setAstralResources(message.energy, message.maximum, message.shield, message.maximumShield);
            ClientCombatState.setDomains(message.barrier, message.domain, message.domainTicks, message.breakCooldown, message.controlCooldown);
        });
        context.setPacketHandled(true);
    }
}
