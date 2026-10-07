package com.example.railgun.client;

import com.example.railgun.RailgunMod;
import net.minecraft.client.player.Input;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.MovementInputUpdateEvent;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = RailgunMod.MOD_ID, value = Dist.CLIENT)
public final class ClientEvents {
    private static final VanillaGuiOverlay[] HIDE = {VanillaGuiOverlay.HOTBAR, VanillaGuiOverlay.CROSSHAIR,
            VanillaGuiOverlay.PLAYER_HEALTH, VanillaGuiOverlay.FOOD_LEVEL, VanillaGuiOverlay.ARMOR_LEVEL,
            VanillaGuiOverlay.EXPERIENCE_BAR, VanillaGuiOverlay.ITEM_NAME};

    private ClientEvents() {}

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent e) {
        if (e.phase == TickEvent.Phase.END) ClientFx.tick();
    }

    /** After the world (and every particle) is drawn: run the impact filter, lens and bloom on what you see. */
    @SubscribeEvent
    public static void stage(RenderLevelStageEvent e) {
        if (e.getStage() == RenderLevelStageEvent.Stage.AFTER_LEVEL) ClientFx.frame(e.getPartialTick(), e.getCamera());
    }

    @SubscribeEvent
    public static void fov(ViewportEvent.ComputeFov e) {
        if (!e.usedConfiguredFov()) return;
        float bonus = BlackHoleFx.fovBonus();
        if (bonus != 0f) e.setFOV(e.getFOV() + bonus);
        BlackHoleFx.lastFov = e.getFOV();
    }

    /** The player can't walk around during the black hole cutscene. */
    @SubscribeEvent
    public static void move(MovementInputUpdateEvent e) {
        if (!BlackHoleFx.cutsceneActive()) return;
        Input in = e.getInput();
        in.leftImpulse = 0;
        in.forwardImpulse = 0;
        in.jumping = false;
        in.shiftKeyDown = false;
    }

    @SubscribeEvent
    public static void overlay(RenderGuiOverlayEvent.Pre e) {
        if (!BlackHoleFx.cutsceneActive()) return;
        ResourceLocation id = e.getOverlay().id();
        for (VanillaGuiOverlay o : HIDE) if (id.equals(o.id())) { e.setCanceled(true); return; }
    }
}
