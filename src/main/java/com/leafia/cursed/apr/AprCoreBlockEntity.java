package com.leafia.cursed.apr;

import com.hbm.api.energymk2.IEnergyProviderMK2;
import com.hbm.api.fluidmk2.IFluidStandardTransceiverMK2;
import com.hbm.entity.logic.EntityNukeExplosionMK5;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.TileEntityMachineBase;
import com.hbm.tileentity.TilePort;
import com.hbm.tileentity.TilePortShapes;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import com.hbm.items.machine.IItemFluidId;
import com.leafia.cursed.CursedItems;
import com.leafia.cursed.item.RodItem;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import com.leafia.cursed.CursedItems;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The APR core: runs AprSim, holds the main coolant/output tanks and one in/out tank pair per chamber,
 * pushes power and fluids out through Continuum's port system. Configured with /cursed apr ... (no GUI yet).
 */
public class AprCoreBlockEntity extends TileEntityMachineBase implements IEnergyProviderMK2, IFluidStandardTransceiverMK2 {
    public final AprSim sim = new AprSim();
    public boolean assembled;
    public final List<Integer> radii = new ArrayList<>();
    public FluidTankNTM oxygen = new FluidTankNTM(Fluids.NONE, 0);
    public FluidTankNTM lox = new FluidTankNTM(Fluids.NONE, 0);
    public FluidTankNTM[] chIn = new FluidTankNTM[0], chOut = new FluidTankNTM[0];
    public long power;
    private TilePort.PortDef[] cachedPorts;

    public AprCoreBlockEntity(BlockPos pos, BlockState state) {
        super(AprBlocks.CORE_BE.get(), pos, state, 0);
    }

    @Override public String getDefaultName() { return "container.cursedApr"; }

    public TilePort.PortDef[] getPorts() {
        if (cachedPorts == null) cachedPorts = TilePortShapes.around(getBlockPos().getX(), getBlockPos().getY(), getBlockPos().getZ());
        return cachedPorts;
    }

    // ---------------------------------------------------------------- setup

    private int totalCapacity() {
        int c = 0;
        for (int r : radii) c += AprStructure.capacity(r);
        return c;
    }

    private static FluidTankNTM withCapacity(FluidTankNTM t, int cap) {
        if (t.getMaxFill() == cap) return t;
        FluidTankNTM n = new FluidTankNTM(t.getTankType(), cap);
        n.setFill(Math.min(t.getFill(), cap));
        return n;
    }

    public void setupMainTank(FluidType type) {
        int cap = totalCapacity();
        int[] rate = AprFluids.rates(type);
        oxygen = new FluidTankNTM(type, cap);
        lox = new FluidTankNTM(AprFluids.conversion(type), (int) Math.min(Integer.MAX_VALUE, (long) cap * rate[1] / Math.max(rate[0], 1)));
    }

    private void onAssemble() {
        sim.chambers.clear();
        chIn = new FluidTankNTM[radii.size()];
        chOut = new FluidTankNTM[radii.size()];
        for (int i = 0; i < radii.size(); i++) {
            int cap = AprStructure.capacity(radii.get(i));
            chIn[i] = new FluidTankNTM(Fluids.NONE, cap);
            chOut[i] = new FluidTankNTM(Fluids.NONE, cap);
            sim.chambers.add(new AprSim.Chamber());
        }
        sim.particleCap = Math.max(totalCapacity() / 10, 1);
        sim.particles = Math.min(sim.particles, sim.particleCap);
        setupMainTank(oxygen.getTankType());
    }

    public String setChambers(List<Integer> list) {
        if (assembled) return "Disassemble first (/cursed apr disassemble).";
        List<Integer> sorted = new ArrayList<>(list);
        sorted.sort(Integer::compare);
        String err = AprStructure.validate(sorted);
        if (err != null) return err;
        radii.clear();
        radii.addAll(sorted);
        setChanged();
        return "Chambers set to " + radii + ". Build the structure (see /cursed apr structure), then /cursed apr assemble.";
    }

    public String setCoolant(String name) { return setCoolantType(Fluids.fromName(name.toUpperCase()), name); }

