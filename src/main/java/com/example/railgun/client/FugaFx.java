package com.example.railgun.client;

import com.example.railgun.Fuga;
import com.example.railgun.FugaItem;
import com.example.railgun.FxPacket;
import com.example.railgun.RailgunMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/** FUGA: a flame arrow drawn in your hand, a blazing flight trail, and a huge spire of fire built from our fire particles. */
public final class FugaFx {
    private static final class Arrow { Vec3 pos, dir; int owner; boolean full; double traveled; }
    private static final class Spire { Vec3 base; int age; boolean full; }
    private static final List<Arrow> ARROWS = new ArrayList<>();
    private static final List<Spire> SPIRES = new ArrayList<>();
    private static float charge;
    private static int used;

    private FugaFx() {}

    // ---------------------------------------------------------------- packets
    public static void onPacket(FxPacket p, Minecraft mc) {
        Vec3 at = new Vec3(p.x, p.y, p.z), dir = new Vec3(p.dx, p.dy, p.dz);
        boolean full = p.arg == 1 || p.arg == 3;
        if (p.arg <= 1) {
            Arrow a = new Arrow(); a.pos = at; a.dir = dir.normalize(); a.owner = p.owner; a.full = full; ARROWS.add(a);
            Vfx.core(Vfx.WHITE, 2.2f, 5, at);
            Vfx.glow(Vfx.ORANGE, 4.5f, 9, at, Vec3.ZERO);
            Vfx.shock(Vfx.ORANGE, 1.2f, 10, at, 1.25f);
            for (int i = 0; i < 14; i++) Vfx.ember(Vfx.YELLOW, 0.2f, 14, at, a.dir.scale(1.2 + Vfx.rf(0, 1.4f)).add(Vfx.rnd(0.12)));
            if (mc.player.getId() == p.owner) { Impact.addShake(6f); mc.player.setXRot(mc.player.getXRot() - 4f); }
            return;
        }
        ARROWS.removeIf(a -> a.owner == p.owner);
        Spire s = new Spire(); s.base = at; s.full = full; SPIRES.add(s);
        double d = mc.player.position().distanceTo(at);
        float k = full ? 1.5f : 1f;
        Vfx.core(Vfx.WHITE, 14f * k, 6, at);
        Vfx.glow(Vfx.ORANGE, 28f * k, 14, at, Vec3.ZERO);
        for (int i = 0; i < 5; i++) Vfx.shock(i % 2 == 0 ? Vfx.ORANGE : Vfx.YELLOW, (3f + i * 2.2f) * k, 18 + i * 4, at.add(0, 0.5, 0), 1.2f);
        for (int i = 0; i < 70; i++) Vfx.fireball(i % 3 == 0 ? Vfx.YELLOW : Vfx.ORANGE, Vfx.rf(2.5f, 5.5f) * k, 14, at.add(Vfx.g(2 * k), 0.3, Vfx.g(2 * k)), new Vec3(Vfx.g(0.5), Vfx.rf(0.2f, 0.9f), Vfx.g(0.5)));
        for (int i = 0; i < 220; i++) Vfx.ember(i % 2 == 0 ? Vfx.YELLOW : Vfx.EMBER, Vfx.rf(0.15f, 0.35f), 26, at, new Vec3(Vfx.g(0.7), Vfx.rf(0.4f, 1.8f), Vfx.g(0.7)));
        for (int i = 0; i < 40; i++) Vfx.smoke(i % 2 == 0 ? Vfx.BLACK : Vfx.GREY, Vfx.rf(3, 6) * k, 60, at.add(Vfx.rnd(2)), new Vec3(Vfx.g(0.3), Vfx.rf(0.2f, 0.8f), Vfx.g(0.3)), false);
        for (int i = 0; i < 10; i++) Vfx.lightning(at, Vfx.unit().add(0, 1.2, 0), 14 + Vfx.rf(0, 10), Vfx.ORANGE, 1);
        if (d < 100) { Impact.start(Impact.FUGA, false); Impact.addShake((float) Math.min(36, 900 / Math.max(d, 25)) * k); }
    }

