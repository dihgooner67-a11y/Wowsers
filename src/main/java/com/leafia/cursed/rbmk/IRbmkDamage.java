package com.leafia.cursed.rbmk;

/** Applied to Continuum's BlockEntityRBMKBase by RbmkBaseMixin. */
public interface IRbmkDamage {
    /** Accumulated column damage; the column melts down above 1200. */
    int cursed$getDamage();
    String cursed$status();
}
