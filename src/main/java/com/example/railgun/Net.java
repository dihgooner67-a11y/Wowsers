package com.example.railgun;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public final class Net {
    private static final String VERSION = "1";
    public static final SimpleChannel CH = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(RailgunMod.MOD_ID, "main"), () -> VERSION, VERSION::equals, VERSION::equals);

    private Net() {}

    public static void init() {
        CH.registerMessage(0, FxPacket.class, FxPacket::encode, FxPacket::decode, FxPacket::handle);
    }

    public static void sendNear(ServerLevel level, Vec3 at, double radius, FxPacket p) {
        CH.send(PacketDistributor.NEAR.with(() ->
                new PacketDistributor.TargetPoint(at.x, at.y, at.z, radius, level.dimension())), p);
    }
}
