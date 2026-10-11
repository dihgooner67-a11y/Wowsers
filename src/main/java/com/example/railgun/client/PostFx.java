package com.example.railgun.client;

import com.example.railgun.RailgunMod;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import com.mojang.blaze3d.pipeline.RenderTarget;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.client.renderer.PostPass;
import net.minecraft.resources.ResourceLocation;

/**
 * Screen-space filter (assets/railgun/shaders): recolours the shading of whatever you are looking at into
 * the impact-frame palette, and adds bloom/glow to bright blasts. If the shader fails to load the mod
 * silently falls back to flat colour frames.
 */
public final class PostFx {
    private static PostChain chain;
    private static PostPass pass;
    private static int cw = -1, ch = -1;
    private static boolean failed;

    private static float style, amount, invert, impactBloom, bloom;
    private static float lensX, lensY, lensStr, lensR;
    /** Effects raise this every tick while a blast is visible; smoothed into the actual bloom strength. */
    public static float want;

    private PostFx() {}

    public static boolean ok() { return !failed; }

    public static void set(int st, float amt, float inv, float bl) { style = st; amount = amt; invert = inv; impactBloom = bl; }
    public static void clear() { amount = 0; impactBloom = 0; }
    /** Gravitational lens centre (uv), strength and horizon radius (vertical-uv units). strength 0 = off. */
    public static void setLens(float x, float y, float str, float r) { lensX = x; lensY = y; lensStr = str; lensR = r; }

    public static void render(float partialTick) {
        bloom += (Math.max(want, impactBloom) - bloom) * 0.25f;
        if (failed || (amount <= 0.002f && bloom <= 0.02f && lensStr <= 0.01f)) return;
        Minecraft mc = Minecraft.getInstance();
        try {
            int w = mc.getWindow().getWidth(), h = mc.getWindow().getHeight();
            if (chain == null) {
                // the json only declares the "swap" target; the two passes are added here so we can reach their uniforms
                chain = new PostChain(mc.getTextureManager(), mc.getResourceManager(), mc.getMainRenderTarget(),
                        new ResourceLocation(RailgunMod.MOD_ID, "shaders/post/impact.json"));
                RenderTarget main = mc.getMainRenderTarget();
                RenderTarget swap = chain.getTempTarget("swap");
                pass = chain.addPass("railgun:impact", main, swap);
                chain.addPass("blit", swap, main);
                cw = -1;
            }
            if (w != cw || h != ch) { chain.resize(w, h); cw = w; ch = h; }
            u("Style", style);
            u("Amount", amount);
            u("Invert", invert);
            u("Bloom", Math.min(bloom, 2.0f));
            u("LensX", lensX); u("LensY", lensY); u("LensStr", lensStr); u("LensR", lensR);
            u("Time", (mc.level != null ? mc.level.getGameTime() : 0) % 1000 + partialTick);
            RenderSystem.disableBlend();
            RenderSystem.disableDepthTest();
            RenderSystem.resetTextureMatrix();
            chain.process(partialTick);
            mc.getMainRenderTarget().bindWrite(false);
        } catch (Exception ex) {
            failed = true;
            chain = null;
            pass = null;
            System.err.println("[railgun] impact shader failed, using flat impact frames: " + ex);
        }
    }

    private static void u(String name, float value) {
        if (pass != null) pass.getEffect().safeGetUniform(name).set(value);
    }
}
