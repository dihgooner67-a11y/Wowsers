package com.leafia.cursed.client;

import com.leafia.cursed.nuke.NukeTimings;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.golem.core.render.CompatRenderTypes;
import dev.golem.core.render.SubmitSink;
import dev.golem.core.render.Vertexes;
import java.util.Random;
import net.minecraft.client.renderer.RenderType;

/**
 * Client only. The nuke visuals that go on top of Continuum's torex:
 *  - plasma ball: every bomb starts as an expanding white-hot ball that cools to red while the cloud forms behind it
 *  - balefire: green-white ball, then a spiralling pillar of green fire and rings racing across the ground
 *  - antimatter: collapsing light, a burst of rays and expanding violet-white shells; no mushroom cloud
 * All drawn with Continuum's additive "lightning" render type. Sizes follow the torex scale (0.5 .. 5).
 */
public final class NukeEffects {
    private NukeEffects() {}

    private static final int LAT = 10, LON = 16, SIDES = 18;

    public static void draw(PoseStack ps, SubmitSink col, int type, double scale, float age, int maxAge) {
        RenderType rt = CompatRenderTypes.lightning();
        float s = (float) scale;
        switch (type) {
            case NukeTimings.TYPE_BALEFIRE -> col.submitCustomGeometry(ps, rt, (pose, buf) -> balefire(pose, buf, s, age, maxAge));
            case NukeTimings.TYPE_ANTIMATTER -> col.submitCustomGeometry(ps, rt, (pose, buf) -> antimatter(pose, buf, s, age, maxAge));
            default -> {
                if (age < NukeTimings.ballTicks(scale)) col.submitCustomGeometry(ps, rt, (pose, buf) -> plasmaBall(pose, buf, s, age, 0));
            }
        }
    }

    // ------------------------------------------------------------------ plasma ball

    /** variant 0 = fire (white -> yellow -> orange -> red), 1 = balefire green. */
    private static void plasmaBall(PoseStack.Pose pose, VertexConsumer buf, float scale, float age, int variant) {
        int ballTicks = NukeTimings.ballTicks(scale);
        float p = Math.min(age / ballTicks, 1.0f);
        float rmax = 10.0f * scale;
        float grow = 1.0f - (float) Math.pow(1.0f - Math.min(p / 0.7f, 1.0f), 2.2);
        float r = rmax * grow * (p > 0.7f ? 1.0f - 0.45f * (p - 0.7f) / 0.3f : 1.0f);
        float fade = p < 0.75f ? 1.0f : 1.0f - (p - 0.75f) / 0.25f;
        if (r < 0.05f || fade <= 0.0f) return;
        float flicker = 1.0f + 0.04f * (float) Math.sin(age * 1.7f);
        float yOff = rmax * 0.3f * (float) Math.sqrt(p);
        int hot = variant == 1 ? lerp(0xE8FFE8, 0x30C040, Math.min(p * 1.4f, 1.0f)) : heatColor(p);
        sphere(pose, buf, 0.0f, yOff, 0.0f, r * 0.8f * flicker, dim(hot, 1.0f * fade));
        sphere(pose, buf, 0.0f, yOff, 0.0f, r * 1.0f * flicker, dim(variant == 1 ? 0x40E060 : 0xFF8020, 0.5f * fade));
        sphere(pose, buf, 0.0f, yOff, 0.0f, r * 1.35f * flicker, dim(variant == 1 ? 0x30B050 : 0xFF5010, 0.22f * fade));
        sphere(pose, buf, 0.0f, yOff, 0.0f, r * 1.8f, dim(variant == 1 ? 0x208040 : 0xC03000, 0.1f * fade));
    }

    private static int heatColor(float p) {
        if (p < 0.2f) return lerp(0xFFFFFF, 0xFFF080, p / 0.2f);
        if (p < 0.45f) return lerp(0xFFF080, 0xFFA030, (p - 0.2f) / 0.25f);
        return lerp(0xFFA030, 0xD03010, Math.min((p - 0.45f) / 0.5f, 1.0f));
    }

