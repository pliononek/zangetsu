package com.zangetsu.client;

import com.zangetsu.ZangetsuMod;
import com.zangetsu.client.renderer.GetsugaTenshoRenderer;
import com.zangetsu.client.renderer.InnerZangetsuRenderer;
import com.zangetsu.init.ModDataAttachments;
import com.zangetsu.init.ModEntities;
import com.zangetsu.init.ModItems;
import com.zangetsu.init.ReiatsuData;
import com.zangetsu.network.BankaiKeyPayload;
import com.zangetsu.network.InfusionKeyPayload;
import com.zangetsu.network.ShunpoKeyPayload;
import com.zangetsu.network.ToggleSlashKeyPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RenderHandEvent;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.network.PacketDistributor;

public class ClientModEvents {

    @EventBusSubscriber(modid = ZangetsuMod.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ModBus {
        @SubscribeEvent
        public static void registerKeys(RegisterKeyMappingsEvent event) {
            event.register(KeyBindings.BANKAI_KEY);
            event.register(KeyBindings.SHUNPO_KEY);
            event.register(KeyBindings.INFUSION_KEY);
            event.register(KeyBindings.TOGGLE_SLASH_KEY);
        }

        @SubscribeEvent
        public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
            event.registerEntityRenderer(ModEntities.GETSUGA_TENSHO.get(), GetsugaTenshoRenderer::new);
            event.registerEntityRenderer(ModEntities.INNER_ZANGETSU.get(), InnerZangetsuRenderer::new);
            event.registerEntityRenderer(ModEntities.DOMAIN_EXPANSION.get(), com.zangetsu.client.renderer.DomainExpansionRenderer::new);
        }

        @SubscribeEvent
        public static void registerGuiLayers(RegisterGuiLayersEvent event) {
            event.registerAbove(
                    VanillaGuiLayers.EXPERIENCE_BAR,
                    ResourceLocation.fromNamespaceAndPath(ZangetsuMod.MODID, "reiatsu_hud"),
                    new ReiatsuHudOverlay()
            );
        }
    }

