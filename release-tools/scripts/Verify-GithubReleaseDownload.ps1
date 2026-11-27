#requires -Version 7.0
param(
    [string]$ProjectRoot = '',
    [Parameter(Mandatory)][string]$ObservationFile
)

$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'Resolve-ReleaseToolsProject.ps1')
$root = Resolve-ReleaseToolsProject $ProjectRoot
$output = Assert-ReleaseToolsExternalPath $ObservationFile @($root)
if (Test-Path -LiteralPath $output) { throw 'GITHUB_OBSERVATION_ALREADY_EXISTS' }
if (-not (Test-Path -LiteralPath (Split-Path -Parent $output) -PathType Container)) { throw 'GITHUB_OBSERVATION_PARENT_MISSING' }
& (Get-Command node -ErrorAction Stop).Source (Join-Path $root 'atf-tests/tools/collect-github-release-retention.mjs') $root $output
if ($LASTEXITCODE -ne 0) { throw "GITHUB_DOWNLOAD_VERIFICATION_FAILED:$LASTEXITCODE" }
