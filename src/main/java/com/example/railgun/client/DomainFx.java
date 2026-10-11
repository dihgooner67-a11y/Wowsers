package com.example.railgun.client;

import com.example.railgun.DomainType;
import com.example.railgun.FxPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.phys.Vec3;

/** Domain visuals: white slashes (shrine), star field + black hole (void), petals (hometown), sealing impact frames. */
public final class DomainFx {
    private static DomainType type;
    private static Vec3 center = Vec3.ZERO;
    private static boolean active;
    private static long ticks;

    private DomainFx() {}

    public static void onPacket(FxPacket p, Minecraft mc) {
        DomainType t = DomainType.byId(p.arg / 10);
        int phase = p.arg % 10;
        Vec3 c = new Vec3(p.x, p.y, p.z);
        boolean near = mc.player.position().distanceTo(c) <= t.radius + 8;
        switch (phase) {
            case 0 -> { type = t; center = c; active = true; if (near) Impact.addShake(3f); }
            case 1 -> {
                if (near) {
                    int kind = switch (t) { case SHRINE -> Impact.D_SHRINE; case VOID -> Impact.D_VOID; case HOMETOWN -> Impact.D_HOME; default -> Impact.D_POCKET; };
                    Impact.start(kind, false);
                    Impact.addShake(14f);
                }
                Vfx.shock(Vfx.WHITE, 2f, 24, c.add(0, 1, 0), 1.3f);
            }
            default -> {
                active = false;
                if (near) { Impact.startSoft(switch (t) { case SHRINE -> 1; case VOID -> 2; case HOMETOWN -> 5; default -> 0; }); Impact.addShake(5f); }
            }
        }
    }

    private static boolean inside(Minecraft mc) {
        if (!active || type == null) return false;
        double dx = mc.player.getX() - center.x, dz = mc.player.getZ() - center.z, dy = mc.player.getY() - center.y;
        return dx * dx + dz * dz <= (double) type.radius * type.radius && dy > -4 && dy < type.height + 4;
    }

    public static void tick(Minecraft mc) {
        ticks++;
        if (!inside(mc)) return;
        Vec3 cam = mc.player.getEyePosition();
        switch (type) {
            case SHRINE -> {
                for (int i = 0; i < 46; i++)                                    // hundreds of white slashes per second
                    Vfx.streak(Vfx.WHITE, Vfx.rf(1.2f, 3.8f), 3 + (int) Vfx.rf(0, 3),
                            cam.add(Vfx.g(7), Vfx.g(3.5), Vfx.g(7)), Vfx.rf(-70, 70) + (Vfx.chance(0.5) ? 20 : 160));
                for (int i = 0; i < 6; i++) Vfx.spark(Vfx.WHITE, 0.14f, 6, cam.add(Vfx.rnd(9)), Vfx.rnd(0.3));
                for (int i = 0; i < 3; i++) Vfx.glow(Vfx.CRIMSON, 0.9f, 8, cam.add(Vfx.rnd(12)), new Vec3(0, 0.05, 0));   // embers
                PostFx.want = Math.max(PostFx.want, 0.4f);
            }
            case VOID -> {
                for (int i = 0; i < 12; i++) Vfx.glow(i % 4 == 0 ? Vfx.CYAN : Vfx.WHITE, 0.09f, 40, cam.add(Vfx.rnd(14)), Vfx.rnd(0.02));
                blackHole();
                PostFx.want = Math.max(PostFx.want, 0.9f);
            }
            case HOMETOWN -> {
                for (int i = 0; i < 4; i++)
                    mc.level.addParticle(ParticleTypes.CHERRY_LEAVES, cam.x + Vfx.g(7), cam.y + 4 + Vfx.g(2), cam.z + Vfx.g(7), 0, -0.02, 0);
                for (int i = 0; i < 5; i++) Vfx.glow(Vfx.YELLOW, 0.12f, 30, cam.add(Vfx.rnd(10)), new Vec3(0, 0.02, 0));
                PostFx.want = Math.max(PostFx.want, 0.5f);
            }
            default -> { }
        }
    }

    private static void blackHole() {
        Vec3 bh = center.add(DomainType.BLACK_HOLE);
        Vec3 n = new Vec3(0.35, 0.9, 0.2).normalize();
        Vec3 u = n.cross(new Vec3(0, 0, 1)).normalize(), v = n.cross(u);
        Vfx.smoke(Vfx.BLACK, 4.2f, 5, bh, Vec3.ZERO, false);                    // event horizon
        for (int i = 0; i < 30; i++) {
            double th = Vfx.rf(0, 6.2832f), r = 5.5 + Vfx.rf(0, 3.5f);
            Vec3 dir = u.scale(Math.cos(th)).add(v.scale(Math.sin(th)));
            Vec3 pos = bh.add(dir.scale(r));
            Vec3 tan = u.scale(-Math.sin(th)).add(v.scale(Math.cos(th))).scale(0.4);
            Vfx.glow(r < 6.8 ? Vfx.WHITE : r < 8 ? Vfx.YELLOW : Vfx.ORANGE, 0.9f, 8, pos, tan);
        }
        Vfx.ring(Vfx.ORANGE, 6.5f, 6, bh, 1.0f);
        for (int i = 0; i < 6; i++) {
            Vec3 dir = Vfx.unit();
            Vfx.spark(Vfx.VIOLET, 0.2f, 14, bh.add(dir.scale(11)), dir.scale(-0.7));
        }
    }

    public static void hud(GuiGraphics g, int w, int h) {
        Minecraft mc = Minecraft.getInstance();
        if (!inside(mc)) return;
        switch (type) {
            case SHRINE -> { Hud.vignette(g, w, h, 0xC01010, 80); }
            case VOID -> Hud.vignette(g, w, h, 0x4010A0, 90);
            case HOMETOWN -> g.fill(0, 0, w, h, 0x1CFFC060);
            default -> { }
        }
    }
}
