package com.example.railgun;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Rules while in ink form: no punching or block breaking, damage halved (Void Black takes none), clean up on exit. */
@Mod.EventBusSubscriber(modid = RailgunMod.MOD_ID)
public final class DiveEvents {
    private DiveEvents() {}

    @SubscribeEvent
    public static void attack(AttackEntityEvent e) {
        if (Dive.isDiving(e.getEntity().getUUID())) e.setCanceled(true);
    }

    @SubscribeEvent
    public static void breakBlock(PlayerInteractEvent.LeftClickBlock e) {
        if (Dive.isDiving(e.getEntity().getUUID())) e.setCanceled(true);
    }

    @SubscribeEvent
    public static void hurt(LivingHurtEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || !Dive.isDiving(p.getUUID())) return;
        if (Dive.colorOf(p.getUUID()) == PuddleColor.BLACK) e.setCanceled(true);
        else e.setAmount(e.getAmount() * 0.5f);
    }

    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent e) { if (e.getEntity() instanceof ServerPlayer p) Dive.drop(p); }
    @SubscribeEvent public static void respawn(PlayerEvent.PlayerRespawnEvent e) { if (e.getEntity() instanceof ServerPlayer p) Dive.drop(p); }
    @SubscribeEvent public static void dim(PlayerEvent.PlayerChangedDimensionEvent e) { if (e.getEntity() instanceof ServerPlayer p) Dive.drop(p); }
    @SubscribeEvent public static void death(LivingDeathEvent e) { if (e.getEntity() instanceof ServerPlayer p) Dive.drop(p); }
}
