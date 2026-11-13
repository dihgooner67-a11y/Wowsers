package com.leafia.cursed.dfc;

import com.hbm.tileentity.machine.dfc.TileEntityCoreReceiver;

/** Applied to Continuum's TileEntityCore by CoreMixin. */
public interface IDfcCore {
    void cursed$addAbsorber(TileEntityCoreReceiver receiver);
    double cursed$getTemperature();
    double cursed$getContainedEnergy();
    int cursed$getOverloadTimer();
}
