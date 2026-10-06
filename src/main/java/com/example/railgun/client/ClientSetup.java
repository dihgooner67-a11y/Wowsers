package com.example.railgun.client;

import com.example.railgun.RailgunMod;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid = RailgunMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientSetup {
    private ClientSetup() {}

    @SubscribeEvent
    public static void setup(FMLClientSetupEvent e) {
        e.enqueueWork(() -> {
            ItemProperties.register(RailgunMod.RAILGUN.get(), new ResourceLocation(RailgunMod.MOD_ID, "charge"),
                    (stack, level, entity, seed) -> {
                        if (entity == null || !entity.isUsingItem() || !entity.getUseItem().is(RailgunMod.RAILGUN.get())) return 0f;
                        return Math.min(entity.getTicksUsingItem() / 20f, 1f);
                    });
            ItemProperties.register(RailgunMod.MECH_BEAM.get(), new ResourceLocation(RailgunMod.MOD_ID, "mech_charge"),
                    (stack, level, entity, seed) -> {
                        if (entity == null || !entity.isUsingItem() || !entity.getUseItem().is(RailgunMod.MECH_BEAM.get())) return 0f;
                        return Math.min(entity.getTicksUsingItem() / 600f, 1f);
                    });
            ItemProperties.register(RailgunMod.FLAME_GLOVE.get(), new ResourceLocation(RailgunMod.MOD_ID, "flicker"),
                    (stack, level, entity, seed) -> ((System.currentTimeMillis() / 90L) % 3) / 3f);
        });
    }

    @SubscribeEvent
    public static void overlays(RegisterGuiOverlaysEvent e) {
        e.registerAboveAll("railgun_fx", (gui, g, pt, w, h) -> ClientFx.hud(g, pt, w, h));
    }

    @SubscribeEvent
    public static void particles(RegisterParticleProvidersEvent e) {
        e.registerSpriteSet(RailgunMod.FX.get(), FxProvider::new);
    }
}
