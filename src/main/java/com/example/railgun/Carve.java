package com.example.railgun;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

final class Carve {
    private Carve() {}

    /** Removes every block in a sphere (no drops). Skips unbreakable blocks, blocks harder than maxHard and chests etc. */
    static void sphere(ServerLevel w, Vec3 c, double r, float maxHard) {
        int ri = (int) Math.ceil(r);
        BlockPos base = BlockPos.containing(c);
        double r2 = r * r;
        BlockState air = Blocks.AIR.defaultBlockState();
        for (int dx = -ri; dx <= ri; dx++)
            for (int dy = -ri; dy <= ri; dy++)
                for (int dz = -ri; dz <= ri; dz++) {
                    if (dx * dx + dy * dy + dz * dz > r2) continue;
                    BlockPos q = base.offset(dx, dy, dz);
                    if (q.getY() < w.getMinBuildHeight() || q.getY() >= w.getMaxBuildHeight()) continue;
                    if (!w.hasChunkAt(q)) continue;
                    BlockState s = w.getBlockState(q);
                    if (s.isAir() || s.hasBlockEntity()) continue;
                    float hd = s.getDestroySpeed(w, q);
                    if (hd < 0 || hd > maxHard) continue;
                    w.setBlock(q, air, Block.UPDATE_CLIENTS);
                }
    }
}
