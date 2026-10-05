package com.example.railgun.client;

import com.example.railgun.FxPacket;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

/** Routes server packets to each power's client effects and drives their per-tick / per-frame work. */
public final class ClientFx {
    private ClientFx() {}

    public static void onPacket(FxPacket p) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;
        switch (p.kind) {
            case FxPacket.RAIL -> RailFx.onPacket(p, mc);
            case FxPacket.DOMAIN -> DomainFx.onPacket(p, mc);
            case FxPacket.GLOVE -> GloveFx.onPacket(p, mc);
            case FxPacket.PURPLE -> PurpleFx.onPacket(p, mc);
            case FxPacket.MECH -> MechFx.onPacket(p, mc);
            case FxPacket.BLACKHOLE -> BlackHoleFx.onPacket(p, mc);
            case FxPacket.CURSED -> CursedFx.onPacket(p, mc);
            default -> { }
        }
    }

    public static void tick() {
        Minecraft mc = Minecraft.getInstance();
        PostFx.want = 0;
        if (mc.level == null || mc.player == null) { Impact.tick(mc); PostFx.clear(); BlackHoleFx.reset(mc); return; }
        if (mc.isPaused()) return;
        Impact.tick(mc);
        RailFx.tick(mc);
        DomainFx.tick(mc);
        GloveFx.tick(mc);
        PurpleFx.tick(mc);
        MechFx.tick(mc);
        BlackHoleFx.tick(mc);
        CursedFx.tick(mc);
    }

    public static void frame(float pt, Camera camera) {
        BlackHoleFx.frame(pt, camera);
        Impact.frame(pt);
        PostFx.render(pt);
    }

    public static void hud(GuiGraphics g, float pt, int w, int h) {
        Impact.render(g, w, h);
        BlackHoleFx.hud(g, w, h);
        if (Impact.active()) return;
        RailFx.hud(g, w, h);
        DomainFx.hud(g, w, h);
        PurpleFx.hud(g, w, h);
        MechFx.hud(g, w, h);
        CursedFx.hud(g, w, h);
    }
}
