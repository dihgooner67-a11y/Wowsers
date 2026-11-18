package com.leafia.cursed.mixin;

import com.hbm.client.render.TorexRenderState;
import com.hbm.client.render.TorexRenderer;
import com.hbm.entity.effect.EntityNukeTorex;
import com.leafia.cursed.client.NukeEffects;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.golem.core.render.CameraInfo;
import dev.golem.core.render.SubmitSink;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Client only: draws the plasma ball / balefire pillar / antimatter burst on top of Continuum's cloud. */
@Mixin(value = TorexRenderer.class, remap = false)
public abstract class TorexRendererMixin {
    @Inject(method = "submitUndeclared", at = @At("HEAD"))
    private void cursed$nukeEffects(TorexRenderState state, PoseStack ps, SubmitSink col, CameraInfo camera, CallbackInfo ci) {
        EntityNukeTorex torex = state.torex;
        if (torex == null || !torex.isClientAnimationReady()) return;
        float age = ((Entity) torex).tickCount + state.partialTicks;
        NukeEffects.draw(ps, col.order(3), torex.getTorexType(), torex.getScale(), age, torex.getMaxAge());
    }
}
