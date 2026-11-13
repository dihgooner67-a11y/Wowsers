CINDER (v0.3): REACTORS PLAN
Source: com/leafia/contents/machines/reactors in the 1.12.2 addon (82 classes)

ORDER (smallest first, so each zip is testable)
1. ZIRNOX   2 classes   - likely a mixin-style tweak to Continuum's existing ZIRNOX
2. RBMK     9 classes   - RealerSim column, debris smoke
3. APR      11 classes  - core, fluid IO ports, power outlet (multiblock)
4. LFTR/MSR 22 classes  - element, plug, control, arbitrary, ejector, salt separator
5. PWR      38 classes  - element, channel, exchanger, conductor, terminal, control,
                          port, computer, OCCS in/out, reflector, hull

PER-REACTOR WORK (same checklist each time)
- Compare with Continuum's own version first (Continuum already has several reactors)
- Block + BlockEntity registration, blockstates, models (1.12 OBJ/JSON -> 1.20.1)
- Server tick logic (port from decompiled source, math stays in plain Java like DfcCoreSim)
- GUI/container, fluid + heat + radiation hooks through Continuum's APIs
- Textures, lang, recipes

STATE
- Not started in code. Start after HALO builds, so the first compile errors are fixed once.
- Decompiled sources live only in the addon jar; each reactor is read out with CFR before porting.
