package com.example.railgun.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;

import java.util.Random;

/**
 * Impact frames. During a hard frame the screen post-filter (PostFx) redraws your actual view as black ink on coloured
 * paper (outlines, solid shadows, halftone dots), flipping between positive and negative every ~25 ms, then it fades out.
 * Paper colours: 0 white, 1 red, 2 purple, 3 fire orange, 4 blue, 5 warm gold, 6 event horizon, 7 FUGA orange.
 */
public final class Impact {
    public static final int RAIL = 0, BLACK = 1, PSPHERE = 2, PBEAM = 3, MECH1 = 4, MECH2 = 5, MECH3 = 6,
            D_POCKET = 7, D_SHRINE = 8, D_VOID = 9, D_HOME = 10, SOFT = 11,
            BH_CAST = 12, BH_WARP = 13, BH_REVEAL = 14, BH_COLLAPSE = 15, CBLACK = 16, FUGA = 17, DISMANTLE = 18, CLEAVE = 19;

    /** hold = how many half-ticks each strobe frame lasts (1 = the quick flicker, 14 = a long frame). */
    private record Spec(int[] styles, int fadeStyle, int fade, float amount, boolean lines, boolean bolts, int flames, int len, int hold) {
        Spec(int[] styles, int fadeStyle, int fade, float amount, boolean lines, boolean bolts, int flames, int len) {
            this(styles, fadeStyle, fade, amount, lines, bolts, flames, len, 1);
        }
    }

    private static final int[] BRIGHT = {0xFFFFFFFF, 0xFFE01830, 0xFFB040FF, 0xFFFF8A00, 0xFF5AA0FF, 0xFFFFE9B0, 0xFFFF7A18, 0xFFFF6A00, 0xFFFFFFFF, 0xFFE01020};
    private static final int[] DARK = {0xFF000000, 0xFF000000, 0xFF000000, 0xFF000000, 0xFF000000, 0xFF201008, 0xFF000000, 0xFF000000, 0xFF000000, 0xFF000000};
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
            case FUGA -> new Spec(rep(7, 8), 7, 30, 1f, false, false, 0, 34);
            case DISMANTLE -> new Spec(new int[]{8}, 8, 8, 1f, false, false, 0, 8);
            case CLEAVE -> new Spec(rep(9, 10), 9, 24, 1f, false, false, 0, 90, 14);
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
        int strobe = spec.styles().length * spec.hold();
        if (sub < strobe) {
            int idx = sub / spec.hold();
            int st = spec.styles()[idx];
            if (kind == RAIL && hit && idx == 3) st = 1;
            curStyle = st;
            PostFx.set(st, 1f, idx % 2 == 0 ? 0f : 1f, 1.5f);
        } else {
            float t = Math.min((sub - strobe) / (float) spec.fade(), 1f);
            curStyle = spec.fadeStyle();
            PostFx.set(spec.fadeStyle(), spec.amount() * (1 - t) * (1 - t), 0f, 0.9f * (1 - t));
        }
    }

    /** Cleave frames: each long frame is a fresh storm of small cuts, black + red on black, then black on red. */
    private static void cleaveCuts(GuiGraphics g, int w, int h, int idx, boolean even) {
        long seed = idx * 7919L + 13;
        if (even) {
            Hud.cuts(g, w, h, seed, 34, 50, 200, 3, 0xFF000000, 0xFFE01020);
            Hud.cuts(g, w, h, seed + 1, 30, 40, 160, 3, 0xFFE01020, 0);
        } else {
            Hud.cuts(g, w, h, seed, 44, 50, 220, 3, 0xFF000000, 0);
            Hud.cuts(g, w, h, seed + 1, 12, 120, 340, 5, 0xFF000000, 0);
        }
    }

    public static void render(GuiGraphics g, int w, int h) {
        if (kind < 0) return;
        int sub = curSub, strobe = spec.styles().length * spec.hold();
        long seed = sub * 104729L;
        if (sub < strobe) {
            int idx = sub / spec.hold();
            boolean even = idx % 2 == 0;
            int bright = BRIGHT[curStyle], dark = DARK[curStyle];
            if (!PostFx.ok()) g.fill(0, 0, w, h, even ? dark : bright);           // flat fallback
            if (kind == DISMANTLE) Hud.cuts(g, w, h, 11L, 4, 220, 560, 9, 0xFF000000, 0xFFFFFFFF);   // black slits, white glowing rims
            else if (kind == CLEAVE) cleaveCuts(g, w, h, idx, even);
            else Hud.streak(g, w, h, seed);                          // the white lightning streak from the FUGA frame
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
