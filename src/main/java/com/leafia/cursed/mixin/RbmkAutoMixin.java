package com.leafia.cursed.mixin;

import com.hbm.tileentity.machine.rbmk.BlockEntityRBMKControlAuto;
import com.leafia.cursed.rbmk.IRbmkScram;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** SCRAM latch for auto control rods: while scrammed the rod stays fully inserted until acknowledged. */
@Mixin(value = BlockEntityRBMKControlAuto.class, remap = false)
public abstract class RbmkAutoMixin implements IRbmkScram {
    @Shadow public double targetLevel;
    @Unique private boolean cursed$scrammed;

    @Inject(method = "tickServer", require = 1,
        at = @At(value = "INVOKE", target = "Lcom/hbm/tileentity/machine/rbmk/BlockEntityRBMKControl;tickServer()V", remap = false))
    private void cursed$scram(CallbackInfo ci) {
        if (cursed$scrammed) targetLevel = 0.0;
    }

    @Inject(method = "saveData", at = @At("TAIL"))
    private void cursed$save(DataOut out, CallbackInfo ci) { out.putInt("cursedScram", cursed$scrammed ? 1 : 0); }

    @Inject(method = "loadData", at = @At("TAIL"))
    private void cursed$load(DataIn in, CallbackInfo ci) { cursed$scrammed = in.getIntOr("cursedScram", 0) == 1; }

    @Override public boolean cursed$isScrammed() { return cursed$scrammed; }
    @Override public void cursed$setScrammed(boolean s) { cursed$scrammed = s; }
}
