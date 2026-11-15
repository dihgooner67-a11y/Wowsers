APR REACTOR (CINDER-E2a): HOW TO BUILD AND RUN IT

Everything is controlled with commands while you LOOK AT THE APR CORE BLOCK (within 8 blocks).
Blocks are in the creative tab "NTM Cursed Edition": APR Core, Outlet, Plating, Dark Plating, Mechanical Parts,
Hadron Coil, Support Beam, Hull, Blades (Hull (Assembled) is what Hull turns into when assembled).

1. Place the APR Core. Choose chambers: /cursed apr chambers 6        (radii 6-32, 3+ blocks apart, e.g. 6 10)
2. See what to build: /cursed apr structure  (reports the first wrong block). Layout below the core, offsets (x,y,z):
   - y-1 and y-5: a flat disc of radius R (R = biggest chamber radius). Plating everywhere, Dark Plating on the
     x=0 and z=0 lines, APR Outlet at the very centre of y-5 (the centre of y-1 stays AIR).
   - y-2 to y-4: Mechanical Parts across the same disc.
   - The 3x3 shaft right under the core (y-1 to y-4) is AIR. Around it, a 3-wide wall of Hadron Coil on all four
     sides at distance 2, y-1 to y-4.
   - For each chamber radius r a ring (torus) of Hull blocks, Support Beams where the ring crosses the x=0/z=0 lines,
     and Blades in the inner ring at the core's level. A radius-6 APR needs about 800 blocks.
   Rule of thumb: build it, run /cursed apr structure, fix the reported block, repeat.
3. /cursed apr assemble     (Hull blocks turn into Assembled Hull). /cursed apr disassemble undoes it.
4. Main coolant: /cursed apr coolants lists usable fluids; /cursed apr coolant <NAME> sets it.
   Chambers: /cursed apr fluid <chamber 1..n> <NAME>. Pipe the fluids to the core (it accepts any tank's fluid).
5. Particles: /cursed apr particles <amount> (debug for now; the addon fed them from items).
6. Run: /cursed apr control <0-100>. /cursed apr shows rps, hull, damage, tanks, chamber temps, power.
   Power comes out of the core's sides like a DFC receiver. 110 rps starts damaging; at max damage the
   chambers blow up, and a spin past 110 rps also disassembles it with a large explosion.

NOT IN YET: GUI, separate fluid port blocks and power outlet function, particle items, 3D core model,
sounds, the addon's own gaseous oxygen/pyrogel fluids, advancement.

ABOUT "CONTROL RODS" (fixed in CINDER-F2)
The addon's APR has no control rods. Its "control" setting is a funnel in the core model that opens and closes
(0-100%). My first port stored it as a number only, so nothing visibly moved. Now the APR Core block changes texture in
5 steps (0/25/50/75/100%) as control moves, and /cursed apr control <0-100> tells you what it is doing.
Control only moves while the reactor is assembled, and it also scales how much coolant is processed per tick
(coolant processed = tank x particle share / 20 x control%). Control defaults to 10%.
