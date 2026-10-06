package com.example.railgun.client;

import com.example.railgun.BlackHoles;
import com.example.railgun.FxPacket;
import com.mojang.math.Axis;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

/**
 * Unlimited Black Hole visuals: a round black sphere, photon rings, a tilted accretion disk, jets and inflow made
 * from our own particles; a gravitational lens in the screen filter; and the scale cutscene (camera pulls back
 * to show how big it is, with measurement callouts and impact frames).
 */
public final class BlackHoleFx {
    private static final class Hole { Vec3 c; int age; int owner; }
    private static final List<Hole> HOLES = new ArrayList<>();

    private static final Vec3 N = new Vec3(0.25, 0.92, 0.30).normalize();         // accretion disk axis
    private static final Vec3 U = N.cross(new Vec3(0, 0, 1)).normalize();
    private static final Vec3 V = N.cross(U);
    private static final int[] DISK = {Vfx.WHITE, Vfx.YELLOW, Vfx.ORANGE, Vfx.EMBER};

    private static final int CUT_LEN = 200;
    private static boolean cut;
    private static int cutAge;
    private static ArmorStand dummy;
    private static Entity savedCam;
    private static Vec3 p0 = Vec3.ZERO, hc = Vec3.ZERO, lockLook = new Vec3(0, 0, 1);
    private static float lockYaw, lockPitch;

    private static Vec3 camPos = Vec3.ZERO, camFwd = new Vec3(0, 0, 1), camLeft = new Vec3(1, 0, 0), camUp = new Vec3(0, 1, 0);
    public static double lastFov = 70;

    private BlackHoleFx() {}

    public static boolean cutsceneActive() { return cut; }

    // ---------------------------------------------------------------- packets
    public static void onPacket(FxPacket p, Minecraft mc) {
        Vec3 c = new Vec3(p.x, p.y, p.z);
        double d = mc.player.position().distanceTo(c);
        switch (p.arg) {
            case 0 -> {
                Hole h = new Hole(); h.c = c; h.owner = p.owner; HOLES.add(h);
                if (mc.player.getId() == p.owner) startCutscene(mc, c);
                else Impact.addShake((float) Math.min(10, 400 / Math.max(d, 20)));
            }
            case 1 -> {
                for (Hole h : HOLES) if (h.c.distanceToSqr(c) < 4) h.age = Math.max(h.age, BlackHoles.COLLAPSE_AT);
                if (d < 300) { Impact.start(Impact.BH_COLLAPSE, false); Impact.addShake(20f); }
            }
            default -> {
                HOLES.removeIf(h -> h.c.distanceToSqr(c) < 4);
                blast(c);
                if (d < 300) { Impact.start(Impact.BH_COLLAPSE, false); Impact.addShake((float) Math.min(40, 1600 / Math.max(d, 30))); }
            }
        }
    }

    private static void blast(Vec3 c) {
        Vfx.core(Vfx.WHITE, 40f, 10, c);
        Vfx.glow(Vfx.ORANGE, 70f, 22, c, Vec3.ZERO);
        for (int i = 0; i < 6; i++) Vfx.shock(i % 2 == 0 ? Vfx.ORANGE : Vfx.VIOLET, 6f + i * 3, 24 + i * 5, c, 1.2f);
        for (int i = 0; i < 400; i++) Vfx.burst(c, i % 2 == 0 ? Vfx.YELLOW : Vfx.VIOLET, 1, 3.5, 0.5f);
        for (int i = 0; i < 80; i++) Vfx.smoke(i % 2 == 0 ? Vfx.BLACK : Vfx.GREY, Vfx.rf(6, 12), 70, c.add(Vfx.rnd(8)), Vfx.unit().scale(0.9), false);
        for (int i = 0; i < 24; i++) Vfx.lightning(c, Vfx.unit(), 20 + Vfx.rf(0, 14), i % 2 == 0 ? Vfx.ORANGE : Vfx.VIOLET, 2);
    }

    // ---------------------------------------------------------------- particles
    public static void tick(Minecraft mc) {
        HOLES.removeIf(h -> h.age > BlackHoles.LIFE + 20);
        for (Hole h : HOLES) {
            h.age++;
            double d = h.c.distanceTo(mc.player.position());
            if (d < 450) particles(h);
            PostFx.want = Math.max(PostFx.want, 1.6f);
            float rumble = (float) Math.max(0, 2.5 - d / 40.0);
            if (rumble > 0) Impact.addShake(rumble);
        }
        if (cut) cutTick(mc);
    }

