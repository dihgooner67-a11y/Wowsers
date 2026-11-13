package com.leafia.cursed.mixin;

import com.hbm.blocks.machine.MachineFieldDisturber;
import com.hbm.entity.ModNtmEntities;
import com.hbm.entity.effect.EntityCloudFleijaRainbow;
import com.hbm.entity.logic.EntityNukeExplosionMK5;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.items.machine.ItemCatalyst;
import com.hbm.items.special.ItemAMSCore;
import com.hbm.lib.ModDamageSources;
import com.hbm.tileentity.TileEntityMachineBase;
import com.hbm.tileentity.machine.dfc.TileEntityCore;
import com.hbm.tileentity.machine.dfc.TileEntityCoreReceiver;
import com.leafia.cursed.CursedItems;
import com.leafia.cursed.CursedSounds;
import com.leafia.cursed.dfc.BeamContext;
import com.leafia.cursed.dfc.DfcCoreSim;
import com.leafia.cursed.dfc.DfcTuning;
import com.leafia.cursed.dfc.IDfcCore;
import com.leafia.cursed.dfc.IDfcReceiver;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
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
 * EMBER + HALO-C1: the addon's core simulation, overload, jammer-delayed collapse, final phase and
 * crucifix / fix tool handling, driven through Continuum's TileEntityCore.
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

    @Unique private DfcCoreSim cursed$sim;
    @Unique private final List<TileEntityCoreReceiver> cursed$absorbers = new ArrayList<>();
    @Unique private double cursed$incomingSpk;
    @Unique private double cursed$stabilization;
    @Unique private double cursed$energyMod = 1.0;
    @Unique private int cursed$stabilizerCount, cursed$lastStabilizers;
    @Unique private boolean cursed$wasBoosted;
    // overload / collapse (not persisted: a reload resets an in-progress countdown)
    @Unique private int cursed$overloadTimer;
    @Unique private BlockPos cursed$jammerPos;
    @Unique private double cursed$explosionIn = -1.0;
    @Unique private double cursed$collapsing;
    @Unique private boolean cursed$finalPhase;

    protected CoreMixin(BlockEntityType<?> type, BlockPos pos, BlockState state, int slots) {
        super(type, pos, state, slots);
    }

    @Unique private DfcCoreSim cursed$sim() {
        if (cursed$sim == null) cursed$sim = new DfcCoreSim(new Random());
        return cursed$sim;
    }

    // ---- beam input ----
    @Inject(method = "burn", at = @At("HEAD"), cancellable = true)
    private void cursed$burn(long joules, CallbackInfoReturnable<Long> cir) {
        if (!isReady()) return;
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

    // ---- main tick ----
    @Inject(method = "tickServer", at = @At("HEAD"))
    private void cursed$tick(Level level, CallbackInfo ci) {
        if (level.isClientSide) return;
        DfcCoreSim sim = cursed$sim();
        ItemStack a = inventory.getStackInSlot(0), core = inventory.getStackInSlot(1), b = inventory.getStackInSlot(2);
        boolean active = a.getItem() instanceof ItemCatalyst && b.getItem() instanceof ItemCatalyst
                      && core.getItem() instanceof ItemAMSCore;

        if (!active) {
            sim.temperature = Math.max(0.0, sim.temperature * 0.98 - 1.0);
            sim.containedEnergy *= 0.9;
            cursed$absorbers.clear();
            cursed$incomingSpk = 0;
            cursed$resetInputs();
            cursed$resetCollapse();
            heat = 0;
            return;
        }

        int meltA = melt(a), meltB = melt(b);
        DfcCoreSim.Inputs in = new DfcCoreSim.Inputs();
        in.incomingSpk = cursed$incomingSpk;
        in.stabilization = cursed$stabilization;
        in.energyMod = cursed$energyMod;
        in.collapsing = cursed$collapsing;
        in.finalPhase = cursed$finalPhase;
        in.absorberLevels = new double[cursed$absorbers.size()];
        for (int i = 0; i < in.absorberLevels.length; i++)
            in.absorberLevels[i] = ((IDfcReceiver) cursed$absorbers.get(i)).cursed$getLevel();
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

        tanks[0].setFill(Math.max(0, tanks[0].getFill() - out.fuelDrain));
        tanks[1].setFill(Math.max(0, tanks[1].getFill() - out.fuelDrain));
        if (cursed$finalPhase) { // receivers cannot take the load any more
            for (TileEntityCoreReceiver r : new ArrayList<>(cursed$absorbers)) ((IDfcReceiver) r).cursed$explode();
        } else {
            for (int i = 0; i < cursed$absorbers.size(); i++) cursed$absorbers.get(i).joules += out.receiverJoules[i];
        }
        cursed$absorbers.clear();
        cursed$wasBoosted = cursed$incomingSpk > 0;
        cursed$incomingSpk = 0;
        cursed$lastStabilizers = cursed$stabilizerCount;
        cursed$resetInputs();
        a.getOrCreateTag().putDouble("damage", out.damageA);
        b.getOrCreateTag().putDouble("damage", out.damageB);
        if (out.shocks > 0) shock(level, out.shocks);
        heat = (int) Math.max(0, Math.min(100, sim.temperature / in.meltingPoint * 100.0));

        // ---- overload -> (jammer?) collapse -> explosion ----
        boolean overload = ((out.damageA >= 100.0 || out.damageB >= 100.0) && sim.temperature > 100.0) || cursed$finalPhase;
        if (overload) {
            if (cursed$handleOverload(level)) return; // core destroyed
        } else if (cursed$explosionIn >= 0.0) {
            cursed$resetCollapse();
        } else cursed$overloadTimer = 0;

        if (cursed$collapsing > 0.0) cursed$handleFixTools(level);
    }

    /** @return true if the core detonated this tick */
    @Unique private boolean cursed$handleOverload(Level level) {
        if (cursed$jammerPos != null && !(level.getBlockState(cursed$jammerPos).getBlock() instanceof MachineFieldDisturber))
            cursed$jammerPos = null;

        if (cursed$jammerPos == null) {
            if (cursed$overloadTimer <= DfcTuning.OVERLOAD_TICKS) {
                if (cursed$overloadTimer == 0)
                    level.playSound(null, getBlockPos(), SoundEvents.BEACON_DEACTIVATE, SoundSource.BLOCKS, 10.0f, 0.5f);
                cursed$overloadTimer++;
            } else {
                BlockPos jammer = cursed$findJammer(level);
                if (jammer == null) { cursed$blast(level); return true; }
                cursed$jammerPos = jammer;
                if (cursed$explosionIn < 0.0) {
                    cursed$explosionIn = DfcTuning.COLLAPSE_SECONDS;
                    level.playSound(null, getBlockPos(), SoundEvents.WITHER_SPAWN, SoundSource.BLOCKS, 20.0f, 0.5f);
                }
            }
        }
        if (cursed$jammerPos != null) {
            cursed$explosionIn = Math.max(cursed$explosionIn - 0.05, 0.0); // one server tick = 0.05 s
            cursed$collapsing = Math.max(0.0, Math.min(1.0, 1.0 - cursed$explosionIn / DfcTuning.COLLAPSE_SECONDS));
            if (cursed$explosionIn <= DfcTuning.FINAL_PHASE_SECONDS && !cursed$finalPhase) {
                cursed$finalPhase = true;
                level.playSound(null, getBlockPos(), SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.BLOCKS, 50.0f, 0.5f);
            }
            if (cursed$explosionIn <= 0.0) { cursed$blast(level); return true; }
        }
        return false;
    }

    @Unique private void cursed$resetCollapse() {
        cursed$jammerPos = null;
        cursed$explosionIn = -1.0;
        cursed$collapsing = 0.0;
        cursed$overloadTimer = 0;
        cursed$finalPhase = false;
    }

    @Unique private void cursed$resetInputs() {
        cursed$stabilization = 0;
        cursed$energyMod = 1.0;
        cursed$stabilizerCount = 0;
    }

    /** Same explosion + rainbow cloud as Continuum's detonate(), but sized like the addon: 20 + temperature^0.4. */
    @SuppressWarnings({"unchecked", "rawtypes"})
    @Unique private void cursed$blast(Level level) {
        BlockPos pos = getBlockPos();
        int size = Math.min(DfcTuning.MAX_BLAST, 20 + (int) Math.pow(cursed$sim().temperature, 0.4));
        EntityNukeExplosionMK5 explosion = EntityNukeExplosionMK5.statFacNoRad(level, size, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
        if (level.addFreshEntity(explosion)) explosion.startChunkLoading();
        level.playSound(null, pos, SoundEvents.GENERIC_EXPLODE, SoundSource.BLOCKS, 100000.0f, 1.0f);
        EntityType<? extends EntityCloudFleijaRainbow> type = (EntityType) ModNtmEntities.CLOUD_RAINBOW.get();
        EntityCloudFleijaRainbow cloud = new EntityCloudFleijaRainbow(type, level);
        cloud.setMaxAge(size);
        cloud.moveTo(pos.getX(), pos.getY(), pos.getZ(), 0.0f, 0.0f);
        level.addFreshEntity(cloud);
        level.removeBlock(pos, false);
    }

    /** ASSUMPTION: Continuum has no jam logic, so any Field Disturber block within range counts as the jammer. */
    @Unique private BlockPos cursed$findJammer(Level level) {
        BlockPos c = getBlockPos();
        int cx = c.getX() >> 4, cz = c.getZ() >> 4, r = DfcTuning.JAMMER_RANGE_CHUNKS;
        for (int dx = -r; dx <= r; dx++) for (int dz = -r; dz <= r; dz++) {
            LevelChunk chunk = level.getChunkSource().getChunkNow(cx + dx, cz + dz);
            if (chunk == null) continue;
            LevelChunkSection[] sections = chunk.getSections();
            for (int i = 0; i < sections.length; i++) {
                LevelChunkSection sec = sections[i];
                if (sec.hasOnlyAir() || !sec.maybeHas(st -> st.getBlock() instanceof MachineFieldDisturber)) continue;
                int baseY = (chunk.getMinSection() + i) << 4;
                for (int x = 0; x < 16; x++) for (int y = 0; y < 16; y++) for (int z = 0; z < 16; z++) {
                    if (sec.getBlockState(x, y, z).getBlock() instanceof MachineFieldDisturber)
                        return new BlockPos((chunk.getPos().x << 4) + x, baseY + y, (chunk.getPos().z << 4) + z);
                }
            }
        }
        return null;
    }

    /** Crucifix / fix tool thrown into a collapsing core (addon rules). */
    @Unique private void cursed$handleFixTools(Level level) {
        BlockPos p = getBlockPos();
        for (ItemEntity e : level.getEntitiesOfClass(ItemEntity.class, new AABB(p).inflate(6.0))) {
            Item item = e.getItem().getItem();
            boolean fixTool = item == CursedItems.FIX_TOOL.get();
            boolean crucifix = item == CursedItems.CRUCIFIX.get();
            if (!fixTool && !crucifix) continue;
            e.discard();
            level.explode(null, p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5, 20.0f, Level.ExplosionInteraction.NONE);
            boolean fails = cursed$finalPhase || cursed$wasBoosted
                || (crucifix && level.random.nextInt(100) < DfcTuning.CRUCIFIX_BASE_FAIL - cursed$lastStabilizers * DfcTuning.CRUCIFIX_FAIL_PER_STABILIZER);
            if (fails) {
                level.playSound(null, p, CursedSounds.CRUCIFIX_FAIL.get(), SoundSource.BLOCKS, 20.0f, 1.0f);
                continue;
            }
            cursed$sim().temperature = 0.0;
            cursed$sim().containedEnergy = 0.0;
            tanks[0].setFill(0);
            tanks[1].setFill(0);
            level.playSound(null, p, CursedSounds.CRUCIFIX.get(), SoundSource.BLOCKS, 20.0f, 1.0f);
        }
    }

    @Unique private void shock(Level level, int count) {
        BlockPos p = getBlockPos();
        level.playSound(null, p, SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.BLOCKS, 2.0f, 1.2f);
        for (Entity e : level.getEntities((Entity) null, new AABB(p).inflate(4 + Math.min(count, 8)))) {
            if (e instanceof ItemEntity ie && (ie.getItem().getItem() == CursedItems.FIX_TOOL.get()
                    || ie.getItem().getItem() == CursedItems.CRUCIFIX.get())) continue; // the fix items survive
            e.hurt(ModDamageSources.amsCore(level), 20.0f * count);
        }
    }

    @Unique private static int melt(ItemStack s) {
        var key = ForgeRegistries.ITEMS.getKey(s.getItem());
        return key == null ? DfcTuning.CATALYST_FALLBACK_MELT : DfcTuning.meltingPoint(key.getPath());
    }
    @Unique private static double damage(ItemStack s) { return s.hasTag() ? s.getTag().getDouble("damage") : 0.0; }

    // ---- persistence ----
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
        cursed$stabilizerCount++;
        cursed$energyMod = lensEnergy >= 1.0f ? cursed$energyMod + (lensEnergy - 1.0f) : cursed$energyMod * lensEnergy;
    }
    @Override public double cursed$getTemperature() { return cursed$sim().temperature; }
    @Override public double cursed$getContainedEnergy() { return cursed$sim().containedEnergy; }
    @Override public int cursed$getOverloadTimer() { return cursed$overloadTimer; }
}
