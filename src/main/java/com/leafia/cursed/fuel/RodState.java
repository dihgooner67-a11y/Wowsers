package com.leafia.cursed.fuel;

/** Per-rod mutable state (the addon kept these in the item's NBT). */
public final class RodState {
    public double heat = 20.0, depletion = 0.0, decay = 0.0, incoming = 0.0, cooled = 0.0;
    public boolean melting = false, nuke = false;
    public int generosityTimer = 90, spillage = 0;
}
