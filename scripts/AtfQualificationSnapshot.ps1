#requires -Version 7.0

function Get-AtfQualificationTempRoot([string]$SourceRoot) {
    $checker = Join-Path $PSScriptRoot '../atf-tests/tools/qualification-temp-root.mjs'
    $temp = if (-not [string]::IsNullOrWhiteSpace($env:WHIST_QUALIFICATION_TEMP_ROOT)) {
        $env:WHIST_QUALIFICATION_TEMP_ROOT
    } else { [IO.Path]::GetTempPath() }
    $result = & node $checker $SourceRoot $temp 2>&1
    if ($LASTEXITCODE -ne 0) { throw ($result -join [Environment]::NewLine) }
    $result
}

function Get-AtfReleaseInputs([string]$Root) {
    $release = Join-Path $Root 'release'
    $rows = [Collections.Generic.List[object]]::new()
    function Visit-ReleaseDirectory([string]$Path) {
        $directory = Get-Item -LiteralPath $Path -Force
        if (-not $directory.PSIsContainer -or ($directory.Attributes -band [IO.FileAttributes]::ReparsePoint)) {
            throw "RELEASE-SNAPSHOT-DIRECTORY: $Path"
        }
        foreach ($entry in Get-ChildItem -LiteralPath $Path -Force) {
            $relative = [IO.Path]::GetRelativePath($Root, $entry.FullName).Replace('\', '/')
            if ($entry.Attributes -band [IO.FileAttributes]::ReparsePoint) { throw "RELEASE-SNAPSHOT-INDIRECT: $relative" }
            if ($entry.PSIsContainer) { Visit-ReleaseDirectory $entry.FullName }
            else {
                $rows.Add([pscustomobject]@{path=$relative;sha256=(Get-FileHash -LiteralPath $entry.FullName -Algorithm SHA256).Hash.ToLowerInvariant()})
            }
        }
    }
    Visit-ReleaseDirectory $release
    $rows.Sort([Comparison[object]]{param($left,$right) [StringComparer]::Ordinal.Compare($left.path,$right.path)})
    $rows.ToArray()
}

function ConvertTo-AtfInputJson([object[]]$Rows) {
    ConvertTo-Json -InputObject $Rows -Depth 5 -Compress
}

function ConvertTo-AtfPortableDiagnostic(
    [string]$Text,
    [string]$SourceRoot,
    [string]$Stage,
    [string]$WorkRoot,
    [string]$RunRoot) {
    $portable = $Text
    $locations = @(
        [pscustomobject]@{Path=$Stage; Token='<STAGE>'},
        [pscustomobject]@{Path=$RunRoot; Token='<EVIDENCE>'},
        [pscustomobject]@{Path=$WorkRoot; Token='<WORK>'},
        [pscustomobject]@{Path=$SourceRoot; Token='<SOURCE>'}
    )
    foreach ($location in $locations) {
        if ([string]::IsNullOrWhiteSpace($location.Path)) { continue }
        $fullPath = [IO.Path]::GetFullPath($location.Path).TrimEnd([char[]]@('\', '/'))
        foreach ($variant in @($fullPath, $fullPath.Replace('\', '/'))) {
            if ($variant) {
                $portable = [regex]::Replace($portable, [regex]::Escape($variant), $location.Token,
                    [Text.RegularExpressions.RegexOptions]::IgnoreCase)
            }
        }
    }
    return $portable
}
function Get-AtfInputFingerprint([string]$Json) {
    [Convert]::ToHexString([Security.Cryptography.SHA256]::HashData([Text.Encoding]::UTF8.GetBytes($Json))).ToLowerInvariant()
}

function Get-AtfSnapshotDifferences($Snapshot, [string]$Stage, [string]$SourceRoot, [string]$CaptureRoot = '') {
    $differences = [Collections.Generic.List[string]]::new()
    $observations = [Collections.Generic.List[object]]::new()
    foreach ($location in @(@{name='STAGED';root=$Stage}, @{name='LIVE';root=$SourceRoot})) {
        foreach ($scope in @('SOURCE','RELEASE')) {
            try {
                $observed = if ($scope -eq 'SOURCE') {
                    @(& (Join-Path $PSScriptRoot 'Get-AtfSourceInputs.ps1') -SourceRoot $location.root)
                } else { @(Get-AtfReleaseInputs $location.root) }
                $observedJson = ConvertTo-AtfInputJson $observed
                $observations.Add(@{path=('observed-' + $location.name.ToLowerInvariant() + '-' + $scope.ToLowerInvariant() + '.json');json=$observedJson})
                $expected = if ($scope -eq 'SOURCE') { $Snapshot.source } else { $Snapshot.release }
                if ($observedJson -cne $expected) {
                    $before = [Collections.Generic.Dictionary[string,string]]::new([StringComparer]::Ordinal)
                    $after = [Collections.Generic.Dictionary[string,string]]::new([StringComparer]::Ordinal)
                    foreach ($row in ($expected | ConvertFrom-Json)) { $before[$row.path] = $row.sha256 }
                    foreach ($row in $observed) { $after[$row.path] = $row.sha256 }
                    $paths = [Collections.Generic.SortedSet[string]]::new([StringComparer]::Ordinal)
                    $paths.UnionWith($before.Keys)
                    $paths.UnionWith($after.Keys)
                    foreach ($path in $paths) {
                        if ($before[$path] -cne $after[$path]) { $differences.Add("$($location.name)-$($scope): $path") }
                    }
                }
            } catch { $differences.Add("$($location.name)-$($scope): $($_.Exception.Message)") }
        }
    }
    if ($CaptureRoot -and $differences.Count -and $observations.Count -eq 4) {
        $captured = @($observations | ForEach-Object {
            [IO.File]::WriteAllText((Join-Path $CaptureRoot $_.path), $_.json)
            [ordered]@{path=$_.path;sha256=(Get-AtfInputFingerprint $_.json)}
        })
        $receipt = [ordered]@{schemaVersion=1;kind='ATF_COMPONENT_SOURCE_CHECK'
            expectedSourceSha256=(Get-AtfInputFingerprint $Snapshot.source)
            expectedReleaseSha256=(Get-AtfInputFingerprint $Snapshot.release)
            observations=$captured;differences=$differences.ToArray()}
        [IO.File]::WriteAllText((Join-Path $CaptureRoot 'source-snapshot-check.json'),
            (($receipt | ConvertTo-Json -Depth 6) + [Environment]::NewLine))
    }
    $differences.ToArray()
}

function Assert-AtfQualificationSnapshot($Snapshot, [string]$Stage, [string]$SourceRoot) {
    $differences = @(Get-AtfSnapshotDifferences $Snapshot $Stage $SourceRoot)
    if ($differences.Count) { throw "QUALIFICATION-SNAPSHOT-CHANGED: $($differences -join ', ')" }
}

function New-AtfQualificationSnapshot([string]$SourceRoot, [string]$Stage, [string]$RunRoot, [string]$PinnedSourceInputs) {
    $sources = @(& (Join-Path $PSScriptRoot 'Get-AtfSourceInputs.ps1') -SourceRoot $SourceRoot)
    $release = @(Get-AtfReleaseInputs $SourceRoot)
    $snapshot = [pscustomobject]@{
        source=ConvertTo-AtfInputJson $sources
        release=ConvertTo-AtfInputJson $release
        inputCount=$sources.Count
    }
    if ($PinnedSourceInputs) {
        $pinned = Get-Content -LiteralPath $PinnedSourceInputs -Raw | ConvertFrom-Json -NoEnumerate
        if ($pinned -isnot [array] -or
            (ConvertTo-AtfInputJson @($pinned | Select-Object path,sha256)) -cne $snapshot.source) {
            throw 'QUALIFICATION-SOURCE-DIFFERS-FROM-PLAN'
        }
    }

    $sourceManifest = Join-Path $RunRoot 'source-inputs.json'
    $releaseManifest = Join-Path $RunRoot 'release-inputs.json'
    [IO.File]::WriteAllText($sourceManifest, $snapshot.source)
    [IO.File]::WriteAllText($releaseManifest, $snapshot.release)
    $copyManifest = Join-Path (Split-Path -Parent $Stage) 'qualification-copy-inputs.json'
    [IO.File]::WriteAllText($copyManifest, (ConvertTo-AtfInputJson ($sources + $release)))
    & (Join-Path $PSScriptRoot 'Copy-AtfSourceInputs.ps1') -SourceRoot $SourceRoot -Destination $Stage -InputManifest $copyManifest
    Assert-AtfQualificationSnapshot $snapshot $Stage $SourceRoot
    $preflight = Join-Path $Stage 'atf-tests/tools/check-release-artifacts.mjs'
    & node $preflight $Stage *> (Join-Path $RunRoot 'release-preflight.log')
    if ($LASTEXITCODE -ne 0) {
        throw "RELEASE-PREFLIGHT: $(Get-Content -LiteralPath (Join-Path $RunRoot 'release-preflight.log') -Raw)"
    }
    $snapshot
}
