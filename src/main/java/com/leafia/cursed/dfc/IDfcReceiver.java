package com.leafia.cursed.dfc;

/** Applied to Continuum's TileEntityCoreReceiver by ReceiverMixin. */
public interface IDfcReceiver {
    /** Absorption share this receiver takes from the core, 0..1 (addon "level"). */
    double cursed$getLevel();
    void cursed$setLevel(double level);
    /** Blows the receiver up (final phase, overload, wrong-side beam). */
    void cursed$explode();
}
