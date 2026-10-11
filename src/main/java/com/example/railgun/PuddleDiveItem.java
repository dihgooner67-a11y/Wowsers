package com.example.railgun;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Puddle Dive. Sneak + right-click cycles the ink colour (red / blue / green / black / white).
 * Right-click drops you into a puddle of that colour; right-click again to surface.
 */
public class PuddleDiveItem extends Item {
    public PuddleDiveItem(Properties p) { super(p); }

    public static PuddleColor colorOf(ItemStack s) {
        return PuddleColor.byId(s.hasTag() ? s.getTag().getInt("color") : 0);
    }

    @Override
    public Component getName(ItemStack stack) {
        PuddleColor c = colorOf(stack);
        int rgb = c == PuddleColor.BLACK ? 0x8A5CFF : c.rgb;           // pure black text would be unreadable
        return Component.literal("Puddle Dive: " + c.title).withStyle(Style.EMPTY.withColor(TextColor.fromRgb(rgb)));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.isShiftKeyDown()) {                                    // cycle the colour
            PuddleColor next = PuddleColor.byId((colorOf(stack).id + 1) % PuddleColor.values().length);
            stack.getOrCreateTag().putInt("color", next.id);
            if (!level.isClientSide) player.displayClientMessage(Component.literal("Ink: " + next.title)
                    .withStyle(Style.EMPTY.withColor(TextColor.fromRgb(next == PuddleColor.BLACK ? 0x8A5CFF : next.rgb))), true);
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
        }
        if (!level.isClientSide && player instanceof ServerPlayer sp) {
            if (!Dive.toggle(sp, colorOf(stack))) return InteractionResultHolder.fail(stack);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }
}
