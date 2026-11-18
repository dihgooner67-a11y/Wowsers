package com.leafia.cursed.client;

/** Applied to Continuum's CoreRenderState (client) so it can carry our look settings to the renderer. */
public interface ILookState {
    void cursed$set(int mode, int colorA, int colorB, boolean crystal, float crack);
    int cursed$mode();
    int cursed$colorA();
    int cursed$colorB();
    boolean cursed$crystal();
    float cursed$crack();
}
