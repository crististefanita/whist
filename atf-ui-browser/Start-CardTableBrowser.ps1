#requires -Version 7.0
param(
    [Parameter(Mandatory)][string]$WorkRoot,
    [Parameter(Mandatory)][string]$GameRoot,
    [Parameter(Mandatory)][string[]]$JavaInputs,
    [string[]]$Libraries = @(),
    [string[]]$MainArguments = @(),
    [Parameter(Mandatory)][string]$MainClass,
    [ValidateRange(0,65535)][int]$Port,
    [string]$ListenAddress = '127.0.0.1',
    [switch]$BuildOnly,
    [switch]$ListSources
)
$ErrorActionPreference = 'Stop'
$checkout = (Resolve-Path -LiteralPath (Join-Path $PSScriptRoot '..')).ProviderPath
$output = [IO.Path]::GetFullPath($WorkRoot)
foreach ($source in $checkout, (Resolve-Path -LiteralPath $GameRoot).ProviderPath) {
    $prefix = $source.TrimEnd([char[]]@('/', '\')) + [IO.Path]::DirectorySeparatorChar
    if ($output.Equals($source, [StringComparison]::OrdinalIgnoreCase) -or
            $output.StartsWith($prefix, [StringComparison]::OrdinalIgnoreCase)) {
        throw 'Use an external browser work directory.'
    }
}
$build = Join-Path $output ('browser-' + [guid]::NewGuid().ToString('N'))
$classes = Join-Path $build 'classes'
$coreRoot = Join-Path $checkout 'whist-lab-app/card-table-core/src/main/java'
$webRoots = @((Join-Path $PSScriptRoot 'src/main/java'), (Join-Path $checkout 'whist-lab-app/card-table-names/src/main/java'))
$inputs = @($coreRoot) + $webRoots + $JavaInputs
$sources = foreach ($inputPath in $inputs) {
    $item = Get-Item -LiteralPath $inputPath
    if ($item.PSIsContainer) {
        Get-ChildItem -LiteralPath $item.FullName -Recurse -Filter '*.java' -File | ForEach-Object FullName
    } else {
        $item.FullName
    }
}
$sources = @($sources | Sort-Object -Unique)
if ($ListSources) {
    $sources
    return
}
$null = New-Item -ItemType Directory -Path $classes -Force
function Format-JavacPath([string]$Path) {
    '"' + $Path.Replace('\', '/') + '"'
}
function Compile-Sources([string[]]$Sources, [string]$Destination, [string[]]$Classpath = @()) {
    $null = New-Item -ItemType Directory -Path $Destination -Force
    $argsFile = Join-Path (Split-Path $Destination) ((Split-Path $Destination -Leaf) + '.args')
    $options = @('--release', '17', '-encoding', 'UTF-8', '-d', (Format-JavacPath $Destination))
    if ($Classpath.Count) { $options += @('-cp', (Format-JavacPath ($Classpath -join [IO.Path]::PathSeparator))) }
    $options += @($Sources | ForEach-Object { Format-JavacPath $_ })
    [IO.File]::WriteAllLines($argsFile, [string[]]$options, [Text.UTF8Encoding]::new($false))
    & javac "@$argsFile"
    if ($LASTEXITCODE -ne 0) { throw 'Presentation module compilation failed.' }
}
function Module-Sources([string[]]$Roots) {
    @(foreach ($folder in $Roots) { Get-ChildItem -LiteralPath $folder -Recurse -Filter '*.java' -File | ForEach-Object FullName })
}
function Module-Jar([string]$Name, [string[]]$Sources, [string[]]$Classpath = @()) {
    $destination = Join-Path $build $Name
    Compile-Sources $Sources $destination $Classpath
    $artifact = Join-Path $build "$Name.jar"
    & jar --create --file $artifact '--date=2026-01-01T00:00:00Z' -C $destination .
    if ($LASTEXITCODE -ne 0) { throw 'Presentation module packaging failed.' }
    $artifact
}
$coreJar = Module-Jar 'card-table-core' (Module-Sources @($coreRoot))
$webJar = Module-Jar 'card-table-web' (Module-Sources $webRoots) @($coreJar)
$libraryPaths = @($coreJar, $webJar) + @(foreach ($library in $Libraries) { (Resolve-Path -LiteralPath $library).ProviderPath })
$sharedSources = @(Module-Sources (@($coreRoot) + $webRoots))
$gameSources = @($sources | Where-Object { $_ -notin $sharedSources })
if ($gameSources.Count) { Compile-Sources $gameSources $classes $libraryPaths }
$runtime = (@($classes) + $libraryPaths) -join [IO.Path]::PathSeparator
if ($BuildOnly) { $runtime; return }
& java '-cp' $runtime $MainClass (Join-Path $PSScriptRoot 'src/main/web') $ListenAddress $Port @MainArguments
if ($LASTEXITCODE -ne 0) { throw 'Browser server stopped with an error.' }
