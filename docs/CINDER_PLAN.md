CINDER (v0.3): REACTORS PLAN  (revised after reading the ZIRNOX source)

KEY FINDING
The addon's reactors all run on its own fuel-rod system (LeafiaRodItem, 1,231 lines, 59 rod types,
in contents/control/fuel). The rewritten ZIRNOX only simulates those rods, so rods come first.

STEPS (each one a zip)
CINDER-A  DONE (written, headless-tested)
  fuel/RodSim      heat function: flux -> heat, cooling, depletion, melting, decay
  fuel/RodTypes    all 59 rod types (life, melting point, reactivity, emission, flags)
  fuel/RodType, RodState
CINDER-B  DONE (written): rod items: register the 59 rods, stack data (heat/depletion/...), tooltips, textures,
          depleted-rod decay products, rod crafting
CINDER-C  DONE (written): ZIRNOX rewrite (868-line TE mixin): hull temperature, pressure, valve, compression,
          control rod movement, debris/explosion; controls via commands until a GUI exists
CINDER-D  RBMK changes (9 classes)           CINDER-E  APR (11)
CINDER-F  LFTR/MSR (22)                      CINDER-G  PWR (38)

PER-STEP CHECKLIST
- Compare with Continuum's own version first
- Pure-Java simulation class + headless test, then the Minecraft-facing hook
- Registration, models, textures, lang, recipes; GUI work grouped into its own milestone
- Known gap: "Shitfuckium" rod uses a lookup table (ShitfuckiumData) not ported yet

TEST TOOLS
- java tools/RodCheck.java     rod heat behaviour per fuel and cooling level
- java tools/ZirnoxCheck.java  ZIRNOX scenarios (steady, rods out, low CO2 -> overheats and explodes)
HOW TO TRY CINDER-C
1. Creative tab "NTM Cursed Edition" -> put rods (e.g. MEU-235) in ZIRNOX rod slots, add CO2 and water.
2. Turn the reactor on (rods go to 100%) or use /cursed zirnox rods 50 while looking at the core block.
3. /cursed zirnox shows hull temperature, pressure, rods and valve.
