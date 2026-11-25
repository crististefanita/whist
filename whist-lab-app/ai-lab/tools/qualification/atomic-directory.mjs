import fs from 'node:fs/promises';
import path from 'node:path';
import {randomUUID} from 'node:crypto';

function within(root, file) {
    const relative = path.relative(root, file);
    return relative === '' || relative !== '..' && !relative.startsWith('..' + path.sep) && !path.isAbsolute(relative);
}

async function absent(io, file) {
    try { await io.lstat(file); }
    catch (error) { if (error.code === 'ENOENT') return; throw error; }
    throw Object.assign(new Error('EEXIST: publication destination already exists: ' + file), {code: 'EEXIST'});
}

function failure(primary, cleanup) {
    if (!cleanup.length) return primary;
    const message = (primary?.message ?? 'Publication cleanup failed')
        + '; cleanup: ' + cleanup.map(row => row.phase + ': ' + row.error.message).join('; ');
    return new AggregateError([...(primary ? [primary] : []), ...cleanup.map(row => row.error)], message,
        {cause: primary ?? cleanup[0].error});
}

/** Complete file bytes are synced before a directory can be promoted. */
export async function writeFreshFile(file, bytes, io = fs) {
    const handle = await io.open(file, 'wx');
    let primary;
    try { await handle.writeFile(bytes); await handle.sync(); }
    catch (error) { primary = error; }
    const cleanup = [];
    try { await handle.close(); } catch (error) { cleanup.push({phase: 'file close', error}); }
    if (primary || cleanup.length) throw failure(primary, cleanup);
}

async function ownsClaim(io, file, identity, content) {
    const current = await io.lstat(file);
    if (current.isSymbolicLink() || current.dev !== identity.dev || current.ino !== identity.ino
            || await io.readFile(file, 'utf8') !== content)
        throw new Error('PUBLICATION_CLAIM_CHANGED');
}

async function ownedStage(io, stage, parent, prefix) {
    const entry = await io.lstat(stage);
    if (entry.isSymbolicLink() || await io.realpath(stage) !== stage
            || path.dirname(stage) !== parent || !path.basename(stage).startsWith(prefix))
        throw new Error('PUBLICATION_STAGE_OWNERSHIP');
}

/** Private sibling staging and an exclusive cooperative claim; never merges into an existing result. */
export async function publishDirectory(output, build, {io = fs, forbiddenRoots = []} = {}) {
    const requested = path.resolve(output), parent = await io.realpath(path.dirname(requested));
    const name = path.basename(requested);
    if (!name || /[<>:"/\\|?*\x00-\x1f]|[. ]$/.test(name)
            || /^(?:CON|PRN|AUX|NUL|COM[1-9]|LPT[1-9])(?:\.|$)/i.test(name))
        throw new Error('PUBLICATION_DESTINATION_UNSAFE');
    const destination = path.join(parent, name), prefix = '.' + name + '.pending-';
    for (const root of forbiddenRoots) {
        if (within(await io.realpath(root), destination)) throw new Error('Publication must remain separate from immutable inputs');
    }
    await absent(io, destination);
    const claim = destination + '.publishing';
    const content = JSON.stringify({schemaVersion: 1, kind: 'OWNED_DIRECTORY_PUBLICATION',
        owner: randomUUID(), destination}) + '\n';
    let handle;
    try { handle = await io.open(claim, 'wx'); }
    catch (error) {
        if (error.code === 'EEXIST') throw Object.assign(new Error('PUBLICATION_BUSY: ' + destination, {cause: error}), {code: 'EEXIST'});
        throw error;
    }
    let identity, stage, result, primary;
    const cleanup = [];
    try {
        identity = await handle.stat();
        await handle.writeFile(content);
        await handle.sync();
        await absent(io, destination);
        stage = await io.mkdtemp(path.join(parent, prefix));
        await ownedStage(io, stage, parent, prefix);
        result = await build(stage, io);
        await ownsClaim(io, claim, identity, content);
        await ownedStage(io, stage, parent, prefix);
        await absent(io, destination);
        await io.rename(stage, destination);
        stage = undefined;
    } catch (error) { primary = error; }
    if (stage) {
        try { await ownedStage(io, stage, parent, prefix); await io.rm(stage, {recursive: true, force: false}); }
        catch (error) { if (error.code !== 'ENOENT') cleanup.push({phase: 'private stage removal', error}); }
    }
    try { await handle.close(); } catch (error) { cleanup.push({phase: 'claim close', error}); }
    try {
        if (!identity) throw new Error('PUBLICATION_CLAIM_IDENTITY_MISSING');
        await ownsClaim(io, claim, identity, content);
        await io.unlink(claim);
    } catch (error) { cleanup.push({phase: 'claim removal', error}); }
    if (primary || cleanup.length) throw failure(primary, cleanup);
    return result;
}
