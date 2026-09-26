package com.zangetsu.network;

import com.zangetsu.ZangetsuMod;
import com.zangetsu.init.ModDataAttachments;
import com.zangetsu.init.ModItems;
import com.zangetsu.init.ModSounds;
import com.zangetsu.init.ReiatsuData;
import com.zangetsu.item.TensaZangetsuItem;
import com.zangetsu.item.ZangetsuShikaiItem;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.joml.Vector3f;

public record BankaiKeyPayload(int action, int chargeTicks) implements CustomPacketPayload {
    public static final Type<BankaiKeyPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(ZangetsuMod.MODID, "bankai_key"));

    public static final StreamCodec<ByteBuf, BankaiKeyPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, BankaiKeyPayload::action,
            ByteBufCodecs.VAR_INT, BankaiKeyPayload::chargeTicks,
            BankaiKeyPayload::new
    );

    public static final int ACTION_REVERT = 0;
    public static final int ACTION_START = 1;
    public static final int ACTION_TICK = 2;
    public static final int ACTION_CANCEL = 3;
    public static final int ACTION_COMPLETE = 4;

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(BankaiKeyPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                ItemStack mainHand = player.getMainHandItem();

                if (payload.action() == ACTION_REVERT) {
                    if (mainHand.is(ModItems.TENSA_ZANGETSU.get())) {
                        long cooldownUntil = player.getPersistentData().getLong("ZangetsuBankaiRevertCooldown");
                        if (player.level().getGameTime() < cooldownUntil) {
                            return; // 3-second cooldown active!
                        }
                        TensaZangetsuItem.revertToShikai(player, mainHand);
                    }
                    return;
                }

                if (!mainHand.is(ModItems.ZANGETSU_SHIKAI.get())) {
                    clearChargingState(player);
                    return;
                }

                ReiatsuData data = player.getData(ModDataAttachments.REIATSU);
                boolean unlocked = data.isBankaiUnlocked() || player.getPersistentData().getBoolean("ZangetsuBankaiUnlocked") || player.isCreative();
                if (!unlocked) {
                    clearChargingState(player);
                    player.displayClientMessage(Component.literal("§cYour soul has not conquered Bankai yet! Defeat your inner Hollow in Jinzen meditation to awaken it."), true);
                    return;
                } else if (!data.isBankaiUnlocked()) {
                    data.setBankaiUnlocked(true);
                }

                ServerLevel level = player.serverLevel();

                switch (payload.action()) {
                    case ACTION_START -> {
                        player.getPersistentData().putBoolean("ZangetsuBankaiCharging", true);
                        player.getPersistentData().putInt("ZangetsuBankaiChargeTicks", 0);

                        // Freeze player firmly in place on ground
                        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 70, 255, false, false, false));
                        player.setDeltaMovement(0, player.getDeltaMovement().y < 0 ? player.getDeltaMovement().y * 0.5 : 0, 0);

                        // Initial deep Reiatsu hum
                        level.playSound(null, player.blockPosition(), ModSounds.REIATSU_BURST.get(), SoundSource.PLAYERS, 1.8f, 0.6f);
                    }
                    case ACTION_TICK -> {
                        int ticks = payload.chargeTicks();
                        player.getPersistentData().putBoolean("ZangetsuBankaiCharging", true);
                        player.getPersistentData().putInt("ZangetsuBankaiChargeTicks", ticks);

                        // Keep player planted firmly on ground
                        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 255, false, false, false));
                        player.setDeltaMovement(0, player.getDeltaMovement().y < 0 ? player.getDeltaMovement().y * 0.5 : 0, 0);

                        // HUD progress display
                        int pct = Math.min(100, (ticks * 100) / 50);
                        float rem = Math.max(0.0f, (50 - ticks) / 20.0f);
                        player.displayClientMessage(Component.literal("§4§l[BANKAI] §cKoncentracja Reiatsu... §e" + pct + "% §7(" + String.format("%.1f", rem) + "s) §8[Trzymaj B]"), true);

                        // Swirling inward Reiatsu vortex from 7 blocks into the outstretched blade
                        spawnInwardReiatsuVortex(level, player, ticks);

                        // Audio beats
                        if (ticks == 20) {
                            level.playSound(null, player.blockPosition(), ModSounds.JINZEN_HEARTBEAT.get(), SoundSource.PLAYERS, 2.5f, 1.1f);
                        } else if (ticks == 38) {
                            level.playSound(null, player.blockPosition(), ModSounds.REIATSU_BURST.get(), SoundSource.PLAYERS, 2.2f, 1.3f);
                        }

                        // Escalating camera shake as power surges
                        if (ticks % 4 == 0) {
                            float intensity = 0.6f + (ticks / 50.0f) * 2.6f;
                            CameraShakePayload shake = new CameraShakePayload(6, intensity);
                            PacketDistributor.sendToPlayer(player, shake);
                        }
                    }
                    case ACTION_CANCEL -> {
                        clearChargingState(player);
                        player.displayClientMessage(Component.literal("§7Transformacja Bankai przerwana."), true);
                    }
                    case ACTION_COMPLETE -> {
                        clearChargingState(player);
                        ZangetsuShikaiItem.activateBankai(player, mainHand);
                    }
                }
            }
        });
    }

    private static void clearChargingState(ServerPlayer player) {
        player.getPersistentData().remove("ZangetsuBankaiCharging");
        player.getPersistentData().remove("ZangetsuBankaiChargeTicks");
        player.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
        player.removeEffect(MobEffects.DARKNESS);
    }

    private static void spawnInwardReiatsuVortex(ServerLevel level, ServerPlayer player, int ticks) {
        Vector3f crimson = new Vector3f(0.95f, 0.05f, 0.15f);
        Vector3f black = new Vector3f(0.01f, 0.01f, 0.01f);

        Vec3 look = player.getLookAngle();
        Vec3 handTarget = player.getEyePosition().add(look.scale(1.3));

        // Swirl particles inward from radius 4.0 - 7.0 blocks towards outstretched hand
        for (int i = 0; i < 6; i++) {
            double r = 4.0 + level.random.nextDouble() * 3.5;
            double angle = (ticks * 0.25) + level.random.nextDouble() * Math.PI * 2.0;
            double startX = player.getX() + Math.cos(angle) * r;
            double startY = player.getY() + 0.2 + level.random.nextDouble() * 2.8;
            double startZ = player.getZ() + Math.sin(angle) * r;

            double vx = (handTarget.x - startX) * 0.22;
            double vy = (handTarget.y - startY) * 0.22;
            double vz = (handTarget.z - startZ) * 0.22;

            if (i % 2 == 0) {
                level.sendParticles(new DustParticleOptions(crimson, 2.2f), startX, startY, startZ, 0, vx, vy, vz, 1.0);
            } else {
                level.sendParticles(new DustParticleOptions(black, 2.5f), startX, startY, startZ, 0, vx, vy, vz, 1.0);
            }

            if (level.random.nextFloat() < 0.3f) {
                level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, startX, startY, startZ, 0, vx, vy, vz, 0.8);
            }
        }

        // Circular ground seal of spiritual pressure beneath feet
        double groundAngle = ticks * 0.3;
        for (int a = 0; a < 3; a++) {
            double theta = groundAngle + a * (2.0 * Math.PI / 3.0);
            double gx = player.getX() + Math.cos(theta) * 1.8;
            double gz = player.getZ() + Math.sin(theta) * 1.8;
            level.sendParticles(ParticleTypes.SMOKE, gx, player.getY() + 0.1, gz, 1, 0, 0.03, 0, 0.02);
            level.sendParticles(new DustParticleOptions(crimson, 1.5f), gx, player.getY() + 0.05, gz, 1, 0, 0.01, 0, 0.01);
        }
    }
}
