import java.util.*; import java.util.function.*;
public class MsrRun {
  static class G implements MsrSim.Grid {
    Map<String, MsrSim.Cell> cells = new HashMap<>(); Map<String, MsrSim.Kind> kinds = new HashMap<>();
    String k(int x,int y,int z){return x+","+y+","+z;}
    public MsrSim.Cell cell(int x,int y,int z){return cells.get(k(x,y,z));}
    public MsrSim.Kind kind(int x,int y,int z){ return cells.containsKey(k(x,y,z)) ? MsrSim.Kind.OTHER : kinds.getOrDefault(k(x,y,z), MsrSim.Kind.AIR);} }
  static void run(String name, int n, double fuelPct, String fuel, double restriction, boolean graphite) {
    G g = new G(); Random r = new Random(5);
    for (int x = 0; x < n; x++) for (int z = 0; z < n; z++) { MsrSim.Cell c = new MsrSim.Cell(); c.amount = 1000; c.mixture.put(fuel, fuelPct); c.restriction = restriction; g.cells.put(g.k(x,0,z), c); }
    if (graphite) for (int x = -1; x <= n; x++) for (int z = -1; z <= n; z++) if (x==-1||z==-1||x==n||z==n) g.kinds.put(g.k(x,0,z), MsrSim.Kind.MODERATOR);
    double maxHeat = 0; int breach = -1; boolean nan = false;
    for (int t = 1; t <= 4000 && breach < 0; t++) {
      for (int x = 0; x < n; x++) for (int z = 0; z < n; z++) { MsrSim.Cell c = g.cells.get(g.k(x,0,z)); if (MsrSim.tick(g, x,0,z, c, 600.0, r)) breach = t; maxHeat = Math.max(maxHeat, c.heat); if (Double.isNaN(c.heat)) nan = true; }
    }
    MsrSim.Cell mid = g.cells.get(g.k(n/2,0,n/2));
    System.out.printf("%-34s heat mid=%.0f max=%.0f breachAt=%d mixture=%s nan=%b%n", name, mid.heat, maxHeat, breach, mid.mixture, nan);
  }
  public static void main(String[] a) {
    run("single cell u235 20%", 1, 0.2, "u235", 0, false);
    run("3x3 u235 20%", 3, 0.2, "u235", 0, false);
    run("3x3 u235 20% graphite", 3, 0.2, "u235", 0, true);
    run("3x3 u235 20% graphite, rods 70%", 3, 0.2, "u235", 0.7, true);
    run("3x3 th232 40% graphite", 3, 0.4, "th232", 0, true);
    run("3x3 u233 30% graphite", 3, 0.3, "u233", 0, true);
  }
}


/**
 * Loader-independent port of the addon's molten salt reactor (LFTR) cell simulation: every fuel cell holds
 * salt with a fuel mixture and a heat value; cells react with neighbours (corners and straight lines, moderated by
 * graphite), transmute fuel, share salt and heat with adjacent cells, and breach when too hot.
 * Source: MSRTEBase + MSRElementTE (1.12.2). The grid (what blocks are where) comes from the caller.
 */
final class MsrSim {
    private MsrSim() {}

    // ------------------------------------------------------------------ fuel data
    public enum Byproduct {
        TH232(1.0, "pa233"), PA233(1.0, "pa234"), PA233_DECAY(1.0, "u233"), U233(1.0, "u235");
        public final double division;
        public final String product;
        Byproduct(double division, String product) { this.division = division; this.product = product; }
    }

    public enum Fuel {
        th232(0.3, x -> Math.pow(x, 0.3), 0.3, Byproduct.TH232, 3.0E7, 0.0, null),
        pa233(1.0, x -> Math.pow(x, 0.3), 1.0, Byproduct.PA233, 10000.0, 0.03, Byproduct.PA233_DECAY),
        u233(1.0, x -> Math.pow(x, 0.85), 1.0, Byproduct.U233, 6.0E8, 0.0, null),
        u235(1.0, x -> Math.pow(x, 0.75), 1.0, null, 0.0, 0.0, null),
        pa234(1.0, x -> Math.pow(x, 0.15), 1.0, null, 0.0, 0.0, null);

        public final DoubleUnaryOperator function;
        public final double heatPerFlux, life, decayRate;
        public final Byproduct byproduct, decayProduct;

        Fuel(double heatPerFlux, DoubleUnaryOperator function, double unused, Byproduct byproduct, double life, double decayRate, Byproduct decayProduct) {
            this.heatPerFlux = heatPerFlux;
            this.function = function;
            this.byproduct = byproduct;
            this.life = life;
            this.decayRate = decayRate;
            this.decayProduct = decayProduct;
        }

