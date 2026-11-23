import assert from 'node:assert/strict';
import {execFileSync} from 'node:child_process';
import fs from 'node:fs/promises';
import os from 'node:os';
import path from 'node:path';
import test from 'node:test';
import {digest, strictJson} from './evaluate-release-rag.mjs';
import {sourceCheckFiles, verifyDocumentationDrift} from './component-source-check.mjs';

test('the real snapshot checker captures rejected observations and admits only live documentation drift', async t => {
  const root = await fs.mkdtemp(path.join(os.tmpdir(), 'whist-source-check-'));
  t.after(() => fs.rm(root, {recursive:true, force:true}));
  const helper = new URL('../../scripts/AtfQualificationSnapshot.ps1', import.meta.url);
  await fs.copyFile(helper, path.join(root, 'AtfQualificationSnapshot.ps1'));
  await fs.copyFile(new URL('../../scripts/AtfFileDigest.ps1', import.meta.url), path.join(root, 'AtfFileDigest.ps1'));
  // The inventory neighbour is controlled; comparison, capture, hashes and release traversal are real.
  await fs.writeFile(path.join(root, 'Get-AtfSourceInputs.ps1'), `param([string]$SourceRoot)
if(Test-Path -LiteralPath (Join-Path $SourceRoot 'reader-error')) { throw 'controlled inventory failure' }
@('README.md','module/src/main/Example.java') | ForEach-Object {
  [pscustomobject]@{path=$_;sha256=(Get-FileHash -LiteralPath (Join-Path $SourceRoot $_)).Hash.ToLowerInvariant()}
}`);
  const script = path.join(root, 'observe.ps1');
  await fs.writeFile(script, `param([string]$Root)
$ErrorActionPreference='Stop'
. (Join-Path $PSScriptRoot 'AtfQualificationSnapshot.ps1')
$rows=@(foreach($mode in @('stable','documentation','staged','code','release','reader-error')) {
  $work=Join-Path $Root $mode
  $stage=Join-Path $work 'stage';$live=Join-Path $work 'live';$capture=Join-Path $work 'capture'
  foreach($dir in @($stage,$live)) {
    New-Item -ItemType Directory -Path (Join-Path $dir 'module/src/main'),(Join-Path $dir 'release') -Force | Out-Null
    [IO.File]::WriteAllText((Join-Path $dir 'README.md'),'Original guide')
    [IO.File]::WriteAllText((Join-Path $dir 'module/src/main/Example.java'),'class Example {}')
    [IO.File]::WriteAllText((Join-Path $dir 'release/index.json'),'{}')
  }
  New-Item -ItemType Directory -Path $capture | Out-Null
  $snapshot=@{source=(ConvertTo-AtfInputJson @(& (Join-Path $PSScriptRoot 'Get-AtfSourceInputs.ps1') -SourceRoot $stage));release=(ConvertTo-AtfInputJson @(Get-AtfReleaseInputs $stage))}
  [IO.File]::WriteAllText((Join-Path $work 'source-inputs.json'),$snapshot.source)
  [IO.File]::WriteAllText((Join-Path $work 'release-inputs.json'),$snapshot.release)
  switch($mode) {
    'documentation' {[IO.File]::WriteAllText((Join-Path $live 'README.md'),'Revised guide')}
    'staged' {[IO.File]::WriteAllText((Join-Path $stage 'module/src/main/Example.java'),'class Changed {}')}
    'code' {[IO.File]::WriteAllText((Join-Path $live 'module/src/main/Example.java'),'class Changed {}')}
    'release' {[IO.File]::WriteAllText((Join-Path $live 'release/index.json'),'changed')}
    'reader-error' {[IO.File]::WriteAllText((Join-Path $live 'reader-error'),'controlled')}
  }
  $differences=@(Get-AtfSnapshotDifferences $snapshot $stage $live -CaptureRoot $capture)
  [ordered]@{mode=$mode;differences=$differences;captured=(Test-Path -LiteralPath (Join-Path $capture 'source-snapshot-check.json'))}
})
ConvertTo-Json -InputObject $rows -Depth 6 -Compress`);
  const output = execFileSync('pwsh', ['-NoProfile','-NonInteractive','-File',script,root], {encoding:'utf8',timeout:45000});
  const rows = JSON.parse(output.trim());
  assert.equal(rows.length,6);
  for (const row of rows) {
    assert.equal(row.captured,!['stable','reader-error'].includes(row.mode),row.mode);
    if (!row.captured) {
      assert.equal(row.differences.length,row.mode==='stable'?0:1);
      assert.deepEqual(await fs.readdir(path.join(root,row.mode,'capture')),[]);
      continue;
    }
    const work=path.join(root,row.mode);
    const inputs=await strictJson(path.join(work,'source-inputs.json'));
    const release=await strictJson(path.join(work,'release-inputs.json'));
    const documents=await Promise.all(sourceCheckFiles.map(file=>strictJson(path.join(work,'capture',file))));
    const run={sourceStable:false,status:'FAIL',sourceCheckReceipt:sourceCheckFiles[0],
      sourceCheckReceiptSha256:documents[0].sha256,snapshotDifferences:row.differences,
      releaseInputManifest:'release-inputs.json',releaseFingerprint:release.sha256};
    const check=()=>verifyDocumentationDrift(inputs,run,{value:run},documents);
    if(row.mode==='documentation') {
      assert.deepEqual(check().observedDocumentationChanges,['README.md']);
      assert.equal(documents[0].value.expectedSourceSha256,digest(inputs.bytes));
    } else assert.throws(check,/COMPONENT_SOURCE_DRIFT_/);
  }
  const launcher=await fs.readFile(new URL('../../scripts/run-component-suites.ps1',import.meta.url),'utf8');
  assert.match(launcher,/Get-AtfSnapshotDifferences[^\r\n]+-CaptureRoot \$RunRoot/);
  assert.match(launcher,/\$run\.sourceCheckReceiptSha256\s*=\s*\(Get-FileHash/);
});
