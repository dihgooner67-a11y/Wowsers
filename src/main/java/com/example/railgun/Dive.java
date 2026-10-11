package com.example.railgun;

import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * Puddle Dive. You sink into a coloured puddle, then swim as ink: very fast, hidden, regenerating, immune to fall damage,
 * and you climb walls (client side, see InkFx). Each colour has its own passive, aura and surfacing burst.
 * Packet args (kind PUDDLE): 0 puddle opens (you are sinking), 1 ink form begins, 2 surfaced (burst). dx = colour id.
 */
public final class Dive {
    public static final int SINK_TICKS = 12;
    private static final UUID SPEED_ID = UUID.fromString("7f2d9c1e-3b3a-4a0e-9a53-6d1f6a0e0d11");

    private static final class S {
        final PuddleColor color; int phase, age; final Vec3 from; final float yRot, xRot;
        S(PuddleColor c, Vec3 from, float yRot, float xRot) { color = c; this.from = from; this.yRot = yRot; this.xRot = xRot; }
    }
    private static final Map<UUID, S> ACTIVE = new HashMap<>();

    private Dive() {}

    public static boolean isDiving(UUID id) {
        S s = ACTIVE.get(id);
        return s != null && s.phase == 1;
    }

    public static PuddleColor colorOf(UUID id) { S s = ACTIVE.get(id); return s == null ? null : s.color; }

    private static void send(ServerPlayer p, int arg, PuddleColor c, Vec3 at) {
        Net.sendNear(p.serverLevel(), at, 128, new FxPacket(FxPacket.PUDDLE, arg, p.getId(), at.x, at.y, at.z, c.id, 0, 0));
    }

