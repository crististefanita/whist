#requires -Version 7.0
param(
    [string]$ProjectRoot = '',
    [Parameter(Mandatory)][string]$CandidateDirectory,
    [Parameter(Mandatory)][string]$SourceDirectory,
    [Parameter(Mandatory)][string]$PlanFile,
    [Parameter(Mandatory)][string]$VerdictFile,
    [string]$GithubRetentionFile = '',
    [string]$Maven = 'mvn'
)

$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'Resolve-ReleaseToolsProject.ps1')
. (Join-Path $PSScriptRoot 'Invoke-ReleaseToolsJavaGate.ps1')
$root = Resolve-ReleaseToolsProject $ProjectRoot
$candidate = (Resolve-Path -LiteralPath $CandidateDirectory).Path
$source = (Resolve-Path -LiteralPath $SourceDirectory).Path
$plan = (Resolve-Path -LiteralPath $PlanFile).Path
$output = Assert-ReleaseToolsExternalPath $VerdictFile @($root, $candidate, $source, $plan, $GithubRetentionFile)
$outputParent = Split-Path -Parent $output
if (-not (Test-Path -LiteralPath $outputParent -PathType Container)) { throw 'RAG_VERDICT_PARENT_MISSING' }
Invoke-ReleaseToolsJavaGate $root @('verify-evidence', $candidate, 'release-validation.json') $Maven
$node = Get-Command node -ErrorAction Stop
$engine = Join-Path $root 'atf-tests/tools/evaluate-release-rag.mjs'
$arguments = @($engine, '--project-root', $root, '--candidate-dir', $candidate, '--source-root', $source,
    '--plan-file', $plan, '--output', $output)
if ($GithubRetentionFile) { $arguments += @('--github-retention-file', (Resolve-Path -LiteralPath $GithubRetentionFile).Path) }
& $node.Source @arguments
$ragExit = $LASTEXITCODE
if ($ragExit -notin @(0, 2)) { throw "RAG_ENGINE_FAILED:$ragExit" }
if (-not (Test-Path -LiteralPath $output -PathType Leaf)) { throw 'RAG_VERDICT_MISSING' }

Invoke-ReleaseToolsJavaGate $root @('verify-rag', $root, $candidate, $source, $output) $Maven
if ($ragExit -ne 0) { exit $ragExit }
