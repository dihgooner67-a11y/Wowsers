package com.example.railgun.client;

import com.example.railgun.FxPacket;
import com.example.railgun.PuddleColor;
import com.example.railgun.RailgunMod;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Puddle Dive visuals. While someone is in ink form their model is hidden and a coloured puddle slides under them
 * (clinging to the wall ahead when they are climbing). Local player: wall climbing, speed comes from the server.
 */
public final class InkFx {
    private static final ResourceLocation TEX = new ResourceLocation(RailgunMod.MOD_ID, "textures/misc/ink_puddle.png");
    private static final Map<Integer, Integer> DIVERS = new HashMap<>();          // entity id -> colour id

    private static final class Open { double x, y, z; int color; float radius, pr; boolean closing; int life = -1; }
    private static final Map<Integer, Open> OPEN = new HashMap<>();
    private static long ticks;

    private InkFx() {}

    public static boolean isDiver(int id) { return DIVERS.containsKey(id); }

    private static int glowOf(PuddleColor c) { return c == PuddleColor.BLACK ? 0x8A5CFF : c.rgb; }

    // ---------------------------------------------------------------- packets
    public static void onPacket(FxPacket p, Minecraft mc) {
        PuddleColor c = PuddleColor.byId((int) p.dx);
        Vec3 at = new Vec3(p.x, p.y, p.z);
        boolean mine = mc.player.getId() == p.owner;
        switch (p.arg) {
            case 0 -> {                                                           // puddle opens, you sink
                Open o = new Open(); o.x = at.x; o.y = at.y; o.z = at.z; o.color = c.id;
                OPEN.put(p.owner, o);
                splash(at, c, 26, 0.5);
                if (mine) Impact.addShake(2f);
            }
            case 1 -> {                                                           // ink form begins
                DIVERS.put(p.owner, c.id);
                Open o = OPEN.get(p.owner);
                if (o != null) o.closing = true;
                splash(at.add(0, 0.3, 0), c, 20, 0.6);
            }
            default -> {                                                          // surfaced
                DIVERS.remove(p.owner);
                Open o = new Open(); o.x = at.x; o.y = at.y; o.z = at.z; o.color = c.id; o.radius = o.pr = 1.2f; o.life = 14;
                OPEN.put(p.owner, o);
                burst(at, c);
                if (mine) Impact.addShake(8f);
                else Impact.addShake((float) (4 / (1 + mc.player.position().distanceTo(at) / 6)));
            }
        }
    }

    private static void droplet(PuddleColor c, Vec3 at, Vec3 vel, float size) {
        Vfx.add(Vfx.DISC, c.rgb, size, 14 + (int) Vfx.rf(0, 8), at, vel, 0.96f, 1.2f, 0.97f, 0f, 0f, false);
    }

    private static void splash(Vec3 at, PuddleColor c, int n, double speed) {
        for (int i = 0; i < n; i++) {
            double a = Math.random() * Math.PI * 2, s = speed * (0.4 + Math.random());
            droplet(c, at.add(0, 0.1, 0), new Vec3(Math.cos(a) * s, 0.15 + Math.random() * 0.35, Math.sin(a) * s), Vfx.rf(0.08f, 0.2f));
        }
        Vfx.shock(glowOf(c), 0.7f, 10, at.add(0, 0.1, 0), 1.2f);
    }

