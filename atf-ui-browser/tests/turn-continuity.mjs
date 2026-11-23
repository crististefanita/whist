import assert from 'node:assert/strict';
import fs from 'node:fs/promises';
import path from 'node:path';
import {fileURLToPath} from 'node:url';
import {withServer,withBrowser,until} from './fixtures.mjs';

// Focused real-game browser check; component contracts own the larger presentation matrix.
const [target,output,executable='C:/Program Files (x86)/Microsoft/Edge/Application/msedge.exe']=process.argv.slice(2);
if(!target||!output)throw Error('Supply a JVM classpath or published URL, a new external output directory and optional Chromium executable');
await fs.mkdir(output,{recursive:true});
const checks=[],profiles=[{players:5,width:1280,height:720,mobile:false,font:20.8,language:'en'},
  {players:6,width:414,height:896,mobile:true,font:16,language:'ro'},
  {players:5,width:393,height:626,mobile:true,font:20.8,language:'ro'},
  {players:6,width:1280,height:720,mobile:false,font:20.8,language:'en'}];
async function exercise(url){
 for(const p of profiles)await withBrowser(executable,path.join(output,'browser-profile-'+p.players+'-'+p.width),async({send,evaluate,version})=>{
  await send('Page.enable');
   await send('Emulation.setDeviceMetricsOverride',{width:p.width,height:p.height,mobile:p.mobile,deviceScaleFactor:1});
   await send('Emulation.setUserAgentOverride',{userAgent:p.mobile?'Mozilla/5.0 (Linux; Android 9; Mobile)':'Mozilla/5.0 (Windows NT 10.0; Win64; x64)'});
   await send('Page.navigate',{url:url.href});
   try{await until(()=>evaluate('!!document.querySelector(".whist-home .primary")'),20000);}
   catch(error){
    await fs.writeFile(path.join(output,'startup-diagnostic.json'),JSON.stringify(await evaluate('({url:location.href,html:document.documentElement.outerHTML})'),null,2));
    throw error;
   }
   await evaluate('document.fonts.ready');
   await evaluate(`document.documentElement.style.fontSize='${p.font}px';document.querySelector('.whist-language select').value='${p.language}';document.querySelector('.whist-language select').dispatchEvent(new Event('change'))`);
   await until(()=>evaluate(`document.documentElement.lang==='${p.language}'&&document.querySelector('#table').getAttribute('aria-busy')==='false'`));
   await evaluate(`document.querySelector('.whist-home .primary').click();document.querySelector('[name=players]').value='${p.players}';document.querySelector('[name=players]').dispatchEvent(new Event('change'));document.querySelector('[name=mode]').value='OH_HELL';document.querySelector('[name=mode]').dispatchEvent(new Event('change'));document.querySelector('.setup form').requestSubmit()`);
   await until(()=>evaluate('!!document.querySelector(".board[data-layout=ready]")&&document.querySelector("#table").getAttribute("aria-busy")==="false"'),30000);
   await evaluate(`(()=>{window.seatSamples=[];window.seatSampling=true;const ids=new WeakMap();let sequence=0,previous='';const id=n=>{if(!ids.has(n))ids.set(n,++sequence);return ids.get(n);};
    const box=n=>{const r=n.getBoundingClientRect();return [r.left,r.top,r.width,r.height];};function tick(){if(!seatSampling)return;const view=document.querySelector('#table > .whist-match:not([data-preparing])');if(view){const s={owners:[id(view),id(view.querySelector(".board")),id(view.querySelector("header")),id(view.querySelector("header h1")),id(view.querySelector(".whist-trump"))],regions:Object.fromEntries(['header','.whist-trump','.board','.center','.hand'].map(key=>[key,box(view.querySelector(key))])),handFaces:[...view.querySelectorAll(".fan .card")].map(n=>({face:n.dataset.face,lifting:n.classList.contains("lifting-card"),rectangle:box(n)})),played:[...view.querySelectorAll('.played-card')].map(n=>({key:n.dataset.seat+'/'+n.querySelector('.card').dataset.face,rectangle:box(n.querySelector('.card'))})),seats:[...view.querySelectorAll('.board > .seat')].map(n=>({rectangle:box(n),name:box(n.querySelector('strong .seat-line-ink')),rows:[...n.querySelectorAll(':scope > strong,:scope > .seat-bid,:scope > .seat-won')].map(box),identity:[n,...n.querySelectorAll(':scope > strong,:scope > .seat-bid,:scope > .seat-won,.seat-line-ink')].map(id)})),actor:view.querySelector('.seat[aria-current]')?.dataset.seat,cards:view.querySelectorAll('.fan .card').length,collecting:document.querySelector('#table').hasAttribute('data-collecting'),busy:document.querySelector('#table').getAttribute('aria-busy')};const json=JSON.stringify(s);if(json!==previous){seatSamples.push(s);previous=json;}}requestAnimationFrame(tick);}tick();})()`);
   await evaluate("document.querySelector('.bid-submit').click()");
   await until(()=>evaluate('!document.querySelector(".bid-submit")&&document.querySelector("#table").getAttribute("aria-busy")==="false"'),30000);
   const initial=await evaluate('document.querySelectorAll(".fan .card").length');
   for(let trick=0;trick<3;trick++){
    await until(()=>evaluate('!!document.querySelector(".fan .legal-card")&&document.querySelector("#table").getAttribute("aria-busy")==="false"'),30000);
    const count=await evaluate('document.querySelectorAll(".fan .card").length');
    await evaluate("document.querySelector('.fan .legal-card').click()");
    await until(()=>evaluate('document.querySelectorAll(".fan .card").length==='+String(count-1)),30000);
    await until(()=>evaluate('document.querySelector("#table").getAttribute("aria-busy")==="false"'),30000);
   }
   await evaluate("document.querySelector('.whist-auto input').click()");
   await until(()=>evaluate('document.querySelectorAll(".fan .card").length==='+String(initial-4)),30000);
   await evaluate("document.querySelector('.whist-auto input').click()");
   await until(()=>evaluate('document.querySelector("#table").getAttribute("aria-busy")==="false"'),30000);
   const samples=await evaluate('window.seatSampling=false;window.seatSamples'),first=samples[0];
   await fs.writeFile(path.join(output,'TURN-CONTINUITY-'+p.players+'-'+p.width+'-samples.json'),JSON.stringify(samples,null,2));
   let maxDelta=0;for(const sample of samples)for(const [i,seat] of sample.seats.entries()){
    assert.deepEqual(sample.owners,first.owners,'Keep the visible view and board mounted across actual actor handoffs');
    assert.deepEqual(seat.identity,first.seats[i].identity,'Keep frame, row and fitted-ink nodes during actual actor handoffs');
    for(const [part,reference] of [[seat.rectangle,first.seats[i].rectangle],...seat.rows.map((r,j)=>[r,first.seats[i].rows[j]])])
     part.forEach((value,j)=>{maxDelta=Math.max(maxDelta,Math.abs(value-reference[j]));});
   }
   assert.ok(maxDelta<.02,'Fixed frame and row geometry: '+maxDelta);
   const placed=new Map();let maxCardDelta=0;
   for(const sample of samples)for(const card of sample.played){
    const origin=placed.get(card.key);if(origin)card.rectangle.forEach((value,i)=>{maxCardDelta=Math.max(maxCardDelta,Math.abs(value-origin[i]));});
    else placed.set(card.key,card.rectangle);
   }
   assert.ok(maxCardDelta<.02,'A placed card does not jump when another player deposits: '+maxCardDelta);
   let handBaseline=first.handFaces,waitingForFace;
   for(const sample of samples){
    const previous=handBaseline.map(n=>n.face).join('|'),current=sample.handFaces.map(n=>n.face).join('|');
    if(previous!==current){
      waitingForFace??=handBaseline.find(n=>!sample.handFaces.some(card=>card.face===n.face))?.face;
      if(!sample.busy||sample.busy==='false'||sample.played.some(n=>n.key==='0/'+waitingForFace)){
        handBaseline=sample.handFaces;waitingForFace=undefined;
      }
      continue;
    }
    sample.handFaces.forEach((card,i)=>{if(!card.lifting&&!handBaseline[i].lifting)card.rectangle.forEach((value,j)=>assert.ok(Math.abs(value-handBaseline[i].rectangle[j])<.02,'Unchanged human cards stay fixed during AI turns'));});
   }
   assert.equal(await evaluate('document.querySelectorAll(".fan .card").length'),initial-4,'Deselecting AutoGame preserves the committed trick without submitting another human card');
   assert.ok(samples.some(s=>s.collecting),'Observe collection as well as card placement');
   const id='TURN-CONTINUITY-'+p.players+'-'+p.width,images=[];
   const png=await send('Page.captureScreenshot',{format:'png'});await fs.writeFile(path.join(output,id+'.png'),Buffer.from(png.data,'base64'));images.push({file:id+'.png',title:id});
   await fs.writeFile(path.join(output,id+'-samples.json'),JSON.stringify(samples,null,2));
   checks.push({id,name:'Stable '+p.players+'-seat table through manual and automatic tricks',status:'PASS',expected:{unchangedFrameAndRowIdentity:true,maxDisplacementCssPx:.02,tricks:4},observed:{profile:p,version,target:url.origin+url.pathname,samples:samples.length,initialCards:initial,remainingCards:initial-4,maxDisplacementCssPx:maxDelta,maxPlayedCardDisplacementCssPx:maxCardDelta,first,last:samples.at(-1)},images});
   await fs.writeFile(path.join(output,'browser-ui-checks.json'),JSON.stringify(checks,null,2));
 });
}
if(/^https?:\/\//.test(target))await exercise(new URL(target));
else{
 const repo=fileURLToPath(new URL('../../',import.meta.url));
 await withServer({classes:target,main:'ro.whistlab.browser.WhistBrowser',arguments:[path.join(output,'game-data'),path.join(repo,'whist-lab-app/app/src/main/res/drawable-nodpi/teo_dedication.png')]},path.join(repo,'atf-ui-browser/src/main/web'),exercise);
}
console.log(JSON.stringify({status:'PASS',cases:checks.length,layer:'real-game browser; no native pixel acceptance claim'}));