        public static Fuel byName(String n) {
            for (Fuel f : values()) if (f.name().equals(n)) return f;
            return null;
        }
    }

    // ------------------------------------------------------------------ cells and grid
    public static final class Cell {
        public static final int CAPACITY = 1000;
        public int amount;                 // mB of salt
        public double heat;                // above the salt's base temperature
        public double restriction;         // 0..1, set by control rods
        public boolean breached;
        public final Map<String, Double> mixture = new LinkedHashMap<>();
        public int capacity = CAPACITY;
    }

    public enum Kind { AIR, MODERATOR, RESISTANT, REFLECTOR, OTHER }

    public interface Grid {
        Cell cell(int x, int y, int z);   // null if no cell there
        Kind kind(int x, int y, int z);
    }

    /** Temperature of the salt when cold, added to heat in reaction rates and for the breach limit. */
    public static final double BREACH_HEAT = 6000.0;

    private static void addMixture(Map<String, Double> m, String type, double amt) { m.merge(type, amt, Double::sum); }

    private static void consume(Map<String, Double> m, String key, double mix, double addAmt) {
        double left = mix - addAmt;
        if (left <= 0.0) m.remove(key); else m.put(key, left);
    }

    // ------------------------------------------------------------------ per-cell tick pieces
    /** Radioactive decay of fuel in the salt (addon: decay()). */
    public static void decay(Cell c) {
        if (c.amount <= 0) return;
        double b = c.amount / 1000.0;
        for (Map.Entry<String, Double> e : new ArrayList<>(c.mixture.entrySet())) {
            double mix = e.getValue();
            if (mix <= 0.0) continue;
            Fuel f = Fuel.byName(e.getKey());
            if (f == null || f.decayProduct == null) continue;
            double add = f.decayRate * mix * b * 0.05;
            addMixture(c.mixture, f.decayProduct.product, add / (f.byproduct == null ? 1.0 : f.byproduct.division));
            consume(c.mixture, e.getKey(), mix, add);
        }
    }

    /** This cell reacting with another cell at a given distance (addon: react()). */
    public static void react(Cell self, Cell target, double distance, double multiplier, double baseTemp) {
        if (self.amount <= 0) return;
        double b = target.amount / 2000.0 + self.amount / 2000.0;
        double y = 0.0;
        multiplier *= 1.0 - Math.max(self.restriction, target.restriction);
        multiplier /= distance / 2.0;
        if (target.amount > 0) {
            for (Map.Entry<String, Double> e : new ArrayList<>(self.mixture.entrySet())) {
                double mix = e.getValue();
                if (mix <= 0.0) continue;
                Fuel f = Fuel.byName(e.getKey());
                if (f == null) continue;
                double tempAdd = f.function.applyAsDouble(target.heat + baseTemp) * mix * b * multiplier;
                y += tempAdd;
                if (f.byproduct == null) continue;
                double add = tempAdd / f.life;
                addMixture(self.mixture, f.byproduct.product, add / f.byproduct.division);
                consume(self.mixture, e.getKey(), mix, add);
            }
        }
        double heatMg = y - self.heat;
        self.heat += Math.pow(Math.abs(heatMg), 0.2) * Math.signum(heatMg);
    }

    private static final int[][][] PLANES = {
        {{1, 0, 0}, {0, 1, 0}},   // XY
        {{0, 0, 1}, {0, 1, 0}},   // ZY (after the addon's rotateY(90))
        {{1, 0, 0}, {0, 0, 1}}    // XZ (after rotateX(90))
    };
    private static final int[][] FACES = {{1, 0, 0}, {-1, 0, 0}, {0, 1, 0}, {0, -1, 0}, {0, 0, 1}, {0, 0, -1}};

    private static Cell reactionTarget(Grid g, int x, int y, int z, Cell self) {
        if (g.kind(x, y, z) == Kind.REFLECTOR) return self;
        return g.cell(x, y, z);
    }

