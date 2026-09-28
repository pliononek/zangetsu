package com.zangetsu.network;

import com.zangetsu.ZangetsuMod;
import com.zangetsu.init.ModDataAttachments;
import com.zangetsu.init.ModItems;
import com.zangetsu.init.ReiatsuData;
import com.zangetsu.item.TensaZangetsuItem;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record ShunpoKeyPayload() implements CustomPacketPayload {
    public static final Type<ShunpoKeyPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(ZangetsuMod.MODID, "shunpo_key"));

    public static final StreamCodec<ByteBuf, ShunpoKeyPayload> STREAM_CODEC = StreamCodec.unit(new ShunpoKeyPayload());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ShunpoKeyPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                ItemStack mainHand = player.getMainHandItem();
                if (mainHand.is(ModItems.TENSA_ZANGETSU.get())) {
                    ReiatsuData data = player.getData(ModDataAttachments.REIATSU);
                    float cost = com.zangetsu.item.ShihakushoArmorItem.isWearingFullBankaiSet(player) ? 5.0f : 10.0f;
                    if (data.consume(cost)) {
                        TensaZangetsuItem.performShunpo(player);
                    } else {
                        player.displayClientMessage(Component.literal("§cNot enough Reiatsu for Shunpo! (Requires " + (int)cost + ")"), true);
                    }
                }
            }
        });
    }
}
