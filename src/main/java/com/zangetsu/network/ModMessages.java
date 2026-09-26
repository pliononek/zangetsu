package com.zangetsu.network;

import com.zangetsu.ZangetsuMod;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

@EventBusSubscriber(modid = ZangetsuMod.MODID, bus = EventBusSubscriber.Bus.MOD)
public class ModMessages {
    @SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");

        registrar.playToServer(
                BankaiKeyPayload.TYPE,
                BankaiKeyPayload.STREAM_CODEC,
                BankaiKeyPayload::handle
        );

        registrar.playToServer(
                ShunpoKeyPayload.TYPE,
                ShunpoKeyPayload.STREAM_CODEC,
                ShunpoKeyPayload::handle
        );

        registrar.playToServer(
                InfusionKeyPayload.TYPE,
                InfusionKeyPayload.STREAM_CODEC,
                InfusionKeyPayload::handle
        );

        registrar.playToServer(
                ToggleSlashKeyPayload.TYPE,
                ToggleSlashKeyPayload.STREAM_CODEC,
                ToggleSlashKeyPayload::handle
        );

        registrar.playToClient(
                SyncReiatsuPayload.TYPE,
                SyncReiatsuPayload.STREAM_CODEC,
                SyncReiatsuPayload::handle
        );

        registrar.playToClient(
                CameraShakePayload.TYPE,
                CameraShakePayload.STREAM_CODEC,
                CameraShakePayload::handle
        );
    }
}
