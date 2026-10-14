# Development Launch Profiles

Run these commands from the `neoforge-1.21.1` directory using Java 21.

## Default: Create Propulsion: Simulated

```powershell
.\gradlew.bat runClient -PfastDev=true
```

## Without Propulsion

```powershell
.\gradlew.bat runClient -PfastDev=true -PpropulsionDev=false
```

The default profile includes Propulsion 1.1.5, Sable 2.0.3 (with Sable Companion), and
the Aeronautics 1.2.1 bundle (with Simulated and Offroad). Propulsion references the
Simulated creative-tab API directly, so Sable alone is not enough to launch it.
It uses NeoForge 21.1.228 because Sable requires that version. With `-PpropulsionDev=false`,
all three addons are omitted and the dev runtime uses NeoForge 21.1.219.
Omit `-PpropulsionDev=false` to enable Propulsion again. Omit `-PfastDev=true` for normal debug logging and JVM settings.
In Git Bash, use `./gradlew.bat` instead of `.\gradlew.bat`.

These addons are dev runtime only, not bundled with Geo-Resonance or required by its release metadata.
The integration detects stationary world Platinum ores; it does not add Sable moving-structure support.
