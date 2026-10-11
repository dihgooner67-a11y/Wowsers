package com.example.railgun.client;

import com.example.railgun.Cleaves;
import com.example.railgun.FxPacket;
import com.example.railgun.Slashes;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/** Dismantle (long black cut with glowing white rims) and Cleave (a storm of small black and solid-red cuts). */
public final class SlashFx {
    private static final class Blade { Vec3 pos, dir; double traveled; }
    private static final class Storm { int owner, target, age; Vec3 last; }
    private static final List<Blade> BLADES = new ArrayList<>();
    private static final List<Storm> STORMS = new ArrayList<>();

    private SlashFx() {}

    public static void onPacket(FxPacket p, Minecraft mc) {
        Vec3 at = new Vec3(p.x, p.y, p.z), dir = new Vec3(p.dx, p.dy, p.dz);
        boolean mine = mc.player.getId() == p.owner;
        switch (p.arg) {
            case 0 -> {                                                              // Dismantle leaves your hands
                Blade b = new Blade(); b.pos = at; b.dir = dir.normalize(); BLADES.add(b);
                float roll = BlackHoleFx.rollOf(b.dir);
                Vfx.flare(Vfx.WHITE, 3f, 3, at);
                Vfx.core(Vfx.WHITE, 1.4f, 3, at);
                for (int i = 0; i < 3; i++) Vfx.slash(Vfx.WHITE, 2.2f + i * 0.5f, 5, at.add(b.dir.scale(i * 1.2)), roll + (i - 1) * 25f);
                Vfx.burst(at, Vfx.WHITE, 14, 1.0, 0.12f);
                if (mine) Impact.addShake(4f);
            }
            case 1, 3 -> {                                                           // a mob gets cut
                hitBurst(at, dir);
                if (p.arg == 1 && mine) { Impact.start(Impact.DISMANTLE, false); Impact.addShake(9f); }
                else Impact.addShake((float) (3 / (1 + mc.player.position().distanceTo(at) / 8)));
            }
            case 2 -> {                                                              // Cleave begins
                Storm s = new Storm(); s.owner = p.owner; s.target = (int) p.dx; s.last = at; STORMS.add(s);
                Vfx.core(Vfx.WHITE, 3f, 4, at);
                Vfx.shock(Vfx.RED, 1.5f, 14, at, 1.25f);
                if (mine) { Impact.start(Impact.CLEAVE, false); Impact.addShake(10f); }
            }
            default -> {                                                             // Cleave ends: one last storm
                for (int i = 0; i < 90; i++) cut(at.add(Vfx.rnd(2.2)), i % 3 == 0, 1.0f + Vfx.rf(0, 1.4f));
                Vfx.shock(Vfx.RED, 2f, 18, at, 1.3f);
                Vfx.core(Vfx.WHITE, 4f, 5, at);
                Vfx.burst(at, Vfx.RED, 60, 1.6, 0.16f);
                if (mine) Impact.addShake(20f);
            }
        }
    }

    /** A burst of tiny slashes + sparks where a mob was cut. */
    private static void hitBurst(Vec3 at, Vec3 dir) {
        float roll = BlackHoleFx.rollOf(dir);
        Vfx.core(Vfx.WHITE, 2.4f, 3, at);
        Vfx.glow(Vfx.WHITE, 4f, 6, at, Vec3.ZERO);
        for (int i = 0; i < 26; i++) Vfx.slash(Vfx.WHITE, Vfx.rf(0.8f, 2.4f), 4 + (int) Vfx.rf(0, 3), at.add(Vfx.rnd(0.6)), Vfx.rf(0, 360));
        Vfx.slash(Vfx.WHITE, 4.5f, 6, at, roll);
        Vfx.burst(at, Vfx.WHITE, 30, 1.2, 0.14f);
        for (int i = 0; i < 6; i++) Vfx.smoke(Vfx.GREY, 1.2f, 22, at.add(Vfx.rnd(0.3)), Vfx.rnd(0.08), false);
    }

