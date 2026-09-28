package com.zangetsu.entity;

import com.zangetsu.init.ModBlocks;
import com.zangetsu.init.ModEntities;
import com.zangetsu.init.ModGameRules;
import com.zangetsu.init.ModSounds;
import com.zangetsu.network.CameraShakePayload;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.neoforged.neoforge.network.PacketDistributor;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class GetsugaTenshoEntity extends Projectile {
    private static final EntityDataAccessor<Boolean> IS_BANKAI =
            SynchedEntityData.defineId(GetsugaTenshoEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Float> CHARGE =
            SynchedEntityData.defineId(GetsugaTenshoEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Boolean> IS_VERTICAL =
            SynchedEntityData.defineId(GetsugaTenshoEntity.class, EntityDataSerializers.BOOLEAN);

    private final Set<Integer> piercedEntities = new HashSet<>();
    private int life = 0;
    private static final int MAX_LIFE = 80;
    private boolean isBossGetsuga = false;

    public GetsugaTenshoEntity(EntityType<? extends Projectile> entityType, Level level) {
        super(entityType, level);
        this.noPhysics = true;
    }

    public GetsugaTenshoEntity(Level level, LivingEntity owner, boolean isBankai, float charge, boolean isVertical) {
        super(ModEntities.GETSUGA_TENSHO.get(), level);
        this.setOwner(owner);
        this.setBankai(isBankai);
        this.setCharge(charge);
        this.setVertical(isVertical);
        this.isBossGetsuga = (owner instanceof InnerZangetsuEntity);

        Vec3 eyePos = owner.getEyePosition();
        Vec3 look = owner.getLookAngle();
        this.setPos(eyePos.x + look.x * 1.5, eyePos.y + look.y * 1.5, eyePos.z + look.z * 1.5);
        
        float speed = isBankai ? (2.2f + charge * 0.8f) : (1.8f + charge * 0.5f);
        this.setDeltaMovement(look.scale(speed));
        this.setYRot(owner.getYRot());
        this.setXRot(owner.getXRot());
    }

    public GetsugaTenshoEntity(Level level, LivingEntity owner, boolean isBankai, float charge) {
        this(level, owner, isBankai, charge, owner.getPersistentData().getBoolean("ZangetsuSlashVertical"));
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(IS_BANKAI, false);
        builder.define(CHARGE, 0.0f);
        builder.define(IS_VERTICAL, false);
    }

    public boolean isBankai() {
        return this.entityData.get(IS_BANKAI);
    }

    public void setBankai(boolean bankai) {
        this.entityData.set(IS_BANKAI, bankai);
    }

    public float getCharge() {
        return this.entityData.get(CHARGE);
    }

    public void setCharge(float charge) {
        this.entityData.set(CHARGE, charge);
    }

    public boolean isVertical() {
        return this.entityData.get(IS_VERTICAL);
    }

    public void setVertical(boolean vertical) {
        this.entityData.set(IS_VERTICAL, vertical);
    }

    public boolean isBossGetsuga() {
        return this.isBossGetsuga || (this.getOwner() instanceof InnerZangetsuEntity);
    }

    public void setBossGetsuga(boolean bossGetsuga) {
        this.isBossGetsuga = bossGetsuga;
    }

    @Override
    public void tick() {
        super.tick();
        this.life++;

        Vec3 movement = this.getDeltaMovement();
        this.setPos(this.getX() + movement.x, this.getY() + movement.y, this.getZ() + movement.z);

        if (this.level().isClientSide) {
            spawnParticles();
        } else {
            handleServerCollision();

            if (this.life >= MAX_LIFE) {
                if (this.isBankai() && this.getCharge() > 0.5f) {
                    detonate();
                }
                this.discard();
            }
        }
    }

    private void spawnParticles() {
        boolean bankai = isBankai();
        float charge = getCharge();
        boolean vertical = isVertical();
        Vec3 motion = this.getDeltaMovement();
        double speed = motion.length();
        if (speed < 0.01) return;

        Vec3 forward = motion.normalize();
        Vec3 right;
        if (Math.abs(forward.y) > 0.95) {
            right = new Vec3(1, 0, 0);
        } else {
            right = new Vec3(-forward.z, 0, forward.x).normalize();
        }
        Vec3 up = right.cross(forward).normalize();

        // Orient perpendicular arc: along Y for vertical slash, along X/Z for horizontal slash
        Vec3 perp = vertical ? up : right;

        // Scale particles to span 10 to 20 blocks wide
        float radius = (charge >= 0.8f) ? 10.0f : (bankai ? 7.0f : 5.0f);
        int count = (charge >= 0.8f) ? 26 : 14;

        for (int i = -count; i <= count; i++) {
            double offsetFactor = (double) i / (double) count;
            double forwardOffset = -(offsetFactor * offsetFactor) * (radius * 0.38);

            double px = this.getX() + perp.x * offsetFactor * radius + forward.x * forwardOffset;
            double py = this.getY() + perp.y * offsetFactor * radius + forward.y * forwardOffset;
            double pz = this.getZ() + perp.z * offsetFactor * radius + forward.z * forwardOffset;

            if (bankai) {
                Vector3f color = Math.abs(offsetFactor) > 0.55
                        ? new Vector3f(0.95f, 0.05f, 0.15f)
                        : new Vector3f(0.01f, 0.01f, 0.01f);
                this.level().addParticle(new DustParticleOptions(color, (charge >= 0.8f) ? 2.5f : 1.8f), px, py, pz, 0, 0, 0);

                if (Math.abs(offsetFactor) > 0.6) {
                    this.level().addParticle(ParticleTypes.SOUL_FIRE_FLAME, px, py, pz, 0, 0.02, 0);
                }
                this.level().addParticle(ParticleTypes.SMOKE, px, py, pz, -motion.x * 0.08, 0.02, -motion.z * 0.08);
            } else {
                Vector3f color = Math.abs(offsetFactor) > 0.6
                        ? new Vector3f(0.15f, 0.75f, 1.0f)
                        : new Vector3f(0.95f, 0.98f, 1.0f);
                this.level().addParticle(new DustParticleOptions(color, (charge >= 0.8f) ? 2.4f : 1.6f), px, py, pz, 0, 0, 0);
                this.level().addParticle(ParticleTypes.ELECTRIC_SPARK, px, py, pz, 0, 0.01, 0);
            }
        }

        if (this.life % (charge >= 0.8f ? 3 : 5) == 0) {
            this.level().addParticle(ParticleTypes.SONIC_BOOM, this.getX(), this.getY(), this.getZ(), 0, 0, 0);
        }
    }

    private void handleServerCollision() {
        boolean bankai = isBankai();
        float charge = getCharge();
        boolean vertical = isVertical();
        float baseDmg = bankai ? (80.0f + charge * 120.0f) : (40.0f + charge * 60.0f);

        float widthExtent = vertical ? 2.5f : ((charge >= 0.8f) ? 10.0f : (bankai ? 6.5f : 5.0f));
        float heightExtent = vertical ? ((charge >= 0.8f) ? 10.0f : (bankai ? 6.5f : 5.0f)) : 2.5f;
        AABB hitBox = this.getBoundingBox().inflate(widthExtent, heightExtent, widthExtent);
        List<LivingEntity> targets = this.level().getEntitiesOfClass(LivingEntity.class, hitBox, e -> e != this.getOwner() && !this.piercedEntities.contains(e.getId()));

        for (LivingEntity target : targets) {
            this.piercedEntities.add(target.getId());

            Entity owner = this.getOwner();
            DamageSource damageSource;
            float damageToDeal = baseDmg;
            double knockbackScale = (charge >= 0.8f) ? 2.2 : (1.2 + charge * 0.8);

            if (owner instanceof InnerZangetsuEntity boss) {
                // Boss trial projectile: mitigated by iron armor & blockable by shield
                damageSource = this.damageSources().thrown(this, boss);
                damageToDeal = 9.0f; // 4.5 hearts raw -> ~2.2 hearts in full iron armor
                knockbackScale = 0.5; // Controlled knockback
            } else if (owner instanceof LivingEntity livingOwner) {
                // Player OP Getsuga Tensho
                damageSource = this.damageSources().indirectMagic(this, livingOwner);
                if (livingOwner instanceof Player player && com.zangetsu.item.ShihakushoArmorItem.isWearingFullBankaiSet(player)) {
                    damageToDeal *= 1.3f;
                }
            } else {
                damageSource = this.damageSources().magic();
            }

            target.hurt(damageSource, damageToDeal);
            target.setDeltaMovement(this.getDeltaMovement().normalize().scale(knockbackScale).add(0, (charge >= 0.8f ? 0.6 : 0.25), 0));

            // Sound on direct impact
            this.level().playSound(null, target.blockPosition(), ModSounds.REIATSU_BURST.get(), SoundSource.PLAYERS, 1.0f, 1.2f);
        }

        if (!targets.isEmpty()) {
            CameraShakePayload shake = new CameraShakePayload((charge >= 0.8f) ? 28 : (bankai ? 22 : 14), (charge >= 0.8f) ? 3.4f : (bankai ? 2.4f : 1.4f));
            PacketDistributor.sendToPlayersTrackingEntity(this, shake);
            if (this.getOwner() instanceof ServerPlayer sp) {
                PacketDistributor.sendToPlayer(sp, shake);
            }
        }

        // Terrain carving if enabled (strictly disabled for boss Getsuga Tensho or if gamerule is false!)
        boolean allowGriefing = !isBossGetsuga() && ModGameRules.isGetsugaDestructionAllowed(this.level());
        if (allowGriefing && this.level() instanceof ServerLevel serverLevel) {
            carveTerrain(serverLevel);
        }
    }

    private void carveTerrain(ServerLevel serverLevel) {
        if (isBossGetsuga()) return;
        boolean bankai = isBankai();
        float charge = getCharge();
        boolean vertical = isVertical();

        Vec3 motion = this.getDeltaMovement();
        if (motion.lengthSqr() < 0.001) return;
        Vec3 forward = motion.normalize();
        Vec3 right;
        if (Math.abs(forward.y) > 0.95) {
            right = new Vec3(1, 0, 0);
        } else {
            right = new Vec3(-forward.z, 0, forward.x).normalize();
        }
        Vec3 up = right.cross(forward).normalize();

        Vec3 spanVector = vertical ? up : right;
        Vec3 thickVector = vertical ? right : up;

        // User requested: width 10 to 20 blocks, height 3 to 5 blocks
        int halfSpan = (charge >= 0.8f) ? 10 : (bankai ? 7 : 5); // 21 or 11-15 blocks wide!
        int halfThick = (charge >= 0.8f) ? 2 : 1; // 5 or 3 blocks tall!

        Vec3 currPos = this.position();
        Vec3 prevPos = new Vec3(this.xOld, this.yOld, this.zOld);
        if (prevPos.distanceToSqr(currPos) < 0.001) {
            prevPos = currPos.subtract(motion);
        }

        double travelDist = prevPos.distanceTo(currPos);
        int forwardSteps = Math.max(1, (int) Math.ceil(travelDist / 0.65));

        Set<BlockPos> blocksToBreak = new HashSet<>();

        // Continuous volume interpolation between previous and current position
        for (int step = 0; step <= forwardSteps; step++) {
            double f = (double) step / forwardSteps;
            Vec3 sliceCenter = prevPos.lerp(currPos, f);

            for (double s = -halfSpan; s <= halfSpan; s += 0.65) {
                double curveOffset = -((s * s) / (double) (halfSpan * halfSpan)) * (halfSpan * 0.35);

                for (double h = -halfThick; h <= halfThick; h += 0.65) {
                    Vec3 sampleVec = sliceCenter
                            .add(spanVector.scale(s))
                            .add(thickVector.scale(h))
                            .add(forward.scale(curveOffset));

                    blocksToBreak.add(BlockPos.containing(sampleVec));
                }
            }
        }

        for (BlockPos pos : blocksToBreak) {
            if (DomainExpansionEntity.isProtectedFromDestruction(serverLevel, pos)) {
                continue; // Protected domain floor, barrier, or interior!
            }
            BlockState state = serverLevel.getBlockState(pos);
            if (!state.isAir() && state.getBlock() != Blocks.BEDROCK && !state.is(ModBlocks.DOMAIN_BARRIER.get()) && state.getDestroySpeed(serverLevel, pos) >= 0) {
                if (charge >= 0.8f || bankai || state.getDestroySpeed(serverLevel, pos) <= 10.0f) {
                    serverLevel.destroyBlock(pos, false);
                }
            }
        }
    }

    private void detonate() {
        boolean inDomain = DomainExpansionEntity.isInsideAnyDomain(this.level(), this.position());
        boolean allowGriefing = !isBossGetsuga() && !inDomain && ModGameRules.isGetsugaDestructionAllowed(this.level());
        Level.ExplosionInteraction interaction = allowGriefing ? Level.ExplosionInteraction.MOB : Level.ExplosionInteraction.NONE;
        this.level().explode(this, this.getX(), this.getY(), this.getZ(), 3.0f + getCharge() * 4.0f, interaction);

        CameraShakePayload shake = new CameraShakePayload(28, 3.5f);
        PacketDistributor.sendToPlayersTrackingEntity(this, shake);
        if (this.getOwner() instanceof ServerPlayer sp) {
            PacketDistributor.sendToPlayer(sp, shake);
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        this.life = tag.getInt("Life");
        this.setBankai(tag.getBoolean("IsBankai"));
        this.setCharge(tag.getFloat("Charge"));
        this.setVertical(tag.getBoolean("IsVertical"));
        this.isBossGetsuga = tag.getBoolean("IsBossGetsuga");
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("Life", this.life);
        tag.putBoolean("IsBankai", this.isBankai());
        tag.putFloat("Charge", this.getCharge());
        tag.putBoolean("IsVertical", this.isVertical());
        tag.putBoolean("IsBossGetsuga", this.isBossGetsuga);
    }
}
