package com.example.railgun;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Builds a domain (clears the interior, lays floor + scenery, seals the shell), runs its effects,
 * then restores every original block. Work is spread over many ticks to avoid lag spikes.
 */
public final class Domains {
    private static final int FLAGS = Block.UPDATE_CLIENTS;   // no neighbour updates -> nothing pops off

    private record E(BlockPos pos, BlockState state) {}

    private static final class Spell {
        final ServerLevel level; final UUID owner; final BlockPos center; final DomainType type; final int floorY;
        final List<BlockPos> order = new ArrayList<>();
        final List<BlockState> neu = new ArrayList<>();
        final List<BlockState> old = new ArrayList<>();
        int n1, placed, age, holdStart, phase;      // phase 0 build, 1 hold, 2 restore
        boolean teleported;
        Spell(ServerLevel l, UUID o, BlockPos c, DomainType t, int fy) { level = l; owner = o; center = c; type = t; floorY = fy; }
    }
    private static final List<Spell> ACTIVE = new ArrayList<>();

    private Domains() {}

    public static boolean canStart(ServerPlayer p, DomainType t) {
        BlockPos c = p.blockPosition();
        for (Spell s : ACTIVE) {
            if (s.owner.equals(p.getUUID())) return false;
            if (s.level == p.serverLevel()) {
                double lim = s.type.radius + t.radius + 2;
                if (s.center.distSqr(c) < lim * lim) return false;
            }
        }
        return true;
    }

    public static void start(ServerPlayer p, DomainType t) {
        ServerLevel w = p.serverLevel();
        final BlockPos c = p.blockPosition();
        final int R = t.radius, H = t.height;
        final int floorY = c.getY() - 1;
        Spell s = new Spell(w, p.getUUID(), c, t, floorY);
        Map<Long, BlockState> over = t.sphere ? Map.of() : DomainBuilder.structures(t, c.getX(), floorY, c.getZ());
        BlockState air = Blocks.AIR.defaultBlockState();

        List<E> a = new ArrayList<>(), st = new ArrayList<>(), sh = new ArrayList<>();
        double in = R - 1.8, mid = R - 0.9;
        int bottom = w.getMinBuildHeight(), top = w.getMaxBuildHeight();

        for (int dx = -R; dx <= R; dx++)
            for (int dz = -R; dz <= R; dz++) {
                int h2 = dx * dx + dz * dz;
                if (h2 > R * R) continue;
                int x = c.getX() + dx, z = c.getZ() + dz;
                if (!w.hasChunkAt(new BlockPos(x, c.getY(), z))) continue;
                int dyMin = t.sphere ? -R : -1, dyMax = t.sphere ? R : H;
                for (int dy = dyMin; dy <= dyMax; dy++) {
                    int y = t.sphere ? c.getY() + dy : floorY + dy;
                    if (y < bottom || y >= top) continue;
                    boolean shell = false;
                    BlockState ns;
                    if (t.sphere) {
                        int d2 = h2 + dy * dy;
                        if (d2 > R * R) continue;
                        double d = Math.sqrt(d2);
                        if (d > in) { shell = true; ns = DomainBuilder.shell(t, dx, dy, dz, d > mid); }
                        else ns = (y == floorY) ? DomainBuilder.floor(t, dx, dz, true) : air;
                    } else if (dy <= 0) {
                        ns = DomainBuilder.floor(t, dx, dz, dy == 0);
                    } else {
                        double eo = h2 / (double) (R * R) + (dy * dy) / (double) (H * H);
                        if (eo > 1) continue;
                        double ei = h2 / (in * in) + (dy * dy) / ((H - 1.8) * (H - 1.8));
                        if (ei > 1) {
                            shell = true;
                            double em = h2 / (mid * mid) + (dy * dy) / ((H - 0.9) * (H - 0.9));
                            ns = DomainBuilder.shell(t, dx, dy, dz, em > 1);
                        } else ns = air;
                    }
                    BlockPos pos = new BlockPos(x, y, z);
                    BlockState old = w.getBlockState(pos);
                    if (old.hasBlockEntity()) continue;               // never touch chests etc.
                    if (shell) {
                        if (old != ns) sh.add(new E(pos, ns));
                    } else {
                        if (old != ns) a.add(new E(pos, ns));
                        BlockState so = over.get(pos.asLong());
                        if (so != null && so != ns) st.add(new E(pos, so));
                    }
                }
            }

        a.sort(Comparator.comparingDouble((E e) -> e.pos().distSqr(c)));
        Comparator<E> byY = Comparator.comparingInt((E e) -> e.pos().getY()).thenComparingDouble(e -> e.pos().distSqr(c));
        st.sort(byY);
        sh.sort(byY);
        for (E e : a) { s.order.add(e.pos()); s.neu.add(e.state()); }
        s.n1 = a.size();
        for (E e : st) { s.order.add(e.pos()); s.neu.add(e.state()); }
        for (E e : sh) { s.order.add(e.pos()); s.neu.add(e.state()); }
        ACTIVE.add(s);

        Vec3 cv = Vec3.atCenterOf(c);
        send(w, cv, t.id, 0);
        play(w, cv, SoundEvents.WITHER_SPAWN, 4f, 0.5f);
    }

