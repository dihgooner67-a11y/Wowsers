package com.leafia.cursed.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import dev.golem.core.render.CompatRenderTypes;
import dev.golem.core.render.SubmitSink;
import dev.golem.core.render.Vertexes;
import java.awt.Color;
import java.util.Random;
import net.minecraft.client.renderer.RenderType;

/**
 * Client only. Draws the DFC as a faceted gem (6-sided double pyramid with a band) instead of Continuum's orb.
 * Colours: DEFAULT (Continuum's fuel colour), SOLID, GRADIENT (A at the top, B at the bottom) or RAINBOW.
 * Cracks appear as bright lines, and from about 70% crack the facets drift apart before the shatter.
 */
public final class CrystalRenderer {
    private CrystalRenderer() {}

    private static final int SIDES = 6;
    private static final float LIGHT_X = 0.35f, LIGHT_Y = 0.8f, LIGHT_Z = 0.5f;

    public static void draw(PoseStack ps, SubmitSink col, int mode, int colorA, int colorB, int baseColor,
                            float fill, float crack, long gameTime, boolean standby) {
        RenderType type = CompatRenderTypes.lightning();
        float half = standby ? 0.3f : 0.45f + 1.05f * Math.max(0.0f, Math.min(fill, 1.0f));
        float width = half * 0.55f;
        float spin = gameTime * (standby ? 0.01f : 0.03f);
        float bob = standby ? 0.0f : (float) Math.sin(gameTime * 0.05) * 0.04f;
        float pulse = 0.85f + 0.15f * (float) Math.sin(gameTime * (0.12 + crack * 0.6));
        int base = standby ? 0x555560 : (baseColor == 0 ? 0xE0E0FF : baseColor);
        int m = standby ? 0 : mode;

        ps.pushPose();
        ps.translate(0.0f, bob, 0.0f);
        ps.mulPose(Axis.YP.rotation(spin));

        final float fHalf = half, fWidth = width, fPulse = pulse, fCrack = crack;
        final int fBase = base, fMode = m;
        final long t = gameTime;
        col.submitCustomGeometry(ps, type, (pose, buf) -> gem(pose, buf, fMode, colorA, colorB, fBase, fHalf, fWidth, fCrack, fPulse, t, 1.0f, 1.0f));
        ps.pushPose();
        ps.scale(1.2f, 1.12f, 1.2f);
        col.submitCustomGeometry(ps, type, (pose, buf) -> gem(pose, buf, fMode, colorA, colorB, fBase, fHalf, fWidth, fCrack, fPulse, t, 0.22f, 0.0f));
        ps.popPose();
        if (crack > 0.02f) {
            col.submitCustomGeometry(ps, type, (pose, buf) -> cracks(pose, buf, fHalf, fWidth, fCrack, fPulse, t));
        }
        ps.popPose();
    }

    // ------------------------------------------------------------------ geometry

    private static float[][] ring(float y, float radius, float angleOffset) {
        float[][] r = new float[SIDES][];
        for (int i = 0; i < SIDES; i++) {
            double a = angleOffset + i * Math.PI * 2.0 / SIDES;
            r[i] = new float[]{(float) Math.cos(a) * radius, y, (float) Math.sin(a) * radius};
        }
        return r;
    }

    private static void gem(PoseStack.Pose pose, VertexConsumer buf, int mode, int ca, int cb, int base, float half, float width,
                            float crack, float pulse, long time, float brightness, float explode) {
        float[] top = {0.0f, half, 0.0f}, bottom = {0.0f, -half, 0.0f};
        float[][] up = ring(half * 0.35f, width, 0.0f);
        float[][] lo = ring(-half * 0.35f, width, (float) (Math.PI / SIDES));
        for (int i = 0; i < SIDES; i++) {
            int j = (i + 1) % SIDES;
            facet(pose, buf, top, up[i], up[j], mode, ca, cb, base, half, crack, pulse, time, brightness, explode);
            facet(pose, buf, up[i], lo[i], up[j], mode, ca, cb, base, half, crack, pulse, time, brightness, explode);
            facet(pose, buf, lo[i], up[j], lo[j], mode, ca, cb, base, half, crack, pulse, time, brightness, explode);
            facet(pose, buf, bottom, lo[j], lo[i], mode, ca, cb, base, half, crack, pulse, time, brightness, explode);
        }
    }

    private static void facet(PoseStack.Pose pose, VertexConsumer buf, float[] a, float[] b, float[] c, int mode, int ca, int cb, int base,
                              float half, float crack, float pulse, long time, float brightness, float explode) {
        // normal for shading and for drifting apart
        float ux = b[0] - a[0], uy = b[1] - a[1], uz = b[2] - a[2], vx = c[0] - a[0], vy = c[1] - a[1], vz = c[2] - a[2];
        float nx = uy * vz - uz * vy, ny = uz * vx - ux * vz, nz = ux * vy - uy * vx;
        float len = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
        if (len < 1.0E-6f) return;
        nx /= len; ny /= len; nz /= len;
        // make the normal point away from the centre
        float cx = (a[0] + b[0] + c[0]) / 3.0f, cy = (a[1] + b[1] + c[1]) / 3.0f, cz = (a[2] + b[2] + c[2]) / 3.0f;
        if (nx * cx + ny * cy + nz * cz < 0.0f) { nx = -nx; ny = -ny; nz = -nz; }
        float shade = 0.4f + 0.6f * Math.abs(nx * LIGHT_X + ny * LIGHT_Y + nz * LIGHT_Z);

        float drift = explode * Math.max(0.0f, crack - 0.7f) * 1.6f;
        float jitter = explode * Math.max(0.0f, crack - 0.45f) * 0.05f;
        long seed = Float.floatToIntBits(cx * 31.0f + cy * 17.0f + cz * 13.0f) * 31L + time / 2L;
        Random r = new Random(seed);
        float ox = nx * drift + (r.nextFloat() - 0.5f) * jitter;
        float oy = ny * drift + (r.nextFloat() - 0.5f) * jitter;
        float oz = nz * drift + (r.nextFloat() - 0.5f) * jitter;

        float k = shade * pulse * brightness;
        put(pose, buf, a, ox, oy, oz, color(mode, ca, cb, base, a[1] / half, time, k));
        put(pose, buf, b, ox, oy, oz, color(mode, ca, cb, base, b[1] / half, time, k));
        put(pose, buf, c, ox, oy, oz, color(mode, ca, cb, base, c[1] / half, time, k));
        // second winding so both sides show
        put(pose, buf, a, ox, oy, oz, color(mode, ca, cb, base, a[1] / half, time, k));
        put(pose, buf, c, ox, oy, oz, color(mode, ca, cb, base, c[1] / half, time, k));
        put(pose, buf, b, ox, oy, oz, color(mode, ca, cb, base, b[1] / half, time, k));
    }

