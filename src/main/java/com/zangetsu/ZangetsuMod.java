package com.zangetsu;

import com.mojang.logging.LogUtils;
import com.zangetsu.init.*;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

@Mod(ZangetsuMod.MODID)
public class ZangetsuMod {
    public static final String MODID = "zangetsu";
    public static final Logger LOGGER = LogUtils.getLogger();

    public ZangetsuMod(IEventBus modEventBus, ModContainer modContainer) {
        LOGGER.info("Initializing Tensa Zangetsu Mod (Bleach)...");

        ModItems.ITEMS.register(modEventBus);
        ModArmorMaterials.ARMOR_MATERIALS.register(modEventBus);
        ModEntities.ENTITIES.register(modEventBus);
        ModSounds.SOUNDS.register(modEventBus);
        ModCreativeTabs.CREATIVE_MODE_TABS.register(modEventBus);
        ModDataAttachments.ATTACHMENT_TYPES.register(modEventBus);

        ModGameRules.init();

        LOGGER.info("Tensa Zangetsu Mod loaded successfully!");
    }
}
