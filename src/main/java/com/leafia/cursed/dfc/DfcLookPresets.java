package com.leafia.cursed.dfc;

import java.util.List;

/** Colour presets the DFC Color Tuner cycles through: plain colours, two-colour gradients, rainbow. */
public final class DfcLookPresets {
    private DfcLookPresets() {}

    public record Preset(String name, int mode, int a, int b) {}

    public static final List<Preset> ALL = List.of(
        new Preset("Default (fuel colour)", IDfcLook.MODE_DEFAULT, 0, 0),
        new Preset("Red", IDfcLook.MODE_SOLID, 0xFF3030, 0),
        new Preset("Orange", IDfcLook.MODE_SOLID, 0xFF9020, 0),
        new Preset("Gold", IDfcLook.MODE_SOLID, 0xFFD040, 0),
        new Preset("Green", IDfcLook.MODE_SOLID, 0x40E060, 0),
        new Preset("Cyan", IDfcLook.MODE_SOLID, 0x30E0E0, 0),
        new Preset("Blue", IDfcLook.MODE_SOLID, 0x4070FF, 0),
        new Preset("Purple", IDfcLook.MODE_SOLID, 0xA050FF, 0),
        new Preset("Pink", IDfcLook.MODE_SOLID, 0xFF60C0, 0),
        new Preset("White", IDfcLook.MODE_SOLID, 0xFFFFFF, 0),
        new Preset("Gradient: red to gold", IDfcLook.MODE_GRADIENT, 0xFF3030, 0xFFD040),
        new Preset("Gradient: cyan to blue", IDfcLook.MODE_GRADIENT, 0x30E0E0, 0x4070FF),
        new Preset("Gradient: pink to purple", IDfcLook.MODE_GRADIENT, 0xFF60C0, 0xA050FF),
        new Preset("Gradient: gold to green", IDfcLook.MODE_GRADIENT, 0xFFD040, 0x40E060),
        new Preset("Gradient: red to blue", IDfcLook.MODE_GRADIENT, 0xFF3030, 0x4070FF),
        new Preset("Rainbow", IDfcLook.MODE_RAINBOW, 0, 0));

    /** Index of the preset matching the current look, or -1 for a custom one. */
    public static int indexOf(IDfcLook look) {
        for (int i = 0; i < ALL.size(); i++) {
            Preset p = ALL.get(i);
            if (p.mode() != look.cursed$getColorMode()) continue;
            if (p.mode() == IDfcLook.MODE_SOLID && p.a() != look.cursed$getColorA()) continue;
            if (p.mode() == IDfcLook.MODE_GRADIENT && (p.a() != look.cursed$getColorA() || p.b() != look.cursed$getColorB())) continue;
            return i;
        }
        return -1;
    }
}