    public static void tick(MinecraftServer server) {
        Iterator<Spell> it = ACTIVE.iterator();
        while (it.hasNext()) {
            Spell s = it.next();
            s.age++;
            DomainType t = s.type;
            int total = s.order.size();
            Vec3 cv = Vec3.atCenterOf(s.center);

            if (s.phase == 0) {
                int per = Math.max(1, Math.min(3000, (int) Math.ceil(total / (double) t.buildTicks)));
                for (int i = 0; i < per && s.placed < total; i++) {
                    if (!s.teleported && s.placed >= s.n1) teleportOwner(s);
                    place(s, s.placed++);
                }
                if (s.age % 4 == 0)
                    s.level.sendParticles(ParticleTypes.PORTAL, cv.x, cv.y + 1, cv.z, 40, 3, 3, 3, 0.8);
                if (s.placed >= total) {
                    if (!s.teleported) teleportOwner(s);
                    s.phase = 1;
                    s.holdStart = s.age;
                    send(s.level, cv, t.id, 1);
                    PhotonBridge.spawn(s.level, "domain_" + t.name().toLowerCase(Locale.ROOT), cv, new Vec3(0, 0, 1), t.radius / 10f);
                    play(s.level, cv, SoundEvents.WARDEN_SONIC_BOOM, 5f, 0.5f);
                    play(s.level, cv, SoundEvents.LIGHTNING_BOLT_IMPACT, 5f, 0.6f);
                }
            } else if (s.phase == 1) {
                effects(s);
                if (s.age - s.holdStart >= t.holdTicks) {
                    s.phase = 2;
                    send(s.level, cv, t.id, 2);
                    play(s.level, cv, SoundEvents.BEACON_DEACTIVATE, 5f, 0.7f);
                }
            } else {
                int per = Math.max(1, Math.min(3500, (int) Math.ceil(total / (double) t.restoreTicks)));
                for (int i = 0; i < per && s.placed > 0; i++) restore(s, --s.placed);
                if (s.placed == 0) it.remove();
            }
        }
    }

    public static void restoreAll() {
        for (Spell s : ACTIVE) while (s.placed > 0) restore(s, --s.placed);
        ACTIVE.clear();
    }

    // ---------------------------------------------------------------- effects while sealed
    private static void effects(Spell s) {
        DomainType t = s.type;
        if (t == DomainType.POCKET) return;
        ServerLevel w = s.level;
        double R = t.radius;
        AABB box = new AABB(s.center.getX() - R, s.floorY - 1, s.center.getZ() - R,
                s.center.getX() + R + 1, s.floorY + t.height + 2, s.center.getZ() + R + 1);
        Vec3 cv = Vec3.atCenterOf(s.center);
        List<LivingEntity> list = w.getEntitiesOfClass(LivingEntity.class, box,
                e -> e.isAlive() && e.distanceToSqr(cv.x, e.getY(), cv.z) <= R * R);
        Entity ownerE = w.getEntity(s.owner);
        DamageSource src = ownerE != null ? w.damageSources().indirectMagic(ownerE, ownerE) : w.damageSources().magic();

        for (LivingEntity e : list) {
            boolean isOwner = e.getUUID().equals(s.owner);
            switch (t) {
                case SHRINE -> {
                    if (!isOwner && s.age % 10 == 0) {
                        e.hurt(src, 3f);
                        w.sendParticles(ParticleTypes.SWEEP_ATTACK, e.getX(), e.getY(0.5), e.getZ(), 4, 0.5, 0.6, 0.5, 0);
                    }
                }
                case VOID -> {
                    if (!isOwner) {
                        e.setDeltaMovement(0, Math.min(e.getDeltaMovement().y, 0), 0);
                        e.hurtMarked = true;
                        if (s.age % 5 == 0)
                            e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 20, 9, false, false));
                    }
                }
                case HOMETOWN -> {
                    if (e instanceof Player && s.age % 20 == 0)
                        e.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 60, 1, false, true));
                }
                default -> { }
            }
        }
    }

    private static void teleportOwner(Spell s) {
        s.teleported = true;
        if (!s.type.teleport) return;
        ServerPlayer p = s.level.getServer().getPlayerList().getPlayer(s.owner);
        if (p == null || p.serverLevel() != s.level) return;
        p.connection.teleport(s.center.getX() + s.type.tx + 0.5, s.floorY + s.type.ty,
                s.center.getZ() + s.type.tz + 0.5, s.type.yaw, 0f);
    }

    private static void place(Spell s, int i) {
        BlockPos pos = s.order.get(i);
        s.old.add(s.level.getBlockState(pos));
        s.level.setBlock(pos, s.neu.get(i), FLAGS);
    }

    private static void restore(Spell s, int i) {
        s.level.setBlock(s.order.get(i), s.old.get(i), FLAGS);
    }

    private static void send(ServerLevel w, Vec3 at, int domain, int phase) {
        Net.sendNear(w, at, 160, new FxPacket(FxPacket.DOMAIN, domain * 10 + phase, 0, at.x, at.y, at.z, 0, 0, 0));
    }

    private static void play(ServerLevel w, Vec3 p, SoundEvent s, float vol, float pitch) {
        w.playSound(null, p.x, p.y, p.z, s, SoundSource.PLAYERS, vol, pitch);
    }
}
