package com.zangetsu.init;

import com.zangetsu.ZangetsuMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, ZangetsuMod.MODID);

    public static final DeferredHolder<SoundEvent, SoundEvent> BANKAI = SOUNDS.register(
            "bankai",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(ZangetsuMod.MODID, "bankai"))
    );

    public static final DeferredHolder<SoundEvent, SoundEvent> GETSUGA_TENSHO = SOUNDS.register(
            "getsuga_tensho",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(ZangetsuMod.MODID, "getsuga_tensho"))
    );

    public static final DeferredHolder<SoundEvent, SoundEvent> SHUNPO = SOUNDS.register(
            "shunpo",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(ZangetsuMod.MODID, "shunpo"))
    );

    public static final DeferredHolder<SoundEvent, SoundEvent> REIATSU_BURST = SOUNDS.register(
            "reiatsu_burst",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(ZangetsuMod.MODID, "reiatsu_burst"))
    );

    public static final DeferredHolder<SoundEvent, SoundEvent> JINZEN_HEARTBEAT = SOUNDS.register(
            "jinzen_heartbeat",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(ZangetsuMod.MODID, "jinzen_heartbeat"))
    );

    public static final DeferredHolder<SoundEvent, SoundEvent> INNER_LAUGH = SOUNDS.register(
            "inner_laugh",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(ZangetsuMod.MODID, "inner_laugh"))
    );

    public static final DeferredHolder<SoundEvent, SoundEvent> DOMAIN_EXPAND = SOUNDS.register(
            "domain_expand",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(ZangetsuMod.MODID, "domain_expand"))
    );

    public static final DeferredHolder<SoundEvent, SoundEvent> DOMAIN_SHATTER = SOUNDS.register(
            "domain_shatter",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(ZangetsuMod.MODID, "domain_shatter"))
    );

    public static final DeferredHolder<SoundEvent, SoundEvent> KUROI_TSUKI = SOUNDS.register(
            "kuroi_tsuki",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(ZangetsuMod.MODID, "kuroi_tsuki"))
    );

    public static final DeferredHolder<SoundEvent, SoundEvent> GRAN_REY_GETSUGA = SOUNDS.register(
            "gran_rey_getsuga",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(ZangetsuMod.MODID, "gran_rey_getsuga"))
    );
}
