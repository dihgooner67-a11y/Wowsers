package com.example.railgun.client;

import com.example.railgun.FxOptions;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.phys.Vec3;

import java.util.Random;

/** Effect toolkit: glows, sparks, rings, shockwaves, smoke, flames and electricity built from our own textures. */
public final class Vfx {
    public static final int GLOW = 0, SPARK = 1, RING = 2, SMOKE = 3, SMOKE2 = 4, FLAME = 5, STREAK = 6, BOLT = 7, BOLT2 = 8, ARC = 9, SHOCK = 10, DISC = 11, SWIRL = 12, HALO = 13;
    public static final int PURPLE = 0xA040FF, VIOLET = 0xD9A8FF, DEEP = 0x5A1CB0, BLUE = 0x3C78FF, RED = 0xFF2A2A, ORANGE = 0xFF8A1C,
            YELLOW = 0xFFE070, WHITE = 0xFFFFFF, CYAN = 0x66E0FF, EMBER = 0xFF4A10, BLACK = 0x040406, GREY = 0x35353D, CRIMSON = 0xB00018;
    private static final Random R = new Random();

    private Vfx() {}

    public static double g(double s) { return R.nextGaussian() * s; }
    public static float rf(float a, float b) { return a + R.nextFloat() * (b - a); }
    public static Vec3 rnd(double s) { return new Vec3(g(s), g(s), g(s)); }
    public static Vec3 unit() { Vec3 v = rnd(1); return v.lengthSqr() < 1e-6 ? new Vec3(0, 1, 0) : v.normalize(); }
    public static boolean chance(double p) { return R.nextDouble() < p; }

    public static void add(int shape, int rgb, float size, int life, Vec3 p, Vec3 v, float drag, float grav,
                           float grow, float spin, float roll, boolean additive) {
        ClientLevel lv = Minecraft.getInstance().level;
        if (lv == null) return;
        lv.addParticle(new FxOptions(shape, rgb, size, life, drag, grav, grow, spin, roll, additive), true, p.x, p.y, p.z, v.x, v.y, v.z);
    }

    /** Soft shrinking glow (the basic building block of bloom). */
    public static void glow(int rgb, float size, int life, Vec3 p, Vec3 v) { add(GLOW, rgb, size, life, p, v, 0.9f, 0f, 0.96f, 0f, 0f, true); }
    /** Glow that holds its size, for beam cores. */
    public static void core(int rgb, float size, int life, Vec3 p) { add(GLOW, rgb, size, life, p, Vec3.ZERO, 1f, 0f, 1f, 0f, 0f, true); }
    public static void spark(int rgb, float size, int life, Vec3 p, Vec3 v) { add(SPARK, rgb, size, life, p, v, 0.9f, 0.05f, 0.95f, rf(-14, 14), R.nextFloat() * 360, true); }
    public static void ring(int rgb, float size, int life, Vec3 p, float grow) { add(RING, rgb, size, life, p, Vec3.ZERO, 1f, 0f, grow, 0f, R.nextFloat() * 360, true); }
    public static void shock(int rgb, float size, int life, Vec3 p, float grow) { add(SHOCK, rgb, size, life, p, Vec3.ZERO, 1f, 0f, grow, 0f, R.nextFloat() * 360, true); }
    public static void smoke(int rgb, float size, int life, Vec3 p, Vec3 v, boolean additive) {
        add(R.nextBoolean() ? SMOKE : SMOKE2, rgb, size, life, p, v, 0.93f, -0.02f, 1.04f, rf(-3, 3), R.nextFloat() * 360, additive);
    }
    public static void flame(int rgb, float size, int life, Vec3 p, Vec3 v) { add(FLAME, rgb, size, life, p, v, 0.9f, -0.03f, 0.95f, rf(-3, 3), rf(-20, 20), true); }
    public static void streak(int rgb, float size, int life, Vec3 p, float rollDeg) { add(STREAK, rgb, size, life, p, Vec3.ZERO, 1f, 0f, 0.97f, 0f, rollDeg, true); }
    /** One electric crackle sprite, randomly rotated. */
    public static void bolt(int rgb, float size, int life, Vec3 p) { add(R.nextBoolean() ? BOLT : BOLT2, rgb, size, life, p, Vec3.ZERO, 1f, 0f, 1f, 0f, R.nextFloat() * 360, true); }
    public static void boltDark(float size, int life, Vec3 p) { add(R.nextBoolean() ? BOLT : BOLT2, BLACK, size, life, p, Vec3.ZERO, 1f, 0f, 1f, 0f, R.nextFloat() * 360, false); }
    public static void arc(int rgb, float size, int life, Vec3 p) { add(ARC, rgb, size, life, p, Vec3.ZERO, 1f, 0f, 1f, 0f, R.nextFloat() * 360, true); }

    /** Solid black disc: overlapping discs read as a perfectly round black sphere from every angle. */
    public static void disc(float size, int life, Vec3 p) { add(DISC, BLACK, size, life, p, Vec3.ZERO, 1f, 0f, 1f, 0f, 0f, false); }
    /** Two-armed spiral glow, spun slowly. */
    public static void swirl(int rgb, float size, int life, Vec3 p, float spinDeg) { add(SWIRL, rgb, size, life, p, Vec3.ZERO, 1f, 0f, 1f, spinDeg, R.nextFloat() * 360, true); }
    /** Thin lensed photon ring. */
    public static void halo(int rgb, float size, int life, Vec3 p) { add(HALO, rgb, size, life, p, Vec3.ZERO, 1f, 0f, 1f, 0f, 0f, true); }

    /** A cloud of crackling electricity around a point. */
    public static void electric(Vec3 c, double spread, int rgb, int n) {
        for (int i = 0; i < n; i++) {
            Vec3 p = c.add(rnd(spread));
            float s = rf(0.5f, 1.4f);
            bolt(rgb, s, 3 + R.nextInt(3), p);
            if (chance(0.5)) bolt(WHITE, s * 0.55f, 3, p);
            if (chance(0.3)) arc(rgb, s * 0.9f, 4, p);
            if (chance(0.35)) spark(WHITE, 0.12f, 6, p, unit().scale(0.3));
        }
    }

    /** A long jagged lightning bolt: glowing path with white core and optional forks. */
    public static void lightning(Vec3 from, Vec3 dir, double len, int rgb, int forks) {
        Vec3 d = dir.normalize(), pos = from;
        double step = 0.8;
        int n = (int) (len / step);
        for (int i = 0; i < n; i++) {
            d = d.add(rnd(0.55)).normalize();
            pos = pos.add(d.scale(step));
            glow(rgb, 0.3f, 5, pos, Vec3.ZERO);
            if (i % 2 == 0) bolt(WHITE, 0.45f, 3, pos);
            if (forks > 0 && chance(0.12)) lightning(pos, d.add(rnd(0.9)), len * 0.35, rgb, forks - 1);
        }
    }

    public static void burst(Vec3 p, int rgb, int n, double speed, float size) {
        for (int i = 0; i < n; i++) {
            Vec3 v = unit().scale(speed * (0.3 + R.nextDouble()));
            if (i % 2 == 0) spark(rgb, size, 10 + R.nextInt(8), p, v); else glow(rgb, size * 1.6f, 8 + R.nextInt(6), p, v);
        }
    }
}
