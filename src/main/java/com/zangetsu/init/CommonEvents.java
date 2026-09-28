package com.zangetsu.init;

import com.zangetsu.ZangetsuMod;
import com.zangetsu.entity.InnerZangetsuEntity;
import com.zangetsu.network.SyncReiatsuPayload;
import com.zangetsu.world.InnerWorldManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

public class CommonEvents {

    @EventBusSubscriber(modid = ZangetsuMod.MODID, bus = EventBusSubscriber.Bus.MOD)
    public static class ModBus {
        @SubscribeEvent
        public static void registerEntityAttributes(EntityAttributeCreationEvent event) {
            event.put(ModEntities.INNER_ZANGETSU.get(), InnerZangetsuEntity.createAttributes().build());
        }
    }

    @EventBusSubscriber(modid = ZangetsuMod.MODID)
    public static class GameBus {
        @SubscribeEvent
        public static void onPlayerTick(PlayerTickEvent.Post event) {
            if (event.getEntity() instanceof ServerPlayer player) {
                ServerLevel level = player.serverLevel();
                ReiatsuData data = player.getData(ModDataAttachments.REIATSU);

                // Auto-migrate max Reiatsu to 1000 if from older version
                data.checkAndMigrateMax();

                // Infusion handling: drain 4 Reiatsu/second & auto-shutoff at 0
                if (data.isInfusionActive()) {
                    if (player.tickCount % 20 == 0) {
                        if (!data.consume(4.0f)) {
                            data.setInfusionActive(false);
                            player.displayClientMessage(Component.literal("§c[Getsuga Infusion] Twoje Reiatsu wyczerpało się!"), true);
                            level.playSound(null, player.blockPosition(), ModSounds.REIATSU_BURST.get(), SoundSource.PLAYERS, 1.2f, 0.5f);
                        }
                    }
                } else {
                    // Passive regeneration when holding Shikai (+5 Reiatsu every second, ONLY when Infusion is OFF)
                    boolean isHoldingShikai = player.getMainHandItem().is(ModItems.ZANGETSU_SHIKAI.get());
                    if (isHoldingShikai && player.tickCount % 20 == 0) {
                        data.restore(5.0f);
                    }
                }

                // Domain Expansion Infinite Surge: 50 Reiatsu every tick!
                if (player.getPersistentData().getBoolean("ZangetsuInDomain")) {
                    data.restore(50.0f);
                }

                // Armor Set Bonuses
                if (com.zangetsu.item.ShihakushoArmorItem.isWearingFullShikaiSet(player)) {
                    if (player.tickCount % 20 == 0) {
                        if (!data.isInfusionActive()) {
                            data.restore(2.0f);
                        }
                        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 40, 0, false, false, false));
                    }
                } else if (com.zangetsu.item.ShihakushoArmorItem.isWearingFullBankaiSet(player)) {
                    if (player.tickCount % 20 == 0) {
                        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 40, 1, false, false, false));
                        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 40, 1, false, false, false));
                        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 40, 1, false, false, false));
                    }
                    if (player.getDeltaMovement().horizontalDistanceSqr() > 0.005 && player.tickCount % 3 == 0) {
                        org.joml.Vector3f crimson = new org.joml.Vector3f(0.9f, 0.05f, 0.15f);
                        level.sendParticles(new net.minecraft.core.particles.DustParticleOptions(crimson, 1.2f),
                                player.getX(), player.getY() + 0.2, player.getZ(), 1, 0.1, 0.1, 0.1, 0.01);
                    }
                }

                // 3x3 White Carpet Jinzen Meditation Check
                handleJinzenMeditation(player, level);

                // Sync to client every 5 ticks
                if (player.tickCount % 5 == 0) {
                    boolean unlocked = data.isBankaiUnlocked() || player.getPersistentData().getBoolean("ZangetsuBankaiUnlocked");
                    PacketDistributor.sendToPlayer(player, new SyncReiatsuPayload(
                            data.getCurrent(),
                            data.getMax(),
                            data.isInfusionActive(),
                            player.getPersistentData().getBoolean("ZangetsuSlashVertical"),
                            unlocked
                    ));
                }
            }
        }

        private static void handleJinzenMeditation(ServerPlayer player, ServerLevel level) {
            ItemStack held = player.getMainHandItem();
            if (!held.is(ModItems.ASAUCHI.get()) && !held.is(ModItems.ZANGETSU_SHIKAI.get())) {
                held = player.getOffhandItem();
            }
            boolean isHoldingRitualBlade = held.is(ModItems.ASAUCHI.get()) || held.is(ModItems.ZANGETSU_SHIKAI.get());
            if (!isHoldingRitualBlade) {
                player.getPersistentData().remove("ZangetsuJinzenTicks");
                return;
            }

            if (!player.isCrouching()) {
                player.getPersistentData().remove("ZangetsuJinzenTicks");
                return;
            }

            // Check if standing on 3x3 white carpets anywhere
            BlockPos carpetCenter = findCarpetCenter(level, player.blockPosition());
            if (carpetCenter == null) {
                player.getPersistentData().remove("ZangetsuJinzenTicks");
                return;
            }

            // Valid Jinzen meditation in progress
            int ticks = player.getPersistentData().getInt("ZangetsuJinzenTicks") + 1;
            player.getPersistentData().putInt("ZangetsuJinzenTicks", ticks);

            // Screen dimming effect
            player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 40, 0, false, false, false));

            String bladeName = held.is(ModItems.ZANGETSU_SHIKAI.get()) ? "Zangetsu (Poskromienie Bankai)" : "Asauchi";
            int pct = Math.min(100, (ticks * 100) / 60);
            float remainingSec = Math.max(0.0f, (60 - ticks) / 20.0f);
            player.displayClientMessage(Component.literal("§8[Jinzen] §bMedytacja nad " + bladeName + "... §e" + pct + "% §7(" + String.format("%.1f", remainingSec) + "s)"), true);

            // Heartbeat audio every 20 ticks
            if (ticks % 20 == 1) {
                level.playSound(null, player.blockPosition(), ModSounds.JINZEN_HEARTBEAT.get(), SoundSource.PLAYERS, 2.0f, 1.0f);
            }

            // Spiral soul particles around 3x3 carpets
            double angle = ticks * 0.4;
            double radius = 1.6;
            double px = carpetCenter.getX() + 0.5 + Math.cos(angle) * radius;
            double pz = carpetCenter.getZ() + 0.5 + Math.sin(angle) * radius;
            level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, px, carpetCenter.getY() + 0.2, pz, 2, 0, 0.05, 0, 0.02);
            level.sendParticles(ParticleTypes.SMOKE, px, carpetCenter.getY() + 0.1, pz, 1, 0, 0.02, 0, 0.01);

            // Complete meditation after 3 seconds (60 ticks)
            if (ticks >= 60) {
                player.getPersistentData().remove("ZangetsuJinzenTicks");
                InnerWorldManager.enterInnerWorld(player);
            }
        }

        private static BlockPos findCarpetCenter(ServerLevel level, BlockPos pos) {
            for (BlockPos testLevel : new BlockPos[]{pos, pos.below(), pos.above()}) {
                for (int ox = -1; ox <= 1; ox++) {
                    for (int oz = -1; oz <= 1; oz++) {
                        BlockPos candidate = testLevel.offset(ox, 0, oz);
                        if (is3x3WhiteCarpet(level, candidate)) {
                            return candidate;
                        }
                    }
                }
            }
            return null;
        }

        private static boolean is3x3WhiteCarpet(ServerLevel level, BlockPos center) {
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if (!level.getBlockState(center.offset(dx, 0, dz)).is(Blocks.WHITE_CARPET)) {
                        return false;
                    }
                }
            }
            return true;
        }

        @SubscribeEvent
        public static void onPlayerDeath(LivingDeathEvent event) {
            if (event.getEntity() instanceof ServerPlayer player) {
                if (player.level().dimension() == InnerWorldManager.INNER_WORLD_KEY) {
                    // Safe death in Inner World - preserve items and return to carpets
                    event.setCanceled(true);
                    InnerWorldManager.returnFromInnerWorld(player, false);
                } else if (player.getPersistentData().contains("ZangetsuDomainId")) {
                    int id = (int) player.getPersistentData().getLong("ZangetsuDomainId");
                    Entity e = player.serverLevel().getEntity(id);
                    if (e instanceof com.zangetsu.entity.DomainExpansionEntity domain) {
                        domain.shatter();
                    }
                }
            }
        }

        @SubscribeEvent
        public static void onExplosionDetonate(net.neoforged.neoforge.event.level.ExplosionEvent.Detonate event) {
            net.minecraft.world.level.Level level = event.getLevel();
            if (!level.isClientSide()) {
                event.getAffectedBlocks().removeIf(pos -> com.zangetsu.entity.DomainExpansionEntity.isProtectedFromDestruction(level, pos));
            }
        }
    }
}
