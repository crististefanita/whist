import assert from 'node:assert/strict';
import fs from 'node:fs';
import os from 'node:os';
import path from 'node:path';
import test from 'node:test';
import {boundedStream,fixtureProfile,runFixtureProcess} from './fixture-process.mjs';

function directory(t){const root=fs.mkdtempSync(path.join(process.env.WHIST_QUALIFICATION_TEMP_ROOT??os.tmpdir(),'whist-process-diagnostics-'));
  t.after(()=>fs.rmSync(root,{recursive:true,force:true}));return root;}

test('pure stream matrices retain bounded tails and declared resource limits',()=>{
  for(const size of [0,1,65535,65536,65537,1048576]){
    const actual=boundedStream(Buffer.alloc(size,120));assert.equal(actual.bytes,size);
    assert.equal(Buffer.byteLength(actual.tail),Math.min(size,65536));assert.equal(actual.truncated,size>65536);
  }
  assert.equal(fixtureProfile.timeoutMs,90000);assert.equal(fixtureProfile.concurrentChildren,1);
  assert.equal(fixtureProfile.jvmHeapMiB,768);
  const jvm=fs.readFileSync(new URL('../../scripts/AtfComponentJvm.ps1',import.meta.url),'utf8');
  assert.ok(jvm.includes('-Xmx'+fixtureProfile.jvmHeapMiB+'m'));
  for(const limit of [1,2,3,7])assert.ok(Buffer.byteLength(boundedStream('😀é'.repeat(10),limit).tail)<=limit);
});
test('mocked timeout and exit failures retain the original cause before fixture cleanup',t=>{
  const root=directory(t);
  for(const code of ['ETIMEDOUT','ENOENT','ERR_CHILD_PROCESS_STDIO_MAXBUFFER','EXIT']){
    const cause=Object.assign(new Error('controlled original failure'),{code,status:7,stdout:'captured prefix',stderr:'captured cause'});
    assert.throws(()=>runFixtureProcess('fixture',[],{diagnosticsRoot:root},()=>{throw cause;}),error=>error===cause);
    const actual=JSON.parse(fs.readFileSync(cause.fixtureDiagnostic));
    assert.equal(actual.status,code==='ETIMEDOUT'?'TIMEOUT':'FAIL');assert.equal(actual.stderr.tail,'captured cause');
    assert.equal(actual.stdout.tail,'captured prefix');assert.equal(actual.cause.code,code);assert.ok(actual.elapsedMs>=0);
  }
});
test('real interrupted child keeps both streams after the fixture directory is removed',t=>{
  const root=directory(t),fixture=path.join(root,'fixture');fs.mkdirSync(fixture);
  const source=path.join(fixture,'child.mjs');fs.writeFileSync(source,"process.stdout.write('prefix-out');process.stderr.write('prefix-error');setInterval(()=>{},10);");
  let cause;
  try{runFixtureProcess(process.execPath,[source],{diagnosticsRoot:root,timeout:2000,fixture:'interrupted-child'});}
  catch(error){cause=error;}
  assert.equal(cause?.code,'ETIMEDOUT');fs.rmSync(fixture,{recursive:true});
  const actual=JSON.parse(fs.readFileSync(cause.fixtureDiagnostic));
  assert.equal(actual.status,'TIMEOUT');assert.equal(actual.stdout.tail,'prefix-out');assert.equal(actual.stderr.tail,'prefix-error');
  process.stdout.write(JSON.stringify(actual)+'\n');
});
test('successful child observations retain independent invocation timings and both streams',t=>{
  const root=directory(t);
  for(const mode of ['first-child','second-child','third-child']){
    const output=runFixtureProcess(process.execPath,['-e',`process.stdout.write(${JSON.stringify(mode)});process.stderr.write('observed warning')`],{diagnosticsRoot:root,fixture:mode});
    assert.equal(output,mode);
  }
  const receipts=fs.readdirSync(root).map(file=>JSON.parse(fs.readFileSync(path.join(root,file))));
  assert.equal(receipts.length,3);assert.ok(receipts.every(row=>row.status==='PASS'&&row.elapsedMs>=0));
  assert.ok(receipts.every(row=>row.stderr.tail==='observed warning'));
  process.stdout.write(JSON.stringify({observations:receipts.map(({fixture,elapsedMs})=>({fixture,elapsedMs})),
    limit:'Process observations only; compiler reuse is qualified by the real launcher contracts.'})+'\n');
});
