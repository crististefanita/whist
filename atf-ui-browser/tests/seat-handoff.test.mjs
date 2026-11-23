import assert from 'node:assert/strict';
import test from 'node:test';
import path from 'node:path';
import {rendererFixture} from './responsive-fixture.mjs';
import {withBrowser,until} from './fixtures.mjs';

test('intrinsic seat measurement is independent of assigned frame height and active action badge',async t=>{
 const {work,url}=await rendererFixture(t);
 await withBrowser(process.env.ATF_BROWSER_EXECUTABLE??'C:/Program Files (x86)/Microsoft/Edge/Application/msedge.exe',path.join(work,'profile'),async({send,evaluate})=>{
  await send('Page.enable');await send('Page.navigate',{url});await until(()=>evaluate('!!document.querySelector(".whist-home")'));await evaluate('document.fonts.ready');
  const observations=await evaluate(`(async()=>{
   const {fitSeatLines,updateSeatFact,measureNativeSeat}=await import('./seat-presentation.mjs');
   const rows=[];
   for(const compact of [true,false])for(const font of [16,20.8,25.6])for(const width of [88,116])for(const anchor of ['south','north','north-west','north-east']){
   document.documentElement.style.fontSize=font+'px';
   const host=document.createElement('section');host.className='match whist-match'+(compact?' compact-table':'');host.style.cssText='position:fixed;width:393px;height:626px;visibility:hidden';document.body.append(host);
   const seat=document.createElement('div');seat.className='seat '+anchor;seat.style.cssText='width:'+width+'px;height:60px';host.append(seat);
   const name=document.createElement('strong');name.textContent='Eva';
   const bid=document.createElement('span'),won=document.createElement('span'),badge=document.createElement('span');bid.className='seat-bid';won.className='seat-won';badge.className='seat-turn';
   seat.append(name,bid,won,badge);updateSeatFact(bid,'Licitat: 2');updateSeatFact(won,'Levate: 0');fitSeatLines([seat]);
   seat.style.inset='0 auto auto 0';const reference=measureNativeSeat(seat);
   for(const placed of [false,true])for(const height of [52,60,72,100])for(const active of [false,true]){
    seat.style.inset=placed?'100px auto auto 0px':'';
    seat.style.height=height+'px';seat.classList.toggle('active',active);badge.hidden=!active;badge.textContent='JOACĂ ACUM';
    rows.push({compact,font,width,anchor,placed,height,active,reference,...measureNativeSeat(seat)});
   }host.remove();}return rows;
  })()`);
  console.log('Seat handoff measurements: '+JSON.stringify(observations));
  for(const row of observations){assert.equal(row.bodyHeight,row.reference.bodyHeight,'Intrinsic demand ignores old anchors: '+JSON.stringify(row));assert.equal(row.nameHeight,row.reference.nameHeight);if(row.compact&&row.font===16){assert.equal(row.bodyHeight,43);assert.equal(row.nameHeight,15);}}
  assert.equal(observations.length,768,'The component owns the anchor, text and phase combinations');
 });
});
