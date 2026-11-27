#requires -Version 7.0
param(
    [string]$ProjectRoot = '',
    [Parameter(Mandatory)][ValidateSet('publish', 'verify-rotation')][string]$Mode,
    [Parameter(Mandatory)][string]$QualifiedCandidate,
    [Parameter(Mandatory)][string]$VerdictFile,
    [Parameter(Mandatory)][string]$ExternalJournalDirectory
)

$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'Resolve-ReleaseToolsProject.ps1')
$root = Resolve-ReleaseToolsProject $ProjectRoot
$candidate = (Resolve-Path -LiteralPath $QualifiedCandidate).Path
$verdict = (Resolve-Path -LiteralPath $VerdictFile).Path
$journal = Assert-ReleaseToolsExternalPath $ExternalJournalDirectory @($root, $candidate, $verdict)
if (-not (Test-Path -LiteralPath $journal -PathType Container)) { throw 'GITHUB_JOURNAL_DIRECTORY_MISSING' }
& (Get-Command node -ErrorAction Stop).Source (Join-Path $root 'atf-tests/tools/publish-github-release.mjs') `
    $Mode $root $candidate $verdict $journal
if ($LASTEXITCODE -ne 0) { throw "GITHUB_PUBLICATION_FAILED:$LASTEXITCODE" }
