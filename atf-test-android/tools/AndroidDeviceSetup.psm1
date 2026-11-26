function Invoke-AtfAdbCommand {
    param(
        [Parameter(Mandatory)][string]$AdbExecutable,
        [Parameter(Mandatory)][string[]]$Arguments,
        [string]$ServerSocket = '',
        [ValidateRange(1,120)][int]$TimeoutSeconds = 15
    )

    $startInfo = [System.Diagnostics.ProcessStartInfo]::new()
    $startInfo.FileName = [IO.Path]::GetFullPath($AdbExecutable)
    $startInfo.UseShellExecute = $false
    $startInfo.CreateNoWindow = $true
    $startInfo.RedirectStandardOutput = $true
    $startInfo.RedirectStandardError = $true
    foreach ($argument in $Arguments) { $startInfo.ArgumentList.Add($argument) }
    if (-not [string]::IsNullOrWhiteSpace($ServerSocket)) {
        $startInfo.EnvironmentVariables['ADB_SERVER_SOCKET'] = $ServerSocket
    }

    $process = [System.Diagnostics.Process]::new()
    $process.StartInfo = $startInfo
    Complete-AtfAdbProcess $process $TimeoutSeconds ($Arguments -join ' ')
}

# Private process port: components can verify ordering without starting a device or a shell.
function Complete-AtfAdbProcess($Process, [int]$TimeoutSeconds, [string]$Command) {
    $started = $false
    $completed = $false
    $failure = $null
    $cleanup = [Collections.Generic.List[Exception]]::new()
    $streams = @()
    $result = $null
    try {
        $started = $Process.Start()
        if (-not $started) { throw "Could not start ADB client: $($Process.StartInfo.FileName)" }
        $streams = @($Process.StandardOutput.ReadToEndAsync(), $Process.StandardError.ReadToEndAsync())
        $completed = $Process.WaitForExit($TimeoutSeconds * 1000)
        if (-not $completed) {
            $failure = [TimeoutException]::new("ADB command timed out after ${TimeoutSeconds}s: $Command")
            try { $Process.Kill($true) } catch { $cleanup.Add($_.Exception) }
            try {
                if (-not $Process.WaitForExit(2000)) { throw [TimeoutException]::new("ADB process did not exit within the 2s termination bound: $Command") }
            } catch { $cleanup.Add($_.Exception) }
            if (-not [System.Threading.Tasks.Task]::WaitAll([System.Threading.Tasks.Task[]]$streams, 2000)) {
                $cleanup.Add([TimeoutException]::new("ADB output did not drain within the 2s termination bound: $Command"))
            }
        } elseif (-not [System.Threading.Tasks.Task]::WaitAll([System.Threading.Tasks.Task[]]$streams, $TimeoutSeconds * 1000)) {
            $failure = [TimeoutException]::new("ADB output timed out after ${TimeoutSeconds}s: $Command")
        }
        if (-not $failure) {
            $failure = $null
            $result = [pscustomobject]@{ExitCode=$Process.ExitCode;Output=($streams[0].Result + $streams[1].Result).Trim()}
        }
    } catch { $failure = $_.Exception }
    try { $Process.Dispose() } catch { $cleanup.Add($_.Exception) }
    if ($failure -and -not $cleanup.Count) { throw $failure }
    if ($failure) { $cleanup.Insert(0, $failure) }
    if ($cleanup.Count) { throw [AggregateException]::new('ADB process failed; original and cleanup causes retained', $cleanup.ToArray()) }
    return $result
}

