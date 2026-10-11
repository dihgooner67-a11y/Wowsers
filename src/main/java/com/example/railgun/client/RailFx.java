package com.example.railgun.client;

import com.example.railgun.FxPacket;
import com.example.railgun.RailgunItem;
import com.example.railgun.RailgunMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/** Rail gun: charge-up, cyan plasma beam with a helix + electricity, big flash and shockwave on impact. */
public final class RailFx {
    private static final class Beam { Vec3 s, dir; double len; int age; }
    private static final List<Beam> BEAMS = new ArrayList<>();
    private static float charge;
    private static long ticks;

    private RailFx() {}

    public static void onPacket(FxPacket p, Minecraft mc) {
        Vec3 s = new Vec3(p.x, p.y, p.z), e = new Vec3(p.dx, p.dy, p.dz);
        boolean hit = p.arg == 1;
        Vec3 d = e.subtract(s);
        double len = d.length();
        Vec3 dir = d.normalize();
        Vec3 up0 = Math.abs(dir.y) > 0.99 ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0);
        Vec3 u = dir.cross(up0).normalize(), v = dir.cross(u);

        for (double t = 0; t < len; t += 0.5) {
            Vec3 c = s.add(dir.scale(t));
            Vfx.core(Vfx.WHITE, 0.3f, 7, c);
            Vfx.glow(Vfx.CYAN, 0.8f, 9, c, Vec3.ZERO);
            if (((int) (t * 2)) % 2 == 0) {
                double a = t * 2.6;
                for (int k = 0; k < 2; k++) {
                    double aa = a + k * Math.PI;
                    Vec3 q = c.add(u.scale(Math.cos(aa) * 0.55)).add(v.scale(Math.sin(aa) * 0.55));
                    Vfx.spark(Vfx.CYAN, 0.22f, 8, q, dir.scale(0.35));
                }
            }
            if (((int) (t * 2)) % 6 == 0) Vfx.ring(Vfx.CYAN, 0.5f, 10, c, 1.12f);
            if (Vfx.chance(0.16)) Vfx.electric(c, 0.5, Vfx.CYAN, 1);
        }
        Beam b = new Beam(); b.s = s; b.dir = dir; b.len = len; BEAMS.add(b);

        // muzzle flash
        Vfx.core(Vfx.WHITE, 2.6f, 6, s);
        Vfx.glow(Vfx.CYAN, 4f, 9, s, Vec3.ZERO);
        Vfx.shock(Vfx.CYAN, 1.2f, 10, s, 1.25f);
        for (int i = 0; i < 16; i++) Vfx.spark(Vfx.WHITE, 0.2f, 9, s, dir.scale(1.2 + Vfx.rf(0, 1.4f)).add(Vfx.rnd(0.12)));
        for (int i = 0; i < 8; i++) Vfx.smoke(Vfx.GREY, 0.7f, 26, s, dir.scale(0.4).add(Vfx.rnd(0.06)), false);

        // impact
        Vfx.core(Vfx.WHITE, 4.5f, 8, e);
        Vfx.glow(Vfx.CYAN, 6f, 12, e, Vec3.ZERO);
        Vfx.shock(Vfx.CYAN, 1.4f, 14, e, 1.2f);
        Vfx.shock(Vfx.WHITE, 0.8f, 11, e, 1.28f);
        Vfx.ring(Vfx.CYAN, 1f, 16, e, 1.18f);
        Vfx.burst(e, Vfx.CYAN, 70, 1.5, 0.2f);
        for (int i = 0; i < 18; i++) Vfx.smoke(Vfx.GREY, 1.4f, 40, e.add(Vfx.rnd(0.6)), Vfx.rnd(0.12).add(0, 0.08, 0), false);
        for (int i = 0; i < 7; i++) Vfx.lightning(e, Vfx.unit(), 6 + Vfx.rf(0, 5), Vfx.CYAN, 1);

        if (mc.player.getId() == p.owner) {
            Impact.start(Impact.RAIL, hit);
            Impact.addShake(hit ? 12f : 8f);
            mc.player.setXRot(mc.player.getXRot() - 7f);
        } else {
            Impact.addShake((float) (4 / (1 + mc.player.position().distanceTo(s) / 12)));
        }
    }

    public static void tick(Minecraft mc) {
        ticks++;
        charge = 0;
        var p = mc.player;
        if (p.isUsingItem() && p.getUseItem().is(RailgunMod.RAILGUN.get()))
            charge = Math.min(p.getTicksUsingItem() / (float) RailgunItem.CHARGE_TICKS, 1f);

        if (charge > 0) {
            Impact.addShake(charge * charge * 1.5f);
            Vec3 m = p.getEyePosition().add(p.getLookAngle().scale(1.2));
            int n = 2 + (int) (charge * 5);
            double radius = 1.5 * (1 - charge) + 0.25;
            for (int i = 0; i < n; i++) {
                Vec3 o = Vfx.unit().scale(radius);
                Vfx.spark(Vfx.CYAN, 0.12f, 6, m.add(o), o.scale(-0.2));
            }
            Vfx.glow(Vfx.CYAN, 0.35f + charge * 0.6f, 3, m, Vec3.ZERO);
            bloom(0.6f * charge);
        }

        for (Iterator<Beam> it = BEAMS.iterator(); it.hasNext(); ) {
            Beam b = it.next();
            if (++b.age > 8) { it.remove(); continue; }
            bloom(1.1f);
            for (double t = Math.random() * 1.5; t < b.len; t += 1.5)
                Vfx.glow(Vfx.CYAN, 0.45f, 4, b.s.add(b.dir.scale(t)), Vfx.rnd(0.02));
        }
    }

    private static void bloom(float v) { PostFx.want = Math.max(PostFx.want, v); }

    public static void hud(GuiGraphics g, int w, int h) {
        if (charge <= 0.01f) return;
        boolean full = charge >= 1f;
        int a = (int) (charge * 90);
        int col = (a << 24) | (full ? 0xFFFFFF : 0x00CCFF);
        int clear = full ? 0x00FFFFFF : 0x0000CCFF;
        g.fillGradient(0, 0, w, h / 3, col, clear);
        g.fillGradient(0, h * 2 / 3, w, h, clear, col);
        int lineA = (int) (80 + charge * 175);
        Hud.speedLines(g, w, h, (lineA << 24) | (full ? 0xFFFFFF : 0x66E0FF), 28, (ticks / 2) * 31, 0.9f - 0.7f * charge);
    }
}
