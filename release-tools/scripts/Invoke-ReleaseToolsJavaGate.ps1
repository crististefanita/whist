#requires -Version 7.0

function Invoke-ReleaseToolsJavaGate([string]$ProjectRoot, [string[]]$Arguments, [string]$Maven = 'mvn') {
    if (-not $Arguments -or $Arguments.Count -eq 0) { throw 'RELEASE_TOOLS_GATE_ARGUMENTS_REQUIRED' }
    . (Join-Path $ProjectRoot 'scripts/AtfQualificationSnapshot.ps1')
    $tempRoot = Get-AtfQualificationTempRoot $ProjectRoot
    $buildDirectory = [IO.Path]::GetFullPath((Join-Path $tempRoot 'release-tools-maven-target'))
    $qualifiedRoot = [IO.Path]::GetFullPath($ProjectRoot).TrimEnd([IO.Path]::DirectorySeparatorChar) + [IO.Path]::DirectorySeparatorChar
    if ($buildDirectory.StartsWith($qualifiedRoot, [StringComparison]::OrdinalIgnoreCase)) {
        throw 'RELEASE_TOOLS_BUILD_DIRECTORY_IN_CHECKOUT'
    }
    New-Item -ItemType Directory -Path $buildDirectory -Force | Out-Null
    $command = Get-Command $Maven -ErrorAction Stop
    $execArgs = @($Arguments | ForEach-Object { '"' + $_.Replace('"', '\"') + '"' }) -join ' '
    & $command.Source -B -ntp -f (Join-Path $ProjectRoot 'release-tools/pom.xml') -o -q `
        "-Datf.releaseTools.buildDirectory=$buildDirectory" -DskipTests compile exec:java "-Dexec.args=$execArgs"
    if ($LASTEXITCODE -ne 0) { throw "RELEASE_TOOLS_JAVA_GATE_FAILED:$LASTEXITCODE" }
}
