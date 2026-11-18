package com.leafia.cursed.mixin;

import com.hbm.tileentity.machine.TileEntityReactorZirnox;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Lets us trigger Continuum's own (private) ZIRNOX meltdown. */
@Mixin(value = TileEntityReactorZirnox.class, remap = false)
public interface ZirnoxInvoker {
    @Invoker("meltdown")
    void cursed$meltdown(Level level, BlockPos pos, BlockState state);
}
