#requires -Version 7.0
. (Join-Path $PSScriptRoot 'AtfStandaloneJava.ps1')

function Invoke-AtfTestChangeAdmission {
    param([string]$SourceRoot, [string]$BaseRef, [string]$TempRoot, [string]$JavaHome,
          [string]$MavenCache, [string]$ReceiptFile, [switch]$GenerateProofs, [string]$Control = '')
    $toolRoot = Split-Path -Parent $PSScriptRoot
    $classpath = Get-AtfJvmCompilerClasspath -ToolRoot $toolRoot -MavenCache $MavenCache -Yaml
    $arguments = @()
    if ($BaseRef) { $arguments += @('--base', $BaseRef) }
    if ($GenerateProofs) { $arguments += '--generate' }
    if ($Control) { $arguments += @('--control', $Control) }
    $result = 1
    $response = @(Invoke-AtfJvmCli -ToolRoot $toolRoot -ProjectRoot $SourceRoot -ScratchRoot $TempRoot `
        -JavaHome $JavaHome -ClassPath $classpath -MainClass 'atf.tests.testchange.TestChangeAdmissionCli' `
        -Arguments $arguments -Result ([ref]$result) `
        -AdditionalSourcePaths @((Join-Path $toolRoot 'atf-test-android/src/main/java')) 2>&1)
    $text = $response -join [Environment]::NewLine
    [IO.File]::WriteAllText($ReceiptFile, $text, [Text.UTF8Encoding]::new($false))
    if ($result -ne 0) { throw "TEST-CHANGE-ADMISSION: $text" }
    $receipt = $text | ConvertFrom-Json
    if ($receipt.schemaVersion -ne 1 -or $receipt.kind -cne 'ATF_TEST_CHANGE_ADMISSION' -or $receipt.status -cne 'PASS' -or
            $receipt.mode -cne $(if ($GenerateProofs) {'GENERATE'} else {'VERIFY'})) {
        throw 'TEST-CHANGE-ADMISSION: invalid canonical admission response'
    }
}
