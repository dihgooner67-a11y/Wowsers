import java.util.*;
public class WindRun {
  public static void main(String[] a) {
    WindSim w = new WindSim(new Random(11));
    double min = 1e9, max = -1, sum = 0; int strongTicks = 0; long out = 0; int n = 200000; boolean nan = false;
    for (int t = 0; t < n; t++) { w.update(); min = Math.min(min, w.power); max = Math.max(max, w.power); sum += w.power; if (w.strongWind) strongTicks++; out += WindSim.turbineOutput(w.power, 1.0, false, 1200, 1200); if (Double.isNaN(w.power) || Double.isNaN(w.angle)) nan = true; }
    System.out.printf("wind power min=%.1f max=%.1f avg=%.1f strong %.0f%% of the time, nan=%b%n", min, max, sum / n, 100.0 * strongTicks / n, nan);
    System.out.printf("turbine average output with full lube: %.0f per tick (max possible 750)%n", out / (double) n);
    System.out.println("output at wind 40: " + WindSim.turbineOutput(40, 1.0, false, 1200, 1200) + ", obstructed: " + WindSim.turbineOutput(40, 1.0, true, 1200, 1200) + ", empty lube: " + WindSim.turbineOutput(40, 1.0, false, 0, 1200) + ", half lube: " + WindSim.turbineOutput(40, 1.0, false, 600, 1200));
  }
}


/**
 * Loader-independent port of the addon's global wind (Wind.java, 1.12.2): wind power 0..100 drifts between random targets
 * every 150-350 ticks, with a "strong wind" mode that changes every 1750-2750 ticks, and the direction slowly wanders.
 * Also the medium wind turbine's output formula.
 */
final class WindSim {
    public double power, angle, lastAngle, startPower, endPower, startAngle, endAngle;
    public boolean strongWind;
    public int shortCounter, shortLength = 200, longCounter, longLength = 2000, angleCounter, angleLength = 200;
    private final Random rand;

    public WindSim(Random rand) { this.rand = rand; }

    private static double lerp(double a, double b, double t) { return a + (b - a) * t; }
    private static double positiveModulo(double v, double m) { return ((v % m) + m) % m; }

    private double targetPower() {
        double basePower = strongWind ? 80.0 : 5.0;
        double out = power + rand.nextDouble() * 40.0 - 20.0;
        out += (basePower - power) / 4.0;
        if (out < 0.0) out = 0.0;
        if (out < basePower - 20.0) out = basePower - 20.0;
        if (out > 100.0) out = 100.0;
        if (out > basePower + 20.0) out = basePower + 20.0;
        return out;
    }

    /** One server tick. */
    public void update() {
        lastAngle = angle;
        if (rand.nextBoolean()) {
            if (++angleCounter > angleLength) {
                angleCounter = 0;
                angleLength = 180 + rand.nextInt(40);
                lastAngle = positiveModulo(lastAngle, 360.0);
                startAngle = angle = positiveModulo(angle, 360.0);
                endAngle = angle + rand.nextDouble() * 90.0 - 45.0;
            }
            angle = lerp(startAngle, endAngle, (double) angleCounter / angleLength);
        }
        shortCounter++;
        if (++longCounter > longLength) {
            longCounter = 0;
            longLength = rand.nextInt(1000) + 1750;
            strongWind = rand.nextFloat() >= 0.66f;
        }
        if (shortCounter > shortLength) {
            shortCounter = 0;
            shortLength = rand.nextInt(200) + 150;
            startPower = power;
            endPower = targetPower();
        }
        power = lerp(startPower, endPower, (double) shortCounter / shortLength);
    }

    /** Medium turbine output per tick: wind share (0..1 of the 40-power sweet spot), lubricant condition, x3 for the medium size. */
    public static long turbineOutput(double windPower, double dimensionMultiplier, boolean obstructed, int lubeFill, int lubeMax) {
        long gen = 0L;
        if (!obstructed) gen = (long) (Math.min(windPower * dimensionMultiplier * 0.5 / 40.0, 1.0) * 5000.0 / 20.0);
        double mult = Math.pow((double) lubeFill / Math.max(lubeMax, 1), 0.25) * 0.95 + 0.05;
        return (long) (gen * mult * 3.0);
    }
}
