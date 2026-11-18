DFC CRYSTAL CORE: HOW IT WORKS AND HOW TO CUSTOMISE IT

WHAT YOU SEE
- The DFC is drawn as a faceted gem (six-sided double pyramid with a band), not Continuum's orb. It spins slowly,
  bobs, pulses, and grows with the fuel level. With no fuel burning (standby) it is a small dark grey gem.
- Two glow layers: the bright gem and a faint larger shell around it.

MELTDOWN
- While the core is overloading, the crystal CRACKS: bright crack lines appear and multiply as the overload countdown
  (120 ticks) or the jammer collapse (120 s) progresses. From about 70% the facets drift apart and shake.
- When the core detonates, the crystal SHATTERS: up to 160 plasma shards fly out in every direction with a flame/end-rod
  trail. Each shard turns every block it hits into LAVA and loses 30% of its speed each time (it burns out after
  10 melts or 12 seconds). Water is boiled away. Entities near a shard take damage and catch fire. Bedrock-type blocks
  stop a shard. At most 600 shards exist at once. Then the usual explosion and rainbow cloud follow.
  (Tuning: PlasmaShards.MAX_SHARDS / MAX_MELTS / MAX_LIFE.)

COLOURS (creative setting)
- DFC Color Tuner (creative tab "NTM Cursed Edition"): right-click a DFC core to step through presets, sneak +
  right-click to go back. Works in creative mode only. 16 presets: Default (Continuum's fuel colour), 9 plain colours,
  5 two-colour gradients (first colour at the top), Rainbow.
- Exact colours (creative mode or operator), look at the core:
    /cursed dfc                              shows the current look
    /cursed dfc color default | rainbow
    /cursed dfc color solid #FF8800
    /cursed dfc color gradient #FF3030 #4070FF     (top colour, bottom colour)
    /cursed dfc crystal false                back to Continuum's orb (and no shards)
- The look is saved with the block and synced to all players.

KNOWN LIMITS
- The renderer hooks Continuum's CoreRenderer (client). Written against the decompiled source but never run: if the
  crystal looks wrong (too dark/bright, culled faces), the knobs are in client/CrystalRenderer.
- No custom shard entity: shards are server-side particles and block changes, so they do not show on the map as entities.
