# Signal Colors and Sounds

## Shared Profiles

Built-in signal profiles live in `SeismicSignalProfiles.java`. Waves and holograms use the same base RGB.
Sidebar labels lighten each RGB channel 22% toward white. Map pixels use the profile's vanilla palette entry.
Rock & Stone deposit variants reuse their corresponding mineral profile.
Custom module RGB and audio come from the synchronized datapack definition.

Iron is consistently silver-gray (`#D7D7D7`); Spawner is consistently teal (`#52C4B5`) with a sculk-sensor pulse.
Coal, Magnetite, Oil and Lignite have brighter base colors.
Cobalt/Lapis, Bauxite/Copper, Nitrate/Prosperity and Lignite/Soulium are more separated.
Palette-limited map markers and naturally similar minerals still require names for reliable identification.
No palette can make every resource reliably distinguishable by color alone.

Existing saved map pixels are not recolored automatically. Make fresh surveys to compare the new palette.
Existing projector records and stored mineral IDs remain valid; projections use the current profiles.
Default hologram border/fill alpha is now 200/70. Exposed opaque ore masks hologram fragments behind it without fading front outlines; see [Hologram Visibility](HOLOGRAM_VISIBILITY.md). Existing saved client config values are not overwritten.

## Audio

- Metals use bells, iron xylophone, chimes or chain tones rather than generic stone breaks.
- Crystal/gem signals use crystal resonance or musical tones.
- Stone deposits use short mineral hits, with muted variations for dense/soft deposits.
- Water/Oil retain bubble sounds; Lava retains its three-pop sequence.
- Chest, Spawner and experience signals retain distinct special cues.
- Pitch decreases by at most `0.06` over the full survey depth, instead of `0.22`.
- Playback pitch is clamped to `0.5-2.0`. Echo arrival timing is unchanged.
- Low-pressure echoes keep the generic gray/hat signature.
- Strike, paper/ink and module insertion/extraction sounds are unchanged.
- Custom modules accept optional `sound_family` and `base_pitch`; see [Custom Modules](CUSTOM_MODULES.md).
- Update both server and client to this build (network protocol 3).

## Built-In Reference

Colors are base RGB values, not the final alpha-blended screen result. Pitches are before depth adjustment.
An entry such as `METAL.sound()` uses the corresponding family's default sound.
Family defaults: metal = note-block bell; crystal = amethyst chime; stone = calcite hit; liquid = whirlpool bubbles; special = sculk clicking.

