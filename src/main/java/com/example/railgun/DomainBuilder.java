package com.example.railgun;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.RailBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.RailShape;

import java.util.HashMap;
import java.util.Map;

/** Decides which block goes where for every domain, plus the hand-built scenery. */
final class DomainBuilder {
    private DomainBuilder() {}

    private static BlockState s(Block b) { return b.defaultBlockState(); }
    private static BlockState stairs(Block b, Direction d) { return b.defaultBlockState().setValue(StairBlock.FACING, d); }
    private static int hash(int x, int y, int z) { return Math.floorMod((x * 73856093) ^ (y * 19349663) ^ (z * 83492791), 100); }

    static BlockState shell(DomainType t, int dx, int dy, int dz, boolean outer) {
        int h = hash(dx, dy, dz);
        switch (t) {
            case POCKET:
                return h % 9 == 0 ? s(Blocks.SEA_LANTERN) : s(Blocks.BEDROCK);
            case SHRINE:
                if (outer) return s(Blocks.BEDROCK);
                if (h < 7) return s(Blocks.GLOWSTONE);
                if (h < 12) return s(Blocks.MAGMA_BLOCK);
                if (h < 45) return s(Blocks.NETHERRACK);
                if (h < 75) return s(Blocks.BLACKSTONE);
                return s(Blocks.NETHER_BRICKS);
            case VOID:
                return s(RailgunMod.VOID_STARS.get());
            default:
                if (outer) return s(Blocks.BEDROCK);
                if (dy < 5) return h < 55 ? s(Blocks.ORANGE_CONCRETE) : s(Blocks.YELLOW_CONCRETE);
                if (h < 4) return s(Blocks.SEA_LANTERN);
                return h < 22 ? s(Blocks.WHITE_CONCRETE) : s(Blocks.LIGHT_BLUE_CONCRETE);
        }
    }

    static BlockState floor(DomainType t, int dx, int dz, boolean top) {
        int h = hash(dx, 0, dz);
        switch (t) {
            case POCKET:
                return ((dx + dz) & 1) == 0 ? s(Blocks.BLACK_CONCRETE) : s(Blocks.WHITE_CONCRETE);
            case SHRINE:
                if (!top || h < 70) return s(Blocks.BLACKSTONE);
                if (h < 85) return s(Blocks.POLISHED_BLACKSTONE_BRICKS);
                if (h < 95) return s(Blocks.CRACKED_POLISHED_BLACKSTONE_BRICKS);
                return s(Blocks.GILDED_BLACKSTONE);
            case VOID:
                return s(RailgunMod.VOID_STARS.get());
            default:
                return top ? s(Blocks.GRASS_BLOCK) : s(Blocks.DIRT);
        }
    }

    // ---------------------------------------------------------------- scenery
    private static final class B {
        final Map<Long, BlockState> m = new HashMap<>();
        void set(int x, int y, int z, BlockState st) { m.put(BlockPos.asLong(x, y, z), st); }
        void fill(int x1, int y1, int z1, int x2, int y2, int z2, BlockState st) {
            for (int x = Math.min(x1, x2); x <= Math.max(x1, x2); x++)
                for (int y = Math.min(y1, y2); y <= Math.max(y1, y2); y++)
                    for (int z = Math.min(z1, z2); z <= Math.max(z1, z2); z++) set(x, y, z, st);
        }
    }

    static Map<Long, BlockState> structures(DomainType t, int cx, int fy, int cz) {
        B b = new B();
        switch (t) {
            case SHRINE -> shrine(b, cx, fy, cz);
            case VOID -> blackHole(b, cx, fy, cz);
            case HOMETOWN -> station(b, cx, fy, cz);
            default -> { }
        }
        return b.m;
    }

