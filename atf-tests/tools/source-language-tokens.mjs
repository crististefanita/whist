import path from 'node:path';

const identifier = /[\p{ID_Start}_$][\p{ID_Continue}$-]*/uy;
function diagnosticArgument(frames) {
  const call = frames.at(-1);
  if (!call?.call) return false;
  const {call: name, argument} = call;
  if (/(?:^|\.)(?:Error|\w*Exception|error|warn|fail)$/.test(name)) return true;
  if (/(?:Problems|problems)\.push$/.test(name)) return true;
  if (/(?:^|\.)(?:audit|check)$/.test(name)) return argument < 2;
  if (/(?:^|\.)(?:test|it)$/.test(name)) return argument === 0;
  if (name === 'recordCheck') return argument === 2;
  if (/(?:assertEquals|assertNotEquals|assertArrayEquals|assertSame|assertNotSame|assertThrows|assert\.\w*[Ee]qual|assert\.match)$/.test(name)) return argument >= 2;
  if (/(?:assertTrue|assertFalse|assert\.ok)$/.test(name)) return argument >= 1;
  return false;
}

/** Lexical admission distinguishes implementation from localized literal data. */
export function sourceSpans(name, source) {
  const ext = path.extname(name).toLowerCase();
  const shell = ['.ps1', '.psm1', '.py', '.sh', '.yaml', '.yml', '.properties'].includes(ext)
    || path.basename(name) === '.gitignore';
  const powershell = ['.ps1', '.psm1'].includes(ext);
  const markup = ['.xml', '.html'].includes(ext);
  const configuration = ['.json', '.yaml', '.yml', '.properties'].includes(ext) || markup
    || path.basename(name) === '.gitignore';
  const spans = [];
  let index = 0, code = '', line = 1;
  const frames = [];
  const emit = (kind, start, end, atLine) => {
    spans.push({kind, text: source.slice(start, end), line: atLine});
    line += (source.slice(start, end).match(/\n/g) ?? []).length;
    index = end;
  };
  const through = (end, from) => {
    const found = source.indexOf(end, from);
    return found < 0 ? source.length : found + end.length;
  };
  while (index < source.length) {
    if (code.length > 512) code = code.slice(-256);
    const start = index, atLine = line;
    const rest = source.slice(index);
    if ((markup && rest.startsWith('<!--')) || (powershell && rest.startsWith('<#'))
        || (!shell && !markup && rest.startsWith('/*'))) {
      const close = markup ? '-->' : powershell ? '#>' : '*/';
      emit('comment', start, through(close, start + 2), atLine);
      code += ' ';
      continue;
    }
    if ((shell && rest.startsWith('#')) || (!shell && !markup && rest.startsWith('//'))) {
      const end = source.indexOf('\n', start);
      emit('comment', start, end < 0 ? source.length : end, atLine);
      code += ' ';
      continue;
    }
    const here = powershell && /^@["']\r?\n/.test(rest);
    const quote = here ? rest[1] : rest[0];
    if (here || ['"', "'", ...(!powershell ? ['`'] : [])].includes(quote)) {
      let end;
      const triple = !powershell && rest.startsWith(quote.repeat(3));
      if (here) {
        const close = new RegExp('^' + quote + '@(?=\\r?$)', 'm').exec(source.slice(start + 2));
        end = close ? start + 2 + close.index + 2 : source.length;
      } else if (triple) end = through(quote.repeat(3), start + 3);
      else {
        end = start + 1;
        while (end < source.length) {
          if (source[end] === (powershell ? '`' : '\\')) { end += 2; continue; }
          if (source[end++] === quote) {
            if (powershell && source[end] === quote) { end++; continue; }
            break;
          }
        }
      }
      const prefix = code.slice(-240);
      const diagnostic = !configuration && (diagnosticArgument(frames)
        || /\b(?:throw|Write-Error|Write-Warning)\s*$/.test(prefix)
        || (ext === '.gradle' && /\bdescription\s*=\s*$/.test(prefix)));
      emit(diagnostic ? 'diagnostic' : 'literal', start, end, atLine);
      // Interpolation is executable even when the surrounding text is data.
      if (quote === '`' && !powershell) {
        for (const match of source.slice(start + 1, end - 1).matchAll(/\$\{([^{}]*)\}/g)) {
          const offset = start + 1 + match.index + 2;
          for (const span of sourceSpans(name, match[1])) spans.push({...span,
            line: atLine + (source.slice(start, offset).match(/\n/g) ?? []).length + span.line - 1});
        }
      }
      code += ' literal ';
      continue;
    }
    // A regex literal is data, including escaped slashes and character classes.
    if (!shell && !markup && rest[0] === '/' && /(?:^|[=(,:!\[?;]|\breturn)\s*$/.test(code)) {
      let end = start + 1, bracket = false;
      for (; end < source.length && source[end] !== '\n'; end++) {
        if (source[end] === '\\') { end++; continue; }
        if (source[end] === '[') bracket = true;
        if (source[end] === ']') bracket = false;
        if (source[end] === '/' && !bracket) { end++; break; }
      }
      emit('literal', start, end, atLine); code += ' regex '; continue;
    }
    identifier.lastIndex = index;
    const token = identifier.exec(source);
    if (token) {
      emit(configuration ? 'data' : 'identifier', start, identifier.lastIndex, atLine);
      code += token[0];
    } else {
      const character = source[index];
      if ('([{'.includes(character)) frames.push({delimiter: character, argument: 0,
        call: character === '(' ? /[\w.$-]+\s*$/.exec(code)?.[0].trim() : null});
      if (')]}'.includes(character)) frames.pop();
      if (character === ',' && frames.length) frames.at(-1).argument++;
      if (character === '\n') line++;
      code += source[index++];
    }
  }
  return spans;
}
