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

/** Hollow Purple: hold 2s for the sphere, hold 20s for the 200% beam. */
public class PurpleItem extends Item {
    public static final int CHARGE_TICKS = 40, BEAM_TICKS = 400;

    public PurpleItem(Properties p) { super(p); }

    @Override public UseAnim getUseAnimation(ItemStack s) { return UseAnim.BOW; }
    @Override public int getUseDuration(ItemStack s) { return 72000; }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        player.startUsingItem(hand);
        if (!level.isClientSide)
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 2f, 0.6f);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public void onUseTick(Level level, LivingEntity user, ItemStack stack, int remaining) {
        if (!(level instanceof ServerLevel sw)) return;
        int used = getUseDuration(stack) - remaining;
        if (used > BEAM_TICKS + 100) return;
        if (used >= CHARGE_TICKS && used % 20 == 0)
            sw.playSound(null, user.getX(), user.getY(), user.getZ(), SoundEvents.WARDEN_HEARTBEAT, SoundSource.PLAYERS, 3f,
                    0.5f + Math.min(used / (float) BEAM_TICKS, 1f) * 1.2f);
        if (used == CHARGE_TICKS)
            PhotonBridge.spawn(sw, "purple_charge", user.getEyePosition().add(user.getLookAngle().scale(3)), user.getLookAngle(), 1f);
        if (used == BEAM_TICKS) {
            sw.playSound(null, user.getX(), user.getY(), user.getZ(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 4f, 0.5f);
            sw.playSound(null, user.getX(), user.getY(), user.getZ(), SoundEvents.ENDER_DRAGON_GROWL, SoundSource.PLAYERS, 5f, 0.7f);
            PhotonBridge.spawn(sw, "purple_full", user.getEyePosition().add(user.getLookAngle().scale(3)), user.getLookAngle(), 2f);
        }
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity user, int timeLeft) {
        if (level.isClientSide || !(user instanceof ServerPlayer p)) return;
        int used = getUseDuration(stack) - timeLeft;
        if (used >= BEAM_TICKS) {
            PurpleBeams.fireBeam(p);
            p.getCooldowns().addCooldown(this, 2400);
        } else if (used >= CHARGE_TICKS) {
            PurpleBeams.launch(p);
            p.getCooldowns().addCooldown(this, 600);
        }
    }
}
