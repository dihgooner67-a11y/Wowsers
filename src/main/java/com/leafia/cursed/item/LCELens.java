package com.leafia.cursed.item;

import com.hbm.items.machine.ItemLens;
import net.minecraft.world.item.Item;

/** Stabilizer lens with the addon's three modifiers. Values come from the 1.12.2 AddonItems. */
public class LCELens extends ItemLens {
    public final float fieldMod, drainMod, energyMod;

    public LCELens(long maxDamage, float fieldMod, float drainMod, float energyMod, Item.Properties props) {
        super(maxDamage, props);
        this.fieldMod = fieldMod;
        this.drainMod = drainMod;
        this.energyMod = energyMod;
    }
}
