package com.leafia.cursed.heat;

import com.hbm.items.machine.ItemRTGPellet;
import com.leafia.cursed.item.RodItem;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Right-click with an RTG pellet (slots 1-15) or an addon rod (rod slot); empty hand shows the status;
 * sneak + empty hand takes the rod, then the last pellet, back out.
 */
public class RtgHeaterBlock extends Block implements EntityBlock {
    public RtgHeaterBlock(Properties p) { super(p); }

    private static ItemStack oneOf(ItemStack s) { ItemStack c = s.copy(); c.setCount(1); return c; }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (!(level.getBlockEntity(pos) instanceof RtgHeaterBlockEntity heater)) return InteractionResult.PASS;
        ItemStack held = player.getItemInHand(hand);
        String msg;
        if (held.getItem() instanceof RodItem) {
            if (!heater.inventory.getStackInSlot(RtgHeaterBlockEntity.ROD_SLOT).isEmpty()) msg = "The rod slot is already in use.";
            else {
                heater.inventory.setStackInSlot(RtgHeaterBlockEntity.ROD_SLOT, oneOf(held));
                if (!player.getAbilities().instabuild) held.shrink(1);
                msg = "Rod inserted.";
            }
        } else if (held.getItem() instanceof ItemRTGPellet) {
            int free = -1;
            for (int i = 0; i < RtgHeaterBlockEntity.PELLET_SLOTS && free < 0; i++) if (heater.inventory.getStackInSlot(i).isEmpty()) free = i;
            if (free < 0) msg = "All 15 pellet slots are full.";
            else {
                heater.inventory.setStackInSlot(free, oneOf(held));
                if (!player.getAbilities().instabuild) held.shrink(1);
                msg = "Pellet inserted in slot " + (free + 1) + ".";
            }
        } else if (player.isShiftKeyDown() && held.isEmpty()) {
            int slot = -1;
            if (!heater.inventory.getStackInSlot(RtgHeaterBlockEntity.ROD_SLOT).isEmpty()) slot = RtgHeaterBlockEntity.ROD_SLOT;
            else for (int i = RtgHeaterBlockEntity.PELLET_SLOTS - 1; i >= 0 && slot < 0; i--) if (!heater.inventory.getStackInSlot(i).isEmpty()) slot = i;
            if (slot < 0) msg = "Nothing to take out.";
            else {
                ItemStack out = heater.inventory.getStackInSlot(slot);
                heater.inventory.setStackInSlot(slot, ItemStack.EMPTY);
                if (!player.getInventory().add(out)) player.drop(out, false);
                msg = "Took out " + out.getHoverName().getString() + ".";
            }
        } else msg = heater.status();
        heater.setChanged();
        player.displayClientMessage(Component.literal(msg), true);
        return InteractionResult.CONSUME;
    }

    @Nullable @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new RtgHeaterBlockEntity(pos, state); }

    @Nullable @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide || type != HeatBlocks.RTG_HEATER_BE.get()) return null;
        return (lvl, pos, st, be) -> ((RtgHeaterBlockEntity) be).tickServer(lvl);
    }
}
