package com.leafia.cursed.dfc;

import com.hbm.tileentity.machine.dfc.TileEntityCoreReceiver;

/** Applied to Continuum's TileEntityCore by CoreMixin. */
public interface IDfcCore {
    void cursed$addAbsorber(TileEntityCoreReceiver receiver);
    /** Called by stabilizers each tick: adds field strength and combines the lens energy modifier. */
    void cursed$stabilize(double stabilization, float lensEnergyMod);
    double cursed$getTemperature();
    double cursed$getContainedEnergy();
    int cursed$getOverloadTimer();
}
