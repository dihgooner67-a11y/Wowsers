package com.leafia.cursed.heat;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class CoolantHeatexBlock extends Block implements EntityBlock {
    public CoolantHeatexBlock(Properties p) { super(p); }

    @Nullable @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new CoolantHeatexBlockEntity(pos, state); }

    @Nullable @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide || type != HeatBlocks.COOLANT_HEATEX_BE.get()) return null;
        return (lvl, pos, st, be) -> ((CoolantHeatexBlockEntity) be).tickServer(lvl);
    }
}
