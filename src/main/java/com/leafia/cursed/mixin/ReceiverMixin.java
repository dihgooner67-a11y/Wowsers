package com.leafia.cursed.mixin;

import com.hbm.tileentity.machine.dfc.TileEntityCore;
import com.hbm.tileentity.machine.dfc.TileEntityCoreReceiver;
import com.leafia.cursed.dfc.BeamContext;
import com.leafia.cursed.dfc.IDfcCore;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** A receiver hit by a beam that just crossed a core becomes one of that core's absorbers. */
@Mixin(value = TileEntityCoreReceiver.class, remap = false)
public abstract class ReceiverMixin {
    @Inject(method = "addEnergy", at = @At("HEAD"))
    private void cursed$link(Level level, BlockPos pos, long energy, Direction dir, CallbackInfo ci) {
        TileEntityCore core = BeamContext.get();
        if (core != null) ((IDfcCore) core).cursed$addAbsorber((TileEntityCoreReceiver) (Object) this);
    }
}
