package com.leafia.cursed.wind;

import com.hbm.api.energymk2.IEnergyProviderMK2;
import com.hbm.api.fluidmk2.IFluidStandardReceiverMK2;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
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
 * Medium wind turbine (addon WindTurbineMediumTE): turns the global wind into power, needs lubricant (1 mB per 60 ticks),
 * and produces nothing when something blocks the wind within 500 blocks upwind. Power leaves through the block's ports.
 */
public class WindTurbineBlockEntity extends TileEntityMachineBase implements IEnergyProviderMK2, IFluidStandardReceiverMK2 {
    public static final int LUBE_PERIOD = 60, RANGE = 500;
    public static final double RAY_HEIGHT = 5.0;

    public long power;
    public boolean obstructed;
    public final FluidTankNTM lube = new FluidTankNTM(Fluids.LUBRICANT, 72000 / LUBE_PERIOD);
    private boolean firstTick = true;
    private int lubeTimer;
    private TilePort.PortDef[] ports;

    public WindTurbineBlockEntity(BlockPos pos, BlockState state) { super(WindBlocks.TURBINE_BE.get(), pos, state, 0); }

    @Override public String getDefaultName() { return "container.cursedWindTurbine"; }

    private TilePort.PortDef[] ports() {
        if (ports == null) ports = TilePortShapes.around(getBlockPos().getX(), getBlockPos().getY(), getBlockPos().getZ());
        return ports;
    }

    /** True if a block (or liquid) stops a ray going up-and-out from the turbine; stops at unloaded chunks. */
    private boolean rayBlocked(Level level, double angleDeg) {
        double dx = Math.cos(Math.toRadians(angleDeg)), dz = -Math.sin(Math.toRadians(angleDeg));
        double sx = getBlockPos().getX() + 0.5 + dx * 0.71, sy = getBlockPos().getY() + RAY_HEIGHT, sz = getBlockPos().getZ() + 0.5 + dz * 0.71;
        for (int i = 1; i <= RANGE; i++) {
            BlockPos p = BlockPos.containing(sx + dx * i, sy + 0.1 * i, sz + dz * i);
            if (p.getY() >= level.getMaxBuildHeight() || !level.hasChunkAt(p)) return false;
            BlockState st = level.getBlockState(p);
            if (!st.isAir() && (!st.getCollisionShape(level, p).isEmpty() || !st.getFluidState().isEmpty())) return true;
        }
        return false;
    }

    public void tickServer(Level level) {
        if (firstTick || level.random.nextInt(100) == 0) {
            double a = WindState.SIM.angle + 180.0;
            obstructed = rayBlocked(level, a + 10.0) && rayBlocked(level, a - 10.0);
            firstTick = false;
        }
        long gen = WindSim.turbineOutput(WindState.SIM.power, WindState.dimensionMultiplier(level), obstructed, lube.getFill(), lube.getMaxFill());
        if (gen > 10L && ++lubeTimer > LUBE_PERIOD) {
            lubeTimer = 0;
            lube.setFill(Math.max(0, lube.getFill() - 1));
        }
        power = gen;
        setupAllPorts(ports());
        updatePortPOFIFO();
        setChanged();
    }

    public String status() {
        return String.format("Wind turbine: output %d/t, wind %.0f (%s) from %.0f deg, %s, lubricant %d/%d",
            power, WindState.SIM.power, WindState.SIM.strongWind ? "strong spell" : "calm spell", WindState.SIM.angle % 360.0,
            obstructed ? "BLOCKED upwind" : "clear upwind", lube.getFill(), lube.getMaxFill());
    }

    @Override public long getPower() { return power; }
    @Override public void setPower(long p) { power = p; }
    @Override public long getMaxPower() { return 100000L; }
    @Override public boolean canConnect(ForgeDirection dir) { return dir != ForgeDirection.UNKNOWN; }

    @Override public boolean canConnect(FluidType type, ForgeDirection dir) { return type == Fluids.LUBRICANT; }
    @Override public FluidTankNTM[] getReceivingTanks() { return new FluidTankNTM[]{lube}; }
    @Override public FluidTankNTM[] getAllTanks() { return new FluidTankNTM[]{lube}; }

    @Override
    protected void saveData(DataOut out) {
        super.saveData(out);
        out.putInt("windObstructed", obstructed ? 1 : 0);
        lube.writeToNBT(out, "windLube");
    }

    @Override
    protected void loadData(DataIn in) {
        super.loadData(in);
        obstructed = in.getIntOr("windObstructed", 0) == 1;
        lube.readFromNBT(in, "windLube");
    }
}
