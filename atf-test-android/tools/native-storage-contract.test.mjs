import assert from 'node:assert/strict';
import test from 'node:test';
import fs from 'node:fs';
import {admitStoragePlan,storageReceipt} from './native-storage-contract.mjs';
const plan=JSON.parse(fs.readFileSync(new URL('../../config/native-storage-components.json',import.meta.url),'utf8'));
const receipt=()=>({schemaVersion:1,phase:'prepare',failures:0,cases:plan.phases[0].cases.map(id=>({id,name:'Declared '+id,expected:'committed',observed:'committed',passed:true}))});
test('native storage phases preserve every original value and distinguish native components from UI cases',()=>{
 const raw=receipt();const checks=storageReceipt(plan,'prepare',raw);
 assert.equal(checks.length,2);assert.ok(checks.every(c=>c.layer==='android-component'&&c.images.length===0));
 assert.equal(checks[1].observed,raw.cases[1].observed);
});
test('lost duplicate or foreign native cases and contradictory outcomes cannot qualify',()=>{
 for(const change of [r=>r.cases.pop(),r=>r.cases.push(r.cases[0]),r=>r.phase='recover',r=>r.executionError='original IO failure',
  r=>r.cases[0].observed='lost',r=>r.cases[0].passed=false,r=>r.failures=1]){
   const raw=receipt();change(raw);assert.throws(()=>storageReceipt(plan,'prepare',raw),/NATIVE_STORAGE_/);}
 const failed=receipt();failed.cases[0].observed='lost';failed.cases[0].passed=false;failed.failures=1;
 assert.equal(storageReceipt(plan,'prepare',failed)[0].status,'FAIL');
});
test('native storage admission rejects reordered phases and duplicate case identities before a device action',()=>{
 const changed=structuredClone(plan);changed.phases.reverse();assert.throws(()=>admitStoragePlan(changed),/NATIVE_STORAGE_PHASE_ORDER/);
 const duplicate=structuredClone(plan);duplicate.phases[1].cases.push(duplicate.phases[0].cases[0]);assert.throws(()=>admitStoragePlan(duplicate),/NATIVE_STORAGE_CASE_ID/);
});
