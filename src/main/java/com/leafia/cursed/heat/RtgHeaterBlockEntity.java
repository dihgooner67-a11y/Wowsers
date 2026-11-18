package com.leafia.cursed.heat;

import com.hbm.api.tile.IHeatSource;
import com.hbm.tileentity.TileEntityMachineBase;
import com.hbm.util.RTGUtil;
import com.leafia.cursed.fuel.RodSim;
import com.leafia.cursed.fuel.RodState;
import com.leafia.cursed.fuel.RodType;
import com.leafia.cursed.fuel.RodTypes;
import com.leafia.cursed.item.RodItem;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * RTG Heater (addon HeaterRTGTE): 15 RTG pellet slots plus 1 addon-rod slot feed a heat buffer that boilers and other
 * machines above it pull from, like Continuum's own heaters. No GUI: items go in by right-click.
 */
public class RtgHeaterBlockEntity extends TileEntityMachineBase implements IHeatSource {
    public static final int MAX_HEAT = 750000, ROD_SLOT = 15, PELLET_SLOTS = 15;
    private static final int[] PELLET_INDEXES = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14};

    public int heatEnergy, heatGen;

    public RtgHeaterBlockEntity(BlockPos pos, BlockState state) { super(HeatBlocks.RTG_HEATER_BE.get(), pos, state, 16); }

    @Override public String getDefaultName() { return "container.cursedRtgHeater"; }

    public void tickServer(Level level) {
        heatEnergy = (int) (heatEnergy * 0.999);
        IHeatSource source = IHeatSource.at(level, getBlockPos().below());
        if (source != null) {
            heatEnergy = (int) (heatEnergy + source.getHeatStored() * 0.85);
            source.useUpHeat(source.getHeatStored());
        }
        heatGen = RTGUtil.updateRTGs(inventory, PELLET_INDEXES) * 10;

        ItemStack stack = inventory.getStackInSlot(ROD_SLOT);
        if (stack.getItem() instanceof RodItem rod) {
            RodState s = RodItem.readState(stack);
            if (s.nuke || s.spillage > 100) { meltdown(level, s.nuke); return; }
            heatGen += (int) Math.floor(Math.pow(s.heat / 250.0, 0.54) * 15.0) * 10 * 15;
            double flux = rod.type.fluxFrom(rod.type, s, false);
            RodSim.step(rod.type, s, flux * 2.0, 0.0, 0.0, 0.0, 0.0, 1.0);
            if (RodSim.depleted(rod.type, s)) {
                RodType next = RodTypes.ALL.get(rod.type.decayProduct);
                if (next != null) { s.depletion = 0.0; inventory.setStackInSlot(ROD_SLOT, RodItem.stackOf(next, s)); }
                else RodItem.writeState(stack, s);
            } else RodItem.writeState(stack, s);
        }
        heatEnergy = Math.min(heatEnergy + heatGen, MAX_HEAT);
        setChanged();
    }

    private void meltdown(Level level, boolean big) {
        BlockPos p = getBlockPos();
        for (int i = 0; i < inventory.getSlots(); i++) inventory.setStackInSlot(i, ItemStack.EMPTY);
        level.removeBlock(p, false);
        level.explode(null, p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5, big ? 8.0f : 3.0f, Level.ExplosionInteraction.BLOCK);
    }

    public String status() {
        int pellets = 0;
        for (int i = 0; i < PELLET_SLOTS; i++) if (!inventory.getStackInSlot(i).isEmpty()) pellets++;
        ItemStack rod = inventory.getStackInSlot(ROD_SLOT);
        return String.format("RTG Heater: heat %d / %d (+%d/t), pellets %d/15, rod: %s", heatEnergy, MAX_HEAT, heatGen, pellets,
            rod.isEmpty() ? "none" : rod.getHoverName().getString());
    }

    @Override public int getHeatStored() { return heatEnergy; }
    @Override public void useUpHeat(int heat) { heatEnergy = Math.max(0, heatEnergy - heat); }

    @Override
    protected void saveData(DataOut out) {
        super.saveData(out);
        out.putInt("rtgHeat", heatEnergy);
    }

    @Override
    protected void loadData(DataIn in) {
        super.loadData(in);
        heatEnergy = in.getIntOr("rtgHeat", 0);
    }
}
