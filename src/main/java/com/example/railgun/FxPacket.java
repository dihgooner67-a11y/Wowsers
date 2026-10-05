package com.example.railgun;

import com.example.railgun.client.ClientFx;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** One generic server -> client effect packet. kind selects the power, arg/owner/vectors carry its data. */
public class FxPacket {
    public static final int RAIL = 1, DOMAIN = 2, GLOVE = 3, PURPLE = 4, MECH = 5, BLACKHOLE = 6, CURSED = 7;

    public final int kind, arg, owner;
    public final double x, y, z, dx, dy, dz;

    public FxPacket(int kind, int arg, int owner, double x, double y, double z, double dx, double dy, double dz) {
        this.kind = kind; this.arg = arg; this.owner = owner;
        this.x = x; this.y = y; this.z = z; this.dx = dx; this.dy = dy; this.dz = dz;
    }

    public void encode(FriendlyByteBuf b) {
        b.writeInt(kind); b.writeInt(arg); b.writeInt(owner);
        b.writeDouble(x); b.writeDouble(y); b.writeDouble(z);
        b.writeDouble(dx); b.writeDouble(dy); b.writeDouble(dz);
    }

    public static FxPacket decode(FriendlyByteBuf b) {
        return new FxPacket(b.readInt(), b.readInt(), b.readInt(), b.readDouble(), b.readDouble(), b.readDouble(),
                b.readDouble(), b.readDouble(), b.readDouble());
    }

    public void handle(Supplier<NetworkEvent.Context> sup) {
        NetworkEvent.Context ctx = sup.get();
        ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientFx.onPacket(this)));
        ctx.setPacketHandled(true);
    }
}
