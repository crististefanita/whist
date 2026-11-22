import assert from 'node:assert/strict';
import fs from 'node:fs/promises';
import os from 'node:os';
import path from 'node:path';
import test from 'node:test';
import {atfCollections, atfGroups, atfOwnershipPath, atfComponentGroups} from './atf-component-ownership.mjs';
import {componentEvidenceScope} from './component-evidence-scope.mjs';
import {deriveReleaseTestScope} from './release-test-scope.mjs';
import {digest, verifyComponentExecution} from './evaluate-release-rag.mjs';
import {prepareComponentReuse} from './prepare-component-reuse.mjs';

const ownership = digest(await fs.readFile(new URL('../../config/atf-component-collections.json', import.meta.url)));
const baseline = new Map([[atfOwnershipPath, ownership], ['README.md', 'a'.repeat(64)]]);
const current = new Map([...baseline, ['README.md', 'b'.repeat(64)]]);
const extent = '<html><img src="data:image/png;base64,AA=="></html>';

test('documentation-only selection runs one primary owner without replaying launcher fixtures', () => {
  const scope = deriveReleaseTestScope(baseline, current, [...atfGroups, 'atf-validation', 'atf-test-android']);
  assert.deepEqual(scope.runComponentGroups, ['atf-tests-documentation']);
  assert.deepEqual(scope.requiredE2eGroups, []);
  assert.equal(scope.kind, 'DOCUMENTATION_ONLY');
  assert.deepEqual(atfComponentGroups(baseline), atfGroups);
  for (const file of ['docs/testing/guide.md', 'whist-lab-app/game-core/src/test/java/README.md']) {
    const edited = new Map([...baseline, [file, 'c'.repeat(64)]]);
    assert.deepEqual(atfGroups.filter(g => componentEvidenceScope(g, edited, baseline).decision === 'RUN'), ['atf-tests-documentation']);
  }
  const rules = new Map([...baseline, ['docs/game/ATF_RulesEn.md', 'c'.repeat(64)]]);
  assert.equal(componentEvidenceScope('whist-lab-app-rules', rules, baseline).decision, 'RUN');
});

test('shared code and ownership changes invalidate every ATF consumer', () => {
  for (const file of ['scripts/AtfQualificationSnapshot.ps1', 'atf-tests/src/test/java/atf/tests/governance/support/Probe.java',
    'config/atf-test-suites.json', atfOwnershipPath]) {
    const edited = new Map([...baseline, [file, 'f'.repeat(64)]]);
    for (const group of atfGroups) assert.equal(componentEvidenceScope(group, edited, baseline).decision, 'RUN', file + ': ' + group);
  }
});

test('unknown inputs and unknown collections fail closed', () => {
  const edited = new Map([...baseline, ['unowned/runtime.bin', 'f'.repeat(64)]]);
  for (const group of [...atfGroups, 'atf-validation', 'whist-lab-app-display'])
    assert.equal(componentEvidenceScope(group, edited, baseline).decision, 'RUN', group);
  assert.equal(componentEvidenceScope('unregistered', current, baseline).decision, 'RUN');
});

test('transition from a historical monolith cannot relabel its evidence as a partition', () => {
  const legacy = new Map([['README.md', 'a'.repeat(64)]]);
  for (const group of atfGroups) assert.equal(componentEvidenceScope(group, baseline, legacy).decision, 'RUN', group);
  assert.equal(componentEvidenceScope('atf-tests', legacy, baseline).decision, 'RUN');
  assert.deepEqual(atfComponentGroups(legacy), ['atf-tests']);
  assert.equal(componentEvidenceScope('atf-tests', new Map([['README.md', 'b'.repeat(64)]]), legacy).decision, 'RUN');
});