function Connect-AtfAndroidDevice {
    param(
        [Parameter(Mandatory)][string]$AdbExecutable,
        [Parameter(Mandatory)][string]$Serial,
        [string]$ServerSocket = '',
        [ValidateRange(1,120)][int]$TimeoutSeconds = 15
    )

    if (-not (Test-Path -LiteralPath $AdbExecutable -PathType Leaf)) {
        throw "ADB client was not found: $AdbExecutable"
    }

    $connection = ''
    if ($Serial -match '^[^:]+:\d+$') {
        $connectResult = Invoke-AtfAdbCommand -AdbExecutable $AdbExecutable `
                -Arguments @('connect', $Serial) -ServerSocket $ServerSocket -TimeoutSeconds $TimeoutSeconds
        $connection = $connectResult.Output
        if ($connectResult.ExitCode -ne 0 -or $connection -match '(?i)failed to connect|cannot connect|connection refused|connection reset|protocol fault|unauthorized') {
            throw "Could not connect ADB transport '$Serial' through '$ServerSocket': $connection"
        }
    }

    $stateResult = Invoke-AtfAdbCommand -AdbExecutable $AdbExecutable `
            -Arguments @('-s', $Serial, 'get-state') -ServerSocket $ServerSocket -TimeoutSeconds $TimeoutSeconds
    $state = $stateResult.Output.Trim()
    if ($stateResult.ExitCode -ne 0 -or $state -ne 'device') {
        throw "ADB transport '$Serial' is not ready through '$ServerSocket': $state"
    }

    [pscustomobject]@{
        Ready = $true
        Serial = $Serial
        ServerSocket = $ServerSocket
        Connection = $connection
        State = $state
    }
}

function Get-AtfEffectiveDisplayValue {
    param([string]$Output, [string]$Property, [string]$ValuePattern)
    foreach ($kind in @('Override','Physical')) {
        $pattern = "(?m)^${kind} ${Property}: *($ValuePattern)[ \t]*\r?$"
        $matchesForKind = [regex]::Matches($Output, $pattern)
        $labels = [regex]::Matches($Output, "(?m)^$kind $($Property):").Count
        if ($matchesForKind.Count -eq 1 -and $labels -eq 1) { return $matchesForKind[0].Groups[1].Value }
        if ($Output -cmatch "(?m)^${kind} ${Property}:") {
            throw "ANDROID_PREFLIGHT_PROFILE: ambiguous or invalid $kind $Property."
        }
    }
    throw "ANDROID_PREFLIGHT_PROFILE: missing effective $Property."
}

function Get-AtfAndroidReadiness {
    param(
        [Parameter(Mandatory)][string]$AdbExecutable,
        [Parameter(Mandatory)][string]$Serial,
        [string]$ServerSocket = '',
        [ValidateRange(1,120)][int]$TimeoutSeconds = 20,
        [int]$ExpectedApi = 0,
        [string]$ExpectedDisplayProfile = ''
    )

    function Read-Device([string[]]$Command) {
        try {
            $response = Invoke-AtfAdbCommand -AdbExecutable $AdbExecutable -Arguments (@('-s', $Serial, 'shell') + $Command) -ServerSocket $ServerSocket -TimeoutSeconds $TimeoutSeconds
            if ($response.ExitCode -ne 0) { throw "exit $($response.ExitCode): $($response.Output)" }
            $response.Output
        } catch {
            throw "ANDROID_PREFLIGHT_ADB: '$Serial' $($Command -join ' '): $($_.Exception.Message)"
        }
    }

    try {
        Connect-AtfAndroidDevice -AdbExecutable $AdbExecutable -Serial $Serial -ServerSocket $ServerSocket -TimeoutSeconds $TimeoutSeconds | Out-Null
    } catch {
        throw "ANDROID_PREFLIGHT_ADB: $($_.Exception.Message)"
    }

    $boot = Read-Device @('getprop','sys.boot_completed')
    if ($boot -cne '1') { throw "ANDROID_PREFLIGHT_BOOT: '$Serial' sys.boot_completed='$boot', expected '1'." }

    $api = Read-Device @('getprop','ro.build.version.sdk')
    if ($api -cnotmatch '^[1-9][0-9]{0,2}$' -or ($ExpectedApi -gt 0 -and [int]$api -ne $ExpectedApi)) {
        throw "ANDROID_PREFLIGHT_PROFILE: '$Serial' observed API '$api', expected API '$ExpectedApi' (0 accepts any valid API)."
    }
    $size = Get-AtfEffectiveDisplayValue (Read-Device @('wm','size')) 'size' '[1-9][0-9]{0,4}x[1-9][0-9]{0,4}'
    $density = Get-AtfEffectiveDisplayValue (Read-Device @('wm','density')) 'density' '[1-9][0-9]{0,3}'
    $display = "$size@$density"
    if ($ExpectedDisplayProfile -and $display -cne $ExpectedDisplayProfile) {
        throw "ANDROID_PREFLIGHT_PROFILE: '$Serial' observed '$display', expected '$ExpectedDisplayProfile'."
    }
    [pscustomobject]@{bootCompleted=$true;api=[int]$api;displayProfile=$display}
}

Export-ModuleMember -Function Invoke-AtfAdbCommand,Connect-AtfAndroidDevice,Get-AtfAndroidReadiness