    // ---------------------------------------------------------------- tick
    public static void tick(Minecraft mc) {
        charge = 0; used = 0;
        for (Player pl : mc.level.players()) {
            if (!pl.isUsingItem() || !pl.getUseItem().is(RailgunMod.FUGA.get())) continue;
            int u = pl.getTicksUsingItem();
            float ch = Math.min(1f, u / (float) FugaItem.FULL_DRAW);
            if (pl == mc.player) { charge = Math.min(1f, u / (float) FugaItem.MIN_DRAW); used = u; Impact.addShake(ch * ch * 1.4f); }
            handArrow(pl, ch, u >= FugaItem.FULL_DRAW);
            PostFx.want = Math.max(PostFx.want, 0.7f + ch * 0.6f);
        }

        for (Iterator<Arrow> it = ARROWS.iterator(); it.hasNext(); ) {
            Arrow a = it.next();
            flight(a);
            a.pos = a.pos.add(a.dir.scale(Fuga.SPEED));
            a.traveled += Fuga.SPEED;
            PostFx.want = Math.max(PostFx.want, 1.2f);
            if (a.traveled > Fuga.RANGE + 12) it.remove();
        }

        for (Iterator<Spire> it = SPIRES.iterator(); it.hasNext(); ) {
            Spire s = it.next();
            s.age++;
            spire(s);
            double d = mc.player.position().distanceTo(s.base);
            Impact.addShake((float) Math.max(0, 3.0 - d / 40.0));
            PostFx.want = Math.max(PostFx.want, 1.9f);
            if (s.age > Fuga.SPIRE_TICKS + 6) it.remove();
        }
    }

    /** The arrow of flame you hold while drawing: glowing shaft, burning head, flaring fletching. */
    private static void handArrow(Player p, float ch, boolean full) {
        Vec3 look = p.getLookAngle();
        Vec3 right = look.cross(new Vec3(0, 1, 0)).normalize();
        Vec3 up = right.cross(look).normalize();
        double side = (p.getUsedItemHand() == net.minecraft.world.InteractionHand.MAIN_HAND ? p.getMainArm() : p.getMainArm().getOpposite()) == HumanoidArm.RIGHT ? 1 : -1;
        Vec3 base = p.getEyePosition().add(look.scale(0.9)).add(right.scale(side * 0.38)).add(0, -0.32, 0);
        double len = 1.4 + 0.7 * ch;
        float s = 1f + ch * 0.8f + (full ? 0.5f : 0f);
        for (double t = 0; t <= len; t += 0.1) {
            Vec3 c = base.add(look.scale(t));
            Vfx.core(Vfx.YELLOW, 0.10f * s, 2, c);
            if (((int) (t * 10)) % 3 == 0) Vfx.fire(Vfx.ORANGE, 0.26f * s, 5, c.add(Vfx.rnd(0.02)), new Vec3(0, 0.03, 0));
        }
        Vec3 tip = base.add(look.scale(len));
        Vfx.core(Vfx.WHITE, 0.40f * s, 2, tip);
        Vfx.glow(Vfx.ORANGE, 0.9f * s, 3, tip, Vec3.ZERO);
        for (int k = 0; k < 3; k++) {                                          // arrowhead
            double a = k * 2.094 + Math.random();
            Vec3 o = up.scale(Math.cos(a) * 0.17).add(right.scale(Math.sin(a) * 0.17));
            Vfx.fire(Vfx.EMBER, 0.22f * s, 4, tip.subtract(look.scale(0.22)).add(o), look.scale(0.03));
        }
        for (int k = 0; k < 4; k++) {                                          // fletching
            double a = k * 1.571 + Math.random() * 0.3;
            Vec3 o = up.scale(Math.cos(a) * 0.16).add(right.scale(Math.sin(a) * 0.16));
            Vfx.flare(Vfx.ORANGE, 0.22f * s, 2, base.add(look.scale(0.14)).add(o));
        }
        for (int i = 0; i < 2 + (int) (ch * 4); i++) {                         // embers drawn into the arrow
            Vec3 o = Vfx.unit().scale(0.9 - 0.5 * ch);
            Vfx.ember(Vfx.YELLOW, 0.1f, 6, base.add(look.scale(len * 0.5)).add(o), o.scale(-0.12));
        }
    }

