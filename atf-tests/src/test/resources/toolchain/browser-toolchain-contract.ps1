param([Parameter(Mandatory)][string]$Root,[ValidateSet('ready','failures')][string]$Case)
$ErrorActionPreference='Stop'
. (Join-Path $Root 'scripts/AtfBrowserToolchain.ps1')
$script:Events=[Collections.Generic.List[string]]::new()
$script:Missing='';$script:Major=17;$script:Bad='';$script:ProcessMode='';$script:Waits=0
function Resolve-AtfBrowserTool([string]$Name) {
    $script:Events.Add('resolve:'+ $Name)
    if($Name -eq $script:Missing){throw "controlled missing $Name"}
    Join-Path ([IO.Path]::GetPathRoot($Root)) "controlled/$Name.exe"
}
function Read-AtfBrowserToolVersion([string]$Executable,[int]$TimeoutSeconds,[string]$Tool) {
    $script:Events.Add('probe:'+ $Tool)
    if($script:Bad -eq 'probe'){throw 'controlled probe failure'}
    $name=if($Tool -eq 'java'){'openjdk'}else{$Tool}
    $output=if($Tool -eq 'mvn'){'Apache Maven 3.10.0'}else{"$name $($script:Major).0.1"}
    if($script:Bad -eq 'malformed'){$output='installation alias, not a runtime'}
    [pscustomobject]@{ExitCode=$(if($script:Bad -eq 'exit'){12}else{0});Output=$output}
}
function New-ControlledProcess {
    $stream=[pscustomobject]@{}
    $stream|Add-Member ScriptMethod ReadToEndAsync {
        $script:Events.Add('read')
        if($script:ProcessMode -eq 'read'){throw 'controlled read failure'}
        [Threading.Tasks.Task]::FromResult('version output')
    }
    $process=[pscustomobject]@{StandardOutput=$stream;StandardError=$stream;ExitCode=0}
    $process|Add-Member ScriptMethod Start {
        $script:Events.Add('start')
        if($script:ProcessMode -eq 'start'){throw 'controlled start failure'}
        $script:ProcessMode -ne 'false-start'
    }
    $process|Add-Member ScriptMethod WaitForExit {param([int]$Milliseconds)
        $script:Events.Add("wait:$Milliseconds");$script:Waits++
        !($script:ProcessMode -in @('timeout','kill') -and $script:Waits -eq 1)
    }
    $process|Add-Member ScriptMethod Kill {param([bool]$Tree)
        $script:Events.Add("kill:$Tree")
        if($script:ProcessMode -eq 'kill'){throw 'controlled kill failure'}
    }
    $process|Add-Member ScriptMethod Dispose {
        $script:Events.Add('dispose')
        if($script:ProcessMode -eq 'dispose'){throw 'controlled dispose failure'}
    }
    $process
}
$rows=@()
if($Case -eq 'ready'){
    foreach($major in @(17,21)){
        $script:Major=$major;$script:Events.Clear()
        $observed=Assert-AtfBrowserToolchain -RequireMaven
        $rows+=@{name="Java $major";capabilities=$observed.capabilities;sourceSha256=$observed.sourceSha256;events=@($script:Events.ToArray())}
    }
    $bindings=@()
    foreach($entry in @(@{file='Start-WhistBrowser.ps1';later='Prepare-WhistCatalog.ps1'},@{file='Start-CardTableBrowser.ps1';later='$null = New-Item'},@{file='Build-WhistPages.ps1';later='Start-WhistBrowser.ps1'})){
        $body=[IO.File]::ReadAllText((Join-Path $Root ('atf-ui-browser/'+$entry.file)))
        $early=$body.IndexOf('Assert-AtfBrowserToolchain');$later=$body.IndexOf($entry.later)
        $bindings+=@{name=$entry.file;preflightBeforePreparation=($early -ge 0 -and $later -gt $early)}
    }
    @{status='PASS';scope='Controlled resolver/version responses and actual launcher source ordering; no build or renderer';rows=$rows;bindings=$bindings}|ConvertTo-Json -Depth 9 -Compress
}else{
    foreach($missing in @('java','javac','jar','mvn')){
        $script:Events.Clear();$script:Missing=$missing;$cause=''
        try{$null=Assert-AtfBrowserToolchain -RequireMaven}catch{$cause=$_.Exception.ToString()}
        if(!$cause.Contains('TOOL_CAPABILITY_MISSING') -or @($script:Events|Where-Object{$_ -like 'probe:*'}).Count){throw 'Missing-tool preflight was not early'}
        $rows+=@{name='missing-'+$missing;refused=$true;sourceBound=($cause -match 'source=[a-f0-9]{64}');cause=$cause;events=@($script:Events.ToArray())}
    }
    $script:Missing=''
    foreach($bad in @('old','malformed','exit','probe')){
        $script:Events.Clear();$script:Bad=$bad;$script:Major=if($bad -eq 'old'){11}else{21};$cause=''
        try{$null=Assert-AtfBrowserToolchain}catch{$cause=$_.Exception.ToString()}
        if(!$cause.Contains('TOOL_CAPABILITY_')){throw 'Bad capability was accepted'}
        $rows+=@{name=$bad;refused=$true;sourceBound=($cause -match 'source=[a-f0-9]{64}');cause=$cause;events=@($script:Events.ToArray())}
    }
    foreach($mode in @('false-start','start','read','timeout','kill','dispose')){
        $script:ProcessMode=$mode;$script:Waits=0;$script:Events.Clear();$cause=''
        try{$null=Complete-AtfBrowserToolProcess (New-ControlledProcess) 1 'controlled'}catch{$cause=$_.Exception.ToString()}
        if(!$cause -or @($script:Events|Where-Object{$_ -eq 'dispose'}).Count -ne 1){throw 'Process failure lost refusal or disposal'}
        if($mode -eq 'kill' -and (!$cause.Contains('LAUNCHER_CAPABILITY_TIMEOUT') -or !$cause.Contains('controlled kill failure'))){throw 'Cleanup masked timeout'}
        $rows+=@{name='process-'+$mode;refused=$true;disposedOnce=$true;cause=$cause;events=@($script:Events.ToArray())}
    }
    @{status='PASS';scope='Controlled missing tools, alias/old versions, startup/read/timeout and cleanup; no real wait or process';rows=$rows}|ConvertTo-Json -Depth 9 -Compress
}
