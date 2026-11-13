package com.leafia.cursed;

import com.leafia.cursed.item.LCELens;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class CursedItems {
    private CursedItems() {}
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, CursedMod.MODID);

    private static Item.Properties one() { return new Item.Properties().stacksTo(1); }

    //                                                                   maxDamage      field  drain  energy
    public static final RegistryObject<LCELens> FOCUS_BLANK   = ITEMS.register("ams_focus_blank",   () -> new LCELens(400_000L,       0.5f, 1.75f, 1.0f,  one()));
    public static final RegistryObject<LCELens> FOCUS_LIMITER = ITEMS.register("ams_focus_limiter", () -> new LCELens(2_500_000_000L, 1.5f, 1.75f, 0.8f,  one()));
    public static final RegistryObject<LCELens> FOCUS_BOOSTER = ITEMS.register("ams_focus_booster", () -> new LCELens(100_000_000L,   0.8f, 0.5f,  1.35f, one()));
    public static final RegistryObject<LCELens> FOCUS_OMEGA   = ITEMS.register("ams_focus_omega",   () -> new LCELens(1_000_000_000L, 5.0f, 10.0f, 3.5f,  one()));
    public static final RegistryObject<LCELens> FOCUS_SAFE    = ITEMS.register("ams_focus_safe",    () -> new LCELens(5_000_000_000L, 8.0f, 4.5f,  0.1f,  one()));

    /** Behaviour (throw into a collapsing DFC) arrives with the jammer/collapse logic; for now plain items. */
    public static final RegistryObject<Item> CRUCIFIX = ITEMS.register("crucifix", () -> new Item(one()));
    public static final RegistryObject<Item> FIX_TOOL = ITEMS.register("fix_tool", () -> new Item(one()));
}
