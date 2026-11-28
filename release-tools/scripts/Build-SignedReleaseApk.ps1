#requires -Version 7.0
param(
    [string]$ProjectRoot = '',
    [Parameter(Mandatory)][string]$SourceDirectory,
    [Parameter(Mandatory)][string]$OutputDirectory,
    [string]$Maven = 'mvn'
)

$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'Resolve-ReleaseToolsProject.ps1')
. (Join-Path $PSScriptRoot 'Invoke-ReleaseToolsJavaGate.ps1')
$root = Resolve-ReleaseToolsProject $ProjectRoot
$source = (Resolve-Path -LiteralPath $SourceDirectory).Path
$outputRoot = Assert-ReleaseToolsExternalPath $OutputDirectory @($root, $source)
if (-not (Test-Path -LiteralPath $outputRoot -PathType Container)) { throw 'APK_OUTPUT_DIRECTORY_MISSING' }
$buildFile = Join-Path $source 'whist-lab-app/app/build.gradle'
$wrapper = Join-Path $source 'whist-lab-app/gradlew.bat'
if (-not (Test-Path -LiteralPath $buildFile -PathType Leaf) -or -not (Test-Path -LiteralPath $wrapper -PathType Leaf)) {
    throw 'APK_PINNED_ANDROID_SOURCE_MISSING'
}
$version = [regex]::Match((Get-Content -LiteralPath $buildFile -Raw), 'versionName\s+"(?<version>0\.(?:0|[1-9]\d*)\.(?:0|[1-9]\d*))"').Groups['version'].Value
if (-not $version) { throw 'APK_VERSION_METADATA_INVALID' }
$identity = Join-Path $root 'release-tools/.atf-signing'
$preservedKey = Join-Path $identity 'whist-android-test.keystore'
if (-not (Test-Path -LiteralPath $preservedKey -PathType Leaf)) { throw 'SIGNING_KEY_CONTINUITY_MISSING' }
$identityParents = @((Join-Path $root 'release-tools'), $identity)
foreach ($identityPath in $identityParents) {
    $identityItem = Get-Item -LiteralPath $identityPath -Force
    if ($identityItem.Attributes.HasFlag([IO.FileAttributes]::ReparsePoint)) { throw 'SIGNING_KEY_LINK_REFUSED' }
}
$keyItem = Get-Item -LiteralPath $preservedKey -Force
if ($keyItem.Attributes.HasFlag([IO.FileAttributes]::ReparsePoint)) { throw 'SIGNING_KEY_LINK_REFUSED' }
if (-not (Test-Path -LiteralPath (Join-Path $identity 'whist-android-test.fingerprint') -PathType Leaf)) { throw 'SIGNING_FINGERPRINT_MISSING' }
$expectedCertificate = (Get-Content -LiteralPath (Join-Path $identity 'whist-android-test.fingerprint') -Raw).Trim().ToLowerInvariant()
if ($expectedCertificate -notmatch '^[0-9a-f]{64}$') { throw 'SIGNING_CERTIFICATE_FINGERPRINT_INVALID' }

$gradleArgs = @('--no-daemon', '--console=plain', '--offline',
    '--project-prop', "android.injected.signing.store.file=$preservedKey",
    '--project-prop', 'android.injected.signing.store.password=android',
    '--project-prop', 'android.injected.signing.key.alias=androiddebugkey',
    '--project-prop', 'android.injected.signing.key.password=android',
    ':app:assembleDebug')
Push-Location (Join-Path $source 'whist-lab-app')
try {
    & $wrapper @gradleArgs
    if ($LASTEXITCODE -ne 0) { throw "APK_BUILD_FAILED:$LASTEXITCODE" }
} finally { Pop-Location }

$apk = Join-Path $source 'whist-lab-app/app/build/outputs/apk/debug/app-debug.apk'
if (-not (Test-Path -LiteralPath $apk -PathType Leaf)) { throw 'APK_BUILD_OUTPUT_MISSING' }
$sdk = if ($env:ANDROID_SDK_ROOT) { $env:ANDROID_SDK_ROOT } else { $env:ANDROID_HOME }
$apksigner = (Get-Command apksigner -ErrorAction SilentlyContinue).Source
if (-not $apksigner -and $sdk) {
    $buildTools = Join-Path $sdk 'build-tools'
    if (Test-Path -LiteralPath $buildTools -PathType Container) {
        $apksigner = Get-ChildItem -LiteralPath $buildTools -Directory | Sort-Object Name -Descending |
            ForEach-Object { Join-Path $_.FullName 'apksigner.bat' } | Where-Object { Test-Path -LiteralPath $_ -PathType Leaf } | Select-Object -First 1
    }
}
if (-not $apksigner) { throw 'APKSIGNER_UNAVAILABLE' }
$identityOutput = & $apksigner verify --verbose --print-certs $apk 2>&1 | Out-String
if ($LASTEXITCODE -ne 0) { throw 'APK_SIGNATURE_VERIFICATION_FAILED' }
$actualCertificate = [regex]::Match($identityOutput, 'Signer #1 certificate SHA-256 digest:\s*(?<sha>[0-9a-fA-F]{64})').Groups['sha'].Value.ToLowerInvariant()
if ($actualCertificate -cne $expectedCertificate) { throw 'APK_CERTIFICATE_CONTINUITY_MISMATCH' }
Invoke-ReleaseToolsJavaGate $root @('verify-signing', $root, $actualCertificate) $Maven

$name = "WhistLab-$version.apk"
$destination = Join-Path $outputRoot $name
Copy-Item -LiteralPath $apk -Destination $destination
$apkHash = (Get-FileHash -LiteralPath $destination -Algorithm SHA256).Hash.ToLowerInvariant()
[IO.File]::WriteAllText($destination + '.sha256', "$apkHash  $name`n", [Text.UTF8Encoding]::new($false))
[PSCustomObject]@{status='PASS';version=$version;bytes=(Get-Item -LiteralPath $destination).Length;certificateContinuity='PASS';apk=$destination} |
    ConvertTo-Json -Compress