    @EventBusSubscriber(modid = ZangetsuMod.MODID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
    public static class GameBus {
        private static int bankaiHoldTicks = 0;
        private static int bankaiRevertCooldown = 0;
        private static boolean bankaiKeyReleasedAfterTransform = false;

        @SubscribeEvent
        public static void onClientTick(ClientTickEvent.Post event) {
            CameraShakeHandler.tick();

            if (bankaiRevertCooldown > 0) {
                bankaiRevertCooldown--;
            }

            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null || mc.screen != null) {
                if (bankaiHoldTicks > 0) {
                    PacketDistributor.sendToServer(new BankaiKeyPayload(BankaiKeyPayload.ACTION_CANCEL, bankaiHoldTicks));
                    bankaiHoldTicks = 0;
                }
                return;
            }

            // Track if key has been released after transformation
            if (!KeyBindings.BANKAI_KEY.isDown()) {
                bankaiKeyReleasedAfterTransform = true;
            }

            ItemStack mainHand = mc.player.getMainHandItem();
            boolean isShikai = mainHand.is(ModItems.ZANGETSU_SHIKAI.get());
            boolean isBankai = mainHand.is(ModItems.TENSA_ZANGETSU.get());

            if (isBankai) {
                bankaiHoldTicks = 0;
                mc.player.getPersistentData().remove("ZangetsuBankaiCharging");

                while (KeyBindings.BANKAI_KEY.consumeClick()) {
                    if (bankaiRevertCooldown > 0 || !bankaiKeyReleasedAfterTransform) {
                        int remSec = (int) Math.ceil(bankaiRevertCooldown / 20.0f);
                        if (remSec > 0) {
                            mc.player.displayClientMessage(Component.literal("§7[Bankai] Odczekaj " + remSec + "s przed powrotem do Shikai..."), true);
                        }
                    } else {
                        PacketDistributor.sendToServer(new BankaiKeyPayload(BankaiKeyPayload.ACTION_REVERT, 0));
                    }
                }
            } else if (isShikai) {
                ReiatsuData data = mc.player.getData(ModDataAttachments.REIATSU);
                boolean unlocked = (data != null && data.isBankaiUnlocked()) || mc.player.getPersistentData().getBoolean("ZangetsuBankaiUnlocked") || mc.player.isCreative();

                if (KeyBindings.BANKAI_KEY.isDown()) {
                    if (!unlocked) {
                        if (KeyBindings.BANKAI_KEY.consumeClick()) {
                            mc.player.displayClientMessage(Component.literal("§cYour soul has not conquered Bankai yet! Defeat your inner Hollow in Jinzen meditation to awaken it."), true);
                        }
                        bankaiHoldTicks = 0;
                        mc.player.getPersistentData().remove("ZangetsuBankaiCharging");
                    } else {
                        bankaiHoldTicks++;
                        mc.player.getPersistentData().putBoolean("ZangetsuBankaiCharging", true);

                        if (bankaiHoldTicks == 1) {
                            PacketDistributor.sendToServer(new BankaiKeyPayload(BankaiKeyPayload.ACTION_START, 0));
                        }

                        PacketDistributor.sendToServer(new BankaiKeyPayload(BankaiKeyPayload.ACTION_TICK, bankaiHoldTicks));

                        if (bankaiHoldTicks >= 50) {
                            // 2.5 seconds (50 ticks) complete: trigger monumental Bankai!
                            PacketDistributor.sendToServer(new BankaiKeyPayload(BankaiKeyPayload.ACTION_COMPLETE, bankaiHoldTicks));
                            bankaiHoldTicks = 0;
                            bankaiRevertCooldown = 60; // 3 seconds (60 ticks) cooldown on deactivation!
                            bankaiKeyReleasedAfterTransform = false; // Must release [B] first!
                            while (KeyBindings.BANKAI_KEY.consumeClick()) {} // Flush any remaining click events
                            mc.player.getPersistentData().remove("ZangetsuBankaiCharging");
                        }
                    }
                } else {
                    if (bankaiHoldTicks > 0) {
                        PacketDistributor.sendToServer(new BankaiKeyPayload(BankaiKeyPayload.ACTION_CANCEL, bankaiHoldTicks));
                        bankaiHoldTicks = 0;
                        mc.player.getPersistentData().remove("ZangetsuBankaiCharging");
                    }
                }
            } else {
                if (bankaiHoldTicks > 0) {
                    PacketDistributor.sendToServer(new BankaiKeyPayload(BankaiKeyPayload.ACTION_CANCEL, bankaiHoldTicks));
                    bankaiHoldTicks = 0;
                    mc.player.getPersistentData().remove("ZangetsuBankaiCharging");
                }
            }

            while (KeyBindings.SHUNPO_KEY.consumeClick()) {
                PacketDistributor.sendToServer(new ShunpoKeyPayload());
            }

            while (KeyBindings.INFUSION_KEY.consumeClick()) {
                PacketDistributor.sendToServer(new InfusionKeyPayload());
            }

            while (KeyBindings.TOGGLE_SLASH_KEY.consumeClick()) {
                if (mc.player != null) {
                    boolean current = mc.player.getPersistentData().getBoolean("ZangetsuSlashVertical");
                    mc.player.getPersistentData().putBoolean("ZangetsuSlashVertical", !current);
                }
                PacketDistributor.sendToServer(new ToggleSlashKeyPayload());
            }
        }

        @SubscribeEvent
        public static void onRenderPlayerPre(RenderPlayerEvent.Pre event) {
            Player player = event.getEntity();
            if (player.getPersistentData().getBoolean("ZangetsuBankaiCharging")) {
                PlayerModel<?> model = event.getRenderer().getModel();
                // Lock right arm pointing horizontally forward holding Zangetsu towards enemy!
                model.rightArm.xRot = (float) (-Math.PI / 2.0);
                model.rightArm.yRot = -0.15f;
                model.rightArm.zRot = 0.0f;

                model.leftArm.xRot = 0.2f;
                model.leftArm.yRot = 0.0f;
                model.leftArm.zRot = -0.15f;

                model.rightLeg.xRot = 0.0f;
                model.leftLeg.xRot = 0.0f;
            } else if (player.getPersistentData().getBoolean("ZangetsuDomainCharging")) {
                PlayerModel<?> model = event.getRenderer().getModel();
                // Crossed arms forming Tensa Zangetsu Manji Hand Seal!
                model.rightArm.xRot = -0.85f;
                model.rightArm.yRot = -0.55f;
                model.rightArm.zRot = 0.45f;

                model.leftArm.xRot = -0.85f;
                model.leftArm.yRot = 0.55f;
                model.leftArm.zRot = -0.45f;

                model.head.xRot = 0.25f; // Focused downward on the seal
            }
        }

        @SubscribeEvent
        public static void onRenderHand(RenderHandEvent event) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null) {
                if (mc.player.getPersistentData().getBoolean("ZangetsuBankaiCharging")) {
                    // Intense spiritual pressure vibration in 1st person
                    float age = mc.player.tickCount + event.getPartialTick();
                    float jitterX = (float) Math.sin(age * 4.2) * 0.007f;
                    float jitterY = (float) Math.cos(age * 5.1) * 0.007f;
                    event.getPoseStack().translate(jitterX, jitterY, 0);
                } else if (mc.player.getPersistentData().getBoolean("ZangetsuDomainCharging")) {
                    // Spiritual hand seal resonance
                    float age = mc.player.tickCount + event.getPartialTick();
                    float jitterX = (float) Math.sin(age * 6.5) * 0.009f;
                    float jitterY = (float) Math.cos(age * 7.2) * 0.009f;
                    event.getPoseStack().translate(jitterX, jitterY, -0.05f);
                }
            }
        }

        @SubscribeEvent
        public static void onComputeCameraAngles(net.neoforged.neoforge.client.event.ViewportEvent.ComputeCameraAngles event) {
            if (CameraShakeHandler.shakeTicks > 0 && CameraShakeHandler.maxShakeTicks > 0) {
                float progress = (float) CameraShakeHandler.shakeTicks / (float) CameraShakeHandler.maxShakeTicks;
                float currentIntensity = CameraShakeHandler.intensity * progress;
                float pitchOffset = (float) (Math.sin(CameraShakeHandler.shakeTicks * 1.8) * currentIntensity);
                float rollOffset = (float) (Math.cos(CameraShakeHandler.shakeTicks * 2.3) * currentIntensity);
                event.setPitch(event.getPitch() + pitchOffset);
                event.setRoll(event.getRoll() + rollOffset);
            }
        }
    }
}
