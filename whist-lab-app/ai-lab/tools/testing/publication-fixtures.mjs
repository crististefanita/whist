import assert from 'node:assert/strict';
import fs from 'node:fs/promises';
import os from 'node:os';
import path from 'node:path';
import crypto from 'node:crypto';
import {createLegalGame} from './legal-game-fixture.mjs';

export const hash=bytes=>crypto.createHash('sha256').update(bytes).digest('hex');
export const exists=file=>fs.lstat(file).then(()=>true,error=>{if(error.code==='ENOENT')return false;throw error;});
export async function scratch(t) {
    const root=await fs.mkdtemp(path.join(os.tmpdir(),'WHIST_CORPUS_PUBLICATION_'));
    t.after(async()=>{
        assert.equal(path.dirname(await fs.realpath(root)),await fs.realpath(os.tmpdir()));
        assert.ok(path.basename(root).startsWith('WHIST_CORPUS_PUBLICATION_'));
        await fs.rm(root,{recursive:true});
    });
    return root;
}

export async function archiveFixture(root) {
    const archive=path.join(root,'archive'),originals=[];
    await fs.mkdir(path.join(archive,'games','development'),{recursive:true});
    await fs.mkdir(path.join(archive,'games','heldout'));
    await fs.writeFile(path.join(archive,'games','heldout','DO_NOT_READ.json'),'invalid heldout sentinel');
    for(const players of [3,4,5,6]){
        const game=createLegalGame({players,seed:370+players});
        const file=path.join(archive,'games','development',game.id+'.json'),bytes=Buffer.from(JSON.stringify(game));
        await fs.writeFile(file,bytes);originals.push({file,bytes,game});
    }
    return {archive,originals};
}

export async function batchFixture(root) {
    const source=path.join(root,'source'),corpus=path.join(root,'corpus');
    const protocol={version:15,roles:['expert'],opponents:['EXPERT_V6'],development:{firstSeed:71,seeds:1}};
    const hashes={'policy.java':'a'.repeat(64)},originals=[];
    await fs.mkdir(path.join(source,'invocations'),{recursive:true});
    await fs.mkdir(path.join(corpus,'games','development'),{recursive:true});
    await fs.writeFile(path.join(source,'candidate-source.json'),JSON.stringify(hashes));
    for(let seat=0;seat<4;seat++){
        const base=createLegalGame({seed:71}),results=[];
        for(const kind of ['before','after']){
            const game=structuredClone(base);game.id='pair-'+seat+'-'+kind;
            game.source={...game.source,heroSeat:seat,sourceHashes:hashes};
            const file=path.join(corpus,'games','development',game.id+'.json');
            await fs.writeFile(file,JSON.stringify(game));
            const best=Math.max(...game.finalScores),points=game.finalScores[seat];
            results.push({kind,gameId:game.id,totals:game.finalScores,points,
                winShare:points===best?1/game.finalScores.filter(x=>x===best).length:0});
        }
        const record={status:'PASS',schemaVersion:1,invocationId:'development-seat-'+seat,
            parameters:{role:'expert',opponent:'EXPERT_V6',seed:71,heroSeat:seat},sourceHashes:hashes,
            durationNanos:1,startedAt:'constructed-start',endedAt:'constructed-end',results,
            checkpoints:[{comparisons:[{passed:true}]}]};
        const file=path.join(source,'invocations',record.invocationId+'.json'),bytes=Buffer.from(JSON.stringify(record));
        await fs.writeFile(file,bytes);originals.push({file,bytes,record});
    }
    return {source,corpus,protocol,originals};
}

export function interruptedWrite(original,segment) {
    return {...fs,open:async(file,...args)=>{
        const handle=await fs.open(file,...args);
        if(!file.includes(path.sep+segment+path.sep))return handle;
        return {writeFile:async bytes=>{await handle.writeFile(Buffer.from(bytes).subarray(0,7));throw original;},
            sync:()=>handle.sync(),close:()=>handle.close()};
    }};
}

export async function unchanged(originals) {
    const rows=[];
    for(const entry of originals){
        const expected=hash(entry.bytes),actual=hash(await fs.readFile(entry.file));
        assert.equal(actual,expected);rows.push({name:path.basename(entry.file),expected,actual});
    }
    return rows;
}

export async function noPrivateState(root,name) {
    const leftovers=(await fs.readdir(root)).filter(file=>file===name+'.publishing'||file.startsWith('.'+name+'.pending-'));
    assert.deepEqual(leftovers,[]);
}
