package com.example.railgun;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

/** Apple Sauce: a bowl of food. Eating it gives the bowl back. */
public class AppleSauceItem extends Item {
    public AppleSauceItem(Properties p) { super(p); }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        ItemStack result = super.finishUsingItem(stack, level, entity);
        if (!(entity instanceof Player p) || p.getAbilities().instabuild) return result;
        ItemStack bowl = new ItemStack(Items.BOWL);
        if (result.isEmpty()) return bowl;
        if (!p.getInventory().add(bowl)) p.drop(bowl, false);
        return result;
    }
}
