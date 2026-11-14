package com.leafia.cursed.rbmk;

import com.hbm.tileentity.machine.rbmk.BlockEntityRBMKBase;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

/** Flood-fills connected RBMK columns on one level and (un)scrams every auto control rod found. */
public final class RbmkScram {
    private RbmkScram() {}

    /** @return number of auto control rods changed */
    public static int apply(Level level, BlockPos start, boolean scram) {
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        Set<BlockPos> seen = new HashSet<>();
        queue.add(start);
        int changed = 0;
        for (int safety = 50000; !queue.isEmpty() && safety > 0; safety--) {
            BlockPos p = queue.poll();
            if (!seen.add(p) || !level.hasChunkAt(p)) continue;
            BlockEntity be = level.getBlockEntity(p);
            if (!(be instanceof BlockEntityRBMKBase)) continue;
            if (be instanceof IRbmkScram s) { s.cursed$setScrammed(scram); be.setChanged(); changed++; }
            queue.add(p.east()); queue.add(p.west()); queue.add(p.north()); queue.add(p.south());
        }
        return changed;
    }
}
