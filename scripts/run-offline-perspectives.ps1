param(
    [long]$Seed = 20261104,
    [ValidateSet('OH_HELL','DESCENDING','ROMANIAN_WHIST','DOWN_AND_UP','LONG_ROMANIAN','REVERSE_OH_HELL')]
    [string]$Mode = 'ROMANIAN_WHIST',
    [string]$EvidenceRoot = 'C:\work\whist_back\offline-mock'
)
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
$repo = Join-Path $projectRoot 'whist-lab-app'
$stamp = Get-Date -Format 'yyyyMMdd-HHmmss'
$run = Join-Path $EvidenceRoot "perspectives-$Mode-$stamp-seed$Seed"
if (Test-Path -LiteralPath $run) { throw "Evidence run already exists: $run" }
New-Item -ItemType Directory -Path $run | Out-Null
& (Join-Path $PSScriptRoot 'Register-WhistDevelopmentExposure.ps1') -Seed $Seed -RunRoot $run -Kind perspectives
$match = Join-Path $run 'perspective-matches.json'
$log = Join-Path $run 'gradle.log'
$buildRoot = Join-Path $run 'gradle-build'
$projectCache = Join-Path $run 'project-cache'
$init = Join-Path $run 'external-build.gradle'
$initText = @'
allprojects {
    def targetRoot = System.getenv("WHIST_EXTERNAL_BUILD_ROOT")
    if (targetRoot != null && !targetRoot.isBlank()) {
        def relative = path == ":" ? "root" : path.substring(1).replace(':', File.separator)
        layout.buildDirectory.set(new File(targetRoot, relative))
    }
}
'@
[IO.File]::WriteAllText($init, $initText)
$env:JAVA_HOME = 'C:\tools\Java21\jdk-21.0.12.1+1'
$env:ANDROID_HOME = 'C:\tools\whist-android-sdk'
$previousBuildRoot = $env:WHIST_EXTERNAL_BUILD_ROOT
$env:WHIST_EXTERNAL_BUILD_ROOT = $buildRoot
Push-Location $repo
try {
    & 'C:\tools\gradle\gradle-9.6.1\bin\gradle.bat' --offline --no-daemon --max-workers=1 `
        --project-cache-dir $projectCache --init-script $init `
        '-Dorg.gradle.jvmargs=-Xmx512m -XX:ActiveProcessorCount=2' `
        :game-core:offlinePerspectiveMatchTest `
        "-Pwhist.offlinePerspective.seed=$Seed" `
        "-Pwhist.offlinePerspective.mode=$Mode" `
        "-Pwhist.offlinePerspective.output=$match" --console=plain *> $log
    if ($LASTEXITCODE -ne 0) { Get-Content -LiteralPath $log -Tail 30; throw 'Offline perspective replay failed' }
    $result = Get-Content -LiteralPath $match -Raw | ConvertFrom-Json
    $xmlPath = Join-Path $buildRoot 'game-core/test-results/offlinePerspectiveMatchTest/TEST-ro.whistlab.core.match.OfflinePerspectiveMatchTest.xml'
    $xml = [xml](Get-Content -LiteralPath $xmlPath -Raw)
    if ($result.runs -ne 6 -or [int]$result.handsPerRun -le 0 -or $result.mode -ne $Mode -or
        !$result.sameDistributionAndOrder -or $result.privateDealsIncluded -or
        $result.opaqueDealCommitmentsCompared -ne (5 * [int]$result.handsPerRun) -or
        $result.opaqueDealCommitmentsSha256 -notmatch '^[0-9a-f]{64}$' -or
        @($result.perspectives).Count -ne 6 -or
        @($result.perspectives | Where-Object { $_.match.mode -ne $Mode -or !$_.match.complete -or
            $_.match.handsPlayed -ne $result.handsPerRun }).Count -ne 0 -or
        [int]$xml.testsuite.failures -or [int]$xml.testsuite.errors) {
        throw 'Perspective replay receipt failed its independent final checks'
    }
    $normalBaseline = $result.perspectives | Where-Object { $_.name -eq 'normal-all' } | Select-Object -First 1
    $sameSeatComparisons = @(
        foreach ($seat in 0..3) {
            $hardPerspective = $result.perspectives | Where-Object { $_.name -eq "hard-seat-$seat" } | Select-Object -First 1
            $hardRows = @($hardPerspective.match.hands | ForEach-Object { $_.results | Where-Object { [int]$_.seat -eq $seat } })
            $normalRows = @($normalBaseline.match.hands | ForEach-Object { $_.results | Where-Object { [int]$_.seat -eq $seat } })
            $hardPoints = ($hardRows | Measure-Object -Property points -Sum).Sum
            $normalPoints = ($normalRows | Measure-Object -Property points -Sum).Sum
            $hardExact = @($hardRows | Where-Object { [int]$_.bid -eq [int]$_.tricksWon }).Count
            $normalExact = @($normalRows | Where-Object { [int]$_.bid -eq [int]$_.tricksWon }).Count
            [ordered]@{
                seat = $seat; hardPoints = $hardPoints; normalPointsSameSeat = $normalPoints
                pointsDelta = $hardPoints - $normalPoints; hardExact = $hardExact
                normalExactSameSeat = $normalExact; exactDelta = $hardExact - $normalExact
            }
        }
    )
    $receipt = [ordered]@{
        status = 'PASS'
        kind = 'SAME_DEALS_SEAT_ROTATED_NORMAL_HARD'
        mode = $Mode
        seed = $Seed
        runs = $result.runs
        handsPerRun = $result.handsPerRun
        sameDistributionAndOrder = $result.sameDistributionAndOrder
        opaqueCommitmentsCompared = $result.opaqueDealCommitmentsCompared
        privateDealsIncluded = $false
        publicCardDecisions = @($result.perspectives | ForEach-Object { $_.match.publicCardDecisions } | Measure-Object -Sum).Sum
        rosters = @($result.perspectives | ForEach-Object { $_.roster })
        sameSeatComparisons = $sameSeatComparisons
        rosterTotalsByPerspective = @($result.perspectives | ForEach-Object {
            [ordered]@{ name = $_.name; hardPoints = $_.hardPoints; normalPoints = $_.normalPoints;
                hardExactContracts = $_.hardExactContracts; normalExactContracts = $_.normalExactContracts }
        })
        rosterTotalsInterpretation = 'Aggregates by policy label; in a one-Hard roster, normalPoints sums the three opponents. Use sameSeatComparisons for focal-seat performance.'
        deviceExecuted = $false
        sourceSha256 = (Get-FileHash -LiteralPath (Join-Path $repo 'game-core/src/main/java/ro/whistlab/core/ai/policy/AiPolicy.java')).Hash.ToLowerInvariant()
        normalPolicySha256 = (Get-FileHash -LiteralPath (Join-Path $repo 'game-core/src/main/java/ro/whistlab/core/ai/policy/NormalPlayPolicy.java')).Hash.ToLowerInvariant()
        hardPolicySha256 = (Get-FileHash -LiteralPath (Join-Path $repo 'game-core/src/main/java/ro/whistlab/core/ai/engine/MonteCarloAi.java')).Hash.ToLowerInvariant()
        objectivesYamlSha256 = (Get-FileHash -LiteralPath (Join-Path $projectRoot 'config/whist-play-objectives.yaml')).Hash.ToLowerInvariant()
        matchSha256 = (Get-FileHash -LiteralPath $match).Hash.ToLowerInvariant()
        nativeTestXml = $xmlPath
    }
    $receipt | ConvertTo-Json -Depth 8 | Set-Content -LiteralPath (Join-Path $run 'receipt.json') -Encoding utf8
    Write-Output "PASS: $($result.runs) $Mode perspectives, same deal order verified, no hidden hands in output"
    Write-Output $run
} finally {
    Pop-Location
    if ($null -eq $previousBuildRoot) { Remove-Item Env:WHIST_EXTERNAL_BUILD_ROOT -ErrorAction SilentlyContinue }
    else { $env:WHIST_EXTERNAL_BUILD_ROOT = $previousBuildRoot }
}
