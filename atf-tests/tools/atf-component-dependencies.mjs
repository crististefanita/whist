import fs from 'node:fs';
import path from 'node:path';
import {fileURLToPath} from 'node:url';
import {createHash} from 'node:crypto';
import {referenceIndex, sourceReferences, opaqueReader, readerBody} from './atf-dependency-references.mjs';
import {atfCollections, atfGroups} from './atf-component-ownership.mjs';
import {componentInputMap} from './source-input-identity.mjs';

export const atfDependencyPolicyPath = 'config/atf-component-dependencies.json';
export const atfReaderSealsPath = 'config/atf-component-reader-seals.json';
const root = fileURLToPath(new URL('../../', import.meta.url));
const hash = bytes => createHash('sha256').update(bytes).digest('hex');
const relative = value => typeof value === 'string' && value && !value.includes('\\')
  && !value.startsWith('/') && !value.includes(':') && value.split('/').every(part => part && part !== '.' && part !== '..');
export function dependencyPattern(pattern) {
  if (!relative(pattern)) throw Error('DEPENDENCY_PATTERN');
  let expression = '';
  for (let at = 0; at < pattern.length; at++) {
    if (pattern.slice(at, at + 3) === '**/') { expression += '(?:.*/)?'; at += 2; }
    else if (pattern.slice(at, at + 2) === '**') { expression += '.*'; at++; }
    else if (pattern[at] === '*') expression += '[^/]*';
    else expression += pattern[at].replace(/[\\^$.*+?()[\]{}|]/g, '\\$&');
  }
  return new RegExp('^' + expression + '$');
}

