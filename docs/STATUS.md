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

HALO (v0.2)      SERVER LOGIC COMPLETE = "HALO-C2" (needs a game test)
  DONE (written):
  - Receiver <-> core linking (front face only), receiver "level" (saved) and destruction
  - Stabilizer: watts^5 * drain, lens field/energy modifiers, 1e13 buffer; 5 addon lenses
  - Crucifix + fix tool items and their collapse behaviour
  - Overload -> blast, or Field Disturber collapse (120 s, final phase at 15 s)
  - Heat zone damage around a hot core, lethal inner box and 150-block entity pull while collapsing
  - Commands: /cursed core (readout of the core you look at), /cursed receiver [0..1] (show/set level)
  NOT in HALO (moved to CORONA, v0.2.5):
  - New blocks with GUIs/models: exchanger, core emitter (cemitter), pulser/detonator,
    osmiridium block, shrapnel entity
  - Client visuals: screen shake, blast particles, rings/lights, looped sounds, GUI readouts
  - Emitter/injector extras (beam burns blocks, sounds/control events), AMS rework
  - Hazmat/HEV damage immunity in the heat zone (Continuum armor API not checked yet)

CORONA (v0.2.5)  PLANNED  (the items above)

CINDER (v0.3)    A, B, C WRITTEN (untested in game)
  A  rod heat simulation (RodSim) + 59 rod types
  B  59 rod items cursed:rod_<id> (NBT heat/depletion like the addon, bar, tooltip, tinted overlay,
     depleted-rod decay products), own creative tab "NTM Cursed Edition"
  C  ZIRNOX rewrite (ZirnoxSim + ZirnoxMixin): used automatically when the reactor holds addon rods;
     Continuum's own rods keep Continuum's tick. Commands: /cursed zirnox [rods <0-100|auto>] [valve <bool>]
  NOT yet: rod crafting table + JEI, addon-style billet-composited rod art, inventory heat/radiation,
  ZIRNOX GUI dials/valve/compression, debris entities, Shitfuckium lookup table
  NEXT: CINDER-D RBMK, E APR, F LFTR/MSR, G PWR   (see docs/CINDER_PLAN.md)
FORGE (v0.4)     other machines
RELIC (v0.5)     items/gear/fluids      BRICK (v0.6)      building/network
WASTELAND (v0.7) worldgen/bombs/effects SCALPEL (v0.8)    mixin triage
CURSED (v1.0)    recipes, lang, JEI, release
