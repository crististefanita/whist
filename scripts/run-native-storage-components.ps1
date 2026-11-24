#requires -Version 7.0
param(
    [Parameter(Mandatory)][string]$RunRoot,
    [Parameter(Mandatory)][string]$ApkPath,
    [Parameter(Mandatory)][string]$TestApkPath,
    [Parameter(Mandatory)][string]$OriginalApkPath,
    [string]$Serial='127.0.0.1:5555',
    [string]$ServerSocket='tcp:192.168.56.1:5038',
    [string]$AdbExecutable='C:/tools/whist-android-sdk/platform-tools/adb.exe',
    [string]$LeasePath='C:/work/whist_back/android-ui-lease.json'
)
$ErrorActionPreference='Stop'
$source=(Resolve-Path (Join-Path $PSScriptRoot '..')).ProviderPath
. (Join-Path $PSScriptRoot 'AtfDeviceLease.ps1')
Import-Module (Join-Path $source 'atf-test-android/tools/AndroidDeviceSetup.psm1') -Force
$run=[IO.Path]::GetFullPath($RunRoot)
if ($run -ieq $source -or $run.StartsWith($source+[IO.Path]::DirectorySeparatorChar,[StringComparison]::OrdinalIgnoreCase) -or (Test-Path -LiteralPath $run)) {
    throw 'NATIVE_STORAGE_NEW_EXTERNAL_RUN_REQUIRED'
}
$planFile=Join-Path $source 'config/native-storage-components.json'
$contract=Join-Path $source 'atf-test-android/tools/native-storage-contract.mjs'
$planText=& node $contract plan $planFile
if($LASTEXITCODE -ne 0){throw 'NATIVE_STORAGE_PLAN_REFUSED'}
$plan=$planText|ConvertFrom-Json
foreach($file in @($ApkPath,$TestApkPath,$OriginalApkPath)) {
    if(!(Test-Path -LiteralPath $file -PathType Leaf)){throw 'NATIVE_STORAGE_APK_REQUIRED'}
}
$apkHash=(Get-FileHash -LiteralPath $ApkPath).Hash.ToLowerInvariant()
$originalHash=(Get-FileHash -LiteralPath $OriginalApkPath).Hash.ToLowerInvariant()
$null=New-Item -ItemType Directory -Path $run
function Invoke-NativeAdb([string[]]$Arguments,[int]$Timeout=90) {
    Get-AtfActiveDeviceLease $LeasePath $lease $Serial|Out-Null
    $result=Invoke-AtfAdbCommand -AdbExecutable $AdbExecutable -Arguments (@('-s',$Serial)+$Arguments) -ServerSocket $ServerSocket -TimeoutSeconds $Timeout
    if($result.ExitCode -ne 0){throw "NATIVE_STORAGE_ADB: $($Arguments -join ' '): $($result.Output)"}
    $result.Output.Trim()
}
function Installed-Hash([string]$User) {
    $location=Invoke-NativeAdb @('shell','cmd','package','path','--user',$User,$plan.applicationId)
    if($location -notmatch '(?m)^package:(/\S+\.apk)\s*$'){throw 'NATIVE_STORAGE_INSTALLED_PATH'}
    $path=$Matches[1]
    $digest=Invoke-NativeAdb @('shell','sha256sum',$path)
    ($digest -split '\s+')[0]
}
$lease=New-AtfDeviceLease $LeasePath $Serial 'Whist native storage components' $run
$device=@{RunRoot=$run;Serial=$Serial;AdbExecutable=$AdbExecutable;ServerSocket=$ServerSocket;LeasePath=$LeasePath;LeaseId=$lease;ExpectedApi=28;ExpectedDisplayProfile='1080x1920@440'}
$failure=$null;$changedPackage=$false
try {
    $originalUser=Invoke-NativeAdb @('shell','am','get-current-user')
    if((Installed-Hash $originalUser) -cne $originalHash){throw 'NATIVE_STORAGE_ORIGINAL_APK_NOT_RECONSTRUCTIBLE'}
    # Android shares package code across users; preserve the original binary before installing a source build.
    $restore=[ordered]@{originalUserId=[int]$originalUser;originalApkSha256=$originalHash;originalApkPath=[IO.Path]::GetFullPath($OriginalApkPath);testApkSha256=$apkHash;leaseId=$lease}
    [IO.File]::WriteAllText((Join-Path $run 'package-restore-intent.json'),(($restore|ConvertTo-Json)+"`n"))
    $changedPackage=$true
    & (Join-Path $PSScriptRoot 'Invoke-AtfAndroidDisposableUser.ps1') @device -Phase Prepare -ApkPath $ApkPath -ApkSha256 $apkHash | Out-Host
    $ready=Get-Content (Join-Path $run 'data-isolation-ready.json') -Raw|ConvertFrom-Json
    $user=[string]$ready.testUserId
    Invoke-NativeAdb @('install','-r','-t','--user',$user,$TestApkPath) 120|Out-Host
    $all=[Collections.Generic.List[object]]::new()
    foreach($phase in $plan.phases) {
        Invoke-NativeAdb @('shell','am','force-stop','--user',$user,$plan.applicationId)|Out-Null
        $text=Invoke-NativeAdb @('shell','am','instrument','--user',$user,'-w','-e','phase',$phase.id,$plan.instrumentation) 120
        [IO.File]::WriteAllText((Join-Path $run ($phase.id+'.log')),$text)
        $match=[regex]::Match($text,'(?m)^ATF_NATIVE_STORAGE=(\{[^\r\n]+\})\s*$')
        if(!$match.Success){throw "NATIVE_STORAGE_RECEIPT_MISSING: $($phase.id); $text"}
        $receipt=Join-Path $run ($phase.id+'.json')
        [IO.File]::WriteAllText($receipt,$match.Groups[1].Value)
        $checks=Join-Path $run ($phase.id+'-checks.json')
        & node $contract verify $planFile $receipt $phase.id $checks
        if($LASTEXITCODE -ne 0){throw "NATIVE_STORAGE_PHASE_NOT_QUALIFIED: $($phase.id)"}
        foreach($row in @(Get-Content $checks -Raw|ConvertFrom-Json)){$all.Add($row)}
        Write-Host "PASS native storage phase: $($phase.id)"
    }
    [IO.File]::WriteAllText((Join-Path $run 'browser-checks.json'),(($all.ToArray()|ConvertTo-Json -Depth 12)+"`n"))
} catch { $failure=$_ }
finally {
    try {
        if(Test-Path -LiteralPath (Join-Path $run 'setup-release.json')) {
            & (Join-Path $PSScriptRoot 'Invoke-AtfAndroidDisposableUser.ps1') @device -Phase Teardown | Out-Host
        }
    } catch {if($failure){$failure.Exception.Data['profileCleanup']=$_.Exception.Message}else{$failure=$_}}
    try {
        if($changedPackage) {
            Invoke-NativeAdb @('install','-r','--user',$originalUser,$OriginalApkPath) 120|Out-Host
            if((Installed-Hash $originalUser) -cne $originalHash){throw 'NATIVE_STORAGE_ORIGINAL_APK_NOT_RESTORED'}
            [IO.File]::WriteAllText((Join-Path $run 'package-restored.json'),((@{status='PASS';originalApkSha256=$originalHash;user=[int]$originalUser;leaseId=$lease}|ConvertTo-Json)+"`n"))
        }
    } catch {if($failure){$failure.Exception.Data['packageRestore']=$_.Exception.Message}else{$failure=$_}}
    finally {Remove-AtfDeviceLease $LeasePath $lease}
}
if($failure){throw $failure}
$run
