package com.leafia.cursed.heat;

import com.hbm.api.energymk2.IEnergyReceiverMK2;
import com.hbm.api.fluidmk2.IFluidStandardTransceiverMK2;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.fluid.trait.FT_Coolable;
import com.hbm.inventory.fluid.trait.FT_Heatable;
import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.TileEntityMachineBase;
import com.hbm.tileentity.TilePort;
import com.hbm.tileentity.TilePortShapes;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Coolant Heat Exchanger (addon CoolantHeatexTE) on Continuum fluids and power: cools a hot fluid into its cooled form,
 * and uses that heat to warm a coolant (default water -> steam). Needs power. Configured with /cursed heatex.
 */
public class CoolantHeatexBlockEntity extends TileEntityMachineBase implements IEnergyReceiverMK2, IFluidStandardTransceiverMK2 {
    public long power;
    public int heatEnergy, amountToCool = CoolantHeatexMath.DEFAULT_CYCLE, tickDelay = 1;
    public FluidTankNTM hotIn = new FluidTankNTM(Fluids.NONE, CoolantHeatexMath.TANK);
    public FluidTankNTM hotOut = new FluidTankNTM(Fluids.NONE, CoolantHeatexMath.TANK);
    public FluidTankNTM coolIn = new FluidTankNTM(Fluids.WATER, CoolantHeatexMath.TANK);
    public FluidTankNTM coolOut = new FluidTankNTM(Fluids.NONE, CoolantHeatexMath.TANK);
    private TilePort.PortDef[] ports;

    public CoolantHeatexBlockEntity(BlockPos pos, BlockState state) {
        super(HeatBlocks.COOLANT_HEATEX_BE.get(), pos, state, 0);
        setCoolant(Fluids.WATER);
    }

    @Override public String getDefaultName() { return "container.cursedCoolantHeatex"; }

    private TilePort.PortDef[] ports() {
        if (ports == null) ports = TilePortShapes.around(getBlockPos().getX(), getBlockPos().getY(), getBlockPos().getZ());
        return ports;
    }

    private static FT_Coolable coolable(FluidType t) {
        if (t == null || t == Fluids.NONE || !t.hasTrait(FT_Coolable.class)) return null;
        FT_Coolable c = t.getTrait(FT_Coolable.class);
        return c.getEfficiency(FT_Coolable.CoolingType.HEATEXCHANGER) > 0.0 ? c : null;
    }

    private static CoolantHeatexMath.Heating heating(FluidType t) {
        if (t == null || t == Fluids.NONE || !t.hasTrait(FT_Heatable.class)) return null;
        FT_Heatable h = t.getTrait(FT_Heatable.class);
        if (h.getEfficiency(FT_Heatable.HeatingType.BOILER) <= 0.0) return null;
        FT_Heatable.HeatingStep s = h.getFirstStep();
        return s == null || s.typeProduced == null ? null : new CoolantHeatexMath.Heating(s.amountReq, s.amountProduced, s.heatReq);
    }

    public String setHotFluid(FluidType t) {
        FT_Coolable c = coolable(t);
        if (c == null) return t.getName() + " cannot be cooled in a heat exchanger.";
        hotIn = new FluidTankNTM(t, CoolantHeatexMath.TANK);
        hotOut = new FluidTankNTM(c.coolsTo, CoolantHeatexMath.TANK);
        return "Hot fluid " + t.getName() + " cools into " + c.coolsTo.getName() + ".";
    }

    public String setCoolant(FluidType t) {
        CoolantHeatexMath.Heating h = heating(t);
        if (h == null) return t.getName() + " cannot be heated here.";
        coolIn = new FluidTankNTM(t, CoolantHeatexMath.TANK);
        coolOut = new FluidTankNTM(t.getTrait(FT_Heatable.class).getFirstStep().typeProduced, CoolantHeatexMath.TANK);
        return "Coolant " + t.getName() + " heats into " + coolOut.getTankType().getName() + ".";
    }

