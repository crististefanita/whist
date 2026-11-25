import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs/promises';
import os from 'node:os';
import path from 'node:path';
import {publishDirectory,writeFreshFile} from './atomic-directory.mjs';

async function folder(t) {
    const root = await fs.mkdtemp(path.join(os.tmpdir(), 'WHIST_PUBLICATION_UNIT_'));
    t.after(async () => {
        assert.equal(path.dirname(await fs.realpath(root)), await fs.realpath(os.tmpdir()));
        assert.ok(path.basename(root).startsWith('WHIST_PUBLICATION_UNIT_'));
        await fs.rm(root, {recursive: true});
    });
    return root;
}
const exists = file => fs.lstat(file).then(() => true, error => {if(error.code === 'ENOENT')return false;throw error;});

test('private staging exposes complete bytes only at promotion', async t => {
    const root=await folder(t),output=path.join(root,'result');
    const bytes=Buffer.from([0,255,13,10,0]);
    const result=await publishDirectory(output,async(stage,io)=>{
        assert.equal(await exists(output),false);
        await writeFreshFile(path.join(stage,'data'),bytes,io);
        return {complete:true};
    });
    assert.deepEqual(result,{complete:true});
    assert.deepEqual(await fs.readFile(path.join(output,'data')),bytes);
    assert.deepEqual(await fs.readdir(root),['result']);
});

test('existing empty directories, populated directories and files cannot be replaced', async t => {
    const root=await folder(t);
    for(const kind of ['empty','populated','file']){
        const output=path.join(root,kind);
        if(kind==='file')await fs.writeFile(output,'original');
        else {await fs.mkdir(output);if(kind==='populated')await fs.writeFile(path.join(output,'data'),'original');}
        let entered=false;
        await assert.rejects(publishDirectory(output,async()=>{entered=true;}),{code:'EEXIST'});
        assert.equal(entered,false);
        if(kind==='empty')assert.deepEqual(await fs.readdir(output),[]);
        else assert.equal(await fs.readFile(kind==='file'?output:path.join(output,'data'),'utf8'),'original');
    }
});

test('build and rename refusal remove only private state and preserve the original exception', async t => {
    const root=await folder(t);
    for(const mode of ['build','rename']){
        const output=path.join(root,mode),original=new Error('controlled '+mode);
        const io={...fs,rename:async(...args)=>{if(mode==='rename')throw original;return fs.rename(...args);}};
        await assert.rejects(publishDirectory(output,async(stage,port)=>{
            await writeFreshFile(path.join(stage,'partial'),'partial',port);
            if(mode==='build')throw original;
        },{io}),error=>error===original);
        assert.equal(await exists(output),false);
        assert.deepEqual(await fs.readdir(root),[]);
    }
});

test('a secondary removal failure cannot erase the build cause or remove a foreign claim', async t => {
    const root=await folder(t),output=path.join(root,'result'),original=new Error('original build failure');
    const secondary=new Error('controlled stage cleanup refusal');
    await assert.rejects(publishDirectory(output,async()=>{throw original;},
        {io:{...fs,rm:async()=>{throw secondary;}}}),error=>error.cause===original&&error.errors.includes(secondary));
    assert.equal(await exists(output),false);
    assert.equal(await exists(output+'.publishing'),false);
    assert.equal((await fs.readdir(root)).length,1);
    const foreign=path.join(root,'other.publishing');await fs.writeFile(foreign,'foreign');
    await assert.rejects(publishDirectory(path.join(root,'other'),async()=>assert.fail('busy build entered')),/PUBLICATION_BUSY/);
    assert.equal(await fs.readFile(foreign,'utf8'),'foreign');
});

test('changed claim ownership blocks promotion and keeps the replacement owner', async t => {
    const root=await folder(t),output=path.join(root,'result');
    await assert.rejects(publishDirectory(output,async()=>{
        await fs.writeFile(output+'.publishing','replacement owner');
    }),error=>error.cause?.message==='PUBLICATION_CLAIM_CHANGED');
    assert.equal(await exists(output),false);
    assert.equal(await fs.readFile(output+'.publishing','utf8'),'replacement owner');
    assert.deepEqual(await fs.readdir(root),['result.publishing']);
});

test('file write or sync and close failures retain the original cause', async () => {
    for(const phase of ['write','sync']){
        const primary=new Error('original '+phase),close=new Error('close failure');
        const io={open:async()=>({writeFile:async()=>{if(phase==='write')throw primary;},
            sync:async()=>{if(phase==='sync')throw primary;},close:async()=>{throw close;}})};
        await assert.rejects(writeFreshFile('unused','bytes',io),error=>error.cause===primary&&error.errors.includes(close));
    }
});

test('immutable roots and their parent aliases cannot become publication targets', async t => {
    const root=await folder(t),source=path.join(root,'source'),alias=path.join(root,'alias');
    await fs.mkdir(source);await fs.symlink(source,alias,process.platform==='win32'?'junction':'dir');
    for(const parent of [source,alias]){
        await assert.rejects(publishDirectory(path.join(parent,'nested'),async()=>assert.fail('immutable input entered'),
            {forbiddenRoots:[source]}),/separate/);
    }
    assert.deepEqual(await fs.readdir(source),[]);
});

test('destination aliases with reserved or trailing characters are refused before claiming', async t => {
    const root=await folder(t);
    for(const name of ['NUL','CON.txt','result.','result ','result:stream'])
        await assert.rejects(publishDirectory(path.join(root,name),async()=>assert.fail('unsafe build entered')),/DESTINATION_UNSAFE/);
    assert.deepEqual(await fs.readdir(root),[]);
});
