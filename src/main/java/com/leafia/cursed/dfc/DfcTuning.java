package com.leafia.cursed.dfc;

/** Tunables. Values marked ASSUMPTION need in-game balancing against Continuum. */
public final class DfcTuning {
    private DfcTuning() {}
    /** Emitter joules -> SPK. Same constant as the 1.12.2 addon (out / 1,000,000). */
    public static final double SPK_PER_JOULE = 1.0E-6;
    public static final int DEFAULT_MELTING_POINT = 2250;
    public static final int OVERLOAD_TICKS = 120;
    /** ASSUMPTION: Continuum has no jam logic, so a Field Disturber block within this many chunks of the core counts as a jammer. */
    public static final int JAMMER_RANGE_CHUNKS = 8;
    public static final double COLLAPSE_SECONDS = 120.0, FINAL_PHASE_SECONDS = 15.0;
    /** Addon blast radius is 20 + temperature^0.4; capped for safety. */
    public static final int MAX_BLAST = 250;
    public static final int CRUCIFIX_BASE_FAIL = 80, CRUCIFIX_FAIL_PER_STABILIZER = 10;
    public static final double TIME_TO_MELTDOWN = 10.0, TIME_TO_REGEN = 30.0;
    public static final int CATALYST_FALLBACK_MELT = 1_500_000;

    /** Catalyst melting points, copied from the addon's LCEItemCatalyst. Key = registry path. */
    public static int meltingPoint(String registryPath) {
        return switch (registryPath) {
            case "ams_catalyst_iron", "ams_catalyst_niobium", "ams_catalyst_cerium" -> 2900;
            case "ams_catalyst_copper", "ams_catalyst_aluminium" -> 2250;
            case "ams_catalyst_lithium" -> 600;
            case "ams_catalyst_beryllium" -> 1287;
            case "ams_catalyst_tungsten" -> 3422;
            case "ams_catalyst_cobalt" -> 1495;
            case "ams_catalyst_thorium" -> 800;
            case "ams_catalyst_strontium" -> 3200;
            case "ams_catalyst_caesium" -> 1952;
            case "ams_catalyst_schrabidium" -> 3000;
            case "ams_catalyst_euphemium" -> 4000;
            case "ams_catalyst_dineutronium" -> 6000;
            default -> CATALYST_FALLBACK_MELT;
        };
    }
}
