package com.example.railgun;

import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = RailgunMod.MOD_ID)
public final class ServerEvents {
    private ServerEvents() {}

    @SubscribeEvent
    public static void tick(TickEvent.ServerTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        Domains.tick(e.getServer());
        PurpleBeams.tick();
        MechBeams.tick();
        BlackHoles.tick();
    }

    @SubscribeEvent
    public static void stopping(ServerStoppingEvent e) {
        Domains.restoreAll();
        PurpleBeams.clear();
        MechBeams.clear();
        BlackHoles.clear();
    }
}
