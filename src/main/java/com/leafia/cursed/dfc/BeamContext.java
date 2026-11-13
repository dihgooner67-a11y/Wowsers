package com.leafia.cursed.dfc;

import com.hbm.tileentity.machine.dfc.TileEntityCore;

/** Remembers which core the current emitter beam just passed through, so the next receiver can register as its absorber. */
public final class BeamContext {
    private BeamContext() {}
    private static final ThreadLocal<TileEntityCore> CURRENT = new ThreadLocal<>();
    public static void set(TileEntityCore core) { CURRENT.set(core); }
    public static TileEntityCore get() { return CURRENT.get(); }
    public static void clear() { CURRENT.remove(); }
}
