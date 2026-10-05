package com.example.railgun;

import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;

public class RailgunItem extends Item {
    public static final int CHARGE_TICKS = 20;
    public static final double RANGE = 128;
    public static final float DAMAGE = 40f;
    public static final int COOLDOWN = 40;
    public static final int MAX_PIERCE = 4;
    public static final float MAX_HARDNESS = 20f;

    public RailgunItem(Properties p) { super(p); }

    @Override public UseAnim getUseAnimation(ItemStack s) { return UseAnim.BOW; }
    @Override public int getUseDuration(ItemStack s) { return 72000; }

    private static boolean hasAmmo(Player p) {
        if (p.isCreative()) return true;
        for (int i = 0; i < p.getInventory().getContainerSize(); i++)
            if (p.getInventory().getItem(i).is(Items.IRON_INGOT)) return true;
        return false;
    }

    private static void consumeAmmo(Player p) {
        if (p.isCreative()) return;
        for (int i = 0; i < p.getInventory().getContainerSize(); i++) {
            ItemStack s = p.getInventory().getItem(i);
            if (s.is(Items.IRON_INGOT)) { s.shrink(1); return; }
        }
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!hasAmmo(player)) {
            if (!level.isClientSide)
                level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.DISPENSER_FAIL, SoundSource.PLAYERS, 1f, 1.4f);
            return InteractionResultHolder.fail(stack);
        }
        player.startUsingItem(hand);
        if (!level.isClientSide)
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.WARDEN_SONIC_CHARGE, SoundSource.PLAYERS, 1.2f, 1.9f);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity user, int timeLeft) {
        if (level.isClientSide || !(user instanceof ServerPlayer p)) return;
        int charged = getUseDuration(stack) - timeLeft;
        if (charged < CHARGE_TICKS || !hasAmmo(p)) return;
        fire(p);
        consumeAmmo(p);
        p.getCooldowns().addCooldown(this, COOLDOWN);
    }

    private void fire(ServerPlayer p) {
        ServerLevel sw = p.serverLevel();
        Vec3 eye = p.getEyePosition();
        Vec3 dir = p.getLookAngle();
        Vec3 far = eye.add(dir.scale(RANGE));
        boolean canBreak = p.getAbilities().mayBuild;

        Vec3 cur = eye, end = far;
        for (int i = 0; i <= MAX_PIERCE; i++) {
            BlockHitResult bhr = sw.clip(new ClipContext(cur, far, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p));
            if (bhr.getType() == HitResult.Type.MISS) { end = far; break; }
            end = bhr.getLocation();
            if (!canBreak) break;
            BlockPos bp = bhr.getBlockPos();
            float hardness = sw.getBlockState(bp).getDestroySpeed(sw, bp);
            if (hardness < 0 || hardness > MAX_HARDNESS) break;
            crater(sw, p, end.add(dir.scale(0.3)));
            if (i == MAX_PIERCE) break;
            cur = end.add(dir.scale(0.5));
        }

        boolean hit = false;
        AABB box = new AABB(eye, end).inflate(1.5);
        for (Entity e : sw.getEntities(p, box, x -> x instanceof LivingEntity le && le.isAlive())) {
            Optional<Vec3> r = e.getBoundingBox().inflate(0.4).clip(eye, end);
            if (r.isEmpty()) continue;
            e.hurt(sw.damageSources().playerAttack(p), DAMAGE);
            e.push(dir.x * 3.0, dir.y * 3.0 + 0.6, dir.z * 3.0);
            e.hurtMarked = true;
            e.setSecondsOnFire(4);
            hit = true;
        }

        Vec3 muzzle = eye.add(dir.scale(1.2)).add(0, -0.2, 0);
        Net.sendNear(sw, p.position(), 192, new FxPacket(FxPacket.RAIL, hit ? 1 : 0, p.getId(),
                muzzle.x, muzzle.y, muzzle.z, end.x, end.y, end.z));
        PhotonBridge.spawn(sw, "rail_muzzle", muzzle, dir, 1f);
        PhotonBridge.spawn(sw, "rail_impact", end, dir.scale(-1), 1f);

        play(sw, eye, SoundEvents.WARDEN_SONIC_BOOM, 3f, 1.3f);
        play(sw, eye, SoundEvents.LIGHTNING_BOLT_THUNDER, 2f, 2.0f);
        play(sw, end, SoundEvents.LIGHTNING_BOLT_IMPACT, 3f, 0.7f);
        play(sw, end, SoundEvents.WARDEN_SONIC_BOOM, 2f, 0.6f);

        p.push(-dir.x * 0.6, -dir.y * 0.3, -dir.z * 0.6);
        p.hurtMarked = true;
        p.connection.send(new ClientboundSetEntityMotionPacket(p));
    }

    private static void crater(ServerLevel sw, ServerPlayer p, Vec3 at) {
        BlockPos c = BlockPos.containing(at);
        for (int dx = -2; dx <= 2; dx++)
            for (int dy = -2; dy <= 2; dy++)
                for (int dz = -2; dz <= 2; dz++) {
                    if (dx * dx + dy * dy + dz * dz > 5) continue;
                    BlockPos q = c.offset(dx, dy, dz);
                    BlockState s = sw.getBlockState(q);
                    if (s.isAir()) continue;
                    float hd = s.getDestroySpeed(sw, q);
                    if (hd < 0 || hd > MAX_HARDNESS) continue;
                    sw.destroyBlock(q, true, p);
                }
    }

    private static void play(ServerLevel w, Vec3 pos, SoundEvent s, float vol, float pitch) {
        w.playSound(null, pos.x, pos.y, pos.z, s, SoundSource.PLAYERS, vol, pitch);
    }
}
