import assert from 'node:assert/strict';
import test from 'node:test';
import {dependencyFixture, dependencyDigest as hash} from './atf-dependency-fixture.mjs';
import {dependencyDecision, atfDependencyPolicyPath, atfReaderSealsPath} from './atf-component-dependencies.mjs';

function reviewedFixture() {
  const f = dependencyFixture();
  f.sources.set('tools/middle.mjs', "import fs from 'node:fs'; export const read = file => fs.readFileSync(file);");
  f.qualify();
  const previous = f.inputs();
  const select = (file, old = previous) => dependencyDecision(file, f.inputs(), old, f.model, ['first', 'second']);
  return {...f, previous, select};
}

test('reviewed seal-only updates retain topology and select the transitive reader consumers', t => {
  const f = reviewedFixture();
  f.sources.set('tools/middle.mjs', f.sources.get('tools/middle.mjs') + '\n// reviewed boundary clarification');
  f.qualify();
  assert.equal(f.inputs().get(atfDependencyPolicyPath), f.previous.get(atfDependencyPolicyPath));
  assert.notEqual(f.inputs().get(atfReaderSealsPath), f.previous.get(atfReaderSealsPath));
  const decision = f.select(atfReaderSealsPath);
  assert.deepEqual(decision.groups, ['first']);
  assert.equal(decision.reason, 'REVIEWED_READER_SEAL_CONSUMERS');
  assert.deepEqual(decision.changedReaders, ['tools/middle.mjs']);
  assert.deepEqual(decision.evidence[0].chain, ['src/first/First.java', 'tools/entry.test.mjs', 'tools/middle.mjs']);
  assert.equal(decision.evidence[0].reader, 'tools/middle.mjs');
  assert.equal(decision.topologySha256, f.policy.topologySha256);
  assert.equal(decision.readerSealsSha256, f.inputs().get(atfReaderSealsPath));
  assert.deepEqual(f.select('tools/middle.mjs').groups, ['first']);
  t.diagnostic(JSON.stringify({previousSelectedOwners: 2, selectedOwners: decision.groups.length,
    reusedOwners: ['second'], reason: decision.reason, topologySha256: decision.topologySha256}));
});

test('simultaneous reviewed reader changes retain every consumer and unrelated changed inputs', () => {
  const f = reviewedFixture();
  f.sources.set('tools/isolated.mjs', "import fs from 'node:fs'; export const read = f => fs.readFileSync(f);");
  f.qualify();
  const previous = f.inputs();
  for (const file of ['tools/middle.mjs', 'tools/isolated.mjs']) f.sources.set(file, f.sources.get(file) + '\n// reviewed');
  f.sources.set('scripts/unowned.ps1', 'function Get-UnownedValue { 3 }');
  f.qualify();
  assert.deepEqual(f.select(atfReaderSealsPath, previous).groups.sort(), ['first', 'second']);
  assert.equal(f.select('scripts/unowned.ps1', previous).reason, 'DEPENDENCY_INPUT_UNOWNED');
  assert.deepEqual(f.select('scripts/unowned.ps1', previous).groups, ['first', 'second']);
});

test('seal inventory observers do not broaden changed reader consumers', () => {
  const f = reviewedFixture();
  f.policy.observers.push({source: 'src/second/Second.java', inputs: ['config/**']});
  f.qualify();
  const previous = f.inputs();
  f.sources.set('tools/middle.mjs', f.sources.get('tools/middle.mjs') + '\n// reviewed');
  f.qualify();
  const decision = f.select(atfReaderSealsPath, previous);
  assert.deepEqual(decision.groups, ['first']);
  assert(decision.evidence.every(row => row.reader === 'tools/middle.mjs'));
  assert(decision.evidence.every(row => !row.observer));
});

test('changed executable topology or declared scope cannot masquerade as a seal-only update', () => {
  for (const change of ['edge', 'observer', 'population', 'global', 'reader-role', 'provider']) {
    const f = reviewedFixture();
    if (change === 'edge') f.sources.set('tools/isolated.mjs', "import './middle.mjs';");
    if (change === 'observer') f.policy.observers.push({source: 'src/second/Second.java', inputs: ['scripts/**']});
    if (change === 'population') f.sources.set('scripts/added.ps1', '1');
    if (change === 'global') f.policy.globalInputs.push('tools/isolated.mjs');
    if (change === 'reader-role') f.sources.set('tools/isolated.mjs', "import fs from 'node:fs';");
    if (change === 'provider') f.policy.generatedModules = [{source: 'tools/isolated.mjs', specifier: './generated.mjs', inputs: ['src/**']}];
    f.qualify();
    const decision = f.select(atfReaderSealsPath);
    assert.equal(decision.status, 'CONSERVATIVE', change);
    assert.equal(decision.reason, 'DEPENDENCY_POLICY_NOT_SHARED', change);
    assert.deepEqual(decision.groups, ['first', 'second'], change);
  }
});

test('unreviewed topology and missing stale malformed or foreign reader seals fail closed', () => {
  for (const change of ['topology', 'missing', 'stale', 'foreign', 'shape', 'hash', 'schema']) {
    const f = reviewedFixture();
    const seals = JSON.parse(f.sources.get(atfReaderSealsPath));
    if (change === 'topology') f.sources.set('tools/isolated.mjs', "import './middle.mjs';");
    if (change === 'missing') delete seals.readerSeals['tools/middle.mjs'];
    if (change === 'stale') f.sources.set('tools/middle.mjs', f.sources.get('tools/middle.mjs') + '\n// unreviewed');
    if (change === 'foreign') seals.readerSeals['tools/absent.mjs'] = hash('foreign');
    if (change === 'shape') seals.readerSeals = [];
    if (change === 'schema') seals.schemaVersion = 9;
    f.sources.set(atfReaderSealsPath, JSON.stringify(seals));
    const load = change === 'hash' ? () => {
      const inputs = f.inputs(); inputs.set(atfReaderSealsPath, hash('different bytes')); return f.model(inputs);
    } : f.model;
    const decision = dependencyDecision(atfReaderSealsPath, f.inputs(), f.previous, load, ['first', 'second']);
    assert.equal(decision.status, 'CONSERVATIVE', change);
    assert.match(decision.reason, /^DEPENDENCY_(?:TOPOLOGY|READER)/, change);
    assert.deepEqual(decision.groups, ['first', 'second'], change);
  }
});

test('missing historical seal inventories unknown consumers and unexplained seal changes cannot narrow selection', () => {
  for (const change of ['historical', 'population', 'unowned-reader', 'unexplained']) {
    const f = reviewedFixture();
    const previous = new Map(f.previous);
    if (change === 'historical') previous.delete(atfReaderSealsPath);
    if (change === 'population') previous.delete('src/shared/Peer.java');
    if (change === 'unowned-reader') {
      f.sources.set('scripts/unowned.ps1', 'Get-Content $path'); f.qualify();
      for (const [file, sha] of f.inputs()) previous.set(file, sha);
      f.sources.set('scripts/unowned.ps1', 'Get-Content $path\n# reviewed but unowned'); f.qualify();
    }
    if (change === 'unexplained') f.sources.set(atfReaderSealsPath, f.sources.get(atfReaderSealsPath) + '\n');
    const decision = f.select(atfReaderSealsPath, previous);
    assert.equal(decision.status, 'CONSERVATIVE', change);
    assert.deepEqual(decision.groups, ['first', 'second'], change);
  }
});
