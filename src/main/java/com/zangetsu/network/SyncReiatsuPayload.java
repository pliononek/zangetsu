package com.zangetsu.network;

import com.zangetsu.ZangetsuMod;
import com.zangetsu.init.ModDataAttachments;
import com.zangetsu.init.ReiatsuData;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record SyncReiatsuPayload(float current, float max, boolean infusion, boolean slashVertical, boolean bankaiUnlocked) implements CustomPacketPayload {
    public static final Type<SyncReiatsuPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(ZangetsuMod.MODID, "sync_reiatsu"));

    public static final StreamCodec<ByteBuf, SyncReiatsuPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.FLOAT, SyncReiatsuPayload::current,
            ByteBufCodecs.FLOAT, SyncReiatsuPayload::max,
            ByteBufCodecs.BOOL, SyncReiatsuPayload::infusion,
            ByteBufCodecs.BOOL, SyncReiatsuPayload::slashVertical,
            ByteBufCodecs.BOOL, SyncReiatsuPayload::bankaiUnlocked,
            SyncReiatsuPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SyncReiatsuPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            Player player = context.player();
            if (player != null) {
                ReiatsuData data = player.getData(ModDataAttachments.REIATSU);
                data.setCurrent(payload.current());
                data.setMax(payload.max());
                data.setInfusionActive(payload.infusion());
                data.setBankaiUnlocked(payload.bankaiUnlocked());
                player.getPersistentData().putBoolean("ZangetsuSlashVertical", payload.slashVertical());
                player.getPersistentData().putBoolean("ZangetsuBankaiUnlocked", payload.bankaiUnlocked());
            }
        });
    }
}