    /** Diagonal reactions in all three planes (addon: reactCorners). */
    public static void reactCorners(Grid g, int px, int py, int pz, Cell self, double baseTemp) {
        for (int[][] plane : PLANES) {
            int[] u = plane[0], v = plane[1];
            for (int su = -1; su <= 1; su += 2) {
                for (int sv = -1; sv <= 1; sv += 2) {
                    int ax = px + u[0] * su, ay = py + u[1] * su, az = pz + u[2] * su;
                    int bx = px + v[0] * sv, by = py + v[1] * sv, bz = pz + v[2] * sv;
                    int cx = ax + v[0] * sv, cy = ay + v[1] * sv, cz = az + v[2] * sv;
                    Kind ka = g.kind(ax, ay, az), kb = g.kind(bx, by, bz);
                    boolean modA = ka == Kind.MODERATOR, modB = kb == Kind.MODERATOR;
                    boolean resA = ka == Kind.RESISTANT, resB = ka == Kind.RESISTANT; // (sic) the addon tests blockA twice
                    Cell te;
                    if (!resA && !resB && (te = reactionTarget(g, cx, cy, cz, self)) != null)
                        react(self, te, 2.0, (modA || modB) ? 2.0 : 0.5, baseTemp);
                }
            }
        }
    }

    /** Straight-line reactions up to 4 blocks away; graphite on the way moderates (addon: reactLine). */
    public static void reactLine(Grid g, int px, int py, int pz, int face, Cell self, double baseTemp) {
        boolean moderated = false;
        for (int i = 1; i <= 4; i++) {
            int x = px + FACES[face][0] * i, y = py + FACES[face][1] * i, z = pz + FACES[face][2] * i;
            Kind k = g.kind(x, y, z);
            if (k == Kind.MODERATOR) moderated = true;
            Cell te = reactionTarget(g, x, y, z, self);
            if (te != null) { react(self, te, i, moderated ? 2.0 : 0.5, baseTemp); return; }
            if (k == Kind.RESISTANT) return;
        }
    }

    /** Equalises salt (and mixture and heat) with adjacent cells (addon: sendFluids). */
    public static void shareWithNeighbours(Grid g, int px, int py, int pz, Cell self) {
        List<Cell> neighbours = new ArrayList<>();
        Map<Cell, Integer> demand = new LinkedHashMap<>();
        int total = 0;
        for (int[] f : FACES) {
            Cell n = g.cell(px + f[0], py + f[1], pz + f[2]);
            if (n == null) continue;
            if (n.amount < self.amount) {
                int a = n.capacity - n.amount;
                int bb = (self.amount - n.amount) / 2;
                int add = Math.min(a, bb);
                total += add;
                demand.put(n, add);
            }
            neighbours.add(n);
        }
        double canSend = Math.min(total, self.amount);
        double multiplier = total > 0 ? canSend / total : 0.0;
        for (Cell n : neighbours) transferStats(self, n, neighbours.size());
        if (demand.isEmpty()) return;
        for (Map.Entry<Cell, Integer> e : demand.entrySet()) {
            int amt = (int) (e.getValue() * multiplier / demand.size());
            if (amt <= 0) continue;
            int moved = Math.min(Math.min(amt, self.amount), e.getKey().capacity - e.getKey().amount);
            e.getKey().amount += moved;
            self.amount -= moved;
        }
    }

    private static void transferStats(Cell a, Cell b, double div) {
        int sum = a.amount + b.amount;
        if (sum <= 0 || a.amount <= 0 || b.amount <= 0) return;
        Map<String, Double> mixed = new LinkedHashMap<>();
        for (Map.Entry<String, Double> e : a.mixture.entrySet()) mixed.merge(e.getKey(), e.getValue() * a.amount / sum, Double::sum);
        for (Map.Entry<String, Double> e : b.mixture.entrySet()) mixed.merge(e.getKey(), e.getValue() * b.amount / sum, Double::sum);
        a.mixture.clear(); a.mixture.putAll(mixed);
        b.mixture.clear(); b.mixture.putAll(mixed);
        double transfer = (a.heat - b.heat) / (div * 2.0);
        a.heat -= transfer;
        b.heat += transfer;
    }

    /**
     * One full tick of one cell: heat bleeds off, breach check, flow, decay, reactions.
     * @return true if the cell breached this tick (the block should turn into slag)
     */
    public static boolean tick(Grid g, int x, int y, int z, Cell c, double baseTemp, Random rand) {
        if (c.amount > 0) {
            c.heat = Math.max(0.0, c.heat * 0.99);
            if (c.heat >= BREACH_HEAT - baseTemp && rand.nextInt(350) == 0) { c.breached = true; return true; }
            shareWithNeighbours(g, x, y, z, c);
        }
        decay(c);
        reactCorners(g, x, y, z, c, baseTemp);
        for (int f = 0; f < 6; f++) reactLine(g, x, y, z, f, c, baseTemp);
        return false;
    }
}
