FORGE (v0.4): THE ADDON'S OTHER MACHINES  (plan + progress)
Counts are classes in the addon jar (blocks, entities, renderers, containers, GUIs).

DONE
  FORGE-A  RTG Heater (written, untested in game). Right-click with an RTG pellet (15 slots) or an addon rod (rod slot);
           empty hand shows status; sneak + empty hand takes items out. Heat buffer 750,000 that machines ABOVE it pull
           from (Continuum heat network, like its own heaters). A rod runs the CINDER rod simulation; a rod that
           "nukes" or spills too much blows the heater up. Placeholder texture. Skips the addon's own pellets
           (nc279, fissium), which are new items not ported yet.

  FORGE-B  HP Boiler (written, untested in game; math headless-tested with java tools/BoilerCheck.java).
           Boils a fluid through up to 4 chained heating steps (e.g. water -> steam -> denser steam) using Continuum's own
           heating data, pulls heat from a heat source directly BELOW it (the RTG Heater works), 128M heat buffer,
           fluid ports on all sides. /cursed boiler shows status; /cursed boiler fluid <NAME> [steps 1-4] chooses what
           it boils. Placeholder texture; the addon's sun heating and GUI/fluid-identifier slot are not in.

  FORGE-C  Coolant Heat Exchanger (written, untested in game; math headless-tested, java tools/HeatexCheck.java).
           Cools a hot "coolable" fluid and uses the heat to warm a coolant (default water -> steam). Needs power (5/tick,
           buffer 10,000). /cursed heatex shows status; /cursed heatex hot <NAME>, coolant <NAME>, cycle <amount> [delay].
           Uses Continuum fluids only: the addon's own hot air / hot water / fluoride fluids and its second fluid system
           are not ported. No GUI, filter slot or fan animation.

  FORGE-D  Medium Wind Turbine + global wind (written, untested in game; math headless-tested, java tools/WindCheck.java).
           Global wind (power 0-100 drifting every 150-350 ticks, calm/strong spells, wandering direction) ticks with the
           overworld and is saved with the world; /cursed wind shows it, operators can /cursed wind set <power> [angle].
           The turbine turns wind into power (about 280/t average with full lubricant, 750/t at most), needs lubricant
           (1 mB per 60 ticks, 1200 mB tank, fed through pipes), and makes nothing if blocks stand in the way within 500
           blocks upwind. Only the overworld has wind. Right-click shows its status. Plain cube for now: no blades, no
           spinning model, no sound.

FINDINGS THAT CHANGED THE ORDER
  - The Sol Blaster only processes the addon's own materials (chydalium, mysticite, fissite) plus a euphemium recipe, and
    needs the addon's solinium core machinery: not worth porting until those materials exist here. Skipped.
  - The AMSP Analyzer and Receiver are EMPTY shells in the addon (no logic, just blocks and models). Skipped for now.
  - The Control Torch only works through the control panel system (a separate 58-class family). Skipped until/unless
    the control panel is wanted.

FAMILIES, WHAT CONTINUUM ALREADY HAS, AND HOW BIG THE JOB IS
  family        addon classes   Continuum has                            job
  heat/HP Boiler       (in 10)  Industrial Heat Boiler                   compare, maybe a logic-only mixin (372-line entity)
  research (AMSP)          6    nothing                                  new, tiny entities (14 and 39 lines) but need AMS items
  panel/Control Torch      2    nothing                                  new, 97-line entity, signal block
  processing/Mixing Vat   14*   nothing                                  new machine: 415-line entity, recipes, fluids, items
  processing/Sol Blaster  (14*) nothing                                  new machine: 293-line entity, recipes
  processing/Salt Separator (14*) nothing                                632-line entity, needs addon molten-salt fluids
  misc/heat exchangers    56    Heater Heatex                            compare first, probably logic changes only
  misc/modular turbine    (56)  Large + Industrial turbines              a different design: new machine
  misc/wind turbines      (56)  nothing                                  new blocks and rendering
  elevators               39    Cargo Elevator (different)               large: blocks, shafts, entity movement
  controlpanel            58    nothing                                  large: panel blocks and screens, rendering
  (* = the 14 processing classes cover all three machines)

RECOMMENDED ORDER (updated)
1. DONE: RTG Heater, HP Boiler
2. DONE: Coolant Heat Exchanger, Wind Turbine. NEXT: Mixing Vat (415 lines, check what it needs first), then the modular turbine (47 classes)
3. Sol Blaster (293 lines) and Mixing Vat (415 lines)
4. Wind turbines, modular turbine
5. Elevators, control panel (big; do last, ask whether wanted at all)

RULES WE ARE FOLLOWING: command/right-click readouts first, GUIs later; machines sit beside Continuum's;
pure-Java simulation + headless test first when there is real logic.

QUESTIONS FOR YOU (they decide what is worth the effort)
- Which of these do you actually use in your modpack? Skipping the unused ones saves most of the work.
- Control panel and elevators are ~100 classes of mostly blocks and rendering: wanted, or skip?
