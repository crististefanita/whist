import assert from 'node:assert/strict';
import test from 'node:test';
import fs from 'node:fs/promises';
import path from 'node:path';
import {rendererFixture} from './responsive-fixture.mjs';
import {withBrowser,until} from './fixtures.mjs';

test('native-style fitted seat lines keep their measured height across bids, won tricks and actors',async t=>{
 const {work,url}=await rendererFixture(t);
 await withBrowser(process.env.ATF_BROWSER_EXECUTABLE??'C:/Program Files (x86)/Microsoft/Edge/Application/msedge.exe',path.join(work,'profile'),async({send,evaluate})=>{
  await send('Page.enable');await send('Page.navigate',{url});await until(()=>evaluate('!!document.querySelector(".whist-home")'));await evaluate('document.fonts.ready');
  const observed=await evaluate(`(async()=>{
   const {fitSeatLines,updateSeatFact,measureNativeSeat}=await import('./seat-presentation.mjs');const profiles=[];
   for(const players of [4,5])for(const width of [240,280,320,374])for(const font of [16,20.8,25.6])for(const language of ['ro','en']){
    document.documentElement.style.fontSize=font+'px';const host=document.createElement('section');host.className='match whist-match compact-table';host.style.cssText='position:fixed;visibility:hidden;width:'+width+'px';document.body.append(host);
    const seat=document.createElement('div');seat.className='seat';seat.style.width=Math.min(116,Math.round(width*.25),Math.floor((width-16)/3))+'px';
    const name=document.createElement('strong');name.textContent='Ana Maria';const bid=document.createElement('span'),won=document.createElement('span');bid.className='seat-bid';won.className='seat-won';seat.append(name,bid,won);host.append(seat);
    const rows=[];
    for(const value of ['—',...Array.from({length:18},(_,i)=>i)]){
     seat.classList.toggle('active',typeof value==='number'&&value%2===0);
     updateSeatFact(bid,(language==='ro'?'Licitat: ':'Bid: ')+value);updateSeatFact(won,(language==='ro'?'Levate: ':'Won: ')+value);fitSeatLines([seat]);
     const measured=measureNativeSeat(seat),line=node=>{const box=node.getBoundingClientRect(),ink=node.firstChild.getBoundingClientRect();return {height:node.offsetHeight,inkWidth:ink.width,available:box.width,whiteSpace:getComputedStyle(node.firstChild).whiteSpace};};
     rows.push({value,frame:seat.offsetHeight,...measured,name:line(name),bid:line(bid),won:line(won)});
    }
    profiles.push({players,width,font,language,rows});host.remove();
   }
   const legacy=document.createElement('div');legacy.className='seat';const fact=document.createElement('span');fact.textContent='First fact\\nSecond fact';legacy.append(fact);document.body.append(legacy);
   const legacyWhitespace=getComputedStyle(fact).whiteSpace;legacy.remove();
   return {profiles,legacyWhitespace,boundary:'Production seat fitter and CSS; explicit native one-line contract, not an Android raster execution'};
  })()`);
  const verify=profile=>{
   const first=profile.rows[0];
   for(const row of profile.rows){
    assert.equal(row.bodyHeight,first.bodyHeight,'Intrinsic frame height must not depend on '+JSON.stringify({players:profile.players,width:profile.width,font:profile.font,language:profile.language,value:row.value}));
    assert.equal(row.frame,first.frame,'Assigned natural frame height remains stable');
   }
   for(const row of profile.rows){
    for(const role of ['name','bid','won']){
     assert.equal(row[role].height,first[role].height,'A fitted role does not wrap');
     assert.ok(row[role].inkWidth<=row[role].available+.02,'Complete ink fits horizontally');
     assert.equal(row[role].whiteSpace,'pre','Explicit line content stays intact without soft wrapping');
    }
   }
  };
  if(process.env.ATF_COMPONENT_CAPTURE_DIR){await fs.mkdir(process.env.ATF_COMPONENT_CAPTURE_DIR,{recursive:true});await fs.writeFile(path.join(process.env.ATF_COMPONENT_CAPTURE_DIR,'seat-measurements.json'),JSON.stringify(observed,null,2));}
  observed.profiles.forEach(verify);assert.equal(observed.profiles.length,48);assert.equal(observed.legacyWhitespace,'pre-line','Unfitted legacy facts keep their explicit multiline contract');
  const mutation=structuredClone(observed.profiles[0]);mutation.rows[1].bodyHeight++;
  assert.throws(()=>verify(mutation),/Intrinsic frame height/,'The oracle rejects even one changed layout unit');
  console.log('Seat measurement observations: '+JSON.stringify({profiles:observed.profiles.length,valuesPerProfile:19,stableIntrinsicHeights:true,completeFittedInk:true,legacyWhitespace:observed.legacyWhitespace,negativeMutationRejected:true}));
 });
});
