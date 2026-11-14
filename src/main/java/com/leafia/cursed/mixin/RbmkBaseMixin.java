package com.leafia.cursed.mixin;

import com.hbm.tileentity.machine.rbmk.BlockEntityRBMKBase;
import com.hbm.tileentity.machine.rbmk.RBMKDials;
import com.leafia.cursed.rbmk.IRbmkDamage;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * CINDER-D: columns no longer melt down the instant they pass max heat. Overheating builds damage
 * (addon rule); the column melts down once damage exceeds 1200 and heals 1 per tick when cool.
 */
@Mixin(value = BlockEntityRBMKBase.class, remap = false)
public abstract class RbmkBaseMixin implements IRbmkDamage {
    @Shadow public double heat;
    @Shadow public abstract double maxHeat();
    @Shadow public abstract void meltdown();

    @Unique private int cursed$damage;
    @Unique private double cursed$lastHeat = 20.0;

    @Inject(method = "tickServer", at = @At("HEAD"))
    private void cursed$damageTick(CallbackInfo ci) {
        Level level = ((BlockEntity) (Object) this).getLevel();
        if (level == null || level.isClientSide) return;
        if (heat > maxHeat() && !RBMKDials.getMeltdownsDisabled(level)) {
            if (heat >= cursed$lastHeat || heat >= 2000.0)
                cursed$damage += 2 + (int) (Math.max(0.0, heat - 3000.0) / 2000.0);
        } else {
            cursed$damage = Math.max(cursed$damage - 1, 0);
        }
        if (cursed$damage > 1200) meltdown();
        cursed$lastHeat = heat;
        cursed$jets(level);
    }

    /** Damaged columns vent smoke (and flame when badly hurt). Stand-in for the addon's jumping-column jets. */
    @Unique private void cursed$jets(Level level) {
        if (cursed$damage <= 0 || !(level instanceof ServerLevel sl)) return;
        if (level.random.nextInt(Math.max(1, (1200 - cursed$damage) / 3 + 5)) != 0) return;
        BlockPos p = ((BlockEntity) (Object) this).getBlockPos();
        double x = p.getX() + 0.5, y = p.getY() + RBMKDials.getColumnHeight(level) + 0.2, z = p.getZ() + 0.5;
        sl.sendParticles(ParticleTypes.LARGE_SMOKE, x, y, z, 6, 0.2, 0.1, 0.2, 0.02);
        if (cursed$damage > 600) sl.sendParticles(ParticleTypes.FLAME, x, y, z, 4, 0.15, 0.1, 0.15, 0.05);
    }

    @Inject(method = "saveData", at = @At("TAIL"))
    private void cursed$save(DataOut out, CallbackInfo ci) { out.putInt("cursedDamage", cursed$damage); }

    @Inject(method = "loadData", at = @At("TAIL"))
    private void cursed$load(DataIn in, CallbackInfo ci) { cursed$damage = in.getIntOr("cursedDamage", 0); }

    @Override public int cursed$getDamage() { return cursed$damage; }
    @Override public String cursed$status() {
        return String.format("RBMK column: heat %.0f / max %.0f, damage %d / 1200", heat, maxHeat(), cursed$damage);
    }
}
