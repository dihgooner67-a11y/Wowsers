package com.leafia.cursed.heat;

import java.util.List;

/**
 * Loader-independent port of the addon HP Boiler's maths (HPBoilerTE.getBoiledFluid / tryConvert / tryPullHeat):
 * chains up to 4 heating steps (water -> steam -> denser steam ...) into one combined conversion.
 */
public final class HpBoilerMath {
    private HpBoilerMath() {}

    public static final int MAX_HEAT = 128_000_000;
    public static final double DIFFUSION = 0.1;
    public static final int TANK_BASE = 64000;

    /** One heating step: needs `req` of the input and `heat`, produces `prod`. `eff` is the boiler efficiency of the step. */
    public record Step(int req, int heat, int prod, double eff) {}

    /** Combined conversion. comp == 0 means the fluid cannot be boiled. */
    public record Data(int comp, int in, int out, int heatReq, double efficiency) {
        public static final Data NONE = new Data(0, 0, 0, 0, 1.0);
    }

    private static int gcd(int a, int b) {
        a = Math.abs(a); b = Math.abs(b);
        while (b != 0) { int t = a % b; a = b; b = t; }
        return a;
    }

    public static Data combine(List<Step> steps, int compression) {
        int comp = 0, in = 0, out = 0, heatReq = 0;
        for (int i = 0; i < compression && i < steps.size(); i++) {
            Step s = steps.get(i);
            if (comp == 0) {
                in = s.req(); out = s.prod(); heatReq = s.heat();
            } else {
                int prevOut = out;
                int g = Math.max(gcd(prevOut, s.req()), 1);
                int prefixMul = s.req() / g;
                int stepMul = prevOut / g;
                in *= prefixMul;
                out = s.prod() * stepMul;
                heatReq = heatReq * prefixMul + s.heat() * stepMul;
            }
            comp++;
        }
        if (comp <= 0) return Data.NONE;
        heatReq = (int) (heatReq / Math.pow(1.5, comp - 1));
        int div = gcd(gcd(in, out), heatReq);
        if (div > 1) { in /= div; out /= div; heatReq /= div; }
        return new Data(comp, in, out, heatReq, 1.0); // (sic) the addon always reports efficiency 1.0
    }

    /** How many conversions fit right now. */
    public static int ops(int inFill, int outFill, int outMax, int heat, Data d) {
        if (d.comp() <= 0 || d.in() <= 0 || d.out() <= 0 || d.heatReq() <= 0) return 0;
        int inputOps = inFill / d.in();
        int outputOps = (outMax - outFill) / d.out();
        int heatOps = (int) (heat * d.efficiency()) / d.heatReq();
        return Math.max(0, Math.min(inputOps, Math.min(outputOps, heatOps)));
    }

    /** Heat pulled from a heat source below (addon tryPullHeat). Returns {newHeat, usedFromSource}. */
    public static int[] pullHeat(int heat, int sourceHeat) {
        int diff = sourceHeat - heat;
        if (diff > 0) {
            diff = (int) Math.ceil(diff * DIFFUSION);
            diff = Math.min(diff, MAX_HEAT - heat);
            return new int[]{Math.min(heat + diff, MAX_HEAT), diff};
        }
        return new int[]{Math.max(heat - Math.max(heat / 1000, 1), 0), 0};
    }

    /** Tank size for a side of the conversion, clamped to int range. */
    public static int tankSize(int amount) { return (int) Math.min(Integer.MAX_VALUE, (long) TANK_BASE * Math.max(amount, 1)); }
}