    /** Flight trail: a white-hot core with a long tail of fire, embers, soot and rings. */
    private static void flight(Arrow a) {
        float k = a.full ? 1.4f : 1f;
        for (int i = 0; i < 5; i++) {
            Vec3 c = a.pos.add(a.dir.scale(Fuga.SPEED * i / 5.0));
            Vfx.core(Vfx.WHITE, 0.7f * k, 3, c);
            Vfx.core(Vfx.YELLOW, 1.2f * k, 3, c);
            Vfx.glow(Vfx.ORANGE, 2.2f * k, 5, c, Vec3.ZERO);
            for (int j = 0; j < 3; j++) Vfx.fire(j == 0 ? Vfx.YELLOW : Vfx.ORANGE, Vfx.rf(1.0f, 1.9f) * k, 12, c.add(Vfx.rnd(0.35 * k)), a.dir.scale(-0.15).add(Vfx.rnd(0.06)));
            Vfx.ember(Vfx.YELLOW, 0.18f, 16, c.add(Vfx.rnd(0.4)), Vfx.rnd(0.15));
            if (Vfx.chance(0.3)) Vfx.smoke(Vfx.GREY, 1.2f * k, 24, c.add(Vfx.rnd(0.3)), Vfx.rnd(0.05), false);
        }
        Vfx.ring(Vfx.ORANGE, 1.4f * k, 8, a.pos, 1.2f);
        if (Vfx.chance(0.35)) Vfx.lightning(a.pos, Vfx.unit(), 3 + Vfx.rf(0, 3), Vfx.ORANGE, 0);
    }

    // ---------------------------------------------------------------- the spire
    private static double radiusAt(Spire s, double h, double hMax, double scale) {
        return Fuga.spireRadius(s.full) * Math.pow(Math.max(0, 1 - h / hMax), 1.4) * scale + 0.7;
    }

