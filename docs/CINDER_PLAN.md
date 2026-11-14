CINDER (v0.3): REACTORS PLAN  (revised after reading the ZIRNOX source)

KEY FINDING
The addon's reactors all run on its own fuel-rod system (LeafiaRodItem, 1,231 lines, 59 rod types,
in contents/control/fuel). The rewritten ZIRNOX only simulates those rods, so rods come first.

STEPS (each one a zip)
CINDER-A  DONE (written, headless-tested)
  fuel/RodSim      heat function: flux -> heat, cooling, depletion, melting, decay
  fuel/RodTypes    all 59 rod types (life, melting point, reactivity, emission, flags)
  fuel/RodType, RodState
CINDER-B  rod items: register the 59 rods, stack data (heat/depletion/...), tooltips, textures,
          depleted-rod decay products, rod crafting
CINDER-C  ZIRNOX rewrite (868-line TE mixin): hull temperature, pressure, valve, compression,
          control rod movement, debris/explosion; controls via commands until a GUI exists
CINDER-D  RBMK changes (9 classes)           CINDER-E  APR (11)
CINDER-F  LFTR/MSR (22)                      CINDER-G  PWR (38)

PER-STEP CHECKLIST
- Compare with Continuum's own version first
- Pure-Java simulation class + headless test, then the Minecraft-facing hook
- Registration, models, textures, lang, recipes; GUI work grouped into its own milestone
- Known gap: "Shitfuckium" rod uses a lookup table (ShitfuckiumData) not ported yet
