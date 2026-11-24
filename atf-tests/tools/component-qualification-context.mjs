import {componentInputMap, digest} from './source-input-identity.mjs';

// Catalog outputs can change at publication. This identity describes exactly
// what the component execution observed, independently of later live checks.
export function qualificationContext(sourceSha256, releaseSha256, rows) {
  const files = componentInputMap(rows);
  const catalogSha256 = files.get('release/index.json');
  if (![sourceSha256, releaseSha256, catalogSha256].every(value => /^[a-f\d]{64}$/.test(value ?? ''))
      || [...files.keys()].some(file => !file.startsWith('release/'))) throw Error('COMPONENT_CONTEXT_INPUTS');
  return {schemaVersion: 1, kind: 'QUALIFIED_SOURCE_AND_CATALOG', sourceSha256, releaseSha256, catalogSha256,
    identitySha256: digest([sourceSha256, releaseSha256, catalogSha256].join('\n'))};
}

export function verifyQualificationContext(origin, release) {
  const {run, provenance, sourceFingerprint} = origin;
  const receipts = [run, provenance.value];
  if (receipts.every(row => row.schemaVersion === undefined || row.schemaVersion === 1)
      && receipts.every(row => row.qualificationContext === undefined)) return null;
  if (receipts.some(row => row.schemaVersion !== 2)) throw Error('COMPONENT_CONTEXT_SCHEMA');
  const expected = qualificationContext(sourceFingerprint, release.sha256, release.value);
  for (const row of receipts) {
    const context = row.qualificationContext;
    if (!context || Object.keys(context).sort().join() !== Object.keys(expected).sort().join()
        || Object.entries(expected).some(([key, value]) => context[key] !== value)) throw Error('COMPONENT_CONTEXT_BINDING');
  }
  return expected;
}
