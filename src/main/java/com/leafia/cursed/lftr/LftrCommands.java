package com.leafia.cursed.lftr;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.function.BiFunction;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.BlockHitResult;

/** /cursed msr ... : readouts and fuel loading for the LFTR until it gets a GUI. Look at the block first. */
public final class LftrCommands {
    private LftrCommands() {}

    public static LiteralArgumentBuilder<CommandSourceStack> node() {
        return Commands.literal("msr")
            .executes(c -> run(c, be -> be instanceof MsrElementBlockEntity e ? e.status()
                : be instanceof MsrControlBlockEntity r ? r.status() : null))
            .then(Commands.literal("fuel").then(Commands.argument("type", StringArgumentType.word())
                .then(Commands.argument("percent", IntegerArgumentType.integer(1, 100)).executes(c -> run(c, be -> {
                    if (!(be instanceof MsrElementBlockEntity e)) return null;
                    String type = StringArgumentType.getString(c, "type").toLowerCase();
                    if (MsrSim.Fuel.byName(type) == null) return "Unknown fuel. Use: th232, pa233, u233, u235, pa234.";
                    e.cell.amount = e.cell.capacity;
                    e.cell.mixture.clear();
                    e.cell.mixture.put(type, IntegerArgumentType.getInteger(c, "percent") / 100.0);
                    e.setChanged();
                    return "Filled with salt carrying " + IntegerArgumentType.getInteger(c, "percent") + "% " + type + " (debug fuel loading).";
                })))))
            .then(Commands.literal("empty").executes(c -> run(c, be -> {
                if (!(be instanceof MsrElementBlockEntity e)) return null;
                e.cell.amount = 0; e.cell.heat = 0; e.cell.mixture.clear(); e.setChanged();
                return "Element emptied.";
            })))
            .then(Commands.literal("control").then(Commands.argument("percent", IntegerArgumentType.integer(0, 100)).executes(c -> run(c, be -> {
                if (!(be instanceof MsrControlBlockEntity r)) return null;
                r.targetInsertion = IntegerArgumentType.getInteger(c, "percent") / 100.0 * Math.max(r.length, 1);
                r.setChanged();
                return "Control rod moving to " + IntegerArgumentType.getInteger(c, "percent") + "% of its length.";
            }))));
    }

    private static int run(CommandContext<CommandSourceStack> ctx, java.util.function.Function<BlockEntity, String> action) throws CommandSyntaxException {
        CommandSourceStack src = ctx.getSource();
        ServerPlayer p = src.getPlayerOrException();
        if (p.pick(8.0, 0.0f, false) instanceof BlockHitResult hit) {
            String msg = action.apply(p.level().getBlockEntity(hit.getBlockPos()));
            if (msg != null) { src.sendSuccess(() -> Component.literal(msg), false); return 1; }
        }
        src.sendFailure(Component.literal("Look at an LFTR element or control rod (within 8 blocks); use the matching command."));
        return 0;
    }
}
