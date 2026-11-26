#requires -Version 7.0
. (Join-Path $PSScriptRoot 'AtfLauncherCapabilities.ps1')

function Resolve-AtfBrowserTool([string]$Name) {
    Resolve-AtfLauncherApplication $Name
}

# Process ownership is a private port for deterministic component failures, not a launcher bypass.
function Complete-AtfBrowserToolProcess($Process, [int]$TimeoutSeconds, [string]$Tool) {
    Complete-AtfLauncherProcess $Process $TimeoutSeconds $Tool
}

function Read-AtfBrowserToolVersion([string]$Executable, [int]$TimeoutSeconds, [string]$Tool) {
    Invoke-AtfLauncherVersion $Executable @('--version') $TimeoutSeconds $Tool
}

function Assert-AtfBrowserToolchain {
    param([switch]$RequireMaven, [ValidateRange(1,30)][int]$TimeoutSeconds = 10)
    $sourceParts = foreach ($name in @('AtfBrowserToolchain.ps1','AtfLauncherCapabilities.ps1')) {
        $hash = (Get-FileHash -LiteralPath (Join-Path $PSScriptRoot $name) -Algorithm SHA256).Hash.ToLowerInvariant()
        "$name=$hash"
    }
    $source = [Convert]::ToHexString([Security.Cryptography.SHA256]::HashData(
        [Text.Encoding]::UTF8.GetBytes(($sourceParts -join "`n")))).ToLowerInvariant()
    $names = @('java','javac','jar'); if ($RequireMaven) { $names += 'mvn' }
    $paths = [ordered]@{}; $capabilities = @()
    foreach ($name in $names) {
        try { $paths[$name] = Resolve-AtfBrowserTool $name }
        catch { throw [InvalidOperationException]::new("TOOL_CAPABILITY_MISSING: $name; source=$source", $_.Exception) }
    }
    foreach ($name in @('java','javac','jar')) {
        try { $observed = Read-AtfBrowserToolVersion $paths[$name] $TimeoutSeconds $name }
        catch { throw [InvalidOperationException]::new("TOOL_CAPABILITY_PROBE: $name; source=$source", $_.Exception) }
        $version = [regex]::Match($observed.Output, '(?m)^(?:openjdk|java|javac|jar)(?: version)?[ \t]+"?(?<major>[0-9]+)(?:[.\s"-]|$)')
        if ($observed.ExitCode -ne 0 -or !$version.Success -or [int]$version.Groups['major'].Value -lt 17) {
            throw "TOOL_CAPABILITY_VERSION: $name requires Java 17 or newer; exit=$($observed.ExitCode); source=$source"
        }
        $capabilities += [pscustomobject]@{tool=$name;kind='JAVA_RELEASE_SUPPORT';major=[int]$version.Groups['major'].Value;executable=$paths[$name]}
    }
    if ($RequireMaven) {
        try { $observed = Read-AtfBrowserToolVersion $paths['mvn'] $TimeoutSeconds 'mvn' }
        catch { throw [InvalidOperationException]::new("TOOL_CAPABILITY_PROBE: mvn; source=$source", $_.Exception) }
        $version = [regex]::Match($observed.Output, '(?im)^Apache Maven[ \t]+(?<version>[0-9]+\.[0-9]+(?:\.[0-9]+)?)(?:[ \t]|$)')
        if ($observed.ExitCode -ne 0 -or !$version.Success) {
            throw "TOOL_CAPABILITY_VERSION: mvn did not report an Apache Maven version; exit=$($observed.ExitCode); source=$source"
        }
        $capabilities += [pscustomobject]@{tool='mvn';kind='RUNTIME_VERSION';version=$version.Groups['version'].Value;executable=$paths['mvn']}
    }
    [pscustomobject]@{schemaVersion=1;kind='ATF_BROWSER_TOOLCHAIN';sourceSha256=$source;executables=$paths;capabilities=$capabilities}
}
