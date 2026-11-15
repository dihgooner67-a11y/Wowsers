package com.leafia.cursed.apr;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/** The APR core: ticks its block entity on the server. Configured with /cursed apr ... for now (no GUI yet). */
public class AprCoreBlock extends Block implements EntityBlock {
    public AprCoreBlock(Properties props) { super(props); }

    @Nullable @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new AprCoreBlockEntity(pos, state); }

    @Nullable @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide || type != AprBlocks.CORE_BE.get()) return null;
        return (lvl, pos, st, be) -> ((AprCoreBlockEntity) be).tickServer(lvl);
    }
}
