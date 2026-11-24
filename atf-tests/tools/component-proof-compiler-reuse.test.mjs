import assert from 'node:assert/strict';
import fs from 'node:fs/promises';
import path from 'node:path';
import os from 'node:os';
import {execFileSync} from 'node:child_process';
import test from 'node:test';

// Only admission and Java execution are neighbouring mocks. Snapshot, cache seals,
// leases, task ordering and pending-proof promotion use the real launcher.
async function exercise(t, mode) {
  const root=await fs.mkdtemp(path.join(os.tmpdir(),'whist-proof-compiler-'));
  t.after(()=>fs.rm(root,{recursive:true,force:true}));
  const project=path.resolve(import.meta.dirname,'../..'),host=path.join(root,'host');
  for(const dir of ['scripts','config','atf-tests/tools']) {
    await fs.mkdir(path.join(host,dir),{recursive:true});
    for(const file of await fs.readdir(path.join(project,dir))) {
      if((dir==='scripts'&&!file.endsWith('.ps1'))||(dir==='atf-tests/tools'&&(!file.endsWith('.mjs')||file.endsWith('.test.mjs'))))continue;
      const source=path.join(project,dir,file);if((await fs.stat(source)).isFile())await fs.copyFile(source,path.join(host,dir,file));
    }
  }
  await fs.writeFile(path.join(host,'scripts/AtfTestChangeAdmission.ps1'),String.raw`
function Invoke-AtfTestChangeAdmission {
 param($SourceRoot,$BaseRef,$TempRoot,$JavaHome,$MavenCache,$ReceiptFile,[switch]$GenerateProofs,$Control)
 if(!$GenerateProofs){throw 'This controlled neighbour accepts generation only'}
 [IO.File]::WriteAllText($ReceiptFile,'{"schemaVersion":1,"kind":"MOCK_TEST_CHANGE_ADMISSION","status":"PASS","mode":"GENERATE"}')
}
`);
  const driver=path.join(root,'exercise.ps1');
  await fs.writeFile(driver,String.raw`
param($Root,$HostRoot,$Mode)
$ErrorActionPreference='Stop'
$scripts=Join-Path $HostRoot 'scripts'
. (Join-Path $scripts 'AtfQualificationSnapshot.ps1')
. (Join-Path $scripts 'AtfQualificationTiming.ps1')
. (Join-Path $scripts 'AtfComponentResume.ps1')
. (Join-Path $scripts 'AtfCompilerIdentity.ps1')
. (Join-Path $scripts 'AtfComponentMaven.ps1')
function Put($File,$Text){$null=New-Item -ItemType Directory -Force (Split-Path -Parent $File);[IO.File]::WriteAllText($File,$Text)}
$source=Join-Path $Root 'source';$javaHome=Join-Path $Root 'jdk';$java=Join-Path $javaHome 'bin/java.exe'
$cache=Join-Path $Root 'cache';$previous=Join-Path $Root 'previous'
$build=Join-Path $Root ('whist-component-suites-'+('a'*32))
foreach($dir in @($source,$cache,$previous,$build)){New-Item -ItemType Directory $dir | Out-Null}
Copy-Item -LiteralPath (Join-Path $HostRoot 'atf-tests') -Destination (Join-Path $source 'atf-tests') -Recurse
Put (Join-Path $source 'pom.xml') '<project/>'
$policy=@{mavenModules=@();additionalSourceRoots=@()}
$null=New-Item -ItemType Directory -Force (Join-Path $source 'config')
if($Mode -eq 'gradle') {
 $policy.additionalSourceRoots=@(@{path='gradle/src/test/java';test=$true})
 Put (Join-Path $source 'gradle/src/test/java/OnlyTest.java') 'original Gradle test'
}
Write-AtfComponentJson (Join-Path $source 'config/atf-governance-policy.json') $policy
foreach($name in @('release-platform-rag-policy.json','gradle-app-collections.properties','atf-component-collections.json','atf-test-suites.json')){
 Copy-Item -LiteralPath (Join-Path $HostRoot "config/$name") -Destination (Join-Path $source "config/$name")
}
$release=Join-Path $source 'release/1.2.3';$apk=Join-Path $release 'WhistLab-1.2.3.apk'
Put $apk 'controlled APK fixture';Put ($apk+'.sha256') ((Get-FileHash $apk).Hash.ToLowerInvariant()+'  WhistLab-1.2.3.apk'+[Environment]::NewLine)
$entries=@($apk,($apk+'.sha256'))|ForEach-Object {@{path=(Split-Path -Leaf $_);bytes=(Get-Item $_).Length;sha256=(Get-FileHash $_).Hash.ToLowerInvariant()}}
Write-AtfComponentJson (Join-Path $release 'release-validation.json') @{release=@{artifactKind='apk';versionName='1.2.3';apkName='WhistLab-1.2.3.apk';apkRelativePath='WhistLab-1.2.3.apk';apkBytes=(Get-Item $apk).Length;apkSha256=(Get-FileHash $apk).Hash.ToLowerInvariant()};integrity=@{files=@($entries)}}
Put (Join-Path $source 'release/index.json') '{"schemaVersion":2,"currentVersion":"1.2.3","versions":["1.2.3"]}'
$null=& git -C $source init -q
$null=& git -C $source config core.autocrlf false
$null=& git -C $source -c user.name=Fixture -c user.email=fixture@example.invalid add .
$null=& git -C $source -c user.name=Fixture -c user.email=fixture@example.invalid commit -q -m 'Controlled source'
$base=(& git -C $source rev-parse HEAD).Trim()
foreach($name in @('bin/javac.exe','release','lib/modules','bin/server/jvm.dll')){Put (Join-Path $javaHome $name) 'fixed tool'}
$csharp=Join-Path $Root 'MockJava.cs'
Put $csharp @'
using System;
using System.IO;
class MockJava {
 static int Main(string[] args) {
  int at=Array.IndexOf(args,"atf.tests.execution.components.TestChangeProofExecution");
  if(at<0)return 21;
  File.AppendAllText(Environment.GetEnvironmentVariable("PROOF_RUN_MARKER"),"fresh-control\n");
  string mode=Environment.GetEnvironmentVariable("PROOF_RUN_MODE");
  if(mode=="failure")return 7;
  if(mode=="resume") {
   if(args.Length!=at+9)return 7;
   File.AppendAllText(Environment.GetEnvironmentVariable("PROOF_RUN_MARKER"),"resumed-control:"+args[at+8]+"\n");
  }
  if(mode=="partial")return 0;
  Directory.CreateDirectory(args[at+3]);
  File.WriteAllText(Path.Combine(args[at+3],"proof.pending.json"),"{\"kind\":\"CONTROLLED_TRANSPORT_FIXTURE\"}");
  return 0;
 }
}
'@
$compiler=Join-Path $env:WINDIR 'Microsoft.NET/Framework64/v4.0.30319/csc.exe'
& $compiler /nologo "/out:$java" $csharp | Out-Null
if($LASTEXITCODE -ne 0){throw 'Controlled Java transport compilation failed'}
$maven=Join-Path $Root 'maven/bin/mvn.ps1';Put $maven '[IO.File]::WriteAllText($env:MAVEN_RUN_MARKER,"cold");exit 31'
$inputs=Join-Path $Root 'inputs.json'
$rows=@(& (Join-Path $scripts 'Get-AtfSourceInputs.ps1') -SourceRoot $source)
Write-AtfComponentJson $inputs $rows
$identity=Get-AtfComponentCompilerIdentity $inputs $java $maven $source
Copy-Item $inputs (Join-Path $build 'compiler-source-inputs.json')
Put (Join-Path $build 'project/pom.xml') ([IO.File]::ReadAllText((Join-Path $source 'pom.xml')))
Put (Join-Path $build 'project/config/atf-governance-policy.json') ([IO.File]::ReadAllText((Join-Path $source 'config/atf-governance-policy.json')))
$repo=Join-Path $build 'maven-repository';$jar=Join-Path $repo 'dependency.jar'
Put $jar 'controlled bytecode';Put (Join-Path $repo 'dependency.pom') '<project/>'
$classpath=Join-Path $build 'component-classpath.txt';Put $classpath $jar
Write-AtfComponentJson (Join-Path $build 'component-maven.json') @{schemaVersion=2;identity=$identity.identity;repo='maven-repository';compilerFingerprint=$identity.compilerFingerprint;toolFingerprint=$identity.toolFingerprint;inputManifestSha256=(Get-FileHash (Join-Path $build 'compiler-source-inputs.json')).Hash.ToLowerInvariant();buildFingerprint=(Get-AtfComponentMavenFingerprint $build $repo @());classpathSha256=(Get-FileHash $classpath).Hash}
Write-AtfComponentJson (Join-Path $previous 'source-inputs.json') $rows
Write-AtfComponentJson (Join-Path $previous 'release-inputs.json') @((Get-AtfReleaseInputs $source))
$qualified=[pscustomobject]@{source=[IO.File]::ReadAllText((Join-Path $previous 'source-inputs.json'));release=[IO.File]::ReadAllText((Join-Path $previous 'release-inputs.json'))}
Write-AtfComponentWorkspace $previous $build $qualified
if($Mode -eq 'tampered'){Put $jar 'changed bytecode'}
$env:PROOF_RUN_MARKER=Join-Path $Root 'controls.marker';$env:MAVEN_RUN_MARKER=Join-Path $Root 'maven.marker';$env:PROOF_RUN_MODE=$Mode
$env:WHIST_QUALIFICATION_TEMP_ROOT=$Root;$env:TEMP=$Root;$env:TMP=$Root
$runs=@();$count=if($Mode -in @('success','gradle','catalog','resume')){2}else{1}
for($n=0;$n -lt $count;$n++){
 if($Mode -eq 'gradle') {
  Put (Join-Path $source 'gradle/src/test/java/OnlyTest.java') "changed Gradle test $n"
  $null=& git -C $source -c user.name=Fixture -c user.email=fixture@example.invalid add .
  $null=& git -C $source -c user.name=Fixture -c user.email=fixture@example.invalid commit -q -m "Changed Gradle test $n"
 }
 if($Mode -eq 'catalog') {
  Write-AtfComponentJson (Join-Path $source 'release/index.json') @{schemaVersion=2;currentVersion='1.2.3';versions=@('1.2.3');fixtureRevision=$n}
  $null=& git -C $source -c user.name=Fixture -c user.email=fixture@example.invalid add .
  $null=& git -C $source -c user.name=Fixture -c user.email=fixture@example.invalid commit -q -m "Changed catalog observation $n"
 }
 $output=Join-Path $Root "run-$n";$log=Join-Path $Root "run-$n.log"
 $resume=@(if($Mode -eq 'resume' -and $n -eq 1){'-ResumeFrom';(Join-Path $Root 'run-0')})
 & pwsh -NoProfile -NonInteractive -File (Join-Path $scripts 'run-component-suites.ps1') -SourceRoot $source -RunRoot $output -TestChangeControl FIXTURE-COMPILER -TestChangeBaseRef $base -CompilerReuseFrom $previous @resume -JavaHome $javaHome -Maven $maven -MavenCache $cache *> $log
 $runs+=@{exit=$LASTEXITCODE;proof=(Test-Path (Join-Path $output 'FIXTURE-COMPILER/proof.json'));sourceFingerprint=if(Test-Path (Join-Path $output 'source-provenance.json')){(Get-Content (Join-Path $output 'source-provenance.json') -Raw|ConvertFrom-Json).sourceFingerprint}else{''};releaseFingerprint=if(Test-Path (Join-Path $output 'source-provenance.json')){(Get-Content (Join-Path $output 'source-provenance.json') -Raw|ConvertFrom-Json).releaseFingerprint}else{''};phases=if(Test-Path (Join-Path $output 'timing.json')){(Get-Content (Join-Path $output 'timing.json') -Raw|ConvertFrom-Json).stages}else{@()};log=(Get-Content $log -Raw)}
}
@{runs=$runs;maven=(Test-Path $env:MAVEN_RUN_MARKER);controls=if(Test-Path $env:PROOF_RUN_MARKER){@(Get-Content $env:PROOF_RUN_MARKER).Count}else{0};controlLines=@(if(Test-Path $env:PROOF_RUN_MARKER){Get-Content $env:PROOF_RUN_MARKER})}|ConvertTo-Json -Depth 8
`);
  const environment={...process.env,MAVEN_OPTS:'',MAVEN_ARGS:'',JAVACMD:'',WHIST_EVIDENCE_ROOT:''};
  for(const name of ['JAVA_TOOL_OPTIONS','JDK_JAVA_OPTIONS','_JAVA_OPTIONS'])delete environment[name];
  return JSON.parse(execFileSync('pwsh',['-NoProfile','-NonInteractive','-File',driver,root,host,mode],{
    encoding:'utf8',timeout:90000,maxBuffer:1024*1024,env:environment
  }));
}

