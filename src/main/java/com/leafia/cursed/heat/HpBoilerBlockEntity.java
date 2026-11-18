package com.leafia.cursed.heat;

import com.hbm.api.fluidmk2.IFluidStandardTransceiverMK2;
import com.hbm.api.tile.IHeatSource;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.fluid.trait.FT_Heatable;
import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.TileEntityMachineBase;
import com.hbm.tileentity.TilePort;
import com.hbm.tileentity.TilePortShapes;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * HP Boiler (addon HPBoilerTE): boils a fluid through up to 4 chained heating steps, pulling heat from a heat source
 * directly below it (e.g. the RTG Heater). Fluid and compression are chosen with /cursed boiler (no GUI yet).
 */
public class HpBoilerBlockEntity extends TileEntityMachineBase implements IFluidStandardTransceiverMK2 {
    public int heat, compression = 4;
    public FluidTankNTM input = new FluidTankNTM(Fluids.WATER, HpBoilerMath.TANK_BASE);
    public FluidTankNTM output = new FluidTankNTM(Fluids.NONE, HpBoilerMath.TANK_BASE);
    public HpBoilerMath.Data data = HpBoilerMath.Data.NONE;
    public boolean isOn;
    private TilePort.PortDef[] ports;

    public HpBoilerBlockEntity(BlockPos pos, BlockState state) {
        super(HeatBlocks.HP_BOILER_BE.get(), pos, state, 0);
        setFluid(Fluids.WATER, compression);
    }

    @Override public String getDefaultName() { return "container.cursedHpBoiler"; }

    private TilePort.PortDef[] ports() {
        if (ports == null) ports = TilePortShapes.around(getBlockPos().getX(), getBlockPos().getY(), getBlockPos().getZ());
        return ports;
    }

    /** Walks the fluid's heating chain (water -> steam -> ...) into one combined conversion. */
    public static HpBoilerMath.Data boilData(FluidType fluid, int compression, FluidType[] outType) {
        List<HpBoilerMath.Step> steps = new ArrayList<>();
        FluidType cur = fluid;
        outType[0] = Fluids.NONE;
        for (int i = 0; i < compression; i++) {
            FT_Heatable trait = cur.getTrait(FT_Heatable.class);
            if (trait == null) break;
            double eff = trait.getEfficiency(FT_Heatable.HeatingType.BOILER);
            if (eff <= 0.0) break;
            FT_Heatable.HeatingStep step = trait.getFirstStep();
            if (step == null || step.typeProduced == null) break;
            steps.add(new HpBoilerMath.Step(step.amountReq, step.heatReq, step.amountProduced, eff));
            outType[0] = cur = step.typeProduced;
        }
        return HpBoilerMath.combine(steps, compression);
    }

    public String setFluid(FluidType fluid, int comp) {
        FluidType[] out = new FluidType[1];
        HpBoilerMath.Data d = boilData(fluid, comp, out);
        if (d.comp() <= 0 || out[0] == Fluids.NONE) return fluid.getName() + " cannot be boiled.";
        input = new FluidTankNTM(fluid, HpBoilerMath.tankSize(d.in()));
        output = new FluidTankNTM(out[0], HpBoilerMath.tankSize(d.out()));
        compression = d.comp();
        data = d;
        return fluid.getName() + " -> " + out[0].getName() + " over " + d.comp() + " step(s): " + d.in() + " in, " + d.out() + " out, " + d.heatReq() + " heat per conversion.";
    }

    public void tickServer(Level level) {
        isOn = false;
        setupFluidPorts(ports());
        updatePortFIFO();

        BlockEntity below = level.getBlockEntity(getBlockPos().below());
        if (below instanceof IHeatSource src) {
            int[] r = HpBoilerMath.pullHeat(heat, src.getHeatStored());
            if (r[1] > 0) src.useUpHeat(r[1]);
            heat = r[0];
        } else {
            heat = HpBoilerMath.pullHeat(heat, 0)[0];
        }

        int ops = HpBoilerMath.ops(input.getFill(), output.getFill(), output.getMaxFill(), heat, data);
        if (ops > 0) {
            input.setFill(input.getFill() - data.in() * ops);
            output.setFill(output.getFill() + data.out() * ops);
            heat -= data.heatReq() * ops;
            isOn = true;
        }
        setChanged();
    }

    public String status() {
        return String.format("HP Boiler: heat %d / %d, %s %d/%d -> %s %d/%d, %d step(s), %s",
            heat, HpBoilerMath.MAX_HEAT, input.getTankType().getName(), input.getFill(), input.getMaxFill(),
            output.getTankType().getName(), output.getFill(), output.getMaxFill(), compression, isOn ? "boiling" : "idle");
    }

    @Override public boolean canConnect(FluidType type, ForgeDirection dir) {
        return type != Fluids.NONE && (type == input.getTankType() || type == output.getTankType());
    }
    @Override public FluidTankNTM[] getReceivingTanks() { return new FluidTankNTM[]{input}; }
    @Override public FluidTankNTM[] getSendingTanks() { return new FluidTankNTM[]{output}; }
    @Override public FluidTankNTM[] getAllTanks() { return new FluidTankNTM[]{input, output}; }

    @Override
    protected void saveData(DataOut out) {
        super.saveData(out);
        out.putInt("hpHeat", heat);
        out.putInt("hpComp", compression);
        input.writeToNBT(out, "hpInput");
        output.writeToNBT(out, "hpOutput");
    }

    @Override
    protected void loadData(DataIn in) {
        super.loadData(in);
        heat = in.getIntOr("hpHeat", 0);
        compression = Math.max(1, Math.min(4, in.getIntOr("hpComp", 4)));
        input.readFromNBT(in, "hpInput");
        output.readFromNBT(in, "hpOutput");
        FluidType[] out = new FluidType[1];
        HpBoilerMath.Data d = boilData(input.getTankType(), compression, out);
        if (d.comp() > 0) {
            data = d;
            if (input.getMaxFill() != HpBoilerMath.tankSize(d.in())) { FluidTankNTM t = new FluidTankNTM(input.getTankType(), HpBoilerMath.tankSize(d.in())); t.setFill(Math.min(input.getFill(), t.getMaxFill())); input = t; }
            if (output.getMaxFill() != HpBoilerMath.tankSize(d.out())) { FluidTankNTM t = new FluidTankNTM(output.getTankType(), HpBoilerMath.tankSize(d.out())); t.setFill(Math.min(output.getFill(), t.getMaxFill())); output = t; }
        }
    }
}
