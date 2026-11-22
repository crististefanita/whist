#requires -Version 7.0

# Maven output admission; snapshot recovery and execution remain with their owners.
function Assert-AtfCompilerDirectPath([string]$Path) {
    $current = [IO.Path]::GetFullPath($Path)
    while ($current) {
        if ((Test-Path -LiteralPath $current) -and
                ((Get-Item -LiteralPath $current -Force).Attributes -band [IO.FileAttributes]::ReparsePoint)) {
            throw 'COMPONENT_MAVEN_INDIRECT'
        }
        $parent = Split-Path -Parent $current
        if ($parent -eq $current) { break }
        $current = $parent
    }
}

function Get-AtfComponentMavenFingerprint([string]$Work, [string]$Repository, [string[]]$Modules) {
    $roots = @($Repository) + @($Modules | ForEach-Object {
        Join-Path $Work "project/$_/target/classes"
        Join-Path $Work "project/$_/target/test-classes"
    })
    $records = [Collections.Generic.List[string]]::new()
    foreach ($root in $roots) {
        if (!(Test-Path -LiteralPath $root -PathType Container)) { continue }
        Assert-AtfCompilerDirectPath $root
        $pending = [Collections.Generic.Stack[string]]::new()
        $pending.Push($root)
        while ($pending.Count) {
          foreach ($file in Get-ChildItem -LiteralPath $pending.Pop() -Force) {
            if ($file.Attributes -band [IO.FileAttributes]::ReparsePoint) { throw 'COMPONENT_MAVEN_INDIRECT' }
            if ($file.PSIsContainer) { $pending.Push($file.FullName); continue }
            if ($root -eq $Repository -and $file.Extension -cnotin @('.jar','.pom')) { continue }
            $relative = [IO.Path]::GetRelativePath($Work,$file.FullName).Replace('\','/')
            $records.Add($relative + ':' + (Get-FileHash -LiteralPath $file.FullName).Hash.ToLowerInvariant())
          }
        }
    }
    $records.Sort([StringComparer]::Ordinal)
    if (!$records.Count) { throw 'COMPONENT_MAVEN_OUTPUTS_MISSING' }
    Get-AtfInputFingerprint ($records -join [char]10)
}

function Read-AtfPreparedComponentMaven([string]$Work, [string]$Identity, [string[]]$Modules) {
    $file = Join-Path $Work 'component-maven.json'
    if (!(Test-Path -LiteralPath $file -PathType Leaf)) { return }
    try {
        Assert-AtfCompilerDirectPath $file
        $receipt = Get-Content -LiteralPath $file -Raw | ConvertFrom-Json
        if ($receipt.schemaVersion -notin @(1,2) -or $receipt.identity -cne $Identity -or
            $receipt.repo -cnotmatch '^maven-repository(?:-[a-f0-9]{8})?$') { return }
        if ($receipt.schemaVersion -eq 2) {
            $manifest = Join-Path $Work 'compiler-source-inputs.json'
            Assert-AtfCompilerDirectPath $manifest
            if ($receipt.identity -cne (Get-AtfInputFingerprint ($receipt.compilerFingerprint + ':' + $receipt.toolFingerprint)) -or
                    (Get-FileHash -LiteralPath $manifest).Hash.ToLowerInvariant() -cne $receipt.inputManifestSha256 -or
                    (Get-AtfCompilerInputs $manifest).fingerprint -cne $receipt.compilerFingerprint) { return }
        }
        $repository = Join-Path $Work $receipt.repo
        if (!(Test-Path -LiteralPath $repository -PathType Container) -or
            (Get-AtfComponentMavenFingerprint $Work $repository $Modules) -cne $receipt.buildFingerprint) { return }
        $classpath = Join-Path $Work 'component-classpath.txt'
        Assert-AtfCompilerDirectPath $classpath
        $classpathReady = (Test-Path -LiteralPath $classpath -PathType Leaf) -and
            (Get-FileHash -LiteralPath $classpath).Hash -ieq $receipt.classpathSha256
        if ($classpathReady -and $receipt.schemaVersion -eq 2) {
            foreach ($entry in (Get-Content -LiteralPath $classpath -Raw).Trim().Split([IO.Path]::PathSeparator)) {
                $entry = [IO.Path]::GetFullPath($entry)
                Assert-AtfCompilerDirectPath $entry
                if (!$entry.StartsWith($repository + [IO.Path]::DirectorySeparatorChar,[StringComparison]::OrdinalIgnoreCase) -or
                        !(Test-Path -LiteralPath $entry -PathType Leaf)) { $classpathReady = $false; break }
            }
        }
        [pscustomobject]@{repository=$repository;classpathReady=$classpathReady;receipt=$receipt}
    } catch {
        Write-Verbose "Discarding Maven preparation: $($_.Exception.Message)"
    }
}
