package com.zangetsu.entity;

import com.zangetsu.init.ModBlocks;
import com.zangetsu.init.ModEntities;
import com.zangetsu.init.ModSounds;
import com.zangetsu.network.CameraShakePayload;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import org.joml.Vector3f;

import java.util.*;

public class DomainExpansionEntity extends Entity {
    private static final EntityDataAccessor<Float> DATA_RADIUS =
            SynchedEntityData.defineId(DomainExpansionEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> DATA_LIFETIME =
            SynchedEntityData.defineId(DomainExpansionEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_FINISHER =
            SynchedEntityData.defineId(DomainExpansionEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Optional<UUID>> DATA_OWNER =
            SynchedEntityData.defineId(DomainExpansionEntity.class, EntityDataSerializers.OPTIONAL_UUID);

    public static final float DEFAULT_RADIUS = 25.0f; // 50 blocks diameter
    public static final int MAX_LIFETIME = 900; // 45 seconds

    private final Map<BlockPos, BlockState> capturedBlocks = new HashMap<>();
    private boolean barrierBuilt = false;

    public DomainExpansionEntity(EntityType<?> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    public DomainExpansionEntity(Level level, Vec3 center, Player owner) {
        this(ModEntities.DOMAIN_EXPANSION.get(), level);
        this.setPos(center.x, center.y, center.z);
        this.setOwnerUUID(owner.getUUID());
        this.setRadius(DEFAULT_RADIUS);
        this.setLifetime(0);
        this.setFinisherTicks(-1);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_RADIUS, DEFAULT_RADIUS);
        builder.define(DATA_LIFETIME, 0);
        builder.define(DATA_FINISHER, -1);
        builder.define(DATA_OWNER, Optional.empty());
    }

    public float getRadius() {
        return this.entityData.get(DATA_RADIUS);
    }

    public void setRadius(float radius) {
        this.entityData.set(DATA_RADIUS, radius);
    }

    public int getLifetime() {
        return this.entityData.get(DATA_LIFETIME);
    }

    public void setLifetime(int lifetime) {
        this.entityData.set(DATA_LIFETIME, lifetime);
    }

    public int getFinisherTicks() {
        return this.entityData.get(DATA_FINISHER);
    }

    public void setFinisherTicks(int ticks) {
        this.entityData.set(DATA_FINISHER, ticks);
    }

    public Optional<UUID> getOwnerUUID() {
        return this.entityData.get(DATA_OWNER);
    }

    public void setOwnerUUID(UUID uuid) {
        this.entityData.set(DATA_OWNER, Optional.ofNullable(uuid));
    }

    public boolean isFinisherActive() {
        return getFinisherTicks() >= 0;
    }

    public void startFinisher() {
        if (!isFinisherActive()) {
            setFinisherTicks(0);
            level().playSound(null, blockPosition(), ModSounds.REIATSU_BURST.get(), SoundSource.PLAYERS, 3.0f, 0.6f);
        }
    }

    public void buildBarrier(ServerLevel level) {
        if (barrierBuilt) return;
        barrierBuilt = true;

        BlockPos centerPos = blockPosition();
        float r = getRadius();
        double rMin = (r - 0.7) * (r - 0.7);
        double rMax = (r + 0.5) * (r + 0.5);
        int ir = (int) Math.ceil(r) + 1;

        BlockState barrierState = ModBlocks.DOMAIN_BARRIER.get().defaultBlockState();
        BlockPos.MutableBlockPos mpos = new BlockPos.MutableBlockPos();

        for (int x = -ir; x <= ir; x++) {
            for (int y = -ir; y <= ir; y++) {
                for (int z = -ir; z <= ir; z++) {
                    double d2 = x * x + y * y + z * z;
                    if (d2 >= rMin && d2 <= rMax) {
                        mpos.set(centerPos.getX() + x, centerPos.getY() + y, centerPos.getZ() + z);
                        BlockState old = level.getBlockState(mpos);
                        if (!old.is(Blocks.BEDROCK) && !old.is(ModBlocks.DOMAIN_BARRIER.get()) && !old.hasBlockEntity()) {
                            capturedBlocks.put(mpos.immutable(), old);
                            level.setBlock(mpos, barrierState, 2);
                        }
                    }
                }
            }
        }
    }

    public void restoreBarrier(ServerLevel level) {
        for (Map.Entry<BlockPos, BlockState> entry : capturedBlocks.entrySet()) {
            BlockPos pos = entry.getKey();
            if (level.getBlockState(pos).is(ModBlocks.DOMAIN_BARRIER.get())) {
                level.setBlock(pos, entry.getValue(), 2);
            }
        }
        capturedBlocks.clear();
        barrierBuilt = false;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 65536.0; // 256 blocks
    }

    @Override
    public AABB getBoundingBoxForCulling() {
        float r = getRadius() + 10.0f;
        return new AABB(getX() - r, getY() - r, getZ() - r, getX() + r, getY() + r, getZ() + r);
    }

    @Override
    public void tick() {
        super.tick();

        float radius = getRadius();
        Vec3 center = this.position();

        if (level().isClientSide) {
            tickClient(center, radius);
            return;
        }

        ServerLevel serverLevel = (ServerLevel) level();

        // Ensure physical block barrier is constructed
        if (!barrierBuilt && !capturedBlocks.isEmpty()) {
            barrierBuilt = true;
        } else if (!barrierBuilt) {
            buildBarrier(serverLevel);
        }

        int life = getLifetime() + 1;
        setLifetime(life);

        ServerPlayer owner = getOwnerPlayer(serverLevel);

        if (owner == null || !owner.isAlive() || life >= MAX_LIFETIME) {
            shatter();
            return;
        }

        double ownerDist = owner.position().distanceTo(center);
        boolean ownerInside = ownerDist <= radius;

        if (ownerInside) {
            owner.getPersistentData().putBoolean("ZangetsuInDomain", true);
            owner.getPersistentData().putLong("ZangetsuDomainId", this.getId());

            // Innate Buffs: Fate Severance (Złamanie Łańcucha)
            owner.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 40, 2, false, false, false));
            owner.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 40, 2, false, false, false));
            owner.addEffect(new MobEffectInstance(MobEffects.JUMP, 40, 1, false, false, false));
            owner.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 40, 2, false, false, false));
        } else {
            owner.getPersistentData().remove("ZangetsuInDomain");
        }

        // Handle Gran Rey Getsuga Finisher
        int finisher = getFinisherTicks();
        if (finisher >= 0) {
            handleFinisherTick(serverLevel, owner, finisher);
            return;
        }

        // Sure-Hit: Kuroi Tsuki (Omnidirectional slashes from suspended rain)
        if (life % 20 == 0) {
            executeSureHit(serverLevel, center, radius, owner);
        }

        // Levitating Rain Particles
        spawnSuspendedRain(serverLevel, center, radius, owner);
    }

    private void executeSureHit(ServerLevel level, Vec3 center, float radius, ServerPlayer owner) {
        AABB hitBox = new AABB(
                center.x - radius, center.y - radius, center.z - radius,
                center.x + radius, center.y + radius, center.z + radius
        );

        List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, hitBox, target -> {
            if (target == owner) return false;
            if (target.isAlliedTo(owner)) return false;
            if (target instanceof Player p && (p.isCreative() || p.isSpectator())) return false;
            return target.position().distanceTo(center) <= radius - 1.5;
        });

        Vector3f crimson = new Vector3f(0.85f, 0.05f, 0.15f);
        Vector3f black = new Vector3f(0.01f, 0.01f, 0.01f);

        for (LivingEntity target : targets) {
            target.hurt(level.damageSources().playerAttack(owner), 18.0f);
            level.playSound(null, target.blockPosition(), ModSounds.KUROI_TSUKI.get(), SoundSource.PLAYERS, 1.4f, 1.1f + level.random.nextFloat() * 0.3f);

            Vec3 pos = target.position().add(0, target.getBbHeight() * 0.5, 0);
            for (int i = 0; i < 4; i++) {
                double angle = i * (Math.PI / 4.0);
                double dx = Math.cos(angle) * 1.0;
                double dz = Math.sin(angle) * 1.0;

                level.sendParticles(new DustParticleOptions(crimson, 2.0f),
                        pos.x - dx, pos.y, pos.z - dz, 0, dx * 0.35, 0, dz * 0.35, 1.0);
                level.sendParticles(new DustParticleOptions(black, 2.2f),
                        pos.x + dx, pos.y, pos.z + dz, 0, -dx * 0.35, 0, -dz * 0.35, 1.0);
            }

            if (target instanceof ServerPlayer sp) {
                PacketDistributor.sendToPlayer(sp, new CameraShakePayload(6, 1.4f));
            }
        }
    }

    private void spawnSuspendedRain(ServerLevel level, Vec3 center, float radius, ServerPlayer owner) {
        Vector3f crimson = new Vector3f(0.85f, 0.05f, 0.15f);
        Vector3f black = new Vector3f(0.02f, 0.02f, 0.02f);

        AABB insideBox = new AABB(
                center.x - radius, center.y - radius, center.z - radius,
                center.x + radius, center.y + radius, center.z + radius
        );

        List<Player> playersInside = level.getEntitiesOfClass(Player.class, insideBox, p -> p.position().distanceTo(center) <= radius);
        for (Player p : playersInside) {
            for (int i = 0; i < 5; i++) {
                double rx = p.getX() + (level.random.nextDouble() - 0.5) * 24.0;
                double ry = p.getY() + level.random.nextDouble() * 12.0 - 2.0;
                double rz = p.getZ() + (level.random.nextDouble() - 0.5) * 24.0;

                if (new Vec3(rx, ry, rz).distanceTo(center) <= radius - 1.5) {
                    if (level.random.nextBoolean()) {
                        level.sendParticles(new DustParticleOptions(crimson, 1.5f), rx, ry, rz, 1, 0, 0, 0, 0);
                    } else {
                        level.sendParticles(new DustParticleOptions(black, 1.7f), rx, ry, rz, 1, 0, 0, 0, 0);
                    }
                }
            }
        }
    }

    private void handleFinisherTick(ServerLevel level, ServerPlayer owner, int finisher) {
        finisher++;
        setFinisherTicks(finisher);

        Vector3f crimson = new Vector3f(0.95f, 0.05f, 0.15f);
        Vector3f black = new Vector3f(0.01f, 0.01f, 0.01f);
        Vec3 eye = owner.getEyePosition();
        Vec3 look = owner.getLookAngle();
        Vec3 bladeTip = eye.add(look.scale(1.8));

        for (int i = 0; i < 15; i++) {
            double angle = level.random.nextDouble() * Math.PI * 2.0;
            double r = 3.0 + level.random.nextDouble() * 10.0;
            double h = (level.random.nextDouble() - 0.5) * 6.0;
            Vec3 particlePos = bladeTip.add(Math.cos(angle) * r, h, Math.sin(angle) * r);

            Vec3 vel = bladeTip.subtract(particlePos).scale(0.28);
            level.sendParticles(new DustParticleOptions(crimson, 2.5f),
                    particlePos.x, particlePos.y, particlePos.z, 0, vel.x, vel.y, vel.z, 1.0);
            level.sendParticles(new DustParticleOptions(black, 2.8f),
                    particlePos.x, particlePos.y, particlePos.z, 0, vel.x, vel.y, vel.z, 1.0);
        }

        if (finisher % 5 == 0) {
            float intensity = 1.0f + (finisher / 40.0f) * 3.5f;
            PacketDistributor.sendToPlayer(owner, new CameraShakePayload(8, intensity));
        }

        if (finisher >= 40) {
            level.playSound(null, owner.blockPosition(), ModSounds.GRAN_REY_GETSUGA.get(), SoundSource.PLAYERS, 3.5f, 0.9f);
            level.playSound(null, owner.blockPosition(), ModSounds.GETSUGA_TENSHO.get(), SoundSource.PLAYERS, 3.0f, 0.7f);

            double beamLength = 60.0;
            for (double d = 2.0; d <= beamLength; d += 1.0) {
                Vec3 pt = eye.add(look.scale(d));
                level.sendParticles(ParticleTypes.SONIC_BOOM, pt.x, pt.y, pt.z, 1, 0, 0, 0, 0);
                level.sendParticles(new DustParticleOptions(crimson, 3.5f), pt.x, pt.y, pt.z, 6, 1.2, 1.2, 1.2, 0.1);
                level.sendParticles(new DustParticleOptions(black, 4.0f), pt.x, pt.y, pt.z, 6, 1.2, 1.2, 1.2, 0.1);
                level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, pt.x, pt.y, pt.z, 3, 0.8, 0.8, 0.8, 0.05);

                AABB sliceBox = new AABB(pt.x - 3.5, pt.y - 3.5, pt.z - 3.5, pt.x + 3.5, pt.y + 3.5, pt.z + 3.5);
                List<LivingEntity> hitList = level.getEntitiesOfClass(LivingEntity.class, sliceBox, e -> e != owner && !e.isAlliedTo(owner));
                for (LivingEntity target : hitList) {
                    target.hurt(level.damageSources().playerAttack(owner), 200.0f);
                    target.push(look.x * 2.2, 0.6, look.z * 2.2);
                }
            }

            for (Player p : level.getEntitiesOfClass(Player.class, getBoundingBox().inflate(getRadius() + 15.0))) {
                if (p instanceof ServerPlayer sp) {
                    PacketDistributor.sendToPlayer(sp, new CameraShakePayload(50, 4.5f));
                }
            }

            shatter();
        }
    }

    public void shatter() {
        if (!level().isClientSide) {
            ServerLevel serverLevel = (ServerLevel) level();
            restoreBarrier(serverLevel);

            ServerPlayer owner = getOwnerPlayer(serverLevel);
            if (owner != null) {
                owner.getPersistentData().remove("ZangetsuInDomain");
                owner.getPersistentData().remove("ZangetsuDomainId");
            }

            Vec3 center = position();
            float radius = getRadius();

            serverLevel.playSound(null, blockPosition(), ModSounds.DOMAIN_SHATTER.get(), SoundSource.PLAYERS, 4.0f, 0.75f);

            Vector3f crimson = new Vector3f(0.95f, 0.05f, 0.15f);
            Vector3f black = new Vector3f(0.01f, 0.01f, 0.01f);

            for (int i = 0; i < 90; i++) {
                double theta = serverLevel.random.nextDouble() * Math.PI * 2.0;
                double phi = (serverLevel.random.nextDouble() - 0.5) * Math.PI;
                double r = radius + (serverLevel.random.nextDouble() - 0.5) * 2.0;

                double sx = center.x + Math.cos(phi) * Math.cos(theta) * r;
                double sy = center.y + Math.sin(phi) * r;
                double sz = center.z + Math.cos(phi) * Math.sin(theta) * r;

                serverLevel.sendParticles(new DustParticleOptions(crimson, 3.0f), sx, sy, sz, 2, 0.4, 0.4, 0.4, 0.15);
                serverLevel.sendParticles(new DustParticleOptions(black, 3.0f), sx, sy, sz, 2, 0.4, 0.4, 0.4, 0.15);
            }

            for (Player p : serverLevel.getEntitiesOfClass(Player.class, getBoundingBox().inflate(radius + 15.0))) {
                if (p instanceof ServerPlayer sp) {
                    PacketDistributor.sendToPlayer(sp, new CameraShakePayload(25, 3.0f));
                }
            }
        }
        this.discard();
    }

    @Override
    public void remove(RemovalReason reason) {
        if (!level().isClientSide && reason.shouldDestroy()) {
            if (level() instanceof ServerLevel serverLevel) {
                restoreBarrier(serverLevel);
            }
        }
        super.remove(reason);
    }

    private void tickClient(Vec3 center, float radius) {
        Player clientPlayer = net.minecraft.client.Minecraft.getInstance().player;
        if (clientPlayer != null && clientPlayer.position().distanceTo(center) <= radius) {
            Vector3f crimson = new Vector3f(0.85f, 0.05f, 0.15f);
            Vector3f black = new Vector3f(0.01f, 0.01f, 0.01f);

            for (int i = 0; i < 4; i++) {
                double rx = clientPlayer.getX() + (level().random.nextDouble() - 0.5) * 22.0;
                double ry = clientPlayer.getY() + level().random.nextDouble() * 10.0 - 2.0;
                double rz = clientPlayer.getZ() + (level().random.nextDouble() - 0.5) * 22.0;

                if (level().random.nextBoolean()) {
                    level().addParticle(new DustParticleOptions(crimson, 1.5f), rx, ry, rz, 0, 0, 0);
                } else {
                    level().addParticle(new DustParticleOptions(black, 1.7f), rx, ry, rz, 0, 0, 0);
                }
            }
        }
    }

    private ServerPlayer getOwnerPlayer(ServerLevel level) {
        Optional<UUID> opt = getOwnerUUID();
        if (opt.isPresent()) {
            return level.getServer().getPlayerList().getPlayer(opt.get());
        }
        return null;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.hasUUID("Owner")) {
            setOwnerUUID(tag.getUUID("Owner"));
        }
        if (tag.contains("Radius")) {
            setRadius(tag.getFloat("Radius"));
        }
        if (tag.contains("Lifetime")) {
            setLifetime(tag.getInt("Lifetime"));
        }
        if (tag.contains("Finisher")) {
            setFinisherTicks(tag.getInt("Finisher"));
        }
        if (tag.contains("CapturedBlocks", Tag.TAG_LIST)) {
            ListTag list = tag.getList("CapturedBlocks", Tag.TAG_COMPOUND);
            HolderGetter<Block> blockGetter = level().holderLookup(Registries.BLOCK);
            for (int i = 0; i < list.size(); i++) {
                CompoundTag bTag = list.getCompound(i);
                BlockPos pos = BlockPos.of(bTag.getLong("Pos"));
                BlockState state = NbtUtils.readBlockState(blockGetter, bTag.getCompound("State"));
                capturedBlocks.put(pos, state);
            }
            if (!capturedBlocks.isEmpty()) {
                barrierBuilt = true;
            }
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        getOwnerUUID().ifPresent(uuid -> tag.putUUID("Owner", uuid));
        tag.putFloat("Radius", getRadius());
        tag.putInt("Lifetime", getLifetime());
        tag.putInt("Finisher", getFinisherTicks());

        if (!capturedBlocks.isEmpty()) {
            ListTag list = new ListTag();
            for (Map.Entry<BlockPos, BlockState> entry : capturedBlocks.entrySet()) {
                CompoundTag bTag = new CompoundTag();
                bTag.putLong("Pos", entry.getKey().asLong());
                bTag.put("State", NbtUtils.writeBlockState(entry.getValue()));
                list.add(bTag);
            }
            tag.put("CapturedBlocks", list);
        }
    }
}
