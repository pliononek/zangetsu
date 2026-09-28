package com.zangetsu.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.zangetsu.ZangetsuMod;
import com.zangetsu.entity.DomainExpansionEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import java.util.Optional;
import java.util.UUID;

public class DomainExpansionRenderer extends EntityRenderer<DomainExpansionEntity> {
    private static final ResourceLocation BARRIER_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(ZangetsuMod.MODID, "textures/entity/domain_barrier.png");
    private static final ResourceLocation VASTO_LORDE_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(ZangetsuMod.MODID, "textures/entity/vasto_lorde_shadow.png");
    private static final ResourceLocation QUINCY_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(ZangetsuMod.MODID, "textures/entity/quincy_shadow.png");

    public DomainExpansionRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public ResourceLocation getTextureLocation(DomainExpansionEntity entity) {
        return BARRIER_TEXTURE;
    }

    @Override
    public void render(DomainExpansionEntity entity, float entityYaw, float partialTicks,
                       PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        // Render Ethereal Avatars (Vasto Lorde Mask & Quincy Shadow) behind Owner
        Optional<UUID> ownerUUID = entity.getOwnerUUID();
        if (ownerUUID.isPresent()) {
            Player owner = entity.level().getPlayerByUUID(ownerUUID.get());
            if (owner != null && owner.isAlive()) {
                if (entity.isInsideDomain(owner.position())) {
                    renderOwnerShadows(owner, entity, partialTicks, poseStack, buffer);
                }
            }
        }

        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    private void renderOwnerShadows(Player owner, DomainExpansionEntity entity, float partialTicks,
                                    PoseStack poseStack, MultiBufferSource buffer) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && mc.player.getUUID().equals(owner.getUUID()) && mc.options.getCameraType().isFirstPerson()) {
            return; // In first-person, don't render back shadows to avoid blocking camera/view
        }

        poseStack.pushPose();

        // Position relative to entity origin
        double ox = (owner.xo + (owner.getX() - owner.xo) * partialTicks) - entity.getX();
        double oy = (owner.yo + (owner.getY() - owner.yo) * partialTicks) - entity.getY();
        double oz = (owner.zo + (owner.getZ() - owner.zo) * partialTicks) - entity.getZ();

        poseStack.translate(ox, oy + 1.2, oz);

        float yaw = owner.yBodyRotO + (owner.yBodyRot - owner.yBodyRotO) * partialTicks;
        poseStack.mulPose(Axis.YP.rotationDegrees(-yaw));

        float age = entity.tickCount + partialTicks;
        float floatBob = (float) Math.sin(age * 0.1f) * 0.12f;

        // 1. Left: Horned Vasto Lorde Mask Silhouette
        poseStack.pushPose();
        poseStack.translate(-1.8, 1.2 + floatBob, -1.8);
        poseStack.scale(2.2f, 2.2f, 2.2f);
        VertexConsumer vlConsumer = buffer.getBuffer(RenderType.entityTranslucentEmissive(VASTO_LORDE_TEXTURE));
        renderBillboardQuad(poseStack.last().pose(), vlConsumer, 255, 255, 255, 220);
        poseStack.popPose();

        // 2. Right: Quincy Old Man Zangetsu Coat Silhouette
        poseStack.pushPose();
        poseStack.translate(1.8, 1.2 - floatBob, -1.8);
        poseStack.scale(2.2f, 2.2f, 2.2f);
        VertexConsumer quincyConsumer = buffer.getBuffer(RenderType.entityTranslucentEmissive(QUINCY_TEXTURE));
        renderBillboardQuad(poseStack.last().pose(), quincyConsumer, 255, 255, 255, 220);
        poseStack.popPose();

        poseStack.popPose();
    }

    private void renderBillboardQuad(Matrix4f matrix, VertexConsumer consumer,
                                     int r, int g, int b, int a) {
        int light = 0xF000F0;
        float half = 0.5f;

        consumer.addVertex(matrix, -half, -half, 0).setColor(r, g, b, a).setUv(0, 1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(0, 0, 1);
        consumer.addVertex(matrix, half, -half, 0).setColor(r, g, b, a).setUv(1, 1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(0, 0, 1);
        consumer.addVertex(matrix, half, half, 0).setColor(r, g, b, a).setUv(1, 0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(0, 0, 1);
        consumer.addVertex(matrix, -half, half, 0).setColor(r, g, b, a).setUv(0, 0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(0, 0, 1);

        // Double sided
        consumer.addVertex(matrix, -half, half, 0).setColor(r, g, b, a).setUv(0, 0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(0, 0, -1);
        consumer.addVertex(matrix, half, half, 0).setColor(r, g, b, a).setUv(1, 0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(0, 0, -1);
        consumer.addVertex(matrix, half, -half, 0).setColor(r, g, b, a).setUv(1, 1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(0, 0, -1);
        consumer.addVertex(matrix, -half, -half, 0).setColor(r, g, b, a).setUv(0, 1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(0, 0, -1);
    }
}
