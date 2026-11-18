package com.leafia.cursed.apr;

import javax.annotation.Nullable;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;
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

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (!(level.getBlockEntity(pos) instanceof AprCoreBlockEntity be)) return InteractionResult.PASS;
        player.displayClientMessage(Component.literal(be.useItem(player, player.getItemInHand(hand), level)), true);
        return InteractionResult.CONSUME;
    }

    @Nullable @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new AprCoreBlockEntity(pos, state); }

    @Nullable @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide || type != AprBlocks.CORE_BE.get()) return null;
        return (lvl, pos, st, be) -> ((AprCoreBlockEntity) be).tickServer(lvl);
    }
}
