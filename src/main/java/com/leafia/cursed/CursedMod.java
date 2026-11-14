package com.leafia.cursed;

import com.leafia.cursed.dfc.IDfcCore;
import com.leafia.cursed.dfc.IDfcReceiver;
import com.leafia.cursed.zirnox.IZirnoxControl;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import com.leafia.cursed.item.LCELens;

/** Entry point. */
@Mod(CursedMod.MODID)
public class CursedMod {
    public static final String MODID = "cursed";

    public CursedMod() {
        var modBus = FMLJavaModLoadingContext.get().getModEventBus();
        CursedItems.ITEMS.register(modBus);
        CursedSounds.SOUNDS.register(modBus);
        com.leafia.cursed.fuel.RodItems.ITEMS.register(modBus);
        CursedTabs.TABS.register(modBus);
        MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void tooltips(ItemTooltipEvent e) {
        ItemStack s = e.getItemStack();
        if (s.getItem() instanceof LCELens l) {
            e.getToolTip().add(Component.literal("\u00a78Field modifier: " + pct(l.fieldMod)));
            e.getToolTip().add(Component.literal("\u00a78Power drain modifier: " + pct(l.drainMod)));
            e.getToolTip().add(Component.literal("\u00a78Energy modifier: " + pct(l.energyMod)));
        } else if (s.getItem() == CursedItems.FIX_TOOL.get()) {
            e.getToolTip().add(Component.literal("\u00a77Throw this into a collapsing DFC to fix it"));
        } else if (s.getItem() == CursedItems.CRUCIFIX.get()) {
            e.getToolTip().add(Component.literal("\u00a77It's a crucifix."));
            e.getToolTip().add(Component.literal("\u00a77Throw this into a collapsing DFC to shut it down."));
        }
    }

    /** /cursed zirnox [rods <0-100|auto> | valve <true|false>] - ZIRNOX controls;  /cursed core  - readout of the core you look at;  /cursed receiver [0..1] - show/set a receiver's absorption level. */
    @SubscribeEvent
    public void commands(RegisterCommandsEvent e) {
        e.getDispatcher().register(Commands.literal("cursed")
            .then(Commands.literal("core").executes(ctx -> {
                ServerPlayer p = ctx.getSource().getPlayerOrException();
                if (target(p) instanceof IDfcCore c) {
                    ctx.getSource().sendSuccess(() -> Component.literal(String.format(
                        "DFC: temperature %.0f, contained energy %.0f, overload timer %d",
                        c.cursed$getTemperature(), c.cursed$getContainedEnergy(), c.cursed$getOverloadTimer())), false);
                    return 1;
                }
                ctx.getSource().sendFailure(Component.literal("Look at a DFC core (within 8 blocks)."));
                return 0;
            }))
            .then(Commands.literal("zirnox")
                .executes(ctx -> zirnox(ctx.getSource().getPlayerOrException(), ctx.getSource(), z -> z.cursed$status()))
                .then(Commands.literal("rods")
                    .then(Commands.literal("auto").executes(ctx -> zirnox(ctx.getSource().getPlayerOrException(), ctx.getSource(),
                        z -> { z.cursed$setRodsTarget(-1); return "Control rods follow the on/off switch."; })))
                    .then(Commands.argument("percent", IntegerArgumentType.integer(0, 100)).executes(ctx -> {
                        int pct = IntegerArgumentType.getInteger(ctx, "percent");
                        return zirnox(ctx.getSource().getPlayerOrException(), ctx.getSource(),
                            z -> { z.cursed$setRodsTarget(pct); return "Control rod target set to " + pct + "%."; });
                    })))
                .then(Commands.literal("valve").then(Commands.argument("open", BoolArgumentType.bool()).executes(ctx -> {
                    boolean open = BoolArgumentType.getBool(ctx, "open");
                    return zirnox(ctx.getSource().getPlayerOrException(), ctx.getSource(),
                        z -> { z.cursed$setValve(open); return "CO2 valve " + (open ? "open." : "closed."); });
                }))))
            .then(Commands.literal("receiver")
                .executes(ctx -> {
                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                    if (target(p) instanceof IDfcReceiver r) {
                        ctx.getSource().sendSuccess(() -> Component.literal("Receiver level: " + r.cursed$getLevel()), false);
                        return 1;
                    }
                    ctx.getSource().sendFailure(Component.literal("Look at a DFC receiver (within 8 blocks)."));
                    return 0;
                })
                .then(Commands.argument("level", DoubleArgumentType.doubleArg(0.0, 1.0)).executes(ctx -> {
                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                    if (target(p) instanceof IDfcReceiver r) {
                        r.cursed$setLevel(DoubleArgumentType.getDouble(ctx, "level"));
                        ctx.getSource().sendSuccess(() -> Component.literal("Receiver level set to " + r.cursed$getLevel()), false);
                        return 1;
                    }
                    ctx.getSource().sendFailure(Component.literal("Look at a DFC receiver (within 8 blocks)."));
                    return 0;
                }))));
    }

    private static int zirnox(ServerPlayer p, net.minecraft.commands.CommandSourceStack src, java.util.function.Function<IZirnoxControl, String> action) {
        if (target(p) instanceof IZirnoxControl z) {
            String msg = action.apply(z);
            src.sendSuccess(() -> Component.literal(msg), false);
            return 1;
        }
        src.sendFailure(Component.literal("Look at the ZIRNOX reactor core block (within 8 blocks)."));
        return 0;
    }

    private static BlockEntity target(ServerPlayer p) {
        HitResult hr = p.pick(8.0, 0.0f, false);
        return hr instanceof BlockHitResult b ? p.level().getBlockEntity(b.getBlockPos()) : null;
    }

    private static String pct(float v) { return Math.round(v * 100.0f) + "%"; }
}
