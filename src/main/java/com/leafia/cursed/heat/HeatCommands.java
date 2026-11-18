package com.leafia.cursed.heat;

import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.BlockHitResult;

/** /cursed boiler [fluid <NAME> [steps]] : HP Boiler controls until it gets a GUI. Look at the boiler. */
public final class HeatCommands {
    private HeatCommands() {}

    public static LiteralArgumentBuilder<CommandSourceStack> node() {
        return Commands.literal("boiler")
            .executes(c -> run(c.getSource(), (be, n, s) -> be.status()))
            .then(Commands.literal("fluid").then(Commands.argument("name", StringArgumentType.word())
                .executes(c -> run(c.getSource(), (be, n, s) -> set(be, StringArgumentType.getString(c, "name"), be.compression)))
                .then(Commands.argument("steps", IntegerArgumentType.integer(1, 4)).executes(c -> run(c.getSource(),
                    (be, n, s) -> set(be, StringArgumentType.getString(c, "name"), IntegerArgumentType.getInteger(c, "steps")))))));
    }

    /** /cursed heatex [hot <NAME> | coolant <NAME> | cycle <amount> [delay]] : Coolant Heat Exchanger controls. */
    public static LiteralArgumentBuilder<CommandSourceStack> heatex() {
        return Commands.literal("heatex")
            .executes(c -> runHx(c.getSource(), be -> be.status()))
            .then(Commands.literal("hot").then(Commands.argument("name", StringArgumentType.word()).executes(c ->
                runHx(c.getSource(), be -> fluid(StringArgumentType.getString(c, "name"), be::setHotFluid)))))
            .then(Commands.literal("coolant").then(Commands.argument("name", StringArgumentType.word()).executes(c ->
                runHx(c.getSource(), be -> fluid(StringArgumentType.getString(c, "name"), be::setCoolant)))))
            .then(Commands.literal("cycle").then(Commands.argument("amount", IntegerArgumentType.integer(1, 2400000))
                .executes(c -> runHx(c.getSource(), be -> { be.amountToCool = IntegerArgumentType.getInteger(c, "amount"); return "Cycle size set to " + be.amountToCool + "."; }))
                .then(Commands.argument("delay", IntegerArgumentType.integer(1, 200)).executes(c -> runHx(c.getSource(), be -> {
                    be.amountToCool = IntegerArgumentType.getInteger(c, "amount");
                    be.tickDelay = IntegerArgumentType.getInteger(c, "delay");
                    return "Cycle size " + be.amountToCool + " every " + be.tickDelay + " tick(s).";
                })))));
    }

    private static String fluid(String name, java.util.function.Function<FluidType, String> action) {
        FluidType t = Fluids.fromName(name.toUpperCase());
        return t == Fluids.NONE ? "Unknown fluid '" + name + "'." : action.apply(t);
    }

    private static int runHx(CommandSourceStack src, java.util.function.Function<CoolantHeatexBlockEntity, String> action) throws CommandSyntaxException {
        ServerPlayer p = src.getPlayerOrException();
        if (p.pick(8.0, 0.0f, false) instanceof BlockHitResult hit && p.level().getBlockEntity(hit.getBlockPos()) instanceof CoolantHeatexBlockEntity be) {
            String msg = action.apply(be);
            be.setChanged();
            src.sendSuccess(() -> Component.literal(msg), false);
            return 1;
        }
        src.sendFailure(Component.literal("Look at a Coolant Heat Exchanger (within 8 blocks)."));
        return 0;
    }

    private static String set(HpBoilerBlockEntity be, String name, int steps) {
        FluidType t = Fluids.fromName(name.toUpperCase());
        if (t == Fluids.NONE) return "Unknown fluid '" + name + "'.";
        return be.setFluid(t, steps);
    }

    private interface Action { String apply(HpBoilerBlockEntity be, Object unused, Object unused2); }

    private static int run(CommandSourceStack src, Action action) throws CommandSyntaxException {
        ServerPlayer p = src.getPlayerOrException();
        if (p.pick(8.0, 0.0f, false) instanceof BlockHitResult hit && p.level().getBlockEntity(hit.getBlockPos()) instanceof HpBoilerBlockEntity be) {
            String msg = action.apply(be, null, null);
            be.setChanged();
            src.sendSuccess(() -> Component.literal(msg), false);
            return 1;
        }
        src.sendFailure(Component.literal("Look at an HP Boiler (within 8 blocks)."));
        return 0;
    }
}
