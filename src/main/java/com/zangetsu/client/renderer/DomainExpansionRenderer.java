package com.zangetsu.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.zangetsu.ZangetsuMod;
import com.zangetsu.entity.DomainExpansionEntity;
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
        poseStack.pushPose();

        float radius = entity.getRadius();
        float age = entity.tickCount + partialTicks;
        float pulse = (float) Math.sin(age * 0.15f);

        int red = Math.min(255, (int) (200 + pulse * 45));
        int green = Math.max(0, (int) (30 + pulse * 15));
        int blue = Math.max(0, (int) (40 + pulse * 20));
        int alpha = 235;

        VertexConsumer consumer = buffer.getBuffer(RenderType.entityTranslucentEmissive(BARRIER_TEXTURE));
        Matrix4f matrix = poseStack.last().pose();

        // 1. Render Monumental 100-Block Spherical Shell
        renderSphericalBarrier(matrix, consumer, radius, red, green, blue, alpha);

        poseStack.popPose();

        // 2. Render Ethereal Avatars (Vasto Lorde Mask & Quincy Shadow) behind Owner
        Optional<UUID> ownerUUID = entity.getOwnerUUID();
        if (ownerUUID.isPresent()) {
            Player owner = entity.level().getPlayerByUUID(ownerUUID.get());
            if (owner != null && owner.isAlive()) {
                double ownerDist = owner.position().distanceTo(entity.position());
                if (ownerDist <= radius) {
                    renderOwnerShadows(owner, entity, partialTicks, poseStack, buffer);
                }
            }
        }

        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    private void renderSphericalBarrier(Matrix4f matrix, VertexConsumer consumer, float r,
                                        int red, int green, int blue, int alpha) {
        int rings = 28;
        int segments = 28;
        int light = 0xF000F0;
        float tile = 8.0f; // Tile cracked texture 8 times across the 100m dome

        for (int i = 0; i < rings; i++) {
            float phi0 = (float) (Math.PI * ((float) i / rings - 0.5f));
            float phi1 = (float) (Math.PI * ((float) (i + 1) / rings - 0.5f));

            float cosPhi0 = (float) Math.cos(phi0);
            float sinPhi0 = (float) Math.sin(phi0);
            float cosPhi1 = (float) Math.cos(phi1);
            float sinPhi1 = (float) Math.sin(phi1);

            float v0 = ((float) i / rings) * tile;
            float v1 = ((float) (i + 1) / rings) * tile;

            for (int j = 0; j < segments; j++) {
                float theta0 = (float) (2.0 * Math.PI * (float) j / segments);
                float theta1 = (float) (2.0 * Math.PI * (float) (j + 1) / segments);

                float u0 = ((float) j / segments) * tile;
                float u1 = ((float) (j + 1) / segments) * tile;

                float x00 = r * cosPhi0 * (float) Math.cos(theta0);
                float y00 = r * sinPhi0;
                float z00 = r * cosPhi0 * (float) Math.sin(theta0);

                float x10 = r * cosPhi1 * (float) Math.cos(theta0);
                float y10 = r * sinPhi1;
                float z10 = r * cosPhi1 * (float) Math.sin(theta0);

                float x11 = r * cosPhi1 * (float) Math.cos(theta1);
                float y11 = r * sinPhi1;
                float z11 = r * cosPhi1 * (float) Math.sin(theta1);

                float x01 = r * cosPhi0 * (float) Math.cos(theta1);
                float y01 = r * sinPhi0;
                float z01 = r * cosPhi0 * (float) Math.sin(theta1);

                // Outside-facing quad
                consumer.addVertex(matrix, x00, y00, z00).setColor(red, green, blue, alpha).setUv(u0, v0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(0, 1, 0);
                consumer.addVertex(matrix, x10, y10, z10).setColor(red, green, blue, alpha).setUv(u0, v1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(0, 1, 0);
                consumer.addVertex(matrix, x11, y11, z11).setColor(red, green, blue, alpha).setUv(u1, v1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(0, 1, 0);
                consumer.addVertex(matrix, x01, y01, z01).setColor(red, green, blue, alpha).setUv(u1, v0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(0, 1, 0);

                // Inside-facing quad (reversed vertex order for trapped players)
                consumer.addVertex(matrix, x01, y01, z01).setColor(red, green, blue, alpha).setUv(u1, v0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(0, -1, 0);
                consumer.addVertex(matrix, x11, y11, z11).setColor(red, green, blue, alpha).setUv(u1, v1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(0, -1, 0);
                consumer.addVertex(matrix, x10, y10, z10).setColor(red, green, blue, alpha).setUv(u0, v1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(0, -1, 0);
                consumer.addVertex(matrix, x00, y00, z00).setColor(red, green, blue, alpha).setUv(u0, v0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(0, -1, 0);
            }
        }
    }

    private void renderOwnerShadows(Player owner, DomainExpansionEntity entity, float partialTicks,
                                    PoseStack poseStack, MultiBufferSource buffer) {
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
