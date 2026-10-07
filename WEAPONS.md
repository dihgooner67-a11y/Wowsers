# New weapons (Forge 1.20.1)

## Unlimited Black Hole  (/give @s railgun:unlimited_black_hole)
Right-click: opens a black hole 45 blocks ahead (5 min cooldown).
- Grows over 3 s to a 28-block-wide event horizon with a ~96-block (6 chunk) accretion disk; pulls everything within
  140 blocks (~9 chunks) toward it, erases terrain, swallows anything past the horizon, burns the disk, then collapses
  after 16.5 s and detonates (everything within 80 blocks takes 300 damage).
- Made from new particles: a round black sphere (stacked black discs), three photon rings, spiral glow layers, an orbiting
  accretion disk with a brighter side, jets, inflow and lightning. The screen filter adds real gravitational lensing
  (bends your view around the hole, black horizon, Einstein ring).
- Scale cutscene (10 s): letterbox, camera push-in, a swooping pull-back to a wide shot with extra FOV, an orbit, then
  back to you. Measurement callouts show the horizon, the disk (in blocks and chunks), the pull range and YOU (1.8 blocks).
  Dynamic impact frames (palette-swapped views of the actual scene) fire at the cast, the warp, the reveal and the collapse.

## Cursed Fists  (/give @s railgun:cursed_fists)
Hold in either hand (if it's in the off hand your main hand must be empty) and punch. Purple flames burn on BOTH hands.
- 20% Black Flash (4x damage + big knockback). Hit the same mob twice and it dies instantly (players take normal damage).
- Right-click = Cursed Surge: 30 s of 95% Black Flash and double damage (3 min cooldown).
- No in-hand model on purpose. The inventory icon is an animated loop of the particles it gives off.

## FUGA: Open Flame  (/give @s railgun:fuga_open_flame)
Hold right-click: an arrow of flame forms in your hand (nothing is shown in the item slot or hand model, the particle arrow IS the
visual; the inventory icon is an animated flame arrow). Release after 1 s to fire, or hold 3 s for a FULL DRAW (bigger spire). 30 s cooldown.
- The arrow flies at 80 blocks/s, trailing fire, embers, soot and rings. Where it lands: a crater, a 120-damage (200 full) blast, and a
  towering spire of flame (90 blocks tall, 130 full) for 5 s that burns and lifts everything inside it.
- The spire is built from four new fire particles (fire tongue, ember, flare, fireball): tongues, a white-hot core, three twisting helixes,
  seven stacked flame rings climbing the spire, embers and a blazing tip.
- Impact frames: the screen filter redraws your actual view as black ink on orange paper (block outlines and textures become ink lines,
  shadows go solid, mid-tones become halftone dots), flips between positive and negative, and a white lightning streak crosses the corner.
  It is computed from whatever is on screen, so it follows the terrain and blocks around you.

## Dismantle  (/give @s railgun:dismantle)
Right-click: a long slash leaves your hands at 12 blocks/tick (range 84). It cuts a thin slit through blocks, digs a trench in the ground
under its path, and hits every mob it touches for 45 damage + Wither. The first mob hit gives ONE short impact frame: the screen turns to
black with soft glowing white outlines of everything around you (the reference look) with black slits across it. 1.5 s cooldown.
In-world the blade is a black slit with white rims (new slash particle) plus ground dust and sparks. Nothing shows in hand.

## Cleave  (/give @s railgun:cleave)
Right-click while a mob is TOUCHING you (otherwise it tells you it needs one). For 3.5 s a storm of short dismantles (small black cuts with
red rims and solid red/black cuts) tears the mob and every mob within 6 blocks (28 damage every half second, 140 finale), pins the target,
and nicks the ground. Multiple LONG impact frames cover the whole attack: ~0.35 s each, alternating black screen with red glowing
outlines and red screen with black outlines, each with a fresh storm of small black and solid-red cuts. 40 s cooldown.
