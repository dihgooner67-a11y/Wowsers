import java.util.*;
public class ZirRun {
  static void run(String id, int n, int co2, int water, int rodsTarget, boolean valve) {
    ZirnoxSim sim = new ZirnoxSim(); sim.rodsTarget = rodsTarget;
    RodType t = RodTypes.ALL.get(id); RodState[] st = new RodState[24];
    for (int i = 0; i < 24; i++) if (i < n) st[i] = new RodState();
    int co2Init = co2, waterInit = water; int steam = 0, tickExplode = -1; boolean nan = false; double maxHull = 0, maxP = 0;
    for (int tick = 1; tick <= 6000 && tickExplode < 0; tick++) {
      ZirnoxSim.Inputs in = new ZirnoxSim.Inputs();
      for (int i = 0; i < 24; i++) if (st[i] != null) { in.types[i] = RodTypes.ALL.get(t.functionId); in.states[i] = st[i]; }
      in.co2Fill = co2; in.waterFill = water; in.valveOpen = valve; in.steamTempC = 500; in.steamLevel = 100;
      ZirnoxSim.Outputs o = sim.tick(in);
      co2 = Math.max(0, co2 - o.co2Drain); water = Math.max(0, water - o.waterDrain); steam += o.steamAdd;
      if (Double.isNaN(sim.hullTemp)) nan = true;
      maxHull = Math.max(maxHull, sim.hullTemp); maxP = Math.max(maxP, sim.pressure);
      if (o.explode) tickExplode = tick;
    }
    System.out.printf("%-6s x%-2d co2=%5d water=%5d target=%3d | hull=%.0f max=%.0f pressure max=%.1f steam=%d water left=%d explodeAt=%d nan=%b%n",
      id, n, co2Init, waterInit, rodsTarget, sim.hullTemp, maxHull, maxP, steam, water, tickExplode, nan);
  }
  public static void main(String[] a) {
    run("nu", 12, 16000, 32000, 100, false);
    run("meu235", 12, 16000, 32000, 100, false);
    run("meu235", 12, 16000, 32000, 0, false);
    run("meu235", 12, 2000, 32000, 100, false);
    run("heu235", 24, 16000, 32000, 100, false);
  }
}

/** Static properties of one rod type (port of LeafiaRodItem's fields). */
final class RodType {
    public final String label, functionId;
    public final double life, meltingPoint, reactivity, emission;
    public final String decayProduct; // functionId of the depleted rod, or null
    public final boolean splitIntoFast, splitWithFast, splitWithAny;

    public RodType(String label, double life, double meltingPoint, double reactivity, double emission,
                   String decayProduct, boolean moderated, boolean preferFast, boolean preferAny) {
        this.label = label;
        this.functionId = label.replace("-", "").replace(" ", "").toLowerCase();
        this.life = life;
        this.meltingPoint = meltingPoint;
        this.reactivity = reactivity;
        this.emission = emission;
        this.decayProduct = decayProduct;
        this.splitIntoFast = !moderated;
        this.splitWithFast = preferFast;
        this.splitWithAny = preferAny;
    }

    /** Neutron flux this rod receives from a neighbour rod (addon getFlux(stack, moderated)). */
    public double fluxFrom(RodType neighbour, RodState neighbourState, boolean moderated) {
        double compat = 0.5;
        if (splitWithAny || moderated != splitWithFast) compat = 3.0;
        compat = compat * neighbour.emission * reactivity;
        return Math.max(neighbourState.heat, 20.0) * compat;
    }
}

/** Per-rod mutable state (the addon kept these in the item's NBT). */
final class RodState {
    public double heat = 20.0, depletion = 0.0, decay = 0.0, incoming = 0.0, cooled = 0.0;
    public boolean melting = false, nuke = false;
    public int generosityTimer = 90, spillage = 0;
}


