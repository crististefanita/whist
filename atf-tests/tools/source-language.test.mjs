import assert from 'node:assert/strict';
import fs from 'node:fs/promises';
import test from 'node:test';
import {inspectSource} from './source-language.mjs';

const policy=JSON.parse(await fs.readFile(new URL('../../config/source-language-policy.json',import.meta.url),'utf8'));
const scan=(name,text)=>inspectSource(name,text,policy);

test('comments and Javadoc reject reviewed accented and ASCII Romanian on the original line',()=>{
  for(const [name,text,line] of [['Case.java','class Case {}\n// verifica rezultatul',2],
    ['case.mjs','/* păstrează progresul */',1],['case.ps1','# Citeste fisierul',1],
    ['case.psm1','<# verifica datele #>',1],['build.gradle','// pentru fiecare',1],
    ['workflow.yml','# verifica rezultatul',1],['settings.properties','# foloseste valoare',1],
    ['layout.xml','<!-- verifica rezultatul -->',1],['view.css','/* afisare */',1],
    ['Case.java','/** English opening.\n * verifica rezultatul\n */',2]]){
    const actual=scan(name,text);assert.equal(actual.length,1,name);assert.equal(actual[0].line,line);
    assert.equal(actual[0].kind,'comment');
  }
});
test('English identifiers and comments coexist with localized data and quoted fixtures',()=>{
  for(const [name,text] of [['Case.java','// Preserve local progress.\nString label="Mâna următoare"; String fixture="// verifica rezultatul";'],
    ['case.mjs','const title="Mâna următoare";const fixture=/pentru\\/fiecare/;const url="https://example.invalid/"; // English explanation'],
    ['case.ps1',"$label='Mâna următoare'\n$fixture=@'\n// verifica rezultatul\n'@\n# English explanation"],
    ['rules.yaml','rule_ro: "Păstrează cartea mare"\nrule_en: "Keep the high card"'],
    ['Case.java','String name="Ștefan"; // Teo and Nicolae are proper names.']])assert.deepEqual(scan(name,text),[],name);
});
test('identifiers reject camelCase snake_case and escaped Romanian diagnostics',()=>{
  for(const source of ['void verificaScor() {}','int numar_carti;','int numărCărți;',
    'throw new IllegalStateException("pentru fiecare");',String.raw`throw new Error("\u0070entru fiecare");`])
    assert.ok(scan('Case.java',source).length,source);
});
test('diagnostic arguments are checked without treating callback fixtures as explanations',()=>{
  for(const text of ['throw Error("verifica rezultatul")','recordCheck("ID",true,"verifica rezultatul")',
    'audit("English title","verifica rezultatul",()=>{})','assert.equal(1,2,"verifica rezultatul")',
    'AuditedAssertions.assertTrue(true,"verifica rezultatul");'])assert.ok(scan('case.mjs',text).length,text);
  assert.deepEqual(scan('case.mjs','audit("English title","English context",()=>{const name="Ștefan";});'),[]);
});
test('PowerShell continuation and JavaScript interpolation do not hide implementation identifiers',()=>{
  assert.ok(scan('case.ps1',"Write-Output `\n 'value'\nthrow 'verifica rezultatul'").length);
  assert.ok(scan('case.mjs','const message=`Value: ${verificaScor()}`;').some(x=>x.kind==='identifier'));
});
test('empty or malformed policies cannot disable enforcement and semantic limits stay explicit',()=>{
  for(const invalid of [{...policy,language:'ro'},{...policy,reviewedRomanianWords:[]},
    {...policy,reviewedRomanianWords:['bad word']},{...policy,exceptions:['**']}])assert.throws(()=>inspectSource('Case.java','',invalid),/SOURCE-LANGUAGE-POLICY/);
  assert.deepEqual(scan('Case.java','// An unfamiliar natural-language word still requires semantic review.'),[]);
});
