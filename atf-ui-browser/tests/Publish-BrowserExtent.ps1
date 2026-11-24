#requires -Version 7.0
param(
    [Parameter(Mandatory)][string]$EvidenceRoot,
    [Parameter(Mandatory)][string]$MavenClasspathFile
)
$ErrorActionPreference = 'Stop'
$root = (Resolve-Path -LiteralPath $EvidenceRoot).ProviderPath
$owner = (Resolve-Path -LiteralPath (Join-Path $PSScriptRoot '../..')).ProviderPath
$jar = Join-Path $owner 'lib/atf-reporting-1.0.0-j24.jar'
$dependencies = (Get-Content -LiteralPath $MavenClasspathFile -Raw).Trim()
$classpath = @($jar,$dependencies) -join [IO.Path]::PathSeparator
$classes = Join-Path $root '_publisher-classes'
New-Item -ItemType Directory -Path $classes -ErrorAction Stop | Out-Null
& javac -proc:none -encoding UTF-8 -cp $classpath -d $classes (Join-Path $PSScriptRoot 'BrowserExtent.java') (Join-Path $owner 'atf-test-android/src/main/java/atf/android/evidence/flow/UiSanityPlan.java')
if ($LASTEXITCODE -ne 0) { throw 'Browser Extent publisher compilation failed.' }
& java -cp (@($classes,$classpath) -join [IO.Path]::PathSeparator) BrowserExtent $root (Join-Path $root 'extent-browser.html')
if ($LASTEXITCODE -ne 0) { throw 'Browser Extent publication failed.' }
