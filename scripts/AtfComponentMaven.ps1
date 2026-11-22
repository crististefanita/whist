#requires -Version 7.0

# Maven output admission; snapshot recovery and execution remain with their owners.
function Get-AtfComponentMavenFingerprint([string]$Work, [string]$Repository, [string[]]$Modules) {
    $roots = @($Repository) + @($Modules | ForEach-Object {
        Join-Path $Work "project/$_/target/classes"
        Join-Path $Work "project/$_/target/test-classes"
    })
    $records = [Collections.Generic.List[string]]::new()
    foreach ($root in $roots) {
        if (!(Test-Path -LiteralPath $root -PathType Container)) { continue }
        if ((Get-Item -LiteralPath $root).Attributes -band [IO.FileAttributes]::ReparsePoint) {
            throw 'COMPONENT_MAVEN_INDIRECT'
        }
        foreach ($file in Get-ChildItem -LiteralPath $root -Recurse -File -Force) {
            if ($file.Attributes -band [IO.FileAttributes]::ReparsePoint) { throw 'COMPONENT_MAVEN_INDIRECT' }
            if ($root -eq $Repository -and $file.Extension -cnotin @('.jar','.pom')) { continue }
            $relative = [IO.Path]::GetRelativePath($Work,$file.FullName).Replace('\','/')
            $records.Add($relative + ':' + (Get-FileHash -LiteralPath $file.FullName).Hash.ToLowerInvariant())
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
        $receipt = Get-Content -LiteralPath $file -Raw | ConvertFrom-Json
        if ($receipt.schemaVersion -ne 1 -or $receipt.identity -cne $Identity -or
            $receipt.repo -cnotmatch '^maven-repository(?:-[a-f0-9]{8})?$') { return }
        $repository = Join-Path $Work $receipt.repo
        if (!(Test-Path -LiteralPath $repository -PathType Container) -or
            (Get-AtfComponentMavenFingerprint $Work $repository $Modules) -cne $receipt.buildFingerprint) { return }
        $classpath = Join-Path $Work 'component-classpath.txt'
        $classpathReady = (Test-Path -LiteralPath $classpath -PathType Leaf) -and
            (Get-FileHash -LiteralPath $classpath).Hash -ceq $receipt.classpathSha256
        [pscustomobject]@{repository=$repository;classpathReady=$classpathReady;receipt=$receipt}
    } catch {
        Write-Verbose "Discarding Maven preparation: $($_.Exception.Message)"
    }
}
