package com.zangetsu.client;

import com.zangetsu.init.ModDataAttachments;
import com.zangetsu.init.ModItems;
import com.zangetsu.init.ReiatsuData;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class ReiatsuHudOverlay implements LayeredDraw.Layer {
    @Override
    public void render(GuiGraphics guiGraphics, DeltaTracker deltaTracker) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null || mc.options.hideGui) return;

        ItemStack mainHand = player.getMainHandItem();
        ItemStack offHand = player.getOffhandItem();
        boolean isShikai = mainHand.is(ModItems.ZANGETSU_SHIKAI.get()) || offHand.is(ModItems.ZANGETSU_SHIKAI.get());
        boolean isBankai = mainHand.is(ModItems.TENSA_ZANGETSU.get()) || offHand.is(ModItems.TENSA_ZANGETSU.get());
        boolean isAsauchi = mainHand.is(ModItems.ASAUCHI.get()) || offHand.is(ModItems.ASAUCHI.get());

        ReiatsuData data = player.getData(ModDataAttachments.REIATSU);
        if (!isShikai && !isBankai && !isAsauchi && !data.isInfusionActive()) return;

        float current = data.getCurrent();
        float max = data.getMax();
        float ratio = Math.max(0.0f, Math.min(1.0f, current / max));

        // Top-left RPG HUD position (away from vanilla health, armor, food, and chat)
        int x = 12;
        int y = 12;
        int boxWidth = 158;
        int boxHeight = 40;

        // Shadow & Backdrop frame
        guiGraphics.fill(x - 2, y - 2, x + boxWidth + 2, y + boxHeight + 2, 0x66000000);
        guiGraphics.fill(x - 1, y - 1, x + boxWidth + 1, y + boxHeight + 1, 0xBB181A24);
        guiGraphics.fill(x, y, x + boxWidth, y + boxHeight, 0xDC0C0D16);

        // Form accent border (top line and left edge)
        int accentColor;
        if (data.isInfusionActive()) {
            accentColor = 0xFFFF2233;
        } else if (isBankai) {
            accentColor = 0xFFCC0022;
        } else if (isShikai) {
            accentColor = 0xFF00B4D8;
        } else {
            accentColor = 0xFF888899;
        }
        guiGraphics.fill(x, y, x + boxWidth, y + 1, accentColor);
        guiGraphics.fill(x, y, x + 3, y + boxHeight, accentColor);

        // --- ROW 1: Form Badge & Reiatsu Numeric Text ---
        String formBadge;
        if (isBankai) {
            formBadge = "§4§lBANKAI";
        } else if (isShikai) {
            formBadge = "§b§lSHIKAI";
        } else {
            formBadge = "§7§lASAUCHI";
        }

        if (data.isInfusionActive()) {
            boolean pulse = (System.currentTimeMillis() / 250) % 2 == 0;
            formBadge += pulse ? " §c§l🔥 INFUSION" : " §6§l🔥 INFUSION";
        }
        guiGraphics.drawString(mc.font, formBadge, x + 7, y + 5, 0xFFFFFFFF, true);

        String reiatsuText = "§f" + (int) current + " §7/ §f" + (int) max;
        int textWidth = mc.font.width(reiatsuText);
        guiGraphics.drawString(mc.font, reiatsuText, x + boxWidth - 6 - textWidth, y + 5, 0xFFFFFFFF, true);

        // --- ROW 2: Reiatsu Energy Bar ---
        int barX = x + 7;
        int barY = y + 17;
        int barWidth = boxWidth - 14; // 144px
        int barHeight = 7;

        // Dark track background
        guiGraphics.fill(barX - 1, barY - 1, barX + barWidth + 1, barY + barHeight + 1, 0xFF161824);
        guiGraphics.fill(barX, barY, barX + barWidth, barY + barHeight, 0xFF080910);

        int fillWidth = (int) (barWidth * ratio);
        if (fillWidth > 0) {
            int barTopColor;
            int barBottomColor;
            if (data.isInfusionActive()) {
                barTopColor = 0xFFFF4D00;
                barBottomColor = 0xFFCC0020;
            } else if (isBankai) {
                barTopColor = 0xFFFF1E38;
                barBottomColor = 0xFF8A0012;
            } else if (isShikai) {
                barTopColor = 0xFF5CE1E6;
                barBottomColor = 0xFF0077B6;
            } else {
                barTopColor = 0xFFAAAAAA;
                barBottomColor = 0xFF555555;
            }

            int halfH = barHeight / 2;
            guiGraphics.fill(barX, barY, barX + fillWidth, barY + halfH, barTopColor);
            guiGraphics.fill(barX, barY + halfH, barX + fillWidth, barY + barHeight, barBottomColor);

            // Subtle gloss highlight on top line
            guiGraphics.fill(barX, barY, barX + fillWidth, barY + 1, 0x88FFFFFF);
        }

        // --- ROW 3: Controls, Orientation & Dynamic Skills ---
        boolean vertical = player.getPersistentData().getBoolean("ZangetsuSlashVertical");
        String slashText = vertical ? "§3[R] §bPionowe ↕" : "§6[R] §ePoziome ↔";
        guiGraphics.drawString(mc.font, slashText, x + 7, y + 27, 0xFFFFFFFF, true);

        // Dynamic Right-side Action/Status
        String actionTag;
        if (player.isUsingItem() && (isShikai || isBankai)) {
            int duration = player.getTicksUsingItem();
            float charge = Math.min(1.0f, (float) duration / 30.0f);
            actionTag = "§e§lŁADUNEK: " + (int) (charge * 100) + "%";
        } else if (data.isInfusionActive()) {
            actionTag = "§c§l[V] DRAIN -4/s";
        } else if (isBankai) {
            actionTag = "§d[X] Shunpo §7| §c[V] Inf";
        } else if (isShikai) {
            actionTag = "§bShift+PPM: Getsuga";
        } else {
            actionTag = "§7Medytacja (Dywany)";
        }

        int actionWidth = mc.font.width(actionTag);
        guiGraphics.drawString(mc.font, actionTag, x + boxWidth - 6 - actionWidth, y + 27, 0xFFFFFFFF, true);
    }
}
