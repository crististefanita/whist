import {runFixtureProcess} from './fixture-process.mjs';
import assert from 'node:assert/strict';
import test from 'node:test';
import fs from 'node:fs/promises';
import os from 'node:os';
import path from 'node:path';
import {createHash} from 'node:crypto';

async function exercise(t,negative,includeReleasePayload=true){
 const parent=await fs.realpath(process.env.WHIST_QUALIFICATION_TEMP_ROOT??os.tmpdir());
 const root=await fs.mkdtemp(path.join(parent,'whist-snapshot-io-'));
 t.after(async()=>{assert.equal(path.dirname(await fs.realpath(root)),parent);await fs.rm(root,{recursive:true,force:true});});
 const script=path.join(root,'exercise.ps1');
 await fs.writeFile(script,String.raw`
param($Root,$Scripts,$Negative,$IncludeReleasePayload)
$ErrorActionPreference='Stop'
[Console]::OutputEncoding=[Text.UTF8Encoding]::new($false)
. (Join-Path $Scripts 'AtfQualificationSnapshot.ps1')
. (Join-Path $Scripts 'AtfQualificationTiming.ps1')
. (Join-Path $Scripts 'AtfComponentMaven.ps1')
function Put($File,$Text){$null=[IO.Directory]::CreateDirectory([IO.Path]::GetDirectoryName($File));[IO.File]::WriteAllText($File,$Text)}
function Expect($Value,$Message){if(!$Value){throw $Message}}
$live=Join-Path $Root 'live';$stage=Join-Path $Root 'work/project';$run=Join-Path $Root 'run'
$null=[IO.Directory]::CreateDirectory($run)
$null=[IO.Directory]::CreateDirectory((Split-Path $stage -Parent))
Put (Join-Path $live 'config/atf-governance-policy.json') '{"mavenModules":[],"additionalSourceRoots":[],"buildRoots":[]}'
Put (Join-Path $live 'pom.xml') '<project/>'
Put (Join-Path $live 'README.md') 'guide'
Put (Join-Path $live '.hidden') 'hidden'
Put (Join-Path $live '.git') 'excluded pointer'
Put (Join-Path $live 'module/src/test/resources/README.md') 'resource'
Put (Join-Path $live 'unicode/cards-ș.txt') 'ș ț â î'
Put (Join-Path $live 'payload/data.bin') (('A'*4096)+'B')
Put (Join-Path $live 'payload/readonly.bin') 'read-only'
$readonly=Join-Path $live 'payload/readonly.bin';[IO.File]::SetLastWriteTimeUtc($readonly,[DateTime]::Parse('2001-02-03T04:05:06Z').ToUniversalTime());[IO.File]::SetAttributes($readonly,[IO.FileAttributes]::ReadOnly)
Put (Join-Path $live 'release/index.json') '{}'
Put (Join-Path $live 'release/payload.bin') 'release payload'
Put (Join-Path $live 'atf-tests/tools/check-release-artifacts.mjs') 'console.log("Controlled artifact-preflight neighbour");'
$timing=New-AtfQualificationTiming 'fixture' (Join-Path $run 'timing.json')
$snapshot=New-AtfQualificationSnapshot $live $stage $run '' $timing ([bool]::Parse($IncludeReleasePayload))
if($Negative -eq 'false' -and $snapshot.includeReleasePayload){
 $sources=@($snapshot.source|ConvertFrom-Json);$releases=@($snapshot.release|ConvertFrom-Json)
 $observed=@(Get-ChildItem -LiteralPath $stage -Recurse -Force -File|ForEach-Object{[IO.Path]::GetRelativePath($stage,$_.FullName).Replace('\','/')})
 Expect ($observed.Count -eq ($sources.Count+$releases.Count)) 'Complete staged population'
 $copiedReadonly=Join-Path $stage 'payload/readonly.bin'
 Expect (([IO.File]::GetAttributes($copiedReadonly) -band [IO.FileAttributes]::ReadOnly) -ne 0) 'Readonly attribute preserved'
 Expect ([IO.File]::GetLastWriteTimeUtc($copiedReadonly) -eq [IO.File]::GetLastWriteTimeUtc($readonly)) 'Original modification time preserved'
 @{source=$sources;release=$releases;timing=$timing.stages;preflight='controlled neighbour';files=@($observed|Sort-Object);attributesPreserved=$true}|ConvertTo-Json -Depth 8 -Compress
}elseif($Negative -eq 'false'){
 $sources=@($snapshot.source|ConvertFrom-Json);$releases=@($snapshot.release|ConvertFrom-Json)
 $observed=@(Get-ChildItem -LiteralPath $stage -Recurse -Force -File|ForEach-Object{[IO.Path]::GetRelativePath($stage,$_.FullName).Replace('\\','/')})
 Expect (!(Test-Path -LiteralPath (Join-Path $stage 'release'))) 'Unselected release payload must not enter source staging'
 Expect ($observed.Count -eq $sources.Count) 'Only the admitted source population is staged'
 Expect (@($releases|Where-Object path -like 'release/*').Count -eq 2) 'Complete release inventory remains bound separately'
 Expect (@(Get-AtfSnapshotDifferences $snapshot $stage $live).Count -eq 0) 'Source stage and complete live release inventory pass'
 $releaseFile=Join-Path $live 'release/payload.bin';$original=[IO.File]::ReadAllText($releaseFile);Put $releaseFile 'changed release payload'
 $capture=Join-Path $Root 'drift-capture';$null=[IO.Directory]::CreateDirectory($capture)
 try {
  $differences=@(Get-AtfSnapshotDifferences $snapshot $stage $live -CaptureRoot $capture)
  Expect ($differences.Count -eq 1 -and $differences[0] -eq 'LIVE-RELEASE: release/payload.bin') 'Unstaged release drift still blocks the source qualification'
  $receipt=Get-Content (Join-Path $capture 'source-snapshot-check.json') -Raw|ConvertFrom-Json
  Expect ($receipt.observations.Count -eq 4) 'Drift receipt retains staged-source, staged-release decision and both live inventories'
 } finally { Put $releaseFile $original }
 $timingRows=@($timing.stages|Where-Object {$_.stage -like 'snapshot.*'}|ForEach-Object {@{stage=$_.stage;status=$_.status;elapsedMs=$_.elapsedMs}})
 @{sourceCount=$sources.Count;releaseCount=$releases.Count;stagedSourceCount=$observed.Count;stagedReleaseCount=0
   releaseFingerprint=(Get-AtfInputFingerprint $snapshot.release);allLiveReleaseInputsStillSealed=$true
   changedReleaseRefused=$true;driftReceiptObservations=4;timing=$timingRows}|ConvertTo-Json -Depth 6 -Compress
}else{
 $observations=@()
 foreach($location in @($live,$stage)){
  $file=Join-Path $location 'payload/data.bin';$time=[IO.File]::GetLastWriteTimeUtc($file)
  Put $file (('A'*4096)+'C');[IO.File]::SetLastWriteTimeUtc($file,$time)
  $difference=@(Get-AtfSnapshotDifferences $snapshot $stage $live)
  Expect ($difference.Count -eq 1 -and $difference[0].Contains('payload/data.bin')) 'Same-time, same-size mutation must fail'
  $observations+=@{control='same metadata';location=$location;differences=$difference}
  Put $file (('A'*4096)+'B');[IO.File]::SetLastWriteTimeUtc($file,$time)
 }
 Put (Join-Path $stage 'unexpected.dat') 'foreign';Expect (@(Get-AtfSnapshotDifferences $snapshot $stage $live).Count -eq 1) 'Extra staged source rejected'
 [IO.File]::Delete((Join-Path $stage 'unexpected.dat'))
 $source=Join-Path $live 'README.md';$hash=Get-AtfFileSha256 $source
 foreach($kind in @('escape','private','changed','duplicate','source-link','target-link','release-link')){
  $destination=Join-Path $Root ('copy-'+$kind);$manifest=Join-Path $Root ('manifest-'+$kind+'.json');$links=@()
  $rows=@(@{path='README.md';sha256=$hash})
  switch($kind){
   'escape' {$rows+=@{path='../outside';sha256=$hash}}
  'private' {$rows+=@{path='release-tools/.atf-signing/private.key';sha256=$hash}}
   'changed' {$rows[0].sha256=('0'*64)}
   'duplicate' {$rows+=@{path='README.md';sha256=$hash}}
   'source-link' {$link=Join-Path $live 'linked';$null=New-Item -ItemType Junction -Path $link -Target $run;$links+= $link;$rows+=@{path='linked/file';sha256=$hash}}
   'target-link' {$null=[IO.Directory]::CreateDirectory($destination);$link=Join-Path $destination 'linked';$null=New-Item -ItemType Junction -Path $link -Target $run;$links+=$link;Put (Join-Path $live 'linked/file') 'input';$rows+=@{path='linked/file';sha256=$hash}}
   'release-link' {$link=Join-Path $live 'release/linked';$null=New-Item -ItemType Junction -Path $link -Target $run;$links+=$link}
  }
  [IO.File]::WriteAllText($manifest,(ConvertTo-AtfInputJson $rows))
  $caught=''
  try {if($kind -eq 'release-link'){Get-AtfReleaseInputs $live|Out-Null}else{& (Join-Path $Scripts 'Copy-AtfSourceInputs.ps1') -SourceRoot $live -Destination $destination -InputManifest $manifest}}catch{$caught=$_.Exception.Message}
  finally{foreach($link in $links){[IO.Directory]::Delete($link)}}
  $prefix=switch($kind){'escape'{'SOURCE-STAGE-ESCAPE'}'private'{'SOURCE-STAGE-PRIVATE-SIGNING'}'changed'{'SOURCE-STAGE-CHANGED'}'duplicate'{'SOURCE-STAGE-DUPLICATE'}'release-link'{'RELEASE-SNAPSHOT-INDIRECT'}default{'SOURCE-STAGE-INDIRECT'}}
  Expect ($caught.StartsWith($prefix)) ("Expected refusal for $kind, observed: $caught")
  if($kind -in @('escape','private','duplicate','source-link')){Expect (![IO.Directory]::Exists($destination)) 'Manifest admission precedes all copying'}
  $observations+=@{control=$kind;diagnostic=$caught}
 }
 $compiler=Join-Path $Root 'compiler';$repo=Join-Path $compiler 'maven-repository'
 Put (Join-Path $repo 'a/dep.jar') 'jar';Put (Join-Path $repo 'a/dep.pom') 'pom'
 Put (Join-Path $compiler 'project/module/target/classes/A.class') 'bytecode'
 Put (Join-Path $compiler 'project/module/target/test-classes/README.md') 'resource'
 $first=Get-AtfComponentMavenFingerprint $compiler $repo @('module')
 $file=Join-Path $compiler 'project/module/target/classes/A.class';$time=[IO.File]::GetLastWriteTimeUtc($file)
 Put $file 'BYTecode';[IO.File]::SetLastWriteTimeUtc($file,$time)
 Expect ((Get-AtfComponentMavenFingerprint $compiler $repo @('module')) -cne $first) 'Same-time corrupted class rejected'
 Put $file 'bytecode';$link=Join-Path $repo 'linked';$null=New-Item -ItemType Junction -Path $link -Target $run
 try{$caught='';try{Get-AtfComponentMavenFingerprint $compiler $repo @('module')|Out-Null}catch{$caught=$_.Exception.Message};Expect ($caught -eq 'COMPONENT_MAVEN_INDIRECT') 'Compiler link refused'}finally{[IO.Directory]::Delete($link)}
 $observations+=@{control='compiler';sameMetadataMutationRejected=$true;indirectTreeRejected=$true}
 Put (Join-Path $live 'atf-tests/tools/check-release-artifacts.mjs') 'process.exit(9);'
 $failedRun=Join-Path $Root 'failed-run';$failedStage=Join-Path $Root 'failed-work/project'
 $null=[IO.Directory]::CreateDirectory($failedRun);$null=[IO.Directory]::CreateDirectory((Split-Path $failedStage -Parent))
 $failedTiming=New-AtfQualificationTiming 'failure' (Join-Path $failedRun 'timing.json');$caught=''
 try{New-AtfQualificationSnapshot $live $failedStage $failedRun '' $failedTiming|Out-Null}catch{$caught=$_.Exception.Message}
 Expect ($caught.StartsWith('RELEASE-PREFLIGHT:') -and $failedTiming.stages[-1].status -eq 'FAIL') 'Failed preflight retains its original phase verdict'
 $observations+=@{control='artifact-preflight';verdict=$failedTiming.stages[-1].status}
 @{observations=$observations;boundaries='Real inventory, copy, full recheck and sealing; controlled artifact preflight only'}|ConvertTo-Json -Depth 8 -Compress
}
`);
 return JSON.parse(runFixtureProcess('pwsh',['-NoProfile','-NonInteractive','-File',script,root,
  path.resolve(import.meta.dirname,'../../scripts'),String(negative),String(includeReleasePayload)],{encoding:'utf8',windowsHide:true,timeout:60000}));
}
test('native snapshot I/O preserves independent bytes inventory and child timing identities',async t=>{
 const result=await exercise(t,false),seen=new Map([...result.source,...result.release].map(row=>[row.path,row.sha256]));
 const expected={'README.md':'guide','.hidden':'hidden','module/src/test/resources/README.md':'resource',
  'unicode/cards-ș.txt':'ș ț â î','payload/data.bin':'A'.repeat(4096)+'B','payload/readonly.bin':'read-only','release/index.json':'{}','release/payload.bin':'release payload'};
 for(const[name,bytes]of Object.entries(expected))assert.equal(seen.get(name),createHash('sha256').update(bytes).digest('hex'),name);
 assert.equal(result.files.length,11);assert.equal(seen.has('.git'),false);assert.equal(result.attributesPreserved,true);
 assert.deepEqual(result.timing.map(row=>[row.stage,row.status]),['snapshot.inventory','snapshot.copy','snapshot.verify','snapshot.artifact-preflight'].map(name=>[name,'PASS']));
 console.log('Snapshot I/O parity observations: '+JSON.stringify(result));
});

test('does not stage unrelated release payloads while retaining their complete source seal',async t=>{
 const result=await exercise(t,false,false);
 assert.equal(result.stagedReleaseCount,0);assert.equal(result.releaseCount,2);
 assert.equal(result.allLiveReleaseInputsStillSealed,true);assert.equal(result.changedReleaseRefused,true);
 assert.equal(result.driftReceiptObservations,4);
 assert.deepEqual(result.timing.map(row=>[row.stage,row.status]),
   ['snapshot.inventory','snapshot.copy','snapshot.verify','snapshot.artifact-preflight'].map(name=>[name,'PASS']));
 console.log('Snapshot release separation observations: '+JSON.stringify(result));
});
test('native snapshot I/O rejects same-metadata drift corruption private paths and links',async t=>{
 const result=await exercise(t,true);assert.equal(result.observations.length,11);
 console.log('Snapshot I/O rejection observations: '+JSON.stringify(result));
});
