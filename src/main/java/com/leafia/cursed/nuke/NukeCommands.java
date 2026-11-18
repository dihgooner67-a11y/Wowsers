package com.leafia.cursed.nuke;

import com.hbm.entity.effect.EntityNukeTorex;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

/** /cursed nuke <radius> [standard|balefire|rainbow|antimatter] : spawns just the cloud effect at your feet (operators). */
public final class NukeCommands {
    private NukeCommands() {}

    public static LiteralArgumentBuilder<CommandSourceStack> node() {
        return Commands.literal("nuke").requires(s -> s.hasPermission(2))
            .then(Commands.argument("radius", IntegerArgumentType.integer(10, 2000))
                .executes(c -> spawn(c.getSource(), IntegerArgumentType.getInteger(c, "radius"), "standard"))
                .then(Commands.argument("kind", StringArgumentType.word()).executes(c ->
                    spawn(c.getSource(), IntegerArgumentType.getInteger(c, "radius"), StringArgumentType.getString(c, "kind")))));
    }

    private static int spawn(CommandSourceStack src, int radius, String kind) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        int type = switch (kind.toLowerCase()) {
            case "balefire" -> NukeTimings.TYPE_BALEFIRE;
            case "rainbow" -> NukeTimings.TYPE_RAINBOW;
            case "antimatter" -> NukeTimings.TYPE_ANTIMATTER;
            default -> NukeTimings.TYPE_STANDARD;
        };
        var p = src.getPlayerOrException();
        float scale = EntityNukeTorex.scaleFor(radius);
        EntityNukeTorex torex = EntityNukeTorex.statFacExact(p.level(), p.getX(), p.getY(), p.getZ(), scale, type);
        p.level().addFreshEntity(torex);
        src.sendSuccess(() -> Component.literal(String.format("Cloud effect spawned: radius %d -> scale %.2f, type %s (%s).", radius, scale, kind,
            scale < NukeTimings.SMALL_SCALE ? "small: no shock rings" : scale > NukeTimings.THICK_SCALE ? "large: thick cloud" : "medium")), false);
        return 1;
    }
}
