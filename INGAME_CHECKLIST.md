# In-Game Checklist

Use this checklist for manual release verification in-game. Mark each item as pass/fail and add notes for anything inconsistent.

## Test Setup

- [ ] Run the mod on the intended release build, not a dev environment with extra debug changes.
- [ ] Verify Create and all required dependencies load without warnings that block gameplay.
- [ ] Test at least once in Survival and once in Creative.
- [ ] Test both a fresh world and a rejoined existing world.

## Creative / Item Access

- [ ] The `Create: Geo-Resonance` creative tab is visible.
- [ ] The creative tab contains all mod items and blocks.
- [ ] Creative search does not show a duplicate empty Seismic Hammer.
- [ ] The creative tab hammer icon appears filled and does not show a pressure bar.

## Seismic Hammer

- [ ] Crafting a Seismic Hammer from a charged tank preserves the tank charge in the crafted hammer.
- [ ] A Creative-obtained hammer starts filled.
- [ ] The hammer can strike terrain normally.
- [ ] The hammer can hit mobs.
- [ ] Placing the hammer creates the placed hammer block correctly.
- [ ] The placed hammer has the intended collision shape and interaction behavior.
- [ ] The hammer ponder animation looks correct: piston extends quickly and returns slowly.

## Seismic Station Placement / Breaking

- [ ] The station places consistently on valid terrain.
- [ ] The station can be placed without the old strict two-block support requirement.
- [ ] Breaking the controller with a wrench drops the station correctly.
- [ ] Breaking the boundary block with a wrench also drops the station correctly.
- [ ] Breaking visuals use the intended casing particles instead of missing-texture particles.
- [ ] Breaking in Creative does not incorrectly drop the station.
- [ ] Breaking blocks under the station does not trigger the removed auto-break behavior.

## Station Operation

- [ ] The station accepts a placed hammer and required inputs correctly.
- [ ] The station requires the intended minimum RPM.
- [ ] The station uses constant base stress and adds the intended stress per installed module.
- [ ] The station starts its scan flow without an excessive pre-start delay.
- [ ] The first cycle includes hammer lift and fall before the first impact.
- [ ] The station performs one initial hit even if no anomalies are found.
- [ ] The station does not perform an unnecessary extra final hit when anomalies exist.
- [ ] After the last required strike, the seismogram is generated immediately.

## Echo / Wave Timing

- [ ] The hammer impact wave happens at the actual moment of impact, not during the lift.
- [ ] The initial impact does not start from the hammer resting on the ground.
- [ ] Anomaly echoes are delayed relative to the hammer strike and do not fire at the same instant unless intended by depth.
- [ ] Hammer impact waves render at the same height as anomaly waves.

## Station Modules

- [ ] Modules can be inserted and removed normally.
- [ ] `Shift` + right click removes installed modules as explained.
- [ ] The station module ponder second page shows the ore/water scene correctly.
- [ ] Below Zero and Noise Cancellation module explanations are visible and understandable.
- [ ] The tooltip states that last modules are prioritized.
- [ ] Last installed modules actually have signal priority in behavior.
- [ ] Noise Cancellation suppresses cave/water/lava noise when used without ore modules.
- [ ] Module rendering on mounted/train stations looks correctly lit, scaled, and positioned.

## Seismogram

- [ ] A produced seismogram records nearby results correctly.
- [ ] The center dot from vanilla-style map rendering is not visible.
- [ ] The sidebar header reads `Nearest Signals (~Y)`.
- [ ] Approximate Y values update dynamically based on player position.
- [ ] The `Shift` description explains that the Y values are approximate.
- [ ] The `Shift` description includes the station boundary guidance where intended.

## Projector

- [ ] The projector accepts seismograms only when powered as intended.
- [ ] The projector uses the intended minimum RPM.
- [ ] The projector uses constant base stress and adds the intended stress per installed module.
- [ ] The projection beam originates from the corrected center-bottom point.
- [ ] Projection lines, sheets, and active lamp state render correctly.
- [ ] Projected ore/liquid colors match the intended mapping.
- [ ] The red boundary line logic appears only after inserting the first seismogram and disappears after inserting the second.
- [ ] Rejoining the world does not require looking directly at the projector before holograms load.
- [ ] The projector tooltip `Shift` text explains the boundary behavior.

## Rotation / Visual Sync

- [ ] Station shafts visually match their power source rotation on all facings.
- [ ] Projector shafts visually match their power source rotation on all facings.
- [ ] The seismogram drum rotates smoothly without snapping back to a default angle.
- [ ] Assembled train-mounted visuals also stay aligned.

## Train Compatibility

- [ ] A projector placed on a train can be assembled without breaking.
- [ ] A station placed on a train can be assembled without leaving invisible ghost blocks.
- [ ] Parked train-mounted projectors work when powered and loaded.
- [ ] Parked train-mounted stations work only when properly powered.
- [ ] Mounted projectors show shaft rotation, inserted sheets, and projection lines.
- [ ] Mounted stations preserve expected interaction behavior.
- [ ] Projections disappear appropriately when the moving train leaves valid loaded areas.

## Ponders / UX / Localization

- [ ] Hand hammer ponder text points to the hammer in the stand's hand where intended.
- [ ] Comment callouts lowered earlier in development still sit at readable positions.
- [ ] The seismic station ponder order is correct: terrain, station, then power instruction.
- [ ] All changed English tooltip and ponder text is present and reads cleanly.
- [ ] Russian localization covers the updated text without obvious mistakes.
- [ ] Ukrainian localization covers the updated text without obvious mistakes.

## Performance / Stability

- [ ] Station scans complete without major lag spikes in normal use.
- [ ] Projector remains responsive with many loaded signals.
- [ ] FPS around an active projector is acceptable compared to previous build.
- [ ] Saving, quitting, and rejoining does not break station/projector state.
- [ ] No new client crashes occur while opening inventory, creative tabs, or interacting with core blocks.

## Release Gate

- [ ] No blocker bugs found.
- [ ] Any remaining minor issues are documented and acceptable for release.
- [ ] Build artifact selected for release matches the version being tested.

## Notes

- Tester:
- Build:
- World seed:
- Result:
- Notes:
