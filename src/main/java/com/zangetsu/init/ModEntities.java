package com.zangetsu.init;

import com.zangetsu.ZangetsuMod;
import com.zangetsu.entity.GetsugaTenshoEntity;
import com.zangetsu.entity.InnerZangetsuEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(Registries.ENTITY_TYPE, ZangetsuMod.MODID);

    public static final DeferredHolder<EntityType<?>, EntityType<GetsugaTenshoEntity>> GETSUGA_TENSHO =
            ENTITIES.register("getsuga_tensho", () -> EntityType.Builder.<GetsugaTenshoEntity>of(GetsugaTenshoEntity::new, MobCategory.MISC)
                    .sized(2.5f, 0.6f)
                    .clientTrackingRange(64)
                    .updateInterval(1)
                    .build(ResourceLocation.fromNamespaceAndPath(ZangetsuMod.MODID, "getsuga_tensho").toString()));

    public static final DeferredHolder<EntityType<?>, EntityType<InnerZangetsuEntity>> INNER_ZANGETSU =
            ENTITIES.register("inner_zangetsu", () -> EntityType.Builder.<InnerZangetsuEntity>of(InnerZangetsuEntity::new, MobCategory.MONSTER)
                    .sized(0.6f, 1.95f)
                    .clientTrackingRange(64)
                    .updateInterval(1)
                    .build(ResourceLocation.fromNamespaceAndPath(ZangetsuMod.MODID, "inner_zangetsu").toString()));

    public static final DeferredHolder<EntityType<?>, EntityType<com.zangetsu.entity.DomainExpansionEntity>> DOMAIN_EXPANSION =
            ENTITIES.register("domain_expansion", () -> EntityType.Builder.<com.zangetsu.entity.DomainExpansionEntity>of(com.zangetsu.entity.DomainExpansionEntity::new, MobCategory.MISC)
                    .sized(1.0f, 1.0f)
                    .clientTrackingRange(256)
                    .updateInterval(1)
                    .build(ResourceLocation.fromNamespaceAndPath(ZangetsuMod.MODID, "domain_expansion").toString()));
}
