import assert from 'node:assert/strict';
import test from 'node:test';
import {compilerInputs} from './component-compiler-inputs.mjs';
const row = path => ({path, sha256: 'a'.repeat(64)});

test('compiler inventories omit editorial inputs and retain resource payloads and unknown dependencies', () => {
  const documentation = ['README.md','docs/testing/guide.md','atf-tests/src/test/README.md',
    'src/test/java/demo/guide.md','lib/README.Reporting.md','config/documentation-editions.json'];
  const compiled = ['pom.xml','.mvn/jvm.config','lib/reporting.jar','module/src/test/java/Probe.java',
    'module/src/test/resources/README.md','src/test/resources/case.md','module/src/main/assets/guide.md',
    'new/unknown-input.bin','scripts/AtfComponentMaven.ps1'];
  assert.deepEqual(compilerInputs([...documentation,...compiled].map(row)).map(r=>r.path), compiled.sort());
  assert.deepEqual(compilerInputs([...compiled].reverse().map(row)), compilerInputs(compiled.map(row)));
});

test('compiler inventories reject duplicate, indirect and malformed source identities', () => {
  for (const rows of [[row('pom.xml'),row('pom.xml')], [row('../foreign.java')], [{path:'pom.xml',sha256:'bad'}]]) {
    assert.throws(()=>compilerInputs(rows));
  }
});

test('literal Maven roots keep shared providers while isolated Gradle test Java leaves the compiler key', () => {
  const scope={schemaVersion:1,compiledRoots:['module/src/main/java','module/src/test/java',
    'module/src/test/resources','game/src/main/java','custom/checks','ANDROID/src/test/java/PAYLOAD'],testRoots:['android/src/test/java',
    'game/src/test/java','module/src/test/java','custom/checks']};
  const keep=['pom.xml','config/atf-governance-policy.json','module/src/test/java/Case.java',
    'game/src/main/java/Engine.java','custom/checks/Case.java','module/src/test/resources/Data.java',
    'android/src/test/java/fixture.yaml','android/src/test/java/payload/Data.java','unknown/src/test/java/Case.java','android/src/test/javax/Case.java'];
  const skip=['android/src/test/java/Case.java','game/src/test/java/RuleTest.java'];
  assert.deepEqual(compilerInputs([...keep,...skip].map(row),scope).map(r=>r.path),keep.sort());
  assert.deepEqual(compilerInputs([...keep,...skip].map(row)).map(r=>r.path),[...keep,...skip].sort());
});

test('invalid compiler scopes cannot grant an exclusion', () => {
  for(const scope of [{schemaVersion:2,compiledRoots:['a'],testRoots:[]},
    {schemaVersion:1,compiledRoots:[],testRoots:['android']},
    {schemaVersion:1,compiledRoots:['../outside'],testRoots:[]},
    {schemaVersion:1,compiledRoots:['a'],testRoots:['android\\test']}])assert.throws(()=>compilerInputs([row('pom.xml')],scope));
});
