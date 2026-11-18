package com.leafia.cursed.mixin;

import com.hbm.client.render.CoreRenderState;
import com.hbm.client.render.CoreRenderer;
import com.hbm.tileentity.machine.dfc.TileEntityCore;
import com.leafia.cursed.client.CrystalRenderer;
import com.leafia.cursed.client.ILookState;
import com.leafia.cursed.dfc.IDfcLook;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.golem.core.render.CameraInfo;
import dev.golem.core.render.SubmitSink;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Client only. Replaces Continuum's orb (and its meltdown flare) with a gem-shaped crystal (colour modes, cracks that
 * grow during an overload) unless the creative setting turned it off.
 */
@Mixin(value = CoreRenderer.class, remap = false)
public abstract class CoreRendererMixin {
    @Inject(method = "extract", at = @At("TAIL"))
    private void cursed$extract(TileEntityCore be, CoreRenderState state, float partialTicks, Vec3 cameraPosition, CallbackInfo ci) {
        IDfcLook look = (IDfcLook) be;
        ((ILookState) state).cursed$set(look.cursed$getColorMode(), look.cursed$getColorA(), look.cursed$getColorB(), look.cursed$isCrystal(), look.cursed$getCrack());
    }

    @Inject(method = "submit", at = @At("HEAD"), cancellable = true)
    private void cursed$submit(CoreRenderState s, PoseStack ps, SubmitSink col, CameraInfo camera, CallbackInfo ci) {
        ILookState look = (ILookState) s;
        if (!look.cursed$crystal()) return; // creative setting: back to Continuum's orb
        ps.pushPose();
        ps.translate(0.5, 0.5, 0.5);
        CrystalRenderer.draw(ps, col, look.cursed$mode(), look.cursed$colorA(), look.cursed$colorB(), s.color, s.fill, look.cursed$crack(), s.gameTime, s.heat == 0);
        ps.popPose();
        ci.cancel();
    }
}
