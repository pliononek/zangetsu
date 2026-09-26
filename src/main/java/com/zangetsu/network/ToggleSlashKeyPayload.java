package com.zangetsu.network;

import com.zangetsu.ZangetsuMod;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record ToggleSlashKeyPayload() implements CustomPacketPayload {
    public static final Type<ToggleSlashKeyPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(ZangetsuMod.MODID, "toggle_slash_key"));

    public static final StreamCodec<ByteBuf, ToggleSlashKeyPayload> STREAM_CODEC = StreamCodec.unit(new ToggleSlashKeyPayload());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ToggleSlashKeyPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                boolean current = player.getPersistentData().getBoolean("ZangetsuSlashVertical");
                boolean next = !current;
                player.getPersistentData().putBoolean("ZangetsuSlashVertical", next);

                player.level().playSound(null, player.blockPosition(), SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.PLAYERS, 0.8f, next ? 1.6f : 1.2f);
                player.displayClientMessage(Component.literal(next ? "§b§l[Getsuga: PIONOWE CIĘCIE]" : "§e§l[Getsuga: POZIOME CIĘCIE]"), true);
            }
        });
    }
}
