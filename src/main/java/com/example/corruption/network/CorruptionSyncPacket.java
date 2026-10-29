package com.example.corruption.network;

import com.example.corruption.CorruptionMod;
import com.example.corruption.client.ClientCorruptionData;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * 服务端 → 客户端：同步本地玩家的腐化值。
 * 客户端的暗角粒子 / 心跳 / 镜头摇晃特效都依赖这个值，且只在客户端读取。
 *
 * NeoForge 21.1 的编解码完全由注册的 STREAM_CODEC 完成，CustomPacketPayload
 * 只需实现 type()；无需（也不应）重写 write/encode。
 */
public record CorruptionSyncPacket(int corruption) implements CustomPacketPayload {
    public static final Type<CorruptionSyncPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(CorruptionMod.MODID, "sync"));

    public static final StreamCodec<RegistryFriendlyByteBuf, CorruptionSyncPacket> STREAM_CODEC =
            StreamCodec.of(
                    (RegistryFriendlyByteBuf buf, CorruptionSyncPacket p) -> buf.writeVarInt(p.corruption()),
                    (RegistryFriendlyByteBuf buf) -> new CorruptionSyncPacket(buf.readVarInt()));

    @Override
    public Type<CorruptionSyncPacket> type() {
        return TYPE;
    }

    public static void handle(CorruptionSyncPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> ClientCorruptionData.setCorruption(packet.corruption()));
    }
}
