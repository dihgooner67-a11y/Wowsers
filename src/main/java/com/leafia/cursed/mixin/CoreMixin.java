package com.leafia.cursed.mixin;

import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.items.machine.ItemCatalyst;
import com.hbm.items.special.ItemAMSCore;
import com.hbm.lib.ModDamageSources;
import com.hbm.tileentity.TileEntityMachineBase;
import com.hbm.tileentity.machine.dfc.TileEntityCore;
import com.hbm.tileentity.machine.dfc.TileEntityCoreReceiver;
import com.leafia.cursed.dfc.BeamContext;
import com.leafia.cursed.dfc.DfcCoreSim;
import com.leafia.cursed.dfc.DfcTuning;
import com.leafia.cursed.dfc.IDfcCore;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.registries.ForgeRegistries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * EMBER: replaces Continuum's simple heat/field core with the addon's simulation (temperature,
 * contained energy, absorber transfer, shocks, catalyst wear, overload countdown).
 * Continuum's own detonation / radiation checks are disabled and driven by the overload timer instead.
 */
@Mixin(value = TileEntityCore.class, remap = false)
public abstract class CoreMixin extends TileEntityMachineBase implements IDfcCore {
    @Shadow public int field;
    @Shadow public int heat;
    @Shadow public boolean meltdownTick;
    @Shadow public FluidTankNTM[] tanks;
    @Shadow public abstract boolean isReady();
    @Shadow public abstract int getCore();
    @Shadow public abstract float getFuelEfficiency(FluidType type);
    @Shadow protected abstract void detonate(Level level);

    @Unique private DfcCoreSim cursed$sim;
    @Unique private final List<TileEntityCoreReceiver> cursed$absorbers = new ArrayList<>();
    @Unique private double cursed$incomingSpk;
    @Unique private int cursed$overloadTimer;
    @Unique private double cursed$stabilization;
    @Unique private double cursed$energyMod = 1.0;

    protected CoreMixin(BlockEntityType<?> type, BlockPos pos, BlockState state, int slots) {
        super(type, pos, state, slots);
    }

    @Unique private DfcCoreSim cursed$sim() {
        if (cursed$sim == null) cursed$sim = new DfcCoreSim(new Random());
        return cursed$sim;
    }

    // ---- beam input: the core no longer passes energy on; absorbers are paid by the simulation ----
    @Inject(method = "burn", at = @At("HEAD"), cancellable = true)
    private void cursed$burn(long joules, CallbackInfoReturnable<Long> cir) {
        if (!isReady()) return; // original returns the joules untouched
        BeamContext.set((TileEntityCore) (Object) this);
        cursed$incomingSpk += joules * DfcTuning.SPK_PER_JOULE;
        cir.setReturnValue(0L);
    }

    // ---- disable Continuum's heat>=field detonation and passive radiation ----
    @Inject(method = "shouldDetonate", at = @At("HEAD"), cancellable = true)
    private void cursed$noOldDetonation(CallbackInfoReturnable<Boolean> cir) { cir.setReturnValue(false); }

    @Inject(method = "radiation", at = @At("HEAD"), cancellable = true)
    private void cursed$radiation(Level level, CallbackInfo ci) {
        if (cursed$overloadTimer <= 0) ci.cancel();
        else meltdownTick = true;
    }

