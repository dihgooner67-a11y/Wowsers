package com.leafia.cursed.dfc;

/** Visual settings of a DFC core (applied to Continuum's TileEntityCore by CoreMixin, synced to clients). */
public interface IDfcLook {
    int MODE_DEFAULT = 0, MODE_SOLID = 1, MODE_GRADIENT = 2, MODE_RAINBOW = 3;

    int cursed$getColorMode();
    int cursed$getColorA();
    int cursed$getColorB();
    boolean cursed$isCrystal();
    /** 0..1: how cracked the crystal is (overload countdown / collapse progress). */
    float cursed$getCrack();

    void cursed$setLook(int mode, int colorA, int colorB);
    void cursed$setCrystal(boolean crystal);
}
