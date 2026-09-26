package com.zangetsu.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.zangetsu.ZangetsuMod;
import com.zangetsu.entity.GetsugaTenshoEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;

public class GetsugaTenshoRenderer extends EntityRenderer<GetsugaTenshoEntity> {
    private static final ResourceLocation SHIKAI_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(ZangetsuMod.MODID, "textures/entity/getsuga_shikai.png");
    private static final ResourceLocation BANKAI_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(ZangetsuMod.MODID, "textures/entity/getsuga_bankai.png");

    public GetsugaTenshoRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public ResourceLocation getTextureLocation(GetsugaTenshoEntity entity) {
        return entity.isBankai() ? BANKAI_TEXTURE : SHIKAI_TEXTURE;
    }

    @Override
    public void render(GetsugaTenshoEntity entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        poseStack.pushPose();

        // 1. Align coordinate frame with entity flight direction (+Z is forward)
        poseStack.mulPose(Axis.YP.rotationDegrees(-entity.getYRot()));
        poseStack.mulPose(Axis.XP.rotationDegrees(entity.getXRot()));

        // 2. Rotate 90 degrees around Z axis if vertical cut is selected
        if (entity.isVertical()) {
            poseStack.mulPose(Axis.ZP.rotationDegrees(90.0F));
        }

        boolean bankai = entity.isBankai();
        float charge = entity.getCharge();

        // Monumental scaling: 10-12 blocks normal, up to 20-22 blocks wide/tall when charged!
        float scale = bankai ? (3.4f + charge * 3.0f) : (3.0f + charge * 3.2f);
        poseStack.scale(scale, scale, scale);

        VertexConsumer consumer = buffer.getBuffer(RenderType.entityTranslucentEmissive(getTextureLocation(entity)));

        float age = entity.tickCount + partialTicks;
        float pulse = (float) Math.sin(age * 0.45f) * 0.06f;

        Matrix4f matrix = poseStack.last().pose();

        if (bankai) {
            // Kuroi Getsuga Tenshō:
            // Layer A: Outer Roaring Crimson Flame Edge (large, sweeping translucent energy)
            renderCurvedCrescent(matrix, consumer, 2.1f, 2.2f + pulse, 1.2f, 0.45f, 0.08f, 250, 15, 35, 200);

            // Layer B: Dense Obsidian Core Blade (sharp, cutting leading edge)
            renderCurvedCrescent(matrix, consumer, 1.9f, 2.0f, 1.35f, 0.25f, 0.12f, 15, 15, 18, 255);

            // Layer C: Inner Blood-Red Spine Highlight
            renderCurvedCrescent(matrix, consumer, 1.7f, 1.85f, 1.5f, 0.15f, 0.16f, 220, 20, 40, 240);
        } else {
            // Shikai Getsuga Tenshō:
            // Layer A: Outer Radiant Azure Energy Aura (glowing cyan edge)
            renderCurvedCrescent(matrix, consumer, 2.1f, 2.2f + pulse, 1.2f, 0.45f, 0.08f, 30, 180, 255, 190);

            // Layer B: Pure White Soul Blade Core (blinding razor sharpness)
            renderCurvedCrescent(matrix, consumer, 1.9f, 2.0f, 1.35f, 0.25f, 0.12f, 255, 255, 255, 255);

            // Layer C: Electric Cyan Trailing Edge
            renderCurvedCrescent(matrix, consumer, 1.7f, 1.85f, 1.5f, 0.15f, 0.15f, 120, 225, 255, 220);
        }

        poseStack.popPose();
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    /**
     * Renders a symmetric, razor-sharp 3D forward-curving energy crescent.
     * Local coordinate space:
     * +Z = Forward flight direction (apex of curve leads in +Z)
     * +X = Width right wing
     * -X = Width left wing
     * +Y / -Y = Blade thickness profile (diamond cross-section)
     */
    private void renderCurvedCrescent(Matrix4f matrix, VertexConsumer consumer,
                                     float spanAngle, float outerRadius, float innerRadius,
                                     float trailLength, float thickness,
                                     int r, int g, int b, int a) {
        int segments = 22;
        int light = 0xF000F0; // Full emissive brightness

        // Center reference: apex reaches forward, wings curve back
        float zApexOffset = -outerRadius * 0.70f;

        for (int i = 0; i < segments; i++) {
            float t0 = (float) i / segments;
            float t1 = (float) (i + 1) / segments;

            float a0 = (t0 - 0.5f) * spanAngle;
            float a1 = (t1 - 0.5f) * spanAngle;

            float taper0 = (float) Math.sin(t0 * Math.PI);
            float taper1 = (float) Math.sin(t1 * Math.PI);

            // Leading cutting edge (sweeps forward towards center a0=0)
            float xOut0 = (float) (Math.sin(a0) * outerRadius);
            float zOut0 = (float) (Math.cos(a0) * outerRadius) + zApexOffset;

            float xOut1 = (float) (Math.sin(a1) * outerRadius);
            float zOut1 = (float) (Math.cos(a1) * outerRadius) + zApexOffset;

            // Trailing edge
            float depth0 = (outerRadius - innerRadius) * (0.6f + taper0 * 0.4f);
            float depth1 = (outerRadius - innerRadius) * (0.6f + taper1 * 0.4f);

            float xIn0 = (float) (Math.sin(a0) * (outerRadius - depth0));
            float zIn0 = (float) (Math.cos(a0) * (outerRadius - depth0)) + zApexOffset - taper0 * trailLength;

            float xIn1 = (float) (Math.sin(a1) * (outerRadius - depth1));
            float zIn1 = (float) (Math.cos(a1) * (outerRadius - depth1)) + zApexOffset - taper1 * trailLength;

            float yTop0 = thickness * taper0 * 0.5f;
            float yTop1 = thickness * taper1 * 0.5f;
            float yBot0 = -thickness * taper0 * 0.5f;
            float yBot1 = -thickness * taper1 * 0.5f;

            // 1. Top Surface
            consumer.addVertex(matrix, xOut0, yTop0, zOut0).setColor(r, g, b, a).setUv(t0, 0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(0, 1, 0);
            consumer.addVertex(matrix, xOut1, yTop1, zOut1).setColor(r, g, b, a).setUv(t1, 0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(0, 1, 0);
            consumer.addVertex(matrix, xIn1, 0, zIn1).setColor(r, g, b, a).setUv(t1, 1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(0, 1, 0);
            consumer.addVertex(matrix, xIn0, 0, zIn0).setColor(r, g, b, a).setUv(t0, 1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(0, 1, 0);

            // 2. Bottom Surface
            consumer.addVertex(matrix, xIn0, 0, zIn0).setColor(r, g, b, a).setUv(t0, 1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(0, -1, 0);
            consumer.addVertex(matrix, xIn1, 0, zIn1).setColor(r, g, b, a).setUv(t1, 1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(0, -1, 0);
            consumer.addVertex(matrix, xOut1, yBot1, zOut1).setColor(r, g, b, a).setUv(t1, 0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(0, -1, 0);
            consumer.addVertex(matrix, xOut0, yBot0, zOut0).setColor(r, g, b, a).setUv(t0, 0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(0, -1, 0);
        }
    }
}
