#requires -Version 7.0

function Get-AtfMavenCompilerScope([string]$Manifest, [string]$SourceRoot) {
    if (!$SourceRoot) { return }
    $root = [IO.Path]::GetFullPath($SourceRoot)
    $inputs = @{}
    foreach ($row in (Get-Content -LiteralPath $Manifest -Raw | ConvertFrom-Json)) { $inputs[$row.path] = $row.sha256 }
    function Read-Recipe([string]$Relative) {
        if (!$inputs.ContainsKey($Relative)) { throw 'COMPILER_SCOPE_UNKNOWN' }
        $file = [IO.Path]::GetFullPath((Join-Path $root $Relative))
        if (!$file.StartsWith($root + [IO.Path]::DirectorySeparatorChar,[StringComparison]::OrdinalIgnoreCase)) { throw 'COMPILER_SCOPE_UNKNOWN' }
        for ($entry = $file; $entry; $entry = Split-Path -Parent $entry) {
            if ((Test-Path -LiteralPath $entry) -and ((Get-Item -LiteralPath $entry -Force).Attributes -band [IO.FileAttributes]::ReparsePoint)) {
                throw 'COMPONENT_COMPILER_RECIPE_INDIRECT'
            }
        }
        if (!(Test-Path -LiteralPath $file -PathType Leaf) -or (Get-FileHash -LiteralPath $file).Hash -ine $inputs[$Relative]) {
            throw "COMPONENT_COMPILER_RECIPE_HASH: $Relative"
        }
        Get-Content -LiteralPath $file -Raw
    }
    function Read-Pom([string]$Relative) {
        $text = Read-Recipe $Relative
        $settings = [Xml.XmlReaderSettings]::new(); $settings.DtdProcessing = [Xml.DtdProcessing]::Prohibit
        $reader = [Xml.XmlReader]::Create([IO.StringReader]::new($text), $settings)
        try { $xml = [Xml.XmlDocument]::new(); $xml.XmlResolver = $null; $xml.Load($reader); return ,$xml }
        finally { $reader.Dispose() }
    }
    function Resolve-RecipeRoot([string]$Module, [string]$Value) {
        $value = $Value.Trim() -replace '^\$\{(?:project\.)?basedir\}/',''
        if (!$value -or $value.Contains('${') -or [IO.Path]::IsPathRooted($value) -or $value.Contains('\')) { throw 'COMPILER_SCOPE_UNKNOWN' }
        $base = if ($Module) { Join-Path $root $Module } else { $root }
        $directory = [IO.Path]::GetFullPath((Join-Path $base $value))
        if (!$directory.StartsWith($root + [IO.Path]::DirectorySeparatorChar,[StringComparison]::OrdinalIgnoreCase)) { throw 'COMPILER_SCOPE_UNKNOWN' }
        [IO.Path]::GetRelativePath($root,$directory).Replace('\','/')
    }
    try {
        $policy = Read-Recipe 'config/atf-governance-policy.json' | ConvertFrom-Json
        $parent = Read-Pom 'pom.xml'
        if ($parent.DocumentElement.LocalName -cne 'project' -or $parent.SelectNodes('/*[local-name()="project"]/*[local-name()="parent"]').Count -or
                $parent.SelectNodes('//*[local-name()="modules"]').Count -ne
                $parent.SelectNodes('/*[local-name()="project"]/*[local-name()="modules"]').Count) { throw 'COMPILER_SCOPE_UNKNOWN' }
        $modules = @($parent.SelectNodes('/*[local-name()="project"]/*[local-name()="modules"]/*[local-name()="module"]') | ForEach-Object { $_.InnerText.Trim() })
        if ($policy.mavenModules -isnot [array] -or $modules.Count -ne $policy.mavenModules.Count -or
                (($modules | Sort-Object) -join "`n") -cne (($policy.mavenModules | Sort-Object) -join "`n")) { throw 'COMPILER_SCOPE_UNKNOWN' }
        $compiled = [Collections.Generic.SortedSet[string]]::new([StringComparer]::Ordinal)
        foreach ($module in @('') + $modules) {
            if ($module -and $module -cnotmatch '^[A-Za-z0-9_-]+(?:/[A-Za-z0-9_-]+)*$') { throw 'COMPILER_SCOPE_UNKNOWN' }
            $pom = if ($module) { Read-Pom "$module/pom.xml" } else { $parent }
            if ($pom.DocumentElement.LocalName -cne 'project' -or ($module -and $pom.SelectNodes('//*[local-name()="modules"]').Count)) { throw 'COMPILER_SCOPE_UNKNOWN' }
            $inheritance = $pom.SelectSingleNode('/*[local-name()="project"]/*[local-name()="parent"]')
            if ($inheritance) {
                $relative = $inheritance.SelectSingleNode('*[local-name()="relativePath"]')
                $parentFile = if ($relative) { $relative.InnerText.Trim() } else { '../pom.xml' }
                if (!$parentFile -or [IO.Path]::GetFullPath((Join-Path (Join-Path $root $module) $parentFile)) -ine (Join-Path $root 'pom.xml')) { throw 'COMPILER_SCOPE_UNKNOWN' }
                foreach ($field in @('groupId','artifactId','version')) {
                    $expected = $parent.SelectSingleNode("/*[local-name()='project']/*[local-name()='$field']")
                    $actual = $inheritance.SelectSingleNode("*[local-name()='$field']")
                    if (!$expected -or !$actual -or $expected.InnerText.Contains('${') -or $actual.InnerText -cne $expected.InnerText) { throw 'COMPILER_SCOPE_UNKNOWN' }
                }
            }
            if ($pom.SelectNodes('//*[local-name()="extensions"]').Count) { throw 'COMPILER_SCOPE_UNKNOWN' }
            foreach ($plugin in $pom.SelectNodes('//*[local-name()="plugin"]/*[local-name()="artifactId"]')) {
                if ($plugin.InnerText -cnotin @('maven-install-plugin','maven-compiler-plugin','maven-surefire-plugin',
                        'maven-enforcer-plugin','maven-resources-plugin','maven-jar-plugin','build-helper-maven-plugin')) { throw 'COMPILER_SCOPE_UNKNOWN' }
                $group = $plugin.ParentNode.SelectSingleNode('*[local-name()="groupId"]')
                $expected = if ($plugin.InnerText -ceq 'build-helper-maven-plugin') { 'org.codehaus.mojo' } else { 'org.apache.maven.plugins' }
                if ($group -and $group.InnerText -cne $expected) { throw 'COMPILER_SCOPE_UNKNOWN' }
            }
            foreach ($goal in $pom.SelectNodes('//*[local-name()="plugin"][*[local-name()="artifactId"]="build-helper-maven-plugin"]//*[local-name()="goal"]')) {
                if ($goal.InnerText -cnotin @('add-source','add-test-source','add-resource','add-test-resource')) { throw 'COMPILER_SCOPE_UNKNOWN' }
            }
            foreach ($value in @('src/main/java','src/test/java','src/main/resources','src/test/resources')) {
                $null = $compiled.Add((Resolve-RecipeRoot $module $value))
            }
            $xpath = '//*[local-name()="build"]/*[local-name()="sourceDirectory" or local-name()="testSourceDirectory"] | ' +
                '//*[local-name()="resource" or local-name()="testResource"]/*[local-name()="directory"] | ' +
                '//*[local-name()="plugin"][*[local-name()="artifactId"]="build-helper-maven-plugin"]//*[local-name()="sources"]/*[local-name()="source"]'
            foreach ($node in $pom.SelectNodes($xpath)) { $null = $compiled.Add((Resolve-RecipeRoot $module $node.InnerText)) }
        }
        $tests = @($policy.additionalSourceRoots | Where-Object { $_.test -eq $true } | ForEach-Object {
            if ($_.path -cnotmatch '^[A-Za-z0-9_.-]+(?:/[A-Za-z0-9_.-]+)*$') { throw 'COMPILER_SCOPE_UNKNOWN' }
            [string]$_.path
        })
        [ordered]@{schemaVersion=1;compiledRoots=@($compiled);testRoots=$tests}
    } catch {
        if ($_.Exception.Message -like 'COMPONENT_COMPILER_RECIPE_*') { throw }
        # Unsupported effective-model features retain the complete compiler inventory.
        Write-Verbose "Conservative compiler inventory: $($_.Exception.Message)"
        return
    }
}

function Get-AtfCompilerInputs([string]$Manifest, [string]$SourceRoot = '') {
    $tool = Join-Path $PSScriptRoot '../atf-tests/tools/component-compiler-inputs.mjs'
    $scope = Get-AtfMavenCompilerScope $Manifest $SourceRoot
    $arguments = @($Manifest)
    if ($scope) { $arguments += @('--scope-json',($scope | ConvertTo-Json -Depth 6 -Compress)) }
    $json = & node $tool @arguments
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

function Get-AtfComponentCompilerIdentity([string]$Manifest, [string]$Java, [string]$MavenCommand, [string]$SourceRoot = '') {
    $inputs = Get-AtfCompilerInputs $Manifest $SourceRoot
    $tools = Get-AtfCompilerToolIdentity $Java $MavenCommand
    [pscustomobject]@{identity=(Get-AtfInputFingerprint ($inputs.fingerprint + ':' + $tools.fingerprint))
        compilerFingerprint=$inputs.fingerprint;toolFingerprint=$tools.fingerprint;eligible=$tools.eligible}
}
