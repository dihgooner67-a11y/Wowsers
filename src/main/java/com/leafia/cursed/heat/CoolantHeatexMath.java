package com.leafia.cursed.heat;

/**
 * Loader-independent port of the addon's Coolant Heat Exchanger maths (CoolantHeatexTE): a hot "coolable" fluid is cooled
 * and its heat goes into a buffer; that heat then heats a coolant fluid (water -> steam, coolant -> hot coolant ...).
 */
public final class CoolantHeatexMath {
    private CoolantHeatexMath() {}

    public static final int TANK = 2_400_000;
    public static final long POWER_USE = 5L, MAX_POWER = 10_000L;
    public static final int DEFAULT_CYCLE = 24_000;

    /** Heating step of the coolant being warmed. */
    public record Heating(int amountReq, int amountProduced, int heatReq) {}

    /** The buffer stops taking in heat once it holds 2.4M times the heat one unit of coolant needs. */
    public static double heatRequired(Heating h) {
        return (double) h.heatReq() / (double) h.amountProduced() * (double) h.amountReq();
    }

    public static boolean wantsMoreHeat(int heatEnergy, Heating h) { return heatEnergy <= heatRequired(h) * TANK; }

    /** Cooling the hot fluid: how many conversions fit this tick. */
    public static int coolOps(int inFill, int outFill, int outMax, int amountReq, int amountProduced, int cycleCap) {
        if (amountReq <= 0 || amountProduced <= 0) return 0;
        return Math.max(0, Math.min(inFill / amountReq, Math.min((outMax - outFill) / amountProduced, cycleCap)));
    }

    public static int heatGained(int heatPerOp, int ops, double efficiency) { return (int) ((double) heatPerOp * ops * efficiency); }

    /** Heating the coolant: conversions allowed by input, output room and stored heat. */
    public static int heatOps(int inFill, int outFill, int outMax, int heatEnergy, Heating h) {
        if (h.amountReq() <= 0 || h.amountProduced() <= 0 || h.heatReq() <= 0) return 0;
        return Math.max(0, Math.min(inFill / h.amountReq(), Math.min((outMax - outFill) / h.amountProduced(), heatEnergy / h.heatReq())));
    }
}
