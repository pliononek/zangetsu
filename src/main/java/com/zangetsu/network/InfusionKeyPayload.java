package com.zangetsu.network;

import com.zangetsu.ZangetsuMod;
import com.zangetsu.init.ModDataAttachments;
import com.zangetsu.init.ModItems;
import com.zangetsu.init.ReiatsuData;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record InfusionKeyPayload() implements CustomPacketPayload {
    public static final Type<InfusionKeyPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(ZangetsuMod.MODID, "infusion_key"));

    public static final StreamCodec<ByteBuf, InfusionKeyPayload> STREAM_CODEC = StreamCodec.unit(new InfusionKeyPayload());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(InfusionKeyPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                ItemStack mainHand = player.getMainHandItem();
                if (mainHand.is(ModItems.TENSA_ZANGETSU.get())) {
                    ReiatsuData data = player.getData(ModDataAttachments.REIATSU);
                    data.setInfusionActive(!data.isInfusionActive());
                    String msg = data.isInfusionActive() ? "§cGetsuga Infusion: ACTIVATED" : "§7Getsuga Infusion: DEACTIVATED";
                    player.displayClientMessage(Component.literal(msg), true);
                }
            }
        });
    }
}
