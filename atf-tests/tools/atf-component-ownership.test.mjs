import {runFixtureProcess} from './fixture-process.mjs';
import assert from 'node:assert/strict';
import fs from 'node:fs/promises';
import os from 'node:os';
import path from 'node:path';
import test from 'node:test';
import {fileURLToPath} from 'node:url';
import {atfCollections, atfGroups, atfOwnershipPath, atfComponentGroups} from './atf-component-ownership.mjs';
import {componentEvidenceScope, changedInputs} from './component-evidence-scope.mjs';
import {deriveReleaseTestScope} from './release-test-scope.mjs';
import {digest, verifyComponentExecution, verifyComponentOrigin, strictJson} from './evaluate-release-rag.mjs';
import {prepareComponentReuse} from './prepare-component-reuse.mjs';
import {sourceCheckFiles, sourceCheckObservations, sourceCheckReceipt} from './component-source-check.mjs';
import {atfReaderSealsPath} from './atf-component-dependencies.mjs';

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
  for (const file of ['docs/testing/guide.md', 'src/test/README.md', 'whist-lab-app/game-core/src/test/java/README.md']) {
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

test('release-tools changes select their Maven component owner without Android journeys', () => {
  const previous = new Map([...baseline, ['release-tools/src/main/java/Gate.java', 'a'.repeat(64)]]);
  const candidate = new Map([...previous, ['release-tools/src/main/java/Gate.java', 'b'.repeat(64)]]);
  const groups = [...atfGroups, 'atf-validation', 'atf-test-android', 'release-tools',
    'whist-lab-app-bidding', 'whist-lab-app-display'];
  const selection = deriveReleaseTestScope(previous, candidate, groups);
  assert.deepEqual(selection.runComponentGroups, [
    'atf-tests', 'atf-tests-documentation', 'atf-tests-provenance', 'atf-tests-android',
    'atf-tests-github', 'atf-tests-orchestration', 'atf-tests-reporting', 'release-tools'
  ]);
  assert.deepEqual(selection.requiredE2eGroups, []);
  assert.equal(selection.userFlowChanged, false);
  assert.equal(componentEvidenceScope('release-tools', candidate, previous).decision, 'RUN');
  assert.equal(componentEvidenceScope('whist-lab-app-bidding', candidate, previous).decision, 'REUSE');
});

test('the Maven aggregator does not invalidate unrelated Gradle product components', () => {
  const groups = [...atfGroups, 'atf-validation', 'atf-test-android', 'release-tools',
    'whist-lab-app-bidding', 'whist-lab-app-display'];
  const previous = new Map([...baseline, ['pom.xml', 'a'.repeat(64)]]);
  const candidate = new Map([...previous, ['pom.xml', 'b'.repeat(64)]]);
  const selection = deriveReleaseTestScope(previous, candidate, groups);
  assert.deepEqual(selection.runComponentGroups, [...atfGroups, 'atf-validation', 'atf-test-android', 'release-tools']);
  assert.equal(selection.components.find(row => row.group === 'whist-lab-app-bidding').decision, 'REUSE');
  assert.equal(selection.components.find(row => row.group === 'whist-lab-app-display').decision, 'REUSE');
  assert.deepEqual(selection.requiredE2eGroups, []);
});

test('shared Markdown fixture payloads keep test consumers active', () => {
  for (const file of ['src/test/resources/README.md', 'atf-tests/src/test/resources/case.md',
    'whist-lab-app/game-core/src/test/resources/case.md', 'whist-lab-app/app/src/main/assets/README.md']) {
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

async function fixture(t, {payload = false, sourceInputs = baseline, targetInputs = current} = {}) {
  const root = await fs.mkdtemp(path.join(os.tmpdir(), 'whist-atf-owners-'));
  t.after(() => fs.rm(root, {recursive: true, force: true}));
  const previous = path.join(root, 'previous'), target = path.join(root, 'target');
  await fs.mkdir(previous); await fs.mkdir(target);
  const write = (dir, name, data) => fs.writeFile(path.join(dir, name), JSON.stringify(data));
  const rows = [...sourceInputs].map(([path, sha256]) => ({path, sha256}));
  await write(previous, 'source-inputs.json', rows);
  await write(target, 'source-inputs.json', [...targetInputs].map(([path, sha256]) => ({path, sha256})));
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

test('reviewed reader updates reuse unrelated original receipts through the live graph and release planner', async t => {
  const rows = JSON.parse(runFixtureProcess('pwsh', ['-NoProfile', '-NonInteractive', '-File',
    fileURLToPath(new URL('../../scripts/Get-AtfSourceInputs.ps1', import.meta.url)),
    '-SourceRoot', path.resolve(fileURLToPath(new URL('../../', import.meta.url))), '-AsJson'],
  {encoding: 'utf8', timeout: 30000, maxBuffer: 4 * 1024 * 1024}));
  const targetInputs = new Map(rows.map(row => [row.path, row.sha256]));
  const sourceInputs = new Map(targetInputs);
  // Controlled prior body on the same admitted graph, with a distinct reviewed seal.
  const reader = 'atf-test-android/src/main/java/atf/android/AdbClient.java';
  sourceInputs.set(reader, digest('prior reader body'));
  sourceInputs.set(atfReaderSealsPath, digest('prior reviewed seals'));
  const f = await fixture(t, {sourceInputs, targetInputs});
  const plan = deriveReleaseTestScope(sourceInputs, targetInputs, atfGroups);
  const affected = ['atf-tests', 'atf-tests-documentation', 'atf-tests-provenance',
    'atf-tests-android', 'atf-tests-github', 'atf-tests-orchestration', 'atf-tests-reporting'];
  assert.deepEqual(plan.runComponentGroups, affected);
  assert.deepEqual(plan.requiredE2eGroups, []);
  const decisions = await prepareComponentReuse(f.target, f.previous, atfGroups);
  for (const group of affected) assert.equal(decisions.find(row => row.group === group).decision, 'RUN', group);
  for (const row of decisions.filter(row => !affected.includes(row.group))) {
    assert.equal(row.decision, 'REUSE', row.group);
    assert(row.dependencyEvidence.some(e => e.path === atfReaderSealsPath
      && e.reason === 'REVIEWED_READER_SEAL_CONSUMERS' && e.changedReaders.includes(reader)));
    for (const name of ['execution.json', 'component-report.html'])
      assert.deepEqual(await fs.readFile(path.join(f.target, row.group, name)), await fs.readFile(path.join(f.previous, row.group, name)));
  }
  t.diagnostic(JSON.stringify({qualifiedOwners: atfGroups.length, execute: plan.runComponentGroups,
    reused: decisions.filter(row => row.decision === 'REUSE').map(row => row.group), originalReportBytesPreserved: true}));
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

// Controlled observations: production captures these through the real snapshot checker.
async function documentationDrift(fixture, patch = {}, liveRows = [...current].map(([path, sha256]) => ({path, sha256}))) {
  const {previous, run} = fixture;
  const read = name => fs.readFile(path.join(previous, name));
  const source = await read('source-inputs.json'), release = await read('release-inputs.json');
  const observations = [source, release, Buffer.from(JSON.stringify(liveRows)), release];
  for (const [index, name] of sourceCheckObservations.entries()) await fs.writeFile(path.join(previous, name), observations[index]);
  const differences = changedInputs(new Map(liveRows.map(row => [row.path,row.sha256])),
    new Map(JSON.parse(source).map(row => [row.path,row.sha256]))).map(file => 'LIVE-SOURCE: ' + file);
  const check = {schemaVersion: 1, kind: 'ATF_COMPONENT_SOURCE_CHECK', expectedSourceSha256: digest(source),
    expectedReleaseSha256: digest(release), observations: sourceCheckObservations.map((name, index) => ({path: name, sha256: digest(observations[index])})), differences};
  await fs.writeFile(path.join(previous, sourceCheckReceipt), JSON.stringify(check));
  const rejected = {...run, sourceStable: false, status: 'FAIL', snapshotDifferences: differences,
    sourceCheckReceipt, sourceCheckReceiptSha256: digest(JSON.stringify(check)), ...patch};
  await fs.writeFile(path.join(previous, 'source-component-run.json'), JSON.stringify(rejected));
  return {read, check, rejected};
}

test('documentation drift readmits six unchanged owners and preserves the rejected aggregate and every original byte', async t => {
  const f = await fixture(t);
  const {read} = await documentationDrift(f);
  const rejected = await read('source-component-run.json');
  const decisions = await prepareComponentReuse(f.target, f.previous, atfGroups);
  assert.equal(decisions.find(row => row.group === 'atf-tests-documentation').decision, 'RUN');
  assert.equal(decisions.filter(row => row.decision === 'REUSE').length, atfGroups.length - 1);
  for (const row of decisions.filter(row => row.decision === 'REUSE')) {
    assert.equal(row.readmission.rejectedRunStatus, 'FAIL');
    assert.deepEqual(row.readmission.observedDocumentationChanges, ['README.md']);
    for (const name of ['execution.json', 'component-report.html'])
      assert.deepEqual(await fs.readFile(path.join(f.target, row.group, name)), await read(row.group + '/' + name));
    for (const name of ['source-component-run.json', ...sourceCheckFiles])
      assert.deepEqual(await fs.readFile(path.join(f.target, 'snapshots', row.componentEvidenceOrigin, name)), await read(name));
  }
  assert.deepEqual(await read('source-component-run.json'), rejected);
  const documents = await Promise.all(sourceCheckFiles.map(name => strictJson(path.join(f.previous, name))));
  const origin = verifyComponentOrigin(await strictJson(path.join(f.previous, 'source-inputs.json')),
    JSON.parse(rejected), await strictJson(path.join(f.previous, 'source-provenance.json')), 'source-inputs.json', documents);
  assert.equal(origin.run.status, 'FAIL');
  assert.equal(origin.readmission.kind, 'DOCUMENTATION_ONLY_SOURCE_DRIFT');
  // A second reuse follows the original rejected origin, never an invented passing receipt.
  const next = path.join(path.dirname(f.target), 'next'); await fs.mkdir(next);
  for (const file of ['source-inputs.json', 'release-inputs.json'])
    await fs.copyFile(path.join(f.target, file), path.join(next, file));
  const repeated = await prepareComponentReuse(next, f.target, atfGroups.filter(group => group !== 'atf-tests-documentation'));
  assert(repeated.every(row => row.decision === 'REUSE' && row.readmission.rejectedRunStatus === 'FAIL'));
});

for (const scenario of ['missing-check', 'missing-observation', 'legacy-run', 'check-hash', 'observation-hash', 'observation-path', 'duplicate-input',
  'staged-source', 'staged-release', 'live-release', 'live-code', 'resource-markdown', 'unknown-markdown',
  'diagnostics', 'aggregate-diagnostics', 'wrong-origin']) {
  test('documentation drift rejects ' + scenario + ' without copying any evidence', async t => {
    const f = await fixture(t);
    const {read, check, rejected} = await documentationDrift(f);
    const write = (name, value) => fs.writeFile(path.join(f.previous, name), JSON.stringify(value));
    const replaceObservation = async (index, mutate) => {
      const rows = JSON.parse(await read(sourceCheckObservations[index])); mutate(rows);
      await write(sourceCheckObservations[index], rows);
      check.observations[index].sha256 = digest(JSON.stringify(rows));
    };
    if (scenario === 'missing-check') await fs.unlink(path.join(f.previous, sourceCheckReceipt));
    else if (scenario === 'missing-observation') await fs.unlink(path.join(f.previous, sourceCheckObservations[2]));
    else if (scenario === 'legacy-run') { delete rejected.sourceCheckReceipt; delete rejected.sourceCheckReceiptSha256; }
    else if (scenario === 'check-hash') rejected.sourceCheckReceiptSha256 = '0'.repeat(64);
    else if (scenario === 'observation-hash') await fs.appendFile(path.join(f.previous, sourceCheckObservations[2]), ' ');
    else if (scenario === 'observation-path') check.observations[2].path = '../foreign.json';
    else if (scenario === 'duplicate-input') await replaceObservation(2, rows => rows.push(rows[0]));
    else if (scenario === 'staged-source') await replaceObservation(0, rows => rows[0].sha256 = 'f'.repeat(64));
    else if (scenario === 'staged-release') await replaceObservation(1, rows => rows[0].sha256 = 'f'.repeat(64));
    else if (scenario === 'live-release') await replaceObservation(3, rows => rows[0].sha256 = 'f'.repeat(64));
    else if (['live-code', 'resource-markdown', 'unknown-markdown'].includes(scenario)) {
      const paths = {'live-code':'module/src/main/Changed.java', 'resource-markdown':'module/src/test/resources/README.md', 'unknown-markdown':'unknown/data.md'};
      await replaceObservation(2, rows => rows.push({path:paths[scenario],sha256:'f'.repeat(64)}));
    } else if (scenario === 'diagnostics') check.differences = [];
    else if (scenario === 'aggregate-diagnostics') rejected.snapshotDifferences = [];
    else if (scenario === 'wrong-origin') check.expectedSourceSha256 = 'f'.repeat(64);
    if (!['missing-check', 'check-hash'].includes(scenario)) {
      await write(sourceCheckReceipt, check);
      if (scenario !== 'legacy-run') rejected.sourceCheckReceiptSha256 = digest(JSON.stringify(check));
    }
    await write('source-component-run.json', rejected);
    const before = await read('source-component-run.json');
    const decisions = await prepareComponentReuse(f.target, f.previous, atfGroups);
    assert(decisions.every(row => row.decision === 'INVALID'), scenario);
    await assert.rejects(fs.stat(path.join(f.target, 'snapshots')), {code:'ENOENT'});
    assert.deepEqual(await read('source-component-run.json'), before);
  });
}

test('documentation drift cannot rescue a failed sibling or a changed report and still schedules later code changes', async t => {
  const f = await fixture(t);
  const {read, rejected} = await documentationDrift(f);
  rejected.results.find(row => row.module === 'atf-tests-github').exitCode = 1;
  await fs.writeFile(path.join(f.previous, 'source-component-run.json'), JSON.stringify(rejected));
  await fs.appendFile(path.join(f.previous, 'atf-tests-android/component-report.html'), 'tampered');
  const decisions = await prepareComponentReuse(f.target, f.previous, atfGroups);
  assert.equal(decisions.find(row => row.group === 'atf-tests-github').decision, 'INVALID');
  assert.equal(decisions.find(row => row.group === 'atf-tests-android').decision, 'INVALID');
  assert.equal(decisions.find(row => row.group === 'atf-tests-documentation').decision, 'RUN');
  assert.equal(decisions.find(row => row.group === 'atf-tests-reporting').decision, 'REUSE');
  const next = path.join(path.dirname(f.target), 'code-change'); await fs.mkdir(next);
  const rows = JSON.parse(await read('source-inputs.json'));
  rows.push({path:'unknown/runtime.bin',sha256:'c'.repeat(64)});
  await fs.writeFile(path.join(next, 'source-inputs.json'), JSON.stringify(rows));
  await fs.copyFile(path.join(f.target, 'release-inputs.json'), path.join(next, 'release-inputs.json'));
  assert((await prepareComponentReuse(next, f.previous, ['atf-tests-reporting'])).every(row => row.decision === 'RUN'));
});

for (const change of ['added', 'deleted', 'renamed', 'library-guide', 'game-rules']) {
  test('documentation drift selects the actual delivered owners for ' + change + ' guides', async t => {
    const f=await fixture(t);
    let rows=[...baseline].map(([path,sha256])=>({path,sha256}));
    if(['deleted','renamed'].includes(change)) rows=rows.filter(row=>row.path!=='README.md');
    if(change!=='deleted') rows.push({path:change==='library-guide'?'lib/README.Core.md':
      change==='game-rules'?'docs/game/ATF_RulesEn.md':'docs/new-guide.md',sha256:'e'.repeat(64)});
    await documentationDrift(f,{},rows);
    await fs.writeFile(path.join(f.target,'source-inputs.json'),JSON.stringify(rows));
    const decisions=await prepareComponentReuse(f.target,f.previous,atfGroups);
    const selected=decisions.filter(row=>row.decision==='RUN').map(row=>row.group).sort();
    assert.deepEqual(selected,change==='library-guide'?['atf-tests','atf-tests-documentation']:['atf-tests-documentation']);
    assert(decisions.filter(row=>row.decision==='REUSE').every(row=>row.readmission));
    if(change==='game-rules') assert.equal(componentEvidenceScope('whist-lab-app-rules',
      new Map(rows.map(row=>[row.path,row.sha256])),baseline).decision,'RUN');
  });
}
