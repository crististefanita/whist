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
