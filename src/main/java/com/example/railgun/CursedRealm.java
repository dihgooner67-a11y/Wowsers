package com.example.railgun;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The Cursed Realm: a flat black dimension with red rain and red lightning, no mobs at all.
 * Entering: a shadow puddle opens under you and you sink into it, then fall into the realm.
 * Leaving works any time (same puddle, in reverse); after returning there is a 30 s cooldown before you can enter again.
 * While you are inside, a puddle glides across the normal world at your matching coordinates.
 * Packet args (kind REALM): 0 puddle opens, 1 puddle moves, 2 puddle closes, 3 you entered (dx=1) / left (dx=0), 4 emerge puddle.
 */
public final class CursedRealm {
    public static final ResourceKey<Level> KEY = ResourceKey.create(Registries.DIMENSION, new ResourceLocation(RailgunMod.MOD_ID, "cursed_realm"));
    public static final int SINK_TICKS = 22, LOCK_TICKS = 600;           // 600 ticks = 30 s

    private static final class S {
        ResourceKey<Level> originDim; Vec3 originPos = Vec3.ZERO, sinkFrom = Vec3.ZERO, puddle = Vec3.ZERO;
        float yRot, xRot; int phase, age;                                  // phase 0 entering, 1 inside, 2 leaving
    }
    private static final Map<UUID, S> SESSIONS = new HashMap<>();
    private static final Map<UUID, Long> LOCK = new HashMap<>();

    private CursedRealm() {}

    public static boolean inRealm(ServerPlayer p) { return p.level().dimension().equals(KEY); }

    // ---------------------------------------------------------------- item use
    public static boolean use(ServerPlayer p) {
        MinecraftServer srv = p.getServer();
        if (srv == null) return false;
        if (srv.getLevel(KEY) == null) {
            p.displayClientMessage(Component.literal("The Cursed Realm dimension isn't loaded."), true);
            return false;
        }
        S s = SESSIONS.get(p.getUUID());
        if (inRealm(p)) {                                                  // coming back: allowed any time
            if (s == null) {                                               // e.g. logged out inside; send them home
                s = new S();
                s.originDim = Level.OVERWORLD;
                s.originPos = Vec3.atBottomCenterOf(srv.overworld().getSharedSpawnPos());
                SESSIONS.put(p.getUUID(), s);
            }
            if (s.phase == 2) return false;
            s.phase = 2; s.age = 0; s.sinkFrom = p.position();
            s.yRot = p.getYRot(); s.xRot = p.getXRot();
            puddleHere(p, 0);
            return true;
        }
        if (s != null) return false;
        long now = srv.overworld().getGameTime();
        Long until = LOCK.get(p.getUUID());
        if (until != null && until > now) {
            p.displayClientMessage(Component.literal("The Cursed Realm is closed for " + ((until - now + 19) / 20) + " s."), true);
            return false;
        }
        s = new S();
        s.originDim = p.level().dimension();
        s.originPos = p.position();
        s.sinkFrom = p.position();
        s.puddle = p.position();
        s.yRot = p.getYRot(); s.xRot = p.getXRot();
        SESSIONS.put(p.getUUID(), s);
        puddleHere(p, 0);
        PhotonBridge.spawn(p.serverLevel(), "cursed_realm_enter", p.position(), new Vec3(0, 1, 0), 1f);
        return true;
    }

    private static void puddleHere(ServerPlayer p, int arg) {
        ServerLevel w = p.serverLevel();
        Vec3 pos = p.position();
        double gy = pos.y;
        BlockHitResult g = w.clip(new ClipContext(pos.add(0, 0.5, 0), pos.add(0, -16, 0), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p));
        if (g.getType() != HitResult.Type.MISS) gy = g.getLocation().y;
        Net.sendNear(w, pos, 96, new FxPacket(FxPacket.REALM, arg, p.getId(), pos.x, gy, pos.z, 1.7, 0, 0));
        w.playSound(null, pos.x, pos.y, pos.z, SoundEvents.WARDEN_EMERGE, SoundSource.PLAYERS, 3f, 0.6f);
        w.playSound(null, pos.x, pos.y, pos.z, SoundEvents.SCULK_CATALYST_BLOOM, SoundSource.PLAYERS, 3f, 0.5f);
    }