    private static void shrine(B b, int cx, int fy, int cz) {
        BlockState air = s(Blocks.AIR), white = s(Blocks.WHITE_CONCRETE), red = s(Blocks.RED_CONCRETE);
        BlockState bone = s(Blocks.BONE_BLOCK), skull = s(Blocks.SKELETON_SKULL);
        BlockState black = s(Blocks.BLACK_CONCRETE), tiles = s(Blocks.DEEPSLATE_TILES);
        BlockState lanternHang = s(Blocks.LANTERN).setValue(LanternBlock.HANGING, true);

        b.fill(cx - 9, fy + 1, cz - 9, cx + 9, fy + 1, cz + 9, s(Blocks.STONE_BRICKS));
        for (int x = -2; x <= 2; x++) b.set(cx + x, fy + 1, cz - 10, stairs(Blocks.STONE_BRICK_STAIRS, Direction.SOUTH));
        b.fill(cx - 1, fy, cz - 15, cx + 1, fy, cz - 11, s(Blocks.POLISHED_ANDESITE));

        for (int x = -5; x <= 5; x++)
            for (int z = -5; z <= 5; z++) {
                if (Math.abs(x) != 5 && Math.abs(z) != 5) continue;
                boolean pillar = (x % 5 == 0) && (z % 5 == 0);
                for (int k = 2; k <= 6; k++) b.set(cx + x, fy + k, cz + z, pillar ? red : white);
            }
        b.fill(cx - 1, fy + 2, cz - 5, cx + 1, fy + 5, cz - 5, air);

        b.fill(cx - 1, fy + 2, cz - 1, cx + 1, fy + 2, cz + 1, bone);
        b.set(cx, fy + 3, cz, bone);
        b.set(cx, fy + 4, cz, skull);
        for (int sx = -3; sx <= 3; sx += 6)
            for (int sz = -3; sz <= 3; sz += 6) {
                b.fill(cx + sx, fy + 2, cz + sz, cx + sx, fy + 3, cz + sz, bone);
                b.set(cx + sx, fy + 4, cz + sz, skull);
                b.set(cx + sx, fy + 6, cz + sz, lanternHang);
            }

        for (int i = 0; i <= 6; i++) {
            int ext = 7 - i, y = fy + 7 + i;
            for (int x = -ext; x <= ext; x++)
                for (int z = -ext; z <= ext; z++) {
                    if (Math.max(Math.abs(x), Math.abs(z)) < ext) { b.set(cx + x, y, cz + z, tiles); continue; }
                    Direction f;
                    if (Math.abs(x) == ext && Math.abs(z) < ext) f = x > 0 ? Direction.WEST : Direction.EAST;
                    else f = z > 0 ? Direction.NORTH : Direction.SOUTH;
                    b.set(cx + x, y, cz + z, stairs(Blocks.DEEPSLATE_TILE_STAIRS, f));
                }
        }
        b.set(cx, fy + 14, cz, s(Blocks.GOLD_BLOCK));
        b.set(cx, fy + 15, cz, s(Blocks.END_ROD));

        for (int sx = -4; sx <= 4; sx += 8) b.fill(cx + sx, fy + 1, cz - 20, cx + sx, fy + 8, cz - 20, red);
        b.fill(cx - 5, fy + 6, cz - 20, cx + 5, fy + 6, cz - 20, red);
        b.fill(cx - 6, fy + 9, cz - 20, cx + 6, fy + 9, cz - 20, black);
        b.fill(cx - 7, fy + 10, cz - 20, cx + 7, fy + 10, cz - 20, s(Blocks.BLACKSTONE_SLAB));

        int[][] lamps = {{-3, -8, 1}, {3, -8, 1}, {-3, -13, 0}, {3, -13, 0}, {-3, -17, 0}, {3, -17, 0}};
        for (int[] l : lamps) {
            int y0 = fy + 1 + l[2];
            b.fill(cx + l[0], y0, cz + l[1], cx + l[0], y0 + 1, cz + l[1], s(Blocks.STONE_BRICK_WALL));
            b.set(cx + l[0], y0 + 2, cz + l[1], s(Blocks.LANTERN));
        }
    }