    public void tickServer(Level level) {
        setupAllPorts(ports());
        updatePortPIFIFO();
        CoolantHeatexMath.Heating h = heating(coolIn.getTankType());
        FT_Coolable c = coolable(hotIn.getTankType());
        if (h != null && c != null && CoolantHeatexMath.wantsMoreHeat(heatEnergy, h) && power >= CoolantHeatexMath.POWER_USE) {
            power -= CoolantHeatexMath.POWER_USE;
            if (tickDelay < 1) tickDelay = 1;
            if (level.getGameTime() % tickDelay == 0) {
                int ops = CoolantHeatexMath.coolOps(hotIn.getFill(), hotOut.getFill(), hotOut.getMaxFill(), c.amountReq, c.amountProduced, amountToCool);
                if (ops > 0) {
                    hotIn.setFill(hotIn.getFill() - c.amountReq * ops);
                    hotOut.setFill(hotOut.getFill() + c.amountProduced * ops);
                    heatEnergy += CoolantHeatexMath.heatGained(c.heatEnergy, ops, c.getEfficiency(FT_Coolable.CoolingType.HEATEXCHANGER));
                }
            }
        }
        if (h != null) {
            int ops = CoolantHeatexMath.heatOps(coolIn.getFill(), coolOut.getFill(), coolOut.getMaxFill(), heatEnergy, h);
            if (ops > 0) {
                coolIn.setFill(coolIn.getFill() - h.amountReq() * ops);
                coolOut.setFill(coolOut.getFill() + h.amountProduced() * ops);
                heatEnergy -= h.heatReq() * ops;
            }
        }
        setChanged();
    }

    public String status() {
        return String.format("Coolant Heat Exchanger: power %d/%d, heat %d%n hot %s %d -> %s %d%n coolant %s %d -> %s %d%n cycle %d every %d tick(s)",
            power, CoolantHeatexMath.MAX_POWER, heatEnergy,
            hotIn.getTankType().getName(), hotIn.getFill(), hotOut.getTankType().getName(), hotOut.getFill(),
            coolIn.getTankType().getName(), coolIn.getFill(), coolOut.getTankType().getName(), coolOut.getFill(), amountToCool, tickDelay);
    }

    @Override public long getPower() { return power; }
    @Override public void setPower(long p) { power = p; }
    @Override public long getMaxPower() { return CoolantHeatexMath.MAX_POWER; }
    @Override public boolean canConnect(ForgeDirection dir) { return dir != ForgeDirection.UNKNOWN; }

    @Override public boolean canConnect(FluidType type, ForgeDirection dir) {
        if (type == Fluids.NONE) return false;
        for (FluidTankNTM t : getAllTanks()) if (t.getTankType() == type) return true;
        return false;
    }
    @Override public FluidTankNTM[] getReceivingTanks() { return new FluidTankNTM[]{hotIn, coolIn}; }
    @Override public FluidTankNTM[] getSendingTanks() { return new FluidTankNTM[]{hotOut, coolOut}; }
    @Override public FluidTankNTM[] getAllTanks() { return new FluidTankNTM[]{hotIn, hotOut, coolIn, coolOut}; }

    @Override
    protected void saveData(DataOut out) {
        super.saveData(out);
        out.putLong("hxPower", power);
        out.putInt("hxHeat", heatEnergy);
        out.putInt("hxCycle", amountToCool);
        out.putInt("hxDelay", tickDelay);
        hotIn.writeToNBT(out, "hxHotIn"); hotOut.writeToNBT(out, "hxHotOut");
        coolIn.writeToNBT(out, "hxCoolIn"); coolOut.writeToNBT(out, "hxCoolOut");
    }

    @Override
    protected void loadData(DataIn in) {
        super.loadData(in);
        power = in.getLongOr("hxPower", 0L);
        heatEnergy = in.getIntOr("hxHeat", 0);
        amountToCool = in.getIntOr("hxCycle", CoolantHeatexMath.DEFAULT_CYCLE);
        tickDelay = Math.max(1, in.getIntOr("hxDelay", 1));
        hotIn.readFromNBT(in, "hxHotIn"); hotOut.readFromNBT(in, "hxHotOut");
        coolIn.readFromNBT(in, "hxCoolIn"); coolOut.readFromNBT(in, "hxCoolOut");
    }
}
