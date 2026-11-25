import fs from 'node:fs/promises';
import path from 'node:path';
import {fileURLToPath} from 'node:url';
import {regularFile, strictJson} from './evaluate-release-rag.mjs';
import {releaseVersionPattern} from './release-version.mjs';
import {androidFlows, androidExecutionProfile, e2ePaths, signedE2eProtocol} from './android-e2e-contract.mjs';
import {digest} from './source-input-identity.mjs';

// Metadata admission deliberately never opens APKs, report HTML or execution receipts.
export async function inspectReleaseMetadata(project) {
  const root = await fs.realpath(project);
  const inputs = [];
  const read = async name => {
    const document = await strictJson(await regularFile(root, name));
    inputs.push({path:name,sha256:document.sha256});
    return document.value;
  };
  const catalog = await read('release/index.json');
  const versions = catalog.versions;
  if(catalog.schemaVersion !== 2 || !Array.isArray(versions) || versions.length < 1 || versions.length > 2
      || versions.some(version => typeof version !== 'string' || !releaseVersionPattern.test(version))
      || new Set(versions).size !== versions.length || catalog.currentVersion !== versions[0]) throw Error('RELEASE-METADATA-CATALOG');
  inputs.push({path:'release/README.md',sha256:digest(await fs.readFile(await regularFile(root,'release/README.md')))});
  let active;
  for(const version of versions) {
    for(const name of ['README.md','reports/README.md']) await regularFile(root,`release/${version}/${name}`);
    const manifest = await read(`release/${version}/release-validation.json`);
    if(manifest.release?.versionName !== version) throw Error('RELEASE-METADATA-VERSION:' + version);
    if(version === catalog.currentVersion) active = manifest;
  }
  const decision = active.qualification?.e2e;
  const declared = active.qualification?.e2eGroups;
  if(!declared || typeof declared !== 'object' || Array.isArray(declared)
      || !decision || !['PASS','NOT_RUN','NOT_APPLICABLE'].includes(decision.status)) throw Error('RELEASE-METADATA-E2E-DECISION');
  const groups = Object.keys(declared).sort();
  if(decision.status === 'PASS') {
    if(!Array.isArray(decision.groups) || !groups.length || decision.groups.some(name => typeof name !== 'string')
        || new Set(decision.groups).size !== decision.groups.length
        || JSON.stringify([...decision.groups].sort()) !== JSON.stringify(groups)) throw Error('RELEASE-METADATA-E2E-INVENTORY');
    const profile = decision.executionProfile;
    androidExecutionProfile(profile?.api,profile?.displayProfile,profile?.responsiveProfiles);
    for(const name of groups) {
      const group = declared[name], expected = e2ePaths(name);
      if(!Object.hasOwn(androidFlows,name) || group?.protocol !== signedE2eProtocol || group.tests !== 1
          || group.failures !== 0 || group.errors !== 0 || group.skipped !== 0
          || group.report !== expected.report || group.executionReceipt !== expected.executionReceipt) throw Error('RELEASE-METADATA-E2E-GROUP:' + name);
    }
  } else if(groups.length || decision.groups !== undefined && (!Array.isArray(decision.groups) || decision.groups.length)
      || typeof decision.rationale !== 'string' || decision.rationale.trim().length < 12) throw Error('RELEASE-METADATA-E2E-RATIONALE');
  return {schemaVersion:1,kind:'ATF_RELEASE_METADATA_PREFLIGHT',status:'PASS',releaseVersion:catalog.currentVersion,
    inputs,e2eGroups:groups,editorialFiles:['release/README.md'],qualification:false,
    limit:'Metadata only. APK/report bytes, original outcomes, full editorial governance and final qualification remain mandatory.'};
}

export function verifyMetadataSnapshot(expected, observed) {
  if(expected?.schemaVersion !== 1 || expected.kind !== 'ATF_RELEASE_METADATA_PREFLIGHT' || expected.status !== 'PASS'
      || JSON.stringify(expected.inputs) !== JSON.stringify(observed.inputs)) throw Error('RELEASE-METADATA-SNAPSHOT-CHANGED');
  return observed;
}

if(process.argv[1] && path.resolve(process.argv[1]) === fileURLToPath(import.meta.url)) {
  try { if(process.argv.length !== 3 && !(process.argv.length === 5 && process.argv[3] === '--against')) throw Error('Use: release-metadata-preflight.mjs <source> [--against receipt]');
    const observed=await inspectReleaseMetadata(process.argv[2]);
    if(process.argv.length===5)verifyMetadataSnapshot(JSON.parse(await fs.readFile(process.argv[4],'utf8')),observed);
    console.log(JSON.stringify(observed));
  } catch(error) { console.error(error.message); process.exitCode=1; }
}
