# Changelog

All notable changes to this project will be documented in this file.

The format is based on Keep a Changelog and this project aims to follow Semantic Versioning where practical for mod releases.

## [Unreleased]

### Added
- Separate Crimsite, Asurine, Ochrum, and Veridium station modules with recipes, localized tooltips, and distinct scan and projection signals.

### Changed
- Reduce default hologram opacity and cull back-facing filled surfaces to improve projection readability.
- Honor configured fill and outline opacity for exact holograms, including mounted projectors and Ponder.

### Fixed
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
