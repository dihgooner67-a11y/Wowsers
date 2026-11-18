package com.leafia.cursed.dfc;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.function.Function;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.BlockHitResult;

/** /cursed dfc ... : crystal on/off and exact colours (creative mode or operator). Look at the DFC core. */
public final class DfcCommands {
    private DfcCommands() {}

    public static LiteralArgumentBuilder<CommandSourceStack> node() {
        return Commands.literal("dfc")
            .executes(c -> run(c.getSource(), l -> describe(l)))
            .then(Commands.literal("crystal").then(Commands.argument("on", BoolArgumentType.bool()).executes(c -> {
                boolean on = BoolArgumentType.getBool(c, "on");
                return run(c.getSource(), l -> { l.cursed$setCrystal(on); return "DFC shown as " + (on ? "a crystal." : "Continuum's orb."); });
            })))
            .then(Commands.literal("color")
                .then(Commands.literal("default").executes(c -> run(c.getSource(), l -> set(l, IDfcLook.MODE_DEFAULT, 0, 0, "Default (fuel colour)"))))
                .then(Commands.literal("rainbow").executes(c -> run(c.getSource(), l -> set(l, IDfcLook.MODE_RAINBOW, 0, 0, "Rainbow"))))
                .then(Commands.literal("solid").then(Commands.argument("hex", StringArgumentType.word()).executes(c ->
                    run(c.getSource(), l -> {
                        int a = hex(StringArgumentType.getString(c, "hex"));
                        return a < 0 ? "Give a colour like #FF8800." : set(l, IDfcLook.MODE_SOLID, a, 0, String.format("Solid #%06X", a));
                    }))))
                .then(Commands.literal("gradient").then(Commands.argument("top", StringArgumentType.word())
                    .then(Commands.argument("bottom", StringArgumentType.word()).executes(c ->
                        run(c.getSource(), l -> {
                            int a = hex(StringArgumentType.getString(c, "top")), b = hex(StringArgumentType.getString(c, "bottom"));
                            return a < 0 || b < 0 ? "Give two colours like #FF8800 #0088FF." : set(l, IDfcLook.MODE_GRADIENT, a, b, String.format("Gradient #%06X to #%06X", a, b));
                        }))))));
    }

    private static String set(IDfcLook l, int mode, int a, int b, String label) {
        l.cursed$setLook(mode, a, b);
        return "DFC colour: " + label;
    }

    private static String describe(IDfcLook l) {
        return String.format("DFC look: %s, colours #%06X / #%06X, crystal %s, crack %.0f%%",
            new String[]{"default", "solid", "gradient", "rainbow"}[Math.max(0, Math.min(3, l.cursed$getColorMode()))],
            l.cursed$getColorA(), l.cursed$getColorB(), l.cursed$isCrystal() ? "on" : "off", l.cursed$getCrack() * 100.0f);
    }

    private static int hex(String s) {
        try {
            String t = s.startsWith("#") ? s.substring(1) : s;
            return t.length() == 6 ? Integer.parseInt(t, 16) : -1;
        } catch (NumberFormatException e) { return -1; }
    }

    private static int run(CommandSourceStack src, Function<IDfcLook, String> action) throws CommandSyntaxException {
        ServerPlayer p = src.getPlayerOrException();
        if (!(p.isCreative() || src.hasPermission(2))) {
            src.sendFailure(Component.literal("Changing the DFC look needs creative mode or operator permission."));
            return 0;
        }
        if (p.pick(8.0, 0.0f, false) instanceof BlockHitResult hit && p.level().getBlockEntity(hit.getBlockPos()) instanceof IDfcLook look) {
            String msg = action.apply(look);
            src.sendSuccess(() -> Component.literal(msg), false);
            return 1;
        }
        src.sendFailure(Component.literal("Look at a DFC core (within 8 blocks)."));
        return 0;
    }
}