export function analyzeAtfDependencies(inputs, read, policy, collections) {
  inputs = componentInputMap([...inputs].map(([path, sha256]) => ({path, sha256})));
  if (policy.schemaVersion !== 2 || policy.readerSealsPath !== atfReaderSealsPath
      || !Array.isArray(policy.javaRoots) || !policy.javaRoots.length
      || !Array.isArray(policy.scriptRoots) || !policy.scriptRoots.length
      || !Array.isArray(policy.globalInputs) || !policy.globalInputs.length || !Array.isArray(policy.observers)
      || [...policy.javaRoots, ...policy.scriptRoots, ...policy.globalInputs].some(file => !relative(file))) {
    throw Error('DEPENDENCY_POLICY');
  }
  if (!Array.isArray(collections) || !collections.length
      || new Set(collections.map(row => row.group)).size !== collections.length
      || collections.some(row => !Array.isArray(row.classes) || !row.classes.length)) throw Error('DEPENDENCY_COLLECTIONS');
  const critical = [atfDependencyPolicyPath, 'config/atf-component-collections.json',
    'atf-tests/tools/atf-component-dependencies.mjs', 'atf-tests/tools/atf-dependency-references.mjs',
    'atf-tests/tools/atf-script-references.mjs', 'atf-tests/tools/component-evidence-scope.mjs'];
  if (collections.some(row => row.group === 'atf-tests') && critical.some(file => !policy.globalInputs.includes(file))) {
    throw Error('DEPENDENCY_CRITICAL_INPUT');
  }
  const nodes = new Map();
  let bytesRead = 0;
  for (const [file, expected] of inputs) {
    const java = file.endsWith('.java') && policy.javaRoots.some(prefix => file.startsWith(prefix + '/'));
    const script = /\.(?:m?js|ps1|psm1)$/.test(file) && policy.scriptRoots.some(prefix => file.startsWith(prefix + '/'));
    if (!java && !script) continue;
    const bytes = read(file);
    bytesRead += bytes.length;
    if (nodes.size >= 10000 || bytesRead > 64 * 1024 * 1024) throw Error('DEPENDENCY_ANALYSIS_BUDGET');
    if (hash(bytes) !== expected) throw Error('DEPENDENCY_SOURCE_HASH: ' + file);
    const text = bytes.toString('utf8');
    nodes.set(file, {path: file, text});
  }
  if (!nodes.size) throw Error('DEPENDENCY_EMPTY');
  const index = referenceIndex(nodes, inputs.keys());
  index.generatedModules = new Map();
  for (const generated of policy.generatedModules ?? []) {
    const source = nodes.get(generated.source);
    if (!source || !generated.specifier?.startsWith('.') || !Array.isArray(generated.inputs)
        || !generated.inputs.length) {
      throw Error('DEPENDENCY_GENERATED_MODULE');
    }
    generated.inputs.forEach(dependencyPattern);
    const key = generated.source + ':' + generated.specifier;
    if (index.generatedModules.has(key)) throw Error('DEPENDENCY_GENERATED_MODULE_DUPLICATE');
    index.generatedModules.set(key, generated);
  }
  let edgeCount = 0;
  for (const node of nodes.values()) {
    Object.assign(node, sourceReferences(node, index));
    edgeCount += node.edges.size;
    if (edgeCount > 250000) throw Error('DEPENDENCY_ANALYSIS_BUDGET');
  }
  for (const observer of policy.observers) {
    const node = nodes.get(observer.source);
    if (!node || !Array.isArray(observer.inputs) || !observer.inputs.length) throw Error('DEPENDENCY_OBSERVER');
    node.patterns.push(...observer.inputs);
  }
  const opaque = new Set([...nodes.values()].filter(node => opaqueReader(node.text) || node.patterns.length)
    .map(node => node.path));
  for (const generated of policy.generatedModules ?? []) opaque.add(generated.source);
  let changed = true;
  while (changed) {
    changed = false;
    for (const node of nodes.values()) {
      if (!opaque.has(node.path) && [...node.edges].some(edge => opaque.has(edge))) {
        opaque.add(node.path); changed = true;
      }
    }
  }
  const requiredReaderSeals = Object.fromEntries([...opaque].sort().map(file => [file, hash(readerBody(nodes.get(file).text))]));
  // Body bytes are sealed independently. Any change to a provider population,
  // executable edge, observer scope or reader role still changes global topology.
  const topologySha256 = hash(JSON.stringify([...nodes].sort(([a], [b]) => a < b ? -1 : a > b ? 1 : 0)
    .map(([file, node]) => [file, [...node.edges].sort(), [...new Set(node.patterns)].sort(), opaque.has(file)])));
  for (const node of nodes.values()) node.patterns = node.patterns.map(dependencyPattern);
  const closures = new Map();
  for (const owner of collections) {
    const selected = new Set(), patterns = [], pending = [], parents = new Map();
    for (const name of owner.classes) {
      const sources = index.types.get(name);
      if (!sources || sources.size !== 1) throw Error('DEPENDENCY_ROOT: ' + name);
      pending.push(...sources);
      for (const file of sources) parents.set(file, null);
    }
    while (pending.length) {
      const file = pending.pop();
      if (selected.has(file)) continue;
      selected.add(file);
      const node = nodes.get(file);
      if (node) {
        for (const edge of node.edges) { if (!parents.has(edge)) parents.set(edge, file); pending.push(edge); }
        patterns.push(...node.patterns.map(pattern => ({pattern, source: file})));
      } else if (/\.(?:java|m?js|ps1|psm1)$/.test(file)) {
        patterns.push({pattern: dependencyPattern('**'), source: file});
      }
    }
    closures.set(owner.group, {selected, patterns, parents});
  }
  const global = new Set(policy.globalInputs);
  return {
    nodes, closures, global, topologySha256, requiredReaderSeals, readerPaths: new Set(Object.keys(requiredReaderSeals)),
    owners(file) {
      if (global.has(file)) return collections.map(row => row.group);
      const groups = [...closures].filter(([, scope]) => scope.selected.has(file)
        || scope.patterns.some(row => row.pattern.test(file))).map(([group]) => group);
      return groups.length ? groups : null;
    },
    explain(file) {
      return [...closures].flatMap(([group, scope]) => {
        const observer = scope.selected.has(file) ? null : scope.patterns.find(row => row.pattern.test(file));
        if (!scope.selected.has(file) && !observer) return [];
        const chain = [], seen = new Set();
        for (let current = observer?.source ?? file; current && !seen.has(current); current = scope.parents.get(current)) {
          chain.unshift(current); seen.add(current);
        }
        return [{group, chain, ...(observer ? {observer: observer.pattern.source} : {})}];
      });
    }
  };
}

export function buildAtfDependencyModel(inputs, read, policy, collections) {
  const model = analyzeAtfDependencies(inputs, read, policy, collections);
  if (policy.topologySha256 !== model.topologySha256) throw Error('DEPENDENCY_TOPOLOGY_NOT_QUALIFIED');
  const bytes = read(atfReaderSealsPath);
  if (hash(bytes) !== inputs.get(atfReaderSealsPath)) throw Error('DEPENDENCY_READER_SEALS_HASH');
  const seals = JSON.parse(bytes);
  if (seals.schemaVersion !== 1 || !seals.readerSeals || typeof seals.readerSeals !== 'object'
      || Array.isArray(seals.readerSeals)) throw Error('DEPENDENCY_READER_SEAL');
  for (const [file, expected] of Object.entries({...model.requiredReaderSeals, ...seals.readerSeals})) {
    if (!model.nodes.has(file) || seals.readerSeals[file] !== model.requiredReaderSeals[file]
        || expected !== hash(readerBody(model.nodes.get(file).text))) {
      throw Error('DEPENDENCY_READER_NOT_QUALIFIED: ' + file);
    }
  }
  return model;
}

