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

/** FUGA: Open Flame. Hold to draw an arrow of flame in your hand (1 s), release to fire. Hold 3 s for a full draw. */
public class FugaItem extends Item {
    public static final int MIN_DRAW = 20, FULL_DRAW = 60, COOLDOWN = 600;

    public FugaItem(Properties p) { super(p); }

    @Override public UseAnim getUseAnimation(ItemStack s) { return UseAnim.BOW; }
    @Override public int getUseDuration(ItemStack s) { return 72000; }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        player.startUsingItem(hand);
        if (!level.isClientSide)
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 2f, 0.5f);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public void onUseTick(Level level, LivingEntity user, ItemStack stack, int remaining) {
        if (!(level instanceof ServerLevel sw)) return;
        int used = getUseDuration(stack) - remaining;
        if (used == FULL_DRAW) {
            sw.playSound(null, user.getX(), user.getY(), user.getZ(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 3f, 0.6f);
            sw.playSound(null, user.getX(), user.getY(), user.getZ(), SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 3f, 0.8f);
            PhotonBridge.spawn(sw, "fuga_full", user.getEyePosition().add(user.getLookAngle()), user.getLookAngle(), 1f);
        }
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity user, int timeLeft) {
        if (level.isClientSide || !(user instanceof ServerPlayer p)) return;
        int used = getUseDuration(stack) - timeLeft;
        if (used < MIN_DRAW) return;
        Fuga.launch(p, used >= FULL_DRAW);
        p.getCooldowns().addCooldown(this, COOLDOWN);
    }
}
