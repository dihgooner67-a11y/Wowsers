# Port notes

## How the addon maps onto Continuum
| 1.12.2 addon | Continuum 1.20.1 |
|---|---|
| Mixin into HBM CE TileEntityCore (1667 lines) | CoreMixin + DfcCoreSim (pure math) |
| Emitter adds SPK to core directly | core.burn() hook accumulates beam joules * 1e-6 |
| Core finds absorbers itself | BeamContext + ReceiverMixin: receiver hit right after the core links itself |
| Stabilizer raises "stabilization" | Stabilizer already writes core.field (0..100); scaled by DfcTuning.STABILIZATION_PER_FIELD |
| Catalyst melt table (LCEItemCatalyst) | DfcTuning.meltingPoint(), same values, keyed by registry path (names match Continuum) |

## Assumptions that need in-game checking
1. STABILIZATION_PER_FIELD = 0.1 (field 0..100 -> stabilization 0..10). Headless sim: stab 0 overheats and melts catalysts, stab >= 2 is stable.
2. Absorber "level" is fixed at 1.0 until HALO-B.
3. Catalyst wear uses NBT key "damage" on the catalyst stack.
4. Overload explosion uses Continuum's detonate() size (fill-based), not the addon's temperature-based radius.
5. Shocks damage entities in a radius with Continuum's amsCore damage source (addon spawned bolt entities).

## Likely first build errors
- Mixin target names (`tickServer`, `burn`, `fire`, `addEnergy`, `radiation`, `shouldDetonate`, `saveData`, `loadData`) were read from the decompiled Continuum 1.0.5 jar; a different Continuum version may differ.
- `@Shadow` on private `detonate(Level)` declared `protected abstract`.
- fg.deobf of the local jars may need a different notation depending on ForgeGradle version.

## Testing the math without Minecraft
`java tools/SimCheck.java` runs the simulation headless with a Continuum-scale emitter beam.
