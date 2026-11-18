package com.leafia.cursed.lftr;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * LFTR control rod drive (addon MSRControlTE): extension blocks behind it set its length; elements in front of it
 * are restricted according to how far it is inserted (0..length).
 */
public class MsrControlBlockEntity extends BlockEntity {
    public static final double SPEED = 0.005;
    public double insertion, targetInsertion;
    public int length;
    private final List<BlockPos> controlled = new ArrayList<>();

    public MsrControlBlockEntity(BlockPos pos, BlockState state) { super(LftrBlocks.CONTROL_BE.get(), pos, state); }

    private Direction direction() {
        BlockState s = getBlockState();
        return s.getBlock() instanceof MsrControlBlock ? s.getValue(MsrControlBlock.FACING) : Direction.NORTH;
    }

    private int extensionLength(Level level, Direction facing) {
        Direction back = facing.getOpposite();
        BlockPos p = getBlockPos().relative(back);
        int n = 0;
        while (level.isLoaded(p) && level.getBlockState(p).getBlock() == LftrBlocks.EXTENSION.get()) { p = p.relative(back); n++; }
        return n;
    }

    private void clearElements(Level level) {
        for (BlockPos p : controlled)
            if (level.getBlockEntity(p) instanceof MsrElementBlockEntity e && getBlockPos().equals(e.control)) { e.control = null; e.cell.restriction = 0.0; }
        controlled.clear();
    }

    public void tickServer(Level level) {
        Direction dir = direction();
        length = extensionLength(level, dir);
        targetInsertion = Mth.clamp(targetInsertion, 0.0, length);
        if (targetInsertion != insertion) {
            insertion = Math.abs(targetInsertion - insertion) < SPEED ? targetInsertion : insertion + Math.signum(targetInsertion - insertion) * SPEED;
            clearElements(level);
            for (int i = 1; i <= length; i++) {
                BlockPos p = getBlockPos().relative(dir, i);
                if (!(level.getBlockEntity(p) instanceof MsrElementBlockEntity e)) continue;
                if (e.control != null && !e.control.equals(getBlockPos())) break;
                if (Math.ceil(insertion) >= i) {
                    e.control = getBlockPos();
                    controlled.add(p);
                    e.cell.restriction = Mth.clamp(insertion - i + 1.0, 0.0, 1.0);
                }
            }
            setChanged();
        }
    }

    public String status() {
        return String.format("LFTR control rod: insertion %.2f -> %.2f of %d extension block(s)", insertion, targetInsertion, length);
    }

    @Override protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putDouble("insertion", insertion);
        tag.putDouble("target", targetInsertion);
    }

    @Override public void load(CompoundTag tag) {
        super.load(tag);
        insertion = tag.getDouble("insertion");
        targetInsertion = tag.getDouble("target");
    }
}
