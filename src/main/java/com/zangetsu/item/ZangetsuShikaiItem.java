package com.zangetsu.item;

import com.zangetsu.entity.GetsugaTenshoEntity;
import com.zangetsu.init.ModDataAttachments;
import com.zangetsu.init.ModItems;
import com.zangetsu.init.ModSounds;
import com.zangetsu.init.ReiatsuData;
import com.zangetsu.network.CameraShakePayload;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import org.joml.Vector3f;

import java.util.function.Consumer;

public class ZangetsuShikaiItem extends SwordItem {
    public ZangetsuShikaiItem() {
        super(Tiers.NETHERITE, new Properties()
                .attributes(SwordItem.createAttributes(Tiers.NETHERITE, 19, -2.2f))
                .component(net.minecraft.core.component.DataComponents.UNBREAKABLE, new net.minecraft.world.item.component.Unbreakable(true))
                .fireResistant());
    }

    @Override
    public boolean isDamageable(ItemStack stack) {
        return false;
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return false;
    }

    @Override
    public boolean mineBlock(ItemStack stack, Level level, net.minecraft.world.level.block.state.BlockState state, net.minecraft.core.BlockPos pos, LivingEntity entity) {
        return true;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return 72000;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.BOW;
    }

    @Override
    public void initializeClient(Consumer<net.neoforged.neoforge.client.extensions.common.IClientItemExtensions> consumer) {
        consumer.accept(new net.neoforged.neoforge.client.extensions.common.IClientItemExtensions() {
            @Override
            public net.minecraft.client.model.HumanoidModel.ArmPose getArmPose(LivingEntity entity, InteractionHand hand, ItemStack itemStack) {
                if (entity.getPersistentData().getBoolean("ZangetsuBankaiCharging")) {
                    return net.minecraft.client.model.HumanoidModel.ArmPose.BOW_AND_ARROW;
                }
                return null;
            }

            @Override
            public boolean applyForgeHandTransform(com.mojang.blaze3d.vertex.PoseStack poseStack, net.minecraft.client.player.LocalPlayer player, net.minecraft.world.entity.HumanoidArm arm, ItemStack itemInHand, float partialTick, float equipProcess, float swingProcess) {
                if (player.getPersistentData().getBoolean("ZangetsuBankaiCharging")) {
                    // Outstretched hand holding Zangetsu forward in 1st person
                    poseStack.translate(-0.14, 0.06, -0.45);
                    poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(22.0f));
                    poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(-15.0f));
                    poseStack.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(-10.0f));
                    return true;
                }
                return false;
            }
        });
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (player.isCrouching()) {
            // Shift + Right-Click: Begin charging monumental Getsuga Tenshō
            ReiatsuData reiatsu = player.getData(ModDataAttachments.REIATSU);
            if (reiatsu.getCurrent() < 40.0f) {
                if (!level.isClientSide) {
                    player.displayClientMessage(Component.literal("§cPotrzebujesz 40 Reiatsu na naładowane Getsuga Tenshō!"), true);
                }
                return InteractionResultHolder.fail(stack);
            }
            player.startUsingItem(hand);
            return InteractionResultHolder.consume(stack);
        } else {
            // Standard Right-Click: Fast slash
            if (!level.isClientSide) {
                ReiatsuData reiatsu = player.getData(ModDataAttachments.REIATSU);
                if (reiatsu.consume(15.0f)) {
                    boolean vertical = player.getPersistentData().getBoolean("ZangetsuSlashVertical");
                    GetsugaTenshoEntity getsuga = new GetsugaTenshoEntity(level, player, false, 0.0f, vertical);
                    level.addFreshEntity(getsuga);

                    level.playSound(null, player.blockPosition(), ModSounds.GETSUGA_TENSHO.get(), SoundSource.PLAYERS, 1.6f, 1.0f);
                    player.getCooldowns().addCooldown(this, 20); // 1 sec cooldown
                    return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
                } else {
                    player.displayClientMessage(Component.literal("§cZa mało Reiatsu! (Wymagane 15)"), true);
                    return InteractionResultHolder.fail(stack);
                }
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
        }
    }

    @Override
    public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int count) {
        if (entity instanceof Player player) {
            int duration = this.getUseDuration(stack, entity) - count;

            if (level.isClientSide) {
                Vec3 eye = player.getEyePosition();
                Vec3 look = player.getLookAngle();
                double px = eye.x + look.x * 1.5;
                double py = eye.y + look.y * 1.5;
                double pz = eye.z + look.z * 1.5;

                // Swirling blue/white soul particles gathering into the blade
                for (int i = 0; i < 2; i++) {
                    double ox = (level.random.nextDouble() - 0.5) * 1.2;
                    double oy = (level.random.nextDouble() - 0.5) * 1.2;
                    double oz = (level.random.nextDouble() - 0.5) * 1.2;
                    level.addParticle(new DustParticleOptions(new Vector3f(0.2f, 0.75f, 1.0f), 1.8f),
                            px + ox, py + oy, pz + oz, -ox * 0.15, -oy * 0.15, -oz * 0.15);
                }
            }

            if (duration == 20) {
                level.playSound(player, player.blockPosition(), ModSounds.REIATSU_BURST.get(), SoundSource.PLAYERS, 1.3f, 1.6f);
                player.displayClientMessage(Component.literal("§b§l[GETSUGA TENSHŌ NAŁADOWANE! PUŚĆ ABY WYSTRZELIĆ]"), true);
            }
        }
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity entity, int timeLeft) {
        if (entity instanceof Player player) {
            int duration = this.getUseDuration(stack, entity) - timeLeft;

            if (!level.isClientSide) {
                ReiatsuData reiatsu = player.getData(ModDataAttachments.REIATSU);
                boolean vertical = player.getPersistentData().getBoolean("ZangetsuSlashVertical");

                if (duration >= 20) {
                    // Fully charged monumental Getsuga Tenshō!
                    if (reiatsu.consume(40.0f)) {
                        GetsugaTenshoEntity getsuga = new GetsugaTenshoEntity(level, player, false, 1.0f, vertical);
                        level.addFreshEntity(getsuga);

                        level.playSound(null, player.blockPosition(), ModSounds.GETSUGA_TENSHO.get(), SoundSource.PLAYERS, 2.5f, 0.8f);
                        level.playSound(null, player.blockPosition(), ModSounds.REIATSU_BURST.get(), SoundSource.PLAYERS, 2.0f, 1.1f);

                        // Recoil camera shake
                        if (player instanceof ServerPlayer sp) {
                            PacketDistributor.sendToPlayer(sp, new CameraShakePayload(18, 2.6f));
                        }

                        // Slight physical recoil
                        Vec3 look = player.getLookAngle();
                        player.push(-look.x * 0.4, 0.1, -look.z * 0.4);

                        player.getCooldowns().addCooldown(this, 35);
                    } else {
                        player.displayClientMessage(Component.literal("§cZa mało Reiatsu! (Wymagane 40)"), true);
                    }
                } else if (duration >= 5) {
                    // Quick release: standard Getsuga
                    if (reiatsu.consume(15.0f)) {
                        GetsugaTenshoEntity getsuga = new GetsugaTenshoEntity(level, player, false, 0.0f, vertical);
                        level.addFreshEntity(getsuga);

                        level.playSound(null, player.blockPosition(), ModSounds.GETSUGA_TENSHO.get(), SoundSource.PLAYERS, 1.6f, 1.0f);
                        player.getCooldowns().addCooldown(this, 20);
                    } else {
                        player.displayClientMessage(Component.literal("§cZa mało Reiatsu! (Wymagane 15)"), true);
                    }
                }
            }
        }
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (attacker instanceof Player player) {
            ReiatsuData reiatsu = player.getData(ModDataAttachments.REIATSU);
            reiatsu.restore(10.0f);
        }
        return true;
    }

    public static void activateBankai(Player player, ItemStack shikaiStack) {
        Level level = player.level();
        if (!level.isClientSide) {
            ItemStack bankaiStack = new ItemStack(ModItems.TENSA_ZANGETSU.get());
            if (shikaiStack.has(net.minecraft.core.component.DataComponents.ENCHANTMENTS)) {
                bankaiStack.set(net.minecraft.core.component.DataComponents.ENCHANTMENTS,
                        shikaiStack.get(net.minecraft.core.component.DataComponents.ENCHANTMENTS));
            }
            player.setItemInHand(player.getUsedItemHand(), bankaiStack);

            // Audio: sub-bass Bankai drop & Reiatsu roar at max volume
            level.playSound(null, player.blockPosition(), ModSounds.BANKAI.get(), SoundSource.PLAYERS, 3.0f, 1.0f);
            level.playSound(null, player.blockPosition(), ModSounds.REIATSU_BURST.get(), SoundSource.PLAYERS, 3.0f, 0.7f);

            if (level instanceof ServerLevel serverLevel) {
                Vector3f crimson = new Vector3f(0.95f, 0.05f, 0.15f);
                Vector3f black = new Vector3f(0.01f, 0.01f, 0.01f);

                // 1. Soaring 40-block monumental pillar of black & crimson Reiatsu reaching into clouds!
                double maxHeight = 40.0;
                for (double y = 0; y < maxHeight; y += 0.3) {
                    double radius = 1.2 + (y / maxHeight) * 2.8;
                    double angle1 = (y * 0.8) + (player.tickCount * 0.15);
                    double angle2 = angle1 + Math.PI;

                    double px1 = player.getX() + Math.cos(angle1) * radius;
                    double pz1 = player.getZ() + Math.sin(angle1) * radius;
                    double px2 = player.getX() + Math.cos(angle2) * radius;
                    double pz2 = player.getZ() + Math.sin(angle2) * radius;

                    serverLevel.sendParticles(new DustParticleOptions(crimson, 2.8f), px1, player.getY() + y, pz1, 1, 0, 0.1, 0, 0.04);
                    serverLevel.sendParticles(new DustParticleOptions(black, 3.0f), px2, player.getY() + y, pz2, 1, 0, 0.1, 0, 0.04);

                    if (y < 12.0) {
                        serverLevel.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, px1, player.getY() + y, pz1, 1, 0, 0.08, 0, 0.02);
                        serverLevel.sendParticles(ParticleTypes.LARGE_SMOKE, px2, player.getY() + y, pz2, 1, 0, 0.05, 0, 0.02);
                    }
                }

                // 2. Sonic boom & concentric ground rings of spiritual fire (radius 2 to 12)
                serverLevel.sendParticles(ParticleTypes.SONIC_BOOM, player.getX(), player.getY() + 1.2, player.getZ(), 1, 0, 0, 0, 0);

                for (double r = 2.0; r <= 12.0; r += 2.0) {
                    int ringPoints = (int) (r * 6);
                    for (int p = 0; p < ringPoints; p++) {
                        double theta = (p * 2.0 * Math.PI) / ringPoints;
                        double rx = player.getX() + Math.cos(theta) * r;
                        double rz = player.getZ() + Math.sin(theta) * r;
                        serverLevel.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, rx, player.getY() + 0.15, rz, 1, 0, 0.05, 0, 0.02);
                        serverLevel.sendParticles(new DustParticleOptions(crimson, 1.8f), rx, player.getY() + 0.1, rz, 1, 0, 0.02, 0, 0.01);
                    }
                }

                // 3. Shockwave: Repel and damage all nearby entities within 12 blocks
                var nearby = level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(12.0), e -> e != player);
                for (LivingEntity e : nearby) {
                    double dx = e.getX() - player.getX();
                    double dz = e.getZ() - player.getZ();
                    double dist = Math.max(0.5, Math.sqrt(dx * dx + dz * dz));
                    e.push((dx / dist) * 2.2, 0.45, (dz / dist) * 2.2);
                    e.hurt(player.damageSources().playerAttack(player), 25.0f);
                }

                // 4. Monumental Camera Shake
                CameraShakePayload shake = new CameraShakePayload(40, 4.5f);
                PacketDistributor.sendToPlayersTrackingEntity(player, shake);
                if (player instanceof ServerPlayer sp) {
                    PacketDistributor.sendToPlayer(sp, shake);
                }
            }

            // Stat boosts
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 600, 3, false, false, true));
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 600, 2, false, false, true));
            player.addEffect(new MobEffectInstance(MobEffects.JUMP, 600, 1, false, false, true));

            // 3-second cooldown on reverting back to Shikai
            player.getPersistentData().putLong("ZangetsuBankaiRevertCooldown", level.getGameTime() + 60);
            player.getCooldowns().addCooldown(ModItems.TENSA_ZANGETSU.get(), 60);
        }
    }
}
