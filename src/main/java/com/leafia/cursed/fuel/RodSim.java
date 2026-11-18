package com.leafia.cursed.fuel;

/**
 * Loader-independent port of LeafiaRodItem.HeatFunction (1.12.2 addon): how a rod heats from incoming flux,
 * cools, depletes and melts. No Minecraft types; the reactor feeds flux/cooling in and applies the state.
 * Unsupported: "shitfuckium" (needs the addon's lookup table), treated as 0 heat here.
 */
public final class RodSim {
    private RodSim() {}

    /** Result of the heat-curve lookup. */
    public record Curve(double y, boolean disableDecay) {}

    /** Target temperature rise y(x) for a rod function at incoming flux x and current heat above 20C. */
    public static Curve curve(String id, double x, double heat) {
        double y = 0.0;
        boolean disable = false;
        switch (id) {
            case "depleteduranium", "depletedmox", "depletednaturaluranium", "depleteduranium233",
                 "depleteduranium235", "depleteduranium238" -> { y = 60.0; disable = true; }
            case "depletedplutonium" -> { y = 70.0; disable = true; }
            case "depletedthorium" -> { y = 40.0; disable = true; }
            case "depletedschrabidium", "depletedmesa326", "depletedhesa326", "depletedsa326", "depletedsa327" -> { y = 80.0; disable = true; }
            case "confusium" -> y = Math.abs(Math.tan(x)) * 1000.0;
            case "catsmilium" -> { disable = true; y = Math.pow(x * 8.0, 0.56) * 3.0; }
            case "meu235", "nu" -> y = Math.pow(x * 8.0, 0.56) * 3.0;
            case "heu235", "heu233" -> y = Math.pow(x * 8.0, 0.56) * 3.0 + Math.pow(Math.max(x - 2500.0, 0.0) / 1000.0, 3.0);
            case "mox" -> y = Math.pow(x * 8.0, 0.56) * 2.0;
            case "thmeu" -> y = Math.pow(x * 65.0, 0.35) * 3.0;
            case "lepu239", "mepu239", "npu", "pu240" -> y = Math.pow(x * 8.0, 0.6) * 3.0;
            case "hepu239", "hepu241" -> y = Math.pow(x * 8.0, 0.6) * 3.0 + Math.pow(Math.max(x - 2500.0, 0.0) / 800.0, 3.0);
            case "leam242", "meam242", "heam242" -> y = Math.pow(x * 8.0, 0.64) * 3.0;
            case "heam241" -> y = Math.pow(x * 8.0, 0.64) * 3.0 + Math.pow(Math.max(x - 2500.0, 0.0) / 800.0, 3.0);
            case "menp237", "henp237" -> y = Math.pow(x * 8.0, 0.52) * 3.0;
            case "lesa326", "mesa326", "hesa326", "sa326", "sa327" -> y = Math.pow(x, 0.65) * 12.0 + Math.pow(Math.max(x - 2500.0, 0.0) / 1600.0, 3.0);
            case "ra226be" -> y = 300.0 / (1.0 + Math.pow(Math.E, -0.02 * x)) - 150.0;
            case "potentialinstantblowoutapplicator" -> y = Math.tan(Math.min(heat / 400.0, 0.5) * Math.PI) + x / 4.0;
            case "balefire" -> y = 100.0 * Math.pow(x / 1000.0 - 2.0, 3.0) - 500.0 * Math.pow(x / 1000.0 - 2.0, 2.0) + x / 600.0 + 2800.0;
            case "blazingbalefire" -> {
                double z = x + 1000.0;
                y = 100.0 * Math.pow(z / 1000.0 - 2.0, 3.0) - 500.0 * Math.pow(z / 1000.0 - 2.0, 2.0) + z / 600.0 + 2800.0;
            }
            case "po210", "po210be" -> y = 700.0 + Math.pow(x * 2.0, 0.69);
            case "au198" -> y = 1580.0 + Math.pow(x, 0.75) * 2.5;
            case "pb209" -> y = 2300.0 + x * 0.4;
            case "pu238", "pu238be" -> y = 1330.0 + Math.pow(x * 2.0, 0.62);
            case "leaus", "heaus" -> y = Math.pow(x * 5.0, 0.65) * 2.0;
            case "debug" -> { y = Math.max(heat, 0.0) + Math.sqrt(x); disable = true; }
            case "yhxxx" -> { y = Math.pow(x, 0.75) * 8.0; disable = true; }
            case "dgomega" -> { y = Math.pow(1.01916169, x); disable = true; }
            case "kys3000" -> y = Math.tan(x);
            default -> { } // u238 and unlisted rods: no self-heating; shitfuckium unsupported
        }
        return new Curve(y, disable);
    }

