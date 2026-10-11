package com.example.railgun.client;

import com.example.railgun.FxPacket;
import com.example.railgun.MechBeams;
import com.example.railgun.MechItem;
import com.example.railgun.RailgunMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/** Ultimate Mech Beam: charge bar, layered fire beam with real smoke and electricity, orange/red filter frames, fire across the screen. */
public final class MechFx {
    private static final class Beam { Vec3 o, d, u, v; int tier; double front; int life; boolean ended; }
    private static final List<Beam> BEAMS = new ArrayList<>();
    private static int chargeTicks;

    private MechFx() {}

    public static void onPacket(FxPacket p, Minecraft mc) {
        Beam b = new Beam();
        b.o = new Vec3(p.x, p.y, p.z);
        b.d = new Vec3(p.dx, p.dy, p.dz).normalize();
        b.tier = p.arg;
        Vec3 up0 = Math.abs(b.d.y) > 0.99 ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0);
        b.u = b.d.cross(up0).normalize();
        b.v = b.d.cross(b.u);
        BEAMS.add(b);
        if (mc.player.getId() == p.owner) {
            Impact.start(b.tier == 3 ? Impact.MECH3 : b.tier == 2 ? Impact.MECH2 : Impact.MECH1, false);
            Impact.addShake(new float[]{0, 6f, 14f, 30f}[b.tier]);
        } else {
            Impact.addShake((float) (b.tier * 4 / (1 + mc.player.position().distanceTo(b.o) / 20)));
        }
        Vfx.core(Vfx.WHITE, 2f + b.tier * 1.5f, 5, b.o);                          // muzzle blast
        Vfx.glow(Vfx.ORANGE, 4f + b.tier * 2f, 9, b.o, Vec3.ZERO);
        Vfx.shock(Vfx.ORANGE, 1.5f, 12, b.o, 1.25f);
    }

    public static void tick(Minecraft mc) {
        chargeTicks = 0;
        var p = mc.player;
        if (p.isUsingItem() && p.getUseItem().is(RailgunMod.MECH_BEAM.get())) {
            chargeTicks = p.getTicksUsingItem();
            float f = Math.min(chargeTicks / (float) MechItem.TIER3, 1f);
            Impact.addShake(f * f * 1.6f + (chargeTicks >= MechItem.TIER3 ? 1.2f : 0f));
            Vec3 m = p.getEyePosition().add(p.getLookAngle().scale(1.8));
            int n = 2 + (int) (f * 10);
            double rad = 1.8 * (1 - f) + 0.3;
            for (int i = 0; i < n; i++) {
                Vec3 o = Vfx.unit().scale(rad);
                if (i % 3 == 0) Vfx.flame(Vfx.ORANGE, 0.3f, 7, m.add(o), o.scale(-0.18));
                else Vfx.spark(Vfx.YELLOW, 0.12f, 6, m.add(o), o.scale(-0.2));
            }
            Vfx.core(Vfx.YELLOW, 0.4f + f * 1.2f, 3, m);
            Vfx.glow(Vfx.ORANGE, 0.9f + f * 2.2f, 4, m, Vec3.ZERO);
            if (chargeTicks >= MechItem.TIER2 && Vfx.chance(0.4)) Vfx.electric(m, 0.9, Vfx.ORANGE, 1);
            PostFx.want = Math.max(PostFx.want, 0.6f + f);
        }

        for (Iterator<Beam> it = BEAMS.iterator(); it.hasNext(); ) {
            Beam b = it.next();
            double len = MechBeams.LENGTH[b.tier];
            PostFx.want = Math.max(PostFx.want, 0.9f + b.tier * 0.35f);
            if (!b.ended) {
                double from = b.front, to = Math.min(from + MechBeams.SPEED, len);
                segment(b, from, to);
                b.front = to;
                if (to >= len) {
                    b.ended = true;
                    Vec3 e = b.o.add(b.d.scale(len));
                    Vfx.core(Vfx.WHITE, 6f + b.tier * 5, 8, e);
                    Vfx.glow(Vfx.ORANGE, 10f + b.tier * 8, 16, e, Vec3.ZERO);
                    for (int i = 0; i < 2 + b.tier * 2; i++) Vfx.shock(i % 2 == 0 ? Vfx.ORANGE : Vfx.YELLOW, 2f + i * 1.5f, 16 + i * 3, e, 1.2f);
                    int blasts = b.tier == 3 ? 40 : b.tier == 2 ? 18 : 8;
                    for (int i = 0; i < blasts; i++) {
                        Vec3 q = e.add(Vfx.rnd(b.tier * 2.2));
                        Vfx.smoke(i % 2 == 0 ? Vfx.BLACK : Vfx.GREY, 2.5f + b.tier, 50, q, Vfx.unit().scale(0.5), false);
                        Vfx.flame(Vfx.ORANGE, 2f, 16, q, Vfx.unit().scale(0.4).add(0, 0.2, 0));
                    }
                    for (int i = 0; i < 6 + b.tier * 3; i++) Vfx.lightning(e, Vfx.unit(), 8 + Vfx.rf(0, 8), Vfx.ORANGE, 1);
                    Vfx.burst(e, Vfx.YELLOW, 40 + b.tier * 60, 2.0, 0.35f);
                }
            } else {
                b.life++;                                                       // beam lingers, smouldering
                double R = MechBeams.RADIUS[b.tier];
                for (double t = Math.random() * 4; t < len; t += 4) {
                    Vec3 c = b.o.add(b.d.scale(t));
                    Vfx.glow(Vfx.EMBER, 1.1f, 6, c.add(Vfx.rnd(R * 0.5)), new Vec3(0, 0.03, 0));
                    if (Vfx.chance(0.4)) Vfx.smoke(Vfx.GREY, 2.4f, 36, c.add(Vfx.rnd(R * 0.5)), new Vec3(0, 0.05, 0), false);
                }
                if (b.life > (b.tier == 3 ? 40 : 20)) it.remove();
            }
        }
    }

    private static void segment(Beam b, double from, double to) {
        double R = MechBeams.RADIUS[b.tier];
        for (double t = from; t <= to; t += 1.0) {
            Vec3 c = b.o.add(b.d.scale(t));
            Vfx.core(Vfx.WHITE, (float) (R * 0.38), 3, c);                        // white-hot core
            Vfx.core(Vfx.YELLOW, (float) (R * 0.65), 3, c);
            Vfx.core(Vfx.ORANGE, (float) (R * 1.0), 3, c);                        // glow shell
            Vfx.glow(Vfx.EMBER, (float) (R * 1.35), 4, c, Vec3.ZERO);
            int per = b.tier == 1 ? 4 : b.tier == 2 ? 8 : 14;
            for (int k = 0; k < per; k++) {
                double ang = Math.random() * Math.PI * 2, rad = R * Math.sqrt(Math.random());
                Vec3 radial = b.u.scale(Math.cos(ang)).add(b.v.scale(Math.sin(ang)));
                Vec3 q = c.add(radial.scale(rad));
                double r = Math.random();
                if (r < 0.45) Vfx.flame(r < 0.2 ? Vfx.YELLOW : Vfx.ORANGE, (float) (0.5 + R * 0.2), 12, q, b.d.scale(0.35).add(radial.scale(0.05)));
                else if (r < 0.65) Vfx.smoke(Vfx.BLACK, (float) (0.9 + R * 0.35), 28, q, radial.scale(0.1).add(0, 0.05, 0), false);   // soot
                else if (r < 0.80) Vfx.smoke(Vfx.EMBER, (float) (0.8 + R * 0.25), 14, q, radial.scale(0.1), true);                   // lit smoke
                else Vfx.spark(Vfx.YELLOW, 0.22f, 9, q, radial.scale(0.7).add(b.d.scale(0.4)));
            }
            if (((int) t) % 3 == 0) Vfx.shock(Vfx.ORANGE, (float) (R * 0.55), 11, c, 1.15f);
            if (b.tier >= 2 && Vfx.chance(0.35)) Vfx.electric(c.add(b.u.scale(Vfx.g(R * 0.6))), 1.2, Vfx.ORANGE, 1);
        }
        if (b.tier == 3)
            for (int i = 0; i < 4; i++) {
                Vec3 c = b.o.add(b.d.scale(from + Math.random() * (to - from)));
                double ang = Math.random() * Math.PI * 2;
                Vfx.lightning(c, b.u.scale(Math.cos(ang)).add(b.v.scale(Math.sin(ang))), 6 + Vfx.rf(0, 6), Vfx.YELLOW, 1);
            }
    }

    public static void hud(GuiGraphics g, int w, int h) {
        if (chargeTicks <= 0) return;
        float f = Math.min(chargeTicks / (float) MechItem.TIER3, 1f);
        int a = (int) (f * 90);
        g.fillGradient(0, 0, w, h / 3, (a << 24) | 0xFF3000, 0x00FF3000);
        g.fillGradient(0, h * 2 / 3, w, h, 0x00FF3000, (a << 24) | 0xFF3000);
        int bw = 160, bh = 8, x = (w - bw) / 2, y = h - 48;
        g.fill(x - 1, y - 1, x + bw + 1, y + bh + 1, 0xFF000000);
        g.fill(x, y, x + bw, y + bh, 0xFF2A2A2A);
        g.fillGradient(x, y, x + (int) (bw * f), y + bh, 0xFFFFA000, 0xFFFF2000);
        g.fill(x + bw / 2, y - 3, x + bw / 2 + 1, y + bh + 3, 0xFFFFFFFF);
        String tier = chargeTicks >= MechItem.TIER3 ? "ULTRA BEAM READY" : chargeTicks >= MechItem.TIER2 ? "TIER 2" : chargeTicks >= MechItem.MIN_CHARGE ? "TIER 1" : "CHARGING";
        int col = chargeTicks >= MechItem.TIER3 ? 0xFFFF3030 : chargeTicks >= MechItem.TIER2 ? 0xFFFFA020 : 0xFFFFE070;
        g.drawCenteredString(Minecraft.getInstance().font, String.format("%.1fs  %s", chargeTicks / 20f, tier), w / 2, y - 14, col);
    }
}
