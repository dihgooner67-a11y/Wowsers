package com.example.railgun;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Cursed Fists. Hold in either hand and punch. Right-click = Cursed Surge (30 s of 95% black flashes + more damage). */
public class CursedFistsItem extends Item {
    public static final int SURGE_TICKS = 600, SURGE_COOLDOWN = 1800;

    public CursedFistsItem(Properties p) { super(p); }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level instanceof ServerLevel sw && player instanceof ServerPlayer sp) {
            CursedEvents.startSurge(sp);
            sw.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.WITHER_SPAWN, SoundSource.PLAYERS, 2f, 1.6f);
            sw.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 3f, 0.8f);
        }
        player.getCooldowns().addCooldown(this, SURGE_COOLDOWN);
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }
}
