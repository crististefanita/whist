#requires -Version 7.0
param(
    [string]$RunRoot = '',
    [string]$SourceRoot = (Split-Path -Parent $PSScriptRoot),
    [string]$PinnedSourceInputs = '',
    [string]$TestChangeBaseRef = '',
    [ValidateSet('AtfQualityGovernanceSuite', 'AtfRagGovernanceSuite')][string]$GovernanceSuite = 'AtfQualityGovernanceSuite',
    [string]$Maven = 'mvn',
    [string]$JavaHome = $env:JAVA_HOME,
    [string]$MavenCache = (Join-Path $env:USERPROFILE '.m2/repository')
)
$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'AtfLocalPaths.ps1')
. (Join-Path $PSScriptRoot 'AtfQualificationSnapshot.ps1')
. (Join-Path $PSScriptRoot 'AtfQualificationTiming.ps1')
. (Join-Path $PSScriptRoot 'AtfTestChangeAdmission.ps1')
$sourceRoot = [IO.Path]::GetFullPath($SourceRoot)
$evidenceRoot = if (-not [string]::IsNullOrWhiteSpace($env:WHIST_EVIDENCE_ROOT) -or [string]::IsNullOrWhiteSpace($RunRoot)) {
    Ensure-AtfEvidenceRoot $sourceRoot
} else { $null }
if ([string]::IsNullOrWhiteSpace($RunRoot)) {
    $RunRoot = Join-Path $evidenceRoot ('atf-governance\' + (Get-Date -Format 'yyyyMMdd-HHmmss') + '-' + [guid]::NewGuid().ToString('N').Substring(0,8))
}
$RunRoot = [IO.Path]::GetFullPath($RunRoot)
if ($RunRoot.Equals($sourceRoot, [StringComparison]::OrdinalIgnoreCase) -or
    $RunRoot.StartsWith($sourceRoot + [IO.Path]::DirectorySeparatorChar, [StringComparison]::OrdinalIgnoreCase)) {
    throw 'Results must remain outside the source checkout.'
}
if (Test-Path -LiteralPath $RunRoot) { throw 'The results directory must be new.' }
$tempRoot = Get-AtfQualificationTempRoot $sourceRoot
New-Item -ItemType Directory -Path $RunRoot | Out-Null
$timing = New-AtfQualificationTiming 'governance' (Join-Path $RunRoot 'timing.json')
function Assert-GovernanceTestChangeAdmission([string]$Receipt) {
    Invoke-AtfTestChangeAdmission -SourceRoot $sourceRoot -BaseRef $TestChangeBaseRef -TempRoot $tempRoot `
        -JavaHome $JavaHome -MavenCache $MavenCache -ReceiptFile (Join-Path $RunRoot $Receipt)
}
Invoke-AtfQualificationPhase $timing 'governance.test-change-admission' {
    Assert-GovernanceTestChangeAdmission 'test-change-admission.json'
}
Invoke-AtfQualificationPhase $timing 'governance.metadata-preflight' {
    Assert-AtfQualificationMetadata $sourceRoot $RunRoot $tempRoot $JavaHome $MavenCache
}
$work = Join-Path $tempRoot ('atf-quality-' + [guid]::NewGuid().ToString('N'))
$stage = Join-Path $work 'project'
New-Item -ItemType Directory -Path $stage | Out-Null
$snapshot = Invoke-AtfQualificationPhase $timing 'governance.snapshot-preflight' {
    New-AtfQualificationSnapshot $sourceRoot $stage $RunRoot $PinnedSourceInputs $timing
}
Invoke-AtfQualificationPhase $timing 'governance.metadata-recheck' {
    Assert-AtfMetadataSnapshot $stage $RunRoot
}
Invoke-AtfQualificationPhase $timing 'governance.test-change-recheck' {
    Assert-GovernanceTestChangeAdmission 'test-change-admission-recheck.json'
}
$localRepository = Join-Path $work 'maven-repository'
$mavenTempRoot = Join-Path $work 'maven-temp'
New-Item -ItemType Directory -Path $localRepository | Out-Null
New-Item -ItemType Directory -Path $mavenTempRoot | Out-Null
Invoke-AtfQualificationPhase $timing 'governance.maven-cache' {
    if (Test-Path -LiteralPath $MavenCache -PathType Container) {
        & robocopy $MavenCache $localRepository /E /R:1 /W:1 /XD atf .locks /XF *.lock *.tmp *.lastUpdated /NFL /NDL /NJH /NJS /NP *> (Join-Path $RunRoot 'copy-maven-cache.log')
        if ($LASTEXITCODE -gt 7) { throw 'Maven cache copying failed.' }
    }
}
$savedJavaHome = $env:JAVA_HOME
$savedTemp = $env:TEMP
$savedTmp = $env:TMP
try {
    if ($JavaHome) { $env:JAVA_HOME = $JavaHome }
    $env:TEMP = $mavenTempRoot
    $env:TMP = $mavenTempRoot
    Push-Location $stage
    try {
        Invoke-AtfQualificationPhase $timing 'governance.pre-maven-snapshot' {
            Assert-AtfQualificationSnapshot $snapshot $stage $sourceRoot
        }
        $testExit = Invoke-AtfQualificationPhase $timing 'governance.maven' {
            $review = @()
            if ($TestChangeBaseRef) { $review += "-Datf.governance.testChanges.baseRef=$TestChangeBaseRef" }
            $suiteSelector = '-Dtest=' + $GovernanceSuite
            & $Maven @review -B "-Dmaven.repo.local=$localRepository" -pl atf-tests -am $suiteSelector '-Dsurefire.failIfNoSpecifiedTests=false' "-Datf.governance.root=$stage" "-Datf.governance.gitRoot=$sourceRoot" test *> (Join-Path $RunRoot 'maven.log')
            $LASTEXITCODE
        } -ExitCode
    } finally { Pop-Location }
} finally {
    $env:JAVA_HOME = $savedJavaHome
    $env:TEMP = $savedTemp
    $env:TMP = $savedTmp
}
$reports = Join-Path $stage 'atf-tests/target/surefire-reports'
if (Test-Path -LiteralPath $reports) { Copy-Item -LiteralPath $reports -Destination (Join-Path $RunRoot 'junit') -Recurse }
$extentStatus = 'NOT_REQUIRED'
$extentPass = $true
if ($GovernanceSuite -eq 'AtfRagGovernanceSuite') {
    $extentDirectory = Join-Path $mavenTempRoot 'whist-component-reports'
    $extentFiles = if (Test-Path -LiteralPath $extentDirectory -PathType Container) {
        @(Get-ChildItem -LiteralPath $extentDirectory -Filter '*.html' -File)
    } else { @() }
    $extentReport = $null
    $extentDetails = @()
    if ($extentFiles.Count -ne 1) {
        $extentDetails += "EXPECTED_ONE_EXTENT_REPORT:found=$($extentFiles.Count)"
    } else {
        $reportText = [IO.File]::ReadAllText($extentFiles[0].FullName)
        $trimmedReport = $reportText.TrimStart()
        $isHtml = $trimmedReport.StartsWith('<!doctype html>', [StringComparison]::OrdinalIgnoreCase) -or
            $trimmedReport.StartsWith('<html', [StringComparison]::OrdinalIgnoreCase)
        if (-not $isHtml) {
            $extentDetails += 'EXTENT_REPORT_IS_NOT_HTML'
        } else {
            $destination = Join-Path $RunRoot 'component-extent.html'
            Copy-Item -LiteralPath $extentFiles[0].FullName -Destination $destination
            $extentReport = [ordered]@{ path='component-extent.html'; bytes=(Get-Item -LiteralPath $destination).Length
                sha256=(Get-FileHash -LiteralPath $destination -Algorithm SHA256).Hash.ToLowerInvariant() }
        }
    }
    $extentPass = $null -ne $extentReport -and $extentDetails.Count -eq 0
    $extentStatus = if ($extentPass) { 'PASS' } else { 'FAIL' }
    $reportManifest = [ordered]@{ schemaVersion=1; kind='ATF_EXTENT_COMPONENT_SUITE'; suite=$GovernanceSuite
        status=$extentStatus; report=$extentReport; diagnostics=$extentDetails }
    $reportManifest | ConvertTo-Json -Depth 5 | Set-Content -LiteralPath (Join-Path $RunRoot 'governance-report-manifest.json') -Encoding utf8
}
$differences = @(Invoke-AtfQualificationPhase $timing 'governance.snapshot-recheck' {
    Get-AtfSnapshotDifferences $snapshot $stage $sourceRoot
})
$stable = $differences.Count -eq 0
$result = [ordered]@{ root=$sourceRoot; stage=$stage; mavenRepository=$localRepository; output=$RunRoot
    sourceFiles=$snapshot.inputCount; sourceFingerprint=Get-AtfInputFingerprint $snapshot.source
    releaseFingerprint=Get-AtfInputFingerprint $snapshot.release; releaseInputManifest='release-inputs.json'
    stable=$stable; snapshotDifferences=$differences; testExit=$testExit; extentReportStatus=$extentStatus
    status=if ($stable -and $testExit -eq 0 -and $extentPass) {'PASS'} else {'FAIL'} }
$result | ConvertTo-Json | Set-Content -LiteralPath (Join-Path $RunRoot 'governance-result.json') -Encoding utf8
Write-Output ($result | ConvertTo-Json -Compress)
if (-not $stable) { throw 'The qualification snapshot changed during verification. Inspect $RunRoot/governance-result.json.' }
if ($testExit -ne 0) { throw "Governance failed. Inspect $RunRoot\maven.log and JUnit results." }
if (-not $extentPass) { throw "The RAG suite Extent report is missing or invalid. Inspect $RunRoot\governance-report-manifest.json." }
