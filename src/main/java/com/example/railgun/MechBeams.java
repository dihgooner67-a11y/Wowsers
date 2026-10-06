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

/** Server side of the Mech Beam: the beam front sweeps 16 blocks/tick, carving and burning. */
public final class MechBeams {
    public static final double SPEED = 16;
    public static final double[] RADIUS = {0, 3.0, 5.5, 9.0};
    public static final double[] LENGTH = {0, 64, 110, 180};
    private static final float[] DAMAGE = {0, 30f, 80f, 250f};
    private static final float[] HARDNESS = {0, 6f, 30f, 1000f};
    private static final int[] FIRE = {0, 8, 15, 30};

    private static final class Beam {
        final ServerLevel w; final UUID owner; final Vec3 o, d; final int tier; double front;
        final Set<UUID> hit = new HashSet<>();
        Beam(ServerLevel w, UUID owner, Vec3 o, Vec3 d, int tier) { this.w = w; this.owner = owner; this.o = o; this.d = d; this.tier = tier; }
    }
    private static final List<Beam> ACTIVE = new ArrayList<>();

    private MechBeams() {}

    public static void fire(ServerPlayer p, int tier) {
        ServerLevel w = p.serverLevel();
        Vec3 dir = p.getLookAngle().normalize();
        Vec3 o = p.getEyePosition().add(dir.scale(1.5)).add(0, -0.2, 0);
        ACTIVE.add(new Beam(w, p.getUUID(), o, dir, tier));
        Net.sendNear(w, o, 220, new FxPacket(FxPacket.MECH, tier, p.getId(), o.x, o.y, o.z, dir.x, dir.y, dir.z));
        PhotonBridge.spawn(w, "mech_beam_" + tier, o, dir, 1f);

        w.playSound(null, o.x, o.y, o.z, SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 3f + tier * 2, 0.8f - tier * 0.15f);
        w.playSound(null, o.x, o.y, o.z, SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 3f + tier * 2, 0.6f);
        w.playSound(null, o.x, o.y, o.z, SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 4f + tier * 2, 0.7f);

        double kick = 0.3 * tier;
        p.push(-dir.x * kick, -dir.y * kick * 0.5, -dir.z * kick);
        p.hurtMarked = true;
        p.connection.send(new ClientboundSetEntityMotionPacket(p));
    }

    public static void tick() {
        Iterator<Beam> it = ACTIVE.iterator();
        while (it.hasNext()) {
            Beam b = it.next();
            double from = b.front, to = Math.min(from + SPEED, LENGTH[b.tier]);
            double r = RADIUS[b.tier];
            double step = Math.max(1.0, r * 0.6);
            for (double t = from; t <= to; t += step)
                Carve.sphere(b.w, b.o.add(b.d.scale(t)), r, HARDNESS[b.tier]);

            Vec3 a = b.o.add(b.d.scale(from)), z = b.o.add(b.d.scale(to));
            Entity owner = b.w.getEntity(b.owner);
            DamageSource src = owner instanceof LivingEntity ? b.w.damageSources().indirectMagic(owner, owner) : b.w.damageSources().magic();
            for (LivingEntity e : b.w.getEntitiesOfClass(LivingEntity.class, new AABB(a, z).inflate(r + 1.5),
                    x -> x.isAlive() && !x.getUUID().equals(b.owner) && !b.hit.contains(x.getUUID()))) {
                if (distToSegment(e.position().add(0, e.getBbHeight() / 2, 0), a, z) > r + 1.0) continue;
                b.hit.add(e.getUUID());
                e.hurt(src, DAMAGE[b.tier]);
                e.setSecondsOnFire(FIRE[b.tier]);
                e.push(b.d.x * (1 + b.tier), 0.5 + 0.3 * b.tier, b.d.z * (1 + b.tier));
                e.hurtMarked = true;
            }

            b.front = to;
            if (to >= LENGTH[b.tier]) {
                PhotonBridge.spawn(b.w, "mech_impact_" + b.tier, b.o.add(b.d.scale(LENGTH[b.tier])), b.d, (float) b.tier);
                it.remove();
            }
        }
    }

    public static void clear() { ACTIVE.clear(); }

    private static double distToSegment(Vec3 p, Vec3 a, Vec3 b) {
        Vec3 ab = b.subtract(a);
        double len2 = ab.lengthSqr();
        double t = len2 < 1e-6 ? 0 : Math.max(0, Math.min(1, p.subtract(a).dot(ab) / len2));
        return p.distanceTo(a.add(ab.scale(t)));
    }
}
