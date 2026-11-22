import {lstatSync, realpathSync, statSync} from 'node:fs';
import path from 'node:path';
import {fileURLToPath} from 'node:url';

function physicalDirectory(directory, name) {
  try {
    if (!path.isAbsolute(directory)) throw new Error('expected an absolute path');
    const resolved = realpathSync.native(directory);
    if (!statSync(resolved).isDirectory()) throw new Error('expected a directory');
    return resolved;
  } catch (error) {
    throw new Error(`QUALIFICATION-TEMP-INVALID: ${name}=${directory} (${error.message})`);
  }
}

function hasGitMarker(directory) {
  try {
    lstatSync(path.join(directory, '.git'));
    return true;
  } catch (error) {
    if (error.code !== 'ENOENT') throw error;
    return false;
  }
}

function requireExternal(directory, source, name) {
  for (let current = path.normalize(directory); ; current = path.dirname(current)) {
    if (path.relative(source, current) === '' || hasGitMarker(current)) {
      throw new Error(`QUALIFICATION-TEMP-IN-CHECKOUT: ${name}=${directory} (checkout ${current})`);
    }
    if (current === path.dirname(current)) return;
  }
}

export function qualificationTempRoot(source, systemTemp, environment = process.env) {
  const physicalSource = physicalDirectory(source, 'SourceRoot');
  const configured = environment.WHIST_QUALIFICATION_TEMP_ROOT?.trim();
  const locations = [['WHIST_QUALIFICATION_TEMP_ROOT', configured], ['TMP', environment.TMP],
    ['TEMP', environment.TEMP], ['GetTempPath', systemTemp]];
  let workRoot;
  for (const [name, directory] of locations) {
    if (name !== 'GetTempPath' && !directory?.trim()) continue;
    const physical = physicalDirectory(directory, name);
    requireExternal(directory, source, name);
    requireExternal(physical, physicalSource, name);
    if (name === 'GetTempPath' && !configured) workRoot = physical;
    if (name === 'WHIST_QUALIFICATION_TEMP_ROOT' && configured) workRoot = physical;
  }
  return workRoot;
}

if (process.argv[1] && fileURLToPath(import.meta.url) === process.argv[1]) {
  try {
    const [source, systemTemp] = process.argv.slice(2);
    process.stdout.write(qualificationTempRoot(source, systemTemp));
  } catch (error) {
    process.stderr.write(error.message + '\n');
    process.exitCode = 1;
  }
}
