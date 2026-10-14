param()

$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
$modelDir = Join-Path $projectRoot 'src/main/resources/assets/creategeoresonance/models/block'
$source = Get-Content -Raw -Encoding UTF8 (Join-Path $modelDir 'seismic_station.json') | ConvertFrom-Json
$parts = @(
    @{ Name = 'lower_right'; X = 0; Y = 0 },
    @{ Name = 'lower_left'; X = -16; Y = 0 },
    @{ Name = 'upper_right'; X = 0; Y = 16 },
    @{ Name = 'upper_left'; X = -16; Y = 16 }
)

foreach ($part in $parts) {
    $elements = @()
    foreach ($original in $source.elements) {
        if ($original.to[0] -le $part.X -or $original.from[0] -ge ($part.X + 16)) {
            continue
        }
        $minY = [Math]::Max($original.from[1], $part.Y)
        $maxY = [Math]::Min($original.to[1], $part.Y + 16)
        if ($minY -ge $maxY) {
            continue
        }
        if ($original.from[0] -lt $part.X -or $original.to[0] -gt ($part.X + 16)) {
            throw "Element '$($original.name)' crosses the column boundary; split it in the source model first."
        }
        if ($original.rotation -and $original.rotation.angle -ne 0) {
            throw "Rotated element '$($original.name)' requires explicit clipping support."
        }

        $element = $original | ConvertTo-Json -Depth 100 | ConvertFrom-Json
        $element.from = @(($original.from[0] - $part.X), ($minY - $part.Y), $original.from[2])
        $element.to = @(($original.to[0] - $part.X), ($maxY - $part.Y), $original.to[2])
        $element.PSObject.Properties.Remove('rotation')

        # Do not create internal caps where a tall frame element crosses two blocks.
        if ($minY -gt $original.from[1]) {
            $element.faces.PSObject.Properties.Remove('down')
        }
        if ($maxY -lt $original.to[1]) {
            $element.faces.PSObject.Properties.Remove('up')
        }
        if ($minY -ne $original.from[1] -or $maxY -ne $original.to[1]) {
            foreach ($faceName in @('north', 'east', 'south', 'west')) {
                $face = $element.faces.$faceName
                if ($null -eq $face) {
                    continue
                }
                if ($face.rotation -and $face.rotation -ne 0) {
                    throw "Rotated UVs on '$($original.name)' require explicit clipping support."
                }
                if ($null -eq $face.uv) {
                    throw "Missing explicit UVs on '$($original.name)'."
                }
                $height = $original.to[1] - $original.from[1]
                $vStart = $face.uv[1]
                $vSpan = $face.uv[3] - $vStart
                # Face V runs from the original element's top to its bottom.
                $face.uv[1] = $vStart + $vSpan * ($original.to[1] - $maxY) / $height
                $face.uv[3] = $vStart + $vSpan * ($original.to[1] - $minY) / $height
            }
        }
        $elements += $element
    }

    $model = [ordered]@{
        credit = 'Generated from seismic_station.json by tools/generate_station_models.ps1'
        parent = 'creategeoresonance:block/seismic_station'
        ambientocclusion = $false
        render_type = 'minecraft:cutout_mipped'
        elements = @($elements)
    }
    $path = Join-Path $modelDir "seismic_station_$($part.Name).json"
    $json = ($model | ConvertTo-Json -Depth 100) + [Environment]::NewLine
    [IO.File]::WriteAllText($path, $json, [Text.UTF8Encoding]::new($false))
    Write-Output "$($part.Name): $($elements.Count) elements"
}
