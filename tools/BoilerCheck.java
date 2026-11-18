import java.util.*;
public class BoilerRun {
  public static void main(String[] a) {
    // synthetic chain in the style of water -> steam -> dense steam -> super dense steam
    List<HpBoilerMath.Step> chain = List.of(new HpBoilerMath.Step(1, 200, 100, 1.0), new HpBoilerMath.Step(10, 50, 1, 1.0), new HpBoilerMath.Step(10, 80, 1, 1.0), new HpBoilerMath.Step(10, 120, 1, 1.0));
    for (int c = 1; c <= 4; c++) System.out.println("compression " + c + ": " + HpBoilerMath.combine(chain, c));
    HpBoilerMath.Data d = HpBoilerMath.combine(chain, 2);
    int heat = 0, inFill = HpBoilerMath.tankSize(d.in()), outFill = 0, outMax = HpBoilerMath.tankSize(d.out()); long steam = 0;
    for (int t = 1; t <= 4000; t++) {
      int[] r = HpBoilerMath.pullHeat(heat, 750000); heat = r[0];            // an RTG heater full of heat below it
      int ops = HpBoilerMath.ops(inFill, outFill, outMax, heat, d);
      if (ops > 0) { inFill -= d.in() * ops; outFill += d.out() * ops; heat -= d.heatReq() * ops; steam += (long) d.out() * ops; }
      outFill = Math.max(0, outFill - 200000); // pipes drain the output
      if (t % 1000 == 0) System.out.printf("t=%d heat=%d in=%d out=%d steamTotal=%d%n", t, heat, inFill, outFill, steam);
    }
    System.out.println("cooling with no source from 1,000,000: " + HpBoilerMath.pullHeat(1000000, 0)[0]);
    System.out.println("none: " + HpBoilerMath.combine(List.of(), 4));
  }
}


/**
 * Loader-independent port of the addon HP Boiler's maths (HPBoilerTE.getBoiledFluid / tryConvert / tryPullHeat):
 * chains up to 4 heating steps (water -> steam -> denser steam ...) into one combined conversion.
 */
final class HpBoilerMath {
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
