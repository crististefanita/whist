import assert from 'node:assert/strict';
import fs from 'node:fs/promises';
import path from 'node:path';
import {fileURLToPath} from 'node:url';
import {withServer,withBrowser,until} from './fixtures.mjs';
import {tableContinuity} from './table-continuity.mjs';
import {tableCheckpoints} from './table-checkpoints.mjs';
import {admitFlowPlan,verifyFlowExecution} from './flow-budget.mjs';
import {executeSanityPlan} from './sanity-plan.mjs';

const [target,output,selection='four-sanity']=process.argv.slice(2);
if(!target||!output)throw Error('Supply JVM classpath or public URL, new evidence directory and optional named profile');
const profiles=[
 {id:'four-sanity',players:4,width:1664,height:916,font:16,language:'en',mobile:false,deals:[1,1]},
 {id:'five-sanity',players:5,width:1664,height:916,font:16,language:'ro',mobile:false,deals:[1,1]},
 {id:'six-sanity',players:6,width:1664,height:916,font:16,language:'en',mobile:false,deals:[1,1]}
];
const selected=admitFlowPlan(profiles,selection);
await fs.mkdir(output,{recursive:true});
const checks=[],repo=fileURLToPath(new URL('../../',import.meta.url));

async function exercise(url){
 for(const profile of [selected]){
  await withBrowser(process.env.ATF_BROWSER_EXECUTABLE??'C:/Program Files (x86)/Microsoft/Edge/Application/msedge.exe',path.join(output,'profile-'+profile.id),async({send,evaluate,version})=>{
   const id='MATCH-ANCHORS-'+profile.id,images=[],flowFrames=[],rounds=[],manual=[],notices=[],flowSteps=[];
   const capture=async role=>{const file=id+'-'+role+'.png';const png=await send('Page.captureScreenshot',{format:'png'});await fs.writeFile(path.join(output,file),Buffer.from(png.data,'base64'));const item={file,title:id+' '+role,role};images.push(item);return item;};
   try{
   await send('Page.enable');await send('Emulation.setDeviceMetricsOverride',{width:profile.width,height:profile.height,mobile:profile.mobile,deviceScaleFactor:1});
   await send('Emulation.setUserAgentOverride',{userAgent:profile.mobile?'Mozilla/5.0 (Linux; Android 9; Mobile)':'Mozilla/5.0 (Windows NT 10.0; Win64; x64)'});
   await send('Page.navigate',{url:url.href});await until(()=>evaluate('!!document.querySelector(".whist-home .primary")'),30000);await evaluate('document.fonts.ready');
   await evaluate(`document.documentElement.style.fontSize='${profile.font}px';document.querySelector('.whist-language select').value='${profile.language}';document.querySelector('.whist-language select').dispatchEvent(new Event('change'))`);
   await until(()=>evaluate('document.querySelector("#table").getAttribute("aria-busy")==="false"'));
   await evaluate(`document.querySelector('.whist-home .primary').click();document.querySelector('[name=players]').value='${profile.players}';document.querySelector('[name=players]').dispatchEvent(new Event('change'));document.querySelector('[name=mode]').value='${profile.mode??'ROMANIAN_WHIST'}';document.querySelector('[name=mode]').dispatchEvent(new Event('change'));document.querySelector('[name=difficulty]').value='HARD';document.querySelector('.setup form').requestSubmit()`);
   await until(()=>evaluate('!!document.querySelector(".board[data-layout=ready]")&&document.querySelector("#table").getAttribute("aria-busy")==="false"'),30000);
   await evaluate('new Promise(resolve=>requestAnimationFrame(()=>requestAnimationFrame(resolve)))');
   await evaluate(`(()=>{
    window.anchorSamples=[];window.recordAnchors=true;let previous='';
    const box=n=>{const r=n.getBoundingClientRect();return [r.left,r.top,r.width,r.height];};
    function snapshot(){
     const view=document.querySelector('#table > .whist-match:not([data-preparing])');
     if(view&&view.querySelector('.board[data-layout=ready]')){
      const sample={hand:view.querySelector('.whist-progress').textContent,actor:view.querySelector('.seat[aria-current]')?.dataset.seat,
       regions:Object.fromEntries(['header','.board','.hand'].map(key=>[key,box(view.querySelector(key))])),
       seats:[...view.querySelectorAll('.board > .seat')].map(n=>({seat:n.dataset.seat,rectangle:box(n),facts:[...n.querySelectorAll(':scope > strong,:scope > .seat-bid,:scope > .seat-won')].map(line=>({text:line.textContent,rectangle:box(line)}))})),
       handFaces:[...view.querySelectorAll('.fan .card')].map(n=>({face:n.dataset.face,lifting:n.classList.contains('lifting-card'),rectangle:box(n)})),
       played:[...view.querySelectorAll('.played-card')].map(n=>({key:n.dataset.seat+'/'+n.querySelector('.card').dataset.face,rectangle:box(n.querySelector('.card'))})),
       collecting:document.querySelector('#table').hasAttribute('data-collecting')};
      return sample;
     }
    }
    window.snapshotAnchors=()=>{const sample=snapshot();return sample?{...sample,at:performance.now()}:null;};
    function tick(){if(!recordAnchors)return;const sample=snapshot();if(sample){const value=JSON.stringify(sample);if(value!==previous){anchorSamples.push({...sample,at:performance.now()});previous=value;}}requestAnimationFrame(tick);}tick();
   })()`);
   const bidding=()=>until(()=>evaluate('!!document.querySelector(".bid-submit")&&document.querySelector("#table").getAttribute("aria-busy")==="false"'),30000);
   const score=async()=>{
    await until(()=>evaluate('!!document.querySelector(".whist-score")&&document.querySelector("#table").getAttribute("aria-busy")==="false"'),60000);
    if(await evaluate('!!document.querySelector(".achievement-notice[open]")')){
     notices.push(await evaluate('document.querySelector(".achievement-notice[open]").textContent'));
     await evaluate(`(()=>{window.noticeAcknowledged=false;let started=false;const stage=document.querySelector('#table');const observer=new MutationObserver(()=>{if(stage.getAttribute('aria-busy')==='true')started=true;else if(started){observer.disconnect();window.noticeAcknowledged=true;}});observer.observe(stage,{attributes:true,attributeFilter:['aria-busy']});document.querySelector('.achievement-notice[open] > button').click();})()`);
     await until(()=>evaluate('window.noticeAcknowledged&&!document.querySelector("dialog[open]")&&document.querySelector("#table").getAttribute("aria-busy")==="false"'),30000);
     flowFrames.push({id:'notice',capture:null});
    }
   };
   let before,face;
   const flowState=()=>evaluate('({at:performance.now(),bid:!!document.querySelector(".bid-submit"),score:!!document.querySelector(".whist-score"),cards:document.querySelectorAll(".fan .card").length,legal:document.querySelectorAll(".fan .legal-card").length,automatic:document.querySelector(".whist-auto input")?.checked??false,text:document.querySelector("#table").innerText})');
   const expectedStates={bidding:{bid:true,cards:1,automatic:false},'before-human':{legal:1},'after-human':{score:true},'manual-score':{score:true},'auto-bidding':{bid:true,cards:1,automatic:false},'auto-score':{score:true},'return-control':{bid:true,automatic:false}};
   await executeSanityPlan(profile.scenario,async step=>{
    const stateBefore=await flowState();
    switch(step){
     case 'bidding': await bidding();assert.equal(await evaluate('document.querySelectorAll(".fan .card").length'),profile.deals[0]);break;
     case 'before-human':
      await evaluate("document.querySelector('.bid-submit').click()");
      await until(()=>evaluate('!!document.querySelector(".fan .legal-card")&&document.querySelector("#table").getAttribute("aria-busy")==="false"'),30000);
      before=await evaluate('window.snapshotAnchors()');face=await evaluate('document.querySelector(".fan .legal-card").dataset.face');break;
     case 'after-human':
      await evaluate("document.querySelector('.fan .legal-card').click()");
      await until(()=>evaluate('[...document.querySelectorAll(\'.played-card[data-seat="0"] .card\')].some(n=>n.dataset.face==='+JSON.stringify(face)+')||!!document.querySelector(".whist-score")'),30000);
      {const after=await evaluate('window.snapshotAnchors()');if(after)manual.push({hand:before.hand,key:'0/'+face,before,after,images:[]});}
      await score();break;
     case 'manual-score': rounds.push({hand:1,cards:profile.deals[0],control:'manual',score:await evaluate('document.querySelector(".whist-score-grid").textContent')});break;
     case 'auto-bidding':
      await evaluate("document.querySelector('.whist-score button.primary').click()");await bidding();
      assert.equal(await evaluate('document.querySelectorAll(".fan .card").length'),profile.deals[1]);
      assert.equal(await evaluate('document.querySelector(".whist-auto input").checked'),false);break;
     case 'auto-score':
      await evaluate("document.querySelector('.whist-auto input').click()");await score();
      rounds.push({hand:2,cards:profile.deals[1],control:'AutoGame',score:await evaluate('document.querySelector(".whist-score-grid").textContent')});break;
     case 'return-control':
      await evaluate("document.querySelector('.whist-score button.primary').click()");await bidding();
      assert.equal(await evaluate('document.querySelector(".whist-auto input").checked'),false);break;
     default: throw Error('Unsupported paired step: '+step);
    }
    const image=profile.scenario.captureSteps.includes(step)?await capture(step):null;
    flowFrames.push({id:step,capture:image?.file??null});
    const after=await flowState();
    const assertions=Object.entries(expectedStates[step]).map(([name,expected])=>({name:step+' '+name,expected,observed:after[name],passed:Object.is(expected,after[name])}));
    const observedStep={id:'FLOW-'+step,name:step,keyword:'THEN',action:'Execute '+step,url:url.origin+url.pathname,source:'ACTION',durationMillis:Math.max(0,Math.round(after.at-stateBefore.at)),before:stateBefore,after,assertions,images:image?[image]:[],status:assertions.every(a=>a.passed)?'PASS':'FAIL'};
    flowSteps.push(observedStep);assert.ok(observedStep.status==='PASS','Shared flow step '+step+' preserves its expected state');
   });
   const samples=await evaluate('window.recordAnchors=false;window.anchorSamples');
   await fs.writeFile(path.join(output,id+'-samples.json'),JSON.stringify(samples,null,2));
   await fs.writeFile(path.join(output,id+'-manual.json'),JSON.stringify(manual,null,2));
   const reference=samples[0];let maxFrameDelta=0,maxPlacedDelta=0,maxHandDelta=0;
   {
    const measured=tableContinuity(samples);({maxFrameDelta,maxPlacedDelta,maxHandDelta}=measured);
    const geometrySteps=tableCheckpoints(samples,url.origin+url.pathname,manual);
    const steps=[...geometrySteps,...flowSteps].sort((a,b)=>a.after.at-b.after.at);
    assert.ok(steps.every(step=>step.status==='PASS'),'Every individual before/after comparison passes');
    assert.equal(measured.acceptedRemovals,rounds.reduce((n,hand)=>n+hand.cards,0),'Every human removal has its observed deposit');
    assert.ok(maxHandDelta<.02,'Unchanged hand cards stay fixed during AI turns: '+maxHandDelta);
    assert.ok(maxFrameDelta<.02,'Frames, board, header and private-hand bounds remain fixed across hands: '+maxFrameDelta);
    assert.ok(maxPlacedDelta<.02,'Deposited faces retain their slot until collection: '+maxPlacedDelta);
    assert.ok(samples.some(s=>s.collecting),'Observe actual collection');
    const assigned=new Set(steps.flatMap(step=>step.images.map(n=>n.file)));
    const flowBudget=verifyFlowExecution(profile,rounds,images,flowFrames);
    checks.push({id,name:'Fixed table through successive hands: '+profile.id,status:'PASS',expected:{maxDisplacementCssPx:.02,maximumHands:2,maximumNormalCaptures:4},observed:{profile,version,target:url.origin+url.pathname,rounds,flowBudget,notices,samples:samples.length,...measured,first:reference,last:samples.at(-1)},steps,images:images.filter(n=>!assigned.has(n.file))});
   }
   await fs.writeFile(path.join(output,'browser-ui-checks.json'),JSON.stringify(checks,null,2));
   }catch(error){
    const diagnosticFailures=[];
    if(!flowSteps.some(step=>step.status==='FAIL'&&step.images.length)){
     try{const image=await capture('failure');flowFrames.push({id:'failure',capture:image.file});}catch(captureError){diagnosticFailures.push(String(captureError));}
    }
    let diagnostic={};try{diagnostic=await evaluate('({samples:window.anchorSamples??[],text:document.body.innerText,error:document.querySelector(".error")?.textContent})');}catch(readError){diagnosticFailures.push(String(readError));}
    let flowBudget=null;try{flowBudget=verifyFlowExecution(profile,rounds,images,flowFrames,false);}catch(budgetError){diagnosticFailures.push(String(budgetError));}
    const failed={id,name:'Fixed table '+profile.id,status:'FAIL',expected:{completedPrefix:true,maxDisplacementCssPx:.02},observed:{profile,version,rounds,flowBudget,failure:String(error),diagnosticFailures,text:diagnostic.text,error:diagnostic.error},images:images.filter(image=>!flowSteps.some(step=>step.images.some(n=>n.file===image.file)))};
    if(flowSteps.length)failed.steps=flowSteps;
    checks.push(failed);
    try{await fs.writeFile(path.join(output,id+'-samples.json'),JSON.stringify(diagnostic.samples??[],null,2));await fs.writeFile(path.join(output,'browser-ui-checks.json'),JSON.stringify(checks,null,2));}catch(writeError){error.evidenceFailure=String(writeError);}
    throw error;
   }
  });
 }
}
if(/^https?:\/\//.test(target))await exercise(new URL(target));
else await withServer({classes:target,main:'ro.whistlab.browser.WhistBrowser',arguments:[path.join(output,'game-data'),path.join(repo,'whist-lab-app/app/src/main/res/drawable-nodpi/teo_dedication.png')]},process.env.ATF_BROWSER_WEB_ROOT??path.join(repo,'atf-ui-browser/src/main/web'),exercise);
console.log(JSON.stringify({status:'PASS',cases:checks.length,boundary:'Real JVM or compiled engine and browser; no native pixel acceptance claim'}));
