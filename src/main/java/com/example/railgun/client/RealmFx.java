package com.example.railgun.client;

import com.example.railgun.FxPacket;
import com.example.railgun.RailgunMod;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;

/**
 * Cursed Realm visuals: shadow puddles (a real flat quad drawn on the ground), and inside the realm red rain, red lightning,
 * drifting ash, and the impact-frame screen filter switched on permanently.
 */
public final class RealmFx {
    private static final ResourceLocation TEX = new ResourceLocation(RailgunMod.MOD_ID, "textures/misc/puddle.png");
    private static final ResourceLocation REALM = new ResourceLocation(RailgunMod.MOD_ID, "cursed_realm");
    private static final Random RNG = new Random();

    private static final class Puddle {
        double x, y, z, px, py, pz, tx, ty, tz;
        float radius, pr, target = 1.7f;
        boolean closing;
        int life = -1;
        void snap(double x, double y, double z) { this.x = px = tx = x; this.y = py = ty = y; this.z = pz = tz = z; }
    }
    private static final Map<Integer, Puddle> PUDDLES = new HashMap<>();

    private static boolean inRealm;
    private static int lightning, nextStrike, titleTicks;
    private static long ticks, readyAt;

    private RealmFx() {}

    // ---------------------------------------------------------------- packets
    public static void onPacket(FxPacket p, Minecraft mc) {
        switch (p.arg) {
            case 0 -> {                                                      // a puddle opens under someone
                Puddle d = new Puddle(); d.snap(p.x, p.y, p.z); d.target = (float) p.dx;
                PUDDLES.put(p.owner, d);
            }
            case 1 -> {                                                      // the puddle glides to a new spot
                Puddle d = PUDDLES.get(p.owner);
                if (d == null) { d = new Puddle(); d.snap(p.x, p.y, p.z); d.radius = d.pr = (float) p.dx; PUDDLES.put(p.owner, d); }
                d.tx = p.x; d.ty = p.y; d.tz = p.z; d.target = (float) p.dx; d.closing = false;
            }
            case 2 -> { Puddle d = PUDDLES.get(p.owner); if (d != null) d.closing = true; }
            case 3 -> {                                                      // your own state: entered / left (dy = cooldown seconds)
                if (p.dx > 0.5) titleTicks = 90;
                else readyAt = ticks + (long) p.dy * 20;
            }
            default -> {                                                     // 4: a puddle opens, then closes again
                Puddle d = new Puddle(); d.snap(p.x, p.y, p.z); d.target = (float) p.dx; d.life = 40;
                PUDDLES.put(p.owner, d);
            }
        }
    }

    // ---------------------------------------------------------------- tick
    public static void tick(Minecraft mc) {
        ticks++;
        boolean now = mc.level.dimension().location().equals(REALM);
        if (now != inRealm) {
            inRealm = now;
            PUDDLES.clear();
            if (now) { titleTicks = 90; nextStrike = 30; }
        }
        if (lightning > 0) lightning--;
        if (titleTicks > 0) titleTicks--;

        for (Iterator<Puddle> it = PUDDLES.values().iterator(); it.hasNext(); ) {
            Puddle d = it.next();
            d.px = d.x; d.py = d.y; d.pz = d.z; d.pr = d.radius;
            d.x += (d.tx - d.x) * 0.5; d.y += (d.ty - d.y) * 0.5; d.z += (d.tz - d.z) * 0.5;
            if (d.life > 0 && --d.life == 0) d.closing = true;
            if (d.closing) d.radius -= 0.12f; else d.radius += (d.target - d.radius) * 0.18f;
            if (d.closing && d.radius < 0.05f) { it.remove(); continue; }
            if (d.radius > 0.4f) puddleFx(d);
        }
        if (inRealm) realm(mc);
    }

    /** Wisps of black smoke, red embers and crackles rising off a shadow puddle. */
    private static void puddleFx(Puddle d) {
        for (int i = 0; i < 3; i++) {
            double a = RNG.nextDouble() * Math.PI * 2, r = d.radius * 0.85 * Math.sqrt(RNG.nextDouble());
            Vec3 p = new Vec3(d.x + Math.cos(a) * r, d.y + 0.1, d.z + Math.sin(a) * r);
            Vfx.smoke(Vfx.BLACK, Vfx.rf(0.5f, 1.0f), 18, p, new Vec3(0, 0.06, 0), false);
        }
        for (int i = 0; i < 2; i++) {
            double a = RNG.nextDouble() * Math.PI * 2;
            Vec3 p = new Vec3(d.x + Math.cos(a) * d.radius * 0.95, d.y + 0.1, d.z + Math.sin(a) * d.radius * 0.95);
            Vfx.ember(Vfx.RED, 0.09f, 14, p, new Vec3(0, 0.07, 0));
            Vfx.glow(Vfx.CRIMSON, 0.4f, 6, p, new Vec3(0, 0.03, 0));
        }
        if (Vfx.chance(0.12)) Vfx.electric(new Vec3(d.x, d.y + 0.3, d.z), d.radius * 0.6, Vfx.RED, 1);
    }

