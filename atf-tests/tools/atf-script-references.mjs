import path from 'node:path';

// Only executable imports and path/process expressions create script edges.
// A path mentioned as classifier test data is not a read of that file.
function tokens(text) {
  const result = [];
  for (let at = 0; at < text.length;) {
    if (/\s/.test(text[at])) { at++; continue; }
    if (text.startsWith('//', at)) { const end = text.indexOf('\n', at); at = end < 0 ? text.length : end; continue; }
    if (text.startsWith('/*', at)) { const end = text.indexOf('*/', at + 2); at = end < 0 ? text.length : end + 2; continue; }
    const quote = text[at];
    if ('\'"`'.includes(quote)) {
      let value = ''; at++;
      while (at < text.length && text[at] !== quote) {
        if (text[at] === '\\') { value += text[++at] ?? ''; at++; }
        else value += text[at++];
      }
      at++;
      result.push({kind: quote === '`' ? 'template' : 'string', value});
    } else {
      const word = /^[A-Za-z_$][\w$]*/.exec(text.slice(at));
      const value = word?.[0] ?? text[at];
      result.push({kind: word ? 'word' : 'symbol', value}); at += value.length;
    }
  }
  return result;
}

export function scriptReferences(node, index) {
  const edges = new Set(), patterns = [], all = tokens(node.text), bindings = new Map();
  const addFile = value => {
    if (!/^[\w./$-]+\.(?:m?js|psm?1|json|properties|xml|ya?ml|java|jar|pom)$/.test(value)) return;
    for (const file of index.basenames.get(value.slice(value.lastIndexOf('/') + 1)) ?? []) edges.add(file);
  };
  const imported = value => {
    if (!value.startsWith('.')) return;
    const file = path.posix.normalize(path.posix.join(path.posix.dirname(node.path), value.split(/[?#]/, 1)[0]));
    if (file.startsWith('../') || path.posix.isAbsolute(file)) throw Error('DEPENDENCY_IMPORT_ESCAPE');
    if (!index.files.has(file)) {
      const generated = index.generatedModules?.get(node.path + ':' + value);
      if (!generated) throw Error('DEPENDENCY_IMPORT_MISSING: ' + file);
      patterns.push(...generated.inputs); return;
    }
    edges.add(file);
  };
  for (let at = 0; at < all.length; at++) {
    const token = all[at];
    if (token.kind !== 'word') continue;
    if (['const', 'let', 'var'].includes(token.value) && all[at + 2]?.value === '=' && all[at + 3]?.kind === 'string') {
      bindings.set(all[at + 1].value, all[at + 3].value);
    }
    if (['import', 'export', 'require'].includes(token.value)) {
      if (all[at + 1]?.kind === 'string') imported(all[at + 1].value);
      else if (all[at + 1]?.value === '(') {
        if (all[at + 2]?.kind === 'string') imported(all[at + 2].value);
        else patterns.push('**/*.mjs', '**/*.js');
      } else {
        for (let next = at + 1; next < all.length && /^[\w$*,{}]+$/.test(all[next].value); next++) {
          if (all[next].value === 'from' && all[next + 1]?.kind === 'string') { imported(all[next + 1].value); break; }
        }
      }
    }
    if (!['resolve', 'join', 'URL', 'readFile', 'readFileSync', 'copyFile', 'copyFileSync', 'execFile', 'execFileSync'].includes(token.value)
        || all[at + 1]?.value !== '(') continue;
    let depth = 1;
    for (let next = at + 2; next < all.length && depth; next++) {
      const value = all[next];
      if (value.value === '(') depth++;
      if (value.value === ')') depth--;
      if (value.kind === 'string') addFile(value.value);
      else if (value.kind === 'word' && bindings.has(value.value)) addFile(bindings.get(value.value));
    }
  }
  // Embedded PowerShell programs really execute these module imports.
  for (const [, value] of node.text.matchAll(/Join-Path\s+\$\w+\s+['"]([^'"\r\n]+\.psm?1)['"]/g)) addFile(value);
  return {edges, patterns};
}
