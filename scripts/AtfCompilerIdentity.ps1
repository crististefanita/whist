#requires -Version 7.0

function Get-AtfCompilerInputs([string]$Manifest) {
    $tool = Join-Path $PSScriptRoot '../atf-tests/tools/component-compiler-inputs.mjs'
    $json = & node $tool $Manifest
    if ($LASTEXITCODE -ne 0) { throw 'COMPONENT_COMPILER_INPUTS' }
    $json | ConvertFrom-Json
}

function Get-AtfCompilerToolIdentity([string]$Java, [string]$MavenCommand) {
    $files = [Collections.Generic.SortedSet[string]]::new([StringComparer]::Ordinal)
    $null = $files.Add([IO.Path]::GetFullPath($Java))
    $null = $files.Add([IO.Path]::GetFullPath($MavenCommand))
    $jdk = Split-Path -Parent (Split-Path -Parent $Java)
    foreach ($name in @('bin/javac.exe','release','lib/modules','bin/server/jvm.dll')) {
        $file = Join-Path $jdk $name
        if (!(Test-Path -LiteralPath $file -PathType Leaf)) { throw "COMPONENT_COMPILER_JDK: $name" }
        $null = $files.Add($file)
    }
    $mavenHome = Split-Path -Parent (Split-Path -Parent $MavenCommand)
    foreach ($name in @('boot','lib','conf')) {
        $folder = Join-Path $mavenHome $name
        if (Test-Path -LiteralPath $folder -PathType Container) {
            foreach ($file in Get-ChildItem -LiteralPath $folder -Recurse -File) { $null = $files.Add($file.FullName) }
        }
    }
    $records = [Collections.Generic.List[string]]::new()
    foreach ($homePath in @((Join-Path $env:USERPROFILE '.m2'),$env:MAVEN_USER_HOME,$env:MAVEN_CONFIG)) {
        if (!$homePath) { continue }
        foreach ($name in @('settings.xml','toolchains.xml')) {
            $file = Join-Path $homePath $name
            if (Test-Path -LiteralPath $file -PathType Leaf) { $null = $files.Add($file) }
            else { $records.Add($file + ':MISSING') }
        }
    }
    foreach ($file in $files) {
        if ((Get-Item -LiteralPath $file -Force).Attributes -band [IO.FileAttributes]::ReparsePoint) {
            throw 'COMPONENT_COMPILER_INDIRECT_TOOL'
        }
        $records.Add($file + ':' + (Get-FileHash -LiteralPath $file).Hash.ToLowerInvariant())
    }
    foreach ($name in @('MAVEN_OPTS','MAVEN_ARGS','MAVEN_CONFIG','MAVEN_USER_HOME','CLASSPATH','JAVACMD')) {
        $records.Add($name + ':' + [Environment]::GetEnvironmentVariable($name,'Process'))
    }
    # External argument files/settings selected through arbitrary overrides are not a qualified recipe.
    $eligible = !$env:MAVEN_ARGS -and !$env:JAVACMD -and $env:MAVEN_OPTS -notmatch '@|\b(?:user\.home|maven\.ext\.class\.path)\b'
    [pscustomobject]@{fingerprint=(Get-AtfInputFingerprint ($records -join [char]10));eligible=$eligible}
}

function Get-AtfComponentCompilerIdentity([string]$Manifest, [string]$Java, [string]$MavenCommand) {
    $inputs = Get-AtfCompilerInputs $Manifest
    $tools = Get-AtfCompilerToolIdentity $Java $MavenCommand
    [pscustomobject]@{identity=(Get-AtfInputFingerprint ($inputs.fingerprint + ':' + $tools.fingerprint))
        compilerFingerprint=$inputs.fingerprint;toolFingerprint=$tools.fingerprint;eligible=$tools.eligible}
}
