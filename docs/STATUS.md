CURSED EDITION -> CONTINUUM PORT: MILESTONES

SEED (v0.0)      DONE (untested build)
  Gradle/Forge 1.20.1 skeleton, mods.toml, mixin config, libs setup

EMBER (v0.1)     WRITTEN (untested in game)
  CoreMixin: addon heat/energy simulation drives Continuum's TileEntityCore
  - beam power in, fuel drain, absorber payout, shocks, catalyst wear (NBT "damage")
  - overload countdown (120 ticks) then Continuum's own detonate()
  - Continuum's old heat>=field detonation and passive radiation disabled
  - temperature/energy saved to NBT
  NOT yet: jammer (field disturber) logic, final-phase sequence, screen shake/flash,
  looped overload sound, spinning-ring/light client rendering, new GUI readouts

HALO (v0.2)      PARTIAL = "HALO-A"
  DONE (written):
  - Receiver <-> core linking (beam passes core, next receiver becomes absorber)
  - Stabilizer feeds core stabilization (via core.field)
  TODO (HALO-B):
  - Emitter changes (cryogel/perfluoromethyl coolant, SPK output)
  - Injector changes (energy boost)
  - Receiver "level", destruction level, shrapnel entities
  - New blocks: exchanger, core emitter (cemitter), pulser/detonator, lens, crucifix
  - DFC models/textures converted to 1.20.1 format

CINDER (v0.3)    reactors/power        FORGE (v0.4)   other machines
RELIC (v0.5)     items/gear/fluids     BRICK (v0.6)   building/network
WASTELAND (v0.7) worldgen/bombs/effects SCALPEL (v0.8) mixin triage
CURSED (v1.0)    recipes, lang, JEI, release
