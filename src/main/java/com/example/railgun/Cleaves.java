package com.example.railgun;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.UUID;

/**
 * Cleave: needs a mob touching you. For 3.5 s a storm of tiny dismantles shreds everything around it.
 * Packet args (kind SLASH): 2 started (dx = target entity id, dy = duration), 4 finished.
 */
public final class Cleaves {
    public static final int DURATION = 70;
    public static final float HIT = 28f, FINALE = 140f;
    public static final double RADIUS = 6.0;

    private static final class C {
        final ServerLevel w; final UUID owner; final int ownerId; final UUID target; Vec3 last; int age;
        C(ServerLevel w, UUID o, int id, UUID t, Vec3 last) { this.w = w; owner = o; ownerId = id; target = t; this.last = last; }
    }
    private static final List<C> ACTIVE = new ArrayList<>();
    private static final Random RNG = new Random();

    private Cleaves() {}

    public static boolean isActive(UUID owner) {
        for (C c : ACTIVE) if (c.owner.equals(owner)) return true;
        return false;
    }

    /** Returns false (and does nothing) if no mob is touching the player. */
    public static boolean start(ServerPlayer p) {
        ServerLevel w = p.serverLevel();
        LivingEntity best = null;
        double bd = Double.MAX_VALUE;
        for (LivingEntity e : w.getEntitiesOfClass(LivingEntity.class, p.getBoundingBox().inflate(0.35),
                x -> x != p && x.isAlive() && !(x instanceof Player))) {
            double d = e.distanceToSqr(p);
            if (d < bd) { bd = d; best = e; }
        }
        if (best == null) return false;
        Vec3 at = best.position().add(0, best.getBbHeight() * 0.5, 0);
        ACTIVE.add(new C(w, p.getUUID(), p.getId(), best.getUUID(), at));
        Net.sendNear(w, at, 160, new FxPacket(FxPacket.SLASH, 2, p.getId(), at.x, at.y, at.z, best.getId(), DURATION, 0));
        w.playSound(null, at.x, at.y, at.z, SoundEvents.WARDEN_SONIC_CHARGE, SoundSource.PLAYERS, 4f, 1.6f);
        PhotonBridge.spawn(w, "cleave", at, new Vec3(0, 1, 0), 1f);
        return true;
    }

    public static void tick() {
        Iterator<C> it = ACTIVE.iterator();
        while (it.hasNext()) {
            C c = it.next();
            c.age++;
            Entity ownerE = c.w.getEntity(c.owner);
            Entity targetE = c.w.getEntity(c.target);
            if (targetE != null && targetE.isAlive()) c.last = targetE.position().add(0, targetE.getBbHeight() * 0.5, 0);
            if (ownerE == null) { it.remove(); continue; }
            DamageSource src = ownerE instanceof LivingEntity ? c.w.damageSources().indirectMagic(ownerE, ownerE) : c.w.damageSources().magic();

            if (targetE instanceof LivingEntity te && te.isAlive() && c.age % 5 == 0) {          // pinned in place
                te.setDeltaMovement(0, Math.min(te.getDeltaMovement().y, 0), 0);
                te.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 12, 9, false, false));
            }

            for (int i = 0; i < 3; i++) {                                                          // little nicks in the ground
                Vec3 q = c.last.add((RNG.nextDouble() - .5) * 2 * RADIUS * 0.8, 1, (RNG.nextDouble() - .5) * 2 * RADIUS * 0.8);
                BlockHitResult g = c.w.clip(new ClipContext(q, q.add(0, -8, 0), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, ownerE));
                if (g.getType() != HitResult.Type.MISS) Carve.sphere(c.w, g.getLocation().add(0, -0.3, 0), 0.8, 8f);
            }
            if (c.age % 2 == 0)
                c.w.playSound(null, c.last.x + (RNG.nextDouble() - .5) * 4, c.last.y, c.last.z + (RNG.nextDouble() - .5) * 4,
                        SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 2f, 0.8f + RNG.nextFloat() * 0.8f);

            if (c.age % 10 == 0 && c.age < DURATION) hurtAround(c, src, HIT);
            if (c.age >= DURATION) {
                hurtAround(c, src, FINALE);
                Net.sendNear(c.w, c.last, 160, new FxPacket(FxPacket.SLASH, 4, c.ownerId, c.last.x, c.last.y, c.last.z, 0, 0, 0));
                c.w.playSound(null, c.last.x, c.last.y, c.last.z, SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 5f, 1.2f);
                c.w.playSound(null, c.last.x, c.last.y, c.last.z, SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.PLAYERS, 5f, 0.6f);
                it.remove();
            }
        }
    }

    private static void hurtAround(C c, DamageSource src, float dmg) {
        AABB box = new AABB(c.last, c.last).inflate(RADIUS);
        for (LivingEntity e : c.w.getEntitiesOfClass(LivingEntity.class, box,
                x -> x.isAlive() && !x.getUUID().equals(c.owner) && !(x instanceof Player) && x.distanceToSqr(c.last) <= RADIUS * RADIUS))
            e.hurt(src, dmg);
    }

    public static void clear() { ACTIVE.clear(); }
}
