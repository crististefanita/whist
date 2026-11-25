#requires -Version 7.0

function Get-AtfSelectionInputHashes([string]$Root) {
    $paths = @('config/atf-test-suites.json','config/gradle-app-collections.properties','config/atf-component-collections.json')
    $hashes = [ordered]@{}
    foreach ($path in $paths) {
        $file = Join-Path $Root $path
        if (!(Test-Path -LiteralPath $file -PathType Leaf)) { throw "COMPONENT-SELECTION-INPUT-MISSING: $path" }
        $hashes[$path] = (Get-FileHash -LiteralPath $file -Algorithm SHA256).Hash.ToLowerInvariant()
    }
    $hashes
}

function Resolve-AtfComponentSelection([string]$SourceRoot, [string[]]$RequestedGroups = @()) {
    $catalogFile = Join-Path $SourceRoot 'config/atf-test-suites.json'
    $gradleFile = Join-Path $SourceRoot 'config/gradle-app-collections.properties'
    $ownershipFile = Join-Path $SourceRoot 'config/atf-component-collections.json'
    foreach ($file in @($catalogFile, $gradleFile, $ownershipFile)) {
        if (!(Test-Path -LiteralPath $file -PathType Leaf)) { throw "COMPONENT-SELECTION-INPUT-MISSING: $file" }
    }

    $catalog = Get-Content -LiteralPath $catalogFile -Raw | ConvertFrom-Json
    $primary = @($catalog.suites | Where-Object { $_.role -eq 'primary' -and $_.layer -eq 'component' } | ForEach-Object {
        if (!$_.group) { $_ | Add-Member -NotePropertyName group -NotePropertyValue $_.module }
        $_
    })
    $properties = ConvertFrom-StringData (Get-Content -LiteralPath $gradleFile -Raw)
    $gradle = @($properties.Values | Sort-Object -Unique | ForEach-Object {
        $suffix = [Globalization.CultureInfo]::InvariantCulture.TextInfo.ToTitleCase($_)
        $task = 'testDebug' + $suffix + 'Components'
        [pscustomobject]@{group="whist-lab-app-$_";project='whist-lab-app/app';task=$task;path=":app:$task";selection=$task}
    })
    $gradle += [pscustomobject]@{group='whist-lab-app-atf-card-table-ui';project='whist-lab-app/atf-card-table-ui';task='testDebugUnitTest';path=':card-table-android:testDebugUnitTest';selection=':card-table-android:testDebugUnitTest'}
    $gradle += [pscustomobject]@{group='whist-lab-app-card-table-core';project='whist-lab-app/card-table-core';task='test';path=':card-table-core:test';selection=':card-table-core:test'}
    $gradle += [pscustomobject]@{group='whist-lab-app-rules';project='whist-lab-app/game-core';task='releaseRulesComponents';path=':game-core:releaseRulesComponents';selection=':game-core:releaseRulesComponents'}
    $known = @($primary.group) + @($gradle.group)
    if (@($known | Sort-Object -Unique).Count -ne $known.Count) { throw 'COMPONENT-SELECTION-CATALOG-DUPLICATE' }
    $requested = @($RequestedGroups)
    if (@($requested | Sort-Object -Unique).Count -ne $requested.Count) { throw 'COMPONENT-SELECTION-DUPLICATE' }
    if (@($requested | Where-Object { $_ -cnotin $known }).Count) { throw 'COMPONENT-SELECTION-UNKNOWN' }
    $selected = if ($requested.Count) { $requested } else { $known }

    $ownership = Get-Content -LiteralPath $ownershipFile -Raw | ConvertFrom-Json
    $releaseGroups = @($ownership.collections | Where-Object { $_.releaseSnapshot } | ForEach-Object { $_.group })
    if (@($releaseGroups | Where-Object { $_ -notin $known }).Count -or
            @($releaseGroups | Sort-Object -Unique).Count -ne $releaseGroups.Count) {
        throw 'COMPONENT-SELECTION-OWNERSHIP'
    }
    $includeRelease = @($selected | Where-Object { $_ -ceq 'atf-tests' -or $_ -cin $releaseGroups }).Count -gt 0
    [pscustomobject]@{groups=@($selected);primaryComponents=$primary;gradleGroups=$gradle
        releaseSnapshotGroups=$releaseGroups;includeReleasePayload=$includeRelease;inputHashes=(Get-AtfSelectionInputHashes $SourceRoot)}
}

function Assert-AtfComponentSelectionSnapshot([string]$Stage, $Selection) {
    $observed = Get-AtfSelectionInputHashes $Stage
    foreach ($path in $Selection.inputHashes.Keys) {
        if ($observed[$path] -cne $Selection.inputHashes[$path]) { throw "COMPONENT-SELECTION-SOURCE-CHANGED: $path" }
    }
}
