# Changelog

All notable changes to this project will be documented in this file.

The format is based on Keep a Changelog and this project aims to follow Semantic Versioning where practical for mod releases.

## [Unreleased]

### Added
- Lava Module for targeted lava surveys, alone or balanced with other selected targets, including on parked trains. Uses one slot and 64 SU, with a lava-bucket-and-obsidian recipe that returns the empty bucket, a vanilla bucket icon, and EN/RU/UK summaries.
- Retrieve the last inserted projector seismogram with empty-hand Shift-RMB, including on trains and without power; preserve original map items, recover legacy survey-only saves, and drop remaining sheets when the projector is broken. Add English, Russian, and Ukrainian tooltip guidance.
- Three client-side impact and confidence-dependent echo opacity settings shared by normal and low-pressure waves, all defaulting to 1.0.
- Optional PneumaticCraft: Repressurized oil-lake detection through the existing Crude Oil module, with source/flowing fluid support, a guarded oil-sample-and-iron recipe, native oil icon fallback, and localized summaries.
- Optional Actually Additions Black Quartz module with natural ore detection, a guarded gem-and-block recipe, native gem icon, and localized summaries.
- Optional Draconic Evolution Draconium module with Overworld, deepslate, Nether, and End ore detection, a guarded dust-and-ingot recipe, native ingot icon, and localized summaries.
- Optional Powah Uraninite module with poor, normal, and dense ore detection, deepslate variants, a guarded recipe, native crystal icon, and localized summaries.
- Optional Forbidden & Arcanus Arcane Crystal, Rune, and Stellarite modules with guarded recipes, native item icons, localized summaries, and natural ore detection.
- Optional Immersive Engineering Bauxite, Lead, Nickel, Silver, and Uranium ore detection, guarded recipes, native raw-material icons, and localized summaries using existing modules.
- Optional Mystical Agriculture Inferium, Prosperity, and Soulium modules with conditional recipes, native item icons, localized summaries, and Mystical Agradditions ore-variant support.
- Optional Applied Energistics 2 Certus Quartz module with budding-block, bud, and cluster detection, a conditional recipe, native crystal icon, and localized summaries.
- Optional Oreganized Lead and Silver ore detection, recipes, native item icons, and localized module summaries.
- Optional Create Propulsion: Simulated Platinum ore detection, native item icon, conditional recipe, and localized tooltip using the existing Platinum module.
- Optional Mekanism ore detection and recipes for Tin, Osmium, Lead, and Uranium, plus a new Fluorite module with localized tooltips and native item icons.
- Standalone Quartz module with vanilla Nether quartz detection and an Ancient Debris module with localized tooltips and projection signals.
- Optional Rock & Stone deposit detection through matching mineral modules, distinct deposit labels, and six additional resource modules.
- Create-style Shift summaries for the remaining basic detection modules in English, Russian, and Ukrainian.
- Optional Create: Nuclear integration with Uranium, Autunite, and Nitrate modules, shared Lead and Thorium support, and guarded recipes.
- Optional Create: Metallurgy integration with a Wolframite module, conditional recipe, localized tooltip, and distinct scan and projection signals.
- Optional TFMG modules for lead, nickel, lithium, bauxite, galena, lignite, sulfur, and physical crude-oil deposits.
- Optional Create: New Age integration with Thorium and Magnetite station modules, conditional recipes, localized tooltips, and distinct scan and projection signals.
- Separate Crimsite, Asurine, Ochrum, and Veridium station modules with recipes, localized tooltips, and distinct scan and projection signals.

