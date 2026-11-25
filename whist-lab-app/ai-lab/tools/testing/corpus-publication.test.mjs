import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs/promises';
import path from 'node:path';
import {curate,loadActiveGame} from '../corpus/whist-active-corpus.mjs';
import {composeBatches} from '../learning/whist-learning-batches.mjs';
import {scratch,archiveFixture,batchFixture,interruptedWrite,unchanged,noPrivateState,hash,exists} from './publication-fixtures.mjs';

test('active corpus publication retains legal immutable bytes',async t=>{
    const root=await scratch(t),{archive,originals}=await archiveFixture(root),output=path.join(root,'active');
    const manifest=await curate(archive,output,{maxGames:2,protectedIds:[originals[0].game.id]});
    assert.equal(manifest.sourceGames,4);assert.ok(manifest.activeGames>0&&manifest.activeGames<=2);
    const rows=await unchanged(originals);
    for(const entry of manifest.games){
        const actual=hash(await fs.readFile(path.join(output,entry.file)));
        assert.equal(actual,entry.sha256);await loadActiveGame(output,manifest,entry);
        rows.push({name:'Active game '+entry.id,expected:entry.sha256,actual});
    }
    assert.equal((JSON.parse(await fs.readFile(path.join(output,'archive-inventory.json')))).length,4);
    assert.equal(await fs.readFile(path.join(archive,'games/heldout/DO_NOT_READ.json'),'utf8'),'invalid heldout sentinel');
    await noPrivateState(root,'active');
    console.log('Corpus publication observations: '+JSON.stringify({sourceGames:4,activeGames:manifest.activeGames,rows}));
});

test('paired batch publication retains every declared original pair',async t=>{
    const root=await scratch(t),f=await batchFixture(root),output=path.join(root,'composed');
    const manifest=await composeBatches([f.source],f.corpus,f.protocol,'development',output);
    assert.equal(manifest.completedPairs,4);assert.equal(manifest.independentGameAudits,8);
    const rows=await unchanged(f.originals);
    for(const entry of f.originals){const actual=hash(await fs.readFile(path.join(output,'invocations',path.basename(entry.file))));
        assert.equal(actual,hash(entry.bytes));rows.push({name:'Copied pair '+entry.record.invocationId,expected:hash(entry.bytes),actual});}
    const receipt=JSON.parse(await fs.readFile(path.join(output,'quality-invocations/batch-integrity.json')));
    assert.equal(receipt.status,'PASS');assert.equal(receipt.runId,'composed');
    assert.equal(receipt.invocationId,'batch-integrity-development-composed');
    assert.deepEqual(JSON.parse(await fs.readFile(path.join(output,'batch-provenance.json'))),manifest);
    await noPrivateState(root,'composed');
    console.log('Pair publication observations: '+JSON.stringify({pairs:4,gameAudits:8,gate:receipt.invocationId,rows}));
});

test('active corpus publication refuses interrupted and competing attempts',async t=>{
    const root=await scratch(t),{archive,originals}=await archiveFixture(root),rows=[];
    for(const phase of ['write','promotion']){
        const output=path.join(root,phase),original=new Error('controlled corpus '+phase);
        const io=phase==='write'?interruptedWrite(original,'games'):{...fs,rename:async()=>{throw original;}};
        await assert.rejects(curate(archive,output,{}, {io}),error=>error===original);
        const actual=await exists(output);assert.equal(actual,false);await noPrivateState(root,phase);
        rows.push({name:'No final corpus after '+phase,expected:false,actual});
    }
    let ready,release;const entered=new Promise(resolve=>{ready=resolve;}),resume=new Promise(resolve=>{release=resolve;});
    const output=path.join(root,'concurrent');
    const first=curate(archive,output,{}, {io:{...fs,rename:async(...args)=>{ready();await resume;return fs.rename(...args);}}});
    await Promise.race([entered,first.then(()=>{throw new Error('Expected the promotion barrier');})]);
    try {
        assert.equal(await exists(output),false);
        await assert.rejects(curate(archive,output),/PUBLICATION_BUSY/);
    }finally{release();}
    const committed=await first;await noPrivateState(root,'concurrent');
    const before=hash(await fs.readFile(path.join(output,'manifest.json')));
    await assert.rejects(curate(archive,output),{code:'EEXIST'});
    const after=hash(await fs.readFile(path.join(output,'manifest.json')));assert.equal(after,before);
    rows.push({name:'Existing corpus remains immutable',expected:before,actual:after});
    rows.push(...await unchanged(originals));
    console.log('Corpus publication rejection observations: '+JSON.stringify({concurrentWinners:1,activeGames:committed.activeGames,rows}));
});

test('paired batch publication refuses interrupted duplicate and failing inputs',async t=>{
    const root=await scratch(t),f=await batchFixture(root),rows=[];
    for(const phase of ['write','promotion']){
        const output=path.join(root,phase),original=new Error('controlled pair '+phase);
        const io=phase==='write'?interruptedWrite(original,'invocations'):{...fs,rename:async()=>{throw original;}};
        await assert.rejects(composeBatches([f.source],f.corpus,f.protocol,'development',output,{io}),error=>error===original);
        const actual=await exists(output);assert.equal(actual,false);await noPrivateState(root,phase);
        rows.push({name:'No final batch after '+phase,expected:false,actual});
    }
    await assert.rejects(composeBatches([f.source,f.source],f.corpus,f.protocol,'development',path.join(root,'duplicate')),/Duplicate completed pair/);
    let ready,release;const entered=new Promise(resolve=>{ready=resolve;}),resume=new Promise(resolve=>{release=resolve;});
    const output=path.join(root,'concurrent');
    const first=composeBatches([f.source],f.corpus,f.protocol,'development',output,
        {io:{...fs,rename:async(...args)=>{ready();await resume;return fs.rename(...args);}}});
    await Promise.race([entered,first.then(()=>{throw new Error('Expected the promotion barrier');})]);
    try{
        assert.equal(await exists(output),false);
        await assert.rejects(composeBatches([f.source],f.corpus,f.protocol,'development',output),/PUBLICATION_BUSY/);
    }finally{release();}
    await first;await noPrivateState(root,'concurrent');
    const bad={...f.originals[0].record,status:'FAIL'};
    await fs.writeFile(f.originals[0].file,JSON.stringify(bad));
    await assert.rejects(composeBatches([f.source],f.corpus,f.protocol,'development',path.join(root,'failed')),/must remain visible/);
    assert.equal(await exists(path.join(root,'duplicate')),false);assert.equal(await exists(path.join(root,'failed')),false);
    const actual=JSON.parse(await fs.readFile(f.originals[0].file)).status;assert.equal(actual,'FAIL');
    rows.push({name:'Original failed invocation remains failed',expected:'FAIL',actual});
    console.log('Pair publication rejection observations: '+JSON.stringify({concurrentWinners:1,duplicateRefused:true,failedRefused:true,rows}));
});
