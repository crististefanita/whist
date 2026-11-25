#requires -Version 7.0
. (Join-Path $PSScriptRoot 'AtfComponentWorkspace.ps1')

function Write-AtfComponentJson([string]$File, $Document, [ref]$Measurement) {
    if ($Measurement) { $Measurement.Value = $null }
    $started = [Diagnostics.Stopwatch]::GetTimestamp()
    $json = ($Document | ConvertTo-Json -Depth 12) + [Environment]::NewLine
    $serialized = [Diagnostics.Stopwatch]::GetTimestamp()
    $temporary = $File + '.writing'
    [IO.File]::WriteAllText($temporary, $json)
    [IO.File]::Move($temporary, $File, $true)
    if ($Measurement) {
        $ended = [Diagnostics.Stopwatch]::GetTimestamp()
        $Measurement.Value = [ordered]@{schemaVersion=1;kind='ATF_JSON_WRITE_COST';qualified=$false
            frequency=[Diagnostics.Stopwatch]::Frequency;totalTicks=($ended-$started)
            serializationTicks=($serialized-$started);publicationTicks=($ended-$serialized)
            bytes=[Text.Encoding]::UTF8.GetByteCount($json)}
    }
}

function Get-AtfResumeComponentWorkspace([string]$Previous, [string]$TempRoot) {
    $document = (Read-AtfComponentWorkspace $Previous).pointer
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
    $bound = Read-AtfComponentWorkspace $Previous -RequireSnapshot
    if ([IO.Path]::GetFullPath($bound.pointer.path) -ine [IO.Path]::GetFullPath($Work)) { throw 'COMPONENT_RESUME_WORKSPACE_RECEIPT' }
    $snapshot = $bound.snapshot
    $inputs = $snapshot.source
    if ($PinnedSourceInputs) {
        $pin = Get-Content -LiteralPath $PinnedSourceInputs -Raw | ConvertFrom-Json -NoEnumerate
        if ($pin -isnot [array] -or (ConvertTo-AtfInputJson @($pin | Select-Object path,sha256)) -cne $inputs) {
            throw 'QUALIFICATION-SOURCE-DIFFERS-FROM-PLAN'
        }
    }
    Assert-AtfQualificationSnapshot $snapshot (Join-Path $Work 'project') $SourceRoot
    $snapshot
}
