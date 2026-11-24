#requires -Version 7.0

function Write-AtfComponentJson([string]$File, $Document) {
    $temporary = $File + '.writing'
    [IO.File]::WriteAllText($temporary, (($Document | ConvertTo-Json -Depth 12) + [Environment]::NewLine))
    [IO.File]::Move($temporary, $File, $true)
}

function Get-AtfResumeComponentWorkspace([string]$Previous, [string]$TempRoot) {
    $document = Get-Content -LiteralPath (Join-Path $Previous 'component-workspace.json') -Raw | ConvertFrom-Json
    if ($document.schemaVersion -notin @(1,2) -or !$document.path) { throw 'COMPONENT_RESUME_WORKSPACE_RECEIPT' }
    $work = [IO.Path]::GetFullPath($document.path)
    $entry = Get-Item -LiteralPath $work -Force
    if ((Split-Path -Parent $work) -ine $TempRoot -or
        (Split-Path -Leaf $work) -cnotmatch '^whist-component-suites-[a-f0-9]{32}$' -or
        !$entry.PSIsContainer -or ($entry.Attributes -band [IO.FileAttributes]::ReparsePoint)) {
        throw 'COMPONENT_RESUME_WORKSPACE_PERIMETER'
    }
    $entry.FullName
}

function Restore-AtfComponentSnapshot([string]$Previous, [string]$Work, [string]$SourceRoot, [string]$PinnedSourceInputs) {
    $document = Get-Content -LiteralPath (Join-Path $Previous 'component-workspace.json') -Raw | ConvertFrom-Json
    $inputs = Get-Content -LiteralPath (Join-Path $Previous 'source-inputs.json') -Raw
    $release = Get-Content -LiteralPath (Join-Path $Previous 'release-inputs.json') -Raw
    $rows = ConvertFrom-Json -InputObject $inputs -NoEnumerate
    if ($document.sourceFingerprint -cne (Get-AtfInputFingerprint $inputs) -or
        $document.releaseFingerprint -cne (Get-AtfInputFingerprint $release) -or !$rows.Count) {
        throw 'COMPONENT_RESUME_SNAPSHOT_RECEIPT'
    }
    if ($PinnedSourceInputs) {
        $pin = Get-Content -LiteralPath $PinnedSourceInputs -Raw | ConvertFrom-Json -NoEnumerate
        if ($pin -isnot [array] -or (ConvertTo-AtfInputJson @($pin | Select-Object path,sha256)) -cne $inputs) {
            throw 'QUALIFICATION-SOURCE-DIFFERS-FROM-PLAN'
        }
    }
    $snapshot = [pscustomobject]@{source=$inputs;release=$release;inputCount=$rows.Count}
    if ($document.schemaVersion -eq 2) {
        $expected = Get-AtfQualifiedCatalogContext $snapshot
        $context = $document.qualificationContext
        if (!$context -or @($context.PSObject.Properties).Count -ne $expected.Count) { throw 'COMPONENT_RESUME_CONTEXT' }
        foreach ($key in $expected.Keys) {
            if ($expected[$key] -is [string] -and $context.$key -isnot [string]) { throw 'COMPONENT_RESUME_CONTEXT' }
            if ($key -eq 'schemaVersion' -and $context.$key -isnot [long] -and $context.$key -isnot [int]) { throw 'COMPONENT_RESUME_CONTEXT' }
            if ($context.$key -cne $expected[$key]) { throw 'COMPONENT_RESUME_CONTEXT' }
        }
    }
    Assert-AtfQualificationSnapshot $snapshot (Join-Path $Work 'project') $SourceRoot
    $snapshot
}