    public static boolean toggle(ServerPlayer p, PuddleColor c) {
        S s = ACTIVE.get(p.getUUID());
        if (s == null) {
            ACTIVE.put(p.getUUID(), new S(c, p.position(), p.getYRot(), p.getXRot()));
            send(p, 0, c, p.position());
            p.serverLevel().playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.BUCKET_EMPTY, SoundSource.PLAYERS, 2f, 0.6f);
            p.serverLevel().playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.SLIME_SQUISH, SoundSource.PLAYERS, 2f, 0.7f);
            PhotonBridge.spawn(p.serverLevel(), "puddle_dive", p.position(), new Vec3(0, 1, 0), 1f);
            return true;
        }
        if (s.phase == 1) { surface(p, s); return true; }
        return false;
    }

    public static void tick(MinecraftServer srv) {
        Iterator<Map.Entry<UUID, S>> it = ACTIVE.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, S> en = it.next();
            S s = en.getValue();
            ServerPlayer p = srv.getPlayerList().getPlayer(en.getKey());
            if (p == null) { it.remove(); continue; }
            s.age++;
            if (s.phase == 0) {                                              // sinking into the puddle
                double t = Math.min(1.0, s.age / (double) SINK_TICKS), e = t * t * (3 - 2 * t);
                p.setInvulnerable(true);
                p.connection.teleport(s.from.x, s.from.y - 1.5 * e, s.from.z, s.yRot, s.xRot);
                if (s.age >= SINK_TICKS) {
                    p.connection.teleport(s.from.x, s.from.y, s.from.z, s.yRot, s.xRot);
                    p.setInvulnerable(false);
                    AttributeInstance sp = p.getAttribute(Attributes.MOVEMENT_SPEED);
                    if (sp != null) {
                        sp.removeModifier(SPEED_ID);
                        sp.addTransientModifier(new AttributeModifier(SPEED_ID, "ink swim", s.color.speed, AttributeModifier.Operation.MULTIPLY_TOTAL));
                    }
                    s.phase = 1; s.age = 0;
                    send(p, 1, s.color, p.position());
                }
            } else {                                                         // ink form
                p.fallDistance = 0;
                if (s.age % 10 == 0) passives(p, s.color);
                if (s.age % 10 == 5) aura(p, s.color);
            }
        }
    }

    private static void eff(ServerPlayer p, MobEffect e, int amp) { p.addEffect(new MobEffectInstance(e, 30, amp, false, false)); }

    private static void passives(ServerPlayer p, PuddleColor c) {
        eff(p, MobEffects.REGENERATION, c.regen);
        p.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 40, 0, false, false));
        switch (c) {
            case RED -> eff(p, MobEffects.DAMAGE_BOOST, 1);
            case BLUE -> { eff(p, MobEffects.DOLPHINS_GRACE, 0); eff(p, MobEffects.WATER_BREATHING, 0); }
            case BLACK -> eff(p, MobEffects.DAMAGE_RESISTANCE, 3);
            case WHITE -> { eff(p, MobEffects.ABSORPTION, 2); eff(p, MobEffects.FIRE_RESISTANCE, 0); }
            default -> { }
        }
    }

    /** Anything that touches your puddle gets hit by the colour's aura. */
    private static void aura(ServerPlayer p, PuddleColor c) {
        ServerLevel w = p.serverLevel();
        DamageSource src = w.damageSources().indirectMagic(p, p);
        AABB box = p.getBoundingBox().inflate(1.8, 0.5, 1.8);
        for (LivingEntity e : w.getEntitiesOfClass(LivingEntity.class, box, x -> x.isAlive() && x != p && !(x instanceof Player))) {
            switch (c) {
                case RED -> { e.hurt(src, 3f); e.setSecondsOnFire(2); }
                case BLUE -> e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 2));
                case GREEN -> e.addEffect(new MobEffectInstance(MobEffects.POISON, 80, 1));
                case BLACK -> { e.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 60, 0)); e.addEffect(new MobEffectInstance(MobEffects.WITHER, 40, 0)); }
                case WHITE -> { if (e.getMobType() == MobType.UNDEAD) e.hurt(src, 8f); else e.addEffect(new MobEffectInstance(MobEffects.GLOWING, 60, 0)); }
            }
        }
    }

    private static void surface(ServerPlayer p, S s) {
        ServerLevel w = p.serverLevel();
        PuddleColor c = s.color;
        AttributeInstance sp = p.getAttribute(Attributes.MOVEMENT_SPEED);
        if (sp != null) sp.removeModifier(SPEED_ID);
        p.removeEffect(MobEffects.INVISIBILITY);
        Vec3 at = p.position();
        send(p, 2, c, at);

        DamageSource src = w.damageSources().indirectMagic(p, p);
        for (LivingEntity e : w.getEntitiesOfClass(LivingEntity.class, new AABB(at, at).inflate(8),
                x -> x.isAlive() && x != p && x.distanceToSqr(at) <= 64)) {
            Vec3 out = e.position().subtract(at);
            Vec3 od = out.lengthSqr() < 1e-4 ? new Vec3(0, 1, 0) : out.normalize();
            boolean player = e instanceof Player;
            double d2 = e.distanceToSqr(at);
            switch (c) {
                case RED -> { if (!player && d2 <= 36) { e.hurt(src, 20f); e.push(od.x * 1.5, 0.6, od.z * 1.5); e.hurtMarked = true; e.setSecondsOnFire(3); } }
                case BLUE -> { if (!player && d2 <= 36) { e.hurt(src, 8f); e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 3)); } }
                case GREEN -> {
                    if (player) e.heal(10f);
                    else if (d2 <= 36) { e.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 1)); e.addEffect(new MobEffectInstance(MobEffects.POISON, 80, 1)); }
                }
                case BLACK -> { if (!player && d2 <= 49) { e.hurt(src, 12f); e.addEffect(new MobEffectInstance(MobEffects.WITHER, 100, 1)); e.push(-od.x * 1.5, 0.2, -od.z * 1.5); e.hurtMarked = true; } }
                case WHITE -> {
                    if (player) e.heal(6f);
                    else if (d2 <= 49) { e.hurt(src, e.getMobType() == MobType.UNDEAD ? 40f : 14f); e.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 60, 0)); }
                }
            }
        }
        p.push(0, 0.8, 0);                                                   // pop out of the puddle
        p.hurtMarked = true;
        p.connection.send(new ClientboundSetEntityMotionPacket(p));
        w.playSound(null, at.x, at.y, at.z, SoundEvents.SLIME_SQUISH, SoundSource.PLAYERS, 3f, 0.9f);
        w.playSound(null, at.x, at.y, at.z, SoundEvents.PLAYER_SPLASH_HIGH_SPEED, SoundSource.PLAYERS, 3f, 0.8f);
        Dev.cooldown(p, RailgunMod.PUDDLE_DIVE.get(), 40);
        ACTIVE.remove(p.getUUID());
    }

    /** Remove the speed boost and forget the dive (death, logout, dimension change, shutdown). */
    public static void drop(ServerPlayer p) {
        S s = ACTIVE.remove(p.getUUID());
        p.setInvulnerable(false);
        AttributeInstance sp = p.getAttribute(Attributes.MOVEMENT_SPEED);
        if (sp != null) sp.removeModifier(SPEED_ID);
        if (s != null) {
            p.removeEffect(MobEffects.INVISIBILITY);
            send(p, 2, s.color, p.position());
        }
    }

    public static void clear() { ACTIVE.clear(); }
}
