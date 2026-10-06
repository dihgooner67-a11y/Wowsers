package com.example.railgun;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * FUGA: Open Flame. A flame arrow flies fast, and where it lands a huge spire of fire erupts.
 * Packet args: 0 arrow fired, 1 full-draw arrow fired, 2 impact, 3 full-draw impact.
 */
public final class Fuga {
    public static final double SPEED = 4.0, RANGE = 200;
    public static final int SPIRE_TICKS = 100;

    public static double spireHeight(boolean full) { return full ? 130 : 90; }
    public static double spireRadius(boolean full) { return full ? 10 : 7; }

    private static final class Arrow {
        final ServerLevel w; final UUID owner; final int ownerId; final boolean full; Vec3 pos; final Vec3 dir; double traveled;
        Arrow(ServerLevel w, UUID o, int id, boolean full, Vec3 pos, Vec3 dir) { this.w = w; owner = o; ownerId = id; this.full = full; this.pos = pos; this.dir = dir; }
    }
    private static final class Spire {
        final ServerLevel w; final UUID owner; final int ownerId; final boolean full; final Vec3 base; int age;
        Spire(ServerLevel w, UUID o, int id, boolean full, Vec3 base) { this.w = w; owner = o; ownerId = id; this.full = full; this.base = base; }
    }
    private static final List<Arrow> ARROWS = new ArrayList<>();
    private static final List<Spire> SPIRES = new ArrayList<>();

    private Fuga() {}

    private static void send(ServerLevel w, Vec3 at, int arg, int owner, Vec3 dir) {
        Net.sendNear(w, at, 320, new FxPacket(FxPacket.FUGA, arg, owner, at.x, at.y, at.z, dir.x, dir.y, dir.z));
    }

    public static void launch(ServerPlayer p, boolean full) {
        ServerLevel w = p.serverLevel();
        Vec3 dir = p.getLookAngle().normalize();
        Vec3 pos = p.getEyePosition().add(dir.scale(1.2)).add(0, -0.2, 0);
        ARROWS.add(new Arrow(w, p.getUUID(), p.getId(), full, pos, dir));
        send(w, pos, full ? 1 : 0, p.getId(), dir);
        w.playSound(null, pos.x, pos.y, pos.z, SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 6f, 0.6f);
        w.playSound(null, pos.x, pos.y, pos.z, SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 6f, 0.5f);
        PhotonBridge.spawn(w, "fuga_launch", pos, dir, full ? 1.5f : 1f);
    }

    public static void tick() {
        Iterator<Arrow> it = ARROWS.iterator();
        while (it.hasNext()) {
            Arrow a = it.next();
            Entity ownerE = a.w.getEntity(a.owner);
            if (ownerE == null) { it.remove(); continue; }
            Vec3 next = a.pos.add(a.dir.scale(SPEED));

            Vec3 hit = null;
            double best = Double.MAX_VALUE;
            BlockHitResult bhr = a.w.clip(new ClipContext(a.pos, next, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, ownerE));
            if (bhr.getType() != HitResult.Type.MISS) { hit = bhr.getLocation(); best = a.pos.distanceToSqr(hit); }
            AABB sweep = new AABB(a.pos, next).inflate(1.0);
            for (LivingEntity e : a.w.getEntitiesOfClass(LivingEntity.class, sweep, x -> x.isAlive() && !x.getUUID().equals(a.owner))) {
                Optional<Vec3> r = e.getBoundingBox().inflate(0.6).clip(a.pos, next);
                if (r.isPresent()) {
                    double d = a.pos.distanceToSqr(r.get());
                    if (d < best) { best = d; hit = r.get(); }
                }
            }
            a.traveled += SPEED;
            if (hit != null || a.traveled >= RANGE) {
                impact(a, hit != null ? hit : next, ownerE);
                it.remove();
            } else {
                a.pos = next;
            }
        }

        Iterator<Spire> si = SPIRES.iterator();
        while (si.hasNext()) {
            Spire s = si.next();
            s.age++;
            double R = spireRadius(s.full), H = spireHeight(s.full);
            if (s.age % 4 == 0) {
                Entity ownerE = s.w.getEntity(s.owner);
                DamageSource src = ownerE instanceof LivingEntity ? s.w.damageSources().indirectMagic(ownerE, ownerE) : s.w.damageSources().magic();
                AABB zone = new AABB(s.base.x - R, s.base.y - 3, s.base.z - R, s.base.x + R, s.base.y + H, s.base.z + R);
                for (LivingEntity e : s.w.getEntitiesOfClass(LivingEntity.class, zone, x -> x.isAlive() && !x.getUUID().equals(s.owner)
                        && x.distanceToSqr(s.base.x, x.getY(), s.base.z) <= R * R)) {
                    e.hurt(src, 10f);
                    e.setSecondsOnFire(10);
                    e.push(0, 0.6, 0);                                   // the spire lifts everything inside it
                    e.hurtMarked = true;
                    if (e instanceof ServerPlayer sp) sp.connection.send(new net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket(sp));
                }
            }
            if (s.age % 10 == 0)
                s.w.playSound(null, s.base.x, s.base.y + 10, s.base.z, SoundEvents.FIRE_AMBIENT, SoundSource.PLAYERS, 12f, 0.5f);
            if (s.age >= SPIRE_TICKS) si.remove();
        }
    }

    private static void impact(Arrow a, Vec3 at, Entity ownerE) {
        ServerLevel w = a.w;
        // the spire stands on the ground under the impact point
        Vec3 base = at;
        BlockHitResult down = w.clip(new ClipContext(at.add(0, 1, 0), at.add(0, -60, 0), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, ownerE));
        if (down.getType() != HitResult.Type.MISS) base = down.getLocation();

        Spire s = new Spire(w, a.owner, a.ownerId, a.full, base);
        SPIRES.add(s);
        send(w, base, a.full ? 3 : 2, a.ownerId, new Vec3(0, 1, 0));

        double blast = a.full ? 22 : 16;
        Carve.sphere(w, base, a.full ? 9 : 6, 60f);
        DamageSource src = ownerE instanceof LivingEntity ? w.damageSources().indirectMagic(ownerE, ownerE) : w.damageSources().magic();
        AABB box = new AABB(base, base).inflate(blast);
        for (LivingEntity e : w.getEntitiesOfClass(LivingEntity.class, box, x -> x.isAlive() && !x.getUUID().equals(a.owner)
                && x.distanceToSqr(base) <= blast * blast)) {
            Vec3 out = e.position().subtract(base);
            Vec3 od = out.lengthSqr() < 1e-4 ? new Vec3(0, 1, 0) : out.normalize();
            e.hurt(src, a.full ? 200f : 120f);
            e.setSecondsOnFire(20);
            e.push(od.x * 1.6, 1.4, od.z * 1.6);
            e.hurtMarked = true;
            if (e instanceof ServerPlayer sp) sp.connection.send(new net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket(sp));
        }
        w.playSound(null, base.x, base.y, base.z, SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 14f, 0.6f);
        w.playSound(null, base.x, base.y, base.z, SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 14f, 0.6f);
        w.playSound(null, base.x, base.y, base.z, SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 10f, 0.7f);
        PhotonBridge.spawn(w, "fuga_impact", base, new Vec3(0, 1, 0), a.full ? 2f : 1.4f);
    }

    public static void clear() { ARROWS.clear(); SPIRES.clear(); }
}
