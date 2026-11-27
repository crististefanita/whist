import assert from 'node:assert/strict';
import fs from 'node:fs/promises';
import path from 'node:path';
import os from 'node:os';
import {execFileSync} from 'node:child_process';
import test from 'node:test';
import {inspectCheckout} from './source-language.mjs';

async function fixture(t){
  const root=await fs.mkdtemp(path.join(process.env.WHIST_QUALIFICATION_TEMP_ROOT??os.tmpdir(),'whist-source-language-'));
  t.after(()=>fs.rm(root,{recursive:true,force:true}));
  await fs.mkdir(path.join(root,'config'));
  await fs.copyFile(new URL('../../config/source-language-policy.json',import.meta.url),path.join(root,'config/source-language-policy.json'));
  execFileSync('git',['-C',root,'init','-q'],{windowsHide:true});
  return root;
}
test('accepts real checkout inventory with localized payloads and reports its actual population',async t=>{
  const root=await fixture(t);
  await fs.writeFile(path.join(root,'Case.java'),'// Preserve progress.\nclass Case { String caption="Mâna următoare"; }');
  await fs.writeFile(path.join(root,'rules.yaml'),'rule_ro: "Păstrează cartea mare"');
  const actual=await inspectCheckout(root);assert.equal(actual.status,'PASS');
  assert.deepEqual(actual.files,['Case.java','config/source-language-policy.json','rules.yaml']);
  process.stdout.write(JSON.stringify(actual)+'\n');
});
test('excludes tracked deletions from the current source population',async t=>{
  const root=await fixture(t);
  const old=path.join(root,'config/release-platform-rag-policy.json');
  await fs.writeFile(old,'{}');
  await fs.writeFile(path.join(root,'Case.java'),'// Preserve progress.\nclass Case {}');
  execFileSync('git',['-C',root,'add','-A'],{windowsHide:true});
  execFileSync('git',['-C',root,'-c','user.name=ATF','-c','user.email=atf@example.invalid','commit','-qm','fixture'],{windowsHide:true});
  await fs.rm(old);
  const actual=await inspectCheckout(root);
  assert.equal(actual.status,'PASS');
  assert.ok(actual.excludedTrackedDeletions.includes('config/release-platform-rag-policy.json'));
  assert.ok(!actual.files.includes('config/release-platform-rag-policy.json'));
  process.stdout.write(JSON.stringify(actual)+'\n');
});
test('rejects implementation violations on both documentation editions without exempting a whole fixture file',async t=>{
  const root=await fixture(t);
  for(const edition of ['en','ro']){
    await fs.writeFile(path.join(root,'config/atf-governance-policy.json'),JSON.stringify({documentationLanguage:edition}));
    await fs.writeFile(path.join(root,'Case.java'),'class Case { String fixture="pentru fiecare"; }\n// verifica rezultatul\n');
    const actual=await inspectCheckout(root);assert.equal(actual.status,'BLOCKED');
    assert.equal(actual.diagnostics.length,1);assert.equal(actual.diagnostics[0].line,2);
    assert.equal(actual.diagnostics[0].kind,'comment');
    process.stdout.write(JSON.stringify({edition,...actual})+'\n');
  }
  await fs.writeFile(path.join(root,'Case.java'),'class Case { void verificaScor() {} }');
  assert.equal((await inspectCheckout(root)).diagnostics[0].kind,'identifier');
  await fs.writeFile(path.join(root,'Case.java'),'class Case {}');
  await fs.writeFile(path.join(root,'.gitignore'),'module/src/');
  await fs.writeFile(path.join(root,'config/atf-governance-policy.json'),JSON.stringify({mavenModules:['module']}));
  await fs.mkdir(path.join(root,'module/src/main/java'),{recursive:true});
  await fs.writeFile(path.join(root,'module/src/main/java/Hidden.java'),'// verifica rezultatul');
  assert.equal((await inspectCheckout(root)).diagnostics[0].file,'module/src/main/java/Hidden.java');
});
