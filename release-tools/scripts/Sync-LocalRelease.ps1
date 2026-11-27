#requires -Version 7.0
param(
    [string]$ProjectRoot = '',
    [Parameter(Mandatory)][string]$PublishedCandidate,
    [Parameter(Mandatory)][string]$PublishedVerdict,
    [Parameter(Mandatory)][string]$ExternalReceipt,
    [Parameter(Mandatory)][string]$LocalReleaseDirectory
)

$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'Resolve-ReleaseToolsProject.ps1')
$root = Resolve-ReleaseToolsProject $ProjectRoot
$candidate = (Resolve-Path -LiteralPath $PublishedCandidate).Path
$verdict = (Resolve-Path -LiteralPath $PublishedVerdict).Path
$receipt = Assert-ReleaseToolsExternalPath $ExternalReceipt @($root, $candidate, $verdict, $LocalReleaseDirectory)
$local = [IO.Path]::GetFullPath($LocalReleaseDirectory)
if (Test-Path -LiteralPath $receipt) { throw 'LOCAL_RELEASE_RECEIPT_ALREADY_EXISTS' }
if (-not (Test-Path -LiteralPath (Split-Path -Parent $receipt) -PathType Container)) { throw 'LOCAL_RELEASE_RECEIPT_PARENT_MISSING' }
& (Get-Command node -ErrorAction Stop).Source (Join-Path $root 'atf-tests/tools/verify-release-git-snapshot.mjs') `
    --repo $root --candidate $candidate --output $receipt --published-verdict $verdict --local-release $local
if ($LASTEXITCODE -ne 0) { throw "LOCAL_RELEASE_SYNC_FAILED:$LASTEXITCODE" }
