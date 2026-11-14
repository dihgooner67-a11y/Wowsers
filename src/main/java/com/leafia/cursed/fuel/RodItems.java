package com.leafia.cursed.fuel;

import com.leafia.cursed.CursedMod;
import com.leafia.cursed.item.RodItem;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/** Registers one item per rod type: cursed:rod_<functionId>. */
public final class RodItems {
    private RodItems() {}
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, CursedMod.MODID);
    public static final Map<String, RegistryObject<RodItem>> BY_ID = new LinkedHashMap<>();

    static {
        for (RodType t : RodTypes.ALL.values())
            BY_ID.put(t.functionId, ITEMS.register("rod_" + t.functionId, () -> new RodItem(t)));
    }

    /** Tint for the rod overlay layer, by fuel family (placeholder for the addon's billet-composited look). */
    public static int color(String id) {
        if (id.startsWith("depleted")) return 0x6B6B6B;
        if (id.startsWith("pu") || id.contains("pu") ) return 0xC0392B;
        if (id.contains("am24")) return 0x8E44AD;
        if (id.contains("np237")) return 0x16A5A5;
        if (id.contains("sa32") || id.contains("saus")) return 0x2EC4F0;
        if (id.startsWith("th")) return 0x3B6FD6;
        if (id.contains("u23") || id.equals("nu") || id.startsWith("meu") || id.startsWith("heu") || id.startsWith("leu")) return 0x4CAF50;
        int h = id.hashCode();
        return 0x606060 + ((h & 0x7F) << 16 | ((h >> 7) & 0x7F) << 8 | ((h >> 14) & 0x7F));
    }
}
