package com.zangetsu.item;

import com.zangetsu.entity.DomainExpansionEntity;
import com.zangetsu.init.ModDataAttachments;
import com.zangetsu.init.ModSounds;
import com.zangetsu.init.ReiatsuData;
import com.zangetsu.network.CameraShakePayload;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import org.joml.Vector3f;

import java.util.List;

public class DomainExpansionItem extends Item {
    public static final float REIATSU_COST = 400.0f;
    public static final int CHARGE_TICKS = 40; // 2 seconds

    public DomainExpansionItem() {
        super(new Item.Properties()
                .stacksTo(1)
                .rarity(Rarity.EPIC)
                .fireResistant());
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!level.isClientSide) {
            // Check if Bankai is unlocked
            ReiatsuData data = player.getData(ModDataAttachments.REIATSU);
            boolean unlocked = data.isBankaiUnlocked()
                    || player.getPersistentData().getBoolean("ZangetsuBankaiUnlocked")
                    || player.isCreative();

            if (!unlocked) {
                player.displayClientMessage(Component.literal("§cTwoja dusza nie opanowała jeszcze Bankai! Pokonaj wewnętrznego Hollowa w Jinzen, by odblokować Domenę."), true);
                return InteractionResultHolder.fail(stack);
            }

            // Check if player is already inside their own active domain
            DomainExpansionEntity activeDomain = findActiveDomainFor(player, level);
            if (activeDomain != null) {
                // Inside active domain -> Trigger Gran Rey Getsuga Finisher!
                activeDomain.startFinisher();
                player.displayClientMessage(Component.literal("§4§l[GRAN REY GETSUGA] §cKondensacja czarnego deszczu Getsugi..."), true);
                player.getCooldowns().addCooldown(this, 120);
                return InteractionResultHolder.sidedSuccess(stack, false);
            }

            // Check Reiatsu for expanding domain
            if (!data.consume(0.0f) && data.getCurrent() < REIATSU_COST && !player.isCreative()) {
                player.displayClientMessage(Component.literal("§cZa mało Reiatsu na Rozszerzenie Domeny! (Wymagane " + (int) REIATSU_COST + ")"), true);
                return InteractionResultHolder.fail(stack);
            }

            // Start Hand Seal charge
            player.startUsingItem(hand);
            player.getPersistentData().putBoolean("ZangetsuDomainCharging", true);
            player.getPersistentData().putInt("ZangetsuDomainChargeTicks", 0);
            level.playSound(null, player.blockPosition(), ModSounds.REIATSU_BURST.get(), SoundSource.PLAYERS, 2.0f, 0.6f);
            return InteractionResultHolder.consume(stack);
        }

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
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
    public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int count) {
        if (entity instanceof Player player) {
            int duration = this.getUseDuration(stack, entity) - count;

            if (level.isClientSide) {
                // Swirling crimson & black particles around hands
                Vec3 eye = player.getEyePosition();
                Vec3 look = player.getLookAngle();
                Vec3 handPos = eye.add(look.scale(0.8)).add(0, -0.2, 0);

                Vector3f crimson = new Vector3f(0.95f, 0.05f, 0.15f);
                Vector3f black = new Vector3f(0.01f, 0.01f, 0.01f);

                for (int i = 0; i < 3; i++) {
                    double ox = (level.random.nextDouble() - 0.5) * 0.8;
                    double oy = (level.random.nextDouble() - 0.5) * 0.8;
                    double oz = (level.random.nextDouble() - 0.5) * 0.8;
                    level.addParticle(new DustParticleOptions(crimson, 1.8f),
                            handPos.x + ox, handPos.y + oy, handPos.z + oz, -ox * 0.2, -oy * 0.2, -oz * 0.2);
                    level.addParticle(new DustParticleOptions(black, 2.0f),
                            handPos.x - ox, handPos.y - oy, handPos.z - oz, ox * 0.2, oy * 0.2, oz * 0.2);
                }
            } else if (player instanceof ServerPlayer sp) {
                ServerLevel serverLevel = sp.serverLevel();
                player.getPersistentData().putInt("ZangetsuDomainChargeTicks", duration);
                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 255, false, false, false));

                int pct = Math.min(100, (duration * 100) / CHARGE_TICKS);
                float rem = Math.max(0.0f, (CHARGE_TICKS - duration) / 20.0f);
                player.displayClientMessage(Component.literal("§4§l[ROZSZERZENIE DOMENY] §cZnak Dłoni... §e" + pct + "% §7(" + String.format("%.1f", rem) + "s)"), true);

                if (duration == 20) {
                    serverLevel.playSound(null, sp.blockPosition(), ModSounds.JINZEN_HEARTBEAT.get(), SoundSource.PLAYERS, 2.5f, 1.0f);
                }

                if (duration % 4 == 0) {
                    float intensity = 0.5f + (duration / (float) CHARGE_TICKS) * 2.5f;
                    PacketDistributor.sendToPlayer(sp, new CameraShakePayload(6, intensity));
                }

                // Complete 40 ticks: Unleash Kōten Zangetsu!
                if (duration >= CHARGE_TICKS) {
                    player.stopUsingItem();
                    player.getPersistentData().remove("ZangetsuDomainCharging");
                    player.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);

                    ReiatsuData data = player.getData(ModDataAttachments.REIATSU);
                    if (!player.isCreative()) {
                        data.consume(REIATSU_COST);
                    }

                    // Forward offset center (75 blocks in horizontal look direction)
                    Vec3 look = player.getLookAngle();
                    Vec3 horizontalLook = new Vec3(look.x, 0, look.z);
                    if (horizontalLook.lengthSqr() < 0.001) {
                        horizontalLook = new Vec3(0, 0, 1);
                    } else {
                        horizontalLook = horizontalLook.normalize();
                    }
                    Vec3 center = player.position().add(horizontalLook.scale(75.0));

                    // Spawn monumental 100-block radius Domain Expansion Entity
                    DomainExpansionEntity domain = new DomainExpansionEntity(serverLevel, center, player);
                    serverLevel.addFreshEntity(domain);

                    // Sounds & Camera Shake
                    serverLevel.playSound(null, player.blockPosition(), ModSounds.DOMAIN_EXPAND.get(), SoundSource.PLAYERS, 4.0f, 1.0f);
                    serverLevel.playSound(null, player.blockPosition(), ModSounds.BANKAI.get(), SoundSource.PLAYERS, 3.5f, 0.85f);
                    PacketDistributor.sendToPlayer(sp, new CameraShakePayload(50, 4.5f));

                    // Title Announcement
                    sp.connection.send(new ClientboundSetTitlesAnimationPacket(10, 60, 20));
                    sp.connection.send(new ClientboundSetTitleTextPacket(Component.literal("§4§lROZSZERZENIE DOMENY")));
                    sp.connection.send(new ClientboundSetSubtitleTextPacket(Component.literal("§c§oKōten Zangetsu (絶望を断つ月影)")));

                    player.getCooldowns().addCooldown(this, 160); // 8 sec cooldown
                }
            }
        }
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity entity, int timeLeft) {
        if (entity instanceof Player player) {
            player.getPersistentData().remove("ZangetsuDomainCharging");
            player.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
            if (!level.isClientSide) {
                player.displayClientMessage(Component.literal("§7Znak Dłoni przerwany."), true);
            }
        }
    }

    private DomainExpansionEntity findActiveDomainFor(Player player, Level level) {
        AABB box = player.getBoundingBox().inflate(120.0);
        List<DomainExpansionEntity> domains = level.getEntitiesOfClass(DomainExpansionEntity.class, box);
        for (DomainExpansionEntity d : domains) {
            if (d.getOwnerUUID().isPresent() && d.getOwnerUUID().get().equals(player.getUUID())) {
                if (player.position().distanceTo(d.position()) <= d.getRadius()) {
                    return d;
                }
            }
        }
        return null;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("§4§l絶望を断つ月影 §7(Zetsubō o Tatsu Tsukikage)").withStyle(ChatFormatting.ITALIC));
        tooltip.add(Component.literal("§cKsiężycowy Horyzont Złamanego Przeznaczenia"));
        tooltip.add(Component.literal(""));
        tooltip.add(Component.literal("§f[Przytrzymaj PPM] §eZnak Dłoni Tensa Zangetsu"));
        tooltip.add(Component.literal("§7Rozszerza monumentalną barierę o promieniu §c100 bloków§7 z przodu."));
        tooltip.add(Component.literal("§7Wymaga odblokowanego Bankai i §b400 Reiatsu§7."));
        tooltip.add(Component.literal(""));
        tooltip.add(Component.literal("§6Wrodzone Właściwości Domeny:"));
        tooltip.add(Component.literal("§f• §cSure-Hit „Kuroi Tsuki”: §7Zawieszony deszcz bezkierunkowo tnie wrogów"));
        tooltip.add(Component.literal("§f• §eZłamanie Łańcucha: §7Nieskończone Shunpo za plecy celu & boska szybkość"));
        tooltip.add(Component.literal("§f• §4Gran Rey Getsuga: §7Użyj w domenie, by zniszczyć czasoprzestrzeń"));
    }
}
