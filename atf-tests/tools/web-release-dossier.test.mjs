import fs from 'node:fs';
import os from 'node:os';
import path from 'node:path';
import {createHash} from 'node:crypto';
import test from 'node:test';
import assert from 'node:assert/strict';
import {verifyWebDossier} from './web-release-dossier.mjs';
import {fileURLToPath} from 'node:url';
const hash=bytes=>createHash('sha256').update(bytes).digest('hex');

test('APK and Web indexes share the contract, audit roles and relative artifact navigation',()=>{
 const project=fileURLToPath(new URL('../../',import.meta.url));
 for(const file of ['atf-ui-browser/tests/README.md','atf-test-android/src/test/java/atf/android/e2e/display/README.md']){
  const text=fs.readFileSync(path.join(project,file),'utf8');
  for(const marker of ['table-audit-contract: TABLE-PHASE-CONTINUITY','table-audit-levels: component,e2e','table-audit-steps: GIVEN,WHEN,THEN'])assert.ok(text.includes(marker),file+' '+marker);
  const destinations=[...text.matchAll(/\]\(([^)]+)\)/g)].map(n=>{assert.ok(!/^(?:[a-z]+:|\/)/i.test(n[1]),'Index links are relative');return path.resolve(project,path.dirname(file),n[1]);});
  for(const target of ['docs/testing/ATF_TableContinuity.md','release/README.md','release/WEB/README.md'])assert.ok(destinations.includes(path.resolve(project,target)),file+' owns '+target);
  for(const target of destinations)assert.ok(fs.statSync(target).isFile());
 }
});
function fixture(t){
 const project=fs.mkdtempSync(path.join(os.tmpdir(),'web-dossier-'));t.after(()=>fs.rmSync(project,{recursive:true,force:true}));
 const root=path.join(project,'release/WEB');fs.mkdirSync(root,{recursive:true});
 for(const name of ['component','e2e','governance','reference','diagnostics']){fs.mkdirSync(path.join(root,name));fs.writeFileSync(path.join(root,name,'README.md'),'# Synthetic fixture index');}
 const reports=[{id:'COMPONENT',category:'component',path:'component/report.html',role:'REUSED',result:'PASS',scope:'Synthetic schema fixture, not a product execution',validatorCommit:'a'.repeat(40)},
  {id:'FLOW',category:'e2e',path:'e2e/report.html',role:'PRIMARY',result:'PASS',scope:'Synthetic schema fixture, not an actual browser journey',validatorCommit:'b'.repeat(40)}];
 const index={schemaVersion:1,kind:'WHIST_WEB_TEST_DOSSIER',state:'BROWSER_QUALIFIED',publication:{url:'https://example.invalid/game/',sourceCommit:'c'.repeat(40)},reports,integrity:[]};
 const save=()=>fs.writeFileSync(path.join(root,'index.json'),JSON.stringify(index));
 const add=(name,body)=>{const bytes=Buffer.from(body);fs.writeFileSync(path.join(root,name),bytes);const row={path:name,bytes:bytes.length,sha256:hash(bytes)},old=index.integrity.findIndex(n=>n.path===name);if(old>=0)index.integrity[old]=row;else index.integrity.push(row);save();};
 add('component/report.html','<div id="dashboard-view"></div><table class="helm-validation-table"></table>');
 add('e2e/report.html','<div id="dashboard-view"></div><table class="helm-validation-table">TABLE-STEP-1 data:image/png;base64,iVBORw0KGgo=</table>');
 fs.writeFileSync(path.join(root,'README.md'),'# Synthetic fixture\n\n[Component](component/report.html)\n[Flow](e2e/report.html)');
 fs.writeFileSync(path.join(project,'release/index.json'),JSON.stringify({webDossier:'WEB/index.json'}));
 return {project,root,index,save,add};
}
test('a registered portable dossier preserves component and E2E ownership without changing Android versions',t=>{
 const f=fixture(t),actual=verifyWebDossier(f.project);assert.equal(actual.status,'PASS');assert.equal(actual.primaryE2e,1);assert.equal(actual.reports,2);
 console.log('Web dossier observations: relative links, original SHA-256, explicit provenance and exactly one illustrated primary E2E');
});
test('modified bytes and missing local report links cannot qualify',t=>{
 const f=fixture(t);fs.appendFileSync(path.join(f.root,'component/report.html'),'changed');assert.throws(()=>verifyWebDossier(f.project),/HASH/);
 f.add('component/report.html','<div id="dashboard-view"></div><table class="helm-validation-table"></table>');
 fs.writeFileSync(path.join(f.root,'README.md'),'# Broken index');assert.throws(()=>verifyWebDossier(f.project),/REPORT_LINK/);
});
test('absolute, parent-traversing and indirect evidence paths are rejected',t=>{
 for(const name of ['C:/outside.html','../outside.html']){const f=fixture(t);f.index.integrity[0].path=name;f.save();assert.throws(()=>verifyWebDossier(f.project),/RELATIVE_PATH/);}
 const f=fixture(t),outside=path.join(f.project,'owned-original');fs.renameSync(path.join(f.root,'component'),outside);
 fs.symlinkSync(outside,path.join(f.root,'component'),'junction');assert.throws(()=>verifyWebDossier(f.project),/INDIRECT_PATH/);
});
test('component screenshots and loose PNG or XML payloads remain outside the dossier',t=>{
 const f=fixture(t);f.add('component/report.html','<div id="dashboard-view"></div><table class="helm-validation-table"></table><a data-atf-screenshot="base64">photo</a>');assert.throws(()=>verifyWebDossier(f.project),/COMPONENT_CAPTURE/);
 for(const name of ['loose.png','results.xml']){const x=fixture(t);x.add(name,'regenerable duplicate');assert.throws(()=>verifyWebDossier(x.project),/PAYLOAD_TYPE/);}
});
test('a second primary E2E, missing step audit and failed qualified flow are refused',t=>{
 const f=fixture(t);f.add('e2e/second.html',fs.readFileSync(path.join(f.root,'e2e/report.html')));f.index.reports.push({...f.index.reports[1],id:'SECOND',path:'e2e/second.html'});fs.appendFileSync(path.join(f.root,'README.md'),'\n[Second](e2e/second.html)');f.save();assert.throws(()=>verifyWebDossier(f.project),/PRIMARY_E2E/);
 const x=fixture(t);x.add('e2e/report.html','<div id="dashboard-view"></div><table class="helm-validation-table"></table>');assert.throws(()=>verifyWebDossier(x.project),/E2E_AUDIT/);
 const y=fixture(t);y.index.reports[1].result='FAIL';y.save();assert.throws(()=>verifyWebDossier(y.project),/QUALIFICATION/);
});
test('foreign proof identity, unknown files and unindexed reports cannot pass',t=>{
 const f=fixture(t);f.index.reports[0].proof='component/proof.json';f.add('component/proof.json',JSON.stringify({identity:{commit:'d'.repeat(40)},reportSha256:f.index.integrity[0].sha256}));assert.throws(()=>verifyWebDossier(f.project),/PROOF/);
 const x=fixture(t);fs.writeFileSync(path.join(x.root,'unregistered.log'),'noise');assert.throws(()=>verifyWebDossier(x.project),/UNREGISTERED_FILE/);
 const y=fixture(t);y.add('reference/extra.html','unindexed');assert.throws(()=>verifyWebDossier(y.project),/UNREGISTERED_REPORT/);
});
