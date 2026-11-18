package com.leafia.cursed.fuel;

import java.util.LinkedHashMap;
import java.util.Map;

/** The addon's 59 rod types, generated from AddonItems (label, life, melting point, reactivity, emission, flags, base item). */
public final class RodTypes {
    private RodTypes() {}
    public static final Map<String, RodType> ALL = new LinkedHashMap<>();
    static {
        add("U-238", 4000000.0, 0.0, 1.0, 1.0, "depleteduranium238", false, false, false, "billet_u238");
        add("NU", 4000000.0, 2865.0, 0.4, 1.0, "depletednaturaluranium", false, false, false, "billet_uranium");
        add("MEU-235", 4000000.0, 2865.0, 1.0, 1.0, "depleteduranium", false, false, false, "billet_uranium_fuel");
        add("HEU-233", 1000000.0, 2865.0, 2.0, 1.0, "depleteduranium233", false, false, false, "billet_u233");
        add("HEU-235", 3000000.0, 2865.0, 1.75, 1.0, "depleteduranium235", false, false, false, "billet_u235");
        add("ThMEU", 3500000.0, 3350.0, 1.0, 1.0, "depletedthorium", false, false, false, "billet_thorium_fuel");
        add("LEPu-239", 5000000.0, 2744.0, 0.6666666666666666, 1.0, "depletedplutonium", false, false, false, "billet_plutonium_fuel");
        add("MEPu-239", 5000000.0, 2744.0, 1.0, 1.0, "depletedplutonium", false, false, false, "billet_pu_mix");
        add("HEPu-239", 5000000.0, 2744.0, 1.25, 1.0, "pu240", false, false, false, "billet_pu239");
        add("HEPu-241", 1.8E7, 2744.0, 1.45, 1.0, "depletedplutonium", false, false, false, "billet_pu241");
        add("LEAm-242", 1.8E7, 3986.0, 1.0, 1.0, null, false, false, false, "billet_americium_fuel");
        add("MEAm-242", 1.8E7, 3986.0, 1.0, 1.0, null, false, false, false, "billet_am_mix");
        add("HEAm-241", 1.8E7, 3986.0, 1.0, 1.0, null, false, true, false, "billet_am241");
        add("HEAm-242", 1.8E7, 3986.0, 1.0, 1.0, null, false, false, false, "billet_am242");
        add("MENp-237", 5200000.0, 2800.0, 1.0, 1.0, null, false, false, true, "billet_neptunium_fuel");
        add("HENp-237", 3200000.0, 2800.0, 1.0, 1.0, null, false, true, false, "billet_neptunium");
        add("MOX", 6000000.0, 2815.0, 1.0, 1.0, null, false, false, false, "billet_mox_fuel");
        add("LESa-326", 3500000.0, 2500.0, 1.0, 0.75, "depletedschrabidium", true, false, false, "billet_les");
        add("MESa-326", 3000000.0, 2750.0, 1.0, 1.0, "depletedmesa326", false, false, false, "billet_schrabidium_fuel");
        add("HESa-326", 2500000.0, 3000.0, 1.0, 1.25, "depletedhesa326", false, false, false, "billet_hes");
        add("Sa-326", 2000000.0, 3250.0, 1.0, 2.25, "depletedsa326", false, false, false, "billet_schrabidium");
        add("LEAus", 5500000.0, 7029.0, 1.0, 0.3, "heaus", false, false, false, "billet_australium_lesser");
        add("HEAus", 5500000.0, 5211.0, 1.0, 1.2, null, false, false, false, "billet_australium_greater");
        add("Ra-226Be", 400000.0, 700.0, 1.0, 1.0, null, true, false, false, "billet_ra226be");
        add("Po-210Be", 2000000.0, 1287.0, 1.0, 1.0, null, true, false, false, "billet_po210be");
        add("Pu-238Be", 2000000.0, 2744.0, 1.0, 1.0, null, true, false, false, "billet_pu238be");
        add("Pu-238", 3500000.0, 2744.0, 1.0, 1.0, null, false, false, false, "billet_pu238");
        add("Po-210", 3000000.0, 1287.0, 1.0, 1.0, null, false, false, false, "billet_polonium");
        add("Sa-327", 8000000.0, 2250.0, 0.022222222222222223, 65.0, "depletedsa327", false, false, false, "billet_solinium");
        add("Lithium", 3000.0, 0.0, 1.0, 1.0, null, false, false, false, "lithium");
        add("Pu-240", 6.5E7, 2744.0, 1.0, 1.0, null, false, false, false, "billet_pu240");
        add("Depleted Uranium", 0.0, 0.0, 1.0, 1.0, null, false, false, false, "waste_uranium");
        add("Depleted Natural Uranium", 0.0, 0.0, 1.0, 1.0, null, false, false, false, "waste_natural_uranium");
        add("Depleted Uranium-233", 0.0, 0.0, 1.0, 1.0, null, false, false, false, "waste_u233");
        add("Depleted Uranium-235", 0.0, 0.0, 1.0, 1.0, null, false, false, false, "waste_u235");
        add("Depleted Uranium-238", 0.0, 0.0, 1.0, 1.0, null, false, false, false, null);
        add("Depleted Plutonium", 0.0, 0.0, 1.0, 1.0, null, false, false, false, "waste_plutonium");
        add("Depleted Thorium", 0.0, 0.0, 1.0, 1.0, null, false, false, false, "waste_thorium");
        add("Depleted MOX", 0.0, 0.0, 1.0, 1.0, null, false, false, false, "waste_mox");
        add("Depleted Schrabidium", 0.0, 0.0, 1.0, 1.0, null, false, false, false, "waste_schrabidium");
        add("Depleted MESa-326", 0.0, 0.0, 1.0, 1.0, null, false, false, false, null);
        add("Depleted HESa-326", 0.0, 0.0, 1.0, 1.0, null, false, false, false, null);
        add("Depleted Sa-326", 0.0, 0.0, 1.0, 1.0, null, false, false, false, null);
        add("Depleted Sa-327", 0.0, 0.0, 1.0, 1.0, null, false, false, false, null);
        add("Lead", 9.9999999E7, 0.0, 1.0, 1.0, "pb209", false, false, false, "ingot_lead");
        add("Balefire", 200.0, 8652.0, 0.16666666666666666, 8.0, "blazingbalefire", false, false, false, "egg_balefire");
        add("Blazing Balefire", 65000.0, 8652.0, 0.1111111111111111, 12.0, "balefire", false, false, false, null);
        add("NPu", 2600000.0, 2744.0, 1.0, 1.0, null, false, false, false, "billet_plutonium");
        add("Th-232", 90000.0, 0.0, 1.0, 1.0, "thmeu", false, false, false, "billet_th232");
        add("Au-198", 900000.0, 3850.0, 1.0, 1.0, null, false, false, false, "billet_au198");
        add("Pb-209", 600000.0, 4300.0, 1.0, 1.0, null, false, false, false, "billet_pb209");
        add("Debug", 0.0, 0.0, 1.0, 1.0, null, false, false, false, "wand_d");
        add("Potential Instant Blowout Applicator", 9.5E7, 6.5E8, 1.0, 1.0, null, false, false, false, "meltdown_tool");
        add("Yh-XXX", 6666666.0, 3200.0, 1.0, 1.0, null, false, false, false, "billet_yharonite");
        add("DG-OMEGA", Double.MAX_VALUE, 1916169.0, 2.0, 1.0, null, false, false, false, "particle_digamma");
        add("Kys-3000", Double.MAX_VALUE, 22.0, 1.0, 1.0, null, false, false, false, null);
        add("Shitfuckium", Double.MAX_VALUE, 3200.0, 1.0, 1.0, null, false, false, false, "rbmk_pellet_drx");
        add("Catsmilium", Double.MAX_VALUE, Double.MAX_VALUE, 1.0, 1.0, null, false, false, false, null);
        add("Confusium", Double.MAX_VALUE, 1300.0, 1.0, 1.0, null, false, false, false, null);
    }

    private static void add(String label, double life, double melt, double reactivity, double emission,
                            String decayProduct, boolean moderated, boolean preferFast, boolean preferAny, String baseItem) {
        RodType t = new RodType(label, life, melt, reactivity, emission, decayProduct, moderated, preferFast, preferAny, baseItem);
        ALL.put(t.functionId, t);
    }
}
