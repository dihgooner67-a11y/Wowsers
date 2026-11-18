package com.leafia.cursed.apr;

import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.trait.FT_Coolable;

/**
 * APR fluid conversion data taken from Continuum's own "coolable" fluids (hot fluid -> cooled fluid).
 * The addon's APR also used its own oxygen-gas/pyrogel traits, which Continuum has no counterpart for yet.
 */
public final class AprFluids {
    private AprFluids() {}

    public static boolean isValid(FluidType t) { return t != null && t != Fluids.NONE && t.hasTrait(FT_Coolable.class); }

    private static FT_Coolable cool(FluidType t) { return t.getTrait(FT_Coolable.class); }

    public static double heatDifference(FluidType t) {
        return isValid(t) ? t.getTemperature() - cool(t).coolsTo.getTemperature() : -1.0;
    }

    public static FluidType conversion(FluidType t) { return isValid(t) ? cool(t).coolsTo : Fluids.NONE; }

    /** {in, out} for one conversion step. */
    public static int[] rates(FluidType t) { return isValid(t) ? new int[]{cool(t).amountReq, cool(t).amountProduced} : new int[]{1, 1}; }

    /** Follows the cooling chain to its end (capped), like the addon's getFinal* helpers. */
    public static double finalHeatDifference(FluidType t) {
        double sum = 0.0;
        for (int i = 0; i < 16 && isValid(t); i++) { sum += heatDifference(t); t = conversion(t); }
        return sum;
    }

    public static int[] finalRates(FluidType t) {
        long in = 1, out = 1;
        for (int i = 0; i < 16 && isValid(t); i++) { int[] r = rates(t); in *= r[0]; out *= r[1]; t = conversion(t); }
        return new int[]{(int) Math.min(in, Integer.MAX_VALUE), (int) Math.min(out, Integer.MAX_VALUE)};
    }

    public static FluidType finalConversion(FluidType t) {
        FluidType out = Fluids.NONE;
        for (int i = 0; i < 16 && isValid(t); i++) { t = out = conversion(t); }
        return out;
    }

    public static double energy(double heatDifference) { return heatDifference > 0.0 ? AprSim.energyOf(heatDifference) : 0.0; }
    public static double finalEnergy(double summed) { return summed > 0.0 ? AprSim.finalEnergyOf(summed) : 0.0; }
}
