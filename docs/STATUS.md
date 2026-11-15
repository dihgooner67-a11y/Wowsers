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
  D1 RBMK column damage (written): columns no longer melt the instant they pass max heat; overheating
     builds damage (2+ per tick, more above 3000), cooling heals 1/tick, meltdown above 1200.
     Fuel columns' instant-meltdown check is neutralised. /cursed rbmk shows heat and damage.
  D2 RBMK (written): auto control rod SCRAM latch (rod stays inserted until acknowledged; saved),
     /cursed rbmk scram | acknowledge (look at any column; flood-fills connected columns),
     damaged columns vent smoke/flame particles (stand-in for the jumping-column jets)
  NOT in RBMK (needs client/GUI work): jumping-column animation, debris smoke block, console SCRAM button,
  auto-rod GUI, "rod is shut off" console marker, RealerSim column block
  E1 APR simulation (written, headless-tested): apr/AprSim = ring tick (control rods, particle count,
     oxygen->LOX conversion, chamber blankets, rps, hull temp, damage, failure) and power output formula.
     No machine yet: blocks, assembly check and the failure explosion come with E2.
  E2a APR machine (written, untested in game): 10 blocks, core block entity (AprSim + tanks + chamber tanks +
     Continuum power/fluid ports), full structure check ported from the addon (torus rings, discs, coils),
     assemble/disassemble, failure explosions, /cursed apr ... commands. See docs/APR_GUIDE.md.
  F1 LFTR slice (written, simulation headless-tested): lftr/MsrSim (fuel mixtures, decay, breeding chain,
     reactions, moderation, salt/heat sharing, breach), fuel element + control rod drive + extension blocks,
     /cursed msr readouts, fuel loading and control. See docs/LFTR_GUIDE.md.
  CINDER WRAP-UP: reactors done = DFC core+parts (HALO), ZIRNOX, RBMK, APR, LFTR slice.
  CINDER LEFTOVERS (carried forward, not forgotten):
    - addon PWR (38 files, 8,016 lines) alongside Continuum's PWR: NOT STARTED
    - LFTR heat/power output (ejector, plug, molten-salt fluids) and Salt Separator
    - APR polish (GUI, port blocks, particle items, 3D model)
    - rod crafting table + JEI, ZIRNOX GUI/compression/debris, RBMK client effects and RealerSim column
FORGE (v0.4)     other machines
RELIC (v0.5)     items/gear/fluids      BRICK (v0.6)      building/network
WASTELAND (v0.7) worldgen/bombs/effects SCALPEL (v0.8)    mixin triage
CURSED (v1.0)    recipes, lang, JEI, release