const cache = new WeakMap();
function readDirect(file) {
  if (!relative(file)) throw Error('DEPENDENCY_PATH');
  const target = path.join(root, file);
  for (let current = target; current !== path.dirname(current); current = path.dirname(current)) {
    if (fs.lstatSync(current).isSymbolicLink()) throw Error('DEPENDENCY_INDIRECT');
  }
  return fs.readFileSync(target);
}
function loadModel(inputs) {
  const bytes = readDirect(atfDependencyPolicyPath);
  if (hash(bytes) !== inputs.get(atfDependencyPolicyPath)) throw Error('DEPENDENCY_POLICY_HASH');
  const policy = JSON.parse(bytes);
  // The input manifest must not omit a code provider that exists in this source
  // tree. Snapshot/hash admission precedes dependency classification.
  for (const directory of [...policy.javaRoots, ...policy.scriptRoots]) {
    if (!relative(directory)) throw Error('DEPENDENCY_ROOT_PATH');
    const pending = [directory];
    while (pending.length) {
      const relativeDirectory = pending.pop();
      for (const entry of fs.readdirSync(path.join(root, relativeDirectory), {withFileTypes: true})) {
        const file = relativeDirectory + '/' + entry.name;
        if (entry.isSymbolicLink()) throw Error('DEPENDENCY_INDIRECT');
        if (entry.isDirectory()) pending.push(file);
        else if (/\.(?:java|m?js|ps1|psm1)$/.test(file) && !inputs.has(file)) throw Error('DEPENDENCY_INPUT_MISSING: ' + file);
      }
    }
  }
  return buildAtfDependencyModel(inputs, readDirect, policy, atfCollections);
}

export function dependencyDecision(file, current, previous, load, groups = atfGroups) {
  if (!current.has(atfDependencyPolicyPath) || current.get(atfDependencyPolicyPath) !== previous.get(atfDependencyPolicyPath)) {
    return {groups, status: 'CONSERVATIVE', reason: 'DEPENDENCY_POLICY_NOT_SHARED'};
  }
  let model;
  try { model = load(); }
  catch (error) { return {groups, status: 'CONSERVATIVE', reason: error.message}; }
  if ([...model.nodes.keys()].some(node => !previous.has(node))) {
    return {groups, status: 'CONSERVATIVE', reason: 'DEPENDENCY_PREVIOUS_POPULATION_MISSING'};
  }
  if (!current.has(atfReaderSealsPath) || !previous.has(atfReaderSealsPath)) {
    return {groups, status: 'CONSERVATIVE', reason: 'DEPENDENCY_READER_SEALS_NOT_SHARED'};
  }
  const changedReaders = [...model.readerPaths].filter(node => current.get(node) !== previous.get(node)).sort();
  const sealsChanged = current.get(atfReaderSealsPath) !== previous.get(atfReaderSealsPath);
  if (changedReaders.some(node => !model.owners(node)) || (sealsChanged && !changedReaders.length)) {
    return {groups, status: 'CONSERVATIVE', reason: 'DEPENDENCY_READER_SCOPE_UNKNOWN'};
  }
  if (file === atfReaderSealsPath) {
    // Seal metadata may be observed by every collection. Reuse is admitted only
    // for owners of readers whose source bytes actually changed.
    const consumers = [...new Set(changedReaders.flatMap(node => model.owners(node)))];
    return {groups: consumers, status: 'ADMITTED', reason: 'REVIEWED_READER_SEAL_CONSUMERS',
      evidence: changedReaders.flatMap(node => model.explain(node).map(row => ({...row, reader: node}))),
      changedReaders, topologySha256: model.topologySha256,
      policySha256: current.get(atfDependencyPolicyPath), readerSealsSha256: current.get(atfReaderSealsPath)};
  }
  if (!model.nodes.has(file) && !model.global.has(file)) {
    return {groups, status: 'CONSERVATIVE', reason: 'DEPENDENCY_INPUT_UNQUALIFIED'};
  }
  const consumers = model.owners(file);
  return {groups: consumers ?? groups, status: consumers ? 'ADMITTED' : 'CONSERVATIVE',
    reason: consumers ? 'TRANSITIVE_SOURCE_DEPENDENCIES' : 'DEPENDENCY_INPUT_UNOWNED',
    ...(consumers ? {evidence: model.explain?.(file) ?? []} : {}), topologySha256: model.topologySha256,
    policySha256: current.get(atfDependencyPolicyPath), readerSealsSha256: current.get(atfReaderSealsPath)};
}

export function atfDependencySelection(file, current, previous) {
  return dependencyDecision(file, current, previous, () => {
    if (!cache.has(current)) {
      try { cache.set(current, {model: loadModel(current)}); }
      catch (error) { cache.set(current, {error: error.message}); }
    }
    const {model, error} = cache.get(current);
    if (error) throw Error(error);
    return model;
  });
}
