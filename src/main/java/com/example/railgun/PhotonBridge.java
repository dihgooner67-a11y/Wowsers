package com.example.railgun;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

import java.util.Locale;

/**
 * Optional bridge to the Photon VFX mod. Plays assets/railgun/fx/<effect>.fx through "/photon fx <id> block ...".
 * Does nothing when Photon isn't installed. The mod's own built-in particle effects always play regardless.
 */
public final class PhotonBridge {
    public static final float YAW_SIGN = 1f, PITCH_SIGN = 1f;
    private PhotonBridge() {}

    public static boolean available(MinecraftServer s) {
        var photon = s.getCommands().getDispatcher().getRoot().getChild("photon");
        return photon != null && photon.getChild("fx") != null;
    }

    public static void spawn(ServerLevel w, String effect, Vec3 pos, Vec3 dir, float scale) {
        MinecraftServer s = w.getServer();
        if (s == null || !available(s)) return;
        Vec3 d = dir.lengthSqr() < 1e-6 ? new Vec3(0, 0, 1) : dir.normalize();
        float yaw = (float) Math.toDegrees(Math.atan2(-d.x, d.z));
        float pitch = (float) -Math.toDegrees(Math.asin(Math.max(-1, Math.min(1, d.y))));
        BlockPos bp = BlockPos.containing(pos);
        Vec3 off = pos.subtract(Vec3.atCenterOf(bp));
        CommandSourceStack src = s.createCommandSourceStack().withLevel(w).withSuppressedOutput().withPermission(4);
        String cmd = String.format(Locale.ROOT,
                "photon fx %s:%s block %d %d %d %.3f %.3f %.3f %.2f %.2f %.2f %.3f %.3f %.3f 0 false true false",
                RailgunMod.MOD_ID, effect, bp.getX(), bp.getY(), bp.getZ(), off.x, off.y, off.z,
                PITCH_SIGN * pitch, YAW_SIGN * yaw, 0f, scale, scale, scale);
        s.getCommands().performPrefixedCommand(src, cmd);
    }
}
