package com.leafia.cursed;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class CursedSounds {
    private CursedSounds() {}
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, CursedMod.MODID);
    public static final RegistryObject<SoundEvent> CRUCIFIX = reg("crucifix");
    public static final RegistryObject<SoundEvent> CRUCIFIX_FAIL = reg("crucifix_fail");
    private static RegistryObject<SoundEvent> reg(String n) {
        return SOUNDS.register(n, () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(CursedMod.MODID, n)));
    }
}
