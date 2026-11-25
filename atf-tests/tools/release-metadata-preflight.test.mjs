import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs/promises';
import path from 'node:path';
import {inspectReleaseMetadata,verifyMetadataSnapshot} from './release-metadata-preflight.mjs';
import {androidFlows,e2ePaths,signedE2eProtocol} from './android-e2e-contract.mjs';

async function fixture(t) {
  const parent=process.env.WHIST_QUALIFICATION_TEMP_ROOT??process.env.WHIST_FIXTURE_DIAGNOSTICS;
  assert.ok(parent&&path.isAbsolute(parent),'An external fixture parent is required');
  const root=await fs.mkdtemp(path.join(parent,'metadata-'));
  t.after(()=>fs.rm(root,{recursive:true,force:true}));
  const write=async(file,value)=>{await fs.mkdir(path.dirname(path.join(root,file)),{recursive:true});await fs.writeFile(path.join(root,file),typeof value==='string'?value:JSON.stringify(value));};
  const groups=Object.fromEntries(Object.keys(androidFlows).map(name=>[name,{protocol:signedE2eProtocol,tests:1,failures:0,errors:0,skipped:0,...e2ePaths(name)}]));
  const manifest={release:{versionName:'0.25.0'},qualification:{e2e:{status:'PASS',groups:Object.keys(groups),executionProfile:{api:28,displayProfile:'1080x1920@440',responsiveProfiles:'1080x1920@440'}},e2eGroups:groups,
    auditedGroups:{'failed-primary':{failures:1,report:'absent-primary.html'}}}};
  const catalog={schemaVersion:2,currentVersion:'0.25.0',versions:['0.25.0']};
  for(const file of ['release/README.md','release/0.25.0/README.md','release/0.25.0/reports/README.md'])await write(file,'# Fixture\n\n## Overview\n\nControlled metadata.\n');
  const save=async()=>{await write('release/index.json',catalog);await write('release/0.25.0/release-validation.json',manifest);};
  await save();return{root,manifest,catalog,save,write};
}

test('metadata admission reads declared identity without reading artifacts or qualifying a failed primary group',async t=>{
  const f=await fixture(t);const original=JSON.stringify(f.manifest);
  const observed=await inspectReleaseMetadata(f.root);
  assert.equal(observed.status,'PASS');assert.equal(observed.qualification,false);
  assert.equal(observed.e2eGroups.length,7);assert.equal(observed.inputs.length,3);
  assert.equal(JSON.stringify(f.manifest),original);
  assert.equal(f.manifest.qualification.auditedGroups['failed-primary'].failures,1);
  assert.deepEqual(observed.editorialFiles,['release/README.md']);
  // All named HTML and execution files are absent: byte qualification must remain a later gate.
  await assert.rejects(fs.access(path.join(f.root,e2ePaths('signed-release-smoke').report)));
  console.log('metadata-only; groups=7; primary-failure=preserved; artifacts=unread');
  assert.deepEqual(verifyMetadataSnapshot(observed,await inspectReleaseMetadata(f.root)),observed);
  await f.write('release/README.md','# Changed but structurally readable\n');
  assert.throws(()=>verifyMetadataSnapshot(observed,{...observed,inputs:[]}),/SNAPSHOT-CHANGED/);
  const changed=await inspectReleaseMetadata(f.root);
  assert.throws(()=>verifyMetadataSnapshot(observed,changed),/SNAPSHOT-CHANGED/);
});

test('metadata rejections cover inventory aliasing unsupported decisions missing links and foreign identities',async t=>{
  let cases=0;
  for(const defect of ['duplicate-version','wrong-version','missing-decision','unselected','duplicate-selection','unknown-group','aliased-report','wrong-receipt','failed-e2e','missing-guide','no-rationale']){
    const f=await fixture(t);const q=f.manifest.qualification;const first=Object.keys(q.e2eGroups)[0];
    if(defect==='duplicate-version')f.catalog.versions.push('0.25.0');
    if(defect==='wrong-version')f.manifest.release.versionName='0.24.2';
    if(defect==='missing-decision')delete q.e2e;
    if(defect==='unselected')q.e2e.groups.pop();
    if(defect==='duplicate-selection')q.e2e.groups.push(first);
    if(defect==='unknown-group'){q.e2eGroups.unknown=q.e2eGroups[first];delete q.e2eGroups[first];q.e2e.groups=q.e2e.groups.map(name=>name===first?'unknown':name);}
    if(defect==='aliased-report')q.e2eGroups[first].report='reports/e2e/another/report.html';
    if(defect==='wrong-receipt')q.e2eGroups[first].executionReceipt='../foreign.json';
    if(defect==='failed-e2e')q.e2eGroups[first].failures=1;
    if(defect==='no-rationale'){q.e2e={status:'NOT_RUN'};q.e2eGroups={};}
    await f.save();if(defect==='missing-guide')await fs.rm(path.join(f.root,'release/0.25.0/README.md'));
    let observed;
    await assert.rejects(inspectReleaseMetadata(f.root),error=>{observed=error.code??error.message;return true;},defect);
    console.log(JSON.stringify({defect,expected:'Rejected before collection',observed}));cases++;
  }
  console.log('metadata rejection cases='+cases);
});
