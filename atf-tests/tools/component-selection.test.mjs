import assert from 'node:assert/strict';
import fs from 'node:fs/promises';
import os from 'node:os';
import path from 'node:path';
import test from 'node:test';
import {runFixtureProcess} from './fixture-process.mjs';

test('selection is admitted before staging and copies release data only for owners that need it', async t => {
  const parent = await fs.realpath(process.env.WHIST_QUALIFICATION_TEMP_ROOT ?? os.tmpdir());
  const root = await fs.mkdtemp(path.join(parent, 'whist-component-selection-'));
  t.after(async () => {
    assert.equal(path.dirname(await fs.realpath(root)), parent);
    await fs.rm(root, {recursive: true, force: true});
  });
  const script = path.join(root, 'selection.ps1');
  await fs.writeFile(script, String.raw`
param($ProjectRoot,$Scripts,$Fixture)
$ErrorActionPreference='Stop'
. (Join-Path $Scripts 'AtfComponentSelection.ps1')
$inputs=Join-Path $Fixture 'config';$null=[IO.Directory]::CreateDirectory($inputs)
foreach($name in @('atf-test-suites.json','gradle-app-collections.properties','atf-component-collections.json')){
 Copy-Item -LiteralPath (Join-Path $ProjectRoot ('config/'+$name)) -Destination (Join-Path $inputs $name)
}
$empty=Resolve-AtfComponentSelection $Fixture @()
$narrow=Resolve-AtfComponentSelection $Fixture @('whist-lab-app-bidding')
$docs=Resolve-AtfComponentSelection $Fixture @('atf-tests-documentation')
$legacy=Resolve-AtfComponentSelection $Fixture @('atf-tests')
Assert-AtfComponentSelectionSnapshot $Fixture $narrow
$rejections=@()
foreach($requested in @(@('missing-group'),@('whist-lab-app-bidding','whist-lab-app-bidding'))){
 try{Resolve-AtfComponentSelection $Fixture $requested|Out-Null;throw 'Invalid selection accepted'}catch{
  if($_.Exception.Message -notmatch '^COMPONENT-SELECTION-(UNKNOWN|DUPLICATE)$'){throw}
  if(Test-Path -LiteralPath (Join-Path $Fixture 'staging')){throw 'Rejected selection created a staging directory'}
  $rejections+=@($_.Exception.Message)
 }
}
$ownerFile=Join-Path $inputs 'atf-component-collections.json';$owner=Get-Content $ownerFile -Raw|ConvertFrom-Json
($owner.collections|Where-Object group -eq 'atf-tests-documentation').releaseSnapshot=$false
[IO.File]::WriteAllText($ownerFile,($owner|ConvertTo-Json -Depth 8))
$sourceChange=''
try{Assert-AtfComponentSelectionSnapshot $Fixture $narrow}catch{$sourceChange=$_.Exception.Message}
if($sourceChange -notmatch '^COMPONENT-SELECTION-SOURCE-CHANGED:'){throw 'Changed selector inputs escaped their source binding'}
@{defaultGroupCount=$empty.groups.Count;defaultIncludesRelease=$empty.includeReleasePayload
 biddingGroups=$narrow.groups;bidOnlyIncludesRelease=$narrow.includeReleasePayload
 documentationIncludesRelease=$docs.includeReleasePayload;legacyMonolithIncludesRelease=$legacy.includeReleasePayload
 rejected=$rejections;selectorChangeRefused=$true;stagingCreated=$false}|ConvertTo-Json -Depth 5 -Compress
`);
  const output = await runFixtureProcess('pwsh', ['-NoProfile', '-NonInteractive', '-File', script,
    path.resolve(import.meta.dirname, '../../'), path.resolve(import.meta.dirname, '../../scripts'), root],
  {encoding: 'utf8', windowsHide: true, timeout: 30000});
  const result = JSON.parse(output.trim().split(/\r?\n/).at(-1));
  assert.ok(result.defaultGroupCount > 1);
  assert.equal(result.defaultIncludesRelease, true);
  assert.deepEqual(result.biddingGroups, ['whist-lab-app-bidding']);
  assert.equal(result.bidOnlyIncludesRelease, false);
  assert.equal(result.documentationIncludesRelease, true);
  assert.equal(result.legacyMonolithIncludesRelease, true);
  assert.deepEqual(result.rejected, ['COMPONENT-SELECTION-UNKNOWN', 'COMPONENT-SELECTION-DUPLICATE']);
  assert.equal(result.selectorChangeRefused, true);
  assert.equal(result.stagingCreated, false);
  console.log('Component selection admission observations: ' + JSON.stringify(result));
});
