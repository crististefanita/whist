import fs from 'node:fs/promises';
import path from 'node:path';
import {createHash} from 'node:crypto';
import {publishDirectory, writeFreshFile} from './atomic-directory.mjs';

const [output, source, marker] = process.argv.slice(2);
if (!output || !source || !marker) throw new Error('Expected output, source and marker paths');

const sourceBytes = await fs.readFile(source);
const sourceHash = createHash('sha256').update(sourceBytes).digest('hex');
await publishDirectory(output, async (stage, io) => {
    const stagedSource = path.join(stage, 'source.copy');
    await writeFreshFile(stagedSource, sourceBytes, io);
    const claimPath = output + '.publishing';
    const claimContent = await fs.readFile(claimPath, 'utf8');
    const claimHash = createHash('sha256').update(claimContent).digest('hex');
    await fs.writeFile(marker, JSON.stringify({
        pid: process.pid,
        output,
        stage,
        claimPath,
        claimContent,
        claimHash,
        source,
        sourceHash,
        stagedSource,
        stagedSourceHash: createHash('sha256').update(await fs.readFile(stagedSource)).digest('hex')
    }) + '\n', {flag: 'wx'});
    await new Promise(() => setInterval(() => {}, 1000));
});