    public String setCoolantType(FluidType t, String label) {
        if (!AprFluids.isValid(t)) return "'" + label + "' is not a usable coolant (it needs a cooling conversion, like a hot fluid that cools into another).";
        if (!assembled) return "Assemble the reactor first.";
        setupMainTank(t);
        setChanged();
        return "Main coolant set to " + t.getName() + " -> " + AprFluids.conversion(t).getName() + ".";
    }

    public String setChamberFluid(int index, String name) { return setChamberType(index, Fluids.fromName(name.toUpperCase()), name); }

    public String setChamberType(int index, FluidType t, String label) {
        if (!assembled || index < 1 || index > radii.size()) return "Chamber " + index + " does not exist (assembled chambers: " + (assembled ? radii.size() : 0) + ").";
        if (!AprFluids.isValid(t)) return "'" + label + "' cannot be processed in a chamber.";
        int cap = AprStructure.capacity(radii.get(index - 1));
        chIn[index - 1] = new FluidTankNTM(t, cap);
        chOut[index - 1] = new FluidTankNTM(AprFluids.finalConversion(t), cap);
        setChanged();
        return "Chamber " + index + " now processes " + t.getName() + " into " + AprFluids.finalConversion(t).getName() + ".";
    }

    // ----------------------------------------------------------------- items

    private int nextChamber = 1;

    private static Item hbm(String path) { return ForgeRegistries.ITEMS.getValue(new ResourceLocation("hbm", path)); }

    private static void give(Player p, Item item, int count) {
        if (item == null || count <= 0) return;
        ItemStack out = new ItemStack(item, count);
        if (!p.getInventory().add(out)) p.drop(out, false);
    }

    /**
     * Right-click handling. The APR does NOT take fuel rods: like the addon it runs on particle capsules, a coolant and
     * chamber fluids. Charged capsule = +1000 particles (you get an empty one back); sneak + empty capsules = pull
     * particles back out; a fluid identifier sets the main coolant (sneak: the next chamber).
     */
    public String useItem(Player player, ItemStack held, Level level) {
        if (held.getItem() instanceof RodItem)
            return "The APR does not take fuel rods: it runs on particle capsules, a coolant and chamber fluids. Rods go in the ZIRNOX, RTG Heater or RBMK.";
        if (held.getItem() == CursedItems.PARTICLE_ABSORPTION.get()) {
            if (!assembled) return "Assemble the reactor first (/cursed apr assemble).";
            int space = (int) ((sim.particleCap - sim.particles) / 1000.0);
            int n = Math.min(held.getCount(), space);
            if (n <= 0) return "The particle chamber is full (" + (int) sim.particles + " / " + sim.particleCap + ").";
            sim.particles += 1000.0 * n;
            if (!player.getAbilities().instabuild) { held.shrink(n); give(player, hbm("particle_empty"), n); }
            setChanged();
            return "Added " + n * 1000 + " particles: " + (int) sim.particles + " / " + sim.particleCap + ".";
        }
        Item empty = hbm("particle_empty");
        if (empty != null && held.getItem() == empty && player.isShiftKeyDown()) {
            if (!assembled) return "Assemble the reactor first (/cursed apr assemble).";
            for (AprSim.Chamber c : sim.chambers) if (c.blanketTemp > 100.0) return "The chambers are too hot to take particles out.";
            int n = Math.min(held.getCount(), (int) (sim.particles / 1000.0));
            if (n <= 0) return "Less than 1000 particles in the core.";
            sim.particles -= 1000.0 * n;
            held.shrink(n);
            give(player, CursedItems.PARTICLE_ABSORPTION.get(), n);
            setChanged();
            return "Pulled out " + n * 1000 + " particles into " + n + " capsule(s).";
        }
        if (held.getItem() instanceof IItemFluidId id) {
            FluidType t = id.getType(level, getBlockPos().getX(), getBlockPos().getY(), getBlockPos().getZ(), held);
            if (t == null || t == Fluids.NONE) return "That fluid identifier is not set to a fluid.";
            if (!player.isShiftKeyDown()) return setCoolantType(t, t.getName());
            if (!assembled || radii.isEmpty()) return "Assemble the reactor first (/cursed apr assemble).";
            if (nextChamber > radii.size()) nextChamber = 1;
            return setChamberType(nextChamber++, t, t.getName());
        }
        return status();
    }

