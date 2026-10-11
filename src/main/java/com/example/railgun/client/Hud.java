package com.example.railgun.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.gui.GuiGraphics;

import java.util.Random;

/** 2D overlay helpers: speed lines, lightning bolts, vignettes, fire. */
public final class Hud {
    private Hud() {}

    public static void speedLines(GuiGraphics g, int w, int h, int color, int count, long seed, float inner) {
        Random r = new Random(seed);
        float maxR = (float) Math.hypot(w, h) / 2f;
        PoseStack ps = g.pose();
        ps.pushPose();
        ps.translate(w / 2f, h / 2f, 0);
        for (int i = 0; i < count; i++) {
            float angle = r.nextFloat() * 360f;
            int start = (int) (maxR * (inner + r.nextFloat() * 0.3f));
            int th = 1 + r.nextInt(3);
            ps.pushPose();
            ps.mulPose(Axis.ZP.rotationDegrees(angle));
            g.fill(start, -th, (int) maxR, th, color);
            ps.popPose();
        }
        ps.popPose();
    }

    /** Jagged lightning bolts radiating from screen centre. Same seed = same paths, so layers line up. */
    public static void bolts(GuiGraphics g, int w, int h, int color, int thick, int count, long seed) {
        Random r = new Random(seed);
        PoseStack ps = g.pose();
        for (int b = 0; b < count; b++) {
            float x = w / 2f, y = h / 2f, ang = r.nextFloat() * 360f;
            for (int s = 0; s < 14; s++) {
                float len = 18 + r.nextFloat() * 34;
                ps.pushPose();
                ps.translate(x, y, 0);
                ps.mulPose(Axis.ZP.rotationDegrees(ang));
                g.fill(0, -thick, (int) len, thick, color);
                ps.popPose();
                double rad = Math.toRadians(ang);
                x += (float) Math.cos(rad) * len;
                y += (float) Math.sin(rad) * len;
                ang += (r.nextFloat() - .5f) * 80f;
                if (x < -50 || x > w + 50 || y < -50 || y > h + 50) break;
            }
        }
    }

    /** One white lightning streak racing in from the bottom-right corner. */
    public static void streak(GuiGraphics g, int w, int h, long seed) {
        Random r = new Random(seed);
        float x = w, y = h * 0.9f, ang = 195f;
        PoseStack ps = g.pose();
        for (int s = 0; s < 22; s++) {
            float len = 16 + r.nextFloat() * 22;
            ps.pushPose();
            ps.translate(x, y, 0);
            ps.mulPose(Axis.ZP.rotationDegrees(ang));
            g.fill(0, -2, (int) len, 2, 0xFFFFFFFF);
            ps.popPose();
            double rad = Math.toRadians(ang);
            x += (float) Math.cos(rad) * len;
            y += (float) Math.sin(rad) * len;
            ang = Math.max(150f, Math.min(230f, ang + (r.nextFloat() - .5f) * 70f));
        }
    }

    /** Dismantle-style cuts: a black slit with soft glowing rims (rim = 0 for a plain solid cut). */
    public static void cuts(GuiGraphics g, int w, int h, long seed, int count, int minLen, int maxLen, int thick, int core, int rim) {
        Random r = new Random(seed);
        PoseStack ps = g.pose();
        for (int i = 0; i < count; i++) {
            float x = r.nextFloat() * w, y = r.nextFloat() * h, ang = r.nextFloat() * 180f;
            int len = minLen + r.nextInt(Math.max(1, maxLen - minLen));
            ps.pushPose();
            ps.translate(x, y, 0);
            ps.mulPose(Axis.ZP.rotationDegrees(ang));
            if (rim != 0) {
                g.fill(0, -(thick + 8), len, thick + 8, (rim & 0xFFFFFF) | (0x3C << 24));
                g.fill(0, -(thick + 3), len, thick + 3, (rim & 0xFFFFFF) | (0xB4 << 24));
            }
            g.fill(0, -thick, len, thick, core);
            ps.popPose();
        }
    }

    public static void vignette(GuiGraphics g, int w, int h, int rgb, int alpha) {
        g.fillGradient(0, 0, w, h / 3, (alpha << 24) | rgb, rgb);
        g.fillGradient(0, h * 2 / 3, w, h, rgb, (alpha << 24) | rgb);
    }

    /** Fire licking up from the bottom and down from the top across the whole screen. */
    public static void flames(GuiGraphics g, int w, int h, float inten, long seed) {
        if (inten <= 0.01f) return;
        g.fill(0, 0, w, h, (((int) (inten * 70)) << 24) | 0xFF2000);
        for (int x = 0; x < w; x += 6) {
            Random r = new Random(seed * 31 + x * 17L);
            int ht = (int) (h * (0.25f + 0.6f * r.nextFloat()) * inten);
            g.fillGradient(x, h - ht, x + 6, h, 0x00FF4000, 0xFFFF5A00);
            int ht2 = (int) (ht * 0.6f);
            g.fillGradient(x + 1, h - ht2, x + 5, h, 0x00FFD000, 0xFFFFE070);
            int ht3 = (int) (h * (0.1f + 0.3f * r.nextFloat()) * inten);
            g.fillGradient(x, 0, x + 6, ht3, 0xFFFF3A00, 0x00FF3A00);
        }
    }
}
