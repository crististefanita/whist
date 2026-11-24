import assert from 'node:assert/strict';
import fs from 'node:fs';
import os from 'node:os';
import path from 'node:path';
import test from 'node:test';
import {fileURLToPath} from 'node:url';
import {runFixtureProcess} from '../../atf-tests/tools/fixture-process.mjs';
import {nativeObservationSources} from './native-observation-fixture.mjs';

test('actual Android measurement adapter retains content and rejects clipping or stale owners',t=>{
 const root=fileURLToPath(new URL('../../',import.meta.url));
 const work=fs.mkdtempSync(path.join(os.tmpdir(),'native-observation-'));
 t.after(()=>fs.rmSync(work,{recursive:true,force:true,maxRetries:10,retryDelay:100}));
 const sources=[];
 for(const [name,text] of Object.entries(nativeObservationSources)){
  const file=path.join(work,name);fs.mkdirSync(path.dirname(file),{recursive:true});fs.writeFileSync(file,text);sources.push(file);
 }
 const core=path.join(root,'whist-lab-app/card-table-core');
 const closure=['src/main/java/ro/cardtable/ui/observation/RenderedSurface.java',
  'src/main/java/ro/cardtable/ui/observation/SurfaceEpoch.java',
  'src/testFixtures/java/ro/cardtable/testing/RenderedSurfaceAudit.java',
  'src/testFixtures/java/ro/cardtable/testing/SurfaceContinuity.java',
  'src/testFixtures/java/ro/cardtable/testing/SurfaceObservation.java'].map(name=>path.join(core,name));
 const adapter=path.join(root,'whist-lab-app/atf-card-table-ui/src/main/java/ro/cardtable/ui/observation/AndroidSurfaceObservation.java');
 const argFile=path.join(work,'compile.args');
 fs.writeFileSync(argFile,['-proc:none','-encoding','UTF-8','--release','17','-d',work,...closure,adapter,...sources]
  .map(value=>'"'+value.replaceAll('\\','/')+'"').join('\n'));
 runFixtureProcess('javac',['-J-Xmx128m','@'+argFile],{cwd:root,fixture:'Native observation adapter compilation'});
 for(const mode of ['nominal','negative']){
  const actual=JSON.parse(runFixtureProcess('java',['-Xmx128m','-cp',work,'NativeObservationProbe',mode],
   {cwd:root,fixture:'Native measured view '+mode}));
  assert.equal(actual.status,'PASS');assert.equal(actual.regions,4);assert.equal(actual.rejected,mode==='negative'?11:0);
  console.log('Native observation adapter: '+JSON.stringify({precondition:'Controlled Android View/Layout responses; real production adapter and neutral oracle',mode,...actual}));
 }
});
