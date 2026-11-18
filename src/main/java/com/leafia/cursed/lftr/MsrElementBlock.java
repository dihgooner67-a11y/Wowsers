package com.leafia.cursed.lftr;

import javax.annotation.Nullable;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class MsrElementBlock extends Block implements EntityBlock {
    public MsrElementBlock(Properties p) { super(p); }

    /** Right-click with a Continuum fuel nugget (Th-232, U-233, U-235) to dissolve it into the salt. */
    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        ItemStack stack = player.getItemInHand(hand);
        var key = ForgeRegistries.ITEMS.getKey(stack.getItem());
        String fuel = null;
        if (key != null && key.getNamespace().equals("hbm")) {
            switch (key.getPath()) {
                case "nugget_th232" -> fuel = "th232";
                case "nugget_u233" -> fuel = "u233";
                case "nugget_u235" -> fuel = "u235";
                default -> { }
            }
        }
        if (fuel == null) return InteractionResult.PASS;
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof MsrElementBlockEntity e) {
            String msg = e.addNugget(fuel);
            player.displayClientMessage(Component.literal(msg), true);
            if (msg.startsWith("Dissolved") && !player.getAbilities().instabuild) stack.shrink(1);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Nullable @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new MsrElementBlockEntity(pos, state); }

    @Nullable @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide || type != LftrBlocks.ELEMENT_BE.get()) return null;
        return (lvl, pos, st, be) -> ((MsrElementBlockEntity) be).tickServer(lvl);
    }
}
