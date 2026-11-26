import fs from 'node:fs/promises';
import path from 'node:path';
import {curate} from '../corpus/whist-active-corpus.mjs';

const [archive, output, marker] = process.argv.slice(2);
if (!archive || !output || !marker) throw new Error('Expected archive, output and marker paths');

const io = {
    ...fs,
    async open(file, ...options) {
        const handle = await fs.open(file, ...options);
        const parent = path.dirname(file);
        const stage = path.dirname(parent);
        if (path.basename(parent) === 'games' && path.basename(stage).startsWith('.active.pending-')) {
            const claimPath = output + '.publishing';
            const claimContent = await fs.readFile(claimPath, 'utf8');
            await fs.writeFile(marker, JSON.stringify({pid: process.pid, output, stage, stagedFile: file,
                claimPath, claimContent}));
            return {writeFile: async () => new Promise(() => setInterval(() => {}, 1000)),
                sync: () => handle.sync(), close: () => handle.close()};
        }
        return handle;
    }
};

await curate(archive, output, {maxGames: 16}, {io});
