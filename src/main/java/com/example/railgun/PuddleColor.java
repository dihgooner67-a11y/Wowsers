package com.example.railgun;

/** The five puddle colours. speed = extra swim speed (x1+speed), regen = Regeneration amplifier while in ink form. */
public enum PuddleColor {
    RED(0, "Pure Hatred", 0xFF2020, 1.5, 1),
    BLUE(1, "Lapis Blue", 0x2E5BFF, 2.2, 1),
    GREEN(2, "Nature's Green", 0x2FD060, 1.5, 3),
    BLACK(3, "Void Black", 0x0C0C14, 1.8, 1),
    WHITE(4, "Divine White", 0xF4F4FF, 1.8, 2);

    public final int id, rgb, regen;
    public final String title;
    public final double speed;

    PuddleColor(int id, String title, int rgb, double speed, int regen) {
        this.id = id; this.title = title; this.rgb = rgb; this.speed = speed; this.regen = regen;
    }

    public static PuddleColor byId(int id) {
        for (PuddleColor c : values()) if (c.id == id) return c;
        return RED;
    }
}
