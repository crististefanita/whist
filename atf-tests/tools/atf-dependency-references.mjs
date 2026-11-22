// Class-level dependencies are conservative: imports, package peers, qualified
// types, literal repository files and PowerShell function providers are edges.
// File readers with a wider population declare their data boundary separately.
import {scriptReferences} from './atf-script-references.mjs';
function javaCode(text) {
  let result = '', at = 0;
  while (at < text.length) {
    const start = at;
    if (text.startsWith('//', at)) { at = text.indexOf('\n', at); if (at < 0) at = text.length; }
    else if (text.startsWith('/*', at)) { const end = text.indexOf('*/', at + 2); at = end < 0 ? text.length : end + 2; }
    else if (text[at] === '"' || text[at] === "'") {
      const quote = text.startsWith('"""', at) ? '"""' : text[at]; at += quote.length;
      while (at < text.length) {
        if (text[at] === '\\') { at += 2; continue; }
        if (text.startsWith(quote, at)) { at += quote.length; break; }
        at++;
      }
    } else { result += text[at++]; continue; }
    result += text.slice(start, at).replace(/[^\r\n]/g, ' ');
  }
  return result;
}

export function javaDescription(file, text) {
  text = text.replace(/\\u+([a-f\d]{4})/gi, (_, code) => String.fromCharCode(parseInt(code, 16)));
  const original = text;
  text = javaCode(text);
  const namespace = /^\s*package\s+([\w.$]+)\s*;/m.exec(text)?.[1] ?? '';
  const names = [];
  let at = 0, depth = 0;
  for (const match of text.matchAll(/\b(?:class|interface|enum|record)\s+([A-Za-z_$][\w$]*)/g)) {
    while (at < match.index) { if (text[at] === '{') depth++; if (text[at] === '}') depth--; at++; }
    if (depth === 0) names.push(match[1]);
  }
  const imports = [...text.matchAll(/^\s*import\s+(?:static\s+)?([\w$]+(?:\s*\.\s*[\w$*]+)*)\s*;/gm)]
    .map(row => row[1].replace(/\s/g, ''));
  return {namespace, names, imports, text, original};
}

// Opaque readers are not inferred from a green run. Their reviewed body is
// sealed by the dependency policy; a new or changed reader requires admission
// of that policy and a complete qualification before narrowing is available.
export const opaqueReader = text => /\b(?:readFile(?:Sync)?|readString|readAllBytes|readAllLines|newInputStream|getResource(?:AsStream)?|walk|readdir(?:Sync)?|listFiles|FileInputStream|FileReader)\s*\(|\bFiles\s*\.\s*(?:list|lines|find|newByteChannel|newBufferedReader)\s*\(|\bGet-(?:Content|ChildItem|FileHash)\b|\b(?:from\s*|(?:require|import)\s*\(\s*)['"](?:node:)?fs(?:\/promises)?['"]/.test(text);
export const readerBody = text => text.replaceAll('\r\n', '\n');

export function referenceIndex(nodes, files) {
  files = [...files];
  const types = new Map(), packages = new Map(), basenames = new Map(), functions = new Map();
  const add = (map, key, value) => { if (!map.has(key)) map.set(key, new Set()); map.get(key).add(value); };
  for (const file of files) add(basenames, file.slice(file.lastIndexOf('/') + 1), file);
  for (const node of nodes.values()) {
    if (node.path.endsWith('.java')) {
      node.java = javaDescription(node.path, node.text);
      add(packages, node.java.namespace, node.path);
      for (const name of node.java.names) add(types, (node.java.namespace ? node.java.namespace + '.' : '') + name, node.path);
    } else if (/\.psm?1$/.test(node.path)) {
      for (const [, name] of node.text.matchAll(/\bfunction\s+([\w-]+)/gi)) add(functions, name.toLowerCase(), node.path);
    }
  }
  return {types, packages, basenames, functions, files: new Set(files)};
}

export function sourceReferences(node, index) {
  const edges = new Set(), patterns = [];
  const add = values => { for (const value of values ?? []) edges.add(value); };
  if (node.java) {
    const {namespace, text, imports} = node.java;
    const type = name => {
      for (let candidate = name; candidate.includes('.'); candidate = candidate.slice(0, candidate.lastIndexOf('.'))) {
        if (index.types.has(candidate)) { add(index.types.get(candidate)); return; }
      }
    };
    for (const name of imports) {
      if (name.endsWith('.*')) add(index.packages.get(name.slice(0, -2)));
      type(name.replace(/\.\*$/, ''));
    }
    for (const [, name] of text.matchAll(/\b([A-Za-z_$][\w$]*)\b/g)) add(index.types.get((namespace ? namespace + '.' : '') + name));
    for (const [name] of text.replace(/\s*\.\s*/g, '.').matchAll(/\b(?:[A-Za-z_$][\w$]*\.)+[A-Za-z_$][\w$]*/g)) type(name);
    if (/\b(?:forName|loadClass)\s*\(\s*[^'"\s]/.test(node.java.original)) patterns.push('**/*.java');
  }
  // Literal file names also cover files passed through the shared Java/Node
  // process adapters and scripts embedded in controlled fixtures. Ambiguous
  // names retain every provider rather than guessing a directory.
  if (/\.[cm]?js$/.test(node.path)) {
    const scripts = scriptReferences(node, index);
    add(scripts.edges); patterns.push(...scripts.patterns);
  } else for (const [name] of node.text.matchAll(/[A-Za-z0-9_$.-]+\.(?:mjs|js|ps1|psm1|json|properties|xml|ya?ml|java|jar|pom)\b/g)) add(index.basenames.get(name));
  for (const [name] of node.text.matchAll(/\b[A-Za-z]+-[A-Za-z][\w-]*/g)) add(index.functions.get(name.toLowerCase()));
  if (/\.\s+\$[A-Za-z_]/.test(node.text) && /\.psm?1$/.test(node.path)) patterns.push('scripts/**');
  edges.delete(node.path);
  return {edges, patterns};
}
