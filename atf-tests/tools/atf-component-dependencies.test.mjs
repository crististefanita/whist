import assert from 'node:assert/strict';
import test from 'node:test';
import fs from 'node:fs';
import path from 'node:path';
import {fileURLToPath} from 'node:url';
import {execFileSync} from 'node:child_process';
import os from 'node:os';
import {buildAtfDependencyModel, dependencyDecision, atfDependencyPolicyPath, dependencyPattern} from './atf-component-dependencies.mjs';
import {atfCollections} from './atf-component-ownership.mjs';

import {dependencyFixture as fixture, dependencyDigest as digest} from './atf-dependency-fixture.mjs';

test('dependency graph selects actual Java, Node and PowerShell consumers through transitive edges and cycles', () => {
  const {model} = fixture();
  const graph = model();
  for (const file of ['src/shared/Helper.java', 'src/shared/Peer.java', 'tools/middle.mjs', 'scripts/helper.ps1']) {
    assert.deepEqual(graph.owners(file), ['first'], file);
  }
  assert.deepEqual(graph.owners('tools/isolated.mjs'), ['second']);
  assert.equal(graph.owners('scripts/unowned.ps1'), null);
});

test('classifier path labels and nested Java type names do not create unrelated dependencies', () => {
  const {sources, model, qualify} = fixture();
  sources.set('tools/isolated.mjs', "const label = 'bridge.ps1'; const example = \"import './middle.mjs';\";");
  sources.set('src/first/First.java', 'package first; import shared.Helper; class First { record Run(int value) {} Helper value; String tool="entry.test.mjs"; }');
  sources.set('src/second/Second.java', 'package second; class Second { record Run(int value) {} Run value; String tool="isolated.mjs"; }');
  qualify();
  assert.deepEqual(model().owners('scripts/helper.ps1'), ['first']);
  assert.deepEqual(model().owners('src/shared/Helper.java'), ['first']);
});

test('shared dependencies and declared repository readers invalidate every real consumer', () => {
  const {model, sources, policy, qualify} = fixture();
  sources.set('src/second/Second.java', 'package second; import shared.Helper; class Second { Helper value; }');
  qualify();
  assert.deepEqual(model().owners('src/shared/Peer.java'), ['first', 'second']);
  policy.observers.push({source: 'src/second/Second.java', inputs: ['scripts/**']});
  qualify();
  assert.deepEqual(model().owners('scripts/helper.ps1'), ['first', 'second']);
  assert.deepEqual(model().owners('scripts/unowned.ps1'), ['second']);
});

test('changed import edges update the consumer closure without trusting the previous graph', () => {
  const {model, sources, qualify} = fixture();
  sources.set('src/second/Second.java', 'package second; import shared.Peer; class Second { Peer value; }');
  qualify();
  assert.deepEqual(model().owners('src/shared/Peer.java'), ['first', 'second']);
  sources.set('src/first/First.java', 'package first; class First {}');
  qualify();
  assert.deepEqual(model().owners('src/shared/Peer.java'), ['second']);
});

test('unknown, unowned, historical and missing populations fail closed before evidence can be reused', () => {
  const {model, inputs} = fixture();
  const current = inputs(); current.set(atfDependencyPolicyPath, digest('policy'));
  const previous = new Map(current), groups = ['first', 'second'];
  const select = (file, old = previous, load = model) => dependencyDecision(file, current, old, load, groups);
  assert.deepEqual(select('tools/middle.mjs').groups, ['first']);
  assert.deepEqual(select('new/unknown.mjs').groups, groups);
  assert.deepEqual(select('scripts/unowned.ps1').groups, groups);
  const legacy = new Map(previous); legacy.delete(atfDependencyPolicyPath);
  assert.equal(select('tools/middle.mjs', legacy).reason, 'DEPENDENCY_POLICY_NOT_SHARED');
  const missing = new Map(previous); missing.delete('src/shared/Helper.java');
  assert.equal(select('tools/middle.mjs', missing).reason, 'DEPENDENCY_PREVIOUS_POPULATION_MISSING');
  assert.deepEqual(select('tools/middle.mjs', previous, () => { throw Error('missing edge provider'); }).groups, groups);
});

test('missing roots, malformed scopes and stale source hashes cannot authorize selective reuse', () => {
  const f = fixture(), rows = f.inputs();
  rows.set('tools/middle.mjs', digest('changed bytes'));
  assert.throws(() => f.model(rows), /DEPENDENCY_SOURCE_HASH/);
  f.collections[0].classes = ['missing.Root'];
  assert.throws(() => f.model(), /DEPENDENCY_ROOT/);
  f.collections[0].classes = ['first.First'];
  f.policy.observers.push({source: 'absent.java', inputs: ['scripts/**']});
  assert.throws(() => f.model(), /DEPENDENCY_OBSERVER/);
  assert.throws(() => dependencyPattern('../outside/**'), /DEPENDENCY_PATTERN/);
  assert.throws(() => dependencyPattern('C:/outside/**'), /DEPENDENCY_PATTERN/);
});