| Signal | RGB | Map Palette | Family | Echo Sound | Base Pitch |
| --- | --- | --- | --- | --- | --- |
| WATER | `#50AAFF` | `WATER` | liquid | `BUBBLE_COLUMN_WHIRLPOOL_AMBIENT` | 0.84 |
| LAVA | `#FF7828` | `COLOR_ORANGE` | liquid | `LAVA_POP` | 0.66 |
| CAVE | `#B4C8DC` | `COLOR_LIGHT_GRAY` | stone | `BELL_BLOCK` | 1.12 |
| COAL | `#8A8A8A` | `COLOR_GRAY` | stone | `NOTE_BLOCK_BASS` | 0.62 |
| IRON | `#D7D7D7` | `METAL` | metal | `NOTE_BLOCK_IRON_XYLOPHONE` | 0.72 |
| COPPER | `#D3844B` | `COLOR_BROWN` | metal | `METAL.sound()` | 1.00 |
| GOLD | `#FFD73D` | `COLOR_YELLOW` | metal | `NOTE_BLOCK_CHIME` | 1.22 |
| REDSTONE | `#E53131` | `COLOR_RED` | stone | `REDSTONE_TORCH_BURNOUT` | 0.95 |
| LAPIS | `#426CD0` | `COLOR_BLUE` | crystal | `CRYSTAL.sound()` | 1.06 |
| EMERALD | `#31D271` | `COLOR_GREEN` | crystal | `NOTE_BLOCK_FLUTE` | 1.28 |
| DIAMOND | `#57EEDC` | `COLOR_LIGHT_BLUE` | crystal | `AMETHYST_BLOCK_RESONATE` | 1.34 |
| ZINC | `#9CBECD` | `COLOR_GRAY` | metal | `CHAIN_STEP` | 0.90 |
| CRIMSITE | `#B85C70` | `TERRACOTTA_RED` | stone | `STONE.sound()` | 1.00 |
| ASURINE | `#669FD0` | `COLOR_LIGHT_BLUE` | stone | `STONE.sound()` | 1.10 |
| OCHRUM | `#D9AC55` | `TERRACOTTA_YELLOW` | stone | `STONE.sound()` | 0.95 |
| VERIDIUM | `#79B883` | `TERRACOTTA_GREEN` | stone | `STONE.sound()` | 1.05 |
| THORIUM | `#C6D86B` | `COLOR_LIGHT_GREEN` | metal | `METAL.sound()` | 1.16 |
| MAGNETITE | `#8FA4AD` | `TERRACOTTA_CYAN` | metal | `NOTE_BLOCK_IRON_XYLOPHONE` | 0.76 |
| LEAD | `#848CB8` | `TERRACOTTA_BLUE` | metal | `NOTE_BLOCK_IRON_XYLOPHONE` | 0.68 |
| NICKEL | `#AFC38D` | `COLOR_LIGHT_GREEN` | metal | `METAL.sound()` | 0.88 |
| LITHIUM | `#D896B6` | `COLOR_PINK` | metal | `METAL.sound()` | 1.24 |
| BAUXITE | `#EFA76F` | `TERRACOTTA_ORANGE` | stone | `STONE.sound()` | 0.92 |
| GALENA | `#9E7BB8` | `TERRACOTTA_PURPLE` | stone | `DEEPSLATE_HIT` | 0.74 |
| LIGNITE | `#B18B55` | `TERRACOTTA_BROWN` | stone | `NOTE_BLOCK_BASS` | 0.58 |
| SULFUR | `#DFD85A` | `COLOR_YELLOW` | stone | `STONE.sound()` | 1.12 |
| CRUDE_OIL | `#A76C8E` | `TERRACOTTA_MAGENTA` | liquid | `LIQUID.sound()` | 0.60 |
| WOLFRAMITE | `#90A9BF` | `COLOR_LIGHT_GRAY` | metal | `NOTE_BLOCK_IRON_XYLOPHONE` | 0.64 |
| URANIUM | `#64CE67` | `COLOR_GREEN` | metal | `NOTE_BLOCK_IRON_XYLOPHONE` | 1.02 |
| AUTUNITE | `#B9E45B` | `COLOR_LIGHT_GREEN` | crystal | `CRYSTAL.sound()` | 1.10 |
| NITRATE | `#E6D0A8` | `SAND` | stone | `STONE.sound()` | 1.26 |
| TIN | `#86BFA8` | `COLOR_CYAN` | metal | `METAL.sound()` | 0.96 |
| SILVER | `#CADCEC` | `COLOR_LIGHT_GRAY` | metal | `NOTE_BLOCK_CHIME` | 1.20 |
| PLATINUM | `#C1BBE8` | `COLOR_PURPLE` | metal | `NOTE_BLOCK_CHIME` | 1.30 |
| OSMIUM | `#7592BE` | `COLOR_BLUE` | metal | `NOTE_BLOCK_IRON_XYLOPHONE` | 0.82 |
| QUARTZ | `#F2E7D6` | `QUARTZ` | crystal | `CRYSTAL.sound()` | 1.36 |
| ANCIENT_DEBRIS | `#A97F6C` | `TERRACOTTA_BROWN` | metal | `NETHERITE_BLOCK_HIT` | 0.70 |
| FLUORITE | `#AEB8EF` | `COLOR_LIGHT_BLUE` | crystal | `CRYSTAL.sound()` | 1.30 |
| CERTUS_QUARTZ | `#9BCDE8` | `COLOR_LIGHT_BLUE` | crystal | `CRYSTAL.sound()` | 1.42 |
| INFERIUM | `#83B74D` | `COLOR_LIGHT_GREEN` | crystal | `CRYSTAL.sound()` | 1.05 |
| PROSPERITY | `#D9E6E9` | `SNOW` | crystal | `CRYSTAL.sound()` | 1.58 |
| SOULIUM | `#B48B83` | `TERRACOTTA_PINK` | stone | `SOUL_SAND_HIT` | 0.78 |
| ARCANE_CRYSTAL | `#6F94F1` | `COLOR_BLUE` | crystal | `CRYSTAL.sound()` | 1.22 |
| RUNE | `#B480D9` | `COLOR_PURPLE` | crystal | `CRYSTAL.sound()` | 1.06 |
| STELLARITE | `#F1C45D` | `COLOR_YELLOW` | crystal | `CRYSTAL.sound()` | 1.66 |
| URANINITE | `#9DBE55` | `TERRACOTTA_LIGHT_GREEN` | metal | `NOTE_BLOCK_IRON_XYLOPHONE` | 0.96 |
| DRACONIUM | `#A57CE6` | `COLOR_PURPLE` | crystal | `CRYSTAL.sound()` | 0.78 |
| BLACK_QUARTZ | `#8B93A6` | `COLOR_GRAY` | crystal | `CRYSTAL.sound()` | 0.88 |
| COBALT | `#655BE0` | `TERRACOTTA_BLUE` | metal | `METAL.sound()` | 0.90 |
| AMETHYST | `#C08AE6` | `COLOR_PURPLE` | crystal | `CRYSTAL.sound()` | 1.18 |
| CHEST | `#AB7742` | `WOOD` | special | `CHEST_OPEN` | 0.85 |
| SPAWNER | `#52C4B5` | `COLOR_CYAN` | special | `SPECIAL.sound()` | 0.85 |
| SOLID | `#A0A0A0` | `STONE` | stone | `NOTE_BLOCK_BASS` | 0.56 |

