package com.example.railgun;

import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

/**
 * Unlimited Black Hole. Grows for 3 s, hauls everything within 140 blocks toward it, erases terrain and
 * swallows whatever crosses the event horizon, collapses after 16.5 s and detonates.
 * Packet args: 0 spawned, 1 collapse started, 2 final blast.
 */
public final class BlackHoles {
    public static final double R = 14.0;            // event horizon radius (28 blocks across)
    public static final double RING = 3.4;          // accretion disk radius = R * RING (~96 blocks across)
    public static final double PULL = 140.0;        // pull range (~9 chunks)
    public static final int GROW = 60, COLLAPSE_AT = 330, LIFE = 360;

    /** Horizon radius after `age` ticks (shared by server + client). */
    public static double radius(int age) {
        double g = Math.min(1.0, age / (double) GROW);
        g = g * g * (3 - 2 * g);
        double r = 2.0 + (R - 2.0) * g;
        if (age >= COLLAPSE_AT) r *= 1.0 - 0.8 * Math.min(1.0, (age - COLLAPSE_AT) / (double) (LIFE - COLLAPSE_AT));
        return r;
    }

    private static final class H {
        final ServerLevel w; final UUID owner; final int ownerId; final Vec3 c; int age;
        H(ServerLevel w, UUID o, int id, Vec3 c) { this.w = w; owner = o; ownerId = id; this.c = c; }
    }
    private static final List<H> ACTIVE = new ArrayList<>();

    private BlackHoles() {}

    public static boolean isActive(UUID owner) {
        for (H h : ACTIVE) if (h.owner.equals(owner)) return true;
        return false;
    }

    private static void send(H h, int arg) {
        Net.sendNear(h.w, h.c, 400, new FxPacket(FxPacket.BLACKHOLE, arg, h.ownerId, h.c.x, h.c.y, h.c.z, R, 0, 0));
    }

    public static void spawn(ServerPlayer p) {
        ServerLevel w = p.serverLevel();
        Vec3 c = p.getEyePosition().add(p.getLookAngle().scale(45));
        H h = new H(w, p.getUUID(), p.getId(), c);
        ACTIVE.add(h);
        send(h, 0);
        w.playSound(null, c.x, c.y, c.z, SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 12f, 0.5f);
        w.playSound(null, c.x, c.y, c.z, SoundEvents.WITHER_SPAWN, SoundSource.PLAYERS, 12f, 0.4f);
        PhotonBridge.spawn(w, "black_hole", c, new Vec3(0, 1, 0), 3f);
    }

    public static void tick() {
        Iterator<H> it = ACTIVE.iterator();
        while (it.hasNext()) {
            H h = it.next();
            h.age++;
            double r = radius(h.age);
            Entity ownerE = h.w.getEntity(h.owner);
            DamageSource src = ownerE instanceof LivingEntity ? h.w.damageSources().indirectMagic(ownerE, ownerE) : h.w.damageSources().magic();

            if (h.age % 2 == 0) Carve.sphere(h.w, h.c, r * 1.05, 1000f);

            AABB box = new AABB(h.c, h.c).inflate(PULL);
            for (Entity e : h.w.getEntities((Entity) null, box,
                    x -> !x.getUUID().equals(h.owner) && !x.isSpectator() && !x.isRemoved())) {
                Vec3 to = h.c.subtract(e.position().add(0, e.getBbHeight() / 2, 0));
                double d = Math.max(to.length(), 0.5);
                Vec3 dir = to.scale(1.0 / d);
                double strength = Math.min(2.2, 0.045 + 3.0 * (r * r) / (d * d));
                e.push(dir.x * strength, dir.y * strength, dir.z * strength);
                Vec3 dm = e.getDeltaMovement();
                if (dm.length() > 3.0) e.setDeltaMovement(dm.scale(3.0 / dm.length()));
                e.hurtMarked = true;
                if (e instanceof ServerPlayer sp) sp.connection.send(new ClientboundSetEntityMotionPacket(sp));

                if (d < r * 0.9) {
                    if (e instanceof LivingEntity le) le.hurt(src, 10000f);
                    else if (!(e instanceof Player)) e.discard();          // items, arrows, falling blocks
                } else if (d < r * RING && h.age % 10 == 0 && e instanceof LivingEntity le) {
                    le.hurt(src, 6f);                                       // the disk burns
                }
            }

            if (h.age % 20 == 0)
                h.w.playSound(null, h.c.x, h.c.y, h.c.z, SoundEvents.WARDEN_HEARTBEAT, SoundSource.PLAYERS, 16f, 0.5f);
            if (h.age % 40 == 5)
                h.w.playSound(null, h.c.x, h.c.y, h.c.z, SoundEvents.PORTAL_AMBIENT, SoundSource.PLAYERS, 16f, 0.5f);

            if (h.age == COLLAPSE_AT) {
                send(h, 1);
                h.w.playSound(null, h.c.x, h.c.y, h.c.z, SoundEvents.WARDEN_SONIC_CHARGE, SoundSource.PLAYERS, 16f, 0.6f);
            }
            if (h.age >= LIFE) {
                Carve.sphere(h.w, h.c, R * 1.8, 1000f);
                AABB blast = new AABB(h.c, h.c).inflate(80);
                for (LivingEntity e : h.w.getEntitiesOfClass(LivingEntity.class, blast,
                        x -> x.isAlive() && !x.getUUID().equals(h.owner))) {
                    Vec3 out = e.position().subtract(h.c);
                    Vec3 od = out.lengthSqr() < 1e-4 ? new Vec3(0, 1, 0) : out.normalize();
                    e.hurt(src, 300f);
                    e.push(od.x * 4, 1.5, od.z * 4);
                    e.hurtMarked = true;
                    if (e instanceof ServerPlayer sp) sp.connection.send(new ClientboundSetEntityMotionPacket(sp));
                }
                send(h, 2);
                h.w.playSound(null, h.c.x, h.c.y, h.c.z, SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 20f, 0.5f);
                h.w.playSound(null, h.c.x, h.c.y, h.c.z, SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 20f, 0.5f);
                h.w.playSound(null, h.c.x, h.c.y, h.c.z, SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.PLAYERS, 20f, 0.4f);
                PhotonBridge.spawn(h.w, "black_hole_blast", h.c, new Vec3(0, 1, 0), 4f);
                it.remove();
            }
        }
    }

    public static void clear() { ACTIVE.clear(); }
}
