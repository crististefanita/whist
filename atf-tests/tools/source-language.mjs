import fs from 'node:fs/promises';
import path from 'node:path';
import {execFileSync} from 'node:child_process';
import {fileURLToPath} from 'node:url';
import {sourceSpans} from './source-language-tokens.mjs';
import {safeRelative} from './source-input-identity.mjs';

const extensions = /\.(?:java|m?js|ps1|psm1|gradle|kts|kt|py|sh|yaml|yml|xml|html|css|properties|json)$/i;
export const policyPath = 'config/source-language-policy.json';

export function inspectSource(name, source, policy) {
  if (Object.keys(policy).some(key=>!['schemaVersion','language','reviewedRomanianWords'].includes(key))
      || policy.schemaVersion !== 1 || policy.language !== 'en' || !Array.isArray(policy.reviewedRomanianWords)
      || !policy.reviewedRomanianWords.length || new Set(policy.reviewedRomanianWords).size !== policy.reviewedRomanianWords.length
      || policy.reviewedRomanianWords.some(word => !/^[a-z]+$/.test(word))) throw Error('SOURCE-LANGUAGE-POLICY');
  const words = new Set(policy.reviewedRomanianWords);
  const diagnostics = [];
  for (const span of sourceSpans(name, source)) {
    if (!['comment', 'identifier', 'diagnostic'].includes(span.kind)) continue;
    const decoded = span.text.replace(/\\u\{([a-f\d]+)\}|\\u([a-f\d]{4})/gi,
      (_, braced, fixed) => String.fromCodePoint(parseInt(braced ?? fixed, 16)));
    const foreignLine = decoded.split('\n').findIndex(line=>{
      const segments=line.replace(/([a-z])([A-Z])/g, '$1 $2').split(/[^\p{L}]+/u);
      return /[ăâîșțşţ]/iu.test(line)||segments.some(word=>
        words.has(word.normalize('NFD').replace(/\p{M}/gu, '').toLowerCase()));
    });
    if (foreignLine>=0) diagnostics.push({code: 'SOURCE-LANGUAGE-EN', file: name, line: span.line+foreignLine,
      kind: span.kind, text: span.text, requirement: 'Use English implementation text; localized literals are data.'});
  }
  return diagnostics;
}

export async function inspectCheckout(project) {
  const root = await fs.realpath(project);
  const policy = JSON.parse(await fs.readFile(path.join(root, policyPath), 'utf8'));
  const gitFiles = args => execFileSync('git', ['-C', root, 'ls-files', '-z', ...args],
    {windowsHide: true, maxBuffer: 16 * 1024 * 1024}).toString().split('\0').filter(Boolean);
  const deleted = new Set(gitFiles(['--deleted']));
  const names = gitFiles(['--cached', '--others', '--exclude-standard']).filter(name => !deleted.has(name));
  const configuration=path.join(root,'config/atf-governance-policy.json');
  const governance=await fs.readFile(configuration,'utf8').then(JSON.parse).catch(error=>{
    if(error.code==='ENOENT')return {};throw error;
  });
  const declared=['scripts/','atf-tests/tools/','atf-ui-browser/src/','atf-ui-browser/tests/','atf-ui-browser/pages/'];
  for(const module of [...(governance.mavenModules??[]),...(governance.additionalSourceRoots??[]).map(row=>row.module)])
    declared.push(safeRelative(module)+'/src/');
  const ignored=gitFiles(['--others','--ignored','--exclude-standard']).filter(name => !deleted.has(name));
  // Ignore rules cannot hide implementation within a declared source owner.
  names.push(...ignored.filter(name=>declared.some(prefix=>name.startsWith(prefix))));
  const files = [...new Set(names)].filter(name => !name.startsWith('release/')
    && (extensions.test(name) || name === '.gitignore')).sort();
  const diagnostics = [];
  for (let start = 0; start < files.length; start += 16) {
    const batch = await Promise.all(files.slice(start, start + 16).map(async name => {
      const file = path.join(root, name);
      if ((await fs.lstat(file)).isSymbolicLink() || await fs.realpath(file) !== file)
        throw Error('SOURCE-LANGUAGE-INDIRECT: ' + name);
      return inspectSource(name, await fs.readFile(file, 'utf8'), policy);
    }));
    diagnostics.push(...batch.flat());
  }
  return {schemaVersion: 1, status: diagnostics.length ? 'BLOCKED' : 'PASS', files,
    excludedTrackedDeletions: [...deleted].sort(), diagnostics,
    limits: 'Reviewed Romanian violations and lexical contexts are checked; semantic English review remains mandatory.'};
}

if (process.argv[1] && path.resolve(process.argv[1]) === fileURLToPath(import.meta.url)) {
  try {
    if (process.argv.length !== 3) throw Error('Use: node source-language.mjs <checkout>');
    const result = await inspectCheckout(process.argv[2]);
    process.stdout.write(JSON.stringify(result, null, 2) + '\n');
    if (result.status !== 'PASS') process.exitCode = 2;
  } catch (error) { console.error(error.message); process.exitCode = 1; }
}
