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

HALO (v0.2)      PARTIAL = "HALO-B"
  DONE (written, untested):
  - Receiver <-> core linking (beam passes core, next receiver becomes absorber)
  - Stabilizer: addon power demand (watts^5 * drain), lens field/energy modifiers, 1e13 buffer
  - 5 addon lenses (blank, limiter, booster, omega, safe) with models/textures/lang
  - Crucifix + DFC fix tool as items (tooltips only; behaviour needs collapse logic)
  TODO (HALO-C):
  - Emitter changes (coolant, SPK output), injector changes
  - Receiver level / destruction, shrapnel entity
  - Jammer + collapse sequence, crucifix/fix-tool behaviour, final phase
  - New blocks: exchanger, core emitter (cemitter), pulser/detonator, osmiridium block
  - GUIs, particles, core/component renderers, sounds
  - AMS rework (powercores/ams: base, emitter, stabilizer blocks)

CINDER (v0.3)    PLANNED, see docs/CINDER_PLAN.md   (reactors: ZIRNOX, RBMK, APR, LFTR, PWR)
FORGE (v0.4)     other machines
RELIC (v0.5)     items/gear/fluids      BRICK (v0.6)      building/network
WASTELAND (v0.7) worldgen/bombs/effects SCALPEL (v0.8)    mixin triage
CURSED (v1.0)    recipes, lang, JEI, release