    private static void realm(Minecraft mc) {
        Vec3 cam = mc.player.getEyePosition();
        for (int i = 0; i < 130; i++)                                       // red rain
            Vfx.add(Vfx.STREAK, i % 4 == 0 ? Vfx.CRIMSON : Vfx.RED, Vfx.rf(0.5f, 1.0f), 11,
                    cam.add(Vfx.rf(-18, 18), Vfx.rf(4, 18), Vfx.rf(-18, 18)), new Vec3(0.12, -1.3, 0.04), 1f, 0f, 1f, 0f, 84f, true);
        for (int i = 0; i < 14; i++) {                                      // splashes where it lands
            double x = cam.x + Vfx.rf(-16, 16), z = cam.z + Vfx.rf(-16, 16);
            int gy = mc.level.getHeight(Heightmap.Types.MOTION_BLOCKING, (int) Math.floor(x), (int) Math.floor(z));
            Vfx.spark(Vfx.RED, 0.08f, 5, new Vec3(x, gy + 0.1, z), new Vec3(Vfx.g(0.05), 0.12, Vfx.g(0.05)));
        }
        for (int i = 0; i < 6; i++)                                         // drifting ash
            Vfx.ember(Vfx.CRIMSON, 0.12f, 40, cam.add(Vfx.rnd(14)), new Vec3(0.02, 0.03, 0.0));

        if (--nextStrike <= 0) { strike(mc); nextStrike = 50 + RNG.nextInt(80); }
        PostFx.want = Math.max(PostFx.want, 0.7f);
    }

    /** A red lightning bolt from the sky onto the plain, with thunder and a flash. */
    private static void strike(Minecraft mc) {
        double ang = RNG.nextDouble() * Math.PI * 2, dist = 25 + RNG.nextDouble() * 55;
        double x = mc.player.getX() + Math.cos(ang) * dist, z = mc.player.getZ() + Math.sin(ang) * dist;
        int gy = mc.level.getHeight(Heightmap.Types.MOTION_BLOCKING, (int) Math.floor(x), (int) Math.floor(z));
        Vec3 ground = new Vec3(x, gy, z);
        Vfx.lightning(ground.add(0, 140, 0), new Vec3(0, -1, 0), 140, Vfx.RED, 3);
        Vfx.core(Vfx.WHITE, 6f, 5, ground);
        Vfx.glow(Vfx.RED, 12f, 10, ground, Vec3.ZERO);
        Vfx.shock(Vfx.RED, 2f, 14, ground, 1.3f);
        Vfx.burst(ground, Vfx.RED, 40, 1.2, 0.16f);
        mc.level.playLocalSound(x, gy, z, SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.WEATHER, 6f, 0.6f + RNG.nextFloat() * 0.2f, false);
        lightning = 4;
        Impact.addShake((float) Math.max(0, 5 - dist / 20));
    }

    // ---------------------------------------------------------------- every frame: the filter is always on in the realm
    public static void frame(float pt) {
        if (inRealm && !Impact.active()) PostFx.set(9, 0.9f, lightning > 0 ? 1f : 0f, 0.7f);
    }

    /** Draws every shadow puddle as a flat textured quad lying on the ground. */
    public static void renderPuddles(PoseStack ps, Camera cam, float pt) {
        if (PUDDLES.isEmpty()) return;
        Minecraft mc = Minecraft.getInstance();
        MultiBufferSource.BufferSource bs = mc.renderBuffers().bufferSource();
        RenderType rt = RenderType.entityTranslucent(TEX);
        VertexConsumer vc = bs.getBuffer(rt);
        Vec3 c = cam.getPosition();
        for (Puddle d : PUDDLES.values()) {
            float r = d.pr + (d.radius - d.pr) * pt;
            if (r < 0.05f) continue;
            r *= 1f + 0.04f * (float) Math.sin((ticks + pt) * 0.3 + d.x);
            double x = d.px + (d.x - d.px) * pt, y = d.py + (d.y - d.py) * pt, z = d.pz + (d.z - d.pz) * pt;
            ps.pushPose();
            ps.translate(x - c.x, y - c.y + 0.03, z - c.z);
            Matrix4f m = ps.last().pose();
            Matrix3f n = ps.last().normal();
            quad(vc, m, n, -r, -r, 0, 0);
            quad(vc, m, n, -r, r, 0, 1);
            quad(vc, m, n, r, r, 1, 1);
            quad(vc, m, n, r, -r, 1, 0);
            ps.popPose();
        }
        bs.endBatch(rt);
    }

    private static void quad(VertexConsumer vc, Matrix4f m, Matrix3f n, float x, float z, float u, float v) {
        vc.vertex(m, x, 0f, z).color(255, 255, 255, 235).uv(u, v).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(0xF000F0).normal(n, 0f, 1f, 0f).endVertex();
    }

    // ---------------------------------------------------------------- HUD
    public static void hud(GuiGraphics g, int w, int h) {
        Minecraft mc = Minecraft.getInstance();
        if (inRealm) {
            Hud.vignette(g, w, h, 0xB00018, 70);
            if (lightning > 0) g.fill(0, 0, w, h, ((lightning * 35) << 24) | 0xFF2030);
            if (titleTicks > 0) {
                int a = (int) (255 * Math.min(1f, Math.min(titleTicks / 20f, (90 - titleTicks) / 12f)));
                if (a > 4) {
                    g.drawCenteredString(mc.font, "THE CURSED REALM", w / 2, h / 3, (a << 24) | 0xFF3040);
                    g.drawCenteredString(mc.font, "no mobs here  -  use the item to leave", w / 2, h / 3 + 12, (a << 24) | 0xFFB0B0);
                }
            }
        } else if (readyAt > ticks) {
            g.drawCenteredString(mc.font, "Cursed Realm closed for " + ((readyAt - ticks + 19) / 20) + " s", w / 2, h - 62, 0xFFFF6070);
        }
    }
}
