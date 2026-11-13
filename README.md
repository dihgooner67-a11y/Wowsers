# NTM Cursed Edition -> NTM Continuum (Forge 1.20.1)

Port of Leafia's *Cursed Addon* (1.12.2) onto [NTM - Continuum](https://github.com/Warfactory-Official/NTM-Continuum) 1.0.5.
Work in progress. **Nothing here has been built or run in Minecraft yet** (no Forge toolchain was available when it was written).

## Easiest way: let GitHub build it (no installs)
1. Create a **private** GitHub repo and upload everything in this folder (not the big jars).
2. The Continuum jar is ~71 MB, too big for the website's file upload. Instead: repo -> **Releases** -> **Create a new release**,
   tag it exactly `libs`, drag in your Continuum jar and the Golem Core jar (any file names starting with `hbm` and `golemcore` work), then **Publish release**.
   Keep the repo private: those jars belong to their authors.
3. **Actions** -> **Build** -> **Run workflow**. When it finishes, download `cursed-continuum-jar` from the run page.
4. If it fails, copy the red error lines from the failed step and send them to me.

## Local setup (optional)
1. Install JDK 17 and Gradle 8.x, then run `gradle wrapper` once in this folder.
2. Put these in `libs/` (names matter, they are resolved as local deps):
   - `hbm-1.0.5.jar`  (rename your `hbm-forge-1_0_5-1_20_1.jar`)
   - `golemcore-1.0.5.jar`  (Continuum requires Golem Core; download it from CurseForge/Modrinth)
3. `./gradlew build` -> jar in `build/libs/`. `./gradlew runClient` to test.
4. Send the first compile errors back; expect a few (see docs/PORT_NOTES.md).

## Status
See `docs/STATUS.md` for the milestone list with code names (SEED, EMBER, HALO, ...).

## Licensing
Continuum is LGPL-3.0. The original addon's license is unknown: get permission from its authors
(Leafia, movblock) before publishing this port. `mods.toml` license is a placeholder.
