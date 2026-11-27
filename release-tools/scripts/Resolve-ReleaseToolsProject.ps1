#requires -Version 7.0

function Resolve-ReleaseToolsProject([string]$ConfiguredRoot = '') {
    $moduleRoot = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
    $scriptProjectRoot = [IO.Path]::GetFullPath((Join-Path $moduleRoot '..'))
    $hint = if (-not [string]::IsNullOrWhiteSpace($ConfiguredRoot)) { $ConfiguredRoot }
        elseif (-not [string]::IsNullOrWhiteSpace($env:ATF_PROJECT_ROOT)) { $env:ATF_PROJECT_ROOT }
        else { '' }
    $candidate = if ($hint) { [IO.Path]::GetFullPath($hint, $scriptProjectRoot) } else { $scriptProjectRoot }
    $markers = @('pom.xml', 'release-tools/config/release-platform-rag-policy.json', 'release/index.json')
    $missingMarkers = @($markers | Where-Object { -not (Test-Path -LiteralPath (Join-Path $candidate $_) -PathType Leaf) })
    if (-not (Test-Path -LiteralPath $candidate -PathType Container) -or $missingMarkers.Count -gt 0) {
        throw 'RELEASE_PROJECT_ROOT_INVALID: expected the Maven reactor, RAG policy and release catalog.'
    }
    $resolved = (Resolve-Path -LiteralPath $candidate).Path
    $selectedModule = [IO.Path]::GetFullPath((Join-Path $resolved 'release-tools'))
    if (-not $selectedModule.Equals($moduleRoot, [StringComparison]::OrdinalIgnoreCase)) {
        throw 'RELEASE_PROJECT_ROOT_MISMATCH: the selected project root must own this release-tools checkout.'
    }
    return $resolved
}

function Assert-ReleaseToolsExternalPath([string]$Path, [string[]]$Inputs) {
    $target = [IO.Path]::GetFullPath($Path)
    foreach ($input in $Inputs) {
        if ([string]::IsNullOrWhiteSpace($input)) { continue }
        $root = [IO.Path]::GetFullPath($input).TrimEnd([char[]]@('\', '/'))
        $prefix = $root + [IO.Path]::DirectorySeparatorChar
        $insideInput = $target.Equals($root, [StringComparison]::OrdinalIgnoreCase) -or
            $target.StartsWith($prefix, [StringComparison]::OrdinalIgnoreCase)
        if ($insideInput) {
            throw 'RELEASE_TOOLS_OUTPUT_MUST_BE_EXTERNAL'
        }
    }
    return $target
}
