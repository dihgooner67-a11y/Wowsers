FORGE (v0.4): THE ADDON'S OTHER MACHINES
Counts are classes in the addon jar (blocks, entities, renderers, containers, GUIs).

heat        10   HP Boiler, RTG Heater (heater with 15 RTG pellet slots + 1 rod slot, 750k heat buffer)
research     6   AMSP Analyzer, AMSP Receiver
processing  14   Mixing Vat, Sol Blaster (recipes + JEI), Salt Separator
panel        2   Control Torch (97-line entity)
misc        56   heat exchangers (heatex), modular turbine, wind turbines
elevators   39   elevator blocks and shafts
controlpanel 58  control panel blocks and screens
Entity code sizes: AMSP Analyzer 14 lines, AMSP Receiver 39, Control Torch 97, RTG Heater 234, Sol Blaster 293,
HP Boiler 372, Mixing Vat 415.

DECISIONS ALREADY MADE: command readouts first, GUIs later; machines sit beside Continuum's, not replacing them.

PROPOSED ORDER (small and self-contained first)
1. RTG Heater: ties into CINDER (it takes our rod items) and Continuum's heat network. Needs an item inventory;
   without a GUI, items go in by right-click and out by shift-right-click on the block.
2. Control Torch, AMSP Analyzer/Receiver (tiny entities, mostly signal plumbing)
3. Sol Blaster, HP Boiler, Mixing Vat (recipes, fluids, items: each is a real machine)
4. Heat exchangers, modular turbine, wind turbines
5. Elevators, control panel (large, mostly blocks and rendering)

OPEN QUESTION: which of these do you actually use? Starting with the ones you care about saves a lot of work.
