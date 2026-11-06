# Senior – 11th-Dimensional Manipulation (Re:AcademyCraft 1.5.0 add-on)

A separate Forge add-on. It does **not** modify Re:AcademyCraft; it registers a new category ("Senior") through the
mod's public API (`RegisterAbilitiesEvent`). Targets Minecraft 1.20.1, Forge 47.4.10 or newer, Re:AcademyCraft 1.5.0.

## Honest status

| Stage | State |
|---|---|
| Code written | Yes, all 8 abilities |
| Syntax / API-type check | Done in a sandbox: parses cleanly, API types resolve. Minecraft/Forge classes were NOT available there |
| Compiled with Forge | **Not yet** – this is the first thing to do (step 3 below) |
| Tested in Minecraft | **No** |

If the build prints errors, copy the full output back to Claude. Most likely culprits are a vanilla class or constant
name I wrote from memory of the 1.20.1 mappings (particles, sounds, `BlockPos.containing`, etc.); they are one-line fixes.

## What each ability does

| Skill (level) | Behaviour |
|---|---|
| Spatial Displacement (1) | Aim at a target ≤20 blocks. It is moved 8–12 blocks to a checked safe spot (loaded chunk, world border, solid floor, no lava/fire/etc., room for its body). Uses Re:AcademyCraft's safety API (teleport + PvP/protection checks). Bosses resist (refused, no cost). CD 16 s |
| Spatial Compression (2) | One target ≤18 blocks, 4 s: pulled toward the centre, Slowness IV, damage every 0.5 s, knock-back on release. Strong targets (≥100 max HP) resist half, bosses mostly. CD 18 s |
| Dimensional Barrier (2) | 6 s sphere; absorbs 70% of each hit until durability (60) is gone; 30+ damage hits wear it 2× faster. CD 28 s |
| Dimensional Shift (3) | 4 s: invisible, cannot be hurt, cannot attack or use other Senior skills. CD 15 s. **No wall-walking** (see limits) |
| Higher-Dimensional Perception (3) | Pulse 35 blocks: players and hostile mobs get the Glowing outline 5 s; action bar shows count, distance, direction of the 3 nearest. CD 20 s |
| Dimensional Rupture (4) | Aim ≤25 blocks. 1.5 s crack, then burst radius 6 with falloff damage and knock-back. No block damage. CD 22 s |
| Spatial Collapse (5) | Aim ≤25 blocks. 3 s pull in radius 10, then heavy shockwave and throw-back. No block damage. CD 60 s |
| 11th-Dimensional Domain (5) | 15 s, radius-25 field around you. Your other skills get ×1.5 range/radius/displacement distance, Compression hits up to 4 targets, barrier durability ×1.5. CD 120 s |

Skill tree: Displacement → Compression, Barrier → Shift (from Compression), Perception (from Barrier) → Rupture (from Compression) → Collapse; Domain needs Perception + Barrier.

Pressing a timed skill's key again cancels it early (energy is not refunded). All hostile effects and damage go through
`AcademySafety`, so PvP, teams and protected areas of your server are respected.

## Known limits (not bugs)

* **Dimensional Shift cannot walk through blocks.** Block collision is decided on the player's own client; doing it needs
  client-side code the public API does not offer. Everything else in the spec (invulnerable, cannot act, aura) works.
* Shift uses full invisibility (vanilla has no partial transparency); other players see the particle aura.
* Perception's outline is vanilla Glowing, so everyone can see it, not only the caster.
* Visuals are vanilla particles in purple/blue/white. Cracks, transparent geometry and screen warping need custom client rendering (later).
* Energy costs are untuned guesses – see `Costs.java`. Owners can also scale cost/damage per skill in `config/academy-addon-seniordim.toml` (created after first launch).
* The add-on must be installed on the server **and every client** (Re:AcademyCraft refuses mismatched logins).

## Easiest: build it on GitHub (no Java needed)

1. Make a free GitHub account, then create a **new private repository**.
2. Unzip `senior-addon.zip` on your PC. In the repository click **Add file → Upload files**, and drag in the CONTENTS of the
   `senior-addon` folder (the `src`, `libs`, `gradle` folders and the files next to them). Commit.
3. The hidden folder `.github` is often skipped by drag-and-drop. If the **Actions** tab shows no workflow, click
   **Add file → Create new file**, type the name `.github/workflows/build.yml`, paste in the contents of that file from the zip, and commit.
4. Open the **Actions** tab, open the latest "Build mod jar" run (or press **Run workflow**). When it is green, scroll to
   **Artifacts** and download `seniordim-jar`. Unzip it: the `.jar` inside is the mod.
5. If the run is red, open it, copy the red error text and paste it to Claude.

## Or build it on your own PC (never on the live server)

1. **Install JDK 17** (e.g. Eclipse Temurin 17). Check with `java -version`; it should say 17.
2. **Nothing to add:** the SDK's API jar is already in `libs`, which is enough to build.
3. **Build.** Open a terminal in this folder and run:
   * Windows: `gradlew.bat build`
   * Linux/macOS: `./gradlew build`

   What it does: downloads Gradle 8.8, then Minecraft + Forge development files (a few hundred MB; the first run can take
   10+ minutes), compiles the Java code, and packs the jar. It changes nothing outside this folder and your user-level Gradle cache.
4. The result is `build/libs/seniordim-forge-1.20.1-0.1.0.jar`.

## Test on a separate development instance

1. Create a NEW Minecraft instance (Prism Launcher / CurseForge app / launcher) with Minecraft 1.20.1 and Forge 47.4.10 or newer.
2. Put these two jars in that instance's `mods` folder: `re-academycraft-forge-1.20.1-1.5.0.jar` and `seniordim-forge-1.20.1-0.1.0.jar`.
3. Start a singleplayer creative world. Take **Senior Factor** from the Ingredients creative tab.
4. Use the mod's Ability Developer machine (see the mod's in-game tutorial) with the factor to get the Senior category,
   learn skills in the skill tree, and bind them in the skill preset editor.
5. Check, in this order: Spatial Displacement → Compression → Barrier → Perception → Rupture → Shift → Collapse → Domain.
   Test on mobs first; then with a second player for PvP/team rules.

Only after that move the jar to the dev copy of your server, and finally the live server.

## Files

* `src/main/java/dev/senior/dimension/SeniorDim.java` – registration (category, factor item, skill tree, behaviours)
* one class per ability (`SpatialDisplacement`, `SpatialCompression`, `DimensionalBarrier`, `DimensionalShift`, `HigherPerception`, `DimensionalRupture`, `SpatialCollapse`, `DimensionalDomain`)
* `SeniorEvents.java` – damage hooks for Shift and Barrier; `Costs.java` – energy costs
* `assets/seniordim/lang/en_us.json` – names and descriptions
