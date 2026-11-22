import {isDeepStrictEqual} from 'node:util';
import {componentInputMap} from './source-input-identity.mjs';
import {changedInputs, nonBuildDocumentation} from './component-evidence-scope.mjs';

export const sourceCheckReceipt = 'source-snapshot-check.json';
export const sourceCheckObservations = Object.freeze([
  'observed-staged-source.json', 'observed-staged-release.json',
  'observed-live-source.json', 'observed-live-release.json'
]);
export const sourceCheckFiles = Object.freeze([sourceCheckReceipt, ...sourceCheckObservations]);

/** Admit an original rejected aggregate only as evidence for independently passing owners. */
export function verifyDocumentationDrift(inputs, run, provenance, documents) {
  const check = documents?.[0];
  if (run?.sourceStable !== false || run.status !== 'FAIL' || run.sourceCheckReceipt !== sourceCheckReceipt
      || check?.sha256 !== run.sourceCheckReceiptSha256 || documents?.length !== sourceCheckFiles.length) {
    throw Error('COMPONENT_SOURCE_DRIFT_EVIDENCE_REQUIRED');
  }
  const receipt = check.value;
  if (receipt?.schemaVersion !== 1 || receipt.kind !== 'ATF_COMPONENT_SOURCE_CHECK'
      || receipt.expectedSourceSha256 !== inputs.sha256
      || ![run, provenance.value].every(row => row.releaseInputManifest === 'release-inputs.json'
        && row.releaseFingerprint === receipt.expectedReleaseSha256)
      || !Array.isArray(receipt.observations) || receipt.observations.length !== sourceCheckObservations.length) {
    throw Error('COMPONENT_SOURCE_DRIFT_BINDING');
  }
  for (const [index, name] of sourceCheckObservations.entries()) {
    const observed = receipt.observations[index];
    if (observed?.path !== name || observed.sha256 !== documents[index + 1]?.sha256) {
      throw Error('COMPONENT_SOURCE_DRIFT_OBSERVATION:' + name);
    }
    componentInputMap(documents[index + 1].value);
  }
  const [, staged, stagedRelease, live, liveRelease] = documents;
  if (staged.sha256 !== inputs.sha256 || stagedRelease.sha256 !== receipt.expectedReleaseSha256
      || liveRelease.sha256 !== receipt.expectedReleaseSha256) {
    throw Error('COMPONENT_SOURCE_DRIFT_STAGED_OR_RELEASE_CHANGED');
  }
  const changed = changedInputs(componentInputMap(live.value), componentInputMap(inputs.value));
  if (!changed.length || !changed.every(nonBuildDocumentation)) {
    throw Error('COMPONENT_SOURCE_DRIFT_NOT_DOCUMENTATION');
  }
  const differences = changed.map(file => 'LIVE-SOURCE: ' + file);
  if (!isDeepStrictEqual(receipt.differences, differences) || !isDeepStrictEqual(run.snapshotDifferences, differences)) {
    throw Error('COMPONENT_SOURCE_DRIFT_DIAGNOSTICS');
  }
  return {kind: 'DOCUMENTATION_ONLY_SOURCE_DRIFT', rejectedRunStatus: run.status,
    sourceCheckSha256: check.sha256, stagedInputsUnchanged: true, observedDocumentationChanges: changed};
}
