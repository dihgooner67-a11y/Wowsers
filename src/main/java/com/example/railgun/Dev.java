package com.example.railgun;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;

/** Build switches. The dev build sets NO_COOLDOWNS = true. */
public final class Dev {
    /** true = every item cooldown, the Cursed Realm 30 s lock and the Cursed Fists swing cooldown are disabled. */
    public static final boolean NO_COOLDOWNS = true;

    private Dev() {}

    /** Use this instead of player.getCooldowns().addCooldown(...). */
    public static void cooldown(Player p, Item item, int ticks) {
        if (!NO_COOLDOWNS) p.getCooldowns().addCooldown(item, ticks);
    }
}
