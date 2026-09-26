package com.zangetsu.network;

import com.zangetsu.ZangetsuMod;
import com.zangetsu.client.CameraShakeHandler;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record CameraShakePayload(int duration, float intensity) implements CustomPacketPayload {
    public static final Type<CameraShakePayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(ZangetsuMod.MODID, "camera_shake"));

    public static final StreamCodec<ByteBuf, CameraShakePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.INT, CameraShakePayload::duration,
            ByteBufCodecs.FLOAT, CameraShakePayload::intensity,
            CameraShakePayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(CameraShakePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            CameraShakeHandler.addShake(payload.duration(), payload.intensity());
        });
    }
}
