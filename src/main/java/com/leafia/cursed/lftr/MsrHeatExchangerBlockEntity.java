package com.leafia.cursed.lftr;

import com.hbm.api.energymk2.IEnergyProviderMK2;
import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.TileEntityMachineBase;
import com.hbm.tileentity.TilePort;
import com.hbm.tileentity.TilePortShapes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * LFTR heat exchanger (not in the addon: it sent hot salt to the addon's molten-salt fluid network, which Continuum has
 * no counterpart for). This block touches fuel elements, draws heat out of them and turns it into power.
 */
public class MsrHeatExchangerBlockEntity extends TileEntityMachineBase implements IEnergyProviderMK2 {
    /** Share of an element's heat drawn per tick, and power per heat unit (tunable). */
    public static final double EXTRACT_FRACTION = 0.1, POWER_PER_HEAT = 2000.0;

    public long power;
    private TilePort.PortDef[] ports;

    public MsrHeatExchangerBlockEntity(BlockPos pos, BlockState state) { super(LftrBlocks.EXCHANGER_BE.get(), pos, state, 0); }

    @Override public String getDefaultName() { return "container.cursedLftrExchanger"; }

    private TilePort.PortDef[] ports() {
        if (ports == null) ports = TilePortShapes.around(getBlockPos().getX(), getBlockPos().getY(), getBlockPos().getZ());
        return ports;
    }

    public void tickServer(Level level) {
        double drawn = 0.0;
        for (Direction d : Direction.values()) {
            if (level.getBlockEntity(getBlockPos().relative(d)) instanceof MsrElementBlockEntity e && e.cell.amount > 0 && e.cell.heat > 0.5) {
                double q = e.cell.heat * EXTRACT_FRACTION;
                e.cell.heat -= q;
                drawn += q;
            }
        }
        power = (long) (drawn * POWER_PER_HEAT);
        setupAllPorts(ports());
        updatePortPOFIFO();
    }

    public String status() { return "LFTR heat exchanger: producing " + power + " power per tick from adjacent fuel elements."; }

    @Override public long getPower() { return power; }
    @Override public void setPower(long p) { power = p; }
    @Override public long getMaxPower() { return power; }
    @Override public boolean canConnect(ForgeDirection dir) { return dir != ForgeDirection.UNKNOWN; }
}