    /** The burst when you surface: a ring of ink, then the colour's own flourish. */
    private static void burst(Vec3 at, PuddleColor c) {
        int g = glowOf(c);
        Vfx.core(Vfx.WHITE, 1.6f, 4, at.add(0, 0.5, 0));
        Vfx.glow(g, 3.5f, 9, at.add(0, 0.5, 0), Vec3.ZERO);
        Vfx.shock(g, 1.2f, 14, at.add(0, 0.1, 0), 1.3f);
        Vfx.shock(g, 0.7f, 18, at.add(0, 0.1, 0), 1.22f);
        for (int i = 0; i < 70; i++) {
            double a = Math.random() * Math.PI * 2, s = 0.35 + Math.random() * 0.8;
            droplet(c, at.add(0, 0.2, 0), new Vec3(Math.cos(a) * s, 0.3 + Math.random() * 0.5, Math.sin(a) * s), Vfx.rf(0.1f, 0.28f));
        }
        switch (c) {
            case RED -> { for (int i = 0; i < 30; i++) Vfx.flame(Vfx.RED, Vfx.rf(0.4f, 0.9f), 16, at.add(Vfx.rnd(0.4)), new Vec3(Vfx.g(0.2), Vfx.rf(0.1f, 0.35f), Vfx.g(0.2))); Vfx.electric(at.add(0, 0.8, 0), 1.2, Vfx.RED, 6); }
            case BLUE -> { for (int i = 0; i < 4; i++) Vfx.ring(Vfx.CYAN, 0.6f + i * 0.3f, 12 + i * 3, at.add(0, 0.4 + i * 0.35, 0), 1.22f); Vfx.burst(at.add(0, 0.6, 0), Vfx.CYAN, 40, 0.9, 0.12f); }
            case GREEN -> { for (int i = 0; i < 24; i++) Vfx.glow(0x6BFF8A, Vfx.rf(0.3f, 0.7f), 22, at.add(Vfx.rnd(0.6)), new Vec3(Vfx.g(0.05), Vfx.rf(0.05f, 0.2f), Vfx.g(0.05))); Vfx.burst(at.add(0, 0.6, 0), 0x6BFF8A, 30, 0.7, 0.12f); }
            case BLACK -> { for (int i = 0; i < 24; i++) Vfx.smoke(Vfx.BLACK, Vfx.rf(1.0f, 2.2f), 30, at.add(Vfx.rnd(0.5)), Vfx.unit().scale(0.18), false); Vfx.swirl(0x8A5CFF, 3f, 10, at.add(0, 0.8, 0), 6f); Vfx.disc(1.2f, 8, at.add(0, 0.8, 0)); }
            case WHITE -> { Vfx.flare(Vfx.WHITE, 3.5f, 6, at.add(0, 0.8, 0)); for (int i = 0; i < 6; i++) Vfx.streak(Vfx.WHITE, 2.5f, 8, at.add(0, 0.8 + Vfx.g(0.4), 0), Vfx.rf(0, 180)); Vfx.burst(at.add(0, 0.6, 0), Vfx.WHITE, 40, 1.0, 0.14f); }
        }
    }

    // ---------------------------------------------------------------- tick
    public static void tick(Minecraft mc) {
        ticks++;
        for (Iterator<Open> it = OPEN.values().iterator(); it.hasNext(); ) {
            Open o = it.next();
            o.pr = o.radius;
            if (o.life > 0 && --o.life == 0) o.closing = true;
            if (o.closing) o.radius -= 0.14f; else o.radius += (1.3f - o.radius) * 0.25f;
            if (o.closing && o.radius < 0.05f) it.remove();
        }

        for (Iterator<Map.Entry<Integer, Integer>> it = DIVERS.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<Integer, Integer> en = it.next();
            Entity e = mc.level.getEntity(en.getKey());
            if (e == null || !e.isAlive()) { it.remove(); continue; }
            PuddleColor c = PuddleColor.byId(en.getValue());
            double speed = e.position().distanceTo(new Vec3(e.xo, e.yo, e.zo));
            Vec3 feet = e.position().add(0, 0.08, 0);
            int n = 1 + (int) Math.min(8, speed * 14);
            for (int i = 0; i < n; i++) {                                         // ink flicked up behind you
                double a = Math.random() * Math.PI * 2;
                droplet(c, feet, new Vec3(Math.cos(a) * 0.12, 0.12 + Math.random() * 0.2, Math.sin(a) * 0.12), Vfx.rf(0.06f, 0.14f));
            }
            if (ticks % 3 == 0) Vfx.ring(glowOf(c), 0.55f, 9, feet, 1.15f);
            if (speed > 0.15) Vfx.smoke(c.rgb, 0.6f, 14, feet, Vec3.ZERO, false);
            switch (c) {
                case RED -> { if (Vfx.chance(0.3)) Vfx.ember(Vfx.RED, 0.1f, 12, feet, new Vec3(0, 0.08, 0)); }
                case BLUE -> { if (Vfx.chance(0.3)) Vfx.spark(Vfx.CYAN, 0.1f, 8, feet.add(Vfx.rnd(0.3)), Vfx.rnd(0.05)); }
                case GREEN -> { if (Vfx.chance(0.3)) Vfx.glow(0x6BFF8A, 0.3f, 16, feet.add(Vfx.rnd(0.3)), new Vec3(0, 0.06, 0)); }
                case BLACK -> { if (Vfx.chance(0.3)) Vfx.smoke(Vfx.BLACK, 1.1f, 20, feet.add(Vfx.rnd(0.3)), new Vec3(0, 0.05, 0), false); }
                case WHITE -> { if (Vfx.chance(0.3)) Vfx.flare(Vfx.WHITE, 0.3f, 4, feet.add(Vfx.rnd(0.3))); }
            }
        }

        LocalPlayer lp = mc.player;                                              // Splatoon-style wall climb: run into a wall to swim up it
        if (lp != null && isDiver(lp.getId())) {
            lp.fallDistance = 0;
            if (lp.horizontalCollision && lp.input.forwardImpulse > 0) {
                Vec3 dm = lp.getDeltaMovement();
                lp.setDeltaMovement(dm.x, 0.45, dm.z);
            }
        }
    }

