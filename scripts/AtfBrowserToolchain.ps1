#requires -Version 7.0

function Resolve-AtfBrowserTool([string]$Name) {
    $command = Get-Command $Name -CommandType Application -ErrorAction Stop | Select-Object -First 1
    if (!$command.Source -or !(Test-Path -LiteralPath $command.Source -PathType Leaf)) { throw "TOOL_CAPABILITY_MISSING: $Name" }
    [IO.Path]::GetFullPath($command.Source)
}

# Process ownership is a private port for deterministic component failures, not a launcher bypass.
function Complete-AtfBrowserToolProcess($Process, [int]$TimeoutSeconds, [string]$Tool) {
    $primary = $null; $result = $null; $started = $false; $finished = $false
    $cleanup = [Collections.Generic.List[Exception]]::new()
    try {
        $started = $Process.Start()
        if (!$started) { throw "TOOL_CAPABILITY_START: $Tool" }
        $stdout = $Process.StandardOutput.ReadToEndAsync()
        $stderr = $Process.StandardError.ReadToEndAsync()
        $finished = $Process.WaitForExit($TimeoutSeconds * 1000)
        if (!$finished) { throw [TimeoutException]::new("TOOL_CAPABILITY_TIMEOUT: $Tool after $($TimeoutSeconds)s") }
        if (![Threading.Tasks.Task]::WaitAll([Threading.Tasks.Task[]]@($stdout, $stderr), $TimeoutSeconds * 1000)) {
            throw [TimeoutException]::new("TOOL_CAPABILITY_OUTPUT_TIMEOUT: $Tool")
        }
        $result = [pscustomobject]@{ ExitCode=$Process.ExitCode; Output=($stdout.Result + "
" + $stderr.Result).Trim() }
    } catch { $primary = $_ }
    if ($started -and !$finished) {
        try {
            $Process.Kill($true)
            if (!$Process.WaitForExit(2000)) { throw "TOOL_CAPABILITY_TIMEOUT_RECOVERY: $Tool" }
        } catch { $cleanup.Add($_.Exception) }
    }
    try { $Process.Dispose() } catch { $cleanup.Add($_.Exception) }
    if ($primary -and !$cleanup.Count) { throw $primary }
    if ($primary) { $cleanup.Insert(0, $primary.Exception) }
    if ($cleanup.Count) { throw [AggregateException]::new('TOOL_CAPABILITY_PROCESS: original and cleanup causes retained', $cleanup.ToArray()) }
    $result
}

function Read-AtfBrowserToolVersion([string]$Executable, [int]$TimeoutSeconds, [string]$Tool) {
    $start = [Diagnostics.ProcessStartInfo]::new()
    $start.FileName = $Executable; $start.UseShellExecute = $false; $start.CreateNoWindow = $true
    $start.RedirectStandardOutput = $true; $start.RedirectStandardError = $true
    $start.ArgumentList.Add('--version')
    $process = [Diagnostics.Process]::new(); $process.StartInfo = $start
    Complete-AtfBrowserToolProcess $process $TimeoutSeconds $Tool
}

function Assert-AtfBrowserToolchain {
    param([switch]$RequireMaven, [ValidateRange(1,30)][int]$TimeoutSeconds = 10)
    $source = (Get-FileHash -LiteralPath (Join-Path $PSScriptRoot 'AtfBrowserToolchain.ps1') -Algorithm SHA256).Hash.ToLowerInvariant()
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
    if ($RequireMaven) { $capabilities += [pscustomobject]@{tool='mvn';kind='APPLICATION_PRESENT';executable=$paths['mvn']} }
    [pscustomobject]@{schemaVersion=1;kind='ATF_BROWSER_TOOLCHAIN';sourceSha256=$source;executables=$paths;capabilities=$capabilities}
}