    private static void particles(Hole h) {
        double r = BlackHoles.radius(h.age);
        Vec3 c = h.c;
        for (int i = 0; i < 4; i++) Vfx.disc((float) r, 4, c);                         // the black sphere
        Vfx.halo(Vfx.WHITE, (float) (r * 1.10), 3, c);                                  // photon rings
        Vfx.halo(Vfx.YELLOW, (float) (r * 1.22), 3, c);
        Vfx.halo(Vfx.ORANGE, (float) (r * 1.45), 3, c);
        Vfx.swirl(0xFF7A20, (float) (r * 3.0), 4, c, 3f);                               // spiral glow, counter-rotating layers
        Vfx.swirl(Vfx.DEEP, (float) (r * 5.0), 4, c, -1.5f);

        for (int i = 0; i < 150; i++) {                                                 // accretion disk (orbiting streaks)
            double th = Vfx.rf(0, 6.2832f), f = Math.pow(Math.random(), 1.6);
            double rad = r * (1.25 + f * (BlackHoles.RING - 1.25));
            double dop = 0.5 + 0.5 * Math.cos(th - h.age * 0.05);                       // one side burns brighter
            int tier = rad < r * 1.6 ? 0 : rad < r * 2.1 ? 1 : rad < r * 2.8 ? 2 : 3;
            if (dop > 0.8 && tier > 0) tier--;
            Vec3 dir = U.scale(Math.cos(th)).add(V.scale(Math.sin(th)));
            Vec3 pos = c.add(dir.scale(rad)).add(N.scale(Vfx.g(r * 0.04)));
            double speed = 0.8 / Math.sqrt(rad / r);
            Vec3 tan = U.scale(-Math.sin(th)).add(V.scale(Math.cos(th))).scale(speed);
            Vfx.add(Vfx.GLOW, DISK[tier], (float) (0.25 + r * 0.05 * (1.2 - f)), 6, pos, tan, 1f, 0f, 0.97f, 0f, 0f, true);
        }
        for (int s = -1; s <= 1; s += 2)                                                // relativistic jets
            for (int i = 0; i < 8; i++) {
                double t = r * (1.2 + Math.random() * 9);
                Vfx.glow(Vfx.VIOLET, (float) (0.35 + t * 0.03), 7, c.add(N.scale(s * t)).add(Vfx.rnd(r * 0.08)), N.scale(s * 0.9));
            }
        for (int i = 0; i < 18; i++) {                                                  // matter spiralling in
            Vec3 dir = Vfx.unit();
            double d = r * (3.5 + Math.random() * 4);
            Vec3 tan = dir.cross(N).normalize().scale(0.5);
            Vfx.glow(Vfx.VIOLET, 0.45f, 9, c.add(dir.scale(d)), dir.scale(-0.6).add(tan));
            if (i % 6 == 0) Vfx.smoke(Vfx.GREY, 1.6f, 14, c.add(dir.scale(d)), dir.scale(-0.7), false);
        }
        if (h.age % 6 == 0) {
            double th = Vfx.rf(0, 6.2832f);
            Vec3 dir = U.scale(Math.cos(th)).add(V.scale(Math.sin(th)));
            Vfx.lightning(c.add(dir.scale(r * 1.4)), dir, 8 + Vfx.rf(0, 8), Vfx.chance(0.5) ? Vfx.ORANGE : Vfx.WHITE, 1);
        }
    }

    // ---------------------------------------------------------------- cutscene
    private static double ease(double x) { x = Math.max(0, Math.min(1, x)); return x * x * (3 - 2 * x); }
    private static Vec3 lerp(Vec3 a, Vec3 b, double t) { return a.add(b.subtract(a).scale(t)); }

    private static void startCutscene(Minecraft mc, Vec3 c) {
        if (cut) return;
        cut = true; cutAge = 0; hc = c;
        p0 = mc.player.getEyePosition();
        lockYaw = mc.player.getYRot(); lockPitch = mc.player.getXRot();
        lockLook = mc.player.getLookAngle();
        dummy = new ArmorStand(EntityType.ARMOR_STAND, mc.level);
        savedCam = mc.getCameraEntity();
        place(camPos(0), target(0), true);
        mc.setCameraEntity(dummy);
        Impact.start(Impact.BH_CAST, false);
        Impact.addShake(8f);
    }

    private static Vec3 vantage() {
        Vec3 dir = hc.subtract(p0).normalize();
        Vec3 side = dir.cross(new Vec3(0, 1, 0)).normalize();
        Vec3 back = p0.subtract(hc).normalize();
        return hc.add(side.scale(95)).add(back.scale(40)).add(0, 35, 0);
    }

    private static Vec3 orbit(double ang) {
        Vec3 rel = vantage().subtract(hc);
        double cs = Math.cos(ang), sn = Math.sin(ang);
        return hc.add(new Vec3(rel.x * cs - rel.z * sn, rel.y, rel.x * sn + rel.z * cs));
    }

