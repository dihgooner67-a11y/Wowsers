package com.example.railgun;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Dismantle: a long slash leaves your hands at 12 blocks/tick, cutting a thin slit through anything in its way,
 * a trench in the ground under it, and every mob it touches.
 * Packet args (kind SLASH): 0 fired, 1 mob hit (first hit = impact frame), 3 mob hit (no frame).
 */
public final class Slashes {
    public static final double SPEED = 12, RANGE = 84;
    public static final float DAMAGE = 45f;

    private static final class S {
        final ServerLevel w; final UUID owner; final int ownerId; Vec3 pos; final Vec3 dir; double traveled; boolean framed;
        final Set<UUID> hit = new HashSet<>();
        S(ServerLevel w, UUID o, int id, Vec3 pos, Vec3 dir) { this.w = w; owner = o; ownerId = id; this.pos = pos; this.dir = dir; }
    }
    private static final List<S> ACTIVE = new ArrayList<>();

    private Slashes() {}

    public static void fire(ServerPlayer p) {
        ServerLevel w = p.serverLevel();
        Vec3 dir = p.getLookAngle().normalize();
        Vec3 pos = p.getEyePosition().add(dir.scale(1.0)).add(0, -0.35, 0);
        ACTIVE.add(new S(w, p.getUUID(), p.getId(), pos, dir));
        Net.sendNear(w, pos, 300, new FxPacket(FxPacket.SLASH, 0, p.getId(), pos.x, pos.y, pos.z, dir.x, dir.y, dir.z));
        w.playSound(null, pos.x, pos.y, pos.z, SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 5f, 0.6f);
        w.playSound(null, pos.x, pos.y, pos.z, SoundEvents.TRIDENT_RIPTIDE_3, SoundSource.PLAYERS, 4f, 1.4f);
        w.playSound(null, pos.x, pos.y, pos.z, SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.PLAYERS, 2f, 1.8f);
        PhotonBridge.spawn(w, "dismantle", pos, dir, 1f);
    }

    public static void tick() {
        Iterator<S> it = ACTIVE.iterator();
        while (it.hasNext()) {
            S s = it.next();
            Entity ownerE = s.w.getEntity(s.owner);
            if (ownerE == null) { it.remove(); continue; }
            Vec3 next = s.pos.add(s.dir.scale(SPEED));

            for (double t = 0; t < SPEED; t += 1.5)                                  // the slit itself
                Carve.sphere(s.w, s.pos.add(s.dir.scale(t)), 0.9, 12f);
            for (double t = 0; t < SPEED; t += 3) {                                  // the trench in the ground below
                Vec3 c = s.pos.add(s.dir.scale(t));
                BlockHitResult g = s.w.clip(new ClipContext(c, c.add(0, -12, 0), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, ownerE));
                if (g.getType() != HitResult.Type.MISS) {
                    Vec3 ground = g.getLocation();
                    Carve.sphere(s.w, ground.add(0, -1.0, 0), 1.5, 12f);
                    Carve.sphere(s.w, ground.add(0, -3.2, 0), 1.2, 12f);
                }
            }

            DamageSource src = ownerE instanceof LivingEntity ? s.w.damageSources().indirectMagic(ownerE, ownerE) : s.w.damageSources().magic();
            for (LivingEntity e : s.w.getEntitiesOfClass(LivingEntity.class, new AABB(s.pos, next).inflate(2.0),
                    x -> x.isAlive() && !x.getUUID().equals(s.owner) && !s.hit.contains(x.getUUID()))) {
                if (distToSegment(e.position().add(0, e.getBbHeight() / 2, 0), s.pos, next) > 1.9) continue;
                s.hit.add(e.getUUID());
                e.hurt(src, DAMAGE);
                e.addEffect(new MobEffectInstance(MobEffects.WITHER, 60, 1));
                Vec3 at = e.position().add(0, e.getBbHeight() * 0.5, 0);
                Net.sendNear(s.w, at, 160, new FxPacket(FxPacket.SLASH, s.framed ? 3 : 1, s.ownerId, at.x, at.y, at.z, s.dir.x, s.dir.y, s.dir.z));
                s.framed = true;
                s.w.playSound(null, at.x, at.y, at.z, SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 3f, 0.7f);
            }

            s.pos = next;
            s.traveled += SPEED;
            if (s.traveled >= RANGE) it.remove();
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