    // ------------------------------------------------------------ structure

    private static boolean matches(Block found, AprStructure.Kind kind) {
        if (kind == AprStructure.Kind.HULL) return found == AprBlocks.HULL.get() || found == AprBlocks.HULL_ASSEMBLED.get();
        return found == AprBlocks.blockFor(kind);
    }

    /** @return null if the structure is complete, otherwise a description of the first problems. */
    public String checkStructure(Level level) {
        String err = AprStructure.validate(radii);
        if (err != null) return err;
        int wrong = 0;
        String first = null;
        for (Map.Entry<AprStructure.Pos, AprStructure.Kind> e : AprStructure.requirement(radii).entrySet()) {
            AprStructure.Pos o = e.getKey();
            BlockPos p = getBlockPos().offset(o.x(), o.y(), o.z());
            BlockState st = level.getBlockState(p);
            boolean ok = e.getValue() == AprStructure.Kind.AIR ? st.isAir() : matches(st.getBlock(), e.getValue());
            if (!ok) {
                wrong++;
                if (first == null) first = p.toShortString() + " should be " + e.getValue() + " (found " + st.getBlock().getName().getString() + ")";
            }
        }
        return wrong == 0 ? null : wrong + " position(s) are wrong. First: " + first;
    }

    public String assemble(Level level) {
        if (assembled) return "Already assembled.";
        String problem = checkStructure(level);
        if (problem != null) return problem;
        swapHull(level, AprBlocks.HULL_ASSEMBLED.get());
        assembled = true;
        onAssemble();
        setChanged();
        return "APR assembled: " + radii.size() + " chamber(s), particle capacity " + sim.particleCap + ". Next: /cursed apr coolant <fluid>.";
    }

    public String disassemble(Level level) {
        if (!assembled) return "Not assembled.";
        swapHull(level, AprBlocks.HULL.get());
        assembled = false;
        sim.rps = 0.0;
        power = 0;
        setChanged();
        return "APR disassembled.";
    }

    private void swapHull(Level level, Block to) {
        for (Map.Entry<AprStructure.Pos, AprStructure.Kind> e : AprStructure.requirement(radii).entrySet()) {
            if (e.getValue() != AprStructure.Kind.HULL) continue;
            AprStructure.Pos o = e.getKey();
            BlockPos p = getBlockPos().offset(o.x(), o.y(), o.z());
            if (matches(level.getBlockState(p).getBlock(), AprStructure.Kind.HULL)) level.setBlock(p, to.defaultBlockState(), 3);
        }
    }

    // ----------------------------------------------------------------- tick

    public void tickServer(Level level) {
        setupAllPorts(getPorts());
        updatePortPOFIFO();
        if (!assembled) {
            sim.tick(false);
            power = 0;
            updateFunnel(level);
            return;
        }
        FluidType ot = oxygen.getTankType();
        sim.oxygenFill = oxygen.getFill();
        sim.oxygenMax = oxygen.getMaxFill();
        sim.loxFill = lox.getFill();
        sim.loxMax = lox.getMaxFill();
        int[] rate = AprFluids.rates(ot);
        sim.convIn = rate[0];
        sim.convOut = rate[1];
        sim.oxygenEnergy = AprFluids.energy(AprFluids.heatDifference(ot));
        for (int i = 0; i < sim.chambers.size() && i < chIn.length; i++) {
            AprSim.Chamber c = sim.chambers.get(i);
            FluidType t = chIn[i].getTankType();
            int[] fr = AprFluids.finalRates(t);
            c.inFill = chIn[i].getFill();
            c.outFill = chOut[i].getFill();
            c.outCap = chOut[i].getMaxFill();
            c.rateIn = fr[0];
            c.rateOut = fr[1];
            c.finalEnergy = AprFluids.finalEnergy(AprFluids.finalHeatDifference(t));
        }

        AprSim.Result res = sim.tick(true);

        oxygen.setFill(sim.oxygenFill);
        lox.setFill(sim.loxFill);
        for (int i = 0; i < sim.chambers.size() && i < chIn.length; i++) {
            chIn[i].setFill(sim.chambers.get(i).inFill);
            chOut[i].setFill(sim.chambers.get(i).outFill);
        }
        power = sim.powerOutput();
        updateFunnel(level);

        if (res.disassemble || !res.chamberBlasts.isEmpty()) fail(level, res);
    }