    private static Vec3 camPos(double t) {
        Vec3 push = p0.add(hc.subtract(p0).normalize().scale(4));
        if (t < 30) return lerp(p0, push, ease(t / 30));
        if (t < 100) {                                                   // pull back and up on a swooping curve
            double k = ease((t - 30) / 70.0);
            Vec3 mid = lerp(push, vantage(), 0.5).add(0, 28, 0);
            return lerp(lerp(push, mid, k), lerp(mid, vantage(), k), k);
        }
        if (t < 160) return orbit(Math.toRadians(35) * ease((t - 100) / 60.0));
        return lerp(orbit(Math.toRadians(35)), p0, ease((t - 160) / 40.0));
    }

    private static Vec3 target(double t) {
        Vec3 m = lerp(hc, p0, 0.35).add(0, 3, 0);
        if (t < 30) return hc;
        if (t < 100) return lerp(hc, m, ease((t - 30) / 70.0));
        if (t < 160) return m;
        return lerp(m, p0.add(lockLook.scale(10)), ease((t - 160) / 40.0));
    }

    private static void place(Vec3 pos, Vec3 tgt, boolean snap) {
        Vec3 d = tgt.subtract(pos);
        double hz = Math.sqrt(d.x * d.x + d.z * d.z);
        float yaw = (float) Math.toDegrees(Math.atan2(-d.x, d.z));
        float pitch = (float) -Math.toDegrees(Math.atan2(d.y, hz));
        dummy.xo = snap ? pos.x : dummy.getX();
        dummy.yo = snap ? pos.y - dummy.getEyeHeight() : dummy.getY();
        dummy.zo = snap ? pos.z : dummy.getZ();
        float oldYaw = snap ? yaw : dummy.getYRot();
        while (yaw - oldYaw > 180) oldYaw += 360;
        while (yaw - oldYaw < -180) oldYaw -= 360;
        dummy.yRotO = oldYaw;
        dummy.xRotO = snap ? pitch : dummy.getXRot();
        dummy.setPos(pos.x, pos.y - dummy.getEyeHeight(), pos.z);
        dummy.setYRot(yaw);
        dummy.setXRot(pitch);
    }

    private static void cutTick(Minecraft mc) {
        cutAge++;
        mc.player.setYRot(lockYaw);
        mc.player.setXRot(lockPitch);
        if (cutAge == 28) Impact.start(Impact.BH_WARP, false);
        if (cutAge == 100) { Impact.start(Impact.BH_REVEAL, false); Impact.addShake(10f); }
        place(camPos(cutAge), target(cutAge), false);
        if (cutAge >= CUT_LEN) endCut(mc);
    }

    private static void endCut(Minecraft mc) {
        mc.setCameraEntity(savedCam != null ? savedCam : mc.player);
        cut = false;
        dummy = null;
        savedCam = null;
    }

    public static void reset(Minecraft mc) {
        if (cut) { if (mc.player != null) mc.setCameraEntity(mc.player); cut = false; dummy = null; }
        HOLES.clear();
        PostFx.setLens(0, 0, 0, 0);
    }

    /** Extra field of view during the wide shot, so the scale sinks in. */
    public static float fovBonus() {
        if (!cut) return 0f;
        if (cutAge < 30) return 0f;
        if (cutAge < 100) return (float) (25 * ease((cutAge - 30) / 70.0));
        if (cutAge < 160) return 25f;
        return (float) (25 * (1 - ease((cutAge - 160) / 40.0)));
    }

    // ---------------------------------------------------------------- per-frame: camera snapshot + lens
    public static void frame(float pt, Camera cam) {
        Minecraft mc = Minecraft.getInstance();
        Vector3f l = cam.getLeftVector(), u = cam.getUpVector(), f = cam.getLookVector();
        camPos = cam.getPosition();
        camLeft = new Vec3(l.x, l.y, l.z); camUp = new Vec3(u.x, u.y, u.z); camFwd = new Vec3(f.x, f.y, f.z);

        Hole best = null;
        double bd = Double.MAX_VALUE;
        for (Hole h : HOLES) { double d = camPos.distanceToSqr(h.c); if (d < bd) { bd = d; best = h; } }
        if (best == null) { PostFx.setLens(0, 0, 0, 0); return; }
        Vec3 rel = best.c.subtract(camPos);
        double z = rel.dot(camFwd);
        if (z < 1.0) { PostFx.setLens(0, 0, 0, 0); return; }
        double tan = tanHalf(), aspect = (double) mc.getWindow().getWidth() / mc.getWindow().getHeight();
        double nx = rel.dot(camLeft) / z / (tan * aspect), ny = rel.dot(camUp) / z / tan;
        float lr = (float) (0.5 * (BlackHoles.radius(best.age) / z) / tan);
        if (lr < 0.003f) { PostFx.setLens(0, 0, 0, 0); return; }
        float str = Impact.kind() == Impact.BH_REVEAL ? 1.0f : 0.55f;
        PostFx.setLens((float) (0.5 - 0.5 * nx), (float) (0.5 + 0.5 * ny), str, lr);
    }