    private static void blackHole(B b, int cx, int fy, int cz) {
        int bx = cx + 19, by = fy + 10, bz = cz;
        BlockState hole = s(Blocks.BLACK_CONCRETE);
        for (int dx = -4; dx <= 4; dx++)
            for (int dy = -4; dy <= 4; dy++)
                for (int dz = -4; dz <= 4; dz++)
                    if (dx * dx + dy * dy + dz * dz <= 16) b.set(bx + dx, by + dy, bz + dz, hole);
        double nx = 0.35, ny = 0.9, nz = 0.2, len = Math.sqrt(nx * nx + ny * ny + nz * nz);
        nx /= len; ny /= len; nz /= len;
        for (int dx = -10; dx <= 10; dx++)
            for (int dy = -10; dy <= 10; dy++)
                for (int dz = -10; dz <= 10; dz++) {
                    double dot = dx * nx + dy * ny + dz * nz;
                    if (Math.abs(dot) > 0.75) continue;
                    double r = Math.sqrt(dx * dx + dy * dy + dz * dz - dot * dot);
                    if (r < 5.5 || r > 9) continue;
                    int h = hash(dx, dy, dz);
                    Block blk = r < 6.8 ? Blocks.WHITE_CONCRETE
                            : r < 8 ? (h < 50 ? Blocks.YELLOW_CONCRETE : Blocks.ORANGE_CONCRETE)
                            : (h < 50 ? Blocks.ORANGE_CONCRETE : Blocks.RED_CONCRETE);
                    b.set(bx + dx, by + dy, bz + dz, s(blk));
                }
    }

