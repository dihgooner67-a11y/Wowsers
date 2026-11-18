package com.leafia.cursed.zirnox;

import net.minecraft.world.level.Level;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

/** Applied to Continuum's TileEntityReactorZirnox by ZirnoxMixin. */
public interface IZirnoxControl {
    /** Control rod target 0..100, or -1 to follow the reactor's on/off switch (on = 100). */
    void cursed$setRodsTarget(int target);
    void cursed$setValve(boolean open);
    String cursed$status();
    void cursed$tick(Level level, BlockPos pos, BlockState state);
}
