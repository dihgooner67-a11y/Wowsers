package com.leafia.cursed.client;

import com.leafia.cursed.CursedMod;
import com.leafia.cursed.fuel.RodItems;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterColorHandlersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Tints the rod overlay layer (tint index 1) by fuel family. */
@Mod.EventBusSubscriber(modid = CursedMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class RodColors {
    private RodColors() {}

    @SubscribeEvent
    public static void register(RegisterColorHandlersEvent.Item e) {
        RodItems.BY_ID.forEach((id, obj) -> e.register((stack, tint) -> tint == 1 ? 0xFF000000 | RodItems.color(id) : -1, obj.get()));
    }
}
