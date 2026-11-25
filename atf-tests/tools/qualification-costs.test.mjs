import {runFixtureProcess} from './fixture-process.mjs';
import assert from 'node:assert/strict';
import test from 'node:test';
import fs from 'node:fs/promises';
import os from 'node:os';
import path from 'node:path';
import {createHash} from 'node:crypto';

async function exercise(t, selection) {
  const parent = await fs.realpath(process.env.WHIST_QUALIFICATION_TEMP_ROOT ?? os.tmpdir());
  const root = await fs.mkdtemp(path.join(parent, 'whist-qualification-cost-'));
  t.after(async () => {
    assert.equal(path.dirname(await fs.realpath(root)), parent);
    await fs.rm(root, {recursive: true, force: true});
  });
  const script = path.join(root, 'exercise.ps1');
  await fs.writeFile(script, String.raw`
param($Root,$Scripts,$Selection)
$ErrorActionPreference='Stop'
. (Join-Path $Scripts 'AtfQualificationSnapshot.ps1')
. (Join-Path $Scripts 'AtfQualificationTiming.ps1')
. (Join-Path $Scripts 'AtfComponentMaven.ps1')
. (Join-Path $Scripts 'AtfComponentResume.ps1')
function Put($File,$Text){$null=[IO.Directory]::CreateDirectory([IO.Path]::GetDirectoryName($File));[IO.File]::WriteAllText($File,$Text)}
function Expect($Value,$Message){if(!$Value){throw $Message}}
if($Selection.StartsWith('timing')){
  $file=Join-Path $Root 'timing.json'
  $timing=New-AtfQualificationTiming 'controlled' $file
  $result=Invoke-AtfQualificationPhase $timing 'accepted' {27}
  Expect ($result -eq 27) 'The action result is not replaced by journal output'
  if($Selection -eq 'timing-nominal'){
    $exit=Invoke-AtfQualificationPhase $timing 'native-refusal' {7} -ExitCode
    Expect ($exit -eq 7) 'Original native exit code'
    $cause=[IO.IOException]::new('original command failure');$observed=$null
    try{Invoke-AtfQualificationPhase $timing 'command-failure' {throw $cause}}catch{$observed=$_.Exception}
    Expect ([object]::ReferenceEquals($cause,$observed)) 'Original command exception identity'
    $before=[IO.File]::ReadAllText($file);$caught=$false
    try{New-AtfQualificationTiming 'foreign-owner' $file|Out-Null}catch{$caught=$true}
    Expect ($caught -and [IO.File]::ReadAllText($file) -ceq $before) 'Existing journal cannot be acquired'
    @{journal=($before|ConvertFrom-Json);action=$result;exit=$exit;originalCause=$observed.Message;existingOwnerProtected=$true}|ConvertTo-Json -Depth 10 -Compress
  }else{
    $before=[IO.File]::ReadAllText($file)
    $storage=[IO.IOException]::new('controlled journal replacement refusal')
    $writer={param($destination,$json) [IO.File]::WriteAllText($destination+'.writing',$json);throw $storage}.GetNewClosure()
    $timing.writer=$writer
    $cause=[OperationCanceledException]::new('original cancellation');$observed=$null
    try{Invoke-AtfQualificationPhase $timing 'outer' {Invoke-AtfQualificationPhase $timing 'inner' {throw $cause}}}catch{$observed=$_.Exception}
    Expect ([object]::ReferenceEquals($cause,$observed)) 'Journal failure cannot replace the primary exception'
    $secondary=@($observed.Data['ATF_QUALIFICATION_TIMING_FAILURES'])
    Expect ($secondary.Count -eq 2 -and $secondary[0].phase -eq 'inner' -and $secondary[1].phase -eq 'outer') 'Every nested journal failure keeps its phase'
    Expect ([object]::ReferenceEquals($storage,$secondary[0].exception)) 'Original journal cause identity'
    Expect ([IO.File]::ReadAllText($file) -ceq $before) 'Failed publication cannot replace the previous journal'
    $pending=Get-Content -LiteralPath ($file+'.writing') -Raw|ConvertFrom-Json
    $native=@()
    foreach($code in @(0,7)){
      $caught=$null
      try{Invoke-AtfQualificationPhase $timing ('native-'+$code) {$code} -ExitCode|Out-Null}catch{$caught=$_.Exception}
      Expect ([object]::ReferenceEquals($storage,$caught)) 'A successful action cannot hide publication failure'
      Expect ($caught.Data['ATF_NATIVE_EXIT_CODE'] -eq $code) 'Native acceptance/refusal remains a separate observed fact'
      $native+=@{code=$code;status=$timing.stages[-1].status;cause=$caught.Message}
    }
    $blocked=Join-Path $Root 'blocked-journal'
    $actual=New-AtfQualificationTiming 'actual-filesystem-refusal' $blocked
    $null=[IO.Directory]::CreateDirectory($blocked);$observedActual=$null
    try{Invoke-AtfQualificationPhase $actual 'blocked-destination' {throw $cause}}catch{$observedActual=$_.Exception}
    Expect ([object]::ReferenceEquals($cause,$observedActual)) 'Actual filesystem publication failure cannot mask the command'
    Expect (@($observedActual.Data['ATF_QUALIFICATION_TIMING_FAILURES']).Count -eq 3) 'Actual I/O refusal is retained beside the two controlled failures'
    @{primary=$observed.Message;secondary=@($secondary|ForEach-Object{@{phase=$_.phase;cause=$_.exception.Message}})
      pending=$pending;previousJournalPreserved=$true;actualFilesystemFailurePreservesPrimary=$true;native=$native}|ConvertTo-Json -Depth 10 -Compress
  }
}else{
  $work=Join-Path $Root 'compiler';$repository=Join-Path $work 'maven-repository'
  $payload=@{'maven-repository/x/data.jar'='jar bytes';'maven-repository/x/data.pom'='pom bytes'
    'project/demo/target/classes/A.class'='bytecode';'project/demo/target/test-classes/T.class'='test'
    'project/demo/target/classes/README.md'='packaged resource'}
  foreach($entry in $payload.GetEnumerator()){Put (Join-Path $work $entry.Key) $entry.Value}
  Put (Join-Path $repository 'metadata.txt') 'excluded cache metadata'
  $cost=$null;$timing=New-AtfQualificationTiming 'seal' (Join-Path $Root 'seal-timing.json')
  $fingerprint=Invoke-AtfQualificationPhase $timing 'seal' {Get-AtfComponentMavenFingerprint $work $repository @('demo') -Measurement ([ref]$cost)}
  Expect ($null -ne $cost -and $cost.fingerprint -ceq $fingerprint) 'Measured seal returns one unchanged fingerprint'
  Expect ((Get-AtfComponentMavenFingerprint $work $repository @('demo')) -ceq $fingerprint) 'Measurement is not part of byte identity'
  $classpath=Join-Path $work 'component-classpath.txt';Put $classpath (Join-Path $repository 'x/data.jar')
  $receipt=@{schemaVersion=1;identity='controlled-identity';annotation='Unicode: ș♠🙂';repo='maven-repository';buildFingerprint=$fingerprint
    classpathSha256=(Get-FileHash -LiteralPath $classpath).Hash}
  $receiptPath=Join-Path $work 'component-maven.json';$writeCost=$null
  $expected=($receipt|ConvertTo-Json -Depth 12)+[Environment]::NewLine
  Write-AtfComponentJson $receiptPath $receipt -Measurement ([ref]$writeCost)
  Expect ([IO.File]::ReadAllText($receiptPath) -ceq $expected) 'Existing receipt serialization remains byte-identical'
  Expect ($writeCost.bytes -eq [IO.File]::ReadAllBytes($receiptPath).Length) 'Publication byte count uses actual UTF-8 bytes'
  Expect ($null -ne (Read-AtfPreparedComponentMaven $work 'controlled-identity' @('demo'))) 'The actual receipt remains admissible'
  if($Selection -eq 'seal-negative'){
    $refused=@();$file=Join-Path $work 'project/demo/target/classes/A.class';$time=[IO.File]::GetLastWriteTimeUtc($file)
    foreach($defect in @('same-metadata','missing','foreign-output','wrong-identity')){
      switch($defect){
        'same-metadata' {Put $file 'BYTecode';[IO.File]::SetLastWriteTimeUtc($file,$time)}
        'missing' {[IO.File]::Delete($file)}
        'foreign-output' {Put (Join-Path $work 'project/demo/target/classes/Foreign.class') 'foreign'}
      }
      $identity=if($defect -eq 'wrong-identity'){'foreign'}else{'controlled-identity'}
      Expect ($null -eq (Read-AtfPreparedComponentMaven $work $identity @('demo'))) ('Admitted corrupted seal: '+$defect)
      Put $file 'bytecode';[IO.File]::SetLastWriteTimeUtc($file,$time)
      [IO.File]::Delete((Join-Path $work 'project/demo/target/classes/Foreign.class'))
      $refused+=$defect
    }
    Expect ($null -ne (Read-AtfPreparedComponentMaven $work 'controlled-identity' @('demo'))) 'Restored bytes remain independently admissible'
    $blocked=Join-Path $Root 'blocked-receipt';$null=[IO.Directory]::CreateDirectory($blocked);$caught=$false
    try{Write-AtfComponentJson $blocked $receipt -Measurement ([ref]$writeCost)}catch{$caught=$true}
    Expect ($caught -and $null -eq $writeCost -and [IO.Directory]::Exists($blocked)) 'Failed publication cannot retain a successful cost observation'
    $failedCost=$cost;$caught=$false
    try{Get-AtfComponentMavenFingerprint (Join-Path $Root 'empty') (Join-Path $Root 'empty/cache') @() -Measurement ([ref]$failedCost)|Out-Null}catch{$caught=$_.Exception.Message -eq 'COMPONENT_MAVEN_OUTPUTS_MISSING'}
    Expect ($caught -and $null -eq $failedCost) 'An empty seal cannot retain previous measurements'
    @{refused=$refused;restored=$true;costCannotQualify=$cost.qualified;fingerprint=$fingerprint;failedCostsCleared=$true}|ConvertTo-Json -Compress
  }else{
    @{cost=$cost;receiptCost=$writeCost;fingerprint=$fingerprint;receiptBytesPreserved=$true;payload=$payload}|ConvertTo-Json -Depth 10 -Compress
  }
}
`);
  return JSON.parse(runFixtureProcess('pwsh', ['-NoProfile', '-NonInteractive', '-File', script,
    root, path.resolve(import.meta.dirname, '../../scripts'), selection],
  {fixture: selection, timeout: 60000}));
}

