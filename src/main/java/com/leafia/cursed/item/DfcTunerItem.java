package com.leafia.cursed.item;

import com.leafia.cursed.dfc.DfcLookPresets;
import com.leafia.cursed.dfc.IDfcLook;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Creative tool. Right-click a DFC core to step through colour presets (plain colours, two-colour gradients, rainbow);
 * sneak + right-click steps backwards. Needs creative mode. Exact colours: /cursed dfc color ...
 */
public class DfcTunerItem extends Item {
    public DfcTunerItem() { super(new Item.Properties().stacksTo(1)); }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Player player = ctx.getPlayer();
        BlockEntity be = ctx.getLevel().getBlockEntity(ctx.getClickedPos());
        if (player == null || !(be instanceof IDfcLook look)) return InteractionResult.PASS;
        if (ctx.getLevel().isClientSide) return InteractionResult.SUCCESS;
        if (!player.getAbilities().instabuild) {
            player.displayClientMessage(Component.literal("The DFC Color Tuner only works in creative mode."), true);
            return InteractionResult.CONSUME;
        }
        int n = DfcLookPresets.ALL.size();
        int cur = DfcLookPresets.indexOf(look);
        int next = player.isShiftKeyDown() ? (cur <= 0 ? n - 1 : cur - 1) : (cur + 1) % n;
        DfcLookPresets.Preset p = DfcLookPresets.ALL.get(next);
        look.cursed$setLook(p.mode(), p.a(), p.b());
        player.displayClientMessage(Component.literal("DFC colour: " + p.name()), true);
        return InteractionResult.CONSUME;
    }
}
