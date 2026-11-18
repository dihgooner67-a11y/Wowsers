package com.leafia.cursed.mixin;

import com.hbm.tileentity.machine.dfc.TileEntityCoreEmitter;
import com.leafia.cursed.dfc.BeamContext;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Brackets each beam so a receiver only links to a core the same beam passed through. */
@Mixin(value = TileEntityCoreEmitter.class, remap = false)
public abstract class EmitterMixin {
    @Inject(method = "fire", at = @At("HEAD"))
    private void cursed$beamStart(Level level, BlockPos pos, CallbackInfo ci) { BeamContext.clear(); }

    @Inject(method = "fire", at = @At("RETURN"))
    private void cursed$beamEnd(Level level, BlockPos pos, CallbackInfo ci) { BeamContext.clear(); }
}
