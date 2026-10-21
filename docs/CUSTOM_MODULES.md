# Custom Seismic Modules

Modpack creators can define mineral modules with a datapack. No scripting mod or Java addon is required.
These are variants of `creategeoresonance:custom_module`, not separately registered items.

## Quick Start

1. Copy this repository's `examples/custom_modules` folder into a test world's `datapacks` directory.
2. Keep `pack.mcmeta` directly inside the copied folder. Use the example from the matching Minecraft version.
3. Run `/reload`, then check `/datapack list enabled`.
4. Find Orange Test Module and Cyan Test Module in the Geo-Resonance Creative tab, or craft them.
5. Place small orange/cyan concrete deposits below two stations, scan, and load both records into a powered projector.

The example is deliberately not bundled/enabled in the release jar.

## Definition

Place a JSON file at:
`data/<namespace>/seismic_modules/<path>.json`

For example, `data/my_pack/seismic_modules/ruby.json` defines the stable ID `my_pack:ruby`:

```json
{
  "name": "Ruby Module",
  "signal_name": "Ruby",
  "icon": "examplemod:ruby",
  "icon_size": "small",
  "sound_family": "crystal",
  "base_pitch": 1.15,
  "block_tag": "my_pack:ruby_ores",
  "color": "#E84B70"
}
```

The five original fields are required. Optional `icon_size` accepts `"standard"` or `"small"`, defaulting to `"standard"`.
Optional `sound_family` defaults to `"crystal"`; optional `base_pitch` defaults to `1.0`.
Existing definitions that omit the sound fields retain their crystal-chime signature.
Unknown fields and unsupported icon sizes are rejected to catch spelling mistakes.
Names must contain 1-80 characters. Icon must identify an installed, non-air item, not the custom module itself.
Use explicit namespaces. Color is exactly six hexadecimal digits, prefixed with #.
A maximum of 128 definitions is supported. Invalid definitions fail reload with an error; they are not silently skipped.

`block_tag` is a block tag ID without a leading #.
Add the relevant ore blocks to that tag. Optional entries can use `required: false`.
Empty tags are allowed, but naturally produce no signals.
Custom targets are non-air blocks with no fluid state; fluids/waterlogged blocks are outside this first implementation.

## Version-Specific Paths

| Resource | Forge 1.20.1 | NeoForge 1.21.1 |
| --- | --- | --- |
| Pack format | 15 | 48 |
| Module definitions | `data/<namespace>/seismic_modules/` | Same |
| Block tags | `data/<namespace>/tags/blocks/` | `data/<namespace>/tags/block/` |
| Recipes | `data/<namespace>/recipes/` | `data/<namespace>/recipe/` |

Block tag example:

```json
{
  "replace": false,
  "values": ["examplemod:ruby_ore", "examplemod:deepslate_ruby_ore"]
}
```

## Recipes

Use a normal shaped recipe with a custom-data-bearing result.
See the working example recipe for a complete ingredient pattern.

Forge 1.20.1 result:

```json
{
  "item": "creategeoresonance:custom_module",
  "count": 1,
  "nbt": {"GeoModule": "my_pack:ruby"}
}
```

NeoForge 1.21.1 result:

```json
{
  "id": "creategeoresonance:custom_module",
  "count": 1,
  "components": {
    "minecraft:custom_data": {"GeoModule": "my_pack:ruby"}
  }
}
```

These are result objects inside `minecraft:crafting_shaped` recipes, not complete recipe files.
An unconfigured result has no target and cannot be installed in a station.

Commands for testing:

Forge:
```mcfunction
/give @s creategeoresonance:custom_module{GeoModule:"geo_example:orange"}
```

NeoForge:
```mcfunction
/give @s creategeoresonance:custom_module[minecraft:custom_data={GeoModule:"geo_example:orange"}]
```

## Appearance and Localization

The standard module frame is reused. Its center uses the icon item's particle sprite, which works for both items and blocks.
`icon_size` selects the existing standard or small center-icon model; the frame and overall module size do not change.
The example Orange module uses `"standard"` and Cyan uses `"small"`. Size changes synchronize and update after `/reload`.
This is a flat texture preview, not the full 3D item model; animated/tinted/multi-layer icons may not exactly match their inventory rendering.
No additional resource pack is needed for the default names and icons.

Optional client translations override the supplied names:
- `module.my_pack.ruby.name`: item name.
- `module.my_pack.ruby.signal`: signal name.

For paths containing /, replace / with . in translation keys.
Map pixels use the closest vanilla map color. Echoes and holograms use the configured RGB color; sidebar labels use a lighter version for readability.

## Echo Sounds

Choose one of these `sound_family` values:

| Value | Returning echo |
| --- | --- |
| `metal` | Note-block bell |
| `crystal` | Amethyst chime |
| `stone` | Short calcite hit |
| `liquid` | Whirlpool bubbles |
| `special` | Sculk-sensor clicking |

`base_pitch` must be a finite number between `0.5` and `2.0`.
Depth lowers it by at most `0.06`, with playback clamped to `0.5-2.0`.
Echo travel delay is unchanged. Low-pressure hammer echoes retain the generic hat sound and gray color.
Both fields synchronize from the server and update after `/reload`; existing items do not need to be recreated.
These settings affect returning echoes, not station strikes or module insertion sounds.
The example Orange module uses `metal / 0.85`; Cyan uses `crystal / 1.3`.

## Behavior and Compatibility

- Each custom module uses one of the eight slots and adds the usual 64 SU.
- Different custom IDs can coexist; duplicate IDs are rejected.
- Custom minerals participate independently in station/projector round-robin limits.
- At overlapping custom block tags, the last installed module takes priority; equal priority uses stable ID ordering.
- A matching custom target takes precedence over a built-in target for the same non-fluid block.
- No installed target modules keeps the normal cave/water/lava scan.
- Definitions are synchronized from the server at login and after reload. Clients do not need a separate copy of the datapack.
- Clients and servers must run matching builds; the audio-definition schema uses network protocol 3.
- Creative entries refresh after definitions synchronize. Recipes are ordinary crafting recipes for recipe viewers.
- Optional JEI support distinguishes custom module IDs and refreshes custom ingredients after synchronization. JEI is not required.
- Existing built-in signal names in saved data remain supported.
- Never rename an established definition ID without planning a migration.
- Removing a definition preserves item metadata and saved record identities. Missing definitions show a warning and cannot be inserted.
- Installed missing modules can still be extracted. Their missing targets produce no exact holograms. Restore the same ID to restore recognition.
- Reload changes future scans and current matching/display. It does not rewrite old map pixels or retroactively add blocks to saved surveys.
- Finish active surveys before reload. Rejoin if a recipe viewer keeps an old ingredient list after a datapack change.