async function fixture(t, {payload = false} = {}) {
  const root = await fs.mkdtemp(path.join(os.tmpdir(), 'whist-atf-owners-'));
  t.after(() => fs.rm(root, {recursive: true, force: true}));
  const previous = path.join(root, 'previous'), target = path.join(root, 'target');
  await fs.mkdir(previous); await fs.mkdir(target);
  const write = (dir, name, data) => fs.writeFile(path.join(dir, name), JSON.stringify(data));
  const rows = [...baseline].map(([path, sha256]) => ({path, sha256}));
  await write(previous, 'source-inputs.json', rows);
  await write(target, 'source-inputs.json', [...current].map(([path, sha256]) => ({path, sha256})));
  const release = [{path: 'release/index.json', sha256: 'd'.repeat(64)}];
  await write(previous, 'release-inputs.json', release);
  await write(target, 'release-inputs.json', payload ? [...release, {path: 'release/extra.json', sha256: 'e'.repeat(64)}] : release);
  const provenance = {kind: 'live-source-components', apkQualification: false, sourceFingerprint: digest(JSON.stringify(rows)),
    inputCount: rows.length, inputManifest: 'source-inputs.json', releaseInputManifest: 'release-inputs.json', releaseFingerprint: digest(JSON.stringify(release))};
  await write(previous, 'source-provenance.json', provenance);
  const provenanceSha256 = digest(JSON.stringify(provenance));
  const results = [];
  for (const owner of atfCollections) {
    const invocations = [{id: `[engine:junit-platform-suite]/[suite:${owner.suite}]/[engine:junit-jupiter]/[class:fixture]/[method:works()]`, outcome: 'PASS'}];
    const receipt = {kind: 'live-source-components', apkQualification: false, suite: owner.suite, tests: 1, outcomes: {PASS: 1},
      invocations, junitIdentitySha256: digest(JSON.stringify(invocations)), coverageErrors: [], containerFailures: [], provenanceSha256, reportSha256: digest(extent)};
    await fs.mkdir(path.join(previous, owner.group));
    await write(previous, owner.group + '/execution.json', receipt);
    await fs.writeFile(path.join(previous, owner.group + '/component-report.html'), extent);
    results.push({module: owner.group, exitCode: 0, receipt});
  }
  const run = {...provenance, provenanceReceiptSha256: provenanceSha256, sourceStable: true, status: 'PASS', results};
  await write(previous, 'source-component-run.json', run);
  return {previous, target, run, provenance, provenanceSha256};
}

test('unchanged receipts preserve original JUnit identities, Extent bytes and hashes', async t => {
  const {previous, target} = await fixture(t);
  const decisions = await prepareComponentReuse(target, previous, atfGroups);
  assert.equal(decisions.find(row => row.group === 'atf-tests-documentation').decision, 'RUN');
  for (const owner of atfCollections.filter(row => !row.releaseSnapshot)) {
    assert.equal(decisions.find(row => row.group === owner.group).decision, 'REUSE', owner.group);
    for (const name of ['execution.json', 'component-report.html']) {
      assert.deepEqual(await fs.readFile(path.join(target, owner.group, name)), await fs.readFile(path.join(previous, owner.group, name)));
    }
  }
});

test('payload changes invalidate the dossier owner without replaying independent fixtures', async t => {
  const {previous, target} = await fixture(t, {payload: true});
  const decisions = await prepareComponentReuse(target, previous, atfGroups);
  assert.equal(decisions.find(row => row.group === 'atf-tests-documentation').reason, 'GOVERNANCE_RELEASE_SNAPSHOT_CHANGED');
  assert.equal(decisions.filter(row => row.decision === 'REUSE').length, atfGroups.length - 1);
});

test('forged collection suites, duplicate identities and changed reports are rejected', async t => {
  const {run, provenance, provenanceSha256, previous, target} = await fixture(t);
  const origin = {sourceSet: baseline, provenance: {value: provenance, sha256: provenanceSha256}, run};
  const name = atfGroups[0], receipt = run.results[0].receipt;
  for (const patch of [{suite: 'old.MonolithicSuite'}, {invocations: []}, {junitIdentitySha256: '0'.repeat(64)}]) {
    assert.throws(() => verifyComponentExecution(origin, name, {...receipt, ...patch}, 1, digest(extent)), /ATF_COLLECTION_IDENTITY_MISMATCH/);
  }
  await fs.appendFile(path.join(previous, name, 'component-report.html'), 'tampered');
  const decisions = await prepareComponentReuse(target, previous, atfGroups);
  assert.equal(decisions.find(row => row.group === name).decision, 'INVALID');
});
