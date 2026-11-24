import {runFixtureProcess} from './fixture-process.mjs';
import assert from 'node:assert/strict';
import fs from 'node:fs/promises';
import os from 'node:os';
import path from 'node:path';
import test from 'node:test';
const workflow = await fs.readFile(new URL('../../.github/workflows/whist-components.yml',import.meta.url),'utf8');
const label='      - name: Admit source-control plan before Maven';
const start=workflow.indexOf(label), end=workflow.indexOf('      - name: Prepare public Maven dependencies',start);
assert(start>=0 && end>start && end<workflow.indexOf('mvn -B'));
const lines=workflow.slice(start,end).split(/\r?\n/);
const run=lines.slice(lines.findIndex(line=>line.trim()==='run: |')+1)
  .filter(line=>line.startsWith('          ')).map(line=>line.slice(10)).join('\n');

async function exercise(t, rejected) {
  const root=await fs.mkdtemp(path.join(os.tmpdir(),'whist-hosted-admission-'));
  t.after(()=>fs.rm(root,{recursive:true,force:true}));
  await fs.mkdir(path.join(root,'scripts'));
  await fs.mkdir(path.join(root,'wq/tmp'),{recursive:true});
  await fs.writeFile(path.join(root,'pom.xml'),'<project><properties><jackson.version>2.17.2</jackson.version></properties></project>');
  await fs.writeFile(path.join(root,'scripts/AtfTestChangeAdmission.ps1'),String.raw`
function Invoke-AtfTestChangeAdmission {
 param($SourceRoot,$BaseRef,$TempRoot,$JavaHome,$MavenCache,[switch]$GenerateProofs,$ReceiptFile)
 if(!$GenerateProofs -or $SourceRoot -cne $env:GITHUB_WORKSPACE -or $BaseRef -cne ('a'*40)){throw 'Wrong admission contract'}
 if(@(Get-ChildItem -LiteralPath $MavenCache -File -Recurse).Count -ne 6){throw 'Expected only parser bootstrap inputs'}
 [IO.File]::WriteAllText((Join-Path $SourceRoot 'admission.entered'),'plan')
 if($env:ADMISSION_REJECT -eq 'yes'){throw 'TEST-CHANGE-PROOF-REVIEW: controlled rejection'}
 [IO.File]::WriteAllText($ReceiptFile,'{"status":"PASS","mode":"GENERATE"}')
}`);
  const script=String.raw`
$ErrorActionPreference='Stop'
Set-Location -LiteralPath $env:GITHUB_WORKSPACE
function Invoke-WebRequest {
 param($Uri,$OutFile)
 if(!$Uri.StartsWith('https://repo.maven.apache.org/maven2/')){throw 'Unexpected repository'}
 $text=if($Uri.EndsWith('.pom')){'<project><dependencies><dependency><groupId>org.yaml</groupId><artifactId>snakeyaml</artifactId><version>2.2</version></dependency></dependencies></project>'}else{'mock parser binary'}
 [IO.File]::WriteAllText($OutFile,$text)
}
try {
`+run+String.raw`
 [IO.File]::WriteAllText((Join-Path $env:GITHUB_WORKSPACE 'maven.entered'),'next hosted step')
} catch {
 [IO.File]::WriteAllText((Join-Path $env:GITHUB_WORKSPACE 'rejection.txt'),$_.Exception.Message)
 if($env:ADMISSION_REJECT -ne 'yes'){throw}
}`;
  await fs.writeFile(path.join(root,'exercise.ps1'),script);
  runFixtureProcess('pwsh',['-NoProfile','-NonInteractive','-File',path.join(root,'exercise.ps1')],{
    env:{...process.env,GITHUB_WORKSPACE:root,WHIST_PUBLICATION_TEST_ROOT:path.join(root,'wq/tmp'),
      ATF_TEST_CHANGE_BASE_REF:'a'.repeat(40),ADMISSION_REJECT:rejected?'yes':'no'},
    windowsHide:true,encoding:'utf8',timeout:30_000});
  assert.equal(await fs.readFile(path.join(root,'admission.entered'),'utf8'),'plan');
  if(rejected){
    assert.match(await fs.readFile(path.join(root,'rejection.txt'),'utf8'),/TEST-CHANGE-PROOF-REVIEW/);
    await assert.rejects(fs.stat(path.join(root,'maven.entered')),{code:'ENOENT'});
  }else assert.equal(await fs.readFile(path.join(root,'maven.entered'),'utf8'),'next hosted step');
}
test('hosted preparation admits the canonical source-control plan before any Maven step',t=>exercise(t,false));
test('hosted source-control rejection prevents Maven preparation without network access',t=>exercise(t,true));
