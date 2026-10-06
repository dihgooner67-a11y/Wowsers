package com.example.railgun;

import net.minecraft.world.phys.Vec3;

/** All pocket dimensions. radius/height in blocks; "5 chunks wide" = 80 blocks across (radius 40). */
public enum DomainType {
    //        id  R   H  sphere build hold restore tp   ty tx tz   yaw
    POCKET(   0, 10, 10, true,   40, 160, 24, false, 0, 0,   0,   0f),
    SHRINE(   1, 40, 24, false,  50, 260, 40, true,  1, 0, -12,   0f),
    VOID(     2, 40, 24, false,  50, 300, 40, false, 0, 0,   0,   0f),
    HOMETOWN( 3, 24, 16, false,  40, 300, 30, true,  2, 0,   3, 180f);

    public static final Vec3 BLACK_HOLE = new Vec3(19, 9, 0);

    public final int id, radius, height, buildTicks, holdTicks, restoreTicks, ty, tx, tz;
    public final boolean sphere, teleport;
    public final float yaw;

    DomainType(int id, int radius, int height, boolean sphere, int build, int hold, int restore,
               boolean teleport, int ty, int tx, int tz, float yaw) {
        this.id = id; this.radius = radius; this.height = height; this.sphere = sphere;
        this.buildTicks = build; this.holdTicks = hold; this.restoreTicks = restore;
        this.teleport = teleport; this.ty = ty; this.tx = tx; this.tz = tz; this.yaw = yaw;
    }

    public static DomainType byId(int id) {
        for (DomainType t : values()) if (t.id == id) return t;
        return POCKET;
    }
}
