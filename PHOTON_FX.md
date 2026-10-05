# Photon VFX integration (optional)
The mod ships its own effects (see EFFECTS.md). If Photon (the VFX editor mod, Forge 1.20.1) is installed, the mod also
plays YOUR OWN Photon effects at the same moments through `/photon fx <id> block ...`. Without Photon nothing happens.

Make an effect: single-player creative world, `/photon particle_editor`, build it, export to
`<gameDir>/ldlib/assets/railgun/fx/<name>.fx`, then copy into the mod: `src/main/resources/assets/railgun/fx/<name>.fx`.

Names looked for (id = railgun:<name>):
Hollow Purple : purple_charge, purple_full, purple_launch, purple_impact, purple_beam, purple_beam_impact
Mech Beam     : mech_charge, mech_tier2, mech_tier3, mech_beam_1..3, mech_impact_1..3
Black Hole    : black_hole, black_hole_blast
Cursed Fists  : cursed_black_flash, cursed_kill, cursed_surge
Other         : rail_muzzle, rail_impact, black_flash, domain_pocket, domain_shrine, domain_void, domain_hometown
Missing files are skipped. If effects face the wrong way flip YAW_SIGN / PITCH_SIGN in PhotonBridge.java.
