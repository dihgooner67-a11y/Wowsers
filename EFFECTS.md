# Built-in effects (made for this mod, no other mod needed)
* One custom particle type `railgun:fx` (FxOptions/FxParticle). 11 hand-generated textures in
  assets/railgun/textures/particle: glow, spark, ring, smoke_a, smoke_b, flame, streak, bolt_a, bolt_b, arc, shock.
  Glows/sparks/flames/electricity use additive blending (light adds up = bloom). Smoke is normal alpha blending
  with a lit rim so it looks shaded.
* client/Vfx.java is the toolkit (glow, core, spark, ring, shock, smoke, flame, streak, bolt, electric, lightning, burst).
* Hollow Purple, Black Flash, Rail Gun and the Mech Beam use only these (no vanilla explosion/electric-spark particles).
* Screen filter: assets/railgun/shaders/post/impact.json + program/impact.fsh. During an impact frame it recolours the
  shading of what you are looking at into the frame's palette (black/white, red, purple, fire, blue, gold), flipping
  to the negative every ~25 ms, then fades. A bloom pass adds glow to bright blasts. If the shader can't load, the
  mod falls back to flat colour frames automatically.
