package com.leafia.cursed;

import com.leafia.cursed.fuel.RodItems;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public final class CursedTabs {
    private CursedTabs() {}
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, CursedMod.MODID);

    public static final RegistryObject<CreativeModeTab> MAIN = TABS.register("main", () -> CreativeModeTab.builder()
        .title(Component.translatable("itemGroup.cursed"))
        .icon(() -> new ItemStack(CursedItems.FOCUS_OMEGA.get()))
        .displayItems((params, out) -> {
            CursedItems.ITEMS.getEntries().forEach(o -> out.accept(o.get()));
            RodItems.ITEMS.getEntries().forEach(o -> out.accept(o.get()));
            com.leafia.cursed.apr.AprBlocks.ITEMS.getEntries().forEach(o -> out.accept(o.get()));
            com.leafia.cursed.lftr.LftrBlocks.ITEMS.getEntries().forEach(o -> out.accept(o.get()));
        }).build());
}
