# Create: Geo-Resonance 1.2.0

Minecraft 1.20.1 / Forge

## Added
- New Crimsite, Asurine, Ochrum, Veridium, Quartz, Ancient Debris, and Lava modules.
- Optional mineral and oil integrations: Create: New Age, TFMG, Create: Metallurgy, Create: Nuclear, Rock & Stone, Mekanism, Oreganized, AE2, Mystical Agriculture, Immersive Engineering, Forbidden & Arcanus, Powah, Draconic Evolution, PneumaticCraft: Repressurized, Thermal Foundation, and Tinkers' Construct.
- Datapack-defined modules with configurable targets, colors, icons, icon sizes, and echo sounds.
- Empty-hand Shift-right-click retrieves the last seismogram from a projector, even without power.
- Shift-placement reverses the projector; normal placement puts the shaft toward the player.

## Improved
- More balanced station surveys and projector displays when detecting multiple resources.
- Clearer map markers, refined signal colors and sounds, and improved hologram readability.
- Cleaner exposed-ore projections, better visibility through water, and reliable deep-deposit rendering.
- Smoother station animations, corrected lighting and stress handling, improved recipes, translations, and Ponder guidance.
- Fixed vanilla filled-map textures and removed the station seismogram pickup sound.

## Updating
- Back up your world and update both clients and server to 1.2.0.
- Below Zero and Noise Cancellation are retired: scanning works below Y=0 automatically, and target modules filter surveys without an extra module. Existing retired items are preserved; installed ones can be recovered after active modules with Shift-right-click, or by breaking the station.
- Integrations are optional, not additional required dependencies.
- Existing config values are preserved. New hologram defaults are 200 outline / 70 fill; run fresh surveys to see the updated map palette.

## Modpack Creators
See [Custom Seismic Modules](CUSTOM_MODULES.md) and the matching-version `examples/custom_modules` datapack in this branch. The example is not bundled in the mod jar.
