import java.util.Random;
public class Run {
  public static void main(String[] a) {
    for (double stab : new double[]{0.0, 2.0, 5.0}) {
      DfcCoreSim s = new DfcCoreSim(new Random(1));
      DfcCoreSim.Inputs in = new DfcCoreSim.Inputs();
      in.corePower = 7500; in.coreHeat = 0.4; in.coreFuel = 3.0; in.fuelEff0 = 1.5; in.fuelEff1 = 1.7;
      in.fill0 = in.fill1 = 1; in.catalystPowerAbs = 1000; in.meltingPoint = 2250; in.meltA = in.meltB = 2250;
      in.absorberLevels = new double[]{1.0}; in.stabilization = stab; in.corePower = 7500;
      s.temperature = 100; double dA = 0; boolean nan = false; double maxT = 0; int shocks = 0;
      for (int t = 1; t <= 6000; t++) {
        in.incomingSpk = 0.0095; in.damageA = in.damageB = dA;
        DfcCoreSim.Outputs o = s.tick(in); dA = o.damageA; shocks += o.shocks;
        nan |= Double.isNaN(s.temperature); maxT = Math.max(maxT, s.temperature);
      }
      System.out.printf("stab=%.1f  finalT=%.0f maxT=%.0f energy=%.0f catalystDamage=%.1f shocks=%d nan=%b%n", stab, s.temperature, maxT, s.containedEnergy, dA, shocks, nan);
    }
  }
}


/**
 * Loader-independent port of the DFC core simulation from Leafia's Cursed Addon
 * (MixinTileEntityCore.tickServer, 1.12.2) - heat, contained energy, absorber
 * transfer, shocks and catalyst damage. No Minecraft/Forge types: the Continuum
 * block entity feeds Inputs in and applies Outputs (drain tanks, add joules to
 * receivers, damage catalyst NBT, play effects).
 */
class DfcCoreSim {
    public static final double ENERGY_CAP = 2.5E8;

    public static class Inputs {
        public double incomingSpk;          // emitter beam power this tick
        public double energyMod = 1.0;      // lens/injector boost
        public double stabilization;        // from stabilizers
        public double[] absorberLevels = new double[0]; // one per linked receiver
        public double collapsing;           // 0..1 jammer collapse progress
        public boolean finalPhase;
        public long catalystPowerAbs;       // sum of both catalysts
        public float catalystPowerMod = 1, catalystHeatMod = 1, catalystFuelMod = 1;
        public int meltingPoint = 2250;
        public int meltA = 1500000, meltB = 1500000;
        public double damageA = 100, damageB = 100;
        public double corePower, coreHeat, coreFuel;
        public double fuelEff0, fuelEff1;   // FT_DFCFuel modifiers
        public double fill0, fill1;         // tank fill 0..1
    }

    public static class Outputs {
        public int fuelDrain;               // drain from each tank
        public long[] receiverJoules;       // add to each receiver.joules
        public int shocks;                  // number of shock effects to spawn
        public boolean justActivated;       // play ignite sound/shake/flash
        public double damageA, damageB;     // new catalyst "damage" NBT
    }

    // persisted state (NBT: temperature, energy, wasActive)
    public double temperature, containedEnergy;
    public boolean wasActive;
    // transient
    public double potentialGain, expellingSpk, expellingEnergy;
    public int ticks, shockCooldown;
    private final double[] expelTicks = new double[20];
    private final Random rand;

    public DfcCoreSim(Random rand) { this.rand = rand; }

    public static double stabilizationDiv(double s)    { return 1.0 + Math.sqrt(s * 10.0); }
    public static double stabilizationDivAlt(double s) { return Math.pow(s / 3.0, 2.0) * 3.0 + 1.0; }

    public Outputs tick(Inputs in) {
        Outputs out = new Outputs();
        out.receiverJoules = new long[in.absorberLevels.length];
        out.damageA = in.damageA; out.damageB = in.damageB;

        double tempRatio = temperature / in.meltingPoint;
        double energyRatio = containedEnergy / 10000.0;
        double fuelPower = in.fuelEff0 * in.fuelEff1;
        double sDiv = stabilizationDiv(in.stabilization), sDivAlt = stabilizationDivAlt(in.stabilization);
        ticks++;
        potentialGain = in.energyMod;

        if (temperature >= 100.0) {
            if (!wasActive) { wasActive = true; out.justActivated = true; }
            double randRange = Math.pow(tempRatio, 0.65) * 10.0;
            potentialGain += rand.nextDouble() * randRange / sDivAlt / sDiv
                           + Math.pow(in.collapsing, 0.666) * 66.0;
        } else wasActive = false;

        out.fuelDrain = (int) Math.ceil(Math.pow(in.incomingSpk * in.catalystFuelMod * in.coreFuel, 0.5));

        double boost = in.catalystPowerMod * in.energyMod;
        double deltaEnergy = (Math.pow(Math.pow(in.incomingSpk, 0.333) + 1.0, 0.333) - 1.0) * 6.666 / 3.0
                           * Math.pow(1.2, potentialGain);
        double add0 = (deltaEnergy * in.corePower + Math.pow(Math.max(0.0, in.incomingSpk - deltaEnergy), 0.9))
                    * boost * in.fill0 * in.fill1 / 666.0;
        double add1 = Math.pow(Math.min(temperature, 10000.0) / 100.0, 0.75) * in.corePower * potentialGain
                    * boost * in.fill0 * in.fill1 * fuelPower / 20.0 * Math.pow(0.9, potentialGain);
        add0 = Math.max(add0, 0.0); add1 = Math.max(add1, 0.0);
        containedEnergy = Math.min(Math.min(containedEnergy + add0, ENERGY_CAP) + add1, ENERGY_CAP);

        double tgt = temperature;
        tgt += Math.pow(deltaEnergy * 666.0 * in.catalystHeatMod, 2.0 / (1.0 + in.stabilization))
             * (1.0 - tempRatio / 2.0) * in.coreHeat * Math.pow(potentialGain, 0.25);

        double absorbDiv = 0.001;
        for (double l : in.absorberLevels) absorbDiv += l;
        double collapseAdd = Math.pow(in.collapsing, 2.0) * 500000.0;
        containedEnergy += Math.max(collapseAdd - add0 - add1, 0.0);
        containedEnergy = Math.min(containedEnergy, ENERGY_CAP);
        double absorbed = Math.pow(containedEnergy, 0.75 + 0.25 * (1.0 - 1.0 / (1.0 + absorbDiv))) * absorbDiv;

        double transferred = 0.0;
        for (int i = 0; i < in.absorberLevels.length; i++) {
            double lvl = in.absorberLevels[i];
            if (in.finalPhase) continue; // receivers explode instead (handled by caller)
            long absorb = (long) (absorbed / absorbDiv * lvl * 1000000.0);
            containedEnergy -= absorb / 1000000.0;
            transferred += absorb / 1000000.0;
            double val = (in.catalystPowerAbs * Math.pow(tempRatio, 0.1) + in.incomingSpk * 2000000.0) / absorbDiv * lvl;
            val = Math.min(val, 9.223372036854776E18);
            out.receiverJoules[i] = absorb + (long) val;
        }
        expelTicks[Math.floorMod(ticks, 20)] = expellingSpk = transferred;
        containedEnergy = Math.max(containedEnergy, 0.0);

        double rdc = 1.0 - energyRatio;
        tgt -= Math.pow(Math.abs(rdc), 0.5) * Math.signum(rdc) * tempRatio;
        tgt -= Math.max(0.0, Math.pow(temperature / in.meltingPoint, 4.0) * temperature * sDivAlt)
             * (0.5 + Math.pow(Math.abs(rdc), 0.01) * Math.signum(rdc) / 2.0);
        tgt = Math.min(Math.max(tgt, 0.0), 5000000.0);
        double dT = tgt - temperature;
        if (!in.finalPhase) {
            double limit = (1000 + rand.nextInt(1000)) + rand.nextDouble();
            temperature += Math.min(Math.pow(Math.abs(dT), 0.5) * Math.signum(dT), limit);
        } else {
            temperature += rand.nextInt(5000) + 10000.0 + rand.nextDouble();
        }
        temperature = Math.max(temperature, 0.0);

        if (shockCooldown > 0) shockCooldown--;
        double perShock = 225000.0;
        if (containedEnergy >= 100000.0 * (rand.nextInt(150) + 6.66) + 0.5 && shockCooldown <= 0) {
            double count = Math.ceil(containedEnergy / perShock);
            out.shocks = (int) Math.ceil(Math.pow(count, 0.25));
            containedEnergy = Math.max(containedEnergy - count * perShock, 0.0);
            shockCooldown = 100 - (int) (90.0 * Math.pow(in.collapsing, 1.75));
        }

        out.damageA = clamp(in.damageA + (temperature >= in.meltA ? 5.0 / 10.0 : -5.0 / 30.0), 0, 100);
        out.damageB = clamp(in.damageB + (temperature >= in.meltB ? 5.0 / 10.0 : -5.0 / 30.0), 0, 100);

        expellingEnergy = 0; for (double e : expelTicks) expellingEnergy += e;
        return out;
    }

    private static double clamp(double v, double lo, double hi) { return Math.max(lo, Math.min(hi, v)); }
}
