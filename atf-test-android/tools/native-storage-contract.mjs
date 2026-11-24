import assert from 'node:assert/strict';
import {isDeepStrictEqual} from 'node:util';
import fs from 'node:fs';
import {fileURLToPath} from 'node:url';

export function admitStoragePlan(plan) {
 assert.equal(plan?.schemaVersion,1,'NATIVE_STORAGE_SCHEMA');
 assert.equal(plan.applicationId,'ro.whistlab.app','NATIVE_STORAGE_PACKAGE');
 assert.equal(plan.instrumentation,'ro.whistlab.app.test/ro.whistlab.app.persistence.nativecomponent.StorageRecoveryInstrumentation','NATIVE_STORAGE_RUNNER');
 assert.deepEqual(plan.phases?.map(p=>p.id),['prepare','recover','corrupt-xml','verify-xml'],'NATIVE_STORAGE_PHASE_ORDER');
 const ids=new Set();
 for(const phase of plan.phases){assert.ok(Array.isArray(phase.cases)&&phase.cases.length,'NATIVE_STORAGE_CASES');
  for(const id of phase.cases){assert.ok(typeof id==='string'&&/^[a-z][a-z0-9-]*$/.test(id)&&!ids.has(id),'NATIVE_STORAGE_CASE_ID');ids.add(id);}}
 return plan;
}
export function storageReceipt(plan,phaseId,raw){
 admitStoragePlan(plan);const phase=plan.phases.find(p=>p.id===phaseId);assert.ok(phase,'NATIVE_STORAGE_UNKNOWN_PHASE');
 assert.equal(raw?.schemaVersion,1,'NATIVE_STORAGE_SCHEMA');assert.equal(raw.phase,phaseId,'NATIVE_STORAGE_FOREIGN_PHASE');
 assert.ok(!raw.executionError,'NATIVE_STORAGE_EXECUTION_FAILURE');
 assert.deepEqual(raw.cases?.map(c=>c.id),phase.cases,'NATIVE_STORAGE_CASE_INVENTORY');
 let failed=0;
 for(const row of raw.cases){assert.ok(typeof row.name==='string'&&row.name.trim(),'NATIVE_STORAGE_CASE_NAME');
  assert.ok(Object.hasOwn(row,'expected')&&Object.hasOwn(row,'observed')&&typeof row.passed==='boolean','NATIVE_STORAGE_COMPARISON');
  const passed=isDeepStrictEqual(row.expected,row.observed);assert.equal(row.passed,passed,'NATIVE_STORAGE_VERDICT');if(!passed)failed++;}
 assert.equal(raw.failures,failed,'NATIVE_STORAGE_FAILURE_COUNT');
 return raw.cases.map(row=>({id:'NATIVE-STORAGE-'+row.id,name:row.name,layer:'android-component',
  status:row.passed?'PASS':'FAIL',expected:row.expected,observed:row.observed,images:[]}));
}
if(process.argv[1]&&fileURLToPath(import.meta.url)===process.argv[1]){
 const [verb,planFile,input,phase,output]=process.argv.slice(2);const plan=admitStoragePlan(JSON.parse(fs.readFileSync(planFile,'utf8')));
 if(verb==='plan')console.log(JSON.stringify(plan));
 else if(verb==='verify'){
  const checks=storageReceipt(plan,phase,JSON.parse(fs.readFileSync(input,'utf8')));
  fs.writeFileSync(output,JSON.stringify(checks,null,2)+'\n',{flag:'wx'});
  if(checks.some(c=>c.status!=='PASS'))process.exitCode=1;
 }else throw Error('Use plan <plan-json> or verify <plan-json> <receipt-json> <phase> <new-output-json>');
}
