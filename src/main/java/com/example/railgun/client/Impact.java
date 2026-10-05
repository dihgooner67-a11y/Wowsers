package com.example.railgun.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;

import java.util.Random;

/**
 * Impact frames. During a hard frame the screen post-filter (PostFx) recolours the shading of your actual view
 * into the frame's palette (flipping between normal and inverted every ~25 ms), then it fades out.
 * Palettes: 0 black/white, 1 red, 2 purple, 3 fire, 4 blue, 5 warm gold.
 */
public final class Impact {
    public static final int RAIL = 0, BLACK = 1, PSPHERE = 2, PBEAM = 3, MECH1 = 4, MECH2 = 5, MECH3 = 6,
            D_POCKET = 7, D_SHRINE = 8, D_VOID = 9, D_HOME = 10, SOFT = 11,
            BH_CAST = 12, BH_WARP = 13, BH_REVEAL = 14, BH_COLLAPSE = 15, CBLACK = 16;

    private record Spec(int[] styles, int fadeStyle, int fade, float amount, boolean lines, boolean bolts, int flames, int len) {}

    private static final int[] BRIGHT = {0xFFFFFFFF, 0xFFE01830, 0xFFB040FF, 0xFFFF8A00, 0xFF5AA0FF, 0xFFFFE9B0, 0xFFFF7A18};
    private static final int[] DARK = {0xFF000000, 0xFF000000, 0xFF000000, 0xFF000000, 0xFF000000, 0xFF201008, 0xFF000000};
    private static final Random RNG = new Random();

    private static Spec spec;
    private static int kind = -1, age;
    private static boolean hit;
    private static int curSub, curStyle;
    private static float shake;
    private static long ticks;

    private Impact() {}

    private static int[] rep(int style, int n) { int[] a = new int[n]; java.util.Arrays.fill(a, style); return a; }

    public static void start(int k, boolean h) {
        kind = k; hit = h; age = 0;
        int[] pbeam = new int[14];
        java.util.Arrays.fill(pbeam, 2);
        pbeam[12] = 0; pbeam[13] = 0;
        spec = switch (k) {
            case RAIL -> new Spec(rep(0, 4), 0, 16, 1f, true, false, 0, 14);
            case BLACK -> new Spec(rep(1, 6), 1, 22, 1f, true, true, 0, 18);
            case PSPHERE -> new Spec(rep(2, 4), 2, 16, 1f, true, false, 0, 14);
            case PBEAM -> new Spec(pbeam, 2, 34, 1f, true, true, 0, 30);
            case MECH1 -> new Spec(new int[0], 3, 24, 0.55f, false, false, 0, 14);
            case MECH2 -> new Spec(new int[]{3, 1}, 3, 20, 0.9f, true, false, 40, 40);
            case MECH3 -> new Spec(new int[]{3, 3, 1, 1, 3}, 3, 40, 1f, true, false, 70, 90);
            case D_POCKET -> new Spec(rep(0, 8), 0, 22, 1f, true, false, 0, 20);
            case D_SHRINE -> new Spec(rep(1, 8), 1, 22, 1f, true, false, 0, 20);
            case D_VOID -> new Spec(rep(2, 8), 2, 22, 1f, true, false, 0, 20);
            case BH_CAST -> new Spec(rep(0, 6), 0, 14, 1f, true, false, 0, 14);
            case BH_WARP -> new Spec(rep(6, 4), 6, 12, 1f, true, false, 0, 10);
            case BH_REVEAL -> new Spec(new int[]{6, 6, 6, 6, 0, 0, 6, 6, 6, 6}, 6, 30, 1f, true, true, 0, 26);
            case BH_COLLAPSE -> new Spec(new int[]{0, 6, 0, 6, 0, 6, 0, 6, 0, 6, 0, 6}, 0, 40, 1f, true, true, 0, 34);
            case CBLACK -> new Spec(new int[]{1, 2, 1, 2, 1, 2}, 2, 22, 1f, true, true, 0, 18);
            default -> new Spec(new int[0], 5, 26, 1f, false, false, 0, 16);
        };
    }

    public static void startSoft(int style) {
        kind = SOFT; hit = false; age = 0;
        spec = new Spec(new int[0], style, 20, 0.7f, false, false, 0, 12);
    }

    public static void addShake(float s) { shake = Math.max(shake, s); }
    public static boolean active() { return kind >= 0; }
    public static int kind() { return kind; }

    public static void tick(Minecraft mc) {
        ticks++;
        if (kind >= 0 && ++age > spec.len()) { kind = -1; PostFx.clear(); }
        LocalPlayer p = mc.player;
        if (p != null && shake > 0) {
            p.setYRot(p.getYRot() + (RNG.nextFloat() - .5f) * shake * 0.5f);
            p.setXRot(p.getXRot() + (RNG.nextFloat() - .5f) * shake * 0.3f);
            shake *= 0.86f;
            if (shake < 0.05f) shake = 0;
        }
    }

    /** Called every rendered frame, just before the post filter runs. */
    public static void frame(float pt) {
        if (kind < 0) { PostFx.clear(); return; }
        int sub = (int) ((age + pt) * 2);
        curSub = sub;
        int strobe = spec.styles().length;
        if (sub < strobe) {
            int st = spec.styles()[sub];
            if (kind == RAIL && hit && sub == 3) st = 1;
            curStyle = st;
            PostFx.set(st, 1f, sub % 2 == 0 ? 0f : 1f, 1.5f);
        } else {
            float t = Math.min((sub - strobe) / (float) spec.fade(), 1f);
            curStyle = spec.fadeStyle();
            PostFx.set(spec.fadeStyle(), spec.amount() * (1 - t) * (1 - t), 0f, 0.9f * (1 - t));
        }
    }

    public static void render(GuiGraphics g, int w, int h) {
        if (kind < 0) return;
        int sub = curSub, strobe = spec.styles().length;
        long seed = sub * 104729L;
        if (sub < strobe) {
            boolean even = sub % 2 == 0;
            int bright = BRIGHT[curStyle], dark = DARK[curStyle];
            if (!PostFx.ok()) g.fill(0, 0, w, h, even ? dark : bright);           // flat fallback
            if (spec.lines()) Hud.speedLines(g, w, h, even ? bright : dark, 60, seed, 0.05f);
            if (spec.bolts()) {
                Hud.bolts(g, w, h, even ? bright : dark, 4, 24, seed);
                Hud.bolts(g, w, h, 0xFFFFFFFF, 1, 24, seed);
            }
        } else {
            float t = Math.min((sub - strobe) / (float) spec.fade(), 1f);
            int a = (int) ((1 - t) * (1 - t) * 255 * spec.amount());
            if (a > 0 && !PostFx.ok()) g.fill(0, 0, w, h, (a << 24) | (BRIGHT[curStyle] & 0xFFFFFF));
            else if (a > 0) g.fill(0, 0, w, h, ((a / 5) << 24) | (BRIGHT[curStyle] & 0xFFFFFF));   // faint colour wash on top
            if (spec.bolts() && a > 80) Hud.bolts(g, w, h, 0xFFFFFFFF, 1, 10, seed);
        }
        if (spec.flames() > 0 && sub >= Math.max(2, strobe)) {
            float ramp = Math.min(1f, (sub - strobe) / 4f);
            float inten = ramp * Math.max(0f, 1f - (age - 4) / (float) spec.flames());
            Hud.flames(g, w, h, kind == MECH3 ? inten : inten * 0.6f, ticks);
        }
    }
}
