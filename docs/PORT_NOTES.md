# Port notes

## How the addon maps onto Continuum
| 1.12.2 addon | Continuum 1.20.1 |
|---|---|
| Mixin into HBM CE TileEntityCore (1667 lines) | CoreMixin + DfcCoreSim (pure math) |
| Emitter adds SPK to core directly | core.burn() hook accumulates beam joules * 1e-6 |
| Core finds absorbers itself | BeamContext + ReceiverMixin: receiver hit right after the core links itself |
| Stabilizer + lens | StabilizerMixin settles watts^5*drain and calls core.cursed$stabilize(); 5 lenses registered in CursedItems |
| Catalyst melt table (LCEItemCatalyst) | DfcTuning.meltingPoint(), same values, keyed by registry path (names match Continuum) |

## Assumptions that need in-game checking
1. Stabilization now comes from lens modifiers (fieldMod * watts/100 per stabilizer), as in the addon. Headless sim: stabilization 0 overheats and melts catalysts, >= 2 is stable. Continuum's own `core.field` is no longer used by the simulation.
2. Absorber "level" is fixed at 1.0 until HALO-B.
3. Catalyst wear uses NBT key "damage" on the catalyst stack.
4. Overload blast uses the addon's radius (20 + temperature^0.4), capped at DfcTuning.MAX_BLAST = 250, with Continuum's MK5 explosion and rainbow cloud.
4b. Continuum has no jammer logic, so ANY Field Disturber block within 8 chunks of the core counts as a jammer (DfcTuning.JAMMER_RANGE_CHUNKS). The addon used HBM CE's own jam check.
4c. An in-progress collapse is not saved: reloading the world resets the countdown.
5. Shocks damage entities in a radius with Continuum's amsCore damage source (addon spawned bolt entities).

## Likely first build errors
- Mixin target names (`tickServer`, `burn`, `fire`, `addEnergy`, `radiation`, `shouldDetonate`, `saveData`, `loadData`) were read from the decompiled Continuum 1.0.5 jar; a different Continuum version may differ.
- `@Shadow` on private `detonate(Level)` declared `protected abstract`.
- fg.deobf of the local jars may need a different notation depending on ForgeGradle version.

## Testing the math without Minecraft
`java tools/SimCheck.java` runs the simulation headless with a Continuum-scale emitter beam.

## Continuum facts learned
- Item wear uses Continuum's data-component wrapper (`ItemLens.get/setLensDamage`), not plain NBT. Catalyst wear still writes NBT key "damage" (ASSUMPTION: Continuum's ItemCatalyst has no wear of its own, so nothing conflicts; check in game).
- Continuum ships a single stabilizer lens (`ams_lens`); the addon's five lenses are new items here. `ams_lens` is treated as modifiers 1/1/1.
- Mixin targets in Continuum: stabilizer `lens()` is private and shadowed as protected abstract.
