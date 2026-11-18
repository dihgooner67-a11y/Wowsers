package com.leafia.cursed.wind;

import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

/** /cursed wind : shows the global wind. Operators: /cursed wind set <power 0-100> [angle]. */
public final class WindCommands {
    private WindCommands() {}

    public static LiteralArgumentBuilder<CommandSourceStack> node() {
        return Commands.literal("wind")
            .executes(c -> {
                WindSim w = WindState.SIM;
                c.getSource().sendSuccess(() -> Component.literal(String.format("Wind: power %.1f (%s), direction %.0f deg", w.power,
                    w.strongWind ? "strong spell" : "calm spell", w.angle % 360.0)), false);
                return 1;
            })
            .then(Commands.literal("set").requires(s -> s.hasPermission(2))
                .then(Commands.argument("power", DoubleArgumentType.doubleArg(0.0, 100.0)).executes(c -> setWind(c.getSource(), DoubleArgumentType.getDouble(c, "power"), null))
                    .then(Commands.argument("angle", DoubleArgumentType.doubleArg(-360.0, 720.0)).executes(c ->
                        setWind(c.getSource(), DoubleArgumentType.getDouble(c, "power"), DoubleArgumentType.getDouble(c, "angle"))))));
    }

    private static int setWind(CommandSourceStack src, double power, Double angle) {
        WindSim w = WindState.SIM;
        w.power = w.startPower = w.endPower = power;
        if (angle != null) w.angle = w.lastAngle = w.startAngle = w.endAngle = angle;
        src.sendSuccess(() -> Component.literal("Wind set. It will drift on from here."), true);
        return 1;
    }
}
