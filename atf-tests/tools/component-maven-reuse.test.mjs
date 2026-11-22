import assert from 'node:assert/strict';
import {execFileSync} from 'node:child_process';
import fs from 'node:fs/promises';
import os from 'node:os';
import path from 'node:path';
import test from 'node:test';

async function exercise(t, negative) {
  const root=await fs.mkdtemp(path.join(os.tmpdir(),'whist-compiler-reuse-'));
  t.after(()=>fs.rm(root,{recursive:true,force:true}));
  const script=path.join(root,'exercise.ps1');
  await fs.writeFile(script,String.raw`
param($Root,$Scripts,[switch]$Negative)
$ErrorActionPreference='Stop'
. (Join-Path $Scripts 'AtfQualificationSnapshot.ps1')
. (Join-Path $Scripts 'AtfQualificationTiming.ps1')
. (Join-Path $Scripts 'AtfComponentResume.ps1')
. (Join-Path $Scripts 'AtfCompilerIdentity.ps1')
. (Join-Path $Scripts 'AtfComponentMaven.ps1')
. (Join-Path $Scripts 'AtfComponentMavenReuse.ps1')
function Write-Fixture($File,$Text) { $null=New-Item -ItemType Directory -Force (Split-Path -Parent $File); [IO.File]::WriteAllText($File,$Text) }
function Expect($Condition,$Message) { if(!$Condition){throw $Message} }
$build=Join-Path $Root ('whist-component-suites-'+('a'*32))
$target=Join-Path $Root ('whist-component-suites-'+('b'*32))
$chain=Join-Path $Root ('whist-component-suites-'+('c'*32))
$previous=Join-Path $Root 'previous'
$next=Join-Path $Root 'next'
foreach($folder in @($build,$target,$chain,$previous,$next)){ $null=New-Item -ItemType Directory $folder }
$java=Join-Path $Root 'jdk/bin/java.exe';$maven=Join-Path $Root 'maven/bin/mvn.cmd'
foreach($name in @('bin/java.exe','bin/javac.exe','release','lib/modules','bin/server/jvm.dll')) { Write-Fixture (Join-Path $Root "jdk/$name") 'fixed tool' }
Write-Fixture $maven 'fixed Maven';Write-Fixture (Join-Path $Root 'maven/conf/settings.xml') '<settings/>'
$source=Join-Path $Root 'source.json'
$rows=@(@{path='demo/src/main/Probe.java';sha256=('a'*64)},@{path='README.md';sha256=('b'*64)})
Write-AtfComponentJson $source $rows
$compiler=Get-AtfComponentCompilerIdentity $source $java $maven
Expect $compiler.eligible 'fixture environment must be reusable'
$repo=Join-Path $build 'maven-repository'
$jar=Join-Path $repo 'dep.jar';$pom=Join-Path $repo 'dep.pom'
$class=Join-Path $build 'project/demo/target/classes/Probe.class'
$resource=Join-Path $build 'project/demo/target/test-classes/case.txt'
Write-Fixture $jar 'dependency';Write-Fixture $pom '<project/>';Write-Fixture $class 'bytecode';Write-Fixture $resource 'resource'
$classpath=Join-Path $build 'component-classpath.txt';Write-Fixture $classpath $jar
$manifest=Join-Path $build 'compiler-source-inputs.json';Copy-Item $source $manifest
$receipt=[ordered]@{schemaVersion=2;identity=$compiler.identity;repo='maven-repository';compilerFingerprint=$compiler.compilerFingerprint
 toolFingerprint=$compiler.toolFingerprint;inputManifestSha256=(Get-FileHash $manifest).Hash.ToLowerInvariant()
 buildFingerprint=(Get-AtfComponentMavenFingerprint $build $repo @('demo'));classpathSha256=(Get-FileHash $classpath).Hash}
$receiptFile=Join-Path $build 'component-maven.json';Write-AtfComponentJson $receiptFile $receipt
Write-AtfComponentJson (Join-Path $previous 'component-workspace.json') @{schemaVersion=1;path=$build}
function Borrow { Read-AtfBorrowedComponentMaven $previous $target $compiler.identity @('demo') $Root }
$outcomes=[Collections.Generic.List[string]]::new()
if(!$Negative) {
 $rows[1].sha256=('c'*64);$rows+=@{path='config/documentation-editions.json';sha256=('d'*64)}
 Write-AtfComponentJson $source $rows
 $new=Get-AtfComponentCompilerIdentity $source $java $maven
 Expect ($new.identity -ceq $compiler.identity) 'editorial changes must preserve compiler identity'
 $borrowed=Borrow
 Expect ($null -ne $borrowed) 'complete compiler output must be reusable'
 try {
  Expect ($borrowed.buildWorkspace -ceq $build) 'classes remain in the verified build workspace'
  Expect (!(Test-Path (Join-Path $target 'project/demo/target'))) 'no duplicate output tree'
  $blocked=$false;try{$other=Open-AtfQualificationWorkspace $build;$other.Dispose()}catch{$blocked=$true}
  Expect $blocked 'the build lease must stay held during consumption'
  Write-AtfComponentMavenReference $target $borrowed $new.identity
 } finally { $borrowed.lease.Dispose() }
 Write-AtfComponentJson (Join-Path $next 'component-workspace.json') @{schemaVersion=1;path=$target}
 $borrowed=Read-AtfBorrowedComponentMaven $next $chain $new.identity @('demo') $Root
 Expect ($null -ne $borrowed) 'reference chains must resolve to the original build'
 $borrowed.lease.Dispose()
 $outcomes.Add('documentation preserves compiler identity, output bytes and a leased reference chain')
} else {
 foreach($file in @('demo/src/main/Probe.java','pom.xml','lib/api.jar','module/src/test/resources/README.md','unknown.bin')) {
  $changed=@(@{path=$file;sha256=('e'*64)})
  Write-Fixture $source (ConvertTo-Json -InputObject $changed -Compress)
  $new=Get-AtfComponentCompilerIdentity $source $java $maven
  Expect ($new.identity -cne $compiler.identity) "changed compiler input accepted: $file"
 }
 Copy-Item $manifest $source
 foreach($file in @($java,(Join-Path $Root 'jdk/lib/modules'),(Join-Path $Root 'maven/conf/settings.xml'))) {
  $before=[IO.File]::ReadAllText($file);Write-Fixture $file 'changed tool'
  Expect ((Get-AtfComponentCompilerIdentity $source $java $maven).identity -cne $compiler.identity) 'tool/config change accepted'
  Write-Fixture $file $before
 }
 foreach($file in @($class,$resource,$jar,$pom)) {
  $before=[IO.File]::ReadAllText($file);Write-Fixture $file 'changed output'
  Expect ($null -eq (Borrow)) "altered output accepted: $file";Write-Fixture $file $before
 }
 $saved=[IO.File]::ReadAllText($receiptFile)
 foreach($field in @('identity','compilerFingerprint','toolFingerprint','inputManifestSha256','buildFingerprint','schemaVersion')) {
  $bad=$saved|ConvertFrom-Json;$bad.$field=if($field -eq 'schemaVersion'){99}else{'foreign'}
  Write-AtfComponentJson $receiptFile $bad
  Expect ($null -eq (Borrow)) "foreign receipt accepted: $field";Write-Fixture $receiptFile $saved
 }
 Write-Fixture $classpath 'foreign classpath'
 Expect ($null -eq (Borrow)) 'altered classpath accepted';Write-Fixture $classpath $jar
 $outside=Join-Path $Root 'foreign.jar';Write-Fixture $outside 'foreign';Write-Fixture $classpath $outside
 $bad=$saved|ConvertFrom-Json;$bad.classpathSha256=(Get-FileHash $classpath).Hash;Write-AtfComponentJson $receiptFile $bad
 Expect ($null -eq (Borrow)) 'foreign classpath accepted with a matching text hash'
 Write-Fixture $classpath $jar;Write-Fixture $receiptFile $saved
 $lock=Open-AtfQualificationWorkspace $build
 try { Expect ($null -eq (Borrow)) 'busy compilation workspace accepted' } finally {$lock.Dispose()}
 Write-AtfComponentJson (Join-Path $previous 'component-workspace.json') @{schemaVersion=1;path=$Root}
 Expect ($null -eq (Borrow)) 'workspace outside the configured perimeter accepted'
 Write-AtfComponentJson (Join-Path $previous 'component-workspace.json') @{schemaVersion=1;path=$build}
 $file=Join-Path $build 'project/demo/target/classes/nested';$foreign=Join-Path $Root 'foreign-classes'
 $null=New-Item -ItemType Directory $foreign
 $null=New-Item -ItemType Junction -Path $file -Target $foreign
 try { Expect ($null -eq (Borrow)) 'indirect class directory accepted' } finally { [IO.Directory]::Delete($file) }
 Remove-Item -LiteralPath $classpath
 $partial=Read-AtfPreparedComponentMaven $build $compiler.identity @('demo')
 Expect ($null -ne $partial -and !$partial.classpathReady) 'missing classpath must preserve owned compilation only'
 Expect ($null -eq (Borrow)) 'incomplete classpath must not be borrowed'
 Write-Fixture $classpath $jar
 $restored=Borrow
 Expect ($null -ne $restored) 'restored classpath must admit the original compilation again'
 $restored.lease.Dispose()
 Write-AtfComponentJson (Join-Path $previous 'component-workspace.json') @{schemaVersion=1;path=(Join-Path $Root ('whist-component-suites-'+('d'*32)))}
 Expect ($null -eq (Borrow)) 'cleaned compiler workspace must trigger cold preparation'
 Write-AtfComponentJson (Join-Path $previous 'component-workspace.json') @{schemaVersion=1;path=$build}
 Remove-Item -LiteralPath $class
 Expect ($null -eq (Borrow)) 'missing bytecode accepted'
 $outcomes.Add('changed sources, tools, outputs, foreign receipts, classpaths, locks and links are rejected')
}
$outcomes|ConvertTo-Json -Compress
`);
  return execFileSync('pwsh',['-NoProfile','-NonInteractive','-File',script,root,
    path.resolve(import.meta.dirname,'../../scripts'),...(negative?['-Negative']:[])],{
    encoding:'utf8',timeout:90000,env:{...process.env,MAVEN_ARGS:'',JAVACMD:'',MAVEN_OPTS:''}
  });
}

test('documentation preserves verified compilation without copying the repository or classes', async t=>{
  assert.match(await exercise(t,false),/documentation preserves compiler identity/);
});
test('changed compiler inputs and unverified workspaces reject compilation reuse', async t=>{
  assert.match(await exercise(t,true),/changed sources, tools, outputs/);
});
