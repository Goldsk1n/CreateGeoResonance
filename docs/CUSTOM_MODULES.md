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
  "block_tag": "my_pack:ruby_ores",
  "color": "#E84B70"
}
```

The five original fields are required. Optional `icon_size` accepts `"standard"` or `"small"`, defaulting to `"standard"`.
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
Map pixels use the closest vanilla map color. Echoes, sidebar text, and holograms use the configured RGB color.

## Behavior and Compatibility

- Each custom module uses one of the eight slots and adds the usual 64 SU.
- Different custom IDs can coexist; duplicate IDs are rejected.
- Custom minerals participate independently in station/projector round-robin limits.
- At overlapping custom block tags, the last installed module takes priority; equal priority uses stable ID ordering.
- A matching custom target takes precedence over a built-in target for the same non-fluid block.
- No installed target modules keeps the normal cave/water/lava scan.
- Definitions are synchronized from the server at login and after reload. Clients do not need a separate copy of the datapack.
- Creative entries refresh after definitions synchronize. Recipes are ordinary crafting recipes for recipe viewers.
- Optional JEI support distinguishes custom module IDs and refreshes custom ingredients after synchronization. JEI is not required.
- Existing built-in signal names in saved data remain supported.
- Never rename an established definition ID without planning a migration.
- Removing a definition preserves item metadata and saved record identities. Missing definitions show a warning and cannot be inserted.
- Installed missing modules can still be extracted. Their missing targets produce no exact holograms. Restore the same ID to restore recognition.
- Reload changes future scans and current matching/display. It does not rewrite old map pixels or retroactively add blocks to saved surveys.
- Finish active surveys before reload. Rejoin if a recipe viewer keeps an old ingredient list after a datapack change.

## Manual In-Game Checklist

Run these checks separately on Forge 1.20.1 and NeoForge 1.21.1.

1. Enable the supplied example, reload, and confirm two correctly named/iconed variants appear in the mod tab and Creative Search, without a blank custom module.
   Compare Orange's standard icon with Cyan's smaller icon in inventory/JEI, in hand, and installed in placed/train stations. Change one size and reload; omit the field to verify the standard default.
2. Inspect both recipes in JEI: each custom variant should have only its own recipe. Craft each in Survival and verify the output keeps the correct name, icon, and target. Check Shift tooltips and advanced ID text (F3+H). Also check a launch without JEI.
3. Make a controlled test site: small, separated orange and cyan concrete clusters a few blocks below the surface, with two stations at least the configured minimum distance apart and within survey range.
4. Install both modules in each station. Both should be accepted; installing the same ID twice should be rejected. Confirm station stress increases by 128 SU, to 384 SU with just these two modules.
5. Supply enough power, paper and ink, then survey. Both signal names/colors should appear, with no cave/water/lava background signals. Try adding a normal Copper Module and verify all three types can appear.
6. Insert two valid records into a powered projector. Both custom deposits should be projected, separately colored. Mine some blocks: only those blocks disappear. Mine all of one deposit: the other remains.
7. Extract/reinsert modules and seismograms. Save/rejoin, unload/reload the chunks, and verify names, targets, records and projections survive. Check an older built-in seismogram too.
8. Assemble/disassemble a train carrying the station/projector. Check installed module icons, parked station scans, moving beam origin and fixed world-space custom deposits.
9. Change a definition's name/color/target tag without changing its ID, then reload. Check names, icons/Creative entries and newly produced surveys update; expect existing map pixels to stay unchanged.
10. Remove one definition while retaining its saved item, installed module and old records. Reload/rejoin: no crash, a clear missing-definition warning, safe extraction, no ghost exact hologram for that missing target. Restore the same file/ID and verify recognition returns.
11. On a dedicated server with the datapack installed only on the server, join with two clients. Check names, icons and projections match, then reload and rejoin. Also switch to a world/server without the example and verify no example entries leak across worlds.
12. Test overlapping custom tags: make both definitions target the same test block, reverse insertion order, and verify the later module owns that block's signal.
13. In a disposable copy of the pack, try an invalid color, missing icon item, unsupported icon size, misspelled field or malformed JSON. Reload must report a useful error, not silently enable a broken module.

Only build/compile checks are performed automatically for this change; this checklist covers visual and gameplay behavior.
