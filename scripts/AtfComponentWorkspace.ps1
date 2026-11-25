#requires -Version 7.0

function Assert-AtfDirectWorkspacePath([string]$Path) {
    for ($entry = [IO.Path]::GetFullPath($Path); $entry; $entry = Split-Path -Parent $entry) {
        if (([IO.File]::Exists($entry) -or [IO.Directory]::Exists($entry)) -and
                ([IO.File]::GetAttributes($entry) -band [IO.FileAttributes]::ReparsePoint)) {
            throw 'COMPONENT_WORKSPACE_INDIRECT'
        }
    }
}

function Read-AtfComponentWorkspace([string]$Previous, [switch]$RequireSnapshot) {
    $file = Join-Path $Previous 'component-workspace.json'
    Assert-AtfDirectWorkspacePath $file
    $pointer = Get-Content -LiteralPath $file -Raw | ConvertFrom-Json
    if (($pointer.schemaVersion -isnot [int] -and $pointer.schemaVersion -isnot [long]) -or
            $pointer.schemaVersion -notin @(1,2) -or $pointer.path -isnot [string] -or !$pointer.path) {
        throw 'COMPONENT_RESUME_WORKSPACE_RECEIPT'
    }
    $snapshot = $null
    $includeReleasePayload = $true
    if ($RequireSnapshot -or $pointer.schemaVersion -eq 2) {
        $sourceFile = Join-Path $Previous 'source-inputs.json'
        $releaseFile = Join-Path $Previous 'release-inputs.json'
        Assert-AtfDirectWorkspacePath $sourceFile
        Assert-AtfDirectWorkspacePath $releaseFile
        $source = Get-Content -LiteralPath $sourceFile -Raw
        $release = Get-Content -LiteralPath $releaseFile -Raw
        $rows = ConvertFrom-Json -InputObject $source -NoEnumerate
        $releaseRows = ConvertFrom-Json -InputObject $release -NoEnumerate
        if ($rows -isnot [array] -or !$rows.Count -or
                $releaseRows -isnot [array] -or !$releaseRows.Count -or
                $pointer.sourceFingerprint -cne (Get-AtfInputFingerprint $source) -or
                $pointer.releaseFingerprint -cne (Get-AtfInputFingerprint $release)) {
            throw 'COMPONENT_RESUME_SNAPSHOT_RECEIPT'
        }
        if ($pointer.PSObject.Properties.Name -contains 'includeReleasePayload') {
            if ($pointer.includeReleasePayload -isnot [bool]) { throw 'COMPONENT_RESUME_RELEASE_PAYLOAD' }
            $includeReleasePayload = $pointer.includeReleasePayload
        }
        $snapshot = [pscustomobject]@{source=$source;release=$release;inputCount=$rows.Count;includeReleasePayload=$includeReleasePayload}
        if ($pointer.schemaVersion -eq 2) {
            $expected = Get-AtfQualifiedCatalogContext $snapshot
            $context = $pointer.qualificationContext
            if (!$context -or @($context.PSObject.Properties).Count -ne $expected.Count) { throw 'COMPONENT_RESUME_CONTEXT' }
            foreach ($key in $expected.Keys) {
                if (($expected[$key] -is [string] -and $context.$key -isnot [string]) -or
                        ($key -eq 'schemaVersion' -and $context.$key -isnot [long] -and $context.$key -isnot [int]) -or
                        $context.$key -cne $expected[$key]) { throw 'COMPONENT_RESUME_CONTEXT' }
            }
            foreach ($key in @('sourceSha256','releaseSha256','catalogSha256','identitySha256')) {
                if ($context.$key -cnotmatch '^[a-f0-9]{64}$') { throw 'COMPONENT_RESUME_CONTEXT' }
            }
        } elseif ($null -ne $pointer.qualificationContext) {
            throw 'COMPONENT_RESUME_CONTEXT'
        }
    } elseif ($null -ne $pointer.qualificationContext) { throw 'COMPONENT_RESUME_CONTEXT' }
    [pscustomobject]@{pointer=$pointer;snapshot=$snapshot}
}

function Write-AtfComponentWorkspace([string]$RunRoot, [string]$Work, $Snapshot) {
    Write-AtfComponentJson (Join-Path $RunRoot 'component-workspace.json') ([ordered]@{
        schemaVersion=2;qualificationContext=(Get-AtfQualifiedCatalogContext $Snapshot);path=$Work
        sourceFingerprint=(Get-AtfInputFingerprint $Snapshot.source)
        releaseFingerprint=(Get-AtfInputFingerprint $Snapshot.release)
        includeReleasePayload=$(if ($null -eq $Snapshot.includeReleasePayload) { $true } else { [bool]$Snapshot.includeReleasePayload })
    })
}
