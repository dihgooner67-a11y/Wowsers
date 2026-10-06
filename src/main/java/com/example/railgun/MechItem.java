package com.example.railgun;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

/** Ultimate Mech Beam: 2s = wide flame beam, 15s = larger beam, 30s = ULTRA beam. */
public class MechItem extends Item {
    public static final int MIN_CHARGE = 40, TIER2 = 300, TIER3 = 600;

    public MechItem(Properties p) { super(p); }

    @Override public UseAnim getUseAnimation(ItemStack s) { return UseAnim.BOW; }
    @Override public int getUseDuration(ItemStack s) { return 72000; }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        player.startUsingItem(hand);
        if (level instanceof ServerLevel sw) {
            sw.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 2f, 0.5f);
            PhotonBridge.spawn(sw, "mech_charge", player.getEyePosition().add(player.getLookAngle().scale(1.8)), player.getLookAngle(), 1f);
        }
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public void onUseTick(Level level, LivingEntity user, ItemStack stack, int remaining) {
        if (!(level instanceof ServerLevel sw)) return;
        int used = getUseDuration(stack) - remaining;
        if (used > TIER3 + 200) return;
        if (used % 20 == 0)
            sw.playSound(null, user.getX(), user.getY(), user.getZ(), SoundEvents.BEACON_AMBIENT, SoundSource.PLAYERS, 2f,
                    0.5f + Math.min(used / (float) TIER3, 1f) * 1.5f);
        if (used == TIER2) {
            PhotonBridge.spawn(sw, "mech_tier2", user.position().add(0, 1, 0), user.getLookAngle(), 1f);
            sw.playSound(null, user.getX(), user.getY(), user.getZ(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 3f, 0.7f);
        } else if (used == TIER3) {
            PhotonBridge.spawn(sw, "mech_tier3", user.position().add(0, 1, 0), user.getLookAngle(), 1f);
            sw.playSound(null, user.getX(), user.getY(), user.getZ(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 3f, 0.5f);
            sw.playSound(null, user.getX(), user.getY(), user.getZ(), SoundEvents.ENDER_DRAGON_GROWL, SoundSource.PLAYERS, 4f, 0.8f);
        }
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity user, int timeLeft) {
        if (level.isClientSide || !(user instanceof ServerPlayer p)) return;
        int used = getUseDuration(stack) - timeLeft;
        if (used < MIN_CHARGE) return;
        int tier = used >= TIER3 ? 3 : used >= TIER2 ? 2 : 1;
        MechBeams.fire(p, tier);
        p.getCooldowns().addCooldown(this, 600 * (tier + 1));
    }
}
