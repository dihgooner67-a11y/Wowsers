package com.leafia.cursed.fuel;

/**
 * Loader-independent port of the addon's rewritten ZIRNOX tick (MixinTileEntityReactorZIRNOX, 1.12.2):
 * hull temperature, CO2 pressure, control rod movement, rod-to-rod flux, boiling, stress and failure.
 * The block entity feeds Inputs in and applies Outputs (tank changes, rod write-back, meltdown).
 */
public final class ZirnoxSim {
    public static final int SLOTS = 24;
    public static final double MAX_PRESSURE = 30.0;

    public static class Inputs {
        public final RodType[] types = new RodType[SLOTS];   // null = no rod
        public final RodState[] states = new RodState[SLOTS];
        public int co2Fill, co2Max = 16000, waterFill, waterMax = 32000;
        public boolean valveOpen;
        public int steamTempC = 20;
        /** Steam density level of the steam tank (STEAM 1, HOT 10, SUPERHOT 100, ULTRAHOT 1000). */
        public int steamLevel = 100;
    }

    public static class Outputs {
        public int co2Drain, waterDrain, steamAdd;
        public boolean explode, stressSound, nuke;
    }

    // persisted
    public double hullTemp = 20.0;
    public int rods, rodsTarget;
    // transient
    public double pressure, lastPressure, stressTimer = 300.0, avgHeat = 20.0;
    public int generosity = 100;
    private int moveDelay;

    private double fluxAt(Inputs in, int slot, RodType receiver) {
        RodType other = in.types[slot];
        if (other == null) return 0.0;
        return receiver.fluxFrom(other, in.states[slot], true);
    }

    /** Neighbour flux for one slot, transcribed from the addon's (quirky) slot layout rules. */
    private double detectedHeat(Inputs in, int slot, RodType rod) {
        double d = 0.0;
        for (int o = 1; slot - o * 7 >= 0; o++) d += fluxAt(in, slot - o * 7, rod) / Math.pow(2.0, o - 1);
        for (int o = 1; slot + o * 7 < SLOTS; o++) d += fluxAt(in, slot + o * 7, rod) / Math.pow(2.0, o - 1);
        int smod = Math.floorMod(slot, 7);
        if (smod <= 2) {
            for (int o = 1; slot - o >= 0; o++) d += fluxAt(in, slot - o, rod) / Math.pow(2.0, o - 1);
            for (int o = 1; slot + o <= 2; o++) d += fluxAt(in, slot + o, rod) / Math.pow(2.0, o - 1);
            if (slot - 4 > 0) d += fluxAt(in, slot - 4, rod) / 2.0;
            if (slot - 3 > 0) d += fluxAt(in, slot - 3, rod) / 2.0;
            if (slot + 4 < SLOTS) d += fluxAt(in, slot + 4, rod) / 2.0;
            if (slot + 3 < SLOTS) d += fluxAt(in, slot + 3, rod) / 2.0;
        } else {
            for (int o = 1; slot - o >= 3; o++) d += fluxAt(in, slot - o, rod) / Math.pow(2.0, o - 1);
            for (int o = 1; slot + o <= 7; o++) d += fluxAt(in, slot + o, rod) / Math.pow(2.0, o - 1);
            if (smod != 3) {
                if (slot - 4 > 0) d += fluxAt(in, slot - 4, rod) / 2.0;
                if (slot + 3 < SLOTS) d += fluxAt(in, slot + 3, rod) / 2.0;
            }
            if (smod != 6) {
                if (slot - 3 > 0) d += fluxAt(in, slot - 3, rod) / 2.0;
                if (slot + 4 < SLOTS) d += fluxAt(in, slot + 4, rod) / 2.0;
            }
        }
        return d;
    }

    public Outputs tick(Inputs in) {
        Outputs out = new Outputs();

        moveDelay = (moveDelay + 1) % 2;
        if (moveDelay == 0) {
            if (rods > rodsTarget) rods--;
            else if (rods < rodsTarget) rods++;
        }

        lastPressure = pressure;
        double co2Frac = in.co2Max > 0 ? (double) in.co2Fill / in.co2Max : 0.0;
        pressure = (in.co2Fill * 2.0 + (hullTemp - 20.0) * 125.0 * co2Frac) / 3333.0;
        if (in.valveOpen) out.co2Drain += (int) Math.ceil(Math.pow(Math.max(pressure - 5.0, 0.0), 3.0) / 30.0 + 10.0);
        stressTimer -= Math.pow(Math.max(pressure - 22.0, 0.0) / 6.0, 0.9) * 64.0;
        if (stressTimer <= 0.0) { stressTimer = 300.0; out.stressSound = true; }

        if (pressure >= MAX_PRESSURE && pressure >= lastPressure - 0.2) {
            if (--generosity <= 0) { out.explode = true; return out; }
        } else generosity = 100;
        if (hullTemp >= 1300.0) { out.explode = true; return out; }

        double coolin = (float) Math.pow((float) in.co2Fill / 16000.0f, 0.4);
        double feedwater = (float) Math.pow((float) in.waterFill / 32000.0f, 0.4);
        double cooledSum = 0.0;
        avgHeat = 20.0;
        double coolingRate = Math.pow(pressure / 30.0, 2.0) * 1000.0;
        for (int i = 0; i < SLOTS; i++) {
            RodType t = in.types[i];
            if (t == null) continue;
            RodState s = in.states[i];
            double detected = detectedHeat(in, i, t);
            RodSim.step(t, s, detected * (rods / 100.0f), coolin * 1.5, 20.0, coolingRate, 0.0, 1.0);
            if (RodSim.depleted(t, s)) { // becomes its depleted rod, keeping heat
                RodType next = RodTypes.ALL.get(t.decayProduct);
                if (next != null) { in.types[i] = next; s.depletion = 0.0; }
            }
            avgHeat += (s.heat - 20.0) / 24.0;
            if (s.nuke) { out.nuke = true; out.explode = true; return out; }
            if (s.spillage > 200) { out.explode = true; return out; }
            cooledSum += s.cooled;
        }

        double difference = Math.abs(avgHeat - hullTemp);
        int sign = hullTemp > avgHeat ? -1 : 1;
        hullTemp += Math.pow(difference, 0.25) * sign + cooledSum / 24.0;

        double steamTemp = in.steamTempC;
        double boilBase = Math.pow(Math.max(hullTemp - steamTemp, 0.0), 0.25) * Math.pow(steamTemp / 100.0, 0.75) * feedwater;
        boilBase /= 3.0;
        int boiling = (int) (boilBase * 24.0);
        hullTemp = Math.max(hullTemp - boilBase, 20.0);
        out.waterDrain = (int) ((long) boiling * in.steamLevel / 100L);
        out.steamAdd = boiling;
        return out;
    }
}