    // ------------------------------------------------------------------ balefire

    private static void balefire(PoseStack.Pose pose, VertexConsumer buf, float scale, float age, int maxAge) {
        int ballTicks = (int) (NukeTimings.ballTicks(scale) * 0.8f);
        if (age < ballTicks) plasmaBall(pose, buf, scale * 0.8f, age * (NukeTimings.ballTicks(scale) * 0.8f > 0 ? NukeTimings.ballTicks(scale) / (float) ballTicks : 1.0f), 1);

        float riseStart = ballTicks * 0.25f;
        float lifeEnd = maxAge * 0.55f;
        if (age > riseStart && age < lifeEnd) {
            float grow = Math.min((age - riseStart) / (ballTicks * 3.0f), 1.0f);
            float h = 70.0f * scale * (1.0f - (float) Math.pow(1.0f - grow, 2.0));
            float fadeOut = age > lifeEnd - 80.0f ? Math.max(0.0f, (lifeEnd - age) / 80.0f) : 1.0f;
            float r0 = 2.6f * scale * (1.0f + 0.15f * (float) Math.sin(age * 0.3f));
            pillar(pose, buf, h, r0, age, fadeOut);
            ribbons(pose, buf, h, r0, age, fadeOut);
        }
        // rings racing across the ground
        for (int i = 0; i < 6; i++) {
            float born = ballTicks * 0.3f + i * 14.0f;
            float t = age - born;
            if (t < 0.0f || t > 90.0f) continue;
            float radius = (4.0f + t * 1.6f) * scale;
            float a = 1.0f - t / 90.0f;
            annulus(pose, buf, radius, radius + 1.2f * scale, 0.25f, dim(0x60FF80, 0.65f * a));
        }
    }

    private static void pillar(PoseStack.Pose pose, VertexConsumer buf, float h, float r0, float age, float fade) {
        int rows = 14;
        for (int row = 0; row < rows; row++) {
            float y0 = h * row / rows, y1 = h * (row + 1) / rows;
            float t0 = (float) row / rows, t1 = (float) (row + 1) / rows;
            float ra = r0 * (1.0f - 0.55f * t0), rb = r0 * (1.0f - 0.55f * t1);
            int ca = dim(lerp(0xE0FFE0, 0x20A040, t0), (0.5f - 0.35f * t0) * fade);
            int cb = dim(lerp(0xE0FFE0, 0x20A040, t1), (0.5f - 0.35f * t1) * fade);
            for (int i = 0; i < SIDES; i++) {
                double a0 = i * Math.PI * 2.0 / SIDES + age * 0.01, a1 = (i + 1) * Math.PI * 2.0 / SIDES + age * 0.01;
                float x0a = (float) Math.cos(a0) * ra, z0a = (float) Math.sin(a0) * ra, x1a = (float) Math.cos(a1) * ra, z1a = (float) Math.sin(a1) * ra;
                float x0b = (float) Math.cos(a0) * rb, z0b = (float) Math.sin(a0) * rb, x1b = (float) Math.cos(a1) * rb, z1b = (float) Math.sin(a1) * rb;
                tri2(pose, buf, x0a, y0, z0a, ca, x1a, y0, z1a, ca, x1b, y1, z1b, cb);
                tri2(pose, buf, x0a, y0, z0a, ca, x1b, y1, z1b, cb, x0b, y1, z0b, cb);
            }
        }
    }

