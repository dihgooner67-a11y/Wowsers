package com.example.railgun.client;

import com.example.railgun.FxPacket;
import com.example.railgun.PurpleBeams;
import com.example.railgun.PurpleItem;
import com.example.railgun.RailgunMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/** Hollow Purple: red and blue orbs merge, the purple sphere, and the 200% beam. Smoke + electricity textures, bloom. */
public final class PurpleFx {
    private static final class Proj { Vec3 pos, dir; double traveled; int age; }
    private static final class BeamFx { Vec3 o, d, u, v; double front; int life; boolean ended; }
    private static final List<Proj> PROJ = new ArrayList<>();
    private static final List<BeamFx> BEAMS = new ArrayList<>();
    private static float charge;
    private static int used;
    private static long ticks;

    private PurpleFx() {}

    public static void onPacket(FxPacket p, Minecraft mc) {
        Vec3 pos = new Vec3(p.x, p.y, p.z), dir = new Vec3(p.dx, p.dy, p.dz);
        boolean mine = mc.player.getId() == p.owner;
        switch (p.arg) {
            case 0 -> {
                Proj pr = new Proj(); pr.pos = pos; pr.dir = dir; PROJ.add(pr);
                if (mine) { Impact.start(Impact.PSPHERE, false); Impact.addShake(12f); }
            }
            case 1 -> {
                Vfx.core(Vfx.WHITE, 7f, 6, pos);
                Vfx.glow(Vfx.PURPLE, 11f, 14, pos, Vec3.ZERO);
                Vfx.shock(Vfx.PURPLE, 3f, 18, pos, 1.22f);
                Vfx.shock(Vfx.VIOLET, 2f, 14, pos, 1.28f);
                for (int i = 0; i < 90; i++) Vfx.burst(pos, i % 2 == 0 ? Vfx.PURPLE : Vfx.VIOLET, 1, 1.4, 0.3f);
                for (int i = 0; i < 24; i++) Vfx.smoke(Vfx.DEEP, 3f, 46, pos.add(Vfx.rnd(1.5)), Vfx.unit().scale(0.35), true);
                for (int i = 0; i < 10; i++) Vfx.lightning(pos, Vfx.unit(), 9, Vfx.PURPLE, 1);
                Impact.addShake((float) (10 / (1 + mc.player.position().distanceTo(pos) / 12)));
            }
            case 2 -> {
                BeamFx b = new BeamFx();
                b.o = pos; b.d = dir.normalize();
                Vec3 up0 = Math.abs(b.d.y) > 0.99 ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0);
                b.u = b.d.cross(up0).normalize();
                b.v = b.d.cross(b.u);
                BEAMS.add(b);
                if (mine) { Impact.start(Impact.PBEAM, false); Impact.addShake(36f); }
                else Impact.addShake((float) (16 / (1 + mc.player.position().distanceTo(pos) / 25)));
            }
            default -> {
                Vfx.core(Vfx.WHITE, 18f, 8, pos);
                Vfx.glow(Vfx.PURPLE, 26f, 20, pos, Vec3.ZERO);
                for (int i = 0; i < 4; i++) Vfx.shock(i % 2 == 0 ? Vfx.PURPLE : Vfx.VIOLET, 4f + i * 2, 20 + i * 4, pos, 1.18f);
                for (int i = 0; i < 70; i++) Vfx.smoke(i % 3 == 0 ? Vfx.DEEP : Vfx.GREY, Vfx.rf(4, 8), 60, pos.add(Vfx.rnd(4)), Vfx.unit().scale(0.6), i % 3 == 0);
                for (int i = 0; i < 320; i++) Vfx.burst(pos, i % 2 == 0 ? Vfx.PURPLE : Vfx.VIOLET, 1, 2.4, 0.4f);
                for (int i = 0; i < 16; i++) Vfx.lightning(pos, Vfx.unit(), 14 + Vfx.rf(0, 8), Vfx.PURPLE, 2);
            }
        }
    }

    public static void tick(Minecraft mc) {
        ticks++;
        charge = 0; used = 0;
        for (Player pl : mc.level.players()) {
            if (!pl.isUsingItem() || !pl.getUseItem().is(RailgunMod.HOLLOW_PURPLE.get())) continue;
            int u = pl.getTicksUsingItem();
            float ch = Math.min(u / (float) PurpleItem.CHARGE_TICKS, 1f);
            float extra = Math.max(0f, Math.min((u - PurpleItem.CHARGE_TICKS) / (float) (PurpleItem.BEAM_TICKS - PurpleItem.CHARGE_TICKS), 1f));
            if (pl == mc.player) {
                charge = ch; used = u;
                Impact.addShake(extra * extra * 1.8f + (u >= PurpleItem.BEAM_TICKS ? 1.5f : 0f));
            }
            Vec3 look = pl.getLookAngle();
            Vec3 base = pl.getEyePosition().add(look.scale(3.0 + extra));
            Vec3 right = look.cross(new Vec3(0, 1, 0)).normalize();
            Vec3 up = right.cross(look).normalize();
            double sep = 1.8 * (1 - ch), wob = Math.sin(pl.tickCount * 0.4) * 0.3 * (1 - ch);
            Vec3 blue = base.subtract(right.scale(sep)).add(up.scale(wob));
            Vec3 red = base.add(right.scale(sep)).subtract(up.scale(wob));
            Vfx.core(Vfx.BLUE, 0.9f, 3, blue); Vfx.glow(Vfx.BLUE, 1.7f, 4, blue, Vec3.ZERO);
            Vfx.core(Vfx.RED, 0.9f, 3, red);   Vfx.glow(Vfx.RED, 1.7f, 4, red, Vec3.ZERO);
            for (int i = 0; i < 3; i++) {
                Vfx.spark(Vfx.BLUE, 0.14f, 7, blue.add(Vfx.rnd(0.3)), Vfx.rnd(0.08));
                Vfx.spark(Vfx.RED, 0.14f, 7, red.add(Vfx.rnd(0.3)), Vfx.rnd(0.08));
            }
            if (ch >= 1f) {                                         // merged purple sphere keeps growing
                double rad = 1.0 + 2.4 * extra;
                Vfx.core(Vfx.WHITE, (float) (rad * 0.45), 3, base);
                Vfx.glow(Vfx.PURPLE, (float) (rad * 1.9), 4, base, Vec3.ZERO);
                Vfx.glow(Vfx.VIOLET, (float) (rad * 1.1), 3, base, Vec3.ZERO);
                int n = 3 + (int) (extra * 8);
                for (int i = 0; i < n; i++) Vfx.smoke(Vfx.DEEP, (float) (rad * 0.5), 14, base.add(Vfx.unit().scale(rad)), Vfx.unit().scale(0.05), true);
                if (extra > 0.15f) Vfx.electric(base, rad, Vfx.PURPLE, 1 + (int) (extra * 6));
                if (extra > 0.5f && ticks % 3 == 0) Vfx.lightning(base, Vfx.unit(), 3 + extra * 6, Vfx.PURPLE, 1);
            }
            bloom(0.7f + extra);
        }

        for (Iterator<Proj> it = PROJ.iterator(); it.hasNext(); ) {
            Proj p = it.next();
            p.age++;
            p.pos = p.pos.add(p.dir.scale(PurpleBeams.SPEED));
            p.traveled += PurpleBeams.SPEED;
            sphere(p);
            double d = mc.player.position().distanceTo(p.pos);
            if (d < 30) Impact.addShake((float) (4 / (1 + d / 10)));
            bloom(1.4f);
            if (p.traveled >= PurpleBeams.RANGE + 2) it.remove();
        }

        for (Iterator<BeamFx> it = BEAMS.iterator(); it.hasNext(); ) {
            BeamFx b = it.next();
            bloom(1.9f);
            if (!b.ended) {
                double from = b.front, to = Math.min(from + PurpleBeams.BEAM_SPEED, PurpleBeams.BEAM_LENGTH);
                beamSegment(b, from, to);
                b.front = to;
                if (to >= PurpleBeams.BEAM_LENGTH) b.ended = true;
            } else {
                b.life++;
                for (double t = Math.random() * 6; t < PurpleBeams.BEAM_LENGTH; t += 6)      // beam burns out crackling
                    Vfx.electric(b.o.add(b.d.scale(t)), PurpleBeams.BEAM_RADIUS * 0.5, Vfx.PURPLE, 1);
                if (b.life > 40) it.remove();
            }
        }
    }

    private static void bloom(float v) { PostFx.want = Math.max(PostFx.want, v); }

    private static void sphere(Proj p) {
        double R = PurpleBeams.RADIUS;
        Vfx.core(Vfx.WHITE, 2.2f, 2, p.pos);                                  // layered glow = shaded, blooming sphere
        Vfx.core(Vfx.VIOLET, 4.0f, 2, p.pos);
        Vfx.core(Vfx.PURPLE, 6.2f, 2, p.pos);
        Vfx.glow(Vfx.DEEP, 8.5f, 3, p.pos, Vec3.ZERO);
        for (int i = 0; i < 16; i++) Vfx.glow(Vfx.PURPLE, 1.1f, 5, p.pos.add(Vfx.unit().scale(R * Vfx.rf(0.85f, 1.0f))), Vec3.ZERO);
        for (int i = 0; i < 4; i++) Vfx.smoke(Vfx.DEEP, 1.8f, 16, p.pos.add(Vfx.unit().scale(R * Vfx.rf(0.3f, 0.9f))), Vfx.unit().scale(0.08), true);
        if (p.age % 3 == 0) Vfx.ring(Vfx.VIOLET, 3.5f, 14, p.pos, 1.1f);
        Vfx.electric(p.pos, R * 0.8, Vfx.PURPLE, 4);
        if (p.age % 3 == 0) Vfx.lightning(p.pos, Vfx.unit(), 6, Vfx.PURPLE, 1);

        Vec3 up0 = Math.abs(p.dir.y) > 0.99 ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0);
        Vec3 u = p.dir.cross(up0).normalize(), v = p.dir.cross(u);
        double th = p.age * 0.9;
        Vec3 orbit = u.scale(Math.cos(th)).add(v.scale(Math.sin(th))).scale(R * 0.75);
        Vfx.core(Vfx.BLUE, 0.9f, 3, p.pos.add(orbit)); Vfx.glow(Vfx.BLUE, 1.6f, 5, p.pos.add(orbit), p.dir.scale(-0.2));
        Vfx.core(Vfx.RED, 0.9f, 3, p.pos.subtract(orbit)); Vfx.glow(Vfx.RED, 1.6f, 5, p.pos.subtract(orbit), p.dir.scale(-0.2));
        for (int i = 0; i < 5; i++) {                                         // dust and smoke trailing behind
            Vec3 b = p.pos.subtract(p.dir.scale(R + Vfx.rf(0, 7))).add(Vfx.rnd(R * 0.45));
            Vfx.smoke(i % 2 == 0 ? Vfx.GREY : Vfx.DEEP, 2.2f, 30, b, new Vec3(0, 0.03, 0), i % 2 != 0);
        }
    }

    private static void beamSegment(BeamFx b, double from, double to) {
        double R = PurpleBeams.BEAM_RADIUS;
        for (double t = from; t <= to; t += 1.0) {
            Vec3 c = b.o.add(b.d.scale(t));
            Vfx.core(Vfx.WHITE, 3.2f, 3, c);                                  // white-hot core
            Vfx.core(Vfx.VIOLET, 6.0f, 3, c);
            Vfx.core(Vfx.PURPLE, 9.5f, 3, c);                                 // wide violet glow
            for (int k = 0; k < 7; k++) {
                double ang = Math.random() * Math.PI * 2, rad = R * Math.sqrt(Math.random());
                Vec3 radial = b.u.scale(Math.cos(ang)).add(b.v.scale(Math.sin(ang)));
                Vec3 q = c.add(radial.scale(rad));
                if (k < 3) Vfx.glow(Vfx.PURPLE, 1.6f, 5, q, b.d.scale(0.5));
                else if (k < 5) Vfx.smoke(Vfx.DEEP, 2.4f, 16, q, radial.scale(0.12), true);
                else Vfx.spark(Vfx.VIOLET, 0.2f, 8, q, radial.scale(0.6).add(b.d.scale(0.3)));
            }
            if (((int) t) % 3 == 0) Vfx.shock(Vfx.VIOLET, 3f, 12, c, 1.15f);
            if (Vfx.chance(0.5)) Vfx.smoke(Vfx.GREY, 3.2f, 34, c.add(Vfx.rnd(R * 0.6)), Vfx.rnd(0.1), false);   // smoke the beam throws off
        }
        for (int i = 0; i < 9; i++) {                                         // electric arcs leaping off the beam
            Vec3 c = b.o.add(b.d.scale(from + Math.random() * (to - from)));
            double ang = Math.random() * Math.PI * 2;
            Vec3 dir = b.u.scale(Math.cos(ang)).add(b.v.scale(Math.sin(ang)));
            Vfx.lightning(c, dir, 6 + Vfx.rf(0, 6), Vfx.PURPLE, 1);
            Vfx.electric(c.add(dir.scale(R * 0.6)), 1.5, Vfx.PURPLE, 2);
        }
    }

    public static void hud(GuiGraphics g, int w, int h) {
        if (charge <= 0.01f) return;
        int a = (int) (charge * 90);
        g.fillGradient(0, 0, w, h / 2, (a << 24) | 0x2060FF, 0x002060FF);
        g.fillGradient(0, h / 2, w, h, 0x00FF2020, (a << 24) | 0xFF2020);
        if (charge < 1f) return;
        g.fill(0, 0, w, h, ((int) (40 + 30 * Math.sin(ticks * 0.6)) << 24) | 0x8020FF);
        float f = Math.max(0f, Math.min((used - PurpleItem.CHARGE_TICKS) / (float) (PurpleItem.BEAM_TICKS - PurpleItem.CHARGE_TICKS), 1f));
        int bw = 160, bh = 8, x = (w - bw) / 2, y = h - 48;
        g.fill(x - 1, y - 1, x + bw + 1, y + bh + 1, 0xFF000000);
        g.fill(x, y, x + bw, y + bh, 0xFF2A2A2A);
        g.fillGradient(x, y, x + (int) (bw * f), y + bh, 0xFFB040FF, 0xFF6010C0);
        boolean full = used >= PurpleItem.BEAM_TICKS;
        g.drawCenteredString(Minecraft.getInstance().font, full ? "HOLLOW PURPLE 200%  -  RELEASE" : "HOLLOW PURPLE " + (100 + (int) (100 * f)) + "%",
                w / 2, y - 14, full ? 0xFFFF80FF : 0xFFD0A0FF);
    }
}
