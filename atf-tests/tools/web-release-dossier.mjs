import fs from 'node:fs';
import path from 'node:path';
import {createHash} from 'node:crypto';
import {pathToFileURL} from 'node:url';

const categories=['component','e2e','governance','reference','diagnostics'];
const hash=bytes=>createHash('sha256').update(bytes).digest('hex');
const requireValue=(ok,code)=>{if(!ok)throw Error('WEB_DOSSIER_'+code);};
export function verifyWebDossier(project){
 const root=path.resolve(project,'release/WEB');
 const read=relative=>{
  requireValue(typeof relative==='string'&&relative&&!/[\\:%#?]/.test(relative)&&!relative.startsWith('/')&&relative.split('/').every(n=>n&&n!=='.'&&n!=='..'),'RELATIVE_PATH');
  let at=root;for(const part of relative.split('/')){at=path.join(at,part);requireValue(!fs.lstatSync(at).isSymbolicLink(),'INDIRECT_PATH');}
  requireValue(fs.statSync(at).isFile(),'REGULAR_FILE');return fs.readFileSync(at);
 };
 requireValue(!fs.lstatSync(root).isSymbolicLink()&&fs.statSync(root).isDirectory(),'ROOT');
 const index=JSON.parse(read('index.json')),catalog=JSON.parse(fs.readFileSync(path.join(project,'release/index.json')));
 requireValue(catalog.webDossier==='WEB/index.json','REGISTRATION');
 requireValue(index.schemaVersion===1&&index.kind==='WHIST_WEB_TEST_DOSSIER','SCHEMA');
 requireValue(['OPEN','BROWSER_QUALIFIED'].includes(index.state),'STATE');
 requireValue(/^https:\/\//.test(index.publication?.url??'')&&/^[a-f0-9]{40}$/.test(index.publication?.sourceCommit??''),'PUBLICATION');
 requireValue(Array.isArray(index.reports)&&index.reports.length>0&&Array.isArray(index.integrity),'INVENTORY');
 const files=new Map();for(const row of index.integrity){
  requireValue(typeof row.path==='string'&&/\.(html|json)$/.test(row.path)&&row.path!=='index.json','PAYLOAD_TYPE');
  requireValue(!files.has(row.path),'DUPLICATE_FILE');const bytes=read(row.path);
  requireValue(Number.isSafeInteger(row.bytes)&&row.bytes===bytes.length&&row.sha256===hash(bytes),'HASH');files.set(row.path,row);
 }
 const ids=new Set(),reports=new Set(),guide=read('README.md').toString('utf8');let primary=0;
 for(const report of index.reports){
  requireValue(typeof report.id==='string'&&report.id&&!ids.has(report.id),'DUPLICATE_REPORT');ids.add(report.id);
  requireValue(categories.includes(report.category)&&report.path.startsWith(report.category+'/')&&report.path.endsWith('.html')&&!reports.has(report.path),'REPORT_PATH');reports.add(report.path);
  requireValue(files.has(report.path)&&guide.includes('('+report.path+')'),'REPORT_LINK');
  requireValue(['PRIMARY','REUSED','REFERENCE','REJECTED','SUPPLEMENTAL'].includes(report.role)&&['PASS','FAIL'].includes(report.result)&&typeof report.scope==='string'&&report.scope.length>15,'SCOPE');
  requireValue(/^[a-f0-9]{40}$/.test(report.validatorCommit??''),'PROVENANCE');
  const html=read(report.path).toString('utf8');
  requireValue(html.includes('id="dashboard-view"')&&html.includes('helm-validation-table'),'EXTENT');
  if(report.category==='component')requireValue(!/<a\b[^>]*data-atf-screenshot="base64"/.test(html),'COMPONENT_CAPTURE');
  if(report.category==='e2e'&&report.role==='PRIMARY'){
   primary++;requireValue(html.includes('TABLE-STEP-')&&html.includes('data:image/png;base64,'),'E2E_AUDIT');
   requireValue(index.state!=='BROWSER_QUALIFIED'||report.result==='PASS','QUALIFICATION');
  }
  if(report.proof){const proof=JSON.parse(read(report.proof));requireValue(proof.reportSha256===files.get(report.path).sha256&&proof.identity?.commit===report.validatorCommit,'PROOF');}
 }
 requireValue(primary===1,'PRIMARY_E2E');
 requireValue([...files.keys()].filter(n=>n.endsWith('.html')).every(n=>reports.has(n)),'UNREGISTERED_REPORT');
 const indexedCategories=categories.filter(category=>category!=='reference'
  ||index.reports.some(report=>report.category===category)||fs.existsSync(path.join(root,category,'README.md')));
 const expected=new Set(['index.json','README.md',...indexedCategories.map(n=>n+'/README.md'),...files.keys()]);
 const actual=[];const walk=prefix=>{for(const entry of fs.readdirSync(path.join(root,prefix),{withFileTypes:true})){const name=prefix+entry.name;requireValue(!entry.isSymbolicLink(),'INDIRECT_PATH');if(entry.isDirectory())walk(name+'/');else{requireValue(entry.isFile(),'REGULAR_FILE');actual.push(name);}}};walk('');
 requireValue(actual.length===expected.size&&actual.every(n=>expected.has(n)),'UNREGISTERED_FILE');
 for(const file of expected)if(file.endsWith('.md'))read(file);
 return {status:'PASS',kind:index.kind,sourceCommit:index.publication.sourceCommit,reports:index.reports.length,immutableFiles:files.size,primaryE2e:primary,indexSha256:hash(read('index.json'))};
}
if(process.argv[1]&&import.meta.url===pathToFileURL(path.resolve(process.argv[1])).href){
 try{console.log(JSON.stringify(verifyWebDossier(process.argv[2]??process.cwd())));}catch(error){console.error(error.message);process.exitCode=1;}
}
