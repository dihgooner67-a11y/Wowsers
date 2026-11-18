REMAINING REACTORS (CINDER-E/F/G): what they really are
Sizes are the addon's decompiled source (lines), blocks/TE/GUI/render all included.

APR      11 files   2,643 lines   multiblock core (1,039-line TE: chambers, fluid conversion tables,
                                   heat/RPS simulation), fluid IO ports, power outlet, custom GUI, renderer
LFTR/MSR 22 files   3,807 lines   molten salt reactor: element, plug, control, ejector, separator, GUI
PWR      38 files   8,016 lines   Continuum already ships its OWN PWR (controller, fuel, moderator trait).
                                   The addon's is a different design, so porting it would add a second PWR.

WHY THESE ARE NOT QUICK
Each is a new multiblock machine: block + block entity registration, multiblock assembly check,
fluid ports, GUI/menu, models and textures. The DFC and ZIRNOX worked because Continuum already had
the machine and we only changed its logic. APR and LFTR have no Continuum counterpart.

RECOMMENDED ORDER
1. APR simulation as plain Java first (energy/heat tables, chamber logic, ~300 lines) + headless test
2. APR machine shell: blocks, core entity, assembly, ports, outlet  (needs a decision on GUI approach)
3. LFTR the same way
4. PWR: decide whether to port at all, or only the rods/fuel compatibility with Continuum's PWR

DECISIONS NEEDED FROM YOU
- GUI approach for new machines: commands + status readouts (fast), or full GUIs (slow, needs in-game iteration)
- Whether the addon's PWR is wanted alongside Continuum's

DECISIONS (from you)
1. New machines get command readouts first (like /cursed core, /cursed zirnox), full GUIs later.
2. The addon's PWR is ported ALONGSIDE Continuum's own PWR (separate blocks, no replacement).

PROGRESS
- E1 DONE (written): apr/AprSim + tools/AprCheck.java (java tools/AprCheck.java).
  Test: control 10% -> peak 8.7 rps, 40% -> 16 rps, hot coolant -> 42 rps, hot + chambers at 100% -> 70 rps
  (110 rps is the damage threshold), power output scales from about 87k to 696k per tick, no NaN.
- E2 NEXT: APR machine shell. Needs: component blocks (plating, dark plating, mechanical, coil, hull,
  fluid IO port, power outlet), the core block entity, the multiblock assembly check (disc up to radius 32,
  6 blocks tall), fluid ports, the failure explosion (LCA nuke + torex have no Continuum equivalent yet),
  and /cursed apr readout. First step is studying how Continuum registers a multiblock block entity.
- F: LFTR (22 files). G: addon PWR (38 files), registered under its own ids next to Continuum's PWR.

UPDATE (wrap-up of CINDER)
- E2a APR machine and F1 LFTR slice are written. PWR (G) is the one reactor family not started.
- PWR recommendation: port in two steps. (1) the PWR fuel rod / heat-up logic so addon rods work in Continuum's PWR
  (small, like ZIRNOX), (2) the addon's own PWR multiblock (large, own milestone).
