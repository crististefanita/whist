import fs from 'node:fs';
import path from 'node:path';
import {fileURLToPath} from 'node:url';
import {componentInputMap, digest} from './source-input-identity.mjs';
import {analyzeAtfDependencies, atfDependencyPolicyPath, atfReaderSealsPath} from './atf-component-dependencies.mjs';
import {atfCollections} from './atf-component-ownership.mjs';

// A proposal is not an admitted graph or a qualification verdict. This command
// never changes the checked-in policy or an existing output file.
try {
  const [manifest, destination, ...extra] = process.argv.slice(2);
  if (!manifest || !destination || extra.length) throw Error('Use <source-inputs.json> <new-external-reader-policy.json>');
  const root = path.resolve(fileURLToPath(new URL('../../', import.meta.url)));
  const parent = fs.realpathSync(path.dirname(path.resolve(destination)));
  const topologyOutput = path.join(parent, path.basename(destination));
  const sealsOutput = path.join(parent, path.basename(atfReaderSealsPath));
  for (let current = parent; ; current = path.dirname(current)) {
    if (fs.existsSync(path.join(current, '.git')) || path.relative(root, current) === '') throw Error('DEPENDENCY_PROPOSAL_IN_CHECKOUT');
    if (current === path.dirname(current)) break;
  }
  if (topologyOutput.toLowerCase() === sealsOutput.toLowerCase()
      || [topologyOutput, sealsOutput].some(file => fs.existsSync(file))) throw Error('DEPENDENCY_PROPOSAL_OUTPUT_EXISTS');
  const rows = JSON.parse(fs.readFileSync(manifest, 'utf8').replace(/^\uFEFF/, ''));
  const inputs = componentInputMap(rows);
  const read = file => {
    const target = path.join(root, file);
    for (let current = target; current !== path.dirname(current); current = path.dirname(current)) {
      if (fs.lstatSync(current).isSymbolicLink()) throw Error('DEPENDENCY_INDIRECT');
    }
    const bytes = fs.readFileSync(target);
    if (digest(bytes) !== inputs.get(file)) throw Error('DEPENDENCY_SOURCE_HASH: ' + file);
    return bytes;
  };
  const policy = JSON.parse(read(atfDependencyPolicyPath));
  read('config/atf-component-collections.json');
  const model = analyzeAtfDependencies(inputs, read, policy, atfCollections);
  fs.writeFileSync(topologyOutput, JSON.stringify({...policy, topologySha256: model.topologySha256}, null, 2) + '\n', {flag: 'wx'});
  fs.writeFileSync(sealsOutput, JSON.stringify({schemaVersion: 1,
    readerSeals: model.requiredReaderSeals}, null, 2) + '\n', {flag: 'wx'});
  console.log(JSON.stringify({kind: 'ATF_DEPENDENCY_POLICY_PROPOSAL', readers: Object.keys(model.requiredReaderSeals).length,
    topologySha256: model.topologySha256, policy: topologyOutput, readerSeals: sealsOutput}));
} catch (error) { console.error(error.message); process.exitCode = 1; }
