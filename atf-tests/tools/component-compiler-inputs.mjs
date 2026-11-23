import fs from 'node:fs';
import {fileURLToPath} from 'node:url';
import {nonBuildDocumentation} from './component-evidence-scope.mjs';
import {componentInputMap, digest, safeRelative} from './source-input-identity.mjs';

// Keep execution inventories complete. Only the compiler key drops editorial data.
export function compilerInputs(rows, scope = null) {
  if (scope !== null && (scope.schemaVersion !== 1 || !Array.isArray(scope.compiledRoots)
      || !scope.compiledRoots.length || !Array.isArray(scope.testRoots))) throw Error('COMPILER_SCOPE_INVALID');
  for (const root of [...(scope?.compiledRoots ?? []), ...(scope?.testRoots ?? [])]) safeRelative(root);
  const under = (file, roots) => roots.some(root => file.startsWith(root + '/'));
  // Retain case aliases conservatively on case-sensitive hosts as well as Windows.
  const compiledRoots = scope?.compiledRoots.map(root => root.toLowerCase()) ?? [];
  const compiled = file => under(file.toLowerCase(), compiledRoots);
  const isolated = file => scope && file.endsWith('.java') && under(file, scope.testRoots)
      && !compiled(file);
  return [...componentInputMap(rows)].filter(([file]) => !nonBuildDocumentation(file)
      && file !== 'config/documentation-editions.json' && !isolated(file))
    .sort(([a], [b]) => a < b ? -1 : a > b ? 1 : 0).map(([path, sha256]) => ({path, sha256}));
}

if (process.argv[1] && fileURLToPath(import.meta.url) === process.argv[1]) {
  const [input, option, payload, ...extra] = process.argv.slice(2);
  if (!input) throw Error('Compiler source inventory required');
  if (extra.length || (payload && option !== '--scope-json') || (option === '--scope-json' && !payload)) throw Error('Invalid compiler inventory arguments');
  const scope = option === '--scope-json' ? JSON.parse(payload) : null;
  const output = option === '--scope-json' ? null : option;
  const rows = compilerInputs(JSON.parse(fs.readFileSync(input, 'utf8').replace(/^\uFEFF/, '')), scope);
  if (!rows.length) throw Error('EMPTY_COMPILER_INPUTS');
  const text = JSON.stringify(rows);
  if (output) fs.writeFileSync(output, text);
  process.stdout.write(JSON.stringify({schemaVersion: 1, fingerprint: digest(text), count: rows.length}) + '\n');
}
