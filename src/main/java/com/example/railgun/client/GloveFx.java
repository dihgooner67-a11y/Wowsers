package com.example.railgun.client;

import com.example.railgun.FxPacket;
import com.example.railgun.RailgunMod;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/** Azure flame punches (blue fire, smoke, shockwave) and the Black Flash (black + red lightning, dark smoke). */
public final class GloveFx {
    private GloveFx() {}

    public static void onPacket(FxPacket p, Minecraft mc) {
        Vec3 at = new Vec3(p.x, p.y, p.z);
        Vec3 dir = new Vec3(p.dx, p.dy, p.dz);
        boolean bf = p.arg == 1;
        if (bf) blackFlash(at, dir); else flamePunch(at, dir);

        if (mc.player.getId() == p.owner) {
            if (bf) { Impact.start(Impact.BLACK, false); Impact.addShake(16f); }
            else { Impact.startSoft(4); Impact.addShake(3f); }
        } else {
            double d = mc.player.position().distanceTo(at);
            Impact.addShake((float) ((bf ? 6 : 1.5) / (1 + d / 10)));
        }
    }

    private static void flamePunch(Vec3 a, Vec3 dir) {
        Vfx.core(Vfx.WHITE, 1.2f, 5, a);
        Vfx.glow(Vfx.CYAN, 2.4f, 9, a, Vec3.ZERO);
        Vfx.shock(Vfx.BLUE, 0.8f, 12, a, 1.2f);
        Vfx.ring(Vfx.CYAN, 0.7f, 14, a, 1.15f);
        for (int i = 0; i < 26; i++) Vfx.flame(i % 3 == 0 ? Vfx.CYAN : Vfx.BLUE, Vfx.rf(0.35f, 0.7f), 14 + (int) Vfx.rf(0, 8), a, Vfx.unit().scale(0.25).add(0, 0.05, 0));
        for (int i = 0; i < 24; i++) {                                      // expanding ring of fire
            double ang = i / 24.0 * Math.PI * 2;
            Vfx.flame(Vfx.CYAN, 0.45f, 12, a, new Vec3(Math.cos(ang) * 0.35, 0.03, Math.sin(ang) * 0.35));
        }
        for (int i = 0; i < 10; i++) Vfx.smoke(Vfx.DEEP, 0.7f, 28, a, Vfx.rnd(0.06).add(0, 0.05, 0), true);
        Vfx.burst(a, Vfx.CYAN, 22, 0.8, 0.14f);
        Vfx.electric(a, 0.7, Vfx.CYAN, 3);
    }

    private static void blackFlash(Vec3 a, Vec3 dir) {
        Vfx.core(Vfx.WHITE, 3f, 4, a);
        Vfx.glow(Vfx.RED, 5.5f, 9, a, Vec3.ZERO);
        Vfx.shock(Vfx.RED, 1.2f, 14, a, 1.24f);
        Vfx.shock(Vfx.CRIMSON, 0.8f, 18, a, 1.2f);
        for (int i = 0; i < 26; i++) Vfx.smoke(Vfx.BLACK, Vfx.rf(1.2f, 2.4f), 34, a.add(Vfx.rnd(0.4)), Vfx.unit().scale(0.3), false);   // black smoke
        for (int i = 0; i < 10; i++) Vfx.smoke(Vfx.CRIMSON, 1.4f, 22, a, Vfx.unit().scale(0.25), true);
        for (int i = 0; i < 12; i++) {                                      // black lightning: dark bolt under a red glow
            Vec3 d = Vfx.unit(), pos = a;
            for (int s = 0; s < 14; s++) {
                d = d.add(Vfx.rnd(0.5)).normalize();
                pos = pos.add(d.scale(0.8));
                Vfx.glow(Vfx.RED, 0.45f, 7, pos, Vec3.ZERO);
                Vfx.boltDark(0.9f, 8, pos);
                if (s % 2 == 0) Vfx.bolt(Vfx.RED, 0.8f, 5, pos);
            }
        }
        Vfx.burst(a, Vfx.RED, 70, 1.4, 0.18f);
        Vfx.electric(a, 1.5, Vfx.RED, 12);
    }

    public static void tick(Minecraft mc) {
        for (Player p : mc.level.players()) {
            if (p.position().distanceToSqr(mc.player.position()) > 48 * 48) continue;
            boolean main = p.getMainHandItem().is(RailgunMod.FLAME_GLOVE.get());
            boolean off = p.getOffhandItem().is(RailgunMod.FLAME_GLOVE.get());
            if (!main && !off) continue;
            HumanoidArm arm = main ? p.getMainArm() : p.getMainArm().getOpposite();
            Vec3 fwd = Vec3.directionFromRotation(0, p.getYRot());
            Vec3 right = new Vec3(-fwd.z, 0, fwd.x);
            Vec3 hand = p.position().add(0, 1.0, 0).add(fwd.scale(0.5)).add(right.scale(arm == HumanoidArm.RIGHT ? 0.4 : -0.4));
            for (int i = 0; i < 2; i++)
                Vfx.flame(i == 0 ? Vfx.CYAN : Vfx.BLUE, 0.28f, 12, hand.add(Vfx.rnd(0.06)), new Vec3(0, 0.04, 0));
            if (Vfx.chance(0.35)) Vfx.glow(Vfx.CYAN, 0.5f, 4, hand, Vec3.ZERO);
            if (Vfx.chance(0.15)) Vfx.electric(hand, 0.18, Vfx.CYAN, 1);
            PostFx.want = Math.max(PostFx.want, 0.35f);
        }
    }
}
