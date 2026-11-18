NUKE EFFECTS: PLASMA BALL, YIELD-DEPENDENT CLOUDS, DRIFTING SMOKE, BALEFIRE AND ANTIMATTER LOOKS
Hooks into Continuum's mushroom cloud (EntityNukeTorex) and its renderer. Visual only: no change to damage or craters.
Written against the decompiled Continuum 1.0.5, NEVER RUN: treat sizes, brightness and timings as first guesses.

1. PLASMA BALL FIRST
   Every bomb starts as an expanding white-hot ball (white -> yellow -> orange -> red, four glow layers, flickering,
   rising a little). The mushroom cloud is hidden while the ball is young and fades in as it burns out.
   Ball lasts 30 + 25 x scale ticks (scale 0.5 .. 5), so bigger bombs hold the ball longer (NukeTimings.ballTicks).

2. YIELD CHANGES THE CLOUD (scale comes from the blast radius: radius 50 = 0.6, 100 = 1.1, 300 = 2.3, 500 = 3.1, 1000 = 4.6)
   - Small (scale below 1.0, about radius under 90): no ground shock rings, no condensation rings, no ring/starfish puffs,
     and a thinner cloud (75%).
   - Medium: Continuum's normal cloud.
   - Large (scale above 2.5, about radius over 350): thicker cloud, up to about 1.55x at the biggest yield.
   (NukeTimings.SMALL_SCALE / THICK_SCALE / thickness.) You said "thunder" for small bombs: I read it as "thinner"; tell me if
   you meant something else.

3. DRIFTING SMOKE
   Continuum freezes the cloud at half its life and then fades it grey from three quarters. Now, once the fire has burnt
   out, the cloud is carried by a slowly veering wind that is stronger higher up, with turbulence, a slow rise and a
   gentle spread. Wind direction is fixed per explosion location, so every player sees the same drift.

4. BALEFIRE (Continuum's balefire bomb already uses torex type 1, so this applies to it automatically)
   Green-white plasma ball, then a spiralling pillar of green fire that grows up to 70 x scale blocks, four flame ribbons
   winding around it, rings of green fire racing across the ground. The mushroom cloud is only a faint haze (45%).

5. ANTIMATTER (torex type 3)
   A dark core inside a collapsing ring of light, then a burst: 110 white rays, and expanding violet-white shells.
   No mushroom cloud at all.
   IMPORTANT: the antimatter bomb lives in NTM: Space, and I do not have that jar, so I could not hook it. For now the
   effect exists as type 3 and is spawned by the test command. Upload ntmspace-forge-1.0.2-1.20.1.jar and I will wire the
   real bomb to it (or tell me which torex type it spawns).

TESTING (operators): look anywhere and run
   /cursed nuke 40                       small bomb
   /cursed nuke 100                      medium
   /cursed nuke 700                      large, thick
   /cursed nuke 300 balefire
   /cursed nuke 300 antimatter
   /cursed nuke 200 rainbow              Continuum's rainbow cloud (type 2)
This spawns only the cloud effect at your feet.
