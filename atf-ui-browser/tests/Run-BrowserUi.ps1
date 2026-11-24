#requires -Version 7.0
param(
    [Parameter(Mandatory)][string]$Site,
    [Parameter(Mandatory)][string]$HandFixture,
    [Parameter(Mandatory)][string]$EvidenceRoot,
    [string]$BrowserExecutable,
    [ValidateSet('all','playback','seats','card-face','header','dedication','viewport','trump','numbers','typography')]
    [string]$Check = 'all'
)
$ErrorActionPreference = 'Stop'
$root = (Resolve-Path -LiteralPath $EvidenceRoot).ProviderPath
$checkout = (Resolve-Path -LiteralPath (Join-Path $PSScriptRoot '../..')).ProviderPath
$prefix = $checkout.TrimEnd([char[]]@('/', '\')) + [IO.Path]::DirectorySeparatorChar
if ($root.Equals($checkout, [StringComparison]::OrdinalIgnoreCase) -or
        $root.StartsWith($prefix, [StringComparison]::OrdinalIgnoreCase)) {
    throw 'Use an external evidence directory for the Playwright run.'
}
$dependencies = Join-Path $root 'playwright-classpath.txt'
$log = Join-Path $root 'playwright-dependencies.log'
& mvn -B -ntp -f (Join-Path $PSScriptRoot 'playwright-dependencies.xml') `
    'org.apache.maven.plugins:maven-dependency-plugin:3.8.1:build-classpath' "-Dmdep.outputFile=$dependencies" *> $log
if ($LASTEXITCODE -ne 0) { throw "Could not resolve Playwright. See $log." }
$classpath = (Get-Content -Raw -LiteralPath $dependencies).Trim()
$classes = Join-Path $root 'playwright-classes'
$null = New-Item -ItemType Directory -Path $classes
$support = @(Get-ChildItem -LiteralPath (Join-Path $PSScriptRoot '../src/test/java/ro/cardtable/testing/browser') -Filter '*.java' -File | ForEach-Object FullName)
& javac -encoding UTF-8 -cp $classpath -d $classes (Join-Path $PSScriptRoot 'BrowserUi.java') @support
if ($LASTEXITCODE -ne 0) { throw 'Could not compile the browser runner and its shared static host.' }
$classpath = @($classes, $classpath) -join [IO.Path]::PathSeparator
$arguments = @((Resolve-Path -LiteralPath $Site).ProviderPath, (Resolve-Path -LiteralPath $HandFixture).ProviderPath, $root)
if ($BrowserExecutable) { $arguments += (Resolve-Path -LiteralPath $BrowserExecutable).ProviderPath }
& java "-Djava.io.tmpdir=$root" "-Dwhist.browserCheck=$Check" -cp $classpath BrowserUi @arguments
if ($LASTEXITCODE -ne 0) { throw 'Playwright UI verification failed. See browser-ui-checks.json.' }
