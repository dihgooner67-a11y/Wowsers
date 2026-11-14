package com.leafia.cursed.fuel;

import java.util.LinkedHashMap;
import java.util.Map;

/** The addon's 59 rod types, generated from AddonItems (label, life, melting point, reactivity, emission, flags). */
public final class RodTypes {
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
