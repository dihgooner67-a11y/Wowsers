package com.leafia.cursed.mixin;

import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.machine.dfc.TileEntityCore;
import com.hbm.tileentity.machine.dfc.TileEntityCoreStabilizer;
import com.leafia.cursed.dfc.IDfcCore;
import com.leafia.cursed.item.LCELens;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * HALO-B: lens modifiers and the addon's stronger power demand (watts^5 * drain) on top of Continuum's tick.
 * Continuum has already found the core (beam > 0) and paid watts^4; we settle the difference and report the
 * lens's field/energy contribution to the core. If the stabilizer cannot afford the addon's cost it is refunded
 * and contributes nothing this tick.
 */
@Mixin(value = TileEntityCoreStabilizer.class, remap = false)
public abstract class StabilizerMixin {
    @Shadow public long power;
    @Shadow public int watts;
    @Shadow public int beam;
    @Shadow public abstract ForgeDirection facing();
    @Shadow protected abstract ItemStack lens();

    @Inject(method = "tickServer", at = @At("TAIL"))
    private void cursed$stabilize(Level level, CallbackInfo ci) {
        if (beam <= 0) return;
        BlockPos pos = ((BlockEntity) (Object) this).getBlockPos();
        ForgeDirection dir = facing();
        BlockEntity be = level.getBlockEntity(pos.offset(dir.offsetX * beam, dir.offsetY * beam, dir.offsetZ * beam));
        if (!(be instanceof TileEntityCore core)) return;

        float field = 1.0f, drain = 1.0f, energy = 1.0f;
        if (lens().getItem() instanceof LCELens l) { field = l.fieldMod; drain = l.drainMod; energy = l.energyMod; }

        long continuumPaid = (long) Math.pow(watts, 4.0);
        long required = (long) ((float) Math.pow(watts, 5.0) * drain);
        long available = power + continuumPaid;
        if (available >= required) {
            power = available - required;
            ((IDfcCore) core).cursed$stabilize(field * (watts / 100.0), energy);
        } else {
            power = available;
            beam = 0;
        }
    }

    /** The addon raised the buffer so high-watt lenses can actually be paid for. */
    @Inject(method = "getMaxPower", at = @At("HEAD"), cancellable = true)
    private void cursed$maxPower(CallbackInfoReturnable<Long> cir) { cir.setReturnValue(10_000_000_000_000L); }
}
