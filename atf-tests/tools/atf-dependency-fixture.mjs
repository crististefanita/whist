import {createHash} from 'node:crypto';
import {analyzeAtfDependencies, buildAtfDependencyModel, atfDependencyPolicyPath, atfReaderSealsPath}
  from './atf-component-dependencies.mjs';

export const dependencyDigest = value => createHash('sha256').update(value).digest('hex');

export function dependencyFixture() {
  const sources = new Map([
    ['src/first/First.java', 'package first; import shared.Helper; class First { Helper value; String tool="entry.test.mjs"; }'],
    ['src/second/Second.java', 'package second; class Second { String tool="isolated.mjs"; }'],
    ['src/shared/Helper.java', 'package shared; class Helper { Peer value; }'],
    ['src/shared/Peer.java', 'package shared; class Peer {}'],
    ['tools/entry.test.mjs', "import './middle.mjs';"],
    ['tools/middle.mjs', "import './entry.test.mjs'; execFileSync('pwsh', ['bridge.ps1']);"],
    ['tools/isolated.mjs', 'export const isolated = 1;'],
    ['scripts/bridge.ps1', 'Get-SharedValue'],
    ['scripts/helper.ps1', 'function Get-SharedValue { 1 }'],
    ['scripts/unowned.ps1', 'function Get-UnownedValue { 2 }']
  ]);
  const policy = {schemaVersion: 2, javaRoots: ['src'], scriptRoots: ['tools', 'scripts'],
    globalInputs: [atfDependencyPolicyPath], observers: [], readerSealsPath: atfReaderSealsPath};
  const collections = [{group: 'first', classes: ['first.First']}, {group: 'second', classes: ['second.Second']}];
  const inputs = () => new Map([...sources].map(([file, text]) => [file, dependencyDigest(text)]));
  const read = file => Buffer.from(sources.get(file));
  const qualify = () => {
    const model = analyzeAtfDependencies(inputs(), read, policy, collections);
    policy.topologySha256 = model.topologySha256;
    sources.set(atfDependencyPolicyPath, JSON.stringify(policy));
    sources.set(atfReaderSealsPath, JSON.stringify({schemaVersion: 1, readerSeals: model.requiredReaderSeals}));
  };
  qualify();
  const model = (rows = inputs()) => buildAtfDependencyModel(rows, read, policy, collections);
  return {sources, policy, collections, inputs, read, qualify, model};
}
