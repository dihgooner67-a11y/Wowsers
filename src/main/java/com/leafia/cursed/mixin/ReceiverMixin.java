package com.leafia.cursed.mixin;

import com.hbm.tileentity.machine.dfc.TileEntityCore;
import com.hbm.tileentity.machine.dfc.TileEntityCoreReceiver;
import com.leafia.cursed.dfc.BeamContext;
import com.leafia.cursed.dfc.IDfcCore;
import com.leafia.cursed.dfc.IDfcReceiver;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Links to the core a beam just crossed, takes damage from huge joule loads, and can explode. */
@Mixin(value = TileEntityCoreReceiver.class, remap = false)
public abstract class ReceiverMixin implements IDfcReceiver {
    @Shadow public long joules;
    @Unique private double cursed$level = 1.0;
    @Unique private int cursed$destruction;

    @Inject(method = "addEnergy", at = @At("HEAD"))
    private void cursed$link(Level level, BlockPos pos, long energy, Direction dir, CallbackInfo ci) {
        TileEntityCore core = BeamContext.get();
        if (core != null) ((IDfcCore) core).cursed$addAbsorber((TileEntityCoreReceiver) (Object) this);
    }

    /** Addon rule: >= 1e11 joules in a tick builds damage; past 300 there is a 1% chance per tick to blow. */
    @Inject(method = "tickServer", at = @At("HEAD"), cancellable = true)
    private void cursed$damage(Level level, CallbackInfo ci) {
        if (level.isClientSide) return;
        if (joules >= 100_000_000_000L) {
            cursed$destruction = Math.min(cursed$destruction + level.random.nextInt(2) + 1, 400);
            if (cursed$destruction > 300 && level.random.nextInt(100) == 0) {
                cursed$explode();
                ci.cancel();
            }
        } else cursed$destruction = Math.max(cursed$destruction - 1, 0);
    }

    @Inject(method = "saveData", at = @At("TAIL"))
    private void cursed$save(DataOut output, CallbackInfo ci) { output.putInt("cursedLevel", (int) Math.round(cursed$level * 1000.0)); }

    @Inject(method = "loadData", at = @At("TAIL"))
    private void cursed$load(DataIn input, CallbackInfo ci) { cursed$level = input.getIntOr("cursedLevel", 1000) / 1000.0; }

    @Override public double cursed$getLevel() { return cursed$level; }
    @Override public void cursed$setLevel(double v) { cursed$level = Math.max(0.0, Math.min(1.0, v)); }

    @Override
    public void cursed$explode() {
        BlockEntity be = (BlockEntity) (Object) this;
        Level level = be.getLevel();
        if (level == null || level.isClientSide) return;
        BlockPos p = be.getBlockPos();
        level.removeBlock(p, false);
        level.playSound(null, p, SoundEvents.GENERIC_EXPLODE, SoundSource.BLOCKS, 4.0f, 0.8f);
        level.explode(null, p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5, 2.0f, Level.ExplosionInteraction.BLOCK);
    }
}
