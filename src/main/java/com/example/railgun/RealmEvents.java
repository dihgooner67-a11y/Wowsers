package com.example.railgun;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Keeps every mob out of the Cursed Realm and cleans up realm sessions. */
@Mod.EventBusSubscriber(modid = RailgunMod.MOD_ID)
public final class RealmEvents {
    private RealmEvents() {}

    @SubscribeEvent
    public static void join(EntityJoinLevelEvent e) {
        if (e.getLevel() instanceof ServerLevel sl && sl.dimension().equals(CursedRealm.KEY) && e.getEntity() instanceof Mob)
            e.setCanceled(true);
    }

    @SubscribeEvent
    public static void logout(PlayerEvent.PlayerLoggedOutEvent e) {
        if (e.getEntity() instanceof ServerPlayer sp) CursedRealm.drop(sp);
    }

    @SubscribeEvent
    public static void respawn(PlayerEvent.PlayerRespawnEvent e) {
        if (e.getEntity() instanceof ServerPlayer sp) CursedRealm.drop(sp);
    }

    @SubscribeEvent
    public static void death(LivingDeathEvent e) {
        if (e.getEntity() instanceof ServerPlayer sp) CursedRealm.drop(sp);
    }
}