    /**
     * One simulation step.
     * @param x incoming flux, @param cool cooling strength, @param desiredTemp target coolant temperature,
     * @param coolingRate coolant rate, @param minimumRequired minimum cooling to count, @param heatMultiplier heating scale.
     * @return heat removed by cooling this step (addon data "cooled")
     */
    public static double step(RodType type, RodState s, double x, double cool, double desiredTemp,
                              double coolingRate, double minimumRequired, double heatMultiplier) {
        double heatAbove = s.heat - 20.0; // addon: heat = data.heat - 20 when sampling the curve
        Curve c = curve(type.functionId, x, heatAbove);
        double y = c.y();
        boolean disableDecay = c.disableDecay();

        boolean meltdown = s.melting;
        s.incoming = x;
        if (meltdown) s.spillage++;
        if (s.depletion >= type.life && type.life > 0.0) y = 0.0;

        double heat = s.heat;
        double decay = s.decay;
        double heatX = Math.max(heat, 20.0);
        double heatMg = Math.pow(Math.abs(20.0 + y - heat) + 1.0, 0.25) - 1.0;
        if (heatX > 20.0 + y) heatMg *= -1.0;
        if (Math.abs(heatMg) < 1.0E-5) heatMg = 0.0;
        if (!meltdown && (heatMg != 0.0 || x > 0.0))
            s.depletion += Math.max(heatMg / 2.0 + Math.pow(x, 0.95) / 2000.0, 0.0);
        if (heatMg > 0.0) heatMg *= heatMultiplier;
        double decayBase = Math.min(heatMg / 2.0, x);
        if (decayBase > decay) decay += (decayBase - decay) * 0.01;
        if (disableDecay) decay = 0.0;
        decay *= 0.99992694932;
        s.decay = decay;
        double fuckoff = 1.0 - Math.pow(Math.min(decay * 20.0 / 12.0, 1.0), 0.5);
        if (heatMg < 0.0) heatMg *= fuckoff;
        double newTemp = heat + heatMg + Math.max(decay - Math.max(heatMg, 0.0), 0.0);
        if (Double.isNaN(newTemp)) newTemp = Double.MAX_VALUE;
        double cooled = (Math.pow(Math.max(newTemp - desiredTemp, 0.0) + 1.0, Math.pow(coolingRate, 0.5) / 100.0) - 1.0) * cool;
        if (cooled < minimumRequired) cooled = 0.0;
        double newCooledTemp = Math.max(newTemp - cooled, -273.15);
        s.cooled = cooled;
        if (Double.isNaN(newCooledTemp)) newCooledTemp = Double.MAX_VALUE;
        s.heat = newCooledTemp;
        if (newCooledTemp >= 1.0E7) s.nuke = true;

        if (!meltdown && type.meltingPoint != 0.0) {
            int timer = s.generosityTimer;
            int initial = timer--;
            if (newCooledTemp >= type.meltingPoint) {
                if (!(newCooledTemp - heatX >= 0.0)) timer = Math.min(timer + 20, 90);
                if (timer <= 0) { timer = 0; s.melting = true; }
            } else {
                timer = 90;
                s.spillage = Math.max(s.spillage - 1, 0);
            }
            if (timer != initial) s.generosityTimer = timer;
        }
        if (meltdown && newCooledTemp < type.meltingPoint) {
            s.melting = false;
            s.spillage = 0;
        }
        return cooled;
    }

    /** True once the rod has used up its life and should turn into its decay product. */
    public static boolean depleted(RodType type, RodState s) {
        return !s.melting && s.depletion >= type.life && type.decayProduct != null;
    }
}
