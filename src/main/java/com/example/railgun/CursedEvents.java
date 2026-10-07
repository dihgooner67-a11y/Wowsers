package com.example.railgun;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * Cursed Fists combat. Works from either hand (item in main hand, or in the off hand with a free main hand).
 * 20% Black Flash, a mob hit twice dies instantly, Cursed Surge = 95% Black Flash + double damage for 30 s.
 * Packet args: 0 punch, 1 black flash, 2 curse kill, 3 surge started (dx = duration ticks).
 */
@Mod.EventBusSubscriber(modid = RailgunMod.MOD_ID)
public final class CursedEvents {
    public static final float DAMAGE = 12f, BLACK_DAMAGE = 45f, BLACK_CHANCE = 0.20f, SURGE_CHANCE = 0.95f, SURGE_MULT = 2f;
    private static final long MARK_WINDOW = 600;

    private static final Map<UUID, long[]> MARKS = new HashMap<>();      // target -> {hits, lastTick}
    private static final Map<UUID, Long> SURGE = new HashMap<>();        // player -> game time the surge ends

    private CursedEvents() {}

    public static boolean holding(Player p) {
        net.minecraft.world.item.Item fists = RailgunMod.CURSED_FISTS.get();
        return p.getMainHandItem().is(fists) || (p.getOffhandItem().is(fists) && p.getMainHandItem().isEmpty());
    }

    public static boolean surged(ServerPlayer p) {
        Long end = SURGE.get(p.getUUID());
        return end != null && end > p.serverLevel().getGameTime();
    }

    public static void startSurge(ServerPlayer p) {
        ServerLevel sw = p.serverLevel();
        SURGE.put(p.getUUID(), sw.getGameTime() + CursedFistsItem.SURGE_TICKS);
        Vec3 at = p.position().add(0, 1, 0);
        Net.sendNear(sw, at, 96, new FxPacket(FxPacket.CURSED, 3, p.getId(), at.x, at.y, at.z, CursedFistsItem.SURGE_TICKS, 0, 0));
        p.displayClientMessage(Component.literal("CURSED SURGE - 95% Black Flash, double damage"), true);
        PhotonBridge.spawn(sw, "cursed_surge", at, new Vec3(0, 1, 0), 2f);
    }

    @SubscribeEvent
    public static void attack(AttackEntityEvent e) {
        Player p = e.getEntity();
        if (!holding(p) || !(e.getTarget() instanceof LivingEntity target)) return;
        e.setCanceled(true);                                  // our punch replaces the vanilla one
        if (p.level().isClientSide || !(p instanceof ServerPlayer sp)) return;
        if (p.getAttackStrengthScale(0.5f) < 0.5f) return;    // fist cooldown
        punch(sp, target);
        p.resetAttackStrengthTicker();
    }

    private static void punch(ServerPlayer p, LivingEntity t) {
        ServerLevel sw = p.serverLevel();
        long now = sw.getGameTime();
        boolean surge = surged(p);
        boolean black = p.getRandom().nextFloat() < (surge ? SURGE_CHANCE : BLACK_CHANCE);
        float mult = surge ? SURGE_MULT : 1f;

        Vec3 look = p.getLookAngle();
        Vec3 flat = new Vec3(look.x, 0, look.z).normalize();
        DamageSource src = sw.damageSources().playerAttack(p);

        // two hits and a mob is gone
        long[] m = MARKS.computeIfAbsent(t.getUUID(), k -> new long[]{0, now});
        if (now - m[1] > MARK_WINDOW) m[0] = 0;
        m[0]++; m[1] = now;
        boolean kill = !(t instanceof Player) && m[0] >= 2;

        t.hurt(src, (black ? BLACK_DAMAGE : DAMAGE) * mult);
        double kb = black ? 3.8 : 1.2;
        t.push(flat.x * kb, black ? 1.0 : 0.3, flat.z * kb);
        t.hurtMarked = true;
        t.setSecondsOnFire(black ? 6 : 3);

        Vec3 at = t.position().add(0, t.getBbHeight() * 0.6, 0);
        if (kill) {
            MARKS.remove(t.getUUID());
            t.hurt(sw.damageSources().genericKill(), Float.MAX_VALUE);
            if (t.isAlive()) t.kill();
            send(sw, p, 2, at, flat);
            play(sw, at, SoundEvents.WARDEN_SONIC_BOOM, 3f, 0.8f);
            play(sw, at, SoundEvents.LIGHTNING_BOLT_THUNDER, 3f, 1.2f);
            PhotonBridge.spawn(sw, "cursed_kill", at, flat, 1.5f);
        } else {
            send(sw, p, black ? 1 : 0, at, flat);
            if (black) {
                play(sw, at, SoundEvents.LIGHTNING_BOLT_THUNDER, 3f, 1.4f);
                play(sw, at, SoundEvents.WARDEN_SONIC_BOOM, 2f, 1.1f);
                PhotonBridge.spawn(sw, "cursed_black_flash", at, flat, 1f);
            } else {
                play(sw, at, SoundEvents.FIRECHARGE_USE, 1.5f, 0.6f);
                play(sw, at, SoundEvents.BLAZE_SHOOT, 1f, 1.1f);
            }
            if (!(t instanceof Player) && m[0] == 1 && t.isAlive())
                p.displayClientMessage(Component.literal("Cursed mark 1/2"), true);
            if (!t.isAlive()) MARKS.remove(t.getUUID());
        }

        if (MARKS.size() > 256) {                              // drop stale marks
            for (Iterator<Map.Entry<UUID, long[]>> it = MARKS.entrySet().iterator(); it.hasNext(); )
                if (now - it.next().getValue()[1] > MARK_WINDOW) it.remove();
        }
    }

    private static void send(ServerLevel sw, ServerPlayer p, int arg, Vec3 at, Vec3 dir) {
        Net.sendNear(sw, at, 96, new FxPacket(FxPacket.CURSED, arg, p.getId(), at.x, at.y, at.z, dir.x, dir.y, dir.z));
    }

    private static void play(ServerLevel w, Vec3 p, SoundEvent s, float vol, float pitch) {
        w.playSound(null, p.x, p.y, p.z, s, SoundSource.PLAYERS, vol, pitch);
    }
}
