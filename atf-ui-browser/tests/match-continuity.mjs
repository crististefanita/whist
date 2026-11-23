import assert from 'node:assert/strict';
import fs from 'node:fs/promises';
import path from 'node:path';
import {fileURLToPath} from 'node:url';
import {withServer,withBrowser,until} from './fixtures.mjs';
import {tableContinuity} from './table-continuity.mjs';
import {tableCheckpoints} from './table-checkpoints.mjs';

const [target,output,selection='all',lastCards='3']=process.argv.slice(2),maximum=Number(lastCards);
if(!target||!output)throw Error('Supply JVM classpath or public URL, new evidence directory and optional named profile');
assert.ok(Number.isInteger(maximum)&&maximum>=3&&maximum<=8,'The final hand must contain 3–8 cards');
const profiles=[
 {id:'four-compact',players:4,width:320,height:568,font:20.8,language:'ro',mobile:true},
 {id:'four-tall',players:4,width:414,height:896,font:16,language:'en',mobile:true},
 {id:'four-desktop',players:4,width:1920,height:1080,font:20.8,language:'en',mobile:false},
 {id:'five-compact',players:5,width:393,height:626,font:20.8,language:'ro',mobile:true},
 {id:'five-desktop',players:5,width:1280,height:720,font:20.8,language:'en',mobile:false}
];
assert.ok(selection==='all'||profiles.some(p=>p.id===selection),'Select an explicit supported profile');
await fs.mkdir(output,{recursive:true});
const checks=[],repo=fileURLToPath(new URL('../../',import.meta.url));

