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

HALO (v0.2)      IN PROGRESS = "HALO-C1" (build of HALO-A/B confirmed compiling on GitHub; not run in game yet)
  DONE (written):
  - Receiver <-> core linking; receiver "level" (default 1.0, saved) and destruction
    (>= 1e11 joules/tick builds damage, then 1%/tick explosion)
  - Stabilizer: watts^5 * drain, lens field/energy modifiers, 1e13 buffer
  - 5 addon lenses; crucifix + fix tool items
  - Overload countdown (120 ticks) -> blast sized 20 + temp^0.4, or, if a Field Disturber is in range,
    120 s collapse with final phase at 15 s (receivers explode, no payout), then blast
  - Crucifix / fix tool thrown into a collapsing core (survival 80% fail - 10% per stabilizer;
    always fails in final phase or while a beam is boosting)
  TODO (HALO-C2):
  - Collapse side effects: pulling entities/blocks in, vaporization/heat damage around the core
  - Client: screen shake, DFC blast particles, rings/lights, looped sounds, GUI readouts
  - Receiver level slider (GUI) and wrong-side beam explosion
  - Emitter extras (beam burns blocks/entities), injector extras (sounds/control events)
  - New blocks: exchanger, core emitter (cemitter), pulser/detonator, osmiridium block, shrapnel entity
  - AMS rework (powercores/ams: base, emitter, stabilizer blocks)

CINDER (v0.3)    PLANNED, see docs/CINDER_PLAN.md   (reactors: ZIRNOX, RBMK, APR, LFTR, PWR)
FORGE (v0.4)     other machines
RELIC (v0.5)     items/gear/fluids      BRICK (v0.6)      building/network
WASTELAND (v0.7) worldgen/bombs/effects SCALPEL (v0.8)    mixin triage
CURSED (v1.0)    recipes, lang, JEI, release
