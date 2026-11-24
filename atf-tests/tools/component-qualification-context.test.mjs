import assert from 'node:assert/strict';
import fs from 'node:fs/promises';
import os from 'node:os';
import path from 'node:path';
import {fileURLToPath} from 'node:url';
import {execFileSync} from 'node:child_process';
import test from 'node:test';
import {qualificationContext} from './component-qualification-context.mjs';
import {digest} from './source-input-identity.mjs';
import {verifyComponentOrigin, verifyReleaseInputs, verifyGovernanceReleaseSnapshot} from './evaluate-release-rag.mjs';

const document = value => { const bytes = Buffer.from(JSON.stringify(value)); return {value, bytes, sha256: digest(bytes)}; };
function fixture() {
  const source = document([{path: 'README.md', sha256: digest('source')}]);
  const release = document([{path: 'release/index.json', sha256: digest('catalog')},
    {path: 'release/current/game.apk', sha256: digest('apk')}]);
  const context = qualificationContext(source.sha256, release.sha256, release.value);
  const fields = {schemaVersion: 2, kind: 'live-source-components', sourceFingerprint: source.sha256,
    inputCount: source.value.length, inputManifest: 'source-inputs.json', releaseInputManifest: 'release-inputs.json',
    releaseFingerprint: release.sha256, qualificationContext: context};
  const provenance = document({...fields});
  const run = {...fields, sourceStable: true, status: 'PASS', provenanceReceiptSha256: provenance.sha256,
    results: [{module: 'atf-tests-documentation', exitCode: 0}]};
  const origin = () => verifyComponentOrigin(source, run, provenance);
  return {source, release, context, provenance, run, origin};
}

test('qualified source and catalog context reconstructs exactly while later publication remains a separate observation', () => {
  const f = fixture(), origin = f.origin();
  verifyReleaseInputs(origin, f.release);
  assert.deepEqual(origin.qualificationContext, f.context);
  const published = document([...f.release.value, {path: 'release/current/new-audit.html', sha256: digest('later output')}]);
  assert.notDeepEqual(qualificationContext(f.source.sha256, published.sha256, published.value), f.context);
  assert.throws(() => verifyReleaseInputs(origin, published), /COMPONENT_RELEASE_SNAPSHOT_BINDING/);
  assert.deepEqual(f.provenance.value.qualificationContext, f.context);
});

test('missing foreign altered or downgraded qualified context cannot bind a modern origin', () => {
  for (const mutation of ['missing', 'foreign-source', 'foreign-release', 'catalog', 'identity', 'extra', 'schema', 'downgrade', 'no-release']) {
    const f = fixture();
    if (mutation === 'missing') delete f.run.qualificationContext;
    else if (mutation === 'foreign-source') f.run.qualificationContext = {...f.context, sourceSha256: digest('foreign')};
    else if (mutation === 'foreign-release') f.run.qualificationContext = {...f.context, releaseSha256: digest('foreign')};
    else if (mutation === 'catalog') f.run.qualificationContext = {...f.context, catalogSha256: digest('foreign')};
    else if (mutation === 'identity') f.run.qualificationContext = {...f.context, identitySha256: digest('foreign')};
    else if (mutation === 'extra') f.run.qualificationContext = {...f.context, unbound: true};
    else if (mutation === 'schema') f.run.schemaVersion = 3;
    else if (mutation === 'downgrade') f.run.schemaVersion = 1;
    else if (mutation === 'no-release') delete f.run.releaseInputManifest;
    assert.throws(() => verifyReleaseInputs(f.origin(), f.release), /COMPONENT_(?:CONTEXT|RELEASE)/, mutation);
  }
});