/** The addon's 59 rod types, generated from AddonItems (label, life, melting point, reactivity, emission, flags). */
final class RodTypes {
    private RodTypes() {}
    public static final Map<String, RodType> ALL = new LinkedHashMap<>();
    static {
        add("U-238", 4000000.0, 0.0, 1.0, 1.0, "depleteduranium238", false, false, false);
        add("NU", 4000000.0, 2865.0, 0.4, 1.0, "depletednaturaluranium", false, false, false);
        add("MEU-235", 4000000.0, 2865.0, 1.0, 1.0, "depleteduranium", false, false, false);
        add("HEU-233", 1000000.0, 2865.0, 2.0, 1.0, "depleteduranium233", false, false, false);
        add("HEU-235", 3000000.0, 2865.0, 1.75, 1.0, "depleteduranium235", false, false, false);
        add("ThMEU", 3500000.0, 3350.0, 1.0, 1.0, "depletedthorium", false, false, false);
        add("LEPu-239", 5000000.0, 2744.0, 0.6666666666666666, 1.0, "depletedplutonium", false, false, false);
        add("MEPu-239", 5000000.0, 2744.0, 1.0, 1.0, "depletedplutonium", false, false, false);
        add("HEPu-239", 5000000.0, 2744.0, 1.25, 1.0, "pu240", false, false, false);
        add("HEPu-241", 1.8E7, 2744.0, 1.45, 1.0, "depletedplutonium", false, false, false);
        add("LEAm-242", 1.8E7, 3986.0, 1.0, 1.0, null, false, false, false);
        add("MEAm-242", 1.8E7, 3986.0, 1.0, 1.0, null, false, false, false);
        add("HEAm-241", 1.8E7, 3986.0, 1.0, 1.0, null, false, true, false);
        add("HEAm-242", 1.8E7, 3986.0, 1.0, 1.0, null, false, false, false);
        add("MENp-237", 5200000.0, 2800.0, 1.0, 1.0, null, false, false, true);
        add("HENp-237", 3200000.0, 2800.0, 1.0, 1.0, null, false, true, false);
        add("MOX", 6000000.0, 2815.0, 1.0, 1.0, null, false, false, false);
        add("LESa-326", 3500000.0, 2500.0, 1.0, 0.75, "depletedschrabidium", true, false, false);
        add("MESa-326", 3000000.0, 2750.0, 1.0, 1.0, "depletedmesa326", false, false, false);
        add("HESa-326", 2500000.0, 3000.0, 1.0, 1.25, "depletedhesa326", false, false, false);
        add("Sa-326", 2000000.0, 3250.0, 1.0, 2.25, "depletedsa326", false, false, false);
        add("LEAus", 5500000.0, 7029.0, 1.0, 0.3, "heaus", false, false, false);
        add("HEAus", 5500000.0, 5211.0, 1.0, 1.2, null, false, false, false);
        add("Ra-226Be", 400000.0, 700.0, 1.0, 1.0, null, true, false, false);
        add("Po-210Be", 2000000.0, 1287.0, 1.0, 1.0, null, true, false, false);
        add("Pu-238Be", 2000000.0, 2744.0, 1.0, 1.0, null, true, false, false);
        add("Pu-238", 3500000.0, 2744.0, 1.0, 1.0, null, false, false, false);
        add("Po-210", 3000000.0, 1287.0, 1.0, 1.0, null, false, false, false);
        add("Sa-327", 8000000.0, 2250.0, 0.022222222222222223, 65.0, "depletedsa327", false, false, false);
        add("Lithium", 3000.0, 0.0, 1.0, 1.0, null, false, false, false);
        add("Pu-240", 6.5E7, 2744.0, 1.0, 1.0, null, false, false, false);
        add("Depleted Uranium", 0.0, 0.0, 1.0, 1.0, null, false, false, false);
        add("Depleted Natural Uranium", 0.0, 0.0, 1.0, 1.0, null, false, false, false);
        add("Depleted Uranium-233", 0.0, 0.0, 1.0, 1.0, null, false, false, false);
        add("Depleted Uranium-235", 0.0, 0.0, 1.0, 1.0, null, false, false, false);
        add("Depleted Uranium-238", 0.0, 0.0, 1.0, 1.0, null, false, false, false);
        add("Depleted Plutonium", 0.0, 0.0, 1.0, 1.0, null, false, false, false);
        add("Depleted Thorium", 0.0, 0.0, 1.0, 1.0, null, false, false, false);
        add("Depleted MOX", 0.0, 0.0, 1.0, 1.0, null, false, false, false);
        add("Depleted Schrabidium", 0.0, 0.0, 1.0, 1.0, null, false, false, false);
        add("Depleted MESa-326", 0.0, 0.0, 1.0, 1.0, null, false, false, false);
        add("Depleted HESa-326", 0.0, 0.0, 1.0, 1.0, null, false, false, false);
        add("Depleted Sa-326", 0.0, 0.0, 1.0, 1.0, null, false, false, false);
        add("Depleted Sa-327", 0.0, 0.0, 1.0, 1.0, null, false, false, false);
        add("Lead", 9.9999999E7, 0.0, 1.0, 1.0, "pb209", false, false, false);
        add("Balefire", 200.0, 8652.0, 0.16666666666666666, 8.0, "blazingbalefire", false, false, false);
        add("Blazing Balefire", 65000.0, 8652.0, 0.1111111111111111, 12.0, "balefire", false, false, false);
        add("NPu", 2600000.0, 2744.0, 1.0, 1.0, null, false, false, false);
        add("Th-232", 90000.0, 0.0, 1.0, 1.0, "thmeu", false, false, false);
        add("Au-198", 900000.0, 3850.0, 1.0, 1.0, null, false, false, false);
        add("Pb-209", 600000.0, 4300.0, 1.0, 1.0, null, false, false, false);
        add("Debug", 0.0, 0.0, 1.0, 1.0, null, false, false, false);
        add("Potential Instant Blowout Applicator", 9.5E7, 6.5E8, 1.0, 1.0, null, false, false, false);
        add("Yh-XXX", 6666666.0, 3200.0, 1.0, 1.0, null, false, false, false);
        add("DG-OMEGA", Double.MAX_VALUE, 1916169.0, 2.0, 1.0, null, false, false, false);
        add("Kys-3000", Double.MAX_VALUE, 22.0, 1.0, 1.0, null, false, false, false);
        add("Shitfuckium", Double.MAX_VALUE, 3200.0, 1.0, 1.0, null, false, false, false);
        add("Catsmilium", Double.MAX_VALUE, Double.MAX_VALUE, 1.0, 1.0, null, false, false, false);
        add("Confusium", Double.MAX_VALUE, 1300.0, 1.0, 1.0, null, false, false, false);
    }

    private static void add(String label, double life, double melt, double reactivity, double emission,
                            String decayProduct, boolean moderated, boolean preferFast, boolean preferAny) {
        RodType t = new RodType(label, life, melt, reactivity, emission, decayProduct, moderated, preferFast, preferAny);
        ALL.put(t.functionId, t);
    }
}

/**
 * Loader-independent port of LeafiaRodItem.HeatFunction (1.12.2 addon): how a rod heats from incoming flux,
 * cools, depletes and melts. No Minecraft types; the reactor feeds flux/cooling in and applies the state.
 * Unsupported: "shitfuckium" (needs the addon's lookup table), treated as 0 heat here.
 */
final class RodSim {
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

/**
 * Loader-independent port of the addon's rewritten ZIRNOX tick (MixinTileEntityReactorZIRNOX, 1.12.2):
 * hull temperature, CO2 pressure, control rod movement, rod-to-rod flux, boiling, stress and failure.
 * The block entity feeds Inputs in and applies Outputs (tank changes, rod write-back, meltdown).
 */
final class ZirnoxSim {
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
