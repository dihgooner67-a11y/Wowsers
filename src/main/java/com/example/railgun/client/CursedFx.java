package com.example.railgun.client;

import com.example.railgun.CursedEvents;
import com.example.railgun.FxPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;

/** Cursed Fists: purple flames on BOTH hands, purple punch, black flash, curse kill, and the Cursed Surge aura. */
public final class CursedFx {
    private static final Map<Integer, Long> SURGE = new HashMap<>();
    private static long now;

    private CursedFx() {}

    private static boolean surged(int id) { Long end = SURGE.get(id); return end != null && end > now; }

    public static void onPacket(FxPacket p, Minecraft mc) {
        Vec3 at = new Vec3(p.x, p.y, p.z), dir = new Vec3(p.dx, p.dy, p.dz);
        boolean mine = mc.player.getId() == p.owner;
        switch (p.arg) {
            case 0 -> {
                punch(at);
                if (mine) { Impact.startSoft(2); Impact.addShake(3f); }
            }
            case 1 -> {
                blackFlash(at, false);
                if (mine) { Impact.start(Impact.CBLACK, false); Impact.addShake(16f); }
                else Impact.addShake((float) (6 / (1 + mc.player.position().distanceTo(at) / 10)));
            }
            case 2 -> {
                blackFlash(at, true);
                if (mine) { Impact.start(Impact.CBLACK, false); Impact.addShake(24f); }
                else Impact.addShake((float) (9 / (1 + mc.player.position().distanceTo(at) / 10)));
            }
            default -> {                                                         // surge started: p.dx = duration in ticks
                SURGE.put(p.owner, now + (long) p.dx);
                Vec3 f = mc.level.getEntity(p.owner) != null ? mc.level.getEntity(p.owner).position() : at;
                Vfx.core(Vfx.WHITE, 3f, 6, at);
                Vfx.glow(Vfx.PURPLE, 7f, 12, at, Vec3.ZERO);
                for (int i = 0; i < 4; i++) Vfx.shock(i % 2 == 0 ? Vfx.PURPLE : Vfx.VIOLET, 1.5f + i, 16 + i * 4, f.add(0, 0.2 + i * 0.6, 0), 1.25f);
                for (int i = 0; i < 40; i++) Vfx.flame(i % 2 == 0 ? Vfx.PURPLE : Vfx.VIOLET, Vfx.rf(0.6f, 1.2f), 22, f.add(Vfx.g(0.5), 0, Vfx.g(0.5)), new Vec3(0, Vfx.rf(0.1f, 0.4f), 0));
                if (mine) { Impact.startSoft(2); Impact.addShake(8f); }
            }
        }
    }

    private static void punch(Vec3 a) {
        Vfx.core(Vfx.WHITE, 1.2f, 5, a);
        Vfx.glow(Vfx.VIOLET, 2.6f, 9, a, Vec3.ZERO);
        Vfx.shock(Vfx.PURPLE, 0.8f, 12, a, 1.2f);
        Vfx.ring(Vfx.VIOLET, 0.7f, 14, a, 1.15f);
        for (int i = 0; i < 26; i++) Vfx.flame(i % 3 == 0 ? Vfx.VIOLET : Vfx.PURPLE, Vfx.rf(0.35f, 0.7f), 14 + (int) Vfx.rf(0, 8), a, Vfx.unit().scale(0.25).add(0, 0.05, 0));
        for (int i = 0; i < 24; i++) {
            double ang = i / 24.0 * Math.PI * 2;
            Vfx.flame(Vfx.VIOLET, 0.45f, 12, a, new Vec3(Math.cos(ang) * 0.35, 0.03, Math.sin(ang) * 0.35));
        }
        for (int i = 0; i < 10; i++) Vfx.smoke(Vfx.DEEP, 0.7f, 28, a, Vfx.rnd(0.06).add(0, 0.05, 0), true);
        Vfx.burst(a, Vfx.VIOLET, 22, 0.8, 0.14f);
        Vfx.electric(a, 0.7, Vfx.PURPLE, 3);
    }