    /** Shows the control setting on the block: the funnel opens in 4 steps as control goes 0 -> 100%. */
    private void updateFunnel(Level level) {
        int bucket = assembled ? Math.max(0, Math.min(4, (int) Math.round(sim.control / 25.0))) : 0;
        BlockState st = getBlockState();
        if (st.hasProperty(AprCoreBlock.FUNNEL) && st.getValue(AprCoreBlock.FUNNEL) != bucket)
            level.setBlock(getBlockPos(), st.setValue(AprCoreBlock.FUNNEL, bucket), 3);
    }

    /** Overload: ring blasts, and a large explosion if the reactor spun past its limit (addon: LCA nuke + torex). */
    private void fail(Level level, AprSim.Result res) {
        BlockPos p = getBlockPos();
        for (int idx : res.chamberBlasts) {
            if (idx >= radii.size()) continue;
            double a = level.random.nextDouble() * Math.PI * 2.0;
            level.explode(null, p.getX() + 0.5 + Math.cos(a) * radii.get(idx), p.getY() + 0.5, p.getZ() + 0.5 + Math.sin(a) * radii.get(idx),
                8.0f, Level.ExplosionInteraction.BLOCK);
        }
        if (res.disassemble) {
            int radius = (int) Math.max(10.0, Math.min(120.0, 22.0 + (sim.particles - 30000.0) / 600.0 / 5.0));
            level.playSound(null, p, SoundEvents.GENERIC_EXPLODE, SoundSource.BLOCKS, 100.0f, 1.0f);
            EntityNukeExplosionMK5 nuke = EntityNukeExplosionMK5.statFacNoRad(level, radius, p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5);
            if (level.addFreshEntity(nuke)) nuke.startChunkLoading();
            disassemble(level);
        }
    }


    // ---------------------------------------------------------------- status

    public String status() {
        StringBuilder sb = new StringBuilder("APR: ").append(assembled ? "ASSEMBLED" : "not assembled").append(", chambers ").append(radii);
        if (assembled) {
            sb.append(String.format("%nrps %.1f (damage threshold 110), hull %.0fC, control %.0f%% -> %.0f%%, damage %d/%d",
                sim.rps, sim.hullTemp, sim.control, sim.targetControl, sim.damage, AprSim.MAX_DAMAGE));
            sb.append(String.format("%nparticles %.0f / %d, power output %d", sim.particles, sim.particleCap, power));
            sb.append(String.format("%ncoolant %s %d/%d -> %s %d/%d", oxygen.getTankType().getName(), oxygen.getFill(), oxygen.getMaxFill(),
                lox.getTankType().getName(), lox.getFill(), lox.getMaxFill()));
            for (int i = 0; i < chIn.length; i++)
                sb.append(String.format("%nchamber %d (r=%d): %s %d -> %s %d, blanket %.0fC", i + 1, radii.get(i), chIn[i].getTankType().getName(),
                    chIn[i].getFill(), chOut[i].getTankType().getName(), chOut[i].getFill(), sim.chambers.get(i).blanketTemp));
        }
        return sb.toString();
    }

    // ------------------------------------------------------ Continuum plumbing

    @Override public long getPower() { return power; }
    @Override public void setPower(long p) { power = p; }
    @Override public long getMaxPower() { return power; }
    @Override public boolean canConnect(ForgeDirection dir) { return dir != ForgeDirection.UNKNOWN; }

    @Override public boolean canConnect(FluidType type, ForgeDirection dir) {
        if (type == Fluids.NONE) return false;
        for (FluidTankNTM t : getAllTanks()) if (t.getTankType() == type) return true;
        return false;
    }