    // ---- the simulation, once per server tick, before Continuum's own tick body ----
    @Inject(method = "tickServer", at = @At("HEAD"))
    private void cursed$tick(Level level, CallbackInfo ci) {
        if (level.isClientSide) return;
        DfcCoreSim sim = cursed$sim();
        ItemStack a = inventory.getStackInSlot(0), core = inventory.getStackInSlot(1), b = inventory.getStackInSlot(2);
        boolean active = a.getItem() instanceof ItemCatalyst && b.getItem() instanceof ItemCatalyst
                      && core.getItem() instanceof ItemAMSCore;

        if (!active) { // nothing to burn: cool down and reset
            sim.temperature = Math.max(0.0, sim.temperature * 0.98 - 1.0);
            sim.containedEnergy *= 0.9;
            cursed$absorbers.clear();
            cursed$incomingSpk = 0;
            cursed$overloadTimer = 0;
            cursed$stabilization = 0;
            cursed$energyMod = 1.0;
            heat = 0;
            return;
        }

        int meltA = melt(a), meltB = melt(b);
        DfcCoreSim.Inputs in = new DfcCoreSim.Inputs();
        in.incomingSpk = cursed$incomingSpk;
        in.stabilization = cursed$stabilization;
        in.energyMod = cursed$energyMod;
        in.absorberLevels = new double[cursed$absorbers.size()];
        java.util.Arrays.fill(in.absorberLevels, 1.0); // ASSUMPTION: per-receiver "level" ported in HALO-B
        in.catalystPowerAbs = ItemCatalyst.getPowerAbs(a) + ItemCatalyst.getPowerAbs(b);
        in.catalystPowerMod = ItemCatalyst.getPowerMod(a) * ItemCatalyst.getPowerMod(b);
        in.catalystHeatMod = ItemCatalyst.getHeatMod(a) * ItemCatalyst.getHeatMod(b);
        in.catalystFuelMod = ItemCatalyst.getFuelMod(a) * ItemCatalyst.getFuelMod(b);
        in.meltingPoint = Math.min(DfcTuning.CATALYST_FALLBACK_MELT, Math.min(meltA, meltB));
        in.meltA = meltA; in.meltB = meltB;
        in.damageA = damage(a); in.damageB = damage(b);
        in.corePower = getCore();
        in.coreHeat = ItemAMSCore.getHeatBase(core);
        in.coreFuel = ItemAMSCore.getFuelBase(core);
        in.fuelEff0 = getFuelEfficiency(tanks[0].getTankType());
        in.fuelEff1 = getFuelEfficiency(tanks[1].getTankType());
        in.fill0 = tanks[0].getMaxFill() > 0 ? (double) tanks[0].getFill() / tanks[0].getMaxFill() : 0;
        in.fill1 = tanks[1].getMaxFill() > 0 ? (double) tanks[1].getFill() / tanks[1].getMaxFill() : 0;

        DfcCoreSim.Outputs out = sim.tick(in);

        // fuel
        tanks[0].setFill(Math.max(0, tanks[0].getFill() - out.fuelDrain));
        tanks[1].setFill(Math.max(0, tanks[1].getFill() - out.fuelDrain));
        // absorbers
        for (int i = 0; i < cursed$absorbers.size(); i++) cursed$absorbers.get(i).joules += out.receiverJoules[i];
        cursed$absorbers.clear();
        cursed$incomingSpk = 0;
        cursed$stabilization = 0;
        cursed$energyMod = 1.0;
        // catalyst wear
        a.getOrCreateTag().putDouble("damage", out.damageA);
        b.getOrCreateTag().putDouble("damage", out.damageB);
        // shocks
        if (out.shocks > 0) shock(level, out.shocks);
        // GUI heat bar (Continuum shows heat as 0..100 and syncs prevHeat)
        heat = (int) Math.max(0, Math.min(100, sim.temperature / in.meltingPoint * 100.0));
        // overload: a catalyst fully melted while hot -> countdown -> blast
        if ((out.damageA >= 100.0 || out.damageB >= 100.0) && sim.temperature > 100.0) {
            if (++cursed$overloadTimer > DfcTuning.OVERLOAD_TICKS) {
                heat = 100;
                detonate(level);
                level.removeBlock(getBlockPos(), false);
            }
        } else cursed$overloadTimer = 0;
    }

    @Unique private void shock(Level level, int count) {
        BlockPos p = getBlockPos();
        level.playSound(null, p, SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.BLOCKS, 2.0f, 1.2f);
        for (Entity e : level.getEntities((Entity) null, new AABB(p).inflate(4 + Math.min(count, 8)))) {
            e.hurt(ModDamageSources.amsCore(level), 20.0f * count);
        }
    }

    @Unique private static int melt(ItemStack s) {
        var key = ForgeRegistries.ITEMS.getKey(s.getItem());
        return key == null ? DfcTuning.CATALYST_FALLBACK_MELT : DfcTuning.meltingPoint(key.getPath());
    }
    @Unique private static double damage(ItemStack s) { return s.hasTag() ? s.getTag().getDouble("damage") : 0.0; }

    // ---- persistence (ints: avoids depending on DataOut having double support) ----
    @Inject(method = "saveData", at = @At("TAIL"))
    private void cursed$save(DataOut output, CallbackInfo ci) {
        output.putInt("cursedTemp", (int) cursed$sim().temperature);
        output.putInt("cursedEnergy", (int) cursed$sim().containedEnergy);
    }

    @Inject(method = "loadData", at = @At("TAIL"))
    private void cursed$load(DataIn input, CallbackInfo ci) {
        cursed$sim().temperature = input.getIntOr("cursedTemp", 0);
        cursed$sim().containedEnergy = input.getIntOr("cursedEnergy", 0);
    }

    // ---- IDfcCore ----
    @Override public void cursed$addAbsorber(TileEntityCoreReceiver r) { if (!cursed$absorbers.contains(r)) cursed$absorbers.add(r); }
    @Override public void cursed$stabilize(double stab, float lensEnergy) {
        cursed$stabilization += stab;
        cursed$energyMod = lensEnergy >= 1.0f ? cursed$energyMod + (lensEnergy - 1.0f) : cursed$energyMod * lensEnergy;
    }
    @Override public double cursed$getTemperature() { return cursed$sim().temperature; }
    @Override public double cursed$getContainedEnergy() { return cursed$sim().containedEnergy; }
    @Override public int cursed$getOverloadTimer() { return cursed$overloadTimer; }
}
