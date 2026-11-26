#requires -Version 7.0

function Invoke-AtfTestChangeBatch([string]$RunRoot, [scriptblock]$ReadPlan, [scriptblock]$RunControl,
                                  [scriptblock]$CheckSnapshot) {
    $controls = @(& $ReadPlan)
    if (@($controls | Where-Object { $_ -cnotmatch '^[A-Z][A-Z0-9-]{3,79}$' }).Count -or
        @($controls | Sort-Object -Unique).Count -ne $controls.Count) {
        throw 'TEST-CHANGE-BATCH-PLAN: expected distinct control IDs'
    }
    $pending = Join-Path $RunRoot 'proofs.pending'
    $published = Join-Path $RunRoot 'proofs'
    if ((Test-Path -LiteralPath $pending) -or (Test-Path -LiteralPath $published)) {
        throw 'TEST-CHANGE-BATCH-OUTPUT: expected a fresh proof bundle'
    }
    New-Item -ItemType Directory -Path $pending -ErrorAction Stop | Out-Null
    $failures = [Collections.Generic.List[string]]::new()
    foreach ($id in $controls) {
        $output = Join-Path $pending $id
        $exitCode = & $RunControl $id $output
        $proof = Join-Path $output 'proof.pending.json'
        $report = Join-Path $output 'component-report.html'
        $proofReady = $false
        $reportReady = (Test-Path -LiteralPath $report -PathType Leaf) -and (Get-Item -LiteralPath $report).Length -gt 0
        if ($reportReady -and (Test-Path -LiteralPath $proof -PathType Leaf) -and (Get-Item -LiteralPath $proof).Length -gt 0) {
            try {
                $receipt = Get-Content -LiteralPath $proof -Raw -ErrorAction Stop | ConvertFrom-Json -ErrorAction Stop
                $reportHash = (Get-FileHash -LiteralPath $report -Algorithm SHA256).Hash.ToLowerInvariant()
                $identity = $receipt.identity
                $positive = @($receipt.positive)
                $negative = @($receipt.negative)
                $selectors = @($positive + $negative | ForEach-Object { $_.selector })
                $selectorPattern = '^(?:[A-Za-z_$][\w$]*\.)+[A-Za-z_$][\w$]*#[A-Za-z_$][\w$]*$'
                $checksReady = $positive.Count -gt 0 -and $negative.Count -gt 0 -and
                    @($positive | Where-Object { $_.verdict -cne 'PASS' -or $_.selector -cnotmatch $selectorPattern }).Count -eq 0 -and
                    @($negative | Where-Object { $_.verdict -cne 'PASS' -or $_.selector -cnotmatch $selectorPattern }).Count -eq 0 -and
                    @($selectors | Sort-Object -Unique).Count -eq $selectors.Count
                $identityReady = $identity.base -cmatch '^(?:[0-9a-f]{40}|[0-9a-f]{64})$' -and
                    $identity.commit -cmatch '^(?:[0-9a-f]{40}|[0-9a-f]{64})$' -and
                    $identity.diffSha256 -cmatch '^[0-9a-f]{64}$'
                $proofReady = $receipt.schemaVersion -eq 1 -and $receipt.kind -ceq 'ATF_TEST_CHANGE_PROOF' -and
                    $identity.id -ceq $id -and $identityReady -and $checksReady -and
                    $receipt.reportSha256 -cmatch '^[0-9a-f]{64}$' -and $receipt.reportSha256 -ceq $reportHash
            } catch {
                $proofReady = $false
            }
        }
        if ($exitCode -isnot [int] -or $exitCode -ne 0 -or
            !$proofReady -or !$reportReady) {
            $failures.Add($id)
        }
    }
    if ($failures.Count) {
        throw "TEST-CHANGE-BATCH-CONTROL: $($failures -join ', '); inspect test-change-<ID>.log"
    }
    & $CheckSnapshot
    if (($controls -join "`n") -cne (@(& $ReadPlan) -join "`n")) {
        throw 'TEST-CHANGE-BATCH-PLAN: diff controls changed during execution'
    }
    foreach ($id in $controls) {
        $output = Join-Path $pending $id
        [IO.File]::Move((Join-Path $output 'proof.pending.json'), (Join-Path $output 'proof.json'))
    }
    [IO.Directory]::Move($pending, $published)
    $published
}
