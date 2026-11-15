package com.leafia.cursed.apr;

import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;

/** /cursed apr ... : the APR's control surface until it gets a GUI. Look at the APR core block (within 8 blocks). */
public final class AprCommands {
    private AprCommands() {}

    public static LiteralArgumentBuilder<CommandSourceStack> node() {
        return Commands.literal("apr")
            .executes(c -> run(c, (be, lvl) -> be.status()))
            .then(Commands.literal("coolants").executes(c -> run(c, (be, lvl) -> {
                StringBuilder sb = new StringBuilder("Usable fluids (hot -> cooled): ");
                int n = 0;
                for (FluidType t : Fluids.getAll()) {
                    if (!AprFluids.isValid(t)) continue;
                    if (n++ > 0) sb.append(", ");
                    sb.append(t.getName()).append("->").append(AprFluids.conversion(t).getName());
                }
                return n == 0 ? "No coolable fluids found." : sb.toString();
            })))
            .then(Commands.literal("structure").executes(c -> run(c, (be, lvl) -> {
                String p = be.checkStructure(lvl);
                return p == null ? "Structure is complete. Run /cursed apr assemble." : p;
            })))
            .then(Commands.literal("chambers").then(Commands.argument("radii", StringArgumentType.greedyString()).executes(c -> run(c, (be, lvl) -> {
                List<Integer> list = new ArrayList<>();
                try {
                    for (String s : StringArgumentType.getString(c, "radii").trim().split("\\s+")) list.add(Integer.parseInt(s));
                } catch (NumberFormatException e) { return "Give chamber radii as numbers, e.g. /cursed apr chambers 6 10"; }
                return be.setChambers(list);
            }))))
            .then(Commands.literal("assemble").executes(c -> run(c, (be, lvl) -> be.assemble(lvl))))
            .then(Commands.literal("disassemble").executes(c -> run(c, (be, lvl) -> be.disassemble(lvl))))
            .then(Commands.literal("coolant").then(Commands.argument("fluid", StringArgumentType.word())
                .executes(c -> run(c, (be, lvl) -> be.setCoolant(StringArgumentType.getString(c, "fluid"))))))
            .then(Commands.literal("fluid").then(Commands.argument("chamber", IntegerArgumentType.integer(1, 16))
                .then(Commands.argument("fluid", StringArgumentType.word()).executes(c -> run(c, (be, lvl) ->
                    be.setChamberFluid(IntegerArgumentType.getInteger(c, "chamber"), StringArgumentType.getString(c, "fluid")))))))
            .then(Commands.literal("control").then(Commands.argument("percent", IntegerArgumentType.integer(0, 100)).executes(c -> run(c, (be, lvl) -> {
                be.sim.targetControl = IntegerArgumentType.getInteger(c, "percent");
                return "Control rods moving to " + (int) be.sim.targetControl + "%.";
            }))))
            .then(Commands.literal("particles").then(Commands.argument("amount", IntegerArgumentType.integer(0)).executes(c -> run(c, (be, lvl) -> {
                be.sim.particles = Math.min(IntegerArgumentType.getInteger(c, "amount"), be.sim.particleCap);
                return "Particles set to " + (int) be.sim.particles + " / " + be.sim.particleCap + " (debug command).";
            }))));
    }

    private static int run(CommandContext<CommandSourceStack> ctx, BiFunction<AprCoreBlockEntity, Level, String> action) throws CommandSyntaxException {
        CommandSourceStack src = ctx.getSource();
        ServerPlayer p = src.getPlayerOrException();
        if (p.pick(8.0, 0.0f, false) instanceof BlockHitResult hit && p.level().getBlockEntity(hit.getBlockPos()) instanceof AprCoreBlockEntity be) {
            String msg = action.apply(be, p.level());
            src.sendSuccess(() -> Component.literal(msg), false);
            return 1;
        }
        src.sendFailure(Component.literal("Look at an APR core block (within 8 blocks)."));
        return 0;
    }
}