    // ---------------------------------------------------------------- puddles under divers (flat on the ground, or on the wall ahead)
    public static void render(PoseStack ps, Camera cam, float pt) {
        if (DIVERS.isEmpty() && OPEN.isEmpty()) return;
        Minecraft mc = Minecraft.getInstance();
        MultiBufferSource.BufferSource bs = mc.renderBuffers().bufferSource();
        RenderType rt = RenderType.entityTranslucent(TEX);
        VertexConsumer vc = bs.getBuffer(rt);
        Vec3 cp = cam.getPosition();

        for (Map.Entry<Integer, Integer> en : DIVERS.entrySet()) {
            Entity e = mc.level.getEntity(en.getKey());
            if (e == null) continue;
            int rgb = PuddleColor.byId(en.getValue()).rgb;
            Vec3 pos = e.getPosition(pt);
            float r = 0.95f * (1f + 0.04f * (float) Math.sin((ticks + pt) * 0.4 + e.getId()));
            if (e.onGround()) flat(ps, vc, cp, pos.x, pos.y + 0.03, pos.z, r, rgb);
            else {
                Vec3 f = Vec3.directionFromRotation(0, e.getViewYRot(pt));
                wall(ps, vc, cp, pos.add(f.scale(0.5)).add(0, 0.55, 0), f, r, rgb);
            }
        }
        for (Open o : OPEN.values()) {
            float r = o.pr + (o.radius - o.pr) * pt;
            if (r > 0.05f) flat(ps, vc, cp, o.x, o.y + 0.03, o.z, r, PuddleColor.byId(o.color).rgb);
        }
        bs.endBatch(rt);
    }

    private static void flat(PoseStack ps, VertexConsumer vc, Vec3 cp, double x, double y, double z, float r, int rgb) {
        ps.pushPose();
        ps.translate(x - cp.x, y - cp.y, z - cp.z);
        Matrix4f m = ps.last().pose(); Matrix3f n = ps.last().normal();
        v(vc, m, n, -r, 0, -r, 0, 0, rgb, 0, 1, 0);
        v(vc, m, n, -r, 0, r, 0, 1, rgb, 0, 1, 0);
        v(vc, m, n, r, 0, r, 1, 1, rgb, 0, 1, 0);
        v(vc, m, n, r, 0, -r, 1, 0, rgb, 0, 1, 0);
        ps.popPose();
    }

    private static void wall(PoseStack ps, VertexConsumer vc, Vec3 cp, Vec3 c, Vec3 f, float r, int rgb) {
        ps.pushPose();
        ps.translate(c.x - cp.x, c.y - cp.y, c.z - cp.z);
        Matrix4f m = ps.last().pose(); Matrix3f n = ps.last().normal();
        float rx = (float) -f.z * r, rz = (float) f.x * r;                      // "right" along the wall
        float nx = (float) -f.x, nz = (float) -f.z;
        v(vc, m, n, -rx, -r, -rz, 0, 1, rgb, nx, 0, nz);
        v(vc, m, n, rx, -r, rz, 1, 1, rgb, nx, 0, nz);
        v(vc, m, n, rx, r, rz, 1, 0, rgb, nx, 0, nz);
        v(vc, m, n, -rx, r, -rz, 0, 0, rgb, nx, 0, nz);
        ps.popPose();
    }

    private static void v(VertexConsumer vc, Matrix4f m, Matrix3f n, float x, float y, float z, float u, float vv, int rgb, float nx, float ny, float nz) {
        vc.vertex(m, x, y, z).color((rgb >> 16) & 255, (rgb >> 8) & 255, rgb & 255, 235).uv(u, vv)
                .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(0xF000F0).normal(n, nx, ny, nz).endVertex();
    }

    // ---------------------------------------------------------------- HUD
    public static void hud(GuiGraphics g, int w, int h) {
        Minecraft mc = Minecraft.getInstance();
        Integer id = DIVERS.get(mc.player.getId());
        if (id == null) return;
        PuddleColor c = PuddleColor.byId(id);
        Hud.vignette(g, w, h, glowOf(c), 55);
        g.drawCenteredString(mc.font, "INK FORM  -  " + c.title, w / 2, h - 66, 0xFF000000 | glowOf(c));
        g.drawCenteredString(mc.font, "run into walls to climb  -  right-click the item to surface", w / 2, h - 54, 0xFFFFFFFF);
    }
}
