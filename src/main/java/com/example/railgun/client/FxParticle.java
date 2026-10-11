package com.example.railgun.client;

import com.example.railgun.FxOptions;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureManager;

/** The one custom effect particle: tinted, optionally additive (glow), grows/fades/spins. */
public class FxParticle extends TextureSheetParticle {
    /** Additive blending = light adds up, which is what makes glows and blasts bloom. */
    public static final ParticleRenderType ADDITIVE = new ParticleRenderType() {
        @Override
        public void begin(BufferBuilder b, TextureManager tm) {
            RenderSystem.depthMask(false);
            RenderSystem.setShader(GameRenderer::getParticleShader);
            RenderSystem.setShaderTexture(0, TextureAtlas.LOCATION_PARTICLES);
            RenderSystem.enableBlend();
            RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
            b.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.PARTICLE);
        }
        @Override
        public void end(Tesselator t) {
            t.end();
            RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
            RenderSystem.depthMask(true);
        }
        @Override public String toString() { return "railgun:additive"; }
    };

    private final float grow, spin;
    private final boolean additive;

    FxParticle(ClientLevel lv, double x, double y, double z, double vx, double vy, double vz,
               FxOptions o, TextureAtlasSprite sprite) {
        super(lv, x, y, z);
        this.xd = vx; this.yd = vy; this.zd = vz;
        this.lifetime = Math.max(1, o.life);
        this.quadSize = o.size;
        this.rCol = ((o.rgb >> 16) & 255) / 255f;
        this.gCol = ((o.rgb >> 8) & 255) / 255f;
        this.bCol = (o.rgb & 255) / 255f;
        this.alpha = 0f;
        this.gravity = o.gravity;
        this.friction = o.drag;
        this.hasPhysics = false;
        this.roll = (float) Math.toRadians(o.roll);
        this.oRoll = this.roll;
        this.grow = o.grow;
        this.spin = (float) Math.toRadians(o.spin);
        this.additive = o.additive;
        this.setSprite(sprite);
    }

    @Override
    public void tick() {
        super.tick();
        this.oRoll = this.roll;
        this.roll += this.spin;
        this.quadSize *= this.grow;
        float t = (float) this.age / (float) this.lifetime;
        float fadeIn = Math.min(1f, this.age / 2f);
        this.alpha = fadeIn * (float) Math.pow(Math.max(0f, 1f - t), additive ? 1.4 : 1.1) * (additive ? 1f : 0.9f);
    }

    @Override public ParticleRenderType getRenderType() { return additive ? ADDITIVE : ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT; }

    @Override
    protected int getLightColor(float partialTick) { return additive ? 0xF000F0 : super.getLightColor(partialTick); }
}
