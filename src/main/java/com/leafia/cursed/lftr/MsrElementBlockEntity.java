package com.leafia.cursed.lftr;

import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** One LFTR fuel cell: holds salt, a fuel mixture and heat, and runs MsrSim each tick. */
public class MsrElementBlockEntity extends BlockEntity {
    /** ASSUMPTION: base temperature of the fuel salt (the addon read it from its fluoride fluid). */
    public static final double BASE_TEMPERATURE = 600.0;

    public final MsrSim.Cell cell = new MsrSim.Cell();
    public BlockPos control;
    private final Random rand = new Random();

    public MsrElementBlockEntity(BlockPos pos, BlockState state) { super(LftrBlocks.ELEMENT_BE.get(), pos, state); }

    /** Looks at the world around a cell for MsrSim: other cells, graphite moderators, radiation-resistant blocks. */
    public static final class LevelGrid implements MsrSim.Grid {
        private final Level level;
        public LevelGrid(Level level) { this.level = level; }

        @Override public MsrSim.Cell cell(int x, int y, int z) {
            BlockPos p = new BlockPos(x, y, z);
            if (!level.isLoaded(p)) return null;
            return level.getBlockEntity(p) instanceof MsrElementBlockEntity e ? e.cell : null;
        }

        @Override public MsrSim.Kind kind(int x, int y, int z) {
            BlockPos p = new BlockPos(x, y, z);
            if (!level.isLoaded(p)) return MsrSim.Kind.AIR;
            BlockState st = level.getBlockState(p);
            if (st.isAir()) return MsrSim.Kind.AIR;
            Block b = st.getBlock();
            if (b instanceof com.hbm.interfaces.IRadResistantBlock) return MsrSim.Kind.RESISTANT;
            String path = BuiltInRegistries.BLOCK.getKey(b).getPath();
            if (("_" + path + "_").matches(".*[^a-z]graphite[^a-z].*")) return MsrSim.Kind.MODERATOR;
            return MsrSim.Kind.OTHER;
        }
    }

    public void tickServer(Level level) {
        BlockPos p = getBlockPos();
        if (MsrSim.tick(new LevelGrid(level), p.getX(), p.getY(), p.getZ(), cell, BASE_TEMPERATURE, rand)) {
            level.playSound(null, p, SoundEvents.GENERIC_EXPLODE, SoundSource.BLOCKS, 2.0f, 0.8f);
            level.setBlock(p, Blocks.MAGMA_BLOCK.defaultBlockState(), 3); // the addon turned breached cells into slag
            return;
        }
        setChanged();
    }

    public String status() {
        StringBuilder sb = new StringBuilder(String.format("LFTR element: salt %d/%d mB, heat %.0f (breach at %.0f), control restriction %.0f%%",
            cell.amount, cell.capacity, cell.heat, MsrSim.BREACH_HEAT - BASE_TEMPERATURE, cell.restriction * 100.0));
        sb.append("\nmixture: ");
        if (cell.mixture.isEmpty()) sb.append("(none)");
        cell.mixture.forEach((k, v) -> sb.append(String.format("%s %.4f  ", k, v)));
        return sb.toString();
    }

    @Override protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt("amount", cell.amount);
        tag.putDouble("heat", cell.heat);
        tag.putDouble("restriction", cell.restriction);
        CompoundTag mix = new CompoundTag();
        cell.mixture.forEach(mix::putDouble);
        tag.put("mixture", mix);
        if (control != null) { tag.putInt("cx", control.getX()); tag.putInt("cy", control.getY()); tag.putInt("cz", control.getZ()); }
    }

    @Override public void load(CompoundTag tag) {
        super.load(tag);
        cell.amount = tag.getInt("amount");
        cell.heat = tag.getDouble("heat");
        cell.restriction = tag.getDouble("restriction");
        cell.mixture.clear();
        CompoundTag mix = tag.getCompound("mixture");
        for (String k : mix.getAllKeys()) cell.mixture.put(k, mix.getDouble(k));
        control = tag.contains("cx") ? new BlockPos(tag.getInt("cx"), tag.getInt("cy"), tag.getInt("cz")) : null;
    }
}
