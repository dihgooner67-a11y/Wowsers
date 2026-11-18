package com.leafia.cursed.mixin;

import com.hbm.tileentity.machine.MachineFluidContainerIO;
import com.hbm.tileentity.machine.TileEntityReactorZirnox;
import com.leafia.cursed.item.RodItem;
import com.leafia.cursed.fuel.RodType;
import com.leafia.cursed.fuel.ZirnoxSim;
import com.leafia.cursed.zirnox.IZirnoxControl;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * CINDER-C: when the ZIRNOX holds addon rods (cursed:rod_*), run the addon's simulation instead of
 * Continuum's. With only Continuum's own rods, Continuum's tick runs untouched.
 */
@Mixin(value = TileEntityReactorZirnox.class, remap = false)
public abstract class ZirnoxMixin implements IZirnoxControl {
    @Unique private final ZirnoxSim cursed$sim = new ZirnoxSim();
    @Unique private int cursed$rodsTarget = -1;
    @Unique private boolean cursed$valve;

    @Inject(method = "isItemValidForSlot", at = @At("HEAD"), cancellable = true)
    private void cursed$validRods(int slot, ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        if (slot < 24 && stack.getItem() instanceof RodItem) cir.setReturnValue(true);
    }

    @Inject(method = "serverTick", at = @At("HEAD"), cancellable = true)
    private static void cursed$serverTick(Level level, BlockPos pos, BlockState state, TileEntityReactorZirnox te, CallbackInfo ci) {
        for (int i = 0; i < 24; i++) {
            if (te.inventory.getStackInSlot(i).getItem() instanceof RodItem) {
                ((IZirnoxControl) te).cursed$tick(level, pos, state);
                ci.cancel();
                return;
            }
        }
    }

    @Override
    public void cursed$tick(Level level, BlockPos pos, BlockState state) {
        TileEntityReactorZirnox te = (TileEntityReactorZirnox) (Object) this;
        te.setupFluidPorts(te.getPorts());
        te.updatePortFIFO();
        if (te.redstonePowered) te.isOn = true;
        MachineFluidContainerIO.load(te.carbonDioxide, te.inventory, 24, 26);
        MachineFluidContainerIO.load(te.water, te.inventory, 25, 27);

        ZirnoxSim sim = cursed$sim;
        sim.rodsTarget = cursed$rodsTarget >= 0 ? cursed$rodsTarget : (te.isOn ? 100 : 0);
        ZirnoxSim.Inputs in = new ZirnoxSim.Inputs();
        for (int i = 0; i < 24; i++) {
            ItemStack st = te.inventory.getStackInSlot(i);
            if (st.getItem() instanceof RodItem r) { in.types[i] = r.type; in.states[i] = RodItem.readState(st); }
        }
        in.co2Fill = te.carbonDioxide.getFill(); in.co2Max = te.carbonDioxide.getMaxFill();
        in.waterFill = te.water.getFill(); in.waterMax = te.water.getMaxFill();
        in.valveOpen = cursed$valve;
        in.steamTempC = te.steam.getTankType().getTemperature();

        ZirnoxSim.Outputs out = sim.tick(in);

        // write rods back (state, and replacement when a rod depleted into its decay product)
        for (int i = 0; i < 24; i++) {
            RodType t = in.types[i];
            if (t == null) continue;
            ItemStack old = te.inventory.getStackInSlot(i);
            if (old.getItem() instanceof RodItem r && r.type == t) RodItem.writeState(old, in.states[i]);
            else te.inventory.setStackInSlot(i, RodItem.stackOf(t, in.states[i]));
        }
        te.carbonDioxide.setFill(Math.max(0, te.carbonDioxide.getFill() - out.co2Drain));
        te.water.setFill(Math.max(0, te.water.getFill() - out.waterDrain));
        te.steam.setFill(Math.min(te.steam.getMaxFill(), te.steam.getFill() + out.steamAdd));
        if (out.stressSound)
            level.playSound(null, pos.getX() + 0.5, pos.getY() + 2.5, pos.getZ() + 0.5, SoundEvents.ANVIL_LAND, SoundSource.BLOCKS, 2.0f, 0.4f);
        if (out.explode) {
            ((ZirnoxInvoker) te).cursed$meltdown(level, pos, state);
            return;
        }
        // keep Continuum's GUI gauges meaningful (0..100000)
        te.heat = (int) Math.max(0, Math.min(99999, (sim.hullTemp - 20.0) / 1280.0 * 100000.0));
        te.pressure = (int) Math.max(0, Math.min(99999, sim.pressure / ZirnoxSim.MAX_PRESSURE * 100000.0));
        te.setChanged();
        te.networkPackNT();
    }

    @Inject(method = "saveData", at = @At("TAIL"))
    private void cursed$save(DataOut out, CallbackInfo ci) {
        out.putInt("cursedHull", (int) (cursed$sim.hullTemp * 10.0));
        out.putInt("cursedRods", cursed$sim.rods);
        out.putInt("cursedTarget", cursed$rodsTarget);
        out.putInt("cursedValve", cursed$valve ? 1 : 0);
    }

    @Inject(method = "loadData", at = @At("TAIL"))
    private void cursed$load(DataIn in, CallbackInfo ci) {
        cursed$sim.hullTemp = in.getIntOr("cursedHull", 200) / 10.0;
        cursed$sim.rods = in.getIntOr("cursedRods", 0);
        cursed$rodsTarget = in.getIntOr("cursedTarget", -1);
        cursed$valve = in.getIntOr("cursedValve", 0) == 1;
    }

    @Override public void cursed$setRodsTarget(int t) { cursed$rodsTarget = t < 0 ? -1 : Math.min(t, 100); }
    @Override public void cursed$setValve(boolean open) { cursed$valve = open; }
    @Override public String cursed$status() {
        return String.format("ZIRNOX: hull %.0fC, pressure %.1f/%.0f, rods %d%% (target %s), CO2 valve %s",
            cursed$sim.hullTemp, cursed$sim.pressure, ZirnoxSim.MAX_PRESSURE, cursed$sim.rods,
            cursed$rodsTarget < 0 ? "auto" : cursed$rodsTarget + "%", cursed$valve ? "open" : "closed");
    }
}
