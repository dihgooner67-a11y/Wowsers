package com.leafia.cursed.mixin;

import com.hbm.client.render.CoreRenderState;
import com.leafia.cursed.client.ILookState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

/** Client only: carries the DFC look settings from extract() to submit(). */
@Mixin(value = CoreRenderState.class, remap = false)
public abstract class CoreRenderStateMixin implements ILookState {
    @Unique private int cursed$mode, cursed$a, cursed$b;
    @Unique private boolean cursed$crystal = true;
    @Unique private float cursed$crack;

    @Override public void cursed$set(int mode, int a, int b, boolean crystal, float crack) {
        cursed$mode = mode; cursed$a = a; cursed$b = b; cursed$crystal = crystal; cursed$crack = crack;
    }
    @Override public int cursed$mode() { return cursed$mode; }
    @Override public int cursed$colorA() { return cursed$a; }
    @Override public int cursed$colorB() { return cursed$b; }
    @Override public boolean cursed$crystal() { return cursed$crystal; }
    @Override public float cursed$crack() { return cursed$crack; }
}
