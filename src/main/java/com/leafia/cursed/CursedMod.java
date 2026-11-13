package com.leafia.cursed;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import com.leafia.cursed.item.LCELens;

/** Entry point. */
@Mod(CursedMod.MODID)
public class CursedMod {
    public static final String MODID = "cursed";

    public CursedMod() {
        var modBus = FMLJavaModLoadingContext.get().getModEventBus();
        CursedItems.ITEMS.register(modBus);
        modBus.addListener(this::creativeTabs);
        MinecraftForge.EVENT_BUS.register(this);
    }

    private void creativeTabs(BuildCreativeModeTabContentsEvent e) {
        if (e.getTabKey() != CreativeModeTabs.TOOLS_AND_UTILITIES) return;
        CursedItems.ITEMS.getEntries().forEach(o -> e.accept(o.get()));
    }

    @SubscribeEvent
    public void tooltips(ItemTooltipEvent e) {
        ItemStack s = e.getItemStack();
        if (s.getItem() instanceof LCELens l) {
            e.getToolTip().add(Component.literal("\u00a78Field modifier: " + pct(l.fieldMod)));
            e.getToolTip().add(Component.literal("\u00a78Power drain modifier: " + pct(l.drainMod)));
            e.getToolTip().add(Component.literal("\u00a78Energy modifier: " + pct(l.energyMod)));
        } else if (s.getItem() == CursedItems.FIX_TOOL.get()) {
            e.getToolTip().add(Component.literal("\u00a77Throw this into a collapsing DFC to fix it"));
        } else if (s.getItem() == CursedItems.CRUCIFIX.get()) {
            e.getToolTip().add(Component.literal("\u00a77It's a crucifix."));
            e.getToolTip().add(Component.literal("\u00a77Throw this into a collapsing DFC to shut it down."));
        }
    }

    private static String pct(float v) { return Math.round(v * 100.0f) + "%"; }
}