### Changed
- Retire Noise Cancellation: module-free stations report caves, water, and lava; target modules automatically restrict surveys to their targets, with empty results when none are found. Apply the same rules to parked trains.
- Preserve legacy Noise Cancellation items and recover installed modules after active modules via Shift-RMB or when breaking the station, without active-slot or SU costs. Remove recipes and Creative/JEI entries; update EN/RU/UK tooltips and Ponder.
- Balance station echoes round-robin across installed target minerals, starting with the latest module and selecting strongest signals within each type. Share turns between ore/deposit variants, preserve overlapping different mineral types, redistribute unused turns. Update English, Russian, Ukrainian, and Ponder guidance.
- Show confirmed-deposit hologram fill only behind rendered scenery, leaving exposed ore textures unobscured while retaining outlines, guide beams, and buried-surface fill for placed and train-mounted projectors.
- Retire the Below Zero module: target detection works at all heights within scan range, without an extra module.
- Preserve legacy Below Zero items and recover installed modules with Shift-RMB after extracting active modules; retired modules occupy no active slot and consume no SU.
- Remove the Below Zero crafting recipe and Creative entry, and update English, Russian, Ukrainian, and Ponder guidance.
- Use interchangeable Nuclear/New Age raw and processed ingredients in the Thorium module recipe, including mixed-mod combinations.
- Remove resonant-amethyst module recipes for Thorium, Lead, Nickel, Tin, Silver, and Uranium; retain native provider recipes and Rock & Stone deposit detection.
- Combine native-provider recipes for Lead, Nickel, Silver, Tin, Uranium, Bauxite, Sulfur, and Crude Oil into single guarded recipes with optional ingredient tags and mixed-provider support.
- Use an ordinary iron ingot for every Crude Oil module recipe, including oil buckets and Thermal oil-sand samples.
- Enable Create Propulsion: Simulated and Sable in the default dev runtime; use `-PpropulsionDev=false` to disable them without changing release dependencies.
- Remove added random seismogram height error; approximate Y represents the detected signal's estimated center.
- Reduce default hologram opacity and cull back-facing filled surfaces to improve projection readability.
- Honor configured fill and outline opacity for exact holograms, including mounted projectors and Ponder.

### Fixed
- Preserve the station hammer's animation phase when paper, ink, or other inventory updates sync mid-strike; identify new cycles by replay/reveal transitions, and reconstruct late-loaded animations using the full cycle duration rather than the remaining countdown.
- Apply station module priorities before merging and capping scan signals, so water and lava cannot crowd out installed mineral targets. Share selection between placed and train-mounted stations, retain last-module priority and Noise Cancellation, and keep hand-hammer selection unchanged.
- Preserve scene alpha when drawing projector holograms and beams to prevent dark silhouettes in the 1.21.1 pause-menu blur, while retaining visibility through water.
- Rank visible holograms by their original surveyed centers so mining individual blocks does not hide a partially mined vein at the display limit, including on trains.
- Draw projector holograms and beams after translucent scenery so water does not obscure them; preserve placed and train-mounted projection coordinates.
- Prevent seismic impact and echo waves from hiding water by using alpha blending without depth writes instead of Flywheel's OIT material.
- Show Create's overstressed warning on every station block, not just the shaft input.
- Apply station stress to the connected upper-right input instead of the controller; refresh network load after module, speed, and world-load changes, and check saved power budgets on trains.
- Use hollow station collision shapes so adjacent solid blocks do not darken recessed inner faces; preserve full-block interaction and attachment support.
- Light each occupied station block locally so a block beside the controller no longer darkens the entire side.
- Keep adjacent block faces visible through the projector's recessed rear section.
- Add missing seismic Ponder category translations and remove the itemless placed-hammer entry that crashed when clicked.
- Match mineral module icon sizes to the Noise Cancellation module.
- Prevent signal grouping collisions when scanning the new mineral types.
- Use NeoForge's common zinc tags for the zinc module recipe, scanning, and projection validation.

## [1.21.1-1.1.1] - 2026-09-21

### Fixed
- Avoid reading Create's air capacity config while other mods inspect the default Seismic Hammer stack during world loading.

## [1.20.1-1.1.0] - 2026-06-27

### Added
- Limited Create train support for seismic stations and projectors while parked at a train station.
- Mounted interaction, rendering, and client synchronization for assembled stations and projectors on trains.

## [1.20.1-1.0.0] - 2026-05-30

### Added
- Initial public release baseline for Create: Geo-Resonance.
- Seismic Hammer gameplay loop and pressure-based behavior.
- Seismic Station scanning flow with module support and seismogram output.
- Seismic Projector triangulation flow from two seismograms.
- Ponder scenes for hammer, station operation, station modules, and projector triangulation.
- In-game localization files for `en_us`, `ru_ru`, and `uk_ua`.

### Changed
- Updated station and projector tooltip guidance with RPM and SU requirements.

### Fixed
- Multiple station/projector interaction, rendering, and synchronization issues resolved during pre-release stabilization.
