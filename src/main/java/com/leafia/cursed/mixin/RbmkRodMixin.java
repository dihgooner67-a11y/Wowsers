package com.leafia.cursed.mixin;

import com.hbm.tileentity.machine.rbmk.BlockEntityRBMKRod;
import com.hbm.tileentity.machine.rbmk.RBMKDials;
import com.leafia.cursed.rbmk.IRbmkDamage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Fuel columns: Continuum's "heat > maxHeat -> meltdown" check is neutralised, because RbmkBaseMixin
 * now decides when a column melts (damage > 1200). Matches the addon's MixinTileEntityRBMKRod.
 */
@Mixin(value = BlockEntityRBMKRod.class, remap = false)
public abstract class RbmkRodMixin {
    @Redirect(method = "tickServer", require = 1,
        at = @At(value = "INVOKE", target = "Lcom/hbm/tileentity/machine/rbmk/BlockEntityRBMKRod;maxHeat()D", remap = false))
    private double cursed$maxHeat(BlockEntityRBMKRod rod) {
        if (((IRbmkDamage) rod).cursed$getDamage() > 1200) return -100.0;           // failed: freeze flux handling
        if (rod.getLevel() != null && RBMKDials.getMeltdownsDisabled(rod.getLevel())) return rod.maxHeat();
        return Double.POSITIVE_INFINITY;                                               // keep running while damage builds
    }

    @Redirect(method = "tickServer", require = 1,
        at = @At(value = "INVOKE", target = "Lcom/hbm/tileentity/machine/rbmk/BlockEntityRBMKRod;meltdown()V", remap = false))
    private void cursed$noInstantMeltdown(BlockEntityRBMKRod rod) { /* damage system melts the column instead */ }
}
