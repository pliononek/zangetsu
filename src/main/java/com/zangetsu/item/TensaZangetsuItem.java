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
import net.neoforged.neoforge.network.PacketDistributor;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.List;

public class TensaZangetsuItem extends SwordItem {
    public TensaZangetsuItem() {
        super(Tiers.NETHERITE, new Properties()
                .attributes(SwordItem.createAttributes(Tiers.NETHERITE, 59, -1.0f))
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
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity entity, int timeLeft) {
        if (entity instanceof Player player) {
            int duration = this.getUseDuration(stack, entity) - timeLeft;
            float charge = Math.min(1.0f, (float) duration / 30.0f); // 1.5s for max charge

            if (!level.isClientSide) {
                ReiatsuData reiatsu = player.getData(ModDataAttachments.REIATSU);
                float cost = 20.0f + charge * 20.0f;

                if (reiatsu.consume(cost)) {
                    boolean vertical = player.getPersistentData().getBoolean("ZangetsuSlashVertical");
                    boolean infusion = reiatsu.isInfusionActive();
                    float effectiveCharge = infusion ? Math.min(1.5f, charge * 1.5f + 0.3f) : charge;
                    GetsugaTenshoEntity getsuga = new GetsugaTenshoEntity(level, player, true, effectiveCharge, vertical);
                    if (infusion) {
                        getsuga.setDeltaMovement(getsuga.getDeltaMovement().scale(1.35)); // 35% faster flight!
                    }
                    level.addFreshEntity(getsuga);

                    float pitch = 1.0f - charge * 0.2f;
                    level.playSound(null, player.blockPosition(), ModSounds.GETSUGA_TENSHO.get(), SoundSource.PLAYERS, 2.0f + charge * 0.5f, pitch);
                    if (charge >= 0.8f || infusion) {
                        level.playSound(null, player.blockPosition(), ModSounds.REIATSU_BURST.get(), SoundSource.PLAYERS, 2.2f, 0.9f);
                        if (player instanceof ServerPlayer sp) {
                            PacketDistributor.sendToPlayer(sp, new CameraShakePayload(22, 3.2f));
                        }
                    }

                    int cooldown = (int) (15 + charge * 25);
                    player.getCooldowns().addCooldown(this, cooldown);
                } else {
                    player.displayClientMessage(Component.literal("§cNot enough Reiatsu!"), true);
                }
            }
        }
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (attacker instanceof Player player) {
            ReiatsuData reiatsu = player.getData(ModDataAttachments.REIATSU);

            // Getsuga Infusion: +100% damage, massive melee explosion, zero Reiatsu restoration!
            if (reiatsu.isInfusionActive()) {
                Level level = player.level();
                target.hurt(level.damageSources().playerAttack(player), 60.0f); // Devastating fury strike!
                level.playSound(null, target.blockPosition(), ModSounds.REIATSU_BURST.get(), SoundSource.PLAYERS, 2.0f, 1.2f);

                if (level instanceof ServerLevel serverLevel) {
                    Vector3f crimson = new Vector3f(0.95f, 0.05f, 0.15f);
                    Vector3f black = new Vector3f(0.01f, 0.01f, 0.01f);
                    serverLevel.sendParticles(new DustParticleOptions(crimson, 2.5f),
                            target.getX(), target.getY() + 1.0, target.getZ(), 25, 0.4, 0.6, 0.4, 0.1);
                    serverLevel.sendParticles(new DustParticleOptions(black, 2.5f),
                            target.getX(), target.getY() + 1.0, target.getZ(), 15, 0.3, 0.5, 0.3, 0.08);
                    serverLevel.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                            target.getX(), target.getY() + 1.0, target.getZ(), 15, 0.3, 0.5, 0.3, 0.05);
                    serverLevel.sendParticles(ParticleTypes.SONIC_BOOM, target.getX(), target.getY() + 1.0, target.getZ(), 1, 0, 0, 0, 0);
                }
            } else {
                // Normal hit restores 15 Reiatsu
                reiatsu.restore(15.0f);
            }
        }
        return true;
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        super.inventoryTick(stack, level, entity, slotId, isSelected);

        if (isSelected && entity instanceof Player player) {
            ReiatsuData reiatsu = player.getData(ModDataAttachments.REIATSU);
            boolean infusion = reiatsu.isInfusionActive();

            // Passive combat buffs (supercharged during Infusion)
            int speedAmp = infusion ? 4 : 3; // Speed V during Infusion!
            int resAmp = infusion ? 3 : 2;
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 40, speedAmp, false, false, false));
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 40, resAmp, false, false, false));
            player.addEffect(new MobEffectInstance(MobEffects.JUMP, 40, 1, false, false, false));
            if (infusion) {
                player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 40, 2, false, false, false)); // Strength III!
            }
            player.fallDistance = 0.0f; // No fall damage

            if (!level.isClientSide) {
                // Bankai Self-Regeneration ONLY when Infusion is NOT active!
                if (!infusion && player.tickCount % 20 == 0) {
                    reiatsu.restore(10.0f);

                    // Passive Reiatsu Crush aura against nearby monsters
                    AABB auraBox = player.getBoundingBox().inflate(8.0);
                    List<LivingEntity> nearbyEnemies = level.getEntitiesOfClass(LivingEntity.class, auraBox,
                            e -> e instanceof Enemy && e.isAlive());
                    for (LivingEntity enemy : nearbyEnemies) {
                        enemy.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 1, false, false, false));
                        enemy.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 40, 1, false, false, false));
                        enemy.hurt(level.damageSources().magic(), 2.0f);
                    }
                }

                // Dense blazing Infusion aura swirling around player
                if (infusion && level instanceof ServerLevel serverLevel && player.tickCount % 2 == 0) {
                    Vector3f crimson = new Vector3f(0.95f, 0.05f, 0.15f);
                    Vector3f black = new Vector3f(0.01f, 0.01f, 0.01f);
                    double ox = (level.random.nextDouble() - 0.5) * 0.8;
                    double oy = level.random.nextDouble() * 1.8;
                    double oz = (level.random.nextDouble() - 0.5) * 0.8;
                    serverLevel.sendParticles(new DustParticleOptions(crimson, 1.8f),
                            player.getX() + ox, player.getY() + oy, player.getZ() + oz, 1, 0, 0.04, 0, 0.02);
                    serverLevel.sendParticles(new DustParticleOptions(black, 2.0f),
                            player.getX() + ox, player.getY() + oy, player.getZ() + oz, 1, 0, 0.04, 0, 0.02);
                    if (player.tickCount % 4 == 0) {
                        serverLevel.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                                player.getX() + ox, player.getY() + oy, player.getZ() + oz, 1, 0, 0.03, 0, 0.01);
                    }
                }
            }
        }
    }

    public static void performShunpo(Player player) {
        boolean isBankai = player.getMainHandItem().is(ModItems.TENSA_ZANGETSU.get());
        performShunpo(player, isBankai);
    }

    public static void performShunpo(Player player, boolean isBankai) {
        Level level = player.level();
        Vec3 look = player.getLookAngle();
        boolean inDomain = player.getPersistentData().getBoolean("ZangetsuInDomain");
        double maxDistance = inDomain ? 26.0 : (isBankai ? 14.0 : 12.0);

        Vec3 start = player.getEyePosition();
        Vec3 end = start.add(look.scale(maxDistance));

        // Fate Severance: In Domain, auto-teleport behind targeted enemy's back!
        if (inDomain) {
            net.minecraft.world.phys.AABB targetBox = player.getBoundingBox().expandTowards(look.scale(maxDistance)).inflate(3.0);
            java.util.List<LivingEntity> enemies = level.getEntitiesOfClass(LivingEntity.class, targetBox, e -> e != player && !e.isAlliedTo(player));
            LivingEntity bestTarget = null;
            double bestDot = 0.82;
            for (LivingEntity e : enemies) {
                Vec3 toE = e.getEyePosition().subtract(start).normalize();
                double dot = look.dot(toE);
                if (dot > bestDot) {
                    bestDot = dot;
                    bestTarget = e;
                }
            }
            if (bestTarget != null) {
                Vec3 enemyLook = bestTarget.getLookAngle();
                end = bestTarget.position().subtract(enemyLook.scale(1.4));
            }
        }

        BlockHitResult hit = level.clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        Vec3 dest = hit.getType() == HitResult.Type.MISS ? end : hit.getLocation().subtract(look.scale(0.5));

        // Sounds and particles at origin
        level.playSound(null, player.blockPosition(), ModSounds.SHUNPO.get(), SoundSource.PLAYERS, 1.8f, isBankai ? 1.1f : 1.25f);
        if (level instanceof ServerLevel serverLevel) {
            Vector3f originColor = isBankai ? new Vector3f(0.02f, 0.02f, 0.02f) : new Vector3f(0.15f, 0.65f, 0.95f);
            serverLevel.sendParticles(new DustParticleOptions(originColor, 2.0f),
                    player.getX(), player.getY() + 0.9, player.getZ(), 20, 0.3, 0.6, 0.3, 0.05);
            if (!isBankai) {
                serverLevel.sendParticles(ParticleTypes.CLOUD,
                        player.getX(), player.getY() + 0.2, player.getZ(), 6, 0.2, 0.1, 0.2, 0.02);
            }
        }

        player.teleportTo(dest.x, dest.y, dest.z);
        player.fallDistance = 0.0f;

        // Sounds and particles at destination
        level.playSound(null, player.blockPosition(), ModSounds.SHUNPO.get(), SoundSource.PLAYERS, 1.8f, isBankai ? 1.1f : 1.25f);
        if (level instanceof ServerLevel serverLevel) {
            Vector3f destColor = isBankai ? new Vector3f(0.85f, 0.05f, 0.15f) : new Vector3f(0.2f, 0.8f, 1.0f);
            serverLevel.sendParticles(new DustParticleOptions(destColor, 2.0f),
                    dest.x, dest.y + 0.9, dest.z, 20, 0.3, 0.6, 0.3, 0.05);

            if (player instanceof ServerPlayer sp) {
                PacketDistributor.sendToPlayer(sp, new CameraShakePayload(6, isBankai ? 1.2f : 0.8f));
            }
        }
    }

    public static void revertToShikai(Player player, ItemStack bankaiStack) {
        Level level = player.level();
        if (!level.isClientSide) {
            ItemStack shikaiStack = new ItemStack(ModItems.ZANGETSU_SHIKAI.get());
            if (bankaiStack.has(net.minecraft.core.component.DataComponents.ENCHANTMENTS)) {
                shikaiStack.set(net.minecraft.core.component.DataComponents.ENCHANTMENTS,
                        bankaiStack.get(net.minecraft.core.component.DataComponents.ENCHANTMENTS));
            }
            player.setItemInHand(player.getUsedItemHand(), shikaiStack);

            // Revert Bankai armor to Shikai
            ShihakushoArmorItem.revertToShikai(player);

            level.playSound(null, player.blockPosition(), ModSounds.REIATSU_BURST.get(), SoundSource.PLAYERS, 1.5f, 1.4f);
            player.displayClientMessage(Component.literal("§7Reverted to Zangetsu Shikai."), true);
        }
    }
}
