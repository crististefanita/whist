import {runFixtureProcess} from './fixture-process.mjs';
import assert from 'node:assert/strict';
import test from 'node:test';
import fs from 'node:fs/promises';
import path from 'node:path';
import os from 'node:os';
import {createHash} from 'node:crypto';

async function exercise(t,negative){
 const root=await fs.mkdtemp(path.join(process.env.WHIST_QUALIFICATION_TEMP_ROOT??os.tmpdir(),'whist-compiler-scope-'));
 t.after(async()=>{assert.equal(path.dirname(await fs.realpath(root)),await fs.realpath(path.dirname(root)));await fs.rm(root,{recursive:true,force:true});});
 const project=path.join(root,'project'),manifest=path.join(root,'inputs.json');
 const put=async(file,text)=>{const full=path.join(project,file);await fs.mkdir(path.dirname(full),{recursive:true});await fs.writeFile(full,text);};
 const parent='<project xmlns="http://maven.apache.org/POM/4.0.0"><modules><module>module</module></modules></project>';
 const pom='<project xmlns="http://maven.apache.org/POM/4.0.0"><build><testSourceDirectory>checks</testSourceDirectory><plugins><plugin><groupId>org.codehaus.mojo</groupId><artifactId>build-helper-maven-plugin</artifactId><executions><execution><goals><goal>add-test-source</goal></goals><configuration><sources><source>${project.basedir}/../shared/src/test/java</source></sources></configuration></execution></executions></plugin></plugins></build><profiles><profile><build><testResources><testResource><directory>../android/src/test/java/payload</directory></testResource></testResources></build></profile></profiles></project>';
 const policy={mavenModules:['module'],additionalSourceRoots:[{path:'android/src/test/java',test:true},{path:'shared/src/test/java',test:true}]};
 const files={'pom.xml':parent,'module/pom.xml':pom,'config/atf-governance-policy.json':JSON.stringify(policy),
  'module/checks/Compiled.java':'compiled','shared/src/test/java/Shared.java':'compiled by build helper',
  'android/src/test/java/GradleOnly.java':'not compiled','android/src/test/java/payload/Data.java':'resource',
  'android/src/test/java/fixture.yaml':'runtime fixture','new/Unknown.java':'unknown',
  'module/src/test/resources/README.md':'resource markdown'};
 for(const[file,bytes]of Object.entries(files))await put(file,bytes);
 const bind=()=>fs.writeFile(manifest,JSON.stringify(Object.entries(files).map(([path,bytes])=>({path,sha256:createHash('sha256').update(bytes).digest('hex')}))));
 await bind();
 const script=path.join(root,'exercise.ps1');
 await fs.writeFile(script,String.raw`
param($Scripts,$Project,$Manifest,$Negative)
$ErrorActionPreference='Stop'
. (Join-Path $Scripts 'AtfCompilerIdentity.ps1')
function Observe { $scope=Get-AtfMavenCompilerScope $Manifest $Project; $key=Get-AtfCompilerInputs $Manifest $Project; @{scope=$scope;key=$key} }
if($Negative -eq 'hash') {
 [IO.File]::AppendAllText((Join-Path $Project 'module/pom.xml'),' ')
 try { Observe | Out-Null; throw 'Unbound recipe accepted' } catch { if($_.Exception.Message -notlike 'COMPONENT_COMPILER_RECIPE_HASH*'){throw}; @{rejected='hash'}|ConvertTo-Json -Compress }
} elseif($Negative -eq 'link') {
 $old=Join-Path $Project 'module';$foreign=Join-Path (Split-Path $Project -Parent) 'foreign'
 [IO.Directory]::Move($old,$foreign);$null=New-Item -ItemType Junction -Path $old -Target $foreign
 try { Observe | Out-Null; throw 'Indirect recipe accepted' } catch { if($_.Exception.Message -notlike 'COMPONENT_COMPILER_RECIPE_INDIRECT*'){throw}; @{rejected='link'}|ConvertTo-Json -Compress } finally { [IO.Directory]::Delete($old) }
} else { Observe | ConvertTo-Json -Depth 7 -Compress }
`);
 const observe=(mode='')=>JSON.parse(runFixtureProcess('pwsh',['-NoProfile','-NonInteractive','-File',script,
  path.resolve(import.meta.dirname,'../../scripts'),project,manifest,mode],{encoding:'utf8',windowsHide:true,timeout:20000}));
 if(!negative){
  const first=observe();assert.ok(first.scope);assert.ok(first.scope.compiledRoots.includes('shared/src/test/java'));
  assert.ok(first.scope.compiledRoots.includes('module/checks'));
  assert.ok(first.scope.compiledRoots.includes('android/src/test/java/payload'));
  assert.equal(first.key.count,Object.keys(files).length-1);
  files['android/src/test/java/GradleOnly.java']='changed Gradle test';await put('android/src/test/java/GradleOnly.java',files['android/src/test/java/GradleOnly.java']);await bind();
  assert.equal(observe().key.fingerprint,first.key.fingerprint);
  files['shared/src/test/java/Shared.java']='changed shared Maven input';await put('shared/src/test/java/Shared.java',files['shared/src/test/java/Shared.java']);await bind();
  assert.notEqual(observe().key.fingerprint,first.key.fingerprint);
  console.log('Literal Maven compiler scope observations: '+JSON.stringify({roots:first.scope.compiledRoots,isolatedJavaExcluded:1,sharedProviderInvalidates:true}));
 }else{
  const outcomes=[];
  for(const [name,text]of [['dynamic',pom.replace('checks','${custom.root}')],
    ['plugin',pom.replace('build-helper-maven-plugin','unreviewed-generator')],
    ['foreign-plugin-group',pom.replace('org.codehaus.mojo','foreign.group')],
    ['unknown-helper-goal',pom.replace('add-test-source','generate')],
    ['generated-expression',pom.replace('checks','${project.build.directory}/generated-tests')],
    ['external-parent',pom.replace('<build>','<parent><relativePath>../../foreign/pom.xml</relativePath></parent><build>')],
    ['extension',pom.replace('<build>','<build><extensions><extension/></extensions>')],
    ['nested',pom.replace('<build>','<modules><module>nested</module></modules><build>')]]){
   files['module/pom.xml']=text;await put('module/pom.xml',text);await bind();
   const result=observe();assert.equal(result.scope,null);assert.equal(result.key.count,Object.keys(files).length);outcomes.push(name);
  }
  files['module/pom.xml']=pom;await put('module/pom.xml',pom);await bind();
  assert.equal(observe('hash').rejected,'hash');await put('module/pom.xml',pom);
  assert.equal(observe('link').rejected,'link');
  console.log('Compiler scope rejection observations: '+JSON.stringify({fallback:outcomes,rejected:['unbound recipe','indirect recipe']}));
 }
}
test('literal Maven and build-helper roots isolate Gradle tests while retaining providers and resources',t=>exercise(t,false));
test('unsupported recipes keep all inputs and unbound or indirect recipes reject reuse',t=>exercise(t,true));
