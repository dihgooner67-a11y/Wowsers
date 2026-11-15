LFTR / MOLTEN SALT REACTOR (CINDER-F1): WHAT EXISTS AND HOW TO TRY IT

BLOCKS (creative tab "NTM Cursed Edition"): LFTR Fuel Element, LFTR Control Rod Drive, LFTR Control Rod Extension.
Textures are placeholders borrowed from the APR blocks.

THE SIMULATION (ported from MSRTEBase + MSRElementTE, tested headless: java tools/MsrCheck.java)
- Each Fuel Element holds up to 1000 mB of salt with a fuel mixture and a heat value.
- Fuel types: th232, pa233, u233, u235, pa234. Th-232 breeds Pa-233 -> Pa-234 / U-233 -> U-235, as in the addon.
- Elements react with other elements diagonally (3 planes) and in straight lines up to 4 blocks away.
  A graphite block (any block whose id contains "graphite") on the way moderates the reaction (x4).
  Radiation-resistant blocks block lines of sight. A lone element does nothing: you need neighbours.
- Neighbouring elements share salt, fuel mixture and heat. Heat bleeds off 1% per tick.
- Above 5400 heat a cell can breach (1 in 350 per tick) and turns into a magma block.
- Control rods: place a Control Rod Drive facing the elements, extension blocks BEHIND it set its length.
  Insertion restricts the elements in front of it one by one.

TRYING IT (look at the block, within 8 blocks)
1. Build a plane of 3x3 or bigger Fuel Elements, graphite blocks around/between them if you want moderation.
2. /cursed msr fuel u235 20     (on each element: fills it with salt carrying 20% u235; debug loading)
3. /cursed msr                  (readout: salt, heat, restriction, mixture)
4. Control rod: /cursed msr control 70 (percent of its length) while looking at the drive. Compare heat.
5. /cursed msr empty            (empties an element)

NOT IN YET (the rest of the addon's LFTR)
- Taking power or heat OUT: the Ejector, Plug and heat-exchanger connection need the addon's molten-salt fluids,
  which Continuum has no counterpart for. Right now heat is only visible, not usable.
- Salt Separator machine (632-line machine with recipes, GUI) that turns items into fuel salt.
- Arbitrary-block wrapper (disguise block), GUIs, renderers, real textures, breach slag block.
- BASE_TEMPERATURE = 600 is an assumption (the addon read it from its fluoride fluid).
