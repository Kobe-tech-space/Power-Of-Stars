package com.example.tutorial.network;

import com.example.tutorial.client.ClientGuideOpener;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.InteractionHand;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;

import java.util.function.Supplier;

public record OpenAstralGuidePacket(InteractionHand hand)
{
    public static void encode(OpenAstralGuidePacket message, FriendlyByteBuf buffer)
    {
        buffer.writeEnum(message.hand);
    }

    public static OpenAstralGuidePacket decode(FriendlyByteBuf buffer)
    {
        return new OpenAstralGuidePacket(buffer.readEnum(InteractionHand.class));
    }

    public static void handle(OpenAstralGuidePacket message, Supplier<NetworkEvent.Context> contextSupplier)
    {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> ClientGuideOpener.open(message.hand)));
        context.setPacketHandled(true);
    }
}