    @Override public FluidTankNTM[] getReceivingTanks() {
        List<FluidTankNTM> l = new ArrayList<>();
        l.add(oxygen);
        for (FluidTankNTM t : chIn) l.add(t);
        return l.toArray(new FluidTankNTM[0]);
    }

    @Override public FluidTankNTM[] getSendingTanks() {
        List<FluidTankNTM> l = new ArrayList<>();
        l.add(lox);
        for (FluidTankNTM t : chOut) l.add(t);
        return l.toArray(new FluidTankNTM[0]);
    }

    @Override public FluidTankNTM[] getAllTanks() {
        List<FluidTankNTM> l = new ArrayList<>();
        l.add(oxygen);
        l.add(lox);
        for (FluidTankNTM t : chIn) l.add(t);
        for (FluidTankNTM t : chOut) l.add(t);
        return l.toArray(new FluidTankNTM[0]);
    }

    // ----------------------------------------------------------- persistence

    private static long bits(double d) { return Double.doubleToLongBits(d); }
    private static double unbits(long l) { return Double.longBitsToDouble(l); }

    @Override
    protected void saveData(DataOut out) {
        super.saveData(out);
        out.putInt("aprAssembled", assembled ? 1 : 0);
        out.putInt("aprChambers", radii.size());
        for (int i = 0; i < radii.size(); i++) out.putInt("aprR" + i, radii.get(i));
        out.putLong("aprHull", bits(sim.hullTemp));
        out.putLong("aprRps", bits(sim.rps));
        out.putLong("aprControl", bits(sim.control));
        out.putLong("aprTarget", bits(sim.targetControl));
        out.putLong("aprParticles", bits(sim.particles));
        out.putInt("aprDamage", sim.damage);
        oxygen.writeToNBT(out, "aprOxygen");
        lox.writeToNBT(out, "aprLox");
        for (int i = 0; i < chIn.length; i++) {
            chIn[i].writeToNBT(out, "aprIn" + i);
            chOut[i].writeToNBT(out, "aprOut" + i);
            out.putLong("aprBlanket" + i, bits(sim.chambers.get(i).blanketTemp));
        }
    }

    @Override
    protected void loadData(DataIn in) {
        super.loadData(in);
        assembled = in.getIntOr("aprAssembled", 0) == 1;
        radii.clear();
        int n = in.getIntOr("aprChambers", 0);
        for (int i = 0; i < n; i++) radii.add(in.getIntOr("aprR" + i, 0));
        if (radii.isEmpty() || AprStructure.validate(radii) != null) { radii.clear(); assembled = false; }
        sim.hullTemp = unbits(in.getLongOr("aprHull", bits(20.0)));
        sim.rps = unbits(in.getLongOr("aprRps", 0L));
        sim.control = unbits(in.getLongOr("aprControl", bits(10.0)));
        sim.targetControl = unbits(in.getLongOr("aprTarget", bits(10.0)));
        sim.particles = unbits(in.getLongOr("aprParticles", 0L));
        sim.damage = in.getIntOr("aprDamage", 0);
        if (assembled) {
            onAssemble(); // rebuild tank shells and capacities, then restore contents
            oxygen.readFromNBT(in, "aprOxygen");
            lox.readFromNBT(in, "aprLox");
            int cap = totalCapacity();
            oxygen = withCapacity(oxygen, cap);
            int[] rate = AprFluids.rates(oxygen.getTankType());
            lox = withCapacity(lox, (int) Math.min(Integer.MAX_VALUE, (long) cap * rate[1] / Math.max(rate[0], 1)));
            for (int i = 0; i < chIn.length; i++) {
                chIn[i].readFromNBT(in, "aprIn" + i);
                chOut[i].readFromNBT(in, "aprOut" + i);
                int c = AprStructure.capacity(radii.get(i));
                chIn[i] = withCapacity(chIn[i], c);
                chOut[i] = withCapacity(chOut[i], c);
                sim.chambers.get(i).blanketTemp = unbits(in.getLongOr("aprBlanket" + i, bits(20.0)));
            }
        }
    }
}
