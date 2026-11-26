import test from 'node:test';
import assert from 'node:assert/strict';
import {spawn} from 'node:child_process';
import fs from 'node:fs/promises';
import path from 'node:path';
import {fileURLToPath} from 'node:url';
import {curate,loadActiveGame} from '../corpus/whist-active-corpus.mjs';
import {composeBatches} from '../learning/whist-learning-batches.mjs';
import {publishDirectory,writeFreshFile} from '../qualification/atomic-directory.mjs';
import {createLegalGame} from './legal-game-fixture.mjs';
import {scratch,archiveFixture,batchFixture,interruptedWrite,unchanged,noPrivateState,hash,exists} from './publication-fixtures.mjs';

const delay=milliseconds=>new Promise(resolve=>setTimeout(resolve,milliseconds));
async function waitForPublisherMarker(marker,child,timeoutMs){
    const deadline=Date.now()+timeoutMs;
    while(Date.now()<deadline){
        if(await exists(marker))return JSON.parse(await fs.readFile(marker,'utf8'));
        if(child.exitCode!==null||child.signalCode!==null)throw new Error('Publisher exited before owning its private stage');
        await delay(20);
    }
    throw new Error('Timed out waiting for the publisher stage marker');
}

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

test('curator publishes a generated archive volume within the bounded process budget',async t=>{
    const root=await scratch(t),archive=path.join(root,'archive'),directory=path.join(archive,'games','development');
    const heldout=path.join(archive,'games','heldout','DO_NOT_READ.json'),output=path.join(root,'active');
    const sourceHashes=[];const sourceCount=256,maxGames=32;
    await fs.mkdir(directory,{recursive:true});await fs.mkdir(path.dirname(heldout),{recursive:true});
    await fs.writeFile(heldout,'invalid heldout sentinel');
    const templates=[3,4,5,6].map((players,index)=>createLegalGame({players,seed:11000+index}))
        .map(game=>({id:game.id,json:JSON.stringify(game)}));
    for(let index=0;index<sourceCount;index++){
        const template=templates[index%templates.length],id=`generated-archive-${String(index).padStart(4,'0')}`;
        const bytes=Buffer.from(template.json.replace(`"id":${JSON.stringify(template.id)}`,`"id":${JSON.stringify(id)}`));
        const file=path.join(directory,id+'.json');
        await fs.writeFile(file,bytes,{flag:'wx'});sourceHashes.push({file,sha256:hash(bytes),bytes:bytes.length});
    }
    if(global.gc)global.gc();
    let processPeakRssBytes=process.memoryUsage().rss;
    let sampledPeakHeapBytes=process.memoryUsage().heapUsed;
    const monitor=setInterval(()=>{
        const usage=process.memoryUsage();
        processPeakRssBytes=Math.max(processPeakRssBytes,usage.rss);
        sampledPeakHeapBytes=Math.max(sampledPeakHeapBytes,usage.heapUsed);
    },10);
    monitor.unref();
    const started=performance.now();let manifest;
    try{manifest=await curate(archive,output,{maxGames});}
    finally{clearInterval(monitor);}
    processPeakRssBytes=Math.max(processPeakRssBytes,process.resourceUsage().maxRSS*1024,process.memoryUsage().rss);
    const elapsedMs=performance.now()-started,inventory=JSON.parse(await fs.readFile(path.join(output,'archive-inventory.json'),'utf8'));
    assert.equal(manifest.sourceGames,sourceCount);assert.equal(inventory.length,sourceCount);
    assert.equal(manifest.sourceBytes,sourceHashes.reduce((total,item)=>total+item.bytes,0));
    assert.ok(manifest.activeGames>0&&manifest.activeGames<=maxGames);
    assert.equal(manifest.uniqueTraces,templates.length,'The full archive contains four distinct legal traces and repeated immutable aliases');
    assert.ok(manifest.activeBytes<=manifest.sourceBytes);
    assert.ok(processPeakRssBytes>0&&sampledPeakHeapBytes>0&&sampledPeakHeapBytes<=256*1024*1024);
    assert.ok(elapsedMs>=0&&Number.isFinite(elapsedMs));
    const sourceByName=new Map(sourceHashes.map(source=>[path.basename(source.file),source]));
    for(const entry of inventory){
        const original=sourceByName.get(path.basename(entry.file));
        assert.ok(original,'Every inventoried game belongs to the generated development archive');
        assert.equal(hash(await fs.readFile(original.file)),original.sha256,'Curator leaves archive source bytes unchanged');
    }
    for(const entry of manifest.games){
        const bytes=await fs.readFile(path.join(output,entry.file));assert.equal(hash(bytes),entry.sha256);
    }
    assert.equal(await fs.readFile(heldout,'utf8'),'invalid heldout sentinel');
    await noPrivateState(root,'active');
    console.log('Archive volume observations: '+JSON.stringify({profile:'generated-archive-v1',sourceGames:sourceCount,
        sourceBytes:manifest.sourceBytes,activeGames:manifest.activeGames,activeBytes:manifest.activeBytes,
        uniqueTraces:manifest.uniqueTraces,processPeakRssBytes,sampledPeakHeapBytes,heapBudgetBytes:256*1024*1024,
        elapsedMs:Number(elapsedMs.toFixed(3)),inventoryRows:inventory.length,sourceBytesPreserved:true,heldoutUntouched:true}));
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

test('hard-stopped publisher blocks a live competitor and permits only explicit identity-checked recovery',async t=>{
    const root=await scratch(t),output=path.join(root,'corpus-result'),source=path.join(root,'immutable-input.bin'),marker=path.join(root,'publisher-ready.json');
    const sourceBytes=Buffer.from([0,1,127,128,255,10,13]);await fs.writeFile(source,sourceBytes,{flag:'wx'});
    const sourceHash=hash(sourceBytes),childScript=fileURLToPath(new URL('../qualification/atomic-directory-hard-stop-child.mjs',import.meta.url));
    const child=spawn(process.execPath,[childScript,output,source,marker],{stdio:['ignore','pipe','pipe']});let diagnostic='';
    child.stdout.on('data',chunk=>{diagnostic+=chunk.toString();});child.stderr.on('data',chunk=>{diagnostic+=chunk.toString();});
    const closed=new Promise(resolve=>child.once('close',(code,signal)=>resolve({code,signal})));
    try{
        const observed=await waitForPublisherMarker(marker,child,10000);await fs.unlink(marker);
        assert.equal(observed.pid,child.pid);assert.equal(observed.output,output);assert.equal(observed.source,source);
        assert.equal(observed.sourceHash,sourceHash);assert.equal(observed.stagedSourceHash,sourceHash);
        assert.equal(await fs.readFile(observed.stagedSource).then(hash),sourceHash);assert.equal(await fs.readFile(source).then(hash),sourceHash);
        assert.equal(await exists(output),false);
        const stageRealPath=await fs.realpath(observed.stage);assert.equal(path.dirname(stageRealPath),await fs.realpath(root));
        assert.ok(path.basename(stageRealPath).startsWith('.corpus-result.pending-'));assert.equal((await fs.lstat(stageRealPath)).isSymbolicLink(),false);
        const claim=JSON.parse(observed.claimContent);assert.equal(claim.schemaVersion,1);assert.equal(claim.kind,'OWNED_DIRECTORY_PUBLICATION');
        assert.equal(claim.destination,output);assert.match(claim.owner,/^[0-9a-f-]{36}$/);
        assert.equal(await fs.readFile(observed.claimPath,'utf8'),observed.claimContent);assert.equal(hash(Buffer.from(observed.claimContent)),observed.claimHash);
        await assert.rejects(publishDirectory(output,async()=>assert.fail('A live competing writer must not enter')),
            error=>error.code==='EEXIST'&&error.message.includes('PUBLICATION_BUSY'));
        assert.equal(await exists(output),false);assert.equal(await fs.readFile(source).then(hash),sourceHash);
        assert.equal(child.kill('SIGKILL'),true,diagnostic);
        const stopped=await Promise.race([closed,delay(5000).then(()=>null)]);
        assert.ok(stopped,'The child process must be observed stopped before recovery');assert.notEqual(stopped.code,0);
        assert.equal(await exists(output),false);assert.equal(await fs.readFile(source).then(hash),sourceHash);
        assert.equal(await fs.readFile(observed.stagedSource).then(hash),sourceHash);
        assert.equal(await fs.readFile(observed.claimPath,'utf8'),observed.claimContent);
        assert.equal(hash(Buffer.from(await fs.readFile(observed.claimPath))),observed.claimHash);
        const currentStage=await fs.realpath(observed.stage);assert.equal(currentStage,stageRealPath);
        assert.equal(path.dirname(currentStage),await fs.realpath(root));assert.equal(await exists(output),false);
        await fs.rm(currentStage,{recursive:true,force:false});assert.equal(await fs.readFile(observed.claimPath,'utf8'),observed.claimContent);
        await fs.unlink(observed.claimPath);
        await publishDirectory(output,async(stage,io)=>{await writeFreshFile(path.join(stage,'source.copy'),await fs.readFile(source),io);});
        assert.deepEqual(await fs.readFile(path.join(output,'source.copy')),sourceBytes);
        assert.equal(await fs.readFile(source).then(hash),sourceHash);
        console.log('Hard-stop observations: '+JSON.stringify({liveCompetitorRefused:true,forcedStopObserved:true,
            originalInputSha256:sourceHash,stagedInputSha256:observed.stagedSourceHash,claimSha256:observed.claimHash,
            outputAbsentUntilExplicitRecovery:true,recoveryRequiredDeadWriterAndExactClaim:true,
            republishedInputSha256:hash(await fs.readFile(path.join(output,'source.copy')))}));
    }finally{
        if(child.exitCode===null&&child.signalCode===null){child.kill();await Promise.race([closed,delay(5000)]);}
    }
});

test('hard-stopped active curator preserves the archive and recovers only after exact claim validation',async t=>{
    const root=await scratch(t),archive=path.join(root,'archive'),directory=path.join(archive,'games','development');
    const heldout=path.join(archive,'games','heldout','DO_NOT_READ.json'),output=path.join(root,'active');
    const marker=path.join(root,'curator-ready.json'),childScript=fileURLToPath(new URL('../qualification/active-corpus-hard-stop-child.mjs',import.meta.url));
    await fs.mkdir(directory,{recursive:true});await fs.mkdir(path.dirname(heldout),{recursive:true});
    await fs.writeFile(heldout,'invalid heldout sentinel');
    const templates=[3,4,5,6].map((players,index)=>createLegalGame({players,seed:12000+index}));
    const sources=[];
    for(let index=0;index<256;index++){
        const template=templates[index%templates.length],id=`curator-stop-${String(index).padStart(4,'0')}`;
        const bytes=Buffer.from(JSON.stringify({...template,id})),file=path.join(directory,id+'.json');
        await fs.writeFile(file,bytes,{flag:'wx'});sources.push({file,sha256:hash(bytes)});
    }
    const child=spawn(process.execPath,[childScript,archive,output,marker],{stdio:['ignore','pipe','pipe']});let diagnostic='';
    child.stdout.on('data',chunk=>{diagnostic+=chunk.toString();});child.stderr.on('data',chunk=>{diagnostic+=chunk.toString();});
    const closed=new Promise(resolve=>child.once('close',(code,signal)=>resolve({code,signal})));
    try{
        const observed=await waitForPublisherMarker(marker,child,30000),claim=JSON.parse(observed.claimContent);
        assert.equal(observed.pid,child.pid);assert.equal(observed.output,output);assert.equal(claim.kind,'OWNED_DIRECTORY_PUBLICATION');
        assert.equal(claim.destination,output);assert.equal(path.dirname(await fs.realpath(observed.stage)),await fs.realpath(root));
        assert.ok(path.basename(observed.stage).startsWith('.active.pending-'));
        assert.equal((await fs.lstat(observed.stage)).isSymbolicLink(),false);assert.equal(await exists(output),false);
        assert.equal(await fs.readFile(observed.stagedFile).then(bytes=>bytes.length),0);
        await assert.rejects(curate(archive,output,{maxGames:16}),error=>error.code==='EEXIST'&&error.message.includes('PUBLICATION_BUSY'));
        assert.equal(child.kill('SIGKILL'),true,diagnostic);
        const stopped=await Promise.race([closed,delay(5000).then(()=>null)]);
        assert.ok(stopped,'The curator child must be observed stopped before recovery');assert.notEqual(stopped.code,0);
        assert.equal(await fs.readFile(observed.claimPath,'utf8'),observed.claimContent);
        assert.deepEqual(JSON.parse(await fs.readFile(observed.claimPath,'utf8')),claim);
        for(const source of sources)assert.equal(await fs.readFile(source.file).then(hash),source.sha256);
        assert.equal(await fs.readFile(heldout,'utf8'),'invalid heldout sentinel');assert.equal(await exists(output),false);
        const stage=await fs.realpath(observed.stage);assert.equal(path.dirname(stage),await fs.realpath(root));
        assert.equal(path.basename(stage).startsWith('.active.pending-'),true);
        await fs.rm(stage,{recursive:true,force:false});await fs.unlink(observed.claimPath);
        const manifest=await curate(archive,output,{maxGames:16});
        assert.equal(manifest.sourceGames,256);assert.ok(manifest.activeGames>0&&manifest.activeGames<=16);
        for(const entry of manifest.games)await loadActiveGame(output,manifest,entry);
        for(const source of sources)assert.equal(await fs.readFile(source.file).then(hash),source.sha256);
        await noPrivateState(root,'active');
        console.log('Active curator hard-stop observations: '+JSON.stringify({sourceGames:256,activeGames:manifest.activeGames,
            liveCompetitorRefused:true,forcedStopObserved:true,originalSourceHashesPreserved:true,heldoutUntouched:true,
            outputAbsentUntilClaimRecovery:true,recoveryRequiredDeadWriterAndExactClaim:true,recurationVerified:true}));
    }finally{
        if(child.exitCode===null&&child.signalCode===null){child.kill();await Promise.race([closed,delay(5000)]);}
    }
});