async function exercise(url){
 for(const profile of profiles.filter(p=>selection==='all'||p.id===selection)){
  await withBrowser(process.env.ATF_BROWSER_EXECUTABLE??'C:/Program Files (x86)/Microsoft/Edge/Application/msedge.exe',path.join(output,'profile-'+profile.id),async({send,evaluate,version})=>{
   const id='MATCH-ANCHORS-'+profile.id,images=[];
   try{
   await send('Page.enable');await send('Emulation.setDeviceMetricsOverride',{width:profile.width,height:profile.height,mobile:profile.mobile,deviceScaleFactor:1});
   await send('Emulation.setUserAgentOverride',{userAgent:profile.mobile?'Mozilla/5.0 (Linux; Android 9; Mobile)':'Mozilla/5.0 (Windows NT 10.0; Win64; x64)'});
   await send('Page.navigate',{url:url.href});await until(()=>evaluate('!!document.querySelector(".whist-home .primary")'),30000);await evaluate('document.fonts.ready');
   await evaluate(`document.documentElement.style.fontSize='${profile.font}px';document.querySelector('.whist-language select').value='${profile.language}';document.querySelector('.whist-language select').dispatchEvent(new Event('change'))`);
   await until(()=>evaluate('document.querySelector("#table").getAttribute("aria-busy")==="false"'));
   await evaluate(`document.querySelector('.whist-home .primary').click();document.querySelector('[name=players]').value='${profile.players}';document.querySelector('[name=players]').dispatchEvent(new Event('change'));document.querySelector('[name=mode]').value='ROMANIAN_WHIST';document.querySelector('[name=mode]').dispatchEvent(new Event('change'));document.querySelector('.setup form').requestSubmit()`);
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
    window.snapshotAnchors=()=>({...snapshot(),at:performance.now()});
    function tick(){if(!recordAnchors)return;const sample=snapshot();if(sample){const value=JSON.stringify(sample);if(value!==previous){anchorSamples.push({...sample,at:performance.now()});previous=value;}}requestAnimationFrame(tick);}tick();
   })()`);
   const rounds=[],manual=[];
   const capture=async suffix=>{const file=id+'-'+suffix+'.png';const png=await send('Page.captureScreenshot',{format:'png'});await fs.writeFile(path.join(output,file),Buffer.from(png.data,'base64'));const item={file,title:id+' '+suffix};images.push(item);return item;};
   for(let hand=0;hand<profile.players+maximum-1;hand++){
    await until(()=>evaluate('!!document.querySelector(".bid-submit")&&document.querySelector("#table").getAttribute("aria-busy")==="false"'),30000);
    const cards=await evaluate('document.querySelectorAll(".fan .card").length');
    assert.equal(cards,hand<profile.players?1:hand-profile.players+2,'Exercise the complete one-card prefix then two and three cards');
    if(hand===0||cards===maximum)await capture(cards+'-card-bidding');
    const manualHand=hand===0||cards>=4&&cards%2===0;
    if(manualHand){
     await evaluate("document.querySelector('.bid-submit').click()");
     for(let trick=0;trick<cards;trick++){
      await until(()=>evaluate('!!document.querySelector(".fan .legal-card")&&document.querySelector("#table").getAttribute("aria-busy")==="false"'),30000);
      const before=await evaluate('window.snapshotAnchors()'),face=await evaluate('document.querySelector(".fan .legal-card").dataset.face'),captures=[];
      if(trick===0&&(hand===0||cards===4||cards===maximum))captures.push(await capture('hand-'+(hand+1)+'-before-human'));
      await evaluate("document.querySelector('.fan .legal-card').click()");
      await until(()=>evaluate(`[...document.querySelectorAll('.played-card[data-seat="0"] .card')].some(n=>n.dataset.face===${JSON.stringify(face)})`),30000);
      const after=await evaluate('window.snapshotAnchors()');
      if(trick===0&&(hand===0||cards===4||cards===maximum))captures.push(await capture('hand-'+(hand+1)+'-after-human'));
      manual.push({hand:before.hand,key:'0/'+face,before,after,images:captures});
     }
    }else await evaluate("document.querySelector('.whist-auto input').click()");
    await until(()=>evaluate('!!document.querySelector(".whist-score")&&document.querySelector("#table").getAttribute("aria-busy")==="false"'),60000);
    rounds.push({hand:hand+1,cards,control:manualHand?'manual':'AutoGame',score:await evaluate('document.querySelector(".whist-score-grid").textContent')});
    if(await evaluate('!!document.querySelector(".achievement-notice[open]")')){
     await evaluate(`(()=>{window.noticeAcknowledged=false;let started=false;const stage=document.querySelector('#table');const observer=new MutationObserver(()=>{if(stage.getAttribute('aria-busy')==='true')started=true;else if(started){observer.disconnect();window.noticeAcknowledged=true;}});observer.observe(stage,{attributes:true,attributeFilter:['aria-busy']});document.querySelector('.achievement-notice[open] > button').click();})()`);
     await until(()=>evaluate('window.noticeAcknowledged&&!document.querySelector("dialog[open]")&&document.querySelector("#table").getAttribute("aria-busy")==="false"'),30000);
    }
    if(hand<profile.players+maximum-2)await evaluate("document.querySelector('.whist-score button.primary').click()");
   }
   const samples=await evaluate('window.recordAnchors=false;window.anchorSamples');
   await fs.writeFile(path.join(output,id+'-samples.json'),JSON.stringify(samples,null,2));
   await fs.writeFile(path.join(output,id+'-manual.json'),JSON.stringify(manual,null,2));
   const reference=samples[0];let maxFrameDelta=0,maxPlacedDelta=0,maxHandDelta=0;
   try{
    const measured=tableContinuity(samples);({maxFrameDelta,maxPlacedDelta,maxHandDelta}=measured);
    const steps=tableCheckpoints(samples,url.origin+url.pathname,manual);
    assert.ok(steps.every(step=>step.status==='PASS'),'Every individual before/after comparison passes');
    assert.equal(measured.acceptedRemovals,rounds.reduce((n,hand)=>n+hand.cards,0),'Every human removal has its observed deposit');
    assert.ok(maxHandDelta<.02,'Unchanged hand cards stay fixed during AI turns: '+maxHandDelta);
    assert.ok(maxFrameDelta<.02,'Frames, board, header and private-hand bounds remain fixed across hands: '+maxFrameDelta);
    assert.ok(maxPlacedDelta<.02,'Deposited faces retain their slot until collection: '+maxPlacedDelta);
    assert.ok(samples.some(s=>s.collecting),'Observe actual collection');
    const assigned=new Set(steps.flatMap(step=>step.images.map(n=>n.file)));
    checks.push({id,name:'Fixed table through successive hands: '+profile.id,status:'PASS',expected:{maxDisplacementCssPx:.02,completeOneCardPrefix:true,maximumCards:maximum},observed:{profile,version,target:url.origin+url.pathname,rounds,samples:samples.length,...measured,first:reference,last:samples.at(-1)},steps,images:images.filter(n=>!assigned.has(n.file))});
   }catch(error){await capture('FAIL');checks.push({id,name:'Fixed table '+profile.id,status:'FAIL',expected:{maxDisplacementCssPx:.02},observed:{profile,version,rounds,maxFrameDelta,maxPlacedDelta,failure:error.message},images});await fs.writeFile(path.join(output,'browser-ui-checks.json'),JSON.stringify(checks,null,2));throw error;}
   await fs.writeFile(path.join(output,'browser-ui-checks.json'),JSON.stringify(checks,null,2));
   }catch(error){
    if(!checks.some(check=>check.id===id&&check.status==='FAIL')){
     const diagnostic=await evaluate('({samples:window.anchorSamples??[],text:document.body.innerText,error:document.querySelector(".error")?.textContent})');
     await fs.writeFile(path.join(output,id+'-samples.json'),JSON.stringify(diagnostic.samples,null,2));
     const file=id+'-failure-context.png',png=await send('Page.captureScreenshot',{format:'png'});await fs.writeFile(path.join(output,file),Buffer.from(png.data,'base64'));
     checks.push({id,name:'Fixed table '+profile.id,status:'FAIL',expected:{completedPrefix:true,maxDisplacementCssPx:.02},observed:{profile,version,failure:error.message,text:diagnostic.text,error:diagnostic.error},images:[...images,{file,title:id+' original failure'}]});
     await fs.writeFile(path.join(output,'browser-ui-checks.json'),JSON.stringify(checks,null,2));
    }throw error;
   }
  });
 }
}
if(/^https?:\/\//.test(target))await exercise(new URL(target));
else await withServer({classes:target,main:'ro.whistlab.browser.WhistBrowser',arguments:[path.join(output,'game-data'),path.join(repo,'whist-lab-app/app/src/main/res/drawable-nodpi/teo_dedication.png')]},process.env.ATF_BROWSER_WEB_ROOT??path.join(repo,'atf-ui-browser/src/main/web'),exercise);
console.log(JSON.stringify({status:'PASS',cases:checks.length,boundary:'Real JVM or compiled engine and browser; no native pixel acceptance claim'}));
