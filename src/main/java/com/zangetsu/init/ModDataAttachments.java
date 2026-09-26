package com.zangetsu.init;

import com.zangetsu.ZangetsuMod;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Supplier;

public class ModDataAttachments {
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, ZangetsuMod.MODID);

    public static final Supplier<AttachmentType<ReiatsuData>> REIATSU = ATTACHMENT_TYPES.register(
            "reiatsu",
            () -> AttachmentType.builder(ReiatsuData::new)
                    .serialize(ReiatsuData.CODEC)
                    .copyOnDeath()
                    .build()
    );
}
