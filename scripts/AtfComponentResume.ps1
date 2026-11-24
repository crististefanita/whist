#requires -Version 7.0
. (Join-Path $PSScriptRoot 'AtfComponentWorkspace.ps1')

function Write-AtfComponentJson([string]$File, $Document) {
    $temporary = $File + '.writing'
    [IO.File]::WriteAllText($temporary, (($Document | ConvertTo-Json -Depth 12) + [Environment]::NewLine))
    [IO.File]::Move($temporary, $File, $true)
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
