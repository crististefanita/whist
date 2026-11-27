#requires -Version 7.0

function Resolve-AtfLauncherApplication([string]$Name, [string]$Override = '') {
    $candidate = if ($Override -and [IO.Path]::IsPathRooted($Override)) { $Override } else {
        $commandName = if ($Override) { $Override } else { $Name }
        $command = Get-Command $commandName -CommandType Application -ErrorAction Stop | Select-Object -First 1
        $command.Source
    }
    if (!$candidate -or !(Test-Path -LiteralPath $candidate -PathType Leaf)) {
        throw "LAUNCHER_CAPABILITY_MISSING: $Name"
    }
    [IO.Path]::GetFullPath($candidate)
}

function Invoke-AtfLauncherVersion([string]$Executable, [string[]]$Arguments, [int]$TimeoutSeconds, [string]$Tool) {
    $start = [Diagnostics.ProcessStartInfo]::new()
    $start.UseShellExecute = $false
    $start.CreateNoWindow = $true
    $start.RedirectStandardOutput = $true
    $start.RedirectStandardError = $true
    if ([IO.Path]::GetExtension($Executable) -in @('.cmd','.bat')) {
        $hostExecutable = Join-Path $PSHOME $(if ($IsWindows) { 'pwsh.exe' } else { 'pwsh' })
        $start.FileName = Resolve-AtfLauncherApplication 'pwsh' $hostExecutable
        $start.ArgumentList.Add('-NoProfile')
        $start.ArgumentList.Add('-NonInteractive')
        $start.ArgumentList.Add('-Command')
        $start.ArgumentList.Add('$arguments=[Text.Encoding]::UTF8.GetString([Convert]::FromBase64String($env:ATF_LAUNCHER_ARGUMENTS)) | ConvertFrom-Json; if ($arguments -isnot [array]) { $arguments=@($arguments) }; & $env:ATF_LAUNCHER_EXECUTABLE @arguments; exit $LASTEXITCODE')
        $start.EnvironmentVariables['ATF_LAUNCHER_EXECUTABLE'] = $Executable
        $start.EnvironmentVariables['ATF_LAUNCHER_ARGUMENTS'] = [Convert]::ToBase64String(
            [Text.Encoding]::UTF8.GetBytes((ConvertTo-Json -InputObject ([string[]]$Arguments) -Compress)))
    } else {
        $start.FileName = $Executable
        foreach ($argument in $Arguments) { $start.ArgumentList.Add($argument) }
    }
    $process = [Diagnostics.Process]::new()
    $process.StartInfo = $start
    Complete-AtfLauncherProcess $process $TimeoutSeconds $Tool
}

function Complete-AtfLauncherProcess($Process, [int]$TimeoutSeconds, [string]$Tool) {
    $started = $false
    $finished = $false
    $result = $null
    $cause = $null
    $cleanup = [Collections.Generic.List[Exception]]::new()
    try {
        $started = $Process.Start()
        if (!$started) { throw "LAUNCHER_CAPABILITY_START: $Tool" }
        $stdout = $Process.StandardOutput.ReadToEndAsync()
        $stderr = $Process.StandardError.ReadToEndAsync()
        $finished = $Process.WaitForExit($TimeoutSeconds * 1000)
        if (!$finished) { throw [TimeoutException]::new("LAUNCHER_CAPABILITY_TIMEOUT: $Tool after $($TimeoutSeconds)s") }
        if (![Threading.Tasks.Task]::WaitAll([Threading.Tasks.Task[]]@($stdout, $stderr), $TimeoutSeconds * 1000)) {
            throw [TimeoutException]::new("LAUNCHER_CAPABILITY_OUTPUT_TIMEOUT: $Tool")
        }
        $result = [pscustomobject]@{ExitCode=$Process.ExitCode;Output=($stdout.Result + [Environment]::NewLine + $stderr.Result).Trim()}
    } catch { $cause = $_ }
    if ($started -and !$finished) {
        try {
            $Process.Kill($true)
            if (!$Process.WaitForExit(2000)) { throw "LAUNCHER_CAPABILITY_TIMEOUT_RECOVERY: $Tool" }
        } catch { $cleanup.Add($_.Exception) }
    }
    try { $Process.Dispose() } catch { $cleanup.Add($_.Exception) }
    if ($cause -and !$cleanup.Count) { throw $cause }
    if ($cause) { $cleanup.Insert(0, $cause.Exception) }
    if ($cleanup.Count) { throw [AggregateException]::new('LAUNCHER_CAPABILITY_PROCESS: original and cleanup causes retained', $cleanup.ToArray()) }
    $result
}

