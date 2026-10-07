package com.example.railgun;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Cleave: right-click while a mob is touching you. 40 s cooldown. Nothing shows in hand on purpose. */
public class CleaveItem extends Item {
    public CleaveItem(Properties p) { super(p); }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide && player instanceof ServerPlayer sp) {
            if (Cleaves.isActive(sp.getUUID()) || !Cleaves.start(sp)) {
                sp.displayClientMessage(Component.literal("Cleave needs a mob touching you."), true);
                return InteractionResultHolder.fail(stack);
            }
        }
        player.getCooldowns().addCooldown(this, 800);
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }
}