    private static void spire(Spire s) {
        double hMax = Fuga.spireHeight(s.full);
        double grow = Math.min(1, s.age / 18.0);
        grow = grow * grow * (3 - 2 * grow);
        double fall = s.age > Fuga.SPIRE_TICKS - 20 ? Math.max(0.05, 1 - (s.age - (Fuga.SPIRE_TICKS - 20)) / 26.0) : 1;
        double H = hMax * grow * fall, sc = Math.max(0.15, fall);
        Vec3 b = s.base;

        for (int i = 0; i < 46; i++) {                                         // body of fire tongues
            double h = Math.pow(Math.random(), 0.7) * H;
            double r = radiusAt(s, h, hMax, sc) * Math.sqrt(Math.random());
            double ang = Math.random() * Math.PI * 2;
            Vec3 pos = b.add(Math.cos(ang) * r, h, Math.sin(ang) * r);
            double t = H > 0 ? h / H : 0;
            int col = t < 0.25 ? Vfx.EMBER : t < 0.6 ? Vfx.ORANGE : Vfx.YELLOW;
            Vec3 vel = new Vec3(-Math.sin(ang) * 0.12, 0.5 + Math.random() * 0.9, Math.cos(ang) * 0.12);
            Vfx.fire(col, (float) (1.4 + Math.random() * 2.2 + r * 0.12), 14, pos, vel);
        }
        for (int i = 0; i < 16; i++) {                                         // white-hot core down the axis
            double h = Math.random() * H;
            Vfx.core(i % 2 == 0 ? Vfx.WHITE : Vfx.YELLOW, (float) (1.0 + 2.2 * (1 - h / hMax)), 3, b.add(0, h, 0));
        }
        for (int k = 0; k < 3; k++)                                            // three twisting flame helixes
            for (int j = 0; j < 11; j++) {
                double h = H * j / 11.0, ang = h * 0.22 + s.age * 0.4 + k * 2.094, r = radiusAt(s, h, hMax, sc) * 0.9;
                Vfx.glow(k == 0 ? Vfx.YELLOW : Vfx.ORANGE, (float) (1.0 + r * 0.12), 4, b.add(Math.cos(ang) * r, h, Math.sin(ang) * r), new Vec3(0, 0.4, 0));
            }
        for (int ring = 0; ring < 7; ring++) {                                 // stacked rings climbing the spire
            double h = (s.age * 1.3 + ring * hMax / 7.0) % Math.max(1, H);
            double r = radiusAt(s, h, hMax, sc) * 1.15;
            for (int j = 0; j < 26; j++) {
                double ang = j / 26.0 * Math.PI * 2 + s.age * 0.05;
                Vfx.fire(Vfx.ORANGE, 0.9f, 6, b.add(Math.cos(ang) * r, h, Math.sin(ang) * r), new Vec3(0, 0.15, 0));
            }
        }
        for (int i = 0; i < 22; i++) {
            double r = Fuga.spireRadius(s.full) * sc * 1.4 * Math.random();
            double ang = Math.random() * Math.PI * 2;
            Vfx.ember(Vfx.YELLOW, 0.2f, 22, b.add(Math.cos(ang) * r, Math.random() * 6, Math.sin(ang) * r), new Vec3(Vfx.g(0.1), 0.8 + Math.random() * 1.0, Vfx.g(0.1)));
        }
        for (int i = 0; i < 6; i++) Vfx.smoke(i % 2 == 0 ? Vfx.GREY : Vfx.EMBER, (float) (3.5 + Math.random() * 2.5), 26,
                b.add(Vfx.g(2), H * Math.random(), Vfx.g(2)), new Vec3(0, 0.35, 0), i % 2 != 0);
        for (int i = 0; i < 3; i++) Vfx.fireball(Vfx.ORANGE, (float) (3 + Math.random() * 3), 10, b.add(Vfx.g(Fuga.spireRadius(s.full) * 0.5), 0.5, Vfx.g(Fuga.spireRadius(s.full) * 0.5)), new Vec3(0, 0.5, 0));
        if (s.age % 8 == 0) Vfx.shock(Vfx.ORANGE, (float) (Fuga.spireRadius(s.full) * 0.6), 18, b.add(0, 0.4, 0), 1.18f);
        if (s.age % 5 == 0) Vfx.lightning(b.add(0, H * Math.random(), 0), Vfx.unit(), 8 + Vfx.rf(0, 8), Vfx.YELLOW, 1);
        Vfx.flare(Vfx.WHITE, 5f, 3, b.add(0, H, 0));                          // blazing tip
        Vfx.glow(Vfx.ORANGE, 8f, 4, b.add(0, H, 0), Vec3.ZERO);
    }

    // ---------------------------------------------------------------- HUD
    public static void hud(GuiGraphics g, int w, int h) {
        if (charge <= 0.01f) return;
        int a = (int) (Math.min(1f, used / (float) FugaItem.FULL_DRAW) * 80);
        Hud.vignette(g, w, h, 0xFF5A00, 20 + a);
        float f = Math.min(1f, used / (float) FugaItem.FULL_DRAW);
        int bw = 140, bh = 6, x = (w - bw) / 2, y = h - 48;
        g.fill(x - 1, y - 1, x + bw + 1, y + bh + 1, 0xFF000000);
        g.fill(x, y, x + bw, y + bh, 0xFF2A1000);
        g.fillGradient(x, y, x + (int) (bw * f), y + bh, 0xFFFFB020, 0xFFFF3000);
        boolean ready = used >= FugaItem.MIN_DRAW, full = used >= FugaItem.FULL_DRAW;
        g.drawCenteredString(Minecraft.getInstance().font, full ? "FUGA  -  FULL DRAW" : ready ? "FUGA  -  RELEASE" : "FUGA  -  DRAWING",
                w / 2, y - 14, full ? 0xFFFFE070 : 0xFFFF9030);
    }
}
