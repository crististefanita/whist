import fs from 'node:fs';
import {fileURLToPath} from 'node:url';
import {nonBuildDocumentation} from './component-evidence-scope.mjs';
import {componentInputMap, digest} from './source-input-identity.mjs';

// Keep execution inventories complete. Only the compiler key drops editorial data.
export function compilerInputs(rows) {
  return [...componentInputMap(rows)].filter(([file]) => !nonBuildDocumentation(file)
      && file !== 'config/documentation-editions.json')
    .sort(([a], [b]) => a < b ? -1 : a > b ? 1 : 0).map(([path, sha256]) => ({path, sha256}));
}

if (process.argv[1] && fileURLToPath(import.meta.url) === process.argv[1]) {
  const [input, output] = process.argv.slice(2);
  if (!input) throw Error('Compiler source inventory required');
  const rows = compilerInputs(JSON.parse(fs.readFileSync(input, 'utf8').replace(/^\uFEFF/, '')));
  if (!rows.length) throw Error('EMPTY_COMPILER_INPUTS');
  const text = JSON.stringify(rows);
  if (output) fs.writeFileSync(output, text);
  process.stdout.write(JSON.stringify({schemaVersion: 1, fingerprint: digest(text), count: rows.length}) + '\n');
}