test('proof compiler reuse keeps every control fresh and records compilation reuse twice', async t=>{
  const result=await exercise(t,'success');for(const run of result.runs)assert.equal(run.exit,0,run.log);assert.equal(result.maven,false);assert.equal(result.controls,2);
  for(const run of result.runs){assert.equal(run.exit,0,run.log);assert.equal(run.proof,true);assert.equal(run.phases.find(row=>row.stage==='components.maven-compile').status,'REUSED');}
  observe('success',result);
});
test('isolated Gradle test changes reuse Maven while two source snapshots execute fresh controls',async t=>{
 const result=await exercise(t,'gradle');assert.equal(result.maven,false);assert.equal(result.controls,2);
 assert.equal(new Set(result.runs.map(run=>run.sourceFingerprint)).size,2);
 for(const run of result.runs){assert.equal(run.exit,0,run.log);assert.equal(run.proof,true);assert.equal(run.phases.find(row=>row.stage==='components.maven-compile').status,'REUSED');}
 observe('gradle',result);
});
test('later catalog observations reuse the compiler without relabeling its original context',async t=>{
 const result=await exercise(t,'catalog');assert.equal(result.maven,false);assert.equal(result.controls,2);
 assert.equal(new Set(result.runs.map(run=>run.releaseFingerprint)).size,2);
 for(const run of result.runs){assert.equal(run.exit,0,run.log);assert.equal(run.proof,true);assert.equal(run.phases.find(row=>row.stage==='components.maven-compile').status,'REUSED');}
 observe('catalog',result);
});
for(const mode of ['failure','partial','tampered'])test(`proof compiler reuse rejects ${mode} execution without promoting a proof`,async t=>{
  const result=await exercise(t,mode);assert.equal(result.runs[0].proof,false);assert.notEqual(result.runs[0].exit,0,result.runs[0].log);
  assert.equal(result.maven,mode==='tampered');assert.equal(result.controls,mode==='tampered'?0:1);
  if(mode==='tampered')assert.match(result.runs[0].phases.find(row=>row.stage==='components.compiler-reuse').reason,/COMPONENT_COMPILER_OUTPUTS_UNQUALIFIED/);
  observe(mode,result);
});
function observe(mode,result){process.stdout.write(JSON.stringify({fixture:'Mocked Java transport; real launcher, snapshot and compiler admission',mode,mavenStarted:result.maven,freshControlExecutions:result.controls,runs:result.runs.map(run=>({exit:run.exit,proofPromoted:run.proof,compilerStages:run.phases.filter(row=>row.stage.includes('compiler')||row.stage.includes('maven')).map(({stage,status})=>({stage,status}))}))})+'\n');}

test('single static proof resume forwards only its original control and reuses admitted compilation',async t=>{
 const result=await exercise(t,'resume');
 assert.notEqual(result.runs[0].exit,0);
 assert.equal(result.runs[0].proof,false);
 assert.equal(result.runs[1].exit,0,result.runs[1].log);
 assert.equal(result.runs[1].proof,true);
 assert.equal(result.maven,false);
 assert.equal(result.controls,3,'Two child launches plus the explicit resume marker');
 assert.match(result.controlLines.at(-1),/resumed-control:.*[/\\]run-0[/\\]FIXTURE-COMPILER$/);
 assert.equal(result.runs[1].phases.find(row=>row.stage==='components.maven-compile').status,'REUSED');
 observe('resume',result);
});