test('historical receipts stay explicit and immutable instead of acquiring an invented qualified context', () => {
  const f = fixture();
  for (const row of [f.run, f.provenance.value]) { row.schemaVersion = 1; delete row.qualificationContext; }
  const oldProvenance = document(f.provenance.value);
  f.run.provenanceReceiptSha256 = oldProvenance.sha256;
  const before = JSON.stringify(f.run);
  const origin = verifyComponentOrigin(f.source, f.run, oldProvenance);
  verifyReleaseInputs(origin, f.release);
  assert.equal(origin.qualificationContext, null);
  assert.equal(JSON.stringify(f.run), before);
});

test('live catalog drift missing files and corrupt APK bytes are rejected independently of the original context', async t => {
  const root = await fs.mkdtemp(path.join(os.tmpdir(), 'whist-catalog-context-'));
  t.after(() => fs.rm(root, {recursive: true, force: true}));
  const f = fixture(), origin = f.origin();
  verifyReleaseInputs(origin, f.release);
  await fs.mkdir(path.join(root, 'release/current'), {recursive: true});
  await fs.writeFile(path.join(root, 'release/index.json'), 'catalog');
  await fs.writeFile(path.join(root, 'release/current/game.apk'), 'apk');
  const files = verifyReleaseInputs(origin, f.release);
  await verifyGovernanceReleaseSnapshot(root, files);
  for (const [file, text] of [['release/index.json', 'changed'], ['release/current/game.apk', 'bad']]) {
    await fs.writeFile(path.join(root, file), text);
    await assert.rejects(verifyGovernanceReleaseSnapshot(root, files), /COMPONENT_RELEASE_SNAPSHOT_CHANGED/);
    await fs.writeFile(path.join(root, file), file.endsWith('.apk') ? 'apk' : 'catalog');
  }
  await fs.unlink(path.join(root, 'release/current/game.apk'));
  await assert.rejects(verifyGovernanceReleaseSnapshot(root, files), /COMPONENT_RELEASE_SNAPSHOT_CHANGED/);
  assert.deepEqual(origin.qualificationContext, f.context);
});

test('PowerShell snapshot and restart reconstruct the same context and refuse missing or foreign workspace identity', async t => {
  const root = await fs.mkdtemp(path.join(os.tmpdir(), 'whist-catalog-resume-'));
  t.after(() => fs.rm(root, {recursive: true, force: true}));
  const f = fixture();
  await fs.writeFile(path.join(root, 'source-inputs.json'), JSON.stringify(f.source.value));
  await fs.writeFile(path.join(root, 'release-inputs.json'), JSON.stringify(f.release.value));
  const workspace = {schemaVersion: 2, sourceFingerprint: f.source.sha256, releaseFingerprint: f.release.sha256,
    qualificationContext: f.context};
  const project = fileURLToPath(new URL('../../', import.meta.url));
  const program = path.join(root, 'resume.ps1');
  await fs.writeFile(program, `param($Project,$Root)
. (Join-Path $Project 'scripts/AtfQualificationSnapshot.ps1')
. (Join-Path $Project 'scripts/AtfComponentResume.ps1')
function Assert-AtfQualificationSnapshot { }
$snapshot=Restore-AtfComponentSnapshot $Root $Root $Root ''
Get-AtfQualifiedCatalogContext $snapshot | ConvertTo-Json -Compress
`);
  for (const mutation of ['valid', 'foreign', 'missing']) {
    const value = structuredClone(workspace);
    if (mutation === 'foreign') value.qualificationContext.sourceSha256 = digest('foreign');
    if (mutation === 'missing') delete value.qualificationContext;
    await fs.writeFile(path.join(root, 'component-workspace.json'), JSON.stringify(value));
    const invoke = () => execFileSync('pwsh', ['-NoProfile', '-NonInteractive', '-File', program, project, root],
      {encoding: 'utf8', timeout: 15000, stdio: ['pipe', 'pipe', 'pipe']});
    if (mutation === 'valid') assert.deepEqual(JSON.parse(invoke()), f.context);
    else assert.throws(invoke, error => /COMPONENT_RESUME_CONTEXT/.test(error.stderr.toString()), mutation);
  }
});