    private static void blackFlash(Vec3 a, boolean kill) {
        float k = kill ? 1.6f : 1f;
        Vfx.core(Vfx.WHITE, 3f * k, 4, a);
        Vfx.glow(Vfx.RED, 5.5f * k, 9, a, Vec3.ZERO);
        Vfx.glow(Vfx.PURPLE, 4f * k, 11, a, Vec3.ZERO);
        Vfx.shock(Vfx.RED, 1.2f * k, 14, a, 1.24f);
        Vfx.shock(Vfx.PURPLE, 0.8f * k, 18, a, 1.2f);
        for (int i = 0; i < (kill ? 40 : 26); i++) Vfx.smoke(Vfx.BLACK, Vfx.rf(1.2f, 2.4f) * k, 34, a.add(Vfx.rnd(0.4)), Vfx.unit().scale(0.3), false);
        for (int i = 0; i < 10; i++) Vfx.smoke(Vfx.DEEP, 1.4f * k, 22, a, Vfx.unit().scale(0.25), true);
        for (int i = 0; i < (kill ? 20 : 12); i++) {                       // black lightning with a red/purple glow
            Vec3 d = Vfx.unit(), pos = a;
            for (int s = 0; s < 14; s++) {
                d = d.add(Vfx.rnd(0.5)).normalize();
                pos = pos.add(d.scale(0.8));
                Vfx.glow(i % 2 == 0 ? Vfx.RED : Vfx.PURPLE, 0.45f, 7, pos, Vec3.ZERO);
                Vfx.boltDark(0.9f * k, 8, pos);
                if (s % 2 == 0) Vfx.bolt(Vfx.VIOLET, 0.8f, 5, pos);
            }
        }
        Vfx.burst(a, Vfx.PURPLE, kill ? 120 : 70, 1.4, 0.18f);
        Vfx.electric(a, 1.5, Vfx.RED, 12);
        if (kill) {
            Vfx.disc(3f, 8, a);                                             // a black hole for an instant
            Vfx.swirl(Vfx.PURPLE, 6f, 10, a, 8f);
            for (int i = 0; i < 6; i++) Vfx.lightning(a, Vfx.unit(), 10, Vfx.PURPLE, 1);
        }
    }

    public static void tick(Minecraft mc) {
        now++;
        SURGE.values().removeIf(end -> end < now - 40);
        for (Player p : mc.level.players()) {
            if (p.position().distanceToSqr(mc.player.position()) > 48 * 48) continue;
            boolean surge = surged(p.getId());
            if (!CursedEvents.holding(p) && !surge) continue;

            Vec3 fwd = Vec3.directionFromRotation(0, p.getYRot());
            Vec3 right = new Vec3(-fwd.z, 0, fwd.x);
            if (CursedEvents.holding(p)) {
                for (int side = -1; side <= 1; side += 2) {                     // BOTH hands
                    Vec3 hand = p.position().add(0, 1.0, 0).add(fwd.scale(0.5)).add(right.scale(0.4 * side));
                    for (int i = 0; i < (surge ? 4 : 2); i++)
                        Vfx.flame(i % 2 == 0 ? Vfx.PURPLE : Vfx.VIOLET, surge ? 0.42f : 0.28f, 12, hand.add(Vfx.rnd(0.06)), new Vec3(0, 0.04, 0));
                    if (Vfx.chance(0.35)) Vfx.glow(Vfx.PURPLE, surge ? 0.9f : 0.5f, 4, hand, Vec3.ZERO);
                    if (Vfx.chance(surge ? 0.4 : 0.15)) Vfx.electric(hand, 0.2, Vfx.VIOLET, 1);
                }
                PostFx.want = Math.max(PostFx.want, surge ? 0.8f : 0.4f);
            }
            if (surge) {                                                        // Cursed Surge aura: rising flame pillar
                for (int i = 0; i < 4; i++) {
                    double ang = Vfx.rf(0, 6.2832f);
                    Vec3 o = new Vec3(Math.cos(ang) * 0.6, Vfx.rf(0, 1.8f), Math.sin(ang) * 0.6);
                    Vfx.flame(Vfx.PURPLE, 0.55f, 14, p.position().add(o), new Vec3(0, 0.12, 0));
                }
                if (Vfx.chance(0.4)) Vfx.glow(Vfx.DEEP, 2.2f, 5, p.position().add(0, 0.9, 0), Vec3.ZERO);
                if (Vfx.chance(0.2)) Vfx.lightning(p.position().add(0, 1, 0), Vfx.unit(), 3 + Vfx.rf(0, 3), Vfx.PURPLE, 0);
                PostFx.want = Math.max(PostFx.want, 1.0f);
            }
        }
    }

    public static void hud(GuiGraphics g, int w, int h) {
        Long end = SURGE.get(Minecraft.getInstance().player.getId());
        if (end == null || end <= now) return;
        float left = (end - now) / 20f;
        float f = Math.min(1f, (end - now) / 600f);
        Hud.vignette(g, w, h, 0x7A20D0, 70);
        int bw = 120, bh = 6, x = (w - bw) / 2, y = 18;
        g.fill(x - 1, y - 1, x + bw + 1, y + bh + 1, 0xFF000000);
        g.fill(x, y, x + bw, y + bh, 0xFF2A1040);
        g.fillGradient(x, y, x + (int) (bw * f), y + bh, 0xFFB060FF, 0xFF6010C0);
        g.drawCenteredString(Minecraft.getInstance().font, String.format("CURSED SURGE  %.1fs", left), w / 2, y + 9, 0xFFE0A0FF);
    }
}
