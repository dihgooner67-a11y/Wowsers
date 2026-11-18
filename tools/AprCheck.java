import java.util.*;
public class AprRun {
  static void run(String name, double heatDiff, int convIn, int convOut, double control, int chamberRadiusA, int chamberRadiusB, double chamberEnergy, int chamberFill) {
    AprSim s = new AprSim();
    s.particles = s.particleCap = 30000; s.targetControl = s.control = control;
    int cap = AprSim.capacityFromRadius(chamberRadiusA) + AprSim.capacityFromRadius(chamberRadiusB);
    s.oxygenMax = cap; s.oxygenFill = cap; s.loxMax = cap * convOut / convIn; s.oxygenEnergy = AprSim.energyOf(heatDiff); s.convIn = convIn; s.convOut = convOut;
    for (int r : new int[]{chamberRadiusA, chamberRadiusB}) {
      AprSim.Chamber c = new AprSim.Chamber(); c.inFill = chamberFill; c.outCap = AprSim.capacityFromRadius(r); c.rateIn = 1; c.rateOut = 1; c.finalEnergy = chamberEnergy; s.chambers.add(c);
    }
    double maxRps = 0, maxHull = 0; int disTick = -1; boolean nan = false; long maxPower = 0;
    for (int t = 1; t <= 6000 && disTick < 0; t++) {
      // the machine would refill the oxygen tank from pipes
      s.oxygenFill = Math.min(s.oxygenMax, s.oxygenFill + 400); s.loxFill = Math.max(0, s.loxFill - 400);
      AprSim.Result r = s.tick(true);
      maxRps = Math.max(maxRps, s.rps); maxHull = Math.max(maxHull, s.hullTemp); maxPower = Math.max(maxPower, s.powerOutput());
      if (Double.isNaN(s.rps) || Double.isNaN(s.hullTemp)) nan = true;
      if (r.disassemble) disTick = t;
    }
    System.out.printf("%-26s rps end=%.1f max=%.1f hull max=%.0f damage=%d maxPower=%d disassembleAt=%d nan=%b%n", name, s.rps, maxRps, maxHull, s.damage, maxPower, disTick, nan);
  }
  public static void main(String[] a) {
    run("gentle (control 10%)", 200, 1, 1, 10, 6, 10, 0, 0);
    run("normal (control 40%)", 200, 1, 1, 40, 6, 10, 0, 0);
    run("hot coolant (control 40%)", 900, 1, 1, 40, 6, 10, 0, 0);
    run("hot + chambers (100%)", 900, 1, 1, 100, 6, 10, 150, 200000);
  }
}


/**
 * Loader-independent port of the addon's APR core tick (APRCoreTE, 1.12.2): a particle ring that converts
 * fluids, turning the energy into rotation (rps) and chamber heat. No Minecraft types: the machine feeds fluid
 * amounts and conversion data in and applies the results. Fluid conversion data (heat difference, rates) comes
 * from the fluid traits, so the caller supplies it as numbers.
 */
final class AprSim {
    public static final int MAX_DAMAGE = 600;

    /** One ring chamber with its blanket and in/out fluid tanks. */
    public static final class Chamber {
        public double blanketTemp = 20.0;
        public int inFill, outFill, outCap;
        public int rateIn = 1, rateOut = 1;     // getFinalConversionRates(input type)
        public double finalEnergy;              // getFinalEnergy(input type) = (sum of heat differences)^0.65
    }

    public final List<Chamber> chambers = new ArrayList<>();
    public double hullTemp = 20.0, particles, rps, control = 10.0, targetControl = 10.0;
    public int particleCap = 30000, damage;
    // main (oxygen/coolant) tank and its conversion
    public int oxygenFill, oxygenMax, loxFill, loxMax, convIn = 1, convOut = 1;
    public double oxygenEnergy;                 // getEnergy(oxygen type) = heatDifference^0.75

    public static final class Result {
        public boolean disassemble;
        public final List<Integer> chamberBlasts = new ArrayList<>(); // chambers to blow up at max damage
    }

