package com.leafia.cursed.nuke;

/** Shared timing/size rules for the nuke effects (used by the cloud mixin and the renderer). */
public final class NukeTimings {
    private NukeTimings() {}

    /** Torex types: Continuum's own are 0 (standard), 1 (balefire), 2 (rainbow); 3 is our antimatter effect. */
    public static final int TYPE_STANDARD = 0, TYPE_BALEFIRE = 1, TYPE_RAINBOW = 2, TYPE_ANTIMATTER = 3;

    /** Scale below this is a "small" bomb: no shock rings, no condensation rings, thinner cloud. */
    public static final double SMALL_SCALE = 1.0;
    /** Scale above this makes the cloud thicker (up to about 1.55x at the largest yield). */
    public static final double THICK_SCALE = 2.5;

    /** How many ticks the plasma ball lasts; bigger bombs hold it longer. Scale is 0.5 .. 5. */
    public static int ballTicks(double scale) { return (int) (30.0 + 25.0 * scale); }

    public static double thickness(double scale) { return scale > THICK_SCALE ? 1.0 + (scale - THICK_SCALE) * 0.22 : 1.0; }

    /** 0 while the plasma ball hides the cloud, rising to 1 as the ball burns out. */
    public static float cloudReveal(double scale, int tick, int type) {
        if (type == TYPE_ANTIMATTER) return 0.0f;
        int ball = ballTicks(scale);
        float f = (tick - ball * 0.55f) / (ball * 0.45f);
        return Math.max(0.0f, Math.min(1.0f, f));
    }
}
