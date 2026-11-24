import fs from 'node:fs';
import path from 'node:path';
import {spawnSync} from 'node:child_process';
import {randomUUID} from 'node:crypto';
import {performance} from 'node:perf_hooks';

export const fixtureProfile=Object.freeze(JSON.parse(fs.readFileSync(new URL('../../config/launcher-fixture-profile.json',import.meta.url),'utf8')));

export function boundedStream(value,limit=fixtureProfile.maximumRetainedBytesPerStream){
  if(!Number.isSafeInteger(limit)||limit<1)throw Error('FIXTURE-STREAM-LIMIT');
  const bytes=Buffer.from(value??'');
  let tail=bytes.subarray(Math.max(0,bytes.length-limit)).toString('utf8');
  while(Buffer.byteLength(tail)>limit)tail=tail.slice(tail.codePointAt(0)>0xffff?2:1);
  return {bytes:bytes.length,truncated:bytes.length>limit,tail};
}

function executeFile(command,args,options){
  const result=spawnSync(command,args,options);
  if(result.error)throw Object.assign(result.error,{stdout:result.stdout,stderr:result.stderr,status:result.status,signal:result.signal});
  if(result.status!==0)throw Object.assign(new Error('Fixture child exited unsuccessfully'),
    {code:'FIXTURE-CHILD-EXIT',status:result.status,signal:result.signal,stdout:result.stdout,stderr:result.stderr});
  return {stdout:result.stdout,stderr:result.stderr};
}

/** Persist diagnostics before the fixture lifecycle removes its temporary files. */
export function runFixtureProcess(command,args,options={},execute=executeFile){
  const {diagnosticsRoot=process.env.WHIST_FIXTURE_DIAGNOSTICS??process.env.WHIST_EVIDENCE_ROOT??process.env.WHIST_QUALIFICATION_TEMP_ROOT,
    fixture='launcher',timeout=fixtureProfile.timeoutMs,...processOptions}=options;
  if(!diagnosticsRoot || !path.isAbsolute(diagnosticsRoot))throw Error('FIXTURE-DIAGNOSTICS-ROOT: configure an external absolute directory');
  const root=fs.realpathSync(diagnosticsRoot);
  const checkout=path.resolve(import.meta.dirname,'../..');
  const relative=path.relative(checkout,root);
  if(!relative || (!path.isAbsolute(relative)&&relative!=='..'&&!relative.startsWith('..'+path.sep)))throw Error('FIXTURE-DIAGNOSTICS-IN-CHECKOUT');
  const file=path.join(root,'fixture-'+randomUUID()+'.json');
  const started=performance.now();
  const receipt={schemaVersion:1,kind:'ATF_FIXTURE_PROCESS',fixture,timeoutMs:timeout,profile:fixtureProfile};
  receipt.maximumBufferedBytesPerStream=processOptions.maxBuffer??fixtureProfile.maximumBufferedBytesPerStream;
  let failure;
  try {
    const output=execute(command,args,{encoding:'utf8',windowsHide:true,
      maxBuffer:fixtureProfile.maximumBufferedBytesPerStream,...processOptions,timeout});
    const stdout=typeof output==='string'?output:output.stdout;
    receipt.status='PASS';receipt.stdout=boundedStream(stdout);receipt.stderr=boundedStream(output.stderr??'');
    return stdout;
  } catch(error){
    failure=error;receipt.status=error.code==='ETIMEDOUT'?'TIMEOUT':'FAIL';
    receipt.cause={name:error.name,code:error.code??null,status:error.status??null,signal:error.signal??null,message:boundedStream(error.message)};
    receipt.stdout=boundedStream(error.stdout);receipt.stderr=boundedStream(error.stderr);
    error.fixtureDiagnostic=file;
    throw error;
  } finally {
    receipt.elapsedMs=performance.now()-started;
    try {fs.writeFileSync(file,JSON.stringify(receipt,null,2)+'\n',{flag:'wx'});
      process.stdout.write(JSON.stringify(receipt.status==='PASS'
        ?{fixtureDiagnostic:file,status:receipt.status,elapsedMs:receipt.elapsedMs}:{fixtureDiagnostic:file,...receipt})+'\n');}
    catch(storageFailure){if(failure){failure.diagnosticFailure=storageFailure;}else throw storageFailure;}
  }
}
