import java.util.*;
public class StructRun {
  static void show(String name, List<Integer> radii) {
    Map<AprStructure.Pos, AprStructure.Kind> m = AprStructure.requirement(radii);
    Map<AprStructure.Kind,Integer> c = new TreeMap<>();
    for (AprStructure.Kind k : m.values()) c.merge(k, 1, Integer::sum);
    int minY=0,maxY=0,maxR=0; for (AprStructure.Pos p : m.keySet()) { minY=Math.min(minY,p.y()); maxY=Math.max(maxY,p.y()); maxR=Math.max(maxR,Math.max(Math.abs(p.x()),Math.abs(p.z()))); }
    System.out.printf("%-14s total=%d y=[%d..%d] reach=%d %s validate=%s%n", name, m.size(), minY, maxY, maxR, c, AprStructure.validate(radii));
  }
  public static void main(String[] a) {
    show("r=6", List.of(6));
    show("r=6,10", List.of(6,10));
    show("r=12", List.of(12));
    show("bad r=4", List.of(4));
    show("overlap 6,7", List.of(6,7));
    Map<AprStructure.Pos, AprStructure.Kind> m = AprStructure.requirement(List.of(6));
    System.out.println("center shaft (0,-1,0)=" + m.get(new AprStructure.Pos(0,-1,0)) + "  outlet (0,-5,0)=" + m.get(new AprStructure.Pos(0,-5,0)) + "  coil (0,-2,2)=" + m.get(new AprStructure.Pos(0,-2,2)));
  }
}


/**
 * The APR multiblock requirement, ported from APRCoreTE.rebuildMBRequirement / APRCoreBlock.generateTorus
 * (1.12.2 addon). Offsets are relative to the core block. Later entries override earlier ones, exactly as in the
 * addon, so the central 3x3x4 shaft ends up as air. A HULL position also accepts an already assembled hull.
 */
final class AprStructure {
    private AprStructure() {}

    public enum Kind { AIR, PLATING, PLATING_DARK, MECHANICAL, COIL, SUPPORT, HULL, BLADES, OUTLET }

    public record Pos(int x, int y, int z) {}

    public static final int MIN_RADIUS = 6, MAX_RADIUS = 32;

    public static Map<Pos, Kind> requirement(List<Integer> radii) {
        Map<Pos, Kind> m = new LinkedHashMap<>();
        if (!radii.isEmpty()) {
            int r = 0;
            for (int v : radii) r = Math.max(r, v);
            for (int x = -r; x <= r; x++) {
                for (int z = -r; z <= r; z++) {
                    if (Math.sqrt(x * x + z * z) > r) continue;
                    Kind cover = (x == 0 || z == 0) ? Kind.PLATING_DARK : Kind.PLATING;
                    if (x == 0 && z == 0) cover = Kind.OUTLET;
                    m.put(new Pos(x, -1, z), cover);
                    m.put(new Pos(x, -2, z), Kind.MECHANICAL);
                    m.put(new Pos(x, -3, z), Kind.MECHANICAL);
                    m.put(new Pos(x, -4, z), Kind.MECHANICAL);
                    m.put(new Pos(x, -5, z), cover);
                }
            }
            for (int x = -1; x <= 1; x++)
                for (int y = -1; y >= -4; y--) { m.put(new Pos(x, y, 2), Kind.COIL); m.put(new Pos(x, y, -2), Kind.COIL); }
            for (int z = -1; z <= 1; z++)
                for (int y = -1; y >= -4; y--) { m.put(new Pos(2, y, z), Kind.COIL); m.put(new Pos(-2, y, z), Kind.COIL); }
            for (int x = -1; x <= 1; x++)
                for (int z = -1; z <= 1; z++)
                    for (int y = -1; y >= -4; y--) m.put(new Pos(x, y, z), Kind.AIR);
        }
        for (int rad : radii) torus(rad, m);
        return m;
    }

    private static void torus(double radius, Map<Pos, Kind> m) {
        circle(-1, radius + 0.5, m, 3, false);
        circle(-1, radius - 0.5, m, 3, false);
        circle(-1, radius + 1.0, m, 3, false);
        circle(-1, radius - 1.0, m, 3, false);
        circle(0, radius, m, 5, true);
    }

    private static void circle(int cy, double radius, Map<Pos, Kind> m, int depth, boolean inner) {
        double subdivs = Math.floor(radius * 2.0 * Math.PI / 4.0);
        for (int i = 0; i <= subdivs; i++) {
            double theta = Math.toRadians((i / subdivs) * 90.0);
            double d = radius - 0.25;
            int ox = (int) Math.floor(0.5 + d * Math.sin(theta));
            int oz = (int) Math.floor(0.5 - d * Math.cos(theta));
            Kind state = (ox == 0 || oz == 0) ? Kind.SUPPORT : Kind.HULL;
            for (int y = 0; y >= -depth; y--) {
                Kind layer = inner && y < 0 && y > -depth ? Kind.BLADES : state;
                m.put(new Pos(ox, cy + y, oz), layer);
                m.put(new Pos(-ox, cy + y, oz), layer);
                m.put(new Pos(ox, cy + y, -oz), layer);
                m.put(new Pos(-ox, cy + y, -oz), layer);
            }
        }
    }

    /** Capacity of a chamber's tanks, and the main tank is the sum over chambers. */
    public static int capacity(int radius) { return radius * 50000; }

    public static String validate(List<Integer> radii) {
        if (radii.isEmpty()) return "Set at least one chamber radius (6-32).";
        int last = -100;
        for (int r : radii) {
            if (r < MIN_RADIUS || r > MAX_RADIUS) return "Chamber radius " + r + " is outside " + MIN_RADIUS + "-" + MAX_RADIUS + ".";
            if (r - last < 3) return "Chambers need a gap of at least 3 blocks (" + last + " and " + r + " overlap).";
            last = r;
        }
        return null;
    }
}