test('new opaque readers require topology admission and changed bodies require reviewed seals', () => {
  const {sources, policy, model, qualify} = fixture();
  sources.set('tools/middle.mjs', "import fs from 'node:fs'; export const read = file => fs.readFileSync(file);");
  assert.throws(() => model(), /DEPENDENCY_TOPOLOGY_NOT_QUALIFIED/);
  qualify();
  assert.deepEqual(model().owners('tools/middle.mjs'), ['first']);
  sources.set('tools/middle.mjs', sources.get('tools/middle.mjs') + '\n// changed reader');
  assert.throws(() => model(), /DEPENDENCY_READER_NOT_QUALIFIED/);
});

test('generated module providers retain their producer inputs while undeclared imports are rejected', () => {
  const {sources, policy, model, qualify} = fixture();
  sources.set('tools/isolated.mjs', "import './generated.mjs';");
  assert.throws(() => model(), /DEPENDENCY_IMPORT_MISSING/);
  policy.generatedModules = [{source: 'tools/isolated.mjs', specifier: './generated.mjs', inputs: ['src/shared/**']}];
  qualify();
  assert.deepEqual(model().owners('src/shared/Peer.java'), ['first', 'second']);
  policy.generatedModules[0].source = 'tools/absent.mjs';
  assert.throws(() => model(), /DEPENDENCY_GENERATED_MODULE/);
});

test('live dependency graph admits the complete catalog and keeps unrelated documentation-rule owners reusable', t => {
  const root = path.resolve(fileURLToPath(new URL('../../', import.meta.url)));
  const rows = JSON.parse(execFileSync('pwsh', ['-NoProfile', '-NonInteractive', '-File',
    path.join(root, 'scripts/Get-AtfSourceInputs.ps1'), '-SourceRoot', root, '-AsJson'],
  {encoding: 'utf8', timeout: 30000, maxBuffer: 4 * 1024 * 1024}));
  const inputs = new Map(rows.map(row => [row.path, row.sha256]));
  const policy = JSON.parse(fs.readFileSync(path.join(root, atfDependencyPolicyPath)));
  const model = buildAtfDependencyModel(inputs, file => fs.readFileSync(path.join(root, file)), policy, atfCollections);
  assert.equal(model.closures.size, atfCollections.length);
  const file = 'atf-tests/src/main/java/atf/tests/documentation/DocumentationPathRules.java';
  assert.deepEqual(model.owners(file), ['atf-tests', 'atf-tests-documentation', 'atf-tests-orchestration']);
  const decisions = dependencyDecision(file, inputs, inputs, () => model);
  assert.equal(decisions.status, 'ADMITTED');
  assert(decisions.evidence.every(row => row.chain.length && (row.chain.at(-1) === file || row.observer)));
  const scratch = fs.mkdtempSync(path.join(os.tmpdir(), 'whist-dependency-proposal-'));
  t.after(() => fs.rmSync(scratch, {recursive: true, force: true}));
  const manifest = path.join(scratch, 'inputs.json'), proposal = path.join(scratch, 'proposal.json');
  fs.writeFileSync(manifest, JSON.stringify(rows));
  const cli = path.join(root, 'atf-tests/tools/atf-dependency-cli.mjs');
  const result = execFileSync(process.execPath, [cli, manifest, proposal], {encoding: 'utf8', timeout: 30000});
  assert.equal(JSON.parse(result).kind, 'ATF_DEPENDENCY_POLICY_PROPOSAL');
  assert.equal(JSON.parse(fs.readFileSync(proposal)).topologySha256, model.topologySha256);
  assert.deepEqual(JSON.parse(fs.readFileSync(path.join(scratch, 'atf-component-reader-seals.json'))).readerSeals, model.requiredReaderSeals);
  assert.throws(() => execFileSync(process.execPath, [cli, manifest, proposal], {stdio: 'pipe'}),
    error => error.status === 1 && /DEPENDENCY_PROPOSAL_OUTPUT_EXISTS/.test(error.stderr.toString()));
  const forbidden = path.join(root, 'config/dependency-proposal-must-not-exist.json');
  assert.throws(() => execFileSync(process.execPath, [cli, manifest, forbidden], {stdio: 'pipe'}),
    error => error.status === 1 && /DEPENDENCY_PROPOSAL_IN_CHECKOUT/.test(error.stderr.toString()));
  assert.equal(fs.existsSync(forbidden), false);
});
