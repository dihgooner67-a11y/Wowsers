package com.example.railgun;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Unlimited Black Hole: right-click to open a black hole 45 blocks ahead. 5 minute cooldown. */
public class BlackHoleItem extends Item {
    public BlackHoleItem(Properties p) { super(p); }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide && player instanceof ServerPlayer sp) {
            if (BlackHoles.isActive(sp.getUUID())) {
                sp.displayClientMessage(Component.literal("Your black hole is still open."), true);
                return InteractionResultHolder.fail(stack);
            }
            BlackHoles.spawn(sp);
        }
        player.getCooldowns().addCooldown(this, 6000);
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }
}
