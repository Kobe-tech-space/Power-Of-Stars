package com.example.tutorial.network;

import com.example.tutorial.TutorialMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public final class ModNetwork
{
    private static final String PROTOCOL_VERSION = "12";
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            ResourceLocation.fromNamespaceAndPath(TutorialMod.MOD_ID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals);

    private ModNetwork()
    {
    }

    public static void register()
    {
        CHANNEL.messageBuilder(TalentActionPacket.class, 7, NetworkDirection.PLAY_TO_SERVER)
                .encoder(TalentActionPacket::encode).decoder(TalentActionPacket::decode)
                .consumerMainThread(TalentActionPacket::handle).add();
        CHANNEL.messageBuilder(TalentSnapshotPacket.class, 8, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(TalentSnapshotPacket::encode).decoder(TalentSnapshotPacket::decode)
                .consumerMainThread(TalentSnapshotPacket::handle).add();
        CHANNEL.messageBuilder(BountyActionPacket.class, 5, NetworkDirection.PLAY_TO_SERVER)
                .encoder(BountyActionPacket::encode).decoder(BountyActionPacket::decode)
                .consumerMainThread(BountyActionPacket::handle).add();
        CHANNEL.messageBuilder(BountySnapshotPacket.class, 6, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(BountySnapshotPacket::encode).decoder(BountySnapshotPacket::decode)
                .consumerMainThread(BountySnapshotPacket::handle).add();
        CHANNEL.messageBuilder(CombatFeedbackPacket.class, 0, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(CombatFeedbackPacket::encode)
                .decoder(CombatFeedbackPacket::decode)
                .consumerMainThread(CombatFeedbackPacket::handle)
                .add();
        CHANNEL.messageBuilder(FireAstralSlashPacket.class, 1, NetworkDirection.PLAY_TO_SERVER)
                .encoder(FireAstralSlashPacket::encode)
                .decoder(FireAstralSlashPacket::decode)
                .consumerMainThread(FireAstralSlashPacket::handle)
                .add();
        CHANNEL.messageBuilder(StarEnergySyncPacket.class, 2, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(StarEnergySyncPacket::encode)
                .decoder(StarEnergySyncPacket::decode)
                .consumerMainThread(StarEnergySyncPacket::handle)
                .add();
        CHANNEL.messageBuilder(ActivateStarSkillPacket.class, 3, NetworkDirection.PLAY_TO_SERVER)
                .encoder(ActivateStarSkillPacket::encode)
                .decoder(ActivateStarSkillPacket::decode)
                .consumerMainThread(ActivateStarSkillPacket::handle)
                .add();
        CHANNEL.messageBuilder(OpenAstralGuidePacket.class, 4, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(OpenAstralGuidePacket::encode)
                .decoder(OpenAstralGuidePacket::decode)
                .consumerMainThread(OpenAstralGuidePacket::handle)
                .add();
    }

    public static void sendToPlayer(ServerPlayer player, Object message)
    {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), message);
    }

    public static void sendToServer(Object message)
    {
        CHANNEL.sendToServer(message);
    }
}