    // ---------------------------------------------------------------- per tick
    public static void tick(MinecraftServer srv) {
        long clock = srv.overworld().getGameTime();
        Iterator<Map.Entry<UUID, S>> it = SESSIONS.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, S> en = it.next();
            S s = en.getValue();
            ServerPlayer p = srv.getPlayerList().getPlayer(en.getKey());
            if (p == null) { it.remove(); continue; }
            s.age++;
            if (s.phase == 0 || s.phase == 2) {
                double t = Math.min(1.0, s.age / (double) SINK_TICKS);
                double e = t * t * (3 - 2 * t);
                p.setInvulnerable(true);
                p.connection.teleport(s.sinkFrom.x, s.sinkFrom.y - 1.6 * e, s.sinkFrom.z, s.yRot, s.xRot);
                if (s.age >= SINK_TICKS) {
                    if (s.phase == 0) arrive(srv, p, s); else depart(srv, p, s, clock);
                    if (s.phase == -1) it.remove();
                }
            } else if (s.age % 2 == 0) {
                mirrorPuddle(srv, p, s);
            }
        }
        if (clock % 20 == 0) {                                             // no mobs at all in the realm
            ServerLevel realm = srv.getLevel(KEY);
            if (realm != null) {
                List<Entity> rm = new ArrayList<>();
                for (Entity e : realm.getAllEntities()) if (e instanceof Mob) rm.add(e);
                for (Entity e : rm) e.discard();
            }
        }
    }

    private static void arrive(MinecraftServer srv, ServerPlayer p, S s) {
        ServerLevel realm = srv.getLevel(KEY);
        p.setInvulnerable(false);
        if (realm == null) { s.phase = -1; return; }
        double y = realm.getMinBuildHeight() + 13 + 35;                     // 35 blocks above the floor: you fall in
        p.teleportTo(realm, s.originPos.x, y, s.originPos.z, s.yRot, s.xRot);
        p.fallDistance = 0;
        p.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 400, 0, false, false));
        s.phase = 1; s.age = 0;
        Net.sendTo(p, new FxPacket(FxPacket.REALM, 3, p.getId(), 0, 0, 0, 1, 0, 0));
        realm.playSound(null, s.originPos.x, y, s.originPos.z, SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 6f, 0.5f);
    }

    private static void depart(MinecraftServer srv, ServerPlayer p, S s, long clock) {
        ServerLevel origin = srv.getLevel(s.originDim);
        if (origin == null) origin = srv.overworld();
        p.setInvulnerable(false);
        p.teleportTo(origin, s.originPos.x, s.originPos.y, s.originPos.z, s.yRot, s.xRot);
        p.fallDistance = 0;
        p.removeEffect(MobEffects.SLOW_FALLING);
        LOCK.put(p.getUUID(), clock + LOCK_TICKS);
        p.getCooldowns().addCooldown(RailgunMod.CURSED_REALM.get(), LOCK_TICKS);
        Net.sendNear(origin, s.originPos, 160, new FxPacket(FxPacket.REALM, 2, p.getId(), s.puddle.x, s.puddle.y, s.puddle.z, 0, 0, 0));
        Net.sendNear(origin, s.originPos, 96, new FxPacket(FxPacket.REALM, 4, p.getId(), s.originPos.x, s.originPos.y, s.originPos.z, 1.7, 0, 0));
        Net.sendTo(p, new FxPacket(FxPacket.REALM, 3, p.getId(), 0, 0, 0, 0, LOCK_TICKS / 20, 0));
        origin.playSound(null, s.originPos.x, s.originPos.y, s.originPos.z, SoundEvents.WARDEN_EMERGE, SoundSource.PLAYERS, 3f, 0.7f);
        PhotonBridge.spawn(origin, "cursed_realm_leave", s.originPos, new Vec3(0, 1, 0), 1f);
        s.phase = -1;
    }

    /** The puddle left behind in the normal world follows your coordinates while you roam the realm. */
    private static void mirrorPuddle(MinecraftServer srv, ServerPlayer p, S s) {
        ServerLevel origin = srv.getLevel(s.originDim);
        if (origin == null) return;
        int x = (int) Math.floor(p.getX()), z = (int) Math.floor(p.getZ());
        if (origin.hasChunkAt(new BlockPos(x, 64, z))) {
            int gy = origin.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            s.puddle = new Vec3(p.getX(), gy, p.getZ());
        }
        Net.sendNear(origin, s.puddle, 128, new FxPacket(FxPacket.REALM, 1, p.getId(), s.puddle.x, s.puddle.y, s.puddle.z, 1.7, 0, 0));
    }

    /** Player died / logged out / respawned: forget the session and close their puddle. */
    public static void drop(ServerPlayer p) {
        S s = SESSIONS.remove(p.getUUID());
        p.setInvulnerable(false);
        if (s == null || p.getServer() == null) return;
        ServerLevel origin = p.getServer().getLevel(s.originDim);
        if (origin != null)
            Net.sendNear(origin, s.puddle, 160, new FxPacket(FxPacket.REALM, 2, p.getId(), s.puddle.x, s.puddle.y, s.puddle.z, 0, 0, 0));
    }

    public static void clear() { SESSIONS.clear(); LOCK.clear(); }
}