## Manual Checks

Perform on both Minecraft versions with the updated jar. Do not expect old seismogram map pixels to change.

1. Make fresh Iron and Spawner surveys. Compare wave, sidebar and projector hues; Iron should stay silver-gray and Spawner teal. Check Spawner no longer sounds like cave-vines footsteps.
2. Compare Coal, Magnetite, Oil and Lignite projections against stone, deepslate and grass, above and below water. Confirm they remain visible without making the scene too opaque.
3. Compare Lapis/Cobalt, Copper/Bauxite, Nitrate/Prosperity, Lignite/Soulium and Autunite/Uraninite side by side using separate deposits and fresh maps. Distinct labels are still needed.
4. Listen to Iron/Lead/Copper/Silver, Crimsite/Galena, Quartz/Diamond, Oil/Lava, Chest/Spawner and experience signals where available. Check family identity, comfortable perceived loudness and overlapping echoes. Equal playback volume does not guarantee equal perceived loudness across source sounds.
5. Survey shallow and deep deposits of the same resource. The signature should remain recognizable; arrival delay should still vary, with only a small pitch change.
6. Enable `examples/custom_modules`. Orange should use a lower metallic bell; Cyan a higher crystal chime. Change the sound fields, reload and check existing installed module items use the new sound.
7. Omit the optional sound fields and reload: custom modules should default to crystal / 1.0. Invalid family names or pitches outside 0.5-2.0 should fail reload with a clear error.
8. On a dedicated server, install the example only server-side, join with matching client builds and verify audio/colors, reload updates and rejoining. Check parked station scans and projector records on assembled trains as well.
9. Verify low-pressure hammer echoes remain gray/hat-like, Lava still produces three pops, and station strike/insertion sounds have not changed.

Build and test-source compilation are automated for this change. Listening and gameplay checks are not run automatically.
