package com.example.railgun.client;

import com.example.railgun.FxOptions;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SpriteSet;

/** Picks the texture for the requested shape from the 14 textures listed in particles/fx.json. */
public record FxProvider(SpriteSet sprites) implements ParticleProvider<FxOptions> {
    public static final int SHAPES = 14;

    @Override
    public Particle createParticle(FxOptions o, ClientLevel lv, double x, double y, double z, double xd, double yd, double zd) {
        return new FxParticle(lv, x, y, z, xd, yd, zd, o, sprites.get(Math.min(o.shape, SHAPES - 1), SHAPES - 1));
    }
}
