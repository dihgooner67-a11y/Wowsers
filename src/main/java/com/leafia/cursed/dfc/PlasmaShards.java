package com.leafia.cursed.dfc;

import com.hbm.lib.ModDamageSources;
import com.leafia.cursed.CursedMod;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * When a DFC core shatters it throws plasma shards in every direction. A shard flies (with a little gravity), leaves
 * a flame trail, and turns every block it hits into lava, losing speed each time, until it burns out.
 * No custom entity: shards live in this list and are advanced once per tick on the server.
 */
@Mod.EventBusSubscriber(modid = CursedMod.MODID)
public final class PlasmaShards {
    private PlasmaShards() {}

    private static final int MAX_SHARDS = 600, MAX_MELTS = 10, MAX_LIFE = 240;
    private static final List<Shard> SHARDS = new ArrayList<>();

    private static final class Shard {
        final ResourceKey<Level> dim;
        double x, y, z, vx, vy, vz;
        int life, melts;
        Shard(ResourceKey<Level> dim, double x, double y, double z, double vx, double vy, double vz) {
            this.dim = dim; this.x = x; this.y = y; this.z = z; this.vx = vx; this.vy = vy; this.vz = vz;
        }
    }

    public static int active() { return SHARDS.size(); }

    /** Bursts the crystal: `count` shards in random directions from the given point. */
    public static void shatter(ServerLevel level, double ox, double oy, double oz, int count) {
        RandomSource r = level.random;
        for (int i = 0; i < count && SHARDS.size() < MAX_SHARDS; i++) {
            double a = r.nextDouble() * Math.PI * 2.0;
            double c = r.nextDouble() * 2.0 - 1.0;
            double s = Math.sqrt(1.0 - c * c);
            double speed = 0.5 + r.nextDouble() * 1.0;
            SHARDS.add(new Shard(level.dimension(), ox, oy, oz, Math.cos(a) * s * speed, c * speed, Math.sin(a) * s * speed));
        }
        level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, ox, oy, oz, 1, 0, 0, 0, 0);
        level.sendParticles(ParticleTypes.END_ROD, ox, oy, oz, 80, 0.6, 0.6, 0.6, 0.35);
        level.playSound(null, BlockPos.containing(ox, oy, oz), SoundEvents.GLASS_BREAK, SoundSource.BLOCKS, 20.0f, 0.5f);
    }

    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent e) {
        if (e.phase != TickEvent.Phase.END || !(e.level instanceof ServerLevel sl) || SHARDS.isEmpty()) return;
        Iterator<Shard> it = SHARDS.iterator();
        while (it.hasNext()) {
            Shard s = it.next();
            if (s.dim != sl.dimension()) continue;
            if (!step(sl, s)) it.remove();
        }
    }

    private static boolean step(ServerLevel level, Shard s) {
        s.vy -= 0.015;
        final int sub = 2;
        for (int k = 0; k < sub; k++) {
            s.x += s.vx / sub; s.y += s.vy / sub; s.z += s.vz / sub;
            BlockPos p = BlockPos.containing(s.x, s.y, s.z);
            if (!level.isLoaded(p)) return false;
            BlockState st = level.getBlockState(p);
            if (st.isAir() || st.getFluidState().is(FluidTags.LAVA)) continue;
            if (st.getFluidState().is(FluidTags.WATER)) {                      // boils water away
                level.setBlock(p, Blocks.AIR.defaultBlockState(), 3);
                level.sendParticles(ParticleTypes.CLOUD, s.x, s.y, s.z, 6, 0.3, 0.3, 0.3, 0.05);
                continue;
            }
            if (st.getDestroySpeed(level, p) < 0.0f) return false;              // bedrock and similar stop a shard
            level.setBlock(p, Blocks.LAVA.defaultBlockState(), 3);              // melted
            level.sendParticles(ParticleTypes.LAVA, s.x, s.y, s.z, 8, 0.3, 0.3, 0.3, 0.1);
            level.playSound(null, p, SoundEvents.LAVA_EXTINGUISH, SoundSource.BLOCKS, 1.0f, 0.6f);
            s.vx *= 0.7; s.vy *= 0.7; s.vz *= 0.7;
            if (++s.melts >= MAX_MELTS) return false;
        }
        level.sendParticles(ParticleTypes.FLAME, s.x, s.y, s.z, 2, 0.05, 0.05, 0.05, 0.0);
        if ((s.life & 3) == 0) level.sendParticles(ParticleTypes.END_ROD, s.x, s.y, s.z, 1, 0.0, 0.0, 0.0, 0.0);
        if ((s.life & 1) == 0) {
            for (Entity en : level.getEntities((Entity) null, new AABB(s.x - 1.2, s.y - 1.2, s.z - 1.2, s.x + 1.2, s.y + 1.2, s.z + 1.2))) {
                en.hurt(ModDamageSources.amsCore(level), 6.0f);
                en.setSecondsOnFire(5);
            }
        }
        return ++s.life < MAX_LIFE;
    }
}
