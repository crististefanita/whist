# Shared machine-local roots. SourceRoot remains checkout-relative; only generated data uses these roots.
function Get-AtfEvidenceRoot([string]$ConfiguredRoot = '') {
    $selected = if (-not [string]::IsNullOrWhiteSpace($ConfiguredRoot)) { $ConfiguredRoot }
        elseif (-not [string]::IsNullOrWhiteSpace($env:WHIST_EVIDENCE_ROOT)) { $env:WHIST_EVIDENCE_ROOT }
        else {
            $local = [Environment]::GetFolderPath([Environment+SpecialFolder]::LocalApplicationData)
            if ($local) { Join-Path $local 'WhistATF' } else { Join-Path ([IO.Path]::GetTempPath()) 'WhistATF' }
        }
    if (-not [IO.Path]::IsPathRooted($selected)) { throw 'WHIST_EVIDENCE_ROOT must be an absolute machine path.' }
    [IO.Path]::GetFullPath($selected)
}

function Ensure-AtfEvidenceRoot([string]$SourceRoot) {
    $root = Get-AtfEvidenceRoot
    $source = [IO.Path]::GetFullPath($SourceRoot).TrimEnd([char[]]@('\', '/'))
    $normalizedRoot = $root.TrimEnd([char[]]@('\', '/'))
    $sourcePrefix = $source + [IO.Path]::DirectorySeparatorChar
    if ($normalizedRoot.Equals($source, [StringComparison]::OrdinalIgnoreCase) -or
        $normalizedRoot.StartsWith($sourcePrefix, [StringComparison]::OrdinalIgnoreCase)) {
        throw 'QUALIFICATION-EVIDENCE-IN-CHECKOUT: WHIST_EVIDENCE_ROOT must be outside the source checkout.'
    }
    if (Test-Path -LiteralPath $root) {
        $entry = Get-Item -LiteralPath $root -Force
        if (-not $entry.PSIsContainer -or ($entry.Attributes -band [IO.FileAttributes]::ReparsePoint)) {
            throw 'QUALIFICATION-EVIDENCE-ROOT-INVALID: WHIST_EVIDENCE_ROOT must be a real directory.'
        }
    } else {
        New-Item -ItemType Directory -Path $root -Force | Out-Null
    }
    $root
}

function Get-AtfAndroidSdkRoot([string]$ConfiguredRoot = '') {
    $selected = if (-not [string]::IsNullOrWhiteSpace($ConfiguredRoot)) { $ConfiguredRoot }
        elseif (-not [string]::IsNullOrWhiteSpace($env:ANDROID_SDK_ROOT)) { $env:ANDROID_SDK_ROOT }
        else { $env:ANDROID_HOME }
    if ([string]::IsNullOrWhiteSpace($selected)) {
        throw 'Set ANDROID_SDK_ROOT or ANDROID_HOME, or pass -AndroidSdk for this machine.'
    }
    if (-not [IO.Path]::IsPathRooted($selected)) { throw 'ANDROID_SDK_ROOT must be an absolute machine path.' }
    [IO.Path]::GetFullPath($selected)
}
