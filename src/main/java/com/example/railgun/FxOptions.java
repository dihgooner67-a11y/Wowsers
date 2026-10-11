package com.example.railgun;

import com.mojang.brigadier.StringReader;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.FriendlyByteBuf;

/** Parameters of one custom effect particle (shape, colour, size, motion). Spawned client-side by Vfx. */
public class FxOptions implements ParticleOptions {
    public final int shape, rgb, life;
    public final float size, drag, gravity, grow, spin, roll;
    public final boolean additive;

    public FxOptions(int shape, int rgb, float size, int life, float drag, float gravity, float grow,
                     float spin, float roll, boolean additive) {
        this.shape = shape; this.rgb = rgb; this.size = size; this.life = life; this.drag = drag;
        this.gravity = gravity; this.grow = grow; this.spin = spin; this.roll = roll; this.additive = additive;
    }

    public static final Codec<FxOptions> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.INT.fieldOf("shape").forGetter(o -> o.shape),
            Codec.INT.fieldOf("rgb").forGetter(o -> o.rgb),
            Codec.FLOAT.fieldOf("size").forGetter(o -> o.size),
            Codec.INT.fieldOf("life").forGetter(o -> o.life),
            Codec.FLOAT.fieldOf("drag").forGetter(o -> o.drag),
            Codec.FLOAT.fieldOf("gravity").forGetter(o -> o.gravity),
            Codec.FLOAT.fieldOf("grow").forGetter(o -> o.grow),
            Codec.FLOAT.fieldOf("spin").forGetter(o -> o.spin),
            Codec.FLOAT.fieldOf("roll").forGetter(o -> o.roll),
            Codec.BOOL.fieldOf("additive").forGetter(o -> o.additive)
    ).apply(i, FxOptions::new));

    public static final ParticleOptions.Deserializer<FxOptions> DESERIALIZER = new ParticleOptions.Deserializer<>() {
        @Override
        public FxOptions fromCommand(ParticleType<FxOptions> type, StringReader reader) {
            return new FxOptions(0, 0xFFFFFF, 0.5f, 20, 0.9f, 0f, 1f, 0f, 0f, true);
        }
        @Override
        public FxOptions fromNetwork(ParticleType<FxOptions> type, FriendlyByteBuf b) {
            return new FxOptions(b.readInt(), b.readInt(), b.readFloat(), b.readInt(), b.readFloat(),
                    b.readFloat(), b.readFloat(), b.readFloat(), b.readFloat(), b.readBoolean());
        }
    };

    @Override public ParticleType<?> getType() { return RailgunMod.FX.get(); }

    @Override
    public void writeToNetwork(FriendlyByteBuf b) {
        b.writeInt(shape); b.writeInt(rgb); b.writeFloat(size); b.writeInt(life); b.writeFloat(drag);
        b.writeFloat(gravity); b.writeFloat(grow); b.writeFloat(spin); b.writeFloat(roll); b.writeBoolean(additive);
    }

    @Override public String writeToString() { return "railgun:fx"; }
}
