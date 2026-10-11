package com.example.railgun;

import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Server side of Hollow Purple. arg: 0 sphere launched, 1 sphere end, 2 beam fired, 3 beam end. */
public final class PurpleBeams {
    public static final double SPEED = 1.2, RANGE = 110, RADIUS = 4.5;
    public static final float DAMAGE = 30f;
    public static final double BEAM_SPEED = 20, BEAM_LENGTH = 240, BEAM_RADIUS = 9.0;
    public static final float BEAM_DAMAGE = 200f;

    private static final class P {
        final ServerLevel w; final UUID owner; final int ownerId; Vec3 pos; final Vec3 dir; double traveled; int age;
        P(ServerLevel w, UUID o, int id, Vec3 pos, Vec3 dir) { this.w = w; owner = o; ownerId = id; this.pos = pos; this.dir = dir; }
    }
    private static final class Beam {
        final ServerLevel w; final UUID owner; final int ownerId; final Vec3 o, d; double front; int age;
        final Set<UUID> hit = new HashSet<>();
        Beam(ServerLevel w, UUID owner, int id, Vec3 o, Vec3 d) { this.w = w; this.owner = owner; ownerId = id; this.o = o; this.d = d; }
    }
    private static final List<P> SPHERES = new ArrayList<>();
    private static final List<Beam> BEAMS = new ArrayList<>();

    private PurpleBeams() {}

    private static void send(ServerLevel w, int arg, int owner, Vec3 at, Vec3 dir) {
        Net.sendNear(w, at, 260, new FxPacket(FxPacket.PURPLE, arg, owner, at.x, at.y, at.z, dir.x, dir.y, dir.z));
    }

    public static void launch(ServerPlayer p) {
        ServerLevel w = p.serverLevel();
        Vec3 dir = p.getLookAngle().normalize();
        Vec3 pos = p.getEyePosition().add(dir.scale(3.5));
        SPHERES.add(new P(w, p.getUUID(), p.getId(), pos, dir));
        send(w, 0, p.getId(), pos, dir);
        w.playSound(null, pos.x, pos.y, pos.z, SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 5f, 0.5f);
        w.playSound(null, pos.x, pos.y, pos.z, SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 5f, 0.6f);
        PhotonBridge.spawn(w, "purple_launch", pos, dir, 1f);
    }

    public static void fireBeam(ServerPlayer p) {
        ServerLevel w = p.serverLevel();
        Vec3 dir = p.getLookAngle().normalize();
        Vec3 o = p.getEyePosition().add(dir.scale(2.0)).add(0, -0.2, 0);
        BEAMS.add(new Beam(w, p.getUUID(), p.getId(), o, dir));
        send(w, 2, p.getId(), o, dir);
        for (float pitch : new float[]{0.5f, 0.7f})
            w.playSound(null, o.x, o.y, o.z, SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 8f, pitch);
        w.playSound(null, o.x, o.y, o.z, SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 8f, 0.5f);
        PhotonBridge.spawn(w, "purple_beam", o, dir, 2f);

        p.push(-dir.x * 1.4, -dir.y * 0.7, -dir.z * 1.4);
        p.hurtMarked = true;
        p.connection.send(new ClientboundSetEntityMotionPacket(p));
    }

    public static void tick() {
        Iterator<P> it = SPHERES.iterator();
        while (it.hasNext()) {
            P p = it.next();
            p.age++;
            p.pos = p.pos.add(p.dir.scale(SPEED));
            p.traveled += SPEED;
            Carve.sphere(p.w, p.pos, RADIUS, 1000f);

            if (p.age % 4 == 0) {
                AABB box = new AABB(p.pos, p.pos).inflate(RADIUS + 1.5);
                for (LivingEntity e : p.w.getEntitiesOfClass(LivingEntity.class, box,
                        x -> x.isAlive() && !x.getUUID().equals(p.owner) && x.distanceToSqr(p.pos) <= (RADIUS + 1.5) * (RADIUS + 1.5))) {
                    e.hurt(p.w.damageSources().magic(), DAMAGE);
                    e.push(p.dir.x * 1.5, 0.4, p.dir.z * 1.5);
                    e.hurtMarked = true;
                }
            }
            if (p.age % 8 == 0)
                p.w.playSound(null, p.pos.x, p.pos.y, p.pos.z, SoundEvents.WARDEN_HEARTBEAT, SoundSource.PLAYERS, 4f, 0.5f);

            if (p.traveled >= RANGE) {
                send(p.w, 1, p.ownerId, p.pos, p.dir);
                p.w.playSound(null, p.pos.x, p.pos.y, p.pos.z, SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.PLAYERS, 5f, 0.5f);
                PhotonBridge.spawn(p.w, "purple_impact", p.pos, p.dir, 1f);
                it.remove();
            }
        }

        Iterator<Beam> bi = BEAMS.iterator();
        while (bi.hasNext()) {
            Beam b = bi.next();
            b.age++;
            double from = b.front, to = Math.min(from + BEAM_SPEED, BEAM_LENGTH);
            double step = Math.max(1.0, BEAM_RADIUS * 0.6);
            for (double t = from; t <= to; t += step)
                Carve.sphere(b.w, b.o.add(b.d.scale(t)), BEAM_RADIUS, 1000f);

            Vec3 a = b.o.add(b.d.scale(from)), z = b.o.add(b.d.scale(to));
            Entity ownerE = b.w.getEntity(b.owner);
            DamageSource src = ownerE instanceof LivingEntity ? b.w.damageSources().indirectMagic(ownerE, ownerE) : b.w.damageSources().magic();
            for (LivingEntity e : b.w.getEntitiesOfClass(LivingEntity.class, new AABB(a, z).inflate(BEAM_RADIUS + 1.5),
                    x -> x.isAlive() && !x.getUUID().equals(b.owner) && !b.hit.contains(x.getUUID()))) {
                if (distToSegment(e.position().add(0, e.getBbHeight() / 2, 0), a, z) > BEAM_RADIUS + 1.0) continue;
                b.hit.add(e.getUUID());
                e.hurt(src, BEAM_DAMAGE);
                e.push(b.d.x * 4, 1.2, b.d.z * 4);
                e.hurtMarked = true;
            }
            b.front = to;
            if (b.age % 2 == 0)
                b.w.playSound(null, a.x, a.y, a.z, SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 6f, 0.6f);

            if (to >= BEAM_LENGTH) {
                Vec3 end = b.o.add(b.d.scale(BEAM_LENGTH));
                send(b.w, 3, b.ownerId, end, b.d);
                b.w.playSound(null, end.x, end.y, end.z, SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.PLAYERS, 8f, 0.5f);
                PhotonBridge.spawn(b.w, "purple_beam_impact", end, b.d, 2f);
                bi.remove();
            }
        }
    }

    public static void clear() { SPHERES.clear(); BEAMS.clear(); }

    private static double distToSegment(Vec3 p, Vec3 a, Vec3 b) {
        Vec3 ab = b.subtract(a);
        double len2 = ab.lengthSqr();
        double t = len2 < 1e-6 ? 0 : Math.max(0, Math.min(1, p.subtract(a).dot(ab) / len2));
        return p.distanceTo(a.add(ab.scale(t)));
    }
}
