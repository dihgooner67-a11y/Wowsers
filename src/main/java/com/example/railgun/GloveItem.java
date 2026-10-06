package com.example.railgun;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/** Azure Flame Glove: hold it and punch. Blue-flame hits, 10% chance of a Black Flash. */
public class GloveItem extends Item {
    public static final float DAMAGE = 10f, BLACK_DAMAGE = 40f, BLACK_CHANCE = 0.10f;

    public GloveItem(Properties p) { super(p); }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (!(attacker.level() instanceof ServerLevel sw)) return true;

        boolean black = attacker.getRandom().nextFloat() < BLACK_CHANCE;
        Vec3 look = attacker.getLookAngle();
        Vec3 flat = new Vec3(look.x, 0, look.z).normalize();

        DamageSource src = attacker instanceof Player pl ? sw.damageSources().playerAttack(pl) : sw.damageSources().mobAttack(attacker);
        target.hurt(src, black ? BLACK_DAMAGE : DAMAGE);

        double kb = black ? 3.8 : 1.1;
        target.push(flat.x * kb, black ? 1.0 : 0.3, flat.z * kb);
        target.hurtMarked = true;
        target.setSecondsOnFire(black ? 6 : 3);

        Vec3 at = target.position().add(0, target.getBbHeight() * 0.6, 0);
        Net.sendNear(sw, at, 96, new FxPacket(FxPacket.GLOVE, black ? 1 : 0, attacker.getId(), at.x, at.y, at.z, flat.x, flat.y, flat.z));

        if (black) PhotonBridge.spawn(sw, "black_flash", at, flat, 1f);
        if (black) {
            play(sw, at, SoundEvents.LIGHTNING_BOLT_THUNDER, 3f, 1.4f);
            play(sw, at, SoundEvents.WARDEN_SONIC_BOOM, 2f, 1.1f);
            play(sw, at, SoundEvents.LIGHTNING_BOLT_IMPACT, 3f, 0.8f);
        } else {
            play(sw, at, SoundEvents.FIRECHARGE_USE, 1.5f, 0.8f);
            play(sw, at, SoundEvents.BLAZE_SHOOT, 1f, 1.3f);
        }
        return true;
    }

    private static void play(ServerLevel w, Vec3 p, SoundEvent s, float vol, float pitch) {
        w.playSound(null, p.x, p.y, p.z, s, SoundSource.PLAYERS, vol, pitch);
    }
}