    private static double tanHalf() { return Math.tan(Math.toRadians(lastFov / 2.0)); }

    private static double[] project(Vec3 w, int sw, int sh) {
        Vec3 rel = w.subtract(camPos);
        double z = rel.dot(camFwd);
        if (z < 0.5) return null;
        double tan = tanHalf(), aspect = (double) sw / sh;
        double nx = rel.dot(camLeft) / z / (tan * aspect), ny = rel.dot(camUp) / z / tan;
        return new double[]{(0.5 - 0.5 * nx) * sw, (0.5 - 0.5 * ny) * sh};
    }

    // ---------------------------------------------------------------- HUD: letterbox, title, scale callouts
    public static void hud(GuiGraphics g, int w, int h) {
        if (!cut) return;
        double in = ease(cutAge / 12.0), out = ease((CUT_LEN - cutAge) / 12.0);
        int bar = (int) (h * 0.12 * Math.min(in, out));
        g.fill(0, 0, w, bar, 0xFF000000);
        g.fill(0, h - bar, w, h, 0xFF000000);
        Font font = Minecraft.getInstance().font;

        if (cutAge >= 8 && cutAge <= 72) {
            int a = (int) (255 * Math.min(ease((cutAge - 8) / 10.0), ease((72 - cutAge) / 10.0)));
            if (a > 4) {
                g.drawCenteredString(font, "UNLIMITED BLACK HOLE", w / 2, h - bar - 26, (a << 24) | 0xFFFFFF);
                g.drawCenteredString(font, "- domain of infinite mass -", w / 2, h - bar - 14, (a << 24) | 0xFFB070);
            }
        }
        if (cutAge >= 105 && cutAge <= 172) {
            int a = (int) (255 * Math.min(ease((cutAge - 105) / 10.0), ease((172 - cutAge) / 10.0)));
            if (a <= 4) return;
            int col = (a << 24) | 0xFFD070, white = (a << 24) | 0xFFFFFF;
            double R = BlackHoles.R, ring = R * BlackHoles.RING;
            double[] a1 = project(hc.add(camLeft.scale(R)), w, h), b1 = project(hc.subtract(camLeft.scale(R)), w, h);
            if (a1 != null && b1 != null) bracket(g, font, a1, b1, 0, "EVENT HORIZON  " + (int) (R * 2) + " BLOCKS", col);
            double[] a2 = project(hc.add(camLeft.scale(ring)), w, h), b2 = project(hc.subtract(camLeft.scale(ring)), w, h);
            if (a2 != null && b2 != null) bracket(g, font, a2, b2, 46, "ACCRETION DISK  " + (int) (ring * 2) + " BLOCKS  (" + (int) (ring * 2 / 16) + " CHUNKS)", col);
            double[] you = project(p0.subtract(0, 0.7, 0), w, h);
            if (you != null) {
                int x = (int) you[0], y = (int) you[1];
                g.fill(x - 8, y - 8, x + 8, y - 7, white); g.fill(x - 8, y + 7, x + 8, y + 8, white);
                g.fill(x - 8, y - 8, x - 7, y + 8, white); g.fill(x + 7, y - 8, x + 8, y + 8, white);
                g.drawCenteredString(font, "YOU  (1.8 BLOCKS)", x, y - 22, white);
            }
            g.drawCenteredString(font, "PULL RANGE  " + (int) BlackHoles.PULL + " BLOCKS  (" + (int) (BlackHoles.PULL / 16) + " CHUNKS)", w / 2, bar + 8, col);
        }
    }

    private static void bracket(GuiGraphics g, Font font, double[] a, double[] b, int yOff, String label, int col) {
        int x1 = (int) Math.min(a[0], b[0]), x2 = (int) Math.max(a[0], b[0]);
        int y = (int) ((a[1] + b[1]) / 2) + yOff;
        g.fill(x1, y, x2, y + 1, col);
        g.fill(x1, y - 5, x1 + 1, y + 6, col);
        g.fill(x2, y - 5, x2 + 1, y + 6, col);
        g.drawCenteredString(font, label, (x1 + x2) / 2, y - 14, col);
    }
}
