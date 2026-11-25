import {runFixtureProcess} from './fixture-process.mjs';
import assert from 'node:assert/strict';
import test from 'node:test';
import fs from 'node:fs/promises';
import os from 'node:os';
import path from 'node:path';
import {createHash} from 'node:crypto';

const digest=bytes=>createHash('sha256').update(bytes).digest('hex');
async function exercise(t,mode){
 const parent=await fs.realpath(process.env.WHIST_QUALIFICATION_TEMP_ROOT??os.tmpdir());
 const root=await fs.mkdtemp(path.join(parent,'whist-seal-reader-'));
 t.after(async()=>{assert.equal(path.dirname(await fs.realpath(root)),parent);await fs.rm(root,{recursive:true,force:true});});
 const input=[];
 const sizes=mode==='positive'?[0,1,4095,4096,4097,65535,65536,65537,1048575,1048576,1048577,3145745]:[4096];
 for(const size of sizes){
  const bytes=Buffer.alloc(size);for(let index=0;index<size;index++)bytes[index]=(index*17+size)%251;
  const relative=`project/demo/target/classes/size-${size}-ș.bin`;
  await fs.mkdir(path.dirname(path.join(root,relative)),{recursive:true});await fs.writeFile(path.join(root,relative),bytes);
  input.push({relative,bytes:size,expected:digest(bytes)});
 }
 for(const [relative,body]of [['cache/x/data.jar','jar fixture'],['cache/x/data.pom','pom fixture']]){
  await fs.mkdir(path.dirname(path.join(root,relative)),{recursive:true});await fs.writeFile(path.join(root,relative),body);
  input.push({relative,bytes:Buffer.byteLength(body),expected:digest(body)});
 }
 await fs.writeFile(path.join(root,'cache/x/ignored.txt'),'not a sealed cache artifact');
 const mutation='project/demo/target/classes/mutation.bin';
 const large=Buffer.alloc(3145745,71);await fs.writeFile(path.join(root,mutation),large);
 await fs.writeFile(path.join(root,'input.json'),JSON.stringify(input));
 const script=path.join(root,'exercise.ps1');
 await fs.writeFile(script,String.raw`
param($Root,$Scripts,$Mode)
$ErrorActionPreference='Stop'
[Console]::OutputEncoding=[Text.UTF8Encoding]::new($false)
. (Join-Path $Scripts 'AtfQualificationSnapshot.ps1')
. (Join-Path $Scripts 'AtfComponentMaven.ps1')
function Expect($Value,$Message){if(!$Value){throw $Message}}
function Fingerprint($Folder,[ref]$Cost){Get-AtfComponentMavenFingerprint $Folder (Join-Path $Folder 'cache') @('demo') -Measurement $Cost}
function RootCause($Error){while($Error.InnerException){$Error=$Error.InnerException};$Error.GetType().Name}
$inputRows=Get-Content -LiteralPath (Join-Path $Root 'input.json') -Raw|ConvertFrom-Json
$stateKey='ATF.CheckedFileIo.HashReader.1'
$initial=Get-AtfPreparedFileHasher
Expect ($null -eq $initial -and $null -eq ('Atf.CheckedFileIo' -as [type])) 'Hash-only import cannot compile or invent a native capability'
if($Mode -eq 'foreign'){
 Add-Type -TypeDefinition 'namespace Atf { public static class CheckedFileIo { public static string Sha256(string path) { return new string((char)48, 64); } } }'
 $cost=$null;$fp=Fingerprint $Root ([ref]$cost)
 Expect ($cost.hashReader -eq 'powershell-full-stream' -and $null -eq (Get-AtfPreparedFileHasher)) 'A foreign type cannot qualify the native reader'
 @{foreignTypeRefused=$true;fingerprint=$fp;reader=$cost.hashReader}|ConvertTo-Json -Compress
}else{
 $bootstrap=Join-Path $Root 'copy-initialize.ps1'
 [IO.File]::WriteAllText($bootstrap, 'param($Scripts) . (Join-Path $Scripts ''AtfFileDigest.ps1''); Initialize-AtfCheckedFileCopy')
 & $bootstrap $Scripts
 $native=Get-AtfPreparedFileHasher
 Expect ($null -ne $native) 'The copy initializer publishes its source-bound static reader'
 $rows=@()
 if($Mode -eq 'positive'){foreach($file in $inputRows){
  $location=Join-Path $Root $file.relative
  foreach($reader in @('powershell','native')){
   $actual=if($reader -eq 'native'){$native.Invoke($location)}else{Get-AtfFileSha256 $location}
   Expect ($actual -ceq $file.expected) ('Independent full-byte digest: '+$file.relative+' / '+$reader)
   $rows+=@{file=$file.relative;bytes=$file.bytes;reader=$reader;expected=$file.expected;actual=$actual}
  }
 }}
 $cost=$null;$first=Fingerprint $Root ([ref]$cost)
 Expect ($cost.hashReader -eq 'native-source-bound') 'A verified warm capability selects the shared native implementation'
 $state=[AppDomain]::CurrentDomain.GetData($stateKey)
 if($Mode -eq 'negative'){
  $fallbacks=@()
  foreach($bad in @($null,@{},@{definitionSha256='foreign';reader=$native},
    @{definitionSha256=$state.definitionSha256;reader='not a delegate'},
    @{definitionSha256=$state.definitionSha256;reader=[Uri].GetMethod('EscapeDataString',[type[]]@([string])).CreateDelegate([Func[string,string]])},
    @{definitionSha256=$state.definitionSha256;reader=[Delegate]::Combine($native,$native)})){
   [AppDomain]::CurrentDomain.SetData($stateKey,$bad)
   $fallbackCost=$null;$actual=Fingerprint $Root ([ref]$fallbackCost)
   Expect ($null -eq (Get-AtfPreparedFileHasher) -and $fallbackCost.hashReader -eq 'powershell-full-stream' -and $actual -ceq $first) 'Unadmitted readers fall back to complete fresh byte checks'
   $fallbacks+=@{reader=$fallbackCost.hashReader;expected=$first;actual=$actual}
  }
  [AppDomain]::CurrentDomain.SetData($stateKey,$state)
  $mutation=Join-Path $Root 'project/demo/target/classes/mutation.bin';$time=[IO.File]::GetLastWriteTimeUtc($mutation);$changes=@()
  foreach($offset in @(65536,1572872,3145744)){
   $stream=[IO.File]::Open($mutation,'Open','Write','None')
   try{$stream.Position=$offset;$stream.WriteByte(72)}finally{$stream.Dispose()}
   [IO.File]::SetLastWriteTimeUtc($mutation,$time)
   $hash=$native.Invoke($mutation);$changed=Fingerprint $Root ([ref]$cost)
   Expect ($changed -cne $first -and [IO.File]::GetLastWriteTimeUtc($mutation) -eq $time) 'Same-length and timestamp cannot hide middle or last-byte corruption'
   $changes+=@{offset=$offset;actual=$hash;fingerprint=$changed}
   $stream=[IO.File]::Open($mutation,'Open','Write','None');try{$stream.Position=$offset;$stream.WriteByte(71)}finally{$stream.Dispose()}
  }
  $blocked=[IO.File]::Open($mutation,'Open','Write','None');$readFailure=''
  try{try{$native.Invoke($mutation)|Out-Null}catch{$readFailure=RootCause $_.Exception}}finally{$blocked.Dispose()}
  Expect ($readFailure -eq 'IOException') 'Sharing refusal remains an original I/O failure'
  $missing='';try{$native.Invoke((Join-Path $Root 'absent.bin'))|Out-Null}catch{$missing=RootCause $_.Exception}
  Expect ($missing -eq 'FileNotFoundException') 'A missing file cannot become an empty or cached digest'
  $exclusive=[IO.File]::Open($mutation,'Open','ReadWrite','None');$exclusive.Dispose()
  @{fallbacks=$fallbacks;changes=$changes;baselineFingerprint=$first;readFailure=$readFailure;missing=$missing;handlesReleased=$true}|ConvertTo-Json -Depth 8 -Compress
 }else{
  @{rows=$rows;fingerprint=$first;reader=$cost.hashReader;hashes=$cost.hashedFiles;observedBytes=$cost.observedBytes;coldImportDidNotCompile=$true}|ConvertTo-Json -Depth 8 -Compress
 }
}
`);
 const value=JSON.parse(runFixtureProcess('pwsh',['-NoProfile','-NonInteractive','-File',script,root,
  path.resolve(import.meta.dirname,'../../scripts'),mode],{fixture:'seal-reader-'+mode,timeout:60000}));
 const expected=digest([...input.map(row=>row.relative+':'+row.expected),mutation+':'+digest(large)].sort().join('\n'));
 assert.ok(value.fingerprint===undefined||value.fingerprint===expected,'Seal preserves the original jar/pom filter');
 return {value,input,large};
}

test('source-bound reader preserves complete bytes and the cold fallback',async t=>{
 const {value,input}=await exercise(t,'positive');
 assert.equal(value.rows.length,input.length*2);assert.equal(value.reader,'native-source-bound');
 assert.equal(value.coldImportDidNotCompile,true);
 console.log('Seal reader byte observations: '+JSON.stringify(value));
});
test('unadmitted readers corruption and read failures cannot authorize a seal',async t=>{
 const {value,large}=await exercise(t,'negative');
 assert.equal(value.fallbacks.length,6);
 for(const row of value.changes){const changed=Buffer.from(large);changed[row.offset]=72;row.expected=digest(changed);assert.equal(row.actual,row.expected);}
 assert.equal(value.handlesReleased,true);assert.equal(value.readFailure,'IOException');assert.equal(value.missing,'FileNotFoundException');
 console.log('Seal reader rejection observations: '+JSON.stringify(value));
});
test('a foreign preloaded native type retains complete fallback verification',async t=>{
 const {value}=await exercise(t,'foreign');assert.equal(value.foreignTypeRefused,true);assert.equal(value.reader,'powershell-full-stream');
 console.log('Foreign reader observations: '+JSON.stringify(value));
});
