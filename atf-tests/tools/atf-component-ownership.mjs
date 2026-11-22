import {readFileSync} from 'node:fs';
import {createHash} from 'node:crypto';
import {fileURLToPath} from 'node:url';

export const atfOwnershipPath = 'config/atf-component-collections.json';
const bytes = readFileSync(fileURLToPath(new URL('../../config/atf-component-collections.json', import.meta.url)));
const catalog = JSON.parse(bytes);
const hash = createHash('sha256').update(bytes).digest('hex');
if (catalog.schemaVersion !== 1 || !Array.isArray(catalog.collections) || !catalog.collections.length
    || catalog.collections.some(row => row.module !== 'atf-tests' || !/^atf-tests(?:-[a-z]+)?$/.test(row.group)
      || !row.suite || !Array.isArray(row.classes) || !row.classes.length)
    || new Set(catalog.collections.map(row => row.group)).size !== catalog.collections.length
    || new Set(catalog.collections.flatMap(row => row.classes)).size !== catalog.collections.flatMap(row => row.classes).length
    || catalog.collections.filter(row => row.releaseSnapshot).map(row => row.group).join() !== 'atf-tests-documentation') {
  throw Error('INVALID_ATF_COMPONENT_OWNERSHIP');
}
export const atfCollections = catalog.collections;
export const atfGroups = atfCollections.map(row => row.group);
export const hasAtfCollections = inputs => inputs.has(atfOwnershipPath);
export const atfComponentGroups = inputs => hasAtfCollections(inputs) ? [...atfGroups] : ['atf-tests'];
export const releaseSnapshotRequired = (group, inputs) => hasAtfCollections(inputs)
    ? atfCollections.some(row => row.group === group && row.releaseSnapshot) : group === 'atf-tests';

// Never reinterpret the old monolithic receipt as a new, narrower owner.
export function atfOwnershipCompatible(candidate, previous) {
  return candidate.get(atfOwnershipPath) === previous.get(atfOwnershipPath)
      && (!hasAtfCollections(candidate) || candidate.get(atfOwnershipPath) === hash);
}