    public static double energyFor(double energy, int amount) { return amount * energy / 1000.0; }
    public static int capacityFromRadius(int radius) { return radius * 50000; }
    public static double energyOf(double heatDifference) { return Math.pow(heatDifference, 0.75); }
    public static double finalEnergyOf(double summedHeatDifference) { return Math.pow(summedHeatDifference, 0.65); }

    /** Power the outlets deliver per tick. */
    public long powerOutput() { return (long) (rps * particleCap * 100.0 / 3.0 / 20.0 / 5.0); }

    private static int availableCycles(int in, int out, int inAmt, int outAmt, int outCap) {
        return Math.min(inAmt / Math.max(in, 1), (outCap - outAmt) / Math.max(out, 1));
    }

    private int accountForParticles(int amt) {
        return particleCap <= 0 ? 0 : (int) (amt * (particles / particleCap));
    }

    private void addRpsFromEnergy(double energy) {
        double ratio = Math.min((rps - 30.0) / 80.0, 1.0);
        double neg = 1.0 - ratio * 0.85;
        rps += energy / 1600.0 / Math.pow(particleCap / 30000.0, 0.75) * neg;
    }

    private void addHeat(double energy, Chamber c) {
        double ratio = (c.blanketTemp - 20.0) / 8172.0;
        double neg = 1.0 - ratio * 0.5;
        c.blanketTemp += energy / 100.0 * neg;
    }

    /** One server tick. When not assembled the machine just stops spinning. */
    public Result tick(boolean assembled) {
        Result res = new Result();
        if (!assembled) { rps = 0.0; return res; }

        control = Math.abs(targetControl - control) < 1.0 ? targetControl : control + Math.signum(targetControl - control);
        double highest = 0.0;
        for (Chamber c : chambers) highest = Math.max(highest, c.blanketTemp);
        hullTemp += (highest - hullTemp) * 0.05;

        int cycles = availableCycles(convIn, convOut, (int) (accountForParticles(oxygenFill) / 20.0 * (control / 100.0)), loxFill, loxMax);
        if (cycles > 0) {
            oxygenFill -= cycles * convIn;
            loxFill += cycles * convOut;
            double fah = (double) (cycles * convIn) / Math.max(oxygenMax, 1);
            hullTemp = (hullTemp - 20.0) * (1.0 - Math.pow(Math.max(fah, 0.0), 0.25) * 0.65) + 20.0;
            double energy = energyFor(oxygenEnergy, cycles * convIn);
            if (energy > 0.0) {
                addRpsFromEnergy(energy);
                for (Chamber c : chambers) addHeat(energy, c);
            }
        }
        rps *= Math.pow(0.99, 1.0 / (particleCap / 30000.0));

        boolean damaging = rps > 110.0;
        double highestTemp = 0.0;
        for (Chamber c : chambers) {
            int process = availableCycles(c.rateIn, c.rateOut, accountForParticles(c.inFill), c.outFill, c.outCap);
            if (process > 0) {
                c.inFill -= process * c.rateIn;
                c.outFill += process * c.rateOut;
                double energy = energyFor(c.finalEnergy, process * c.rateIn);
                if (energy > 0.0) { addRpsFromEnergy(energy); addHeat(energy, c); }
            }
            c.blanketTemp = (c.blanketTemp - 20.0) * 0.998 + 20.0;
            highestTemp = Math.max(highest, c.blanketTemp); // (sic) the addon compares against the earlier maximum
            if (c.blanketTemp > 8192.0) damaging = true;
        }
        if (damaging) {
            damage++;
            if (damage > MAX_DAMAGE) {
                damage = MAX_DAMAGE;
                for (int i = 0; i < chambers.size(); i++) if (chambers.get(i).blanketTemp > 8192.0) res.chamberBlasts.add(i);
                if (rps > 110.0) res.disassemble = true;
            }
        } else if (highestTemp < 500.0) {
            damage = Math.max(damage - 1, 0);
        }
        if (hullTemp > 1538.0) res.disassemble = true;
        return res;
    }
}
