import assert from 'node:assert/strict';
import fs from 'node:fs/promises';
import path from 'node:path';
import {test} from 'node:test';
import {digest} from './evaluate-release-rag.mjs';
import {admitWebOnlyRuntimeChanges,admitVerificationRuntimeChanges,e2eSourcePath} from './android-e2e-provenance.mjs';
import {planAndroidE2e} from './prepare-android-e2e.mjs';
import {nativeFixture,flow,request,gameFile,setSources} from './android-e2e-provenance-fixture.mjs';
import {e2ePaths} from './android-e2e-contract.mjs';

const native={path:gameFile,sha256:digest('native')};
const web={path:'atf-ui-browser/src/main/web/table.mjs',sha256:digest('old Web')};
async function androidBuildClosure(read) {
  const settings = await read('settings.gradle'), aliases = new Map();
  for (const match of settings.matchAll(/project\(["'](:[\w-]+)["']\)\.projectDir\s*=\s*file\(["']([^"']+)["']\)/g))
    aliases.set(match[1], match[2]);
  const visited = new Set();
  async function visit(module) {
    if (visited.has(module)) return;
    visited.add(module);
    const directory = aliases.get(module) ?? module.slice(1);
    assert.doesNotMatch(directory, /(?:atf-ui-browser|card-table-web)/, 'Web cannot enter the APK build closure');
    const build = await read(directory + '/build.gradle');
    assert.doesNotMatch(build, /ro\.cardtable\.browser/, module);
    for (const line of build.split(/\r?\n/)) {
      if (!/^\s*(?:api|implementation|compileOnly|runtimeOnly|releaseImplementation)\b/.test(line) || !line.includes('project')) continue;
      const edge = /^\s*(?:api|implementation|compileOnly|runtimeOnly|releaseImplementation)\s+project\(["'](:[\w-]+)["']\)\s*$/.exec(line);
      assert.ok(edge, 'Unsupported production project edge: ' + line);
      await visit(edge[1]);
    }
  }
  await visit(':app');
  return [...visited].sort();
}

test('the browser module remains outside the declared Android build graph',async()=>{
  const modules = await androidBuildClosure(file => fs.readFile(new URL('../../whist-lab-app/'+file,import.meta.url),'utf8'));
  assert.deepEqual(modules, [':app', ':card-table-android', ':card-table-core', ':card-table-names', ':game-core', ':whist-presentation']);
});

test('an unused Web declaration is allowed but transitive and dynamic production edges fail closed',async()=>{
  const fixture = new Map([
    ['settings.gradle', 'include(":app", ":shared", ":card-table-web")\nproject(":card-table-web").projectDir = file("../atf-ui-browser")'],
    ['app/build.gradle', 'implementation project(":shared")\ntestImplementation project(":card-table-web")'],
    ['shared/build.gradle', '']
  ]);
  const read = async file => { assert.ok(fixture.has(file), 'Unknown module: ' + file); return fixture.get(file); };
  assert.deepEqual(await androidBuildClosure(read), [':app', ':shared']);
  for (const edge of ['api project(":card-table-web")', 'implementation project(selected)', 'api project(":missing")']) {
    fixture.set('shared/build.gradle', edge);
    await assert.rejects(androidBuildClosure(read), /Web cannot enter|Unsupported production|Unknown module/);
  }
  fixture.set('shared/build.gradle', 'api project(":app")');
  assert.deepEqual(await androidBuildClosure(read), [':app', ':shared'], 'Cycles cannot hide another dependency or loop indefinitely');
});

for(const change of ['add','modify','remove'])test('Web-only '+change+' preserves the Android runtime fingerprint',()=>{
  const original=change==='add'?[native]:[native,web];
  const current=change==='remove'?[native]:[native,{...web,sha256:digest('new Web')}];
  const admission=admitWebOnlyRuntimeChanges(original,current);
  assert.equal(admission.kind,'UNCHANGED_ANDROID_RUNTIME_WITH_WEB_ONLY_INPUTS');
  assert.deepEqual(admission.changedInputs,[web.path]);
  assert.equal(admission.androidRuntimeFingerprint,admitWebOnlyRuntimeChanges([native],[native,web]).androidRuntimeFingerprint);
});

for(const file of ['whist-lab-app/atf-card-table-ui/lib/table.aar','whist-lab-app/app/build.gradle',
  'config/whist-hard-policy.yaml','atf-ui-browser-shadow/config.json'])test('a Web change cannot conceal native or unknown input '+file,()=>{
  assert.throws(()=>admitWebOnlyRuntimeChanges([native],[native,web,{path:file,sha256:digest('native change')}]),/ANDROID_E2E_RUNTIME_CHANGED/);
});

test('a browser-only inventory cannot impersonate an Android runtime',()=>{
  assert.throws(()=>admitWebOnlyRuntimeChanges([web],[{...web,sha256:digest('new')}]),/ANDROID_E2E_RUNTIME_CHANGED/);
});

test('browser package rotation preserves native reports and their original source identities',async t=>{
  const oldJar='atf-ui-browser/lib/atf-ui-browser-0.4.jar',nextJar='atf-ui-browser/lib/atf-ui-browser-0.5.jar';
  const f=await nativeFixture(t,{extraInputs:[{path:oldJar,sha256:digest('old package')}]});
  const original=await fs.readFile(path.join(f.candidate,e2eSourcePath(flow)));
  await setSources(f.candidate,f.current,f.files.filter(row=>row.path!==oldJar).concat({path:nextJar,sha256:digest('new package')}));
  const [row]=await planAndroidE2e(f.candidate,f.current,request);
  assert.equal(row.decision,'REUSE',row.reason);
  assert.deepEqual(row.provenance.runtimeAdmission.changedInputs,[oldJar,nextJar]);
  assert.equal(row.provenance.executionSourceCommit,f.manifest.source.sourceCommit);
  assert.deepEqual(await fs.readFile(path.join(f.candidate,e2eSourcePath(flow))),original);
  console.log('Web-only source admission: '+JSON.stringify(row.provenance.runtimeAdmission));
});

test('verification maintenance reuses original native evidence without changing its commit or bytes',async t=>{
  const verification = {path:'atf-tests/src/test/java/atf/tests/GovernanceTest.java',sha256:digest('old test')};
  const seals = {path:'config/atf-component-dependencies.json',sha256:digest('old reader seals')};
  const f=await nativeFixture(t,{extraInputs:[verification,seals,web]});
  const receiptFile=e2eSourcePath(flow),original=await fs.readFile(path.join(f.candidate,receiptFile));
  const report=path.join(f.candidate,e2ePaths(flow).report), originalReport=await fs.readFile(report);
  const next=f.files.map(row=>[verification.path,seals.path,web.path].includes(row.path)?{...row,sha256:digest('reviewed '+row.path)}:row);
  await setSources(f.candidate,f.current,next);
  const [row]=await planAndroidE2e(f.candidate,f.current,request);
  assert.equal(row.decision,'REUSE',row.reason);
  assert.equal(row.provenance.runtimeAdmission.kind,'UNCHANGED_ANDROID_BUILD_WITH_VERIFICATION_ONLY_INPUTS');
  assert.deepEqual(row.provenance.runtimeAdmission.changedInputs,[verification.path,web.path,seals.path].sort());
  assert.equal(row.provenance.executionSourceCommit,f.manifest.source.sourceCommit);
  assert.deepEqual(await fs.readFile(path.join(f.candidate,receiptFile)),original);
  assert.deepEqual(await fs.readFile(report),originalReport);
  assert.throws(()=>admitWebOnlyRuntimeChanges(f.files,next),/ANDROID_E2E_RUNTIME_CHANGED/,'The legacy Web-only contract stays strict');
  console.log('Verification-only native readmission: original execution bytes and source commit preserved');
});

for(const file of ['config/release-platform-rag-policy.json','whist-lab-app/app/build.gradle',
  'whist-lab-app/card-table-core/src/main/java/CardFaceMetrics.java','whist-lab-app/app/src/main/assets/table.bin',
  'lib/core.jar','config/whist-hard-policy.yaml'])
test('verification admission cannot conceal a changed native build or scenario '+file,()=>{
  const verification={path:'atf-tests/src/test/java/Test.java',sha256:digest('old test')};
  const changed={path:file,sha256:digest('old native')};
  const before=[native,verification,changed];
  const after=[native,{...verification,sha256:digest('new test')},{...changed,sha256:digest('new native')}];
  assert.throws(()=>admitVerificationRuntimeChanges(before,after),/ANDROID_E2E_RUNTIME_CHANGED/);
});

test('verification maintenance cannot readmit changed native scenarios through the real planner',async t=>{
  const f=await nativeFixture(t,{extraInputs:[{path:'atf-tests/src/test/java/Test.java',sha256:digest('old')}]});
  const next=f.files.map(row=>row.path.startsWith('atf-test-android/')||row.path.startsWith('atf-tests/src/test/')
    ?{...row,sha256:digest('changed '+row.path)}:row);
  await setSources(f.candidate,f.current,next);
  const [row]=await planAndroidE2e(f.candidate,f.current,request);
  assert.equal(row.decision,'RUN');assert.match(row.reason,/ANDROID_E2E_SCENARIO_INPUTS_CHANGED/);
});