    /** Four spiralling flame ribbons wound around the pillar. */
    private static void ribbons(PoseStack.Pose pose, VertexConsumer buf, float h, float r0, float age, float fade) {
        int steps = 40;
        for (int k = 0; k < 4; k++) {
            float prevX = 0, prevY = 0, prevZ = 0;
            for (int i = 0; i <= steps; i++) {
                float t = (float) i / steps;
                double a = age * 0.06 + k * Math.PI / 2.0 + t * 9.0;
                float rad = r0 * (1.25f - 0.6f * t);
                float x = (float) Math.cos(a) * rad, y = h * t, z = (float) Math.sin(a) * rad;
                if (i > 0) {
                    float w = 0.35f * r0 * (1.0f - 0.6f * t);
                    int c = dim(lerp(0xFFFFFF, 0x30D060, t), (0.9f - 0.5f * t) * fade);
                    tri2(pose, buf, prevX - w, prevY, prevZ, c, prevX + w, prevY, prevZ, c, x + w, y, z, c);
                    tri2(pose, buf, prevX - w, prevY, prevZ, c, x + w, y, z, c, x - w, y, z, c);
                }
                prevX = x; prevY = y; prevZ = z;
            }
        }
    }

    // ------------------------------------------------------------------ antimatter

    private static void antimatter(PoseStack.Pose pose, VertexConsumer buf, float scale, float age, int maxAge) {
        int ball = NukeTimings.ballTicks(scale);
        float rmax = 10.0f * scale;
        float yOff = rmax * 0.4f;
        float collapse = Math.min(age / (ball * 0.5f), 1.0f);
        if (age < ball * 0.5f) { // collapsing: a dark core inside a shrinking ring of light
            float rr = rmax * 0.9f * (1.0f - collapse * 0.85f);
            sphere(pose, buf, 0.0f, yOff, 0.0f, rr * 0.35f, dim(0x180020, 1.0f));
            annulus(pose, buf, rr, rr + 0.5f * scale, yOff, dim(0xF0E0FF, 1.0f));
            ringVertical(pose, buf, rr, 0.5f * scale, yOff, dim(0xC8A0FF, 0.8f));
        }
        float burst = (age - ball * 0.5f) / (ball * 1.2f);
        if (burst >= 0.0f) {
            float ease = 1.0f - (float) Math.pow(1.0f - Math.min(burst, 1.0f), 2.5);
            float fade = Math.max(0.0f, 1.0f - Math.max(0.0f, burst - 0.6f) / 1.4f);
            if (fade > 0.0f) {
                float r = rmax * (0.5f + 9.0f * ease);
                sphere(pose, buf, 0.0f, yOff, 0.0f, r, dim(0xB090FF, 0.22f * fade));
                sphere(pose, buf, 0.0f, yOff, 0.0f, r * 0.82f, dim(0xFFFFFF, 0.3f * fade));
                sphere(pose, buf, 0.0f, yOff, 0.0f, r * 0.35f, dim(0xFFFFFF, 0.55f * fade));
                rays(pose, buf, yOff, rmax, ease, fade);
            }
        }
    }

    private static void rays(PoseStack.Pose pose, VertexConsumer buf, float yOff, float rmax, float ease, float fade) {
        Random rnd = new Random(1234L);
        int n = 110;
        for (int i = 0; i < n; i++) {
            double a = rnd.nextDouble() * Math.PI * 2.0, c = rnd.nextDouble() * 2.0 - 1.0, sn = Math.sqrt(1.0 - c * c);
            float dx = (float) (Math.cos(a) * sn), dy = (float) c, dz = (float) (Math.sin(a) * sn);
            float len = rmax * (2.0f + rnd.nextFloat() * 14.0f) * ease;
            float w = rmax * (0.04f + rnd.nextFloat() * 0.05f);
            // perpendicular
            float px = -dz, pz = dx, pl = (float) Math.sqrt(px * px + pz * pz);
            if (pl < 1.0E-4f) { px = 1.0f; pz = 0.0f; pl = 1.0f; }
            px = px / pl * w; pz = pz / pl * w;
            int core = dim(0xFFFFFF, 0.9f * fade), tip = dim(0x9060FF, 0.0f);
            tri2(pose, buf, px, yOff, pz, core, -px, yOff, -pz, core, dx * len, yOff + dy * len, dz * len, tip);
        }
    }

