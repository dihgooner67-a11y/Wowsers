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
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

/** The APR core: ticks its block entity on the server. Configured with /cursed apr ... for now (no GUI yet). */
public class AprCoreBlock extends Block implements EntityBlock {
    /** How far the core funnel is open (0 closed .. 4 fully open), following the control setting. */
    public static final IntegerProperty FUNNEL = IntegerProperty.create("funnel", 0, 4);

    public AprCoreBlock(Properties props) {
        super(props);
        registerDefaultState(stateDefinition.any().setValue(FUNNEL, 0));
    }

    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) { b.add(FUNNEL); }

    @Nullable @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new AprCoreBlockEntity(pos, state); }

    @Nullable @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide || type != AprBlocks.CORE_BE.get()) return null;
        return (lvl, pos, st, be) -> ((AprCoreBlockEntity) be).tickServer(lvl);
    }
}
