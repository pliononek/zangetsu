package com.zangetsu.entity;

import com.zangetsu.init.ModDataAttachments;
import com.zangetsu.init.ModItems;
import com.zangetsu.init.ModSounds;
import com.zangetsu.init.ReiatsuData;
import com.zangetsu.network.CameraShakePayload;
import com.zangetsu.network.SyncReiatsuPayload;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import org.joml.Vector3f;

public class InnerZangetsuEntity extends Monster {
    private final ServerBossEvent bossEvent = (ServerBossEvent) new ServerBossEvent(
            Component.translatable("entity.zangetsu.inner_zangetsu"),
            BossEvent.BossBarColor.WHITE,
            BossEvent.BossBarOverlay.PROGRESS
    ).setDarkenScreen(true);

    private boolean isBankaiTrial = false;
    private boolean isBankaiPhase = false;
    private boolean isInfusionPhase = false;

    private int shunpoCooldown = 140;
    private int getsugaCooldown = 160;
    private int getsugaWindup = 0;
    private int postShunpoStun = 0;
    private int bankaiTransformTicks = 0;
    private int antiPillarCooldown = 0;

    public InnerZangetsuEntity(EntityType<? extends Monster> entityType, Level level) {
        this(entityType, level, false);
    }

    public InnerZangetsuEntity(EntityType<? extends Monster> entityType, Level level, boolean isBankaiTrial) {
        super(entityType, level);
        this.setPersistenceRequired();
        this.setDropChance(EquipmentSlot.MAINHAND, 0.0f);
        if (isBankaiTrial) {
            setBankaiTrial(true);
        } else {
            this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(ModItems.HOLLOW_ZANGETSU.get()));
            this.xpReward = 100;
        }
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 300.0)
                .add(Attributes.MOVEMENT_SPEED, 0.26)
                .add(Attributes.ATTACK_DAMAGE, 6.0)
                .add(Attributes.FOLLOW_RANGE, 48.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.4)
                .add(Attributes.ARMOR, 4.0);
    }

    public void setBankaiTrial(boolean bankaiTrial) {
        this.isBankaiTrial = bankaiTrial;
        if (bankaiTrial) {
            this.isBankaiPhase = true;
            this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(ModItems.TENSA_ZANGETSU.get()));
            this.xpReward = 500;

            var maxHealth = this.getAttribute(Attributes.MAX_HEALTH);
            if (maxHealth != null) {
                maxHealth.setBaseValue(250.0); // Balanced from 450 to 250 HP
                this.setHealth(250.0f);
            }
            var speed = this.getAttribute(Attributes.MOVEMENT_SPEED);
            if (speed != null) {
                speed.setBaseValue(0.28); // Balanced from 0.35 to 0.28 (fair sprint-speed)
            }
            var damage = this.getAttribute(Attributes.ATTACK_DAMAGE);
            if (damage != null) {
                damage.setBaseValue(4.5); // Balanced from 10.0 to 4.5 (with sword deals ~2.5-3 hearts to prot 4)
            }
            var knockback = this.getAttribute(Attributes.KNOCKBACK_RESISTANCE);
            if (knockback != null) {
                knockback.setBaseValue(0.65); // Balanced from 0.95 to 0.65 (rewards player crits and combos)
            }
            var armor = this.getAttribute(Attributes.ARMOR);
            if (armor != null) {
                armor.setBaseValue(4.0); // Balanced from 10.0 to 4.0
            }

            this.bossEvent.setName(Component.literal("§4§lInner Zangetsu §c[Bankai Mastery]"));
            this.bossEvent.setColor(BossEvent.BossBarColor.RED);
            this.shunpoCooldown = 120;
            this.getsugaCooldown = 140;
        }
    }

    public boolean isBankaiTrial() {
        return isBankaiTrial;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        compound.putBoolean("IsBankaiTrial", this.isBankaiTrial);
        compound.putBoolean("IsBankaiPhase", this.isBankaiPhase);
        compound.putBoolean("IsInfusionPhase", this.isInfusionPhase);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        if (compound.getBoolean("IsBankaiTrial")) {
            setBankaiTrial(true);
        }
        this.isBankaiPhase = compound.getBoolean("IsBankaiPhase");
        this.isInfusionPhase = compound.getBoolean("IsInfusionPhase");
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new FloatGoal(this));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.25, true));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 1.0));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 12.0f));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        if (target instanceof Player player) {
            // Anti-Shield: Break shield only during furious Infusion Phase
            if (this.isInfusionPhase && player.isBlocking()) {
                player.disableShield();
                this.level().playSound(null, player.blockPosition(), SoundEvents.SHIELD_BREAK, SoundSource.HOSTILE, 2.0f, 0.8f);
                if (this.level() instanceof ServerLevel serverLevel) {
                    serverLevel.sendParticles(ParticleTypes.SONIC_BOOM, player.getX(), player.getY() + 1.0, player.getZ(), 1, 0, 0, 0, 0);
                    serverLevel.sendParticles(ParticleTypes.ITEM_SLIME, player.getX(), player.getY() + 1.0, player.getZ(), 20, 0.3, 0.3, 0.3, 0.1);
                }
                player.sendSystemMessage(Component.literal("§c§l[Zanpakutō] Twoja tarcza została roztrzaskana furią Getsuga Infusion!"));
            }
        }

        boolean hurt = super.doHurtTarget(target);
        if (hurt) {
            // Getsuga Infusion Phase balanced explosive bonus damage (+3.5 dmg)
            if (this.isInfusionPhase) {
                target.hurt(this.damageSources().mobAttack(this), 3.5f);
                this.level().playSound(null, target.blockPosition(), ModSounds.REIATSU_BURST.get(), SoundSource.HOSTILE, 1.2f, 1.2f);
                if (this.level() instanceof ServerLevel serverLevel) {
                    serverLevel.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, target.getX(), target.getY() + 1.0, target.getZ(), 10, 0.3, 0.3, 0.3, 0.05);
                }
            }
            this.setDeltaMovement(this.getDeltaMovement().multiply(0.6, 1.0, 0.6));
        }
        return hurt;
    }

    @Override
    public boolean hurt(DamageSource damageSource, float amount) {
        // Anti-Ranged: Deflect all arrows and projectiles in Bankai Trial (or Bankai Phase)
        if (damageSource.is(DamageTypeTags.IS_PROJECTILE)) {
            if (this.isBankaiTrial || this.isBankaiPhase) {
                if (this.level() instanceof ServerLevel serverLevel) {
                    serverLevel.sendParticles(ParticleTypes.CRIT, this.getX(), this.getY() + 1.2, this.getZ(), 15, 0.3, 0.3, 0.3, 0.2);
                    serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK, this.getX(), this.getY() + 1.2, this.getZ(), 10, 0.3, 0.3, 0.3, 0.15);
                    serverLevel.playSound(null, this.blockPosition(), SoundEvents.SHIELD_BLOCK, SoundSource.HOSTILE, 1.8f, 1.6f);

                    Entity shooter = damageSource.getEntity();
                    if (shooter instanceof LivingEntity livingShooter && livingShooter.isAlive()) {
                        performShunpo(livingShooter);
                    }
                }
                return false; // Complete immunity to arrows and projectiles!
            }
        }

        if (damageSource.getEntity() instanceof ServerPlayer player) {
            ItemStack weapon = player.getMainHandItem();
            if (weapon.is(ModItems.ASAUCHI.get())) {
                amount += 4.0f;
                if (this.level() instanceof ServerLevel serverLevel) {
                    serverLevel.sendParticles(ParticleTypes.ENCHANTED_HIT, this.getX(), this.getY() + 1.2, this.getZ(), 12, 0.3, 0.3, 0.3, 0.1);
                }
            }
        }
        return super.hurt(damageSource, amount);
    }

    public void cleanupAndDiscard() {
        this.bossEvent.removeAllPlayers();
        this.bossEvent.setVisible(false);
        this.discard();
    }

    @Override
    public void remove(RemovalReason reason) {
        super.remove(reason);
        this.bossEvent.removeAllPlayers();
        this.bossEvent.setVisible(false);
    }

    @Override
    public void onAddedToLevel() {
        super.onAddedToLevel();
        if (!this.level().isClientSide && this.level().dimension() == com.zangetsu.world.InnerWorldManager.INNER_WORLD_KEY) {
            if (this.level() instanceof ServerLevel serverLevel) {
                for (Entity entity : serverLevel.getAllEntities()) {
                    if (entity instanceof InnerZangetsuEntity other && other != this) {
                        other.cleanupAndDiscard();
                    }
                }
            }
        }
    }

    @Override
    public void aiStep() {
        super.aiStep();

        if (!this.level().isClientSide) {
            if (this.level().dimension() == com.zangetsu.world.InnerWorldManager.INNER_WORLD_KEY) {
                if (this.level().players().isEmpty()) {
                    cleanupAndDiscard();
                    return;
                }
            }

            this.bossEvent.setProgress(this.getHealth() / this.getMaxHealth());

            // Phase 2 check for regular Shikai trial boss (at 50% HP <= 150)
            if (!this.isBankaiTrial && !this.isBankaiPhase && this.getHealth() <= 150.0f) {
                triggerBankaiPhase();
                return;
            }

            // Phase 2 check for Bankai Trial boss: GETSUGA INFUSION at <= 50% HP (<= 125 HP)
            if (this.isBankaiTrial && !this.isInfusionPhase && this.getHealth() <= 125.0f) {
                triggerInfusionPhase();
                return;
            }

            // Transformation sequence pause
            if (this.bankaiTransformTicks > 0) {
                this.bankaiTransformTicks--;
                this.getNavigation().stop();
                if (this.level() instanceof ServerLevel serverLevel) {
                    serverLevel.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, this.getX(), this.getY() + 1.0, this.getZ(), 4, 0.3, 0.5, 0.3, 0.02);
                }
                return;
            }

            // Hollow Reiatsu aura
            if ((this.isBankaiPhase || this.isBankaiTrial) && this.level() instanceof ServerLevel serverLevel && this.tickCount % 2 == 0) {
                serverLevel.sendParticles(this.isInfusionPhase ? ParticleTypes.FLAME : ParticleTypes.SOUL_FIRE_FLAME,
                        this.getX(), this.getY() + 0.8, this.getZ(), 2, 0.2, 0.4, 0.2, 0.01);
                serverLevel.sendParticles(ParticleTypes.SMOKE, this.getX(), this.getY() + 0.5, this.getZ(), 1, 0.1, 0.3, 0.1, 0.01);
            }

            // Post-Shunpo pause (0 for Bankai trial so attacks are immediate, 12 ticks for Shikai boss)
            if (this.postShunpoStun > 0) {
                this.postShunpoStun--;
                this.getNavigation().stop();
                return;
            }

            LivingEntity target = this.getTarget();
            if (target != null && target.isAlive()) {
                double distSqr = this.distanceToSqr(target);
                double dy = target.getY() - this.getY();

                // Anti-Pillaring Cheese: Player builds up > 2.0 blocks high!
                if (dy > 2.0) {
                    if (--this.antiPillarCooldown <= 0) {
                        this.antiPillarCooldown = 30; // Check every 1.5s
                        performAntiPillar(target);
                        return;
                    }
                }

                // Handle Getsuga Tensho windup telegraph
                if (this.getsugaWindup > 0) {
                    this.getsugaWindup--;
                    this.getNavigation().stop();
                    this.lookAt(target, 30.0f, 30.0f);

                    if (this.level() instanceof ServerLevel serverLevel) {
                        serverLevel.sendParticles(this.isInfusionPhase ? ParticleTypes.FLAME : (this.isBankaiPhase ? ParticleTypes.SOUL_FIRE_FLAME : ParticleTypes.ELECTRIC_SPARK),
                                this.getX(), this.getY() + 1.2, this.getZ(), 5, 0.3, 0.5, 0.3, 0.02);
                        serverLevel.sendParticles(ParticleTypes.SMOKE, this.getX(), this.getY() + 0.8, this.getZ(), 3, 0.2, 0.4, 0.2, 0.01);
                    }

                    if (this.getsugaWindup == 0) {
                        fireGetsuga(target);
                        int baseGetsugaCd = this.isInfusionPhase ? 90 : (this.isBankaiTrial ? 130 : 160);
                        this.getsugaCooldown = baseGetsugaCd + this.random.nextInt(35);
                    }
                    return;
                }

                // Getsuga Tensho: trigger when player is spaced out (> 6 blocks away) or periodically
                if (--this.getsugaCooldown <= 0 && (distSqr > 36.0 || this.random.nextFloat() < 0.20f)) {
                    this.getsugaWindup = this.isBankaiTrial ? 16 : 20; // 0.8s for Bankai, 1.0s for Shikai
                    this.level().playSound(null, this.blockPosition(), ModSounds.INNER_LAUGH.get(), SoundSource.HOSTILE, 1.8f, 1.2f);
                }

                // Shunpo ability: rapid repositioning / flank
                int baseShunpoCd = this.isInfusionPhase ? 80 : (this.isBankaiTrial ? 110 : 140);
                if (--this.shunpoCooldown <= 0 && (distSqr > 36.0 || (this.isBankaiTrial && distSqr > 25.0))) {
                    performShunpo(target);
                    this.shunpoCooldown = baseShunpoCd + this.random.nextInt(30);
                }
            }
        }
    }

    private void triggerBankaiPhase() {
        this.isBankaiPhase = true;
        this.bankaiTransformTicks = 35;
        this.getNavigation().stop();

        this.level().playSound(null, this.blockPosition(), ModSounds.BANKAI.get(), SoundSource.HOSTILE, 2.5f, 0.9f);
        this.level().playSound(null, this.blockPosition(), ModSounds.INNER_LAUGH.get(), SoundSource.HOSTILE, 2.2f, 0.85f);

        this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(ModItems.TENSA_ZANGETSU.get()));

        for (ServerPlayer player : this.level().getEntitiesOfClass(ServerPlayer.class, this.getBoundingBox().inflate(60.0))) {
            player.sendSystemMessage(Component.literal("§f§lInner Zangetsu: §c§lBANKAI... TENSA ZANGETSU!"));
            PacketDistributor.sendToPlayer(player, new CameraShakePayload(32, 3.5f));
        }

        if (this.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.SONIC_BOOM, this.getX(), this.getY() + 1.2, this.getZ(), 1, 0, 0, 0, 0);

            Vector3f crimson = new Vector3f(0.95f, 0.05f, 0.15f);
            Vector3f black = new Vector3f(0.01f, 0.01f, 0.01f);
            for (double y = 0; y < 16.0; y += 0.5) {
                serverLevel.sendParticles(new DustParticleOptions(crimson, 2.2f), this.getX(), this.getY() + y, this.getZ(), 6, 0.4, 0.1, 0.4, 0.05);
                serverLevel.sendParticles(new DustParticleOptions(black, 2.5f), this.getX(), this.getY() + y, this.getZ(), 6, 0.4, 0.1, 0.4, 0.05);
                serverLevel.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, this.getX(), this.getY() + y, this.getZ(), 3, 0.3, 0.1, 0.3, 0.02);
            }
        }

        var nearbyEntities = this.level().getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(6.0), e -> e != this);
        for (LivingEntity e : nearbyEntities) {
            double dx = e.getX() - this.getX();
            double dz = e.getZ() - this.getZ();
            double dist = Math.max(0.5, Math.sqrt(dx * dx + dz * dz));
            e.push((dx / dist) * 1.5, 0.35, (dz / dist) * 1.5);
        }
    }

    private void triggerInfusionPhase() {
        this.isInfusionPhase = true;
        this.bankaiTransformTicks = 25;
        this.getNavigation().stop();

        this.level().playSound(null, this.blockPosition(), ModSounds.REIATSU_BURST.get(), SoundSource.HOSTILE, 2.5f, 0.8f);
        this.level().playSound(null, this.blockPosition(), ModSounds.INNER_LAUGH.get(), SoundSource.HOSTILE, 2.5f, 0.8f);

        for (ServerPlayer player : this.level().getEntitiesOfClass(ServerPlayer.class, this.getBoundingBox().inflate(60.0))) {
            player.sendSystemMessage(Component.literal("§f§lInner Zangetsu: §c§lGETSUGA INFUSION! Będziesz moim pożywieniem!"));
            PacketDistributor.sendToPlayer(player, new CameraShakePayload(40, 5.0f));
        }

        // Moderate speed boost in Infusion Phase
        var speed = this.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null) {
            speed.setBaseValue(0.30);
        }

        if (this.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.EXPLOSION_EMITTER, this.getX(), this.getY() + 1.0, this.getZ(), 1, 0, 0, 0, 0);
            Vector3f crimson = new Vector3f(1.0f, 0.1f, 0.1f);
            for (double y = 0; y < 12.0; y += 0.5) {
                serverLevel.sendParticles(new DustParticleOptions(crimson, 2.5f), this.getX(), this.getY() + y, this.getZ(), 8, 0.5, 0.1, 0.5, 0.05);
                serverLevel.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, this.getX(), this.getY() + y, this.getZ(), 5, 0.4, 0.1, 0.4, 0.02);
            }
        }

        var nearbyEntities = this.level().getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(8.0), e -> e != this);
        for (LivingEntity e : nearbyEntities) {
            double dx = e.getX() - this.getX();
            double dz = e.getZ() - this.getZ();
            double dist = Math.max(0.5, Math.sqrt(dx * dx + dz * dz));
            e.push((dx / dist) * 2.2, 0.5, (dz / dist) * 2.2);
        }
    }

    private void performShunpo(LivingEntity target) {
        Vec3 targetLook = target.getLookAngle();
        double sideAngle = (this.random.nextBoolean() ? 1.0 : -1.0) * (Math.PI / 4.0);
        double cosA = Math.cos(sideAngle);
        double sinA = Math.sin(sideAngle);
        double dirX = targetLook.x * cosA - targetLook.z * sinA;
        double dirZ = targetLook.x * sinA + targetLook.z * cosA;

        double distOffset = this.isBankaiTrial ? 3.0 : 3.5;
        double targetX = target.getX() - dirX * distOffset;
        double targetY = target.getY();
        double targetZ = target.getZ() - dirZ * distOffset;

        if (this.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.POOF, this.getX(), this.getY() + 1.0, this.getZ(), 8, 0.3, 0.5, 0.3, 0.05);
        }
        this.level().playSound(null, this.blockPosition(), ModSounds.SHUNPO.get(), SoundSource.HOSTILE, 1.5f, 1.0f);

        this.teleportTo(targetX, targetY, targetZ);
        this.lookAt(target, 360.0f, 360.0f);

        if (this.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.POOF, this.getX(), this.getY() + 1.0, this.getZ(), 8, 0.3, 0.5, 0.3, 0.05);
            if (this.isBankaiTrial) {
                serverLevel.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, this.getX(), this.getY() + 1.0, this.getZ(), 6, 0.2, 0.4, 0.2, 0.02);
            }
        }
        this.level().playSound(null, this.blockPosition(), ModSounds.SHUNPO.get(), SoundSource.HOSTILE, 1.5f, 1.0f);

        // Fair 8 ticks (0.4s) pause after teleporting so player can react and turn!
        this.postShunpoStun = this.isBankaiTrial ? 8 : 12;
    }

    private void performAntiPillar(LivingEntity target) {
        if (this.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.POOF, this.getX(), this.getY() + 1.0, this.getZ(), 10, 0.3, 0.5, 0.3, 0.05);
            serverLevel.playSound(null, this.blockPosition(), ModSounds.SHUNPO.get(), SoundSource.HOSTILE, 1.8f, 1.0f);

            // Teleport directly onto the elevated pillar next to target
            this.teleportTo(target.getX(), target.getY(), target.getZ());
            this.lookAt(target, 360.0f, 360.0f);

            serverLevel.sendParticles(ParticleTypes.POOF, this.getX(), this.getY() + 1.0, this.getZ(), 10, 0.3, 0.5, 0.3, 0.05);
            serverLevel.sendParticles(ParticleTypes.SONIC_BOOM, this.getX(), this.getY() + 1.0, this.getZ(), 1, 0, 0, 0, 0);
            serverLevel.playSound(null, this.blockPosition(), ModSounds.SHUNPO.get(), SoundSource.HOSTILE, 1.8f, 1.0f);

            this.postShunpoStun = 0;
            if (target instanceof Player player) {
                player.sendSystemMessage(Component.literal("§c§lInner Zangetsu: §fMyślisz, że możesz się przede mną ukryć na klockach?!"));
            }
        }
    }

    private void fireGetsuga(LivingEntity target) {
        this.level().playSound(null, this.blockPosition(), ModSounds.GETSUGA_TENSHO.get(), SoundSource.HOSTILE, 1.8f, 1.1f);
        boolean isBankai = this.isBankaiPhase || this.isBankaiTrial;
        boolean vertical = this.isBankaiTrial ? this.random.nextBoolean() : false;
        float charge = this.isInfusionPhase ? 0.9f : (this.isBankaiTrial ? 0.6f : 0.4f);

        GetsugaTenshoEntity getsuga = new GetsugaTenshoEntity(this.level(), this, isBankai, charge, vertical);
        Vec3 toTarget = target.position().add(0, target.getEyeHeight() * 0.5, 0).subtract(this.getEyePosition()).normalize();
        double speed = this.isInfusionPhase ? 1.6 : (this.isBankaiTrial ? 1.45 : 1.2);
        getsuga.setDeltaMovement(toTarget.scale(speed));
        this.level().addFreshEntity(getsuga);
    }

    @Override
    public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
        this.bossEvent.addPlayer(player);
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        this.bossEvent.removePlayer(player);
    }

    @Override
    public void die(DamageSource damageSource) {
        super.die(damageSource);

        if (!this.level().isClientSide) {
            if (damageSource.getEntity() instanceof ServerPlayer player) {
                ReiatsuData data = player.getData(ModDataAttachments.REIATSU);
                data.setBankaiUnlocked(true);
                player.getPersistentData().putBoolean("ZangetsuBankaiUnlocked", true);

                // Immediate synchronization to client
                PacketDistributor.sendToPlayer(player, new SyncReiatsuPayload(
                        data.getCurrent(),
                        data.getMax(),
                        data.isInfusionActive(),
                        player.getPersistentData().getBoolean("ZangetsuSlashVertical"),
                        true
                ));

                if (this.level().dimension() == com.zangetsu.world.InnerWorldManager.INNER_WORLD_KEY) {
                    com.zangetsu.world.InnerWorldManager.returnFromInnerWorld(player, true);
                } else {
                    ItemStack mainHand = player.getMainHandItem();
                    if (mainHand.is(ModItems.ASAUCHI.get())) {
                        player.setItemInHand(player.getUsedItemHand(), new ItemStack(ModItems.ZANGETSU_SHIKAI.get()));
                    } else if (!mainHand.is(ModItems.ZANGETSU_SHIKAI.get()) && !mainHand.is(ModItems.TENSA_ZANGETSU.get())) {
                        this.spawnAtLocation(new ItemStack(ModItems.ZANGETSU_SHIKAI.get()));
                    }
                    player.sendSystemMessage(Component.translatable("message.zangetsu.trial_victory"));
                    player.sendSystemMessage(Component.literal("§6§l[Zanpakutō Mastery] §aPokonałeś Inner Zangetsu! Bankai [B] zostało trwale odblokowane!"));
                }
            } else {
                this.spawnAtLocation(new ItemStack(ModItems.ZANGETSU_SHIKAI.get()));
            }

            this.level().playSound(null, this.blockPosition(), ModSounds.REIATSU_BURST.get(), SoundSource.HOSTILE, 2.0f, 0.8f);
        }
    }
}