    private static void put(PoseStack.Pose pose, VertexConsumer buf, float[] p, float ox, float oy, float oz, int argb) {
        Vertexes.color(buf, pose, p[0] + ox, p[1] + oy, p[2] + oz, argb);
    }

    // ------------------------------------------------------------------ colours

    /** y01 is -1 (bottom) .. +1 (top). */
    private static int color(int mode, int ca, int cb, int base, float y, long time, float k) {
        float t = Math.max(0.0f, Math.min(1.0f, (y + 1.0f) * 0.5f));
        int rgb;
        switch (mode) {
            case 1 -> rgb = ca;
            case 2 -> rgb = lerp(cb, ca, t);
            case 3 -> rgb = Color.HSBtoRGB(((t * 0.6f + time * 0.004f) % 1.0f + 1.0f) % 1.0f, 0.85f, 1.0f);
            default -> rgb = base;
        }
        return dim(rgb, k);
    }

    private static int lerp(int from, int to, float t) {
        int r = (int) (((from >> 16) & 0xFF) * (1.0f - t) + ((to >> 16) & 0xFF) * t);
        int g = (int) (((from >> 8) & 0xFF) * (1.0f - t) + ((to >> 8) & 0xFF) * t);
        int b = (int) ((from & 0xFF) * (1.0f - t) + (to & 0xFF) * t);
        return (r << 16) | (g << 8) | b;
    }

    private static int dim(int rgb, float k) {
        k = Math.max(0.0f, Math.min(k, 1.5f));
        int r = Math.min(255, (int) (((rgb >> 16) & 0xFF) * k));
        int g = Math.min(255, (int) (((rgb >> 8) & 0xFF) * k));
        int b = Math.min(255, (int) ((rgb & 0xFF) * k));
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    // ------------------------------------------------------------------ cracks

    private static void cracks(PoseStack.Pose pose, VertexConsumer buf, float half, float width, float crack, float pulse, long time) {
        float[][] up = ring(half * 0.35f, width, 0.0f);
        float[][] lo = ring(-half * 0.35f, width, (float) (Math.PI / SIDES));
        float[] top = {0.0f, half, 0.0f}, bottom = {0.0f, -half, 0.0f};
        int n = 1 + (int) (crack * 14.0f);
        Random r = new Random(77L);
        int argb = dim(0xFFFFFF, pulse * (0.6f + crack * 0.6f));
        float drift = Math.max(0.0f, crack - 0.7f) * 0.5f;
        for (int i = 0; i < n; i++) {
            float[] p = pick(r, top, up, lo, bottom), q = pick(r, top, up, lo, bottom);
            float s = 1.0f + drift;
            line(pose, buf, p[0] * s, p[1] * s, p[2] * s, q[0] * s, q[1] * s, q[2] * s, 0.012f + crack * 0.012f, argb);
        }
    }

    private static float[] pick(Random r, float[] top, float[][] up, float[][] lo, float[] bottom) {
        int k = r.nextInt(2 * SIDES + 2);
        if (k == 2 * SIDES) return top;
        if (k == 2 * SIDES + 1) return bottom;
        return k < SIDES ? up[k] : lo[k - SIDES];
    }

    private static void line(PoseStack.Pose pose, VertexConsumer buf, float x0, float y0, float z0, float x1, float y1, float z1, float rad, int argb) {
        quad(pose, buf, argb, x0 + rad, y0, z0, x0 - rad, y0, z0, x1 - rad, y1, z1, x1 + rad, y1, z1);
        quad(pose, buf, argb, x0, y0, z0 + rad, x0, y0, z0 - rad, x1, y1, z1 - rad, x1, y1, z1 + rad);
    }

    private static void quad(PoseStack.Pose pose, VertexConsumer buf, int argb, float ax, float ay, float az, float bx, float by, float bz,
                             float cx, float cy, float cz, float dx, float dy, float dz) {
        v(pose, buf, argb, ax, ay, az); v(pose, buf, argb, bx, by, bz); v(pose, buf, argb, cx, cy, cz);
        v(pose, buf, argb, ax, ay, az); v(pose, buf, argb, cx, cy, cz); v(pose, buf, argb, dx, dy, dz);
        v(pose, buf, argb, ax, ay, az); v(pose, buf, argb, dx, dy, dz); v(pose, buf, argb, cx, cy, cz);
        v(pose, buf, argb, ax, ay, az); v(pose, buf, argb, cx, cy, cz); v(pose, buf, argb, bx, by, bz);
    }

    private static void v(PoseStack.Pose pose, VertexConsumer buf, int argb, float x, float y, float z) {
        Vertexes.color(buf, pose, x, y, z, argb);
    }
}