    // ------------------------------------------------------------------ primitives

    private static void sphere(PoseStack.Pose pose, VertexConsumer buf, float cx, float cy, float cz, float r, int argb) {
        for (int i = 0; i < LAT; i++) {
            double t0 = Math.PI * i / LAT, t1 = Math.PI * (i + 1) / LAT;
            for (int j = 0; j < LON; j++) {
                double p0 = Math.PI * 2.0 * j / LON, p1 = Math.PI * 2.0 * (j + 1) / LON;
                float[] a = sp(cx, cy, cz, r, t0, p0), b = sp(cx, cy, cz, r, t0, p1), c = sp(cx, cy, cz, r, t1, p1), d = sp(cx, cy, cz, r, t1, p0);
                tri2(pose, buf, a[0], a[1], a[2], argb, b[0], b[1], b[2], argb, c[0], c[1], c[2], argb);
                tri2(pose, buf, a[0], a[1], a[2], argb, c[0], c[1], c[2], argb, d[0], d[1], d[2], argb);
            }
        }
    }

    private static float[] sp(float cx, float cy, float cz, float r, double theta, double phi) {
        return new float[]{cx + r * (float) (Math.sin(theta) * Math.cos(phi)), cy + r * (float) Math.cos(theta), cz + r * (float) (Math.sin(theta) * Math.sin(phi))};
    }

    /** Flat ring in the horizontal plane at height y. */
    private static void annulus(PoseStack.Pose pose, VertexConsumer buf, float r0, float r1, float y, int argb) {
        for (int i = 0; i < 48; i++) {
            double a0 = Math.PI * 2.0 * i / 48, a1 = Math.PI * 2.0 * (i + 1) / 48;
            float ax = (float) Math.cos(a0), az = (float) Math.sin(a0), bx = (float) Math.cos(a1), bz = (float) Math.sin(a1);
            tri2(pose, buf, ax * r0, y, az * r0, argb, ax * r1, y, az * r1, argb, bx * r1, y, bz * r1, argb);
            tri2(pose, buf, ax * r0, y, az * r0, argb, bx * r1, y, bz * r1, argb, bx * r0, y, bz * r0, argb);
        }
    }

    /** Flat ring standing up in the x/y plane, centred at height cy. */
    private static void ringVertical(PoseStack.Pose pose, VertexConsumer buf, float r, float w, float cy, int argb) {
        for (int i = 0; i < 48; i++) {
            double a0 = Math.PI * 2.0 * i / 48, a1 = Math.PI * 2.0 * (i + 1) / 48;
            float ax = (float) Math.cos(a0), ay = (float) Math.sin(a0), bx = (float) Math.cos(a1), by = (float) Math.sin(a1);
            tri2(pose, buf, ax * r, cy + ay * r, 0.0f, argb, ax * (r + w), cy + ay * (r + w), 0.0f, argb, bx * (r + w), cy + by * (r + w), 0.0f, argb);
            tri2(pose, buf, ax * r, cy + ay * r, 0.0f, argb, bx * (r + w), cy + by * (r + w), 0.0f, argb, bx * r, cy + by * r, 0.0f, argb);
        }
    }

    /** One triangle, emitted with both windings so it shows from either side. */
    private static void tri2(PoseStack.Pose pose, VertexConsumer buf, float ax, float ay, float az, int ca, float bx, float by, float bz, int cb,
                             float cx, float cy, float cz, int cc) {
        Vertexes.color(buf, pose, ax, ay, az, ca); Vertexes.color(buf, pose, bx, by, bz, cb); Vertexes.color(buf, pose, cx, cy, cz, cc);
        Vertexes.color(buf, pose, ax, ay, az, ca); Vertexes.color(buf, pose, cx, cy, cz, cc); Vertexes.color(buf, pose, bx, by, bz, cb);
    }

    private static int lerp(int from, int to, float t) {
        t = Math.max(0.0f, Math.min(1.0f, t));
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
}