function Assert-AtfLauncherCapabilities {
    param(
        [string]$JavaHome = $env:JAVA_HOME,
        [string]$Maven = '',
        [switch]$RequireMaven,
        [string]$Node = '',
        [switch]$RequireNode,
        [string]$GradleRoot = '',
        [ValidateRange(1,30)][int]$TimeoutSeconds = 30
    )
    $scriptPath = Join-Path $PSScriptRoot 'AtfLauncherCapabilities.ps1'
    $sourceHash = (Get-FileHash -LiteralPath $scriptPath -Algorithm SHA256).Hash.ToLowerInvariant()
    $paths = [ordered]@{}
    try {
        if ($JavaHome) {
            $jdkRoot = [IO.Path]::GetFullPath($JavaHome)
            foreach ($tool in @('java','javac','jar')) {
                $suffix = if ($IsWindows) { '.exe' } else { '' }
                $paths[$tool] = Resolve-AtfLauncherApplication $tool (Join-Path $jdkRoot ("bin/$tool$suffix"))
            }
        } else {
            foreach ($tool in @('java','javac','jar')) { $paths[$tool] = Resolve-AtfLauncherApplication $tool }
            $parents = @($paths.Values | ForEach-Object { [IO.Path]::GetDirectoryName($_) } | Select-Object -Unique)
            if ($parents.Count -ne 1) { throw 'java, javac and jar must resolve from one JDK' }
        }
        if ($RequireMaven) { $paths['mvn'] = Resolve-AtfLauncherApplication 'mvn' $Maven }
        if ($RequireNode) { $paths['node'] = Resolve-AtfLauncherApplication 'node' $Node }
    } catch {
        throw [InvalidOperationException]::new("LAUNCHER_CAPABILITY_RESOLVE: $($_.Exception.Message); source=$sourceHash", $_.Exception)
    }

    $gradleCapability = $null
    if ($GradleRoot) {
        $root = [IO.Path]::GetFullPath($GradleRoot)
        $wrapper = Join-Path $root 'gradlew.bat'
        $jar = Join-Path $root 'gradle/wrapper/gradle-wrapper.jar'
        $properties = Join-Path $root 'gradle/wrapper/gradle-wrapper.properties'
        foreach ($file in @($wrapper,$jar,$properties)) {
            if (!(Test-Path -LiteralPath $file -PathType Leaf)) { throw "LAUNCHER_CAPABILITY_GRADLE: missing pinned wrapper input $file; source=$sourceHash" }
        }
        $text = [IO.File]::ReadAllText($properties) -replace '\\:', ':'
        $distribution = [regex]::Match($text, '(?m)^distributionUrl=https://services\.gradle\.org/distributions/gradle-(?<version>[0-9]+(?:\.[0-9]+){1,2})-(?:bin|all)\.zip$')
        $checksum = [regex]::Match($text, '(?m)^distributionSha256Sum=(?<sha>[a-f0-9]{64})$')
        if (!$distribution.Success -or !$checksum.Success) {
            throw "LAUNCHER_CAPABILITY_GRADLE: wrapper distribution must be HTTPS and SHA-256 pinned; source=$sourceHash"
        }
        $paths['gradleWrapper'] = $wrapper
        $gradleCapability = [pscustomobject]@{tool='gradle';kind='PINNED_WRAPPER_INPUTS';version=$distribution.Groups['version'].Value
            distributionSha256=$checksum.Groups['sha'].Value;wrapper=$wrapper;wrapperJarSha256=(Get-FileHash -LiteralPath $jar -Algorithm SHA256).Hash.ToLowerInvariant()}
    }

    $capabilities = [Collections.Generic.List[object]]::new()
    try { $observedJava = Invoke-AtfLauncherVersion $paths['java'] @('--version') $TimeoutSeconds 'java' }
    catch { throw [InvalidOperationException]::new("LAUNCHER_CAPABILITY_PROBE: java; source=$sourceHash", $_.Exception) }
    $javaVersion = [regex]::Match($observedJava.Output, '(?m)^(?:openjdk|java)(?: version)?[ \t]+"?(?<major>[0-9]+)(?:[.\s"-]|$)')
    if ($observedJava.ExitCode -ne 0 -or !$javaVersion.Success -or [int]$javaVersion.Groups['major'].Value -lt 17) {
        throw "LAUNCHER_CAPABILITY_VERSION: java requires release 17 or newer; exit=$($observedJava.ExitCode); source=$sourceHash"
    }
    foreach ($tool in @('java','javac','jar')) {
        $kind = if ($tool -eq 'java') { 'JAVA_RELEASE_SUPPORT' } else { 'SAME_JDK_BINARY_PRESENT' }
        $capabilities.Add([pscustomobject]@{tool=$tool;kind=$kind;major=[int]$javaVersion.Groups['major'].Value;executable=$paths[$tool]})
    }
    if ($RequireMaven) {
        try { $observed = Invoke-AtfLauncherVersion $paths['mvn'] @('-version') $TimeoutSeconds 'mvn' }
        catch { throw [InvalidOperationException]::new("LAUNCHER_CAPABILITY_PROBE: mvn; source=$sourceHash", $_.Exception) }
        $version = [regex]::Match($observed.Output, '(?im)^Apache Maven[ \t]+(?<version>[0-9]+\.[0-9]+(?:\.[0-9]+)?)(?:[ \t]|$)')
        if ($observed.ExitCode -ne 0 -or !$version.Success) {
            throw "LAUNCHER_CAPABILITY_VERSION: mvn did not report an Apache Maven version; exit=$($observed.ExitCode); source=$sourceHash"
        }
        $capabilities.Add([pscustomobject]@{tool='mvn';kind='RUNTIME_VERSION';version=$version.Groups['version'].Value;executable=$paths['mvn']})
    }
    if ($RequireNode) {
        try { $observed = Invoke-AtfLauncherVersion $paths['node'] @('--version') $TimeoutSeconds 'node' }
        catch { throw [InvalidOperationException]::new("LAUNCHER_CAPABILITY_PROBE: node; source=$sourceHash", $_.Exception) }
        $version = [regex]::Match($observed.Output, '(?m)^v(?<major>[0-9]+)\.(?<minor>[0-9]+)\.(?<patch>[0-9]+)(?:[-+].*)?$')
        if ($observed.ExitCode -ne 0 -or !$version.Success) {
            throw "LAUNCHER_CAPABILITY_VERSION: node did not report a semantic version; exit=$($observed.ExitCode); source=$sourceHash"
        }
        $capabilities.Add([pscustomobject]@{tool='node';kind='RUNTIME_VERSION';version=$version.Value;executable=$paths['node']})
    }
    if ($gradleCapability) { $capabilities.Add($gradleCapability) }
    [pscustomobject]@{schemaVersion=1;kind='ATF_LAUNCHER_CAPABILITIES';sourceSha256=$sourceHash;executables=$paths;capabilities=$capabilities.ToArray()}
}
