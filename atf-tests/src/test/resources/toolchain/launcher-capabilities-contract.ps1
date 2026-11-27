param([Parameter(Mandatory)][string]$Root,[Parameter(Mandatory)][string]$Work,[ValidateSet('ready','failures')][string]$Case)
$ErrorActionPreference='Stop'
. (Join-Path $Root 'scripts/AtfLauncherCapabilities.ps1')
$script:Events=[Collections.Generic.List[string]]::new();$script:Timeouts=[Collections.Generic.List[string]]::new()
$script:Missing='';$script:Bad='';$script:JavaMajor=21
function Resolve-AtfLauncherApplication([string]$Name,[string]$Override='') {
    $tool=$Name.ToLowerInvariant()
    $script:Events.Add('resolve:'+$tool)
    if($script:Missing -eq $tool){throw "controlled missing $tool"}
    if($Override -and [IO.Path]::IsPathRooted($Override)){return $Override}
    if($tool -eq 'mvn'){return 'C:\fixture\maven\bin\mvn.cmd'}
    if($tool -eq 'node'){return 'C:\fixture\node\node.exe'}
    return "C:\fixture\jdk\bin\$tool.exe"
}
function Invoke-AtfLauncherVersion([string]$Executable,[string[]]$Arguments,[int]$TimeoutSeconds,[string]$Tool) {
    $script:Events.Add('probe:'+$Tool)
    $script:Timeouts.Add($Tool+'='+$TimeoutSeconds)
    if($script:Bad -eq 'java-malformed' -and $Tool -eq 'java'){return @{ExitCode=0;Output='installation alias'} }
    if($script:Bad -eq 'java-old' -and $Tool -eq 'java'){return @{ExitCode=0;Output='openjdk 11.0.1'} }
    if($script:Bad -eq 'java-exit' -and $Tool -eq 'java'){return @{ExitCode=7;Output='openjdk 21.0.1'} }
    if($script:Bad -eq 'node-malformed' -and $Tool -eq 'node'){return @{ExitCode=0;Output='node current'} }
    if($script:Bad -eq 'node-exit' -and $Tool -eq 'node'){return @{ExitCode=7;Output='v24.0.0'} }
    if($script:Bad -eq 'maven-malformed' -and $Tool -eq 'mvn'){return @{ExitCode=0;Output='maven launcher alias'} }
    if($script:Bad -eq 'maven-exit' -and $Tool -eq 'mvn'){return @{ExitCode=7;Output='Apache Maven 3.10.0'} }
    if($Tool -eq 'java'){return @{ExitCode=0;Output="openjdk $($script:JavaMajor).0.1"} }
    if($Tool -eq 'mvn'){return @{ExitCode=0;Output='Apache Maven 3.10.0'} }
    if($Tool -eq 'node'){return @{ExitCode=0;Output='v24.22.3'} }
    return @{ExitCode=0;Output='v24.22.3'}
}
function New-GradleFixture([string]$Directory,[bool]$Pinned) {
    $wrapper=Join-Path $Directory 'gradle/wrapper'
    New-Item -ItemType Directory -Force -Path $wrapper | Out-Null
    Set-Content -LiteralPath (Join-Path $Directory 'gradlew.bat') -Value '@echo off' -NoNewline
    [IO.File]::WriteAllBytes((Join-Path $wrapper 'gradle-wrapper.jar'),[byte[]](1,2,3,4))
    $text="distributionUrl=https\://services.gradle.org/distributions/gradle-9.6.1-bin.zip`n"
    if($Pinned){$text+='distributionSha256Sum='+('a'*64)+"`n"}
    [IO.File]::WriteAllText((Join-Path $wrapper 'gradle-wrapper.properties'),$text)
}
$rows=@();$bindings=@()
if($Case -eq 'ready'){
    $gradle=Join-Path $Work 'gradle-ready';New-GradleFixture $gradle $true
    foreach($major in @(17,21)){
        $script:JavaMajor=$major;$script:Events.Clear();$script:Timeouts.Clear()
        $observed=Assert-AtfLauncherCapabilities -JavaHome 'C:\fixture\jdk' -Maven mvn -RequireMaven -Node node -RequireNode -GradleRoot $gradle
        $rows+=@{name="Java $major";sourceBound=($observed.sourceSha256 -match '^[a-f0-9]{64}$');events=@($script:Events.ToArray());
            probeTimeouts=@($script:Timeouts.ToArray());
            gradleKind=$observed.capabilities[-1].kind;jdkCapabilities=@($observed.capabilities|Where-Object tool -in @('java','javac','jar')).Count;
            capabilities=@($observed.capabilities|ForEach-Object{@{tool=$_.tool;kind=$_.kind}})}
    }
    foreach($entry in @(
        @{file='scripts/run-atf-governance.ps1';call='Assert-AtfLauncherCapabilities';later='Ensure-AtfEvidenceRoot'},
        @{file='scripts/run-component-suites.ps1';call='Assert-AtfLauncherCapabilities';later='Ensure-AtfEvidenceRoot'},
        @{file='scripts/run-whist-collection.ps1';call='Assert-AtfLauncherCapabilities';later='$stageRoot=Join-Path'},
        @{file='scripts/AtfStandaloneJava.ps1';call='Assert-AtfLauncherCapabilities';later='$work = Join-Path'})){
        $body=[IO.File]::ReadAllText((Join-Path $Root $entry.file))
        $early=$body.IndexOf($entry.call);$later=$body.IndexOf($entry.later)
        $bindings+=@{file=$entry.file;beforePreparation=($early -ge 0 -and $later -gt $early)}
    }
    @{status='PASS';scope='Injected command availability and version results plus actual launcher order; no build or device';rows=$rows;bindings=$bindings}|ConvertTo-Json -Depth 8 -Compress
}else{
    foreach($missing in @('java','javac','jar','mvn','node')){
        $script:Events.Clear();$script:Missing=$missing;$cause=''
        try{$null=Assert-AtfLauncherCapabilities -JavaHome 'C:\fixture\jdk' -Maven mvn -RequireMaven -Node node -RequireNode}catch{$cause=$_.Exception.ToString()}
        if(!$cause.Contains('LAUNCHER_CAPABILITY_RESOLVE') -or @($script:Events|Where-Object{$_ -like 'probe:*'}).Count){throw "Missing $missing was not rejected before probes"}
        $rows+=@{name='missing-'+$missing;refused=$true;sourceBound=($cause -match 'source=[a-f0-9]{64}');probeStarted=(@($script:Events|Where-Object{$_ -like 'probe:*'}).Count -gt 0);cause=$cause}
    }
    $script:Missing=''
    foreach($bad in @('java-malformed','java-old','java-exit','maven-malformed','maven-exit','node-malformed','node-exit')){
        $script:Events.Clear();$script:Bad=$bad;$cause=''
        try{$null=Assert-AtfLauncherCapabilities -JavaHome 'C:\fixture\jdk' -Maven mvn -RequireMaven -Node node -RequireNode}catch{$cause=$_.Exception.ToString()}
        if(!$cause.Contains('LAUNCHER_CAPABILITY_VERSION')){throw "Bad version $bad was accepted: $cause"}
        $rows+=@{name=$bad;refused=$true;sourceBound=($cause -match 'source=[a-f0-9]{64}');probeStarted=(@($script:Events|Where-Object{$_ -like 'probe:*'}).Count -gt 0);cause=$cause}
    }
    $badGradle=Join-Path $Work 'gradle-unpinned';New-GradleFixture $badGradle $false;$cause='';$script:Events.Clear()
    try{$null=Assert-AtfLauncherCapabilities -JavaHome 'C:\fixture\jdk' -Maven mvn -RequireMaven -GradleRoot $badGradle}catch{$cause=$_.Exception.ToString()}
    if(!$cause.Contains('LAUNCHER_CAPABILITY_GRADLE')){throw "Unpinned Gradle wrapper was accepted: $cause"}
    $rows+=@{name='gradle-unpinned';refused=$true;sourceBound=($cause -match 'source=[a-f0-9]{64}');probeStarted=(@($script:Events|Where-Object{$_ -like 'probe:*'}).Count -gt 0);cause=$cause}
    @{status='PASS';scope='Missing applications, malformed/nonzero version probes and unpinned wrapper; all fail before launcher preparation';rows=$rows}|ConvertTo-Json -Depth 8 -Compress
}
