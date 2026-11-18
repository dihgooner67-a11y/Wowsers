package com.leafia.cursed.mixin;

import com.hbm.entity.effect.EntityNukeTorex;
import com.leafia.cursed.nuke.NukeTimings;
import java.util.ArrayList;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Shapes Continuum's mushroom cloud by yield, hides it behind the plasma ball at the start, and lets the burnt-out
 * grey cloud drift on the wind. Everything is deterministic (no new random numbers), so Continuum's cached/replayed
 * animations stay in sync between players.
 */
@Mixin(value = EntityNukeTorex.class, remap = false)
public abstract class TorexMixin {
    @Shadow public ArrayList<EntityNukeTorex.Cloudlet> cloudlets;
    @Shadow public double coreHeight;
    @Shadow public abstract double getScale();
    @Shadow public abstract int getMaxAge();
    @Shadow public abstract byte getTorexType();

    @Inject(method = "simulateClientTick", at = @At("TAIL"))
    private void cursed$shapeAndDrift(boolean playEffects, CallbackInfo ci) {
        Entity self = (Entity) (Object) this;
        double scale = getScale();
        int tick = self.tickCount;
        int type = getTorexType();

        if (type == NukeTimings.TYPE_ANTIMATTER) { cloudlets.clear(); return; } // no mushroom at all

        // ---- yield: small bombs lose the shock rings, big ones get a thicker cloud ----
        boolean small = scale < NukeTimings.SMALL_SCALE;
        double thick = NukeTimings.thickness(scale) * (small ? 0.75 : 1.0);
        if (small) {
            cloudlets.removeIf(c -> c.age == 1 && (c.type == EntityNukeTorex.TorexType.SHOCK || c.type == EntityNukeTorex.TorexType.RING
                || c.type == EntityNukeTorex.TorexType.CONDENSATION || c.type == EntityNukeTorex.TorexType.STARFISH));
        }
        if (thick != 1.0) {
            for (EntityNukeTorex.Cloudlet c : cloudlets) {
                if (c.age != 1 || c.type == EntityNukeTorex.TorexType.SHOCK || c.type == EntityNukeTorex.TorexType.CONDENSATION) continue;
                c.startingScale *= (float) thick;
                c.growingScale *= (float) thick;
            }
        }

        // ---- drift: once the fire has burnt out (Continuum freezes the cloud at half life) it is carried by the wind ----
        int maxAge = getMaxAge();
        double start = maxAge * 0.5, full = maxAge * 0.72;
        if (tick <= start) return;
        double k = Math.min(1.0, (tick - start) / (full - start));
        k = k * k * (3.0 - 2.0 * k);
        double baseAngle = Math.abs(Math.sin(self.getX() * 0.0137 + self.getZ() * 0.0291)) * Math.PI * 2.0;
        double windAngle = baseAngle + 0.0015 * (tick - start);
        double wx = Math.cos(windAngle), wz = Math.sin(windAngle);
        double speed = 0.035 * k;
        double baseY = self.getY();
        double cx = self.getX(), cz = self.getZ();
        double lifeLeft = Math.max(0.0, 1.0 - (tick - start) / Math.max(1.0, maxAge - start));
        for (EntityNukeTorex.Cloudlet c : cloudlets) {
            if (c.type == EntityNukeTorex.TorexType.SHOCK) continue;
            double h = Math.max(0.0, Math.min(2.0, (c.posY - baseY) / Math.max(coreHeight, 1.0)));
            double shear = 1.0 + 0.6 * h;                              // higher air moves faster
            double phase = c.angle * 7.1 + tick * 0.011;
            double turbX = Math.sin(phase) * 0.012 + Math.sin(phase * 2.3 + 1.7) * 0.006;
            double turbZ = Math.cos(phase * 1.3) * 0.012 + Math.cos(phase * 2.9 + 0.4) * 0.006;
            c.posX += (wx * speed * shear + turbX * k);
            c.posZ += (wz * speed * shear + turbZ * k);
            c.posY += (0.004 * lifeLeft + Math.sin(phase * 0.7) * 0.003) * k;
            double dx = c.posX - cx, dz = c.posZ - cz;
            double dist = Math.sqrt(dx * dx + dz * dz) + 1.0E-3;
            c.posX += dx / dist * 0.004 * k;                            // slow spreading
            c.posZ += dz / dist * 0.004 * k;
        }
    }

    @Inject(method = "getAlpha", at = @At("RETURN"), cancellable = true)
    private void cursed$alpha(CallbackInfoReturnable<Float> cir) {
        Entity self = (Entity) (Object) this;
        int type = getTorexType();
        float a = cir.getReturnValue() * NukeTimings.cloudReveal(getScale(), self.tickCount, type);
        if (type == NukeTimings.TYPE_BALEFIRE) a *= 0.45f;           // balefire is a pillar of fire, the cloud is only a haze
        cir.setReturnValue(a);
    }
}