test('timing keeps action outcomes and protects an existing journal', async t => {
  const value = await exercise(t, 'timing-nominal');
  assert.equal(value.action, 27); assert.equal(value.exit, 7);
  assert.deepEqual(value.journal.stages.map(row => [row.stage, row.status]),
    [['accepted', 'PASS'], ['native-refusal', 'FAIL'], ['command-failure', 'FAIL']]);
  assert.ok(value.journal.stages.every(row => row.elapsedMs >= 0));
  console.log('Timing nominal observations: ' + JSON.stringify(value));
});
test('timing retains primary and nested publication failures without inventing success', async t => {
  const value = await exercise(t, 'timing-negative');
  assert.equal(value.primary, 'original cancellation');
  assert.deepEqual(value.secondary.map(row => row.phase), ['inner', 'outer']);
  assert.deepEqual(value.pending.stages.map(row => row.status), ['PASS', 'FAIL', 'FAIL']);
  assert.equal(value.previousJournalPreserved, true);
  assert.equal(value.actualFilesystemFailurePreservesPrimary, true);
  assert.deepEqual(value.native.map(row => [row.code, row.status]), [[0, 'PASS'], [7, 'FAIL']]);
  console.log('Timing failure observations: ' + JSON.stringify(value));
});
test('seal measurements preserve independent fingerprints and exact receipt bytes', async t => {
  const value = await exercise(t, 'seal-nominal');
  const digest = bytes => createHash('sha256').update(bytes).digest('hex');
  const expected = digest(Object.entries(value.payload).map(([file, bytes]) => file + ':' + digest(bytes)).sort().join('\n'));
  assert.equal(value.fingerprint, expected); assert.equal(value.receiptBytesPreserved, true);
  assert.equal(value.cost.hashedFiles, 5);
  assert.equal(value.cost.observedBytes, Object.values(value.payload).reduce((sum, bytes) => sum + Buffer.byteLength(bytes), 0));
  const parts = ['traversalTicks', 'hashTicks', 'identityTicks', 'coordinationTicks'];
  for (const part of parts) assert.ok(value.cost[part] >= 0, part);
  assert.equal(parts.reduce((sum, part) => sum + value.cost[part], 0), value.cost.totalTicks);
  assert.equal(value.receiptCost.serializationTicks + value.receiptCost.publicationTicks, value.receiptCost.totalTicks);
  assert.equal(value.cost.qualified, false); assert.equal(value.receiptCost.qualified, false);
  console.log('Seal cost observations: ' + JSON.stringify(value));
});
test('measured seals still reject changed missing foreign output and identity', async t => {
  const value = await exercise(t, 'seal-negative');
  assert.deepEqual(value.refused, ['same-metadata', 'missing', 'foreign-output', 'wrong-identity']);
  assert.equal(value.restored, true); assert.equal(value.costCannotQualify, false);
  assert.equal(value.failedCostsCleared, true);
  console.log('Seal rejection observations: ' + JSON.stringify(value));
});