    /** One short dismantle: black cut with red rim, or solid red. */
    private static void cut(Vec3 p, boolean solid, float size) {
        float roll = Vfx.rf(0, 360);
        int life = 3 + (int) Vfx.rf(0, 3);
        if (solid) Vfx.slashSolid(Vfx.chance(0.3) ? Vfx.BLACK : Vfx.RED, size, life, p, roll);
        else Vfx.slash(Vfx.RED, size, life, p, roll);
    }

    public static void tick(Minecraft mc) {
        for (Iterator<Blade> it = BLADES.iterator(); it.hasNext(); ) {
            Blade b = it.next();
            blade(mc, b);
            b.pos = b.pos.add(b.dir.scale(Slashes.SPEED));
            b.traveled += Slashes.SPEED;
            PostFx.want = Math.max(PostFx.want, 0.6f);
            if (b.traveled >= Slashes.RANGE) it.remove();
        }

        for (Iterator<Storm> it = STORMS.iterator(); it.hasNext(); ) {
            Storm s = it.next();
            s.age++;
            Entity t = mc.level.getEntity(s.target);
            if (t != null) s.last = t.position().add(0, t.getBbHeight() * 0.5, 0);
            for (int i = 0; i < 46; i++) {                                           // the storm of short dismantles
                double rad = 2.6 * Math.sqrt(Math.random());
                double ang = Math.random() * Math.PI * 2;
                Vec3 p = s.last.add(Math.cos(ang) * rad, Vfx.g(1.0), Math.sin(ang) * rad);
                cut(p, i % 2 == 0, Vfx.rf(0.5f, 1.7f));
            }
            for (int i = 0; i < 5; i++) Vfx.spark(Vfx.RED, 0.14f, 6, s.last.add(Vfx.rnd(1.2)), Vfx.rnd(0.35));
            if (s.age % 2 == 0) Vfx.smoke(Vfx.BLACK, 1.8f, 20, s.last.add(Vfx.rnd(0.8)), Vfx.rnd(0.06), false);
            if (s.age % 10 == 0) Vfx.shock(Vfx.RED, 1.2f, 12, s.last, 1.25f);
            PostFx.want = Math.max(PostFx.want, 0.9f);
            if (s.owner == mc.player.getId()) Impact.addShake(1.2f);
            if (s.age > Cleaves.DURATION + 4) it.remove();
        }
    }

    /** The Dismantle blade: a long black cut with white glowing rims, and a trench of dust along the ground. */
    private static void blade(Minecraft mc, Blade b) {
        float roll = BlackHoleFx.rollOf(b.dir);
        for (double t = 0; t < Slashes.SPEED; t += 2.0) {
            Vec3 c = b.pos.add(b.dir.scale(t));
            Vfx.slash(Vfx.WHITE, 2.4f, 3, c, roll);
            Vfx.glow(Vfx.WHITE, 0.5f, 4, c, Vec3.ZERO);                              // thin glow so it blooms
            if (Vfx.chance(0.3)) Vfx.spark(Vfx.WHITE, 0.1f, 6, c.add(Vfx.rnd(0.2)), Vfx.rnd(0.25));
        }
        for (double t = 0; t < Slashes.SPEED; t += 3.0) {                            // ground cut
            Vec3 c = b.pos.add(b.dir.scale(t));
            BlockHitResult g = mc.level.clip(new ClipContext(c, c.add(0, -12, 0), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, mc.player));
            if (g.getType() == HitResult.Type.MISS) continue;
            Vec3 ground = g.getLocation().add(0, 0.15, 0);
            Vfx.slashSolid(Vfx.BLACK, 3.2f, 8, ground, roll);
            for (int i = 0; i < 3; i++) Vfx.smoke(Vfx.GREY, Vfx.rf(1.0f, 2.0f), 24, ground.add(Vfx.rnd(0.5)), new Vec3(Vfx.g(0.05), Vfx.rf(0.05f, 0.2f), Vfx.g(0.05)), false);
            for (int i = 0; i < 2; i++) Vfx.ember(Vfx.WHITE, 0.12f, 10, ground, new Vec3(Vfx.g(0.2), Vfx.rf(0.2f, 0.5f), Vfx.g(0.2)));
        }
    }
}
