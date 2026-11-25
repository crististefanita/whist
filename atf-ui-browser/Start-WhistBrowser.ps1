#requires -Version 7.0
param(
    [Parameter(Mandatory)][string]$WorkRoot,
    [ValidateRange(0,65535)][int]$Port = 8714,
    [string]$ListenAddress = '127.0.0.1',
    [string]$CatalogSource,
    [string]$JsonLibrary,
    [string]$GradleExecutable,
    [switch]$BuildOnly
)
$checkout = Join-Path $PSScriptRoot '..'
$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot '../scripts/AtfBrowserToolchain.ps1')
$null = Assert-AtfBrowserToolchain
$work = [IO.Path]::GetFullPath($WorkRoot)
$source = (Resolve-Path -LiteralPath $checkout).ProviderPath
if ($work -ieq $source -or $work.StartsWith($source + [IO.Path]::DirectorySeparatorChar,[StringComparison]::OrdinalIgnoreCase)) {
    throw 'Use an external browser work directory.'
}
if (!$CatalogSource) {
    $CatalogSource = & (Join-Path $PSScriptRoot 'Prepare-WhistCatalog.ps1') -WorkRoot $work -GradleExecutable $GradleExecutable
}
if (!$JsonLibrary) {
    $gradleHome = if ($env:GRADLE_USER_HOME) { $env:GRADLE_USER_HOME } else { Join-Path $env:USERPROFILE '.gradle' }
    $cache = Join-Path $gradleHome 'caches/modules-2/files-2.1/org.json/json/20240205'
    if (Test-Path -LiteralPath $cache) {
        $JsonLibrary = Get-ChildItem -LiteralPath $cache -Filter 'json-20240205.jar' -Recurse -File | Select-Object -First 1 -ExpandProperty FullName
    }
    if (!$JsonLibrary) { throw 'Supply -JsonLibrary with org.json:json:20240205, the Android JVM test dependency.' }
}
$sources = @(
    Join-Path $PSScriptRoot 'whist/src/main/java'
    Join-Path $checkout 'whist-lab-app/game-core/src/main/java'
    Join-Path $checkout 'whist-lab-app/whist-presentation/src/main/java'
    $CatalogSource
)
$footerImage = Join-Path $checkout 'whist-lab-app/app/src/main/res/drawable-nodpi/teo_dedication.png'
& (Join-Path $PSScriptRoot 'Start-CardTableBrowser.ps1') -WorkRoot $WorkRoot -GameRoot $checkout `
    -JavaInputs $sources -MainClass 'ro.whistlab.browser.WhistBrowser' -Port $Port `
    -ListenAddress $ListenAddress -BuildOnly:$BuildOnly -Libraries @($JsonLibrary) `
    -MainArguments @((Join-Path $work 'whist-profile-data'), $footerImage)
