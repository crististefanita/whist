import assert from 'node:assert/strict';
import test, {before, after} from 'node:test';
import fs from 'node:fs/promises';
import os from 'node:os';
import path from 'node:path';
import {fileURLToPath} from 'node:url';
import {execFileSync} from 'node:child_process';
import {withServer} from './fixtures.mjs';

let root, parent, classes;
const assets = {
  'index.html': '<link href="./table.css?v=test"><script type="module" src="./table.mjs?v=test"></script>',
  'table.css': '@font-face{src:url("./Roboto-Regular.ttf")} @font-face{src:url(./Roboto-Bold.ttf)} .card{src:url(./card-suits.woff2)}',
  'table.mjs': 'import {value} from "./viewport.mjs?v=test"; const worker=new URL("./worker.mjs",import.meta.url);',
  'viewport.mjs': 'export const value=1;',
  'worker.mjs': 'import {main} from "./whist-engine.mjs";',
  'whist-engine.mjs': 'export function main(){}',
  'surface-fit.mjs': 'export const fit=1;',
  'Roboto-Regular.ttf': 'controlled regular font bytes', 'Roboto-Bold.ttf': 'controlled bold font bytes',
  'card-suits.woff2': 'wOF2controlled font bytes', 'menu-footer.png': 'controlled image bytes',
  'Roboto-NOTICE.txt': 'controlled font notice'
};
const game = () => ({classes, main:'ro.cardtable.testing.browser.StaticSiteHost'});
before(async () => {
  parent = await fs.realpath(process.env.WHIST_QUALIFICATION_TEMP_ROOT ?? os.tmpdir());
  root = await fs.mkdtemp(path.join(parent,'whist-static-host-'));
  classes = path.join(root,'classes'); await fs.mkdir(classes);
  execFileSync('javac',['--release','17','-encoding','UTF-8','-d',classes,
    fileURLToPath(new URL('../src/test/java/ro/cardtable/testing/browser/StaticSiteHost.java',import.meta.url))],{windowsHide:true});
});
after(async () => {
  if (root) {assert.equal(path.dirname(await fs.realpath(root)),parent); await fs.rm(root,{recursive:true,force:true});}
});
async function site(name, missing) {
  const dir = path.join(root,name); await fs.mkdir(dir);
  for(const [file,text] of Object.entries(assets))if(file!==missing)await fs.writeFile(path.join(dir,file),text);
  return dir;
}

test('delivered module and font inventory is served with its original bytes and MIME',async () => {
  const dir=await site('complete'),observations=[];
  await withServer(game(),dir,async url => {
    for(const [name,expected]of Object.entries(assets)){
      const response=await fetch(new URL(name+'?v=current',url));assert.equal(response.status,200,name);
      const body=await response.text(),type=response.headers.get('content-type');assert.equal(body,expected,name);
      const wanted=name.endsWith('.mjs')?'text/javascript; charset=utf-8':name.endsWith('.ttf')?'font/ttf':
        name.endsWith('.woff2')?'font/woff2':name.endsWith('.css')?'text/css; charset=utf-8':
        name.endsWith('.png')?'image/png':name.endsWith('.txt')?'text/plain; charset=utf-8':'text/html; charset=utf-8';
      assert.equal(type,wanted,name);assert.equal(response.headers.get('cache-control'),'no-store');
      observations.push({name,expectedType:wanted,observedType:type,body});
    }
  });
  console.log('Static site HTTP observations: '+JSON.stringify({fixture:'Controlled assets, no browser or font rendering',observations}));
});

test('missing transitive modules or fonts fail before the browser callback',async () => {
  const observations=[];
  for(const missing of ['viewport.mjs','worker.mjs','whist-engine.mjs','Roboto-Regular.ttf','Roboto-Bold.ttf','card-suits.woff2']){
    let started=false;const dir=await site('missing-'+missing,missing);
    await assert.rejects(withServer(game(),dir,async()=>{started=true;}),/Missing or indirect site asset/);
    assert.equal(started,false);observations.push({missing,browserCallbackStarted:started});
  }
  console.log('Static site admission observations: '+JSON.stringify(observations));
});

test('unknown paths methods and traversal never expose files outside the delivered inventory',async()=>{
  const dir=await site('rejections'),observations=[];await fs.writeFile(path.join(dir,'private.secret'),'not a web asset');
  await withServer(game(),dir,async url=>{
    for(const [name,method]of [['missing.mjs','GET'],['private.secret','GET'],['table.mjs','POST'],['table.css','HEAD'],
      ['%252e%252e/private.secret','GET'],['%2Fprivate.secret','GET']]){
      const response=await fetch(new URL(name,url),{method});assert.equal(response.status,404,name);
      assert.equal(await response.text(),'');observations.push({name,method,status:response.status});
    }
  });
  console.log('Static site rejection observations: '+JSON.stringify(observations));
});

test('snapshot admission keeps original bytes and rejects files added after startup',async()=>{
  const dir=await site('snapshot');await withServer(game(),dir,async url=>{
    await fs.writeFile(path.join(dir,'viewport.mjs'),'changed after admission');
    await fs.writeFile(path.join(dir,'late.mjs'),'added after admission');
    assert.equal(await(await fetch(new URL('viewport.mjs',url))).text(),assets['viewport.mjs']);
    assert.equal((await fetch(new URL('late.mjs',url))).status,404);
  });
  console.log('Static site snapshot observations: original module retained; late module rejected');
});