    private static void station(B b, int cx, int fy, int cz) {
        BlockState air = s(Blocks.AIR), white = s(Blocks.WHITE_CONCRETE), blue = s(Blocks.BLUE_CONCRETE);
        BlockState glass = s(Blocks.LIGHT_BLUE_STAINED_GLASS);

        b.fill(cx - 22, fy, cz - 4, cx + 22, fy, cz - 2, s(Blocks.GRAVEL));
        b.fill(cx - 22, fy + 1, cz - 3, cx + 22, fy + 1, cz - 3, s(Blocks.RAIL).setValue(RailBlock.SHAPE, RailShape.EAST_WEST));

        b.fill(cx - 16, fy + 1, cz, cx + 16, fy + 1, cz + 6, s(Blocks.STONE_BRICKS));
        b.fill(cx - 16, fy + 1, cz, cx + 16, fy + 1, cz, s(Blocks.YELLOW_CONCRETE));

        for (int x = -13; x <= 8; x++) for (int z = -4; z <= -2; z += 2) if ((x & 3) < 2) b.set(cx + x, fy + 1, cz + z, s(Blocks.BLACK_CONCRETE));
        b.fill(cx - 14, fy + 2, cz - 5, cx + 9, fy + 2, cz - 1, s(Blocks.GRAY_CONCRETE));
        for (int x = -14; x <= 9; x++)
            for (int z = -5; z <= -1; z++) {
                boolean edge = z == -5 || z == -1 || x == -14 || x == 9;
                if (!edge) continue;
                BlockState body = x >= 3 ? blue : white;
                for (int k = 3; k <= 6; k++) b.set(cx + x, fy + k, cz + z, k == 4 ? blue : body);
                if (((x + 14) & 3) == 1 || ((x + 14) & 3) == 2) { if (z == -5 || z == -1) b.set(cx + x, fy + 5, cz + z, glass); }
            }
        for (int z = -4; z <= -2; z++) b.set(cx + 9, fy + 5, cz + z, glass);
        b.fill(cx - 8, fy + 3, cz - 1, cx - 7, fy + 5, cz - 1, air);
        b.fill(cx, fy + 3, cz - 1, cx + 1, fy + 5, cz - 1, air);
        b.fill(cx - 14, fy + 7, cz - 5, cx + 9, fy + 7, cz - 1, s(Blocks.LIGHT_GRAY_CONCRETE));
        b.fill(cx + 10, fy + 3, cz - 4, cx + 10, fy + 5, cz - 2, blue);
        b.set(cx + 10, fy + 4, cz - 3, s(Blocks.SEA_LANTERN));

        for (int x = -12; x <= 12; x += 6) b.fill(cx + x, fy + 2, cz + 6, cx + x, fy + 6, cz + 6, s(Blocks.OAK_FENCE));
        b.fill(cx - 14, fy + 7, cz, cx + 14, fy + 7, cz + 7, s(Blocks.SPRUCE_SLAB));
        for (int x = -9; x <= 9; x += 6) b.set(cx + x, fy + 6, cz + 3, s(Blocks.LANTERN).setValue(LanternBlock.HANGING, true));
        for (int x = -9; x <= 9; x += 6) b.set(cx + x + 3, fy + 2, cz + 5, stairs(Blocks.OAK_STAIRS, Direction.SOUTH));

        for (int x = -7; x <= 7; x++)
            for (int z = 9; z <= 15; z++) {
                b.set(cx + x, fy, cz + z, s(Blocks.SPRUCE_PLANKS));
                if (Math.abs(x) == 7 || z == 9 || z == 15)
                    for (int k = 1; k <= 5; k++) b.set(cx + x, fy + k, cz + z, s(Blocks.BRICKS));
            }
        for (int x = -5; x <= 5; x += 2) if (x != -1 && x != 1) b.set(cx + x, fy + 3, cz + 9, s(Blocks.GLASS_PANE));
        b.fill(cx, fy + 1, cz + 9, cx, fy + 2, cz + 9, air);
        b.fill(cx - 8, fy + 6, cz + 8, cx + 8, fy + 6, cz + 16, s(Blocks.SPRUCE_SLAB));
        b.fill(cx - 3, fy + 4, cz + 8, cx + 3, fy + 5, cz + 8, s(Blocks.WHITE_CONCRETE));
        b.set(cx, fy + 5, cz + 8, s(Blocks.SEA_LANTERN));

        for (int gx = -20; gx <= 20; gx += 5)
            for (int gz = -20; gz <= 22; gz++) {
                boolean zone = gx > -18 && gx < 18 && gz > -8 && gz < 18;
                if (zone || gx * gx + gz * gz > 19 * 19) continue;
                int h = hash(gx, 7, gz);
                if (h < 12) tree(b, cx + gx + (h % 3 - 1), fy, cz + gz, h % 2 == 0);
            }
        for (int dx = -21; dx <= 21; dx++)
            for (int dz = -21; dz <= 21; dz++) {
                if ((dx > -18 && dx < 18 && dz > -8 && dz < 18) || dx * dx + dz * dz > 21 * 21) continue;
                int h = hash(dx, 3, dz);
                long key = BlockPos.asLong(cx + dx, fy + 1, cz + dz);
                if (b.m.containsKey(key)) continue;
                if (h < 3) b.m.put(key, s(Blocks.POPPY));
                else if (h < 6) b.m.put(key, s(Blocks.DANDELION));
                else if (h < 14) b.m.put(key, s(Blocks.GRASS));
            }
    }

    private static void tree(B b, int x, int fy, int z, boolean cherry) {
        BlockState log = s(cherry ? Blocks.CHERRY_LOG : Blocks.OAK_LOG);
        BlockState leaf = s(cherry ? Blocks.CHERRY_LEAVES : Blocks.OAK_LEAVES).setValue(LeavesBlock.PERSISTENT, true);
        for (int ly = 4; ly <= 7; ly++) {
            int r = ly >= 7 ? 1 : 2;
            for (int dx = -r; dx <= r; dx++)
                for (int dz = -r; dz <= r; dz++)
                    if (Math.abs(dx) + Math.abs(dz) <= r + (ly == 5 || ly == 6 ? 1 : 0)) b.set(x + dx, fy + ly, z + dz, leaf);
        }
        b.fill(x, fy + 1, z, x, fy + 6, z, log);
    }
}
