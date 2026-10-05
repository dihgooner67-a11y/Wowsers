package com.example.railgun;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class DomainItem extends Item {
    private final DomainType type;

    public DomainItem(Properties p, DomainType type) { super(p); this.type = type; }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide && player instanceof ServerPlayer sp) {
            if (!Domains.canStart(sp, type)) {
                sp.displayClientMessage(Component.literal("A domain is already active nearby."), true);
                return InteractionResultHolder.fail(stack);
            }
            Domains.start(sp, type);
        }
        player.getCooldowns().addCooldown(this, type.radius >= 24 ? 3600 : 2400);
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }
}
