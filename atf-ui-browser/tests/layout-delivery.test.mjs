import assert from 'node:assert/strict';
import fs from 'node:fs';
import path from 'node:path';
import test from 'node:test';
import {runInNewContext} from 'node:vm';
import {fileURLToPath,pathToFileURL} from 'node:url';
const web=process.env.ATF_BROWSER_WEB_ROOT??fileURLToPath(new URL('../src/main/web/',import.meta.url));
const {textScale,intrinsicTableHeight}=await import(pathToFileURL(path.join(web,'viewport.mjs')));
const source=fs.readFileSync(path.join(web,'table.mjs'),'utf8');
const start=source.indexOf('async function placeTable('),end=source.indexOf('function whistHeader(',start);
assert.ok(start>=0&&end>start,'Use the production layout and placement functions');
const settle=()=>new Promise(resolve=>setImmediate(resolve));

function harness({deferFonts=false,playerCount=3,fontSize="16px"}={}){
  const requests=[],fonts=[],seats=Array.from({length:playerCount},()=>({style:{},offsetHeight:52,dataset:{actorLabel:'PLAY'}}));
  const cards=Array.from({length:2},()=>({style:{}})),fan={children:cards,style:{},querySelectorAll:()=>[]};
  const center={style:{}},played={childElementCount:0},hand={clientWidth:414,querySelector:()=>fan};
  const board={clientWidth:414,clientHeight:400,isConnected:true,style:{minHeight:''},dataset:{},
    querySelectorAll:key=>key==='.seat'?seats:key==='[data-bid]'?[{},{}]:[],querySelector:key=>key==='.center'?center:key==='.cards-on-table'?played:null};
  const content={dataset:{},classList:{toggle(){}},hasAttribute:()=>false,querySelector:key=>key==='.board'?board:key==='.hand'?hand:null};
  const context={stage:{clientHeight:896,dataset:{},firstElementChild:content},state:{view:{header:{mode:'OH_HELL'}}},
    layoutRequests:new WeakMap(),playedLayouts:new WeakMap(),document:{documentElement:{}},surface:{schedule(){}},getComputedStyle:node=>({fontSize,height:node===board?'400px':'600px'}),
    textScale,intrinsicTableHeight,fitTrumpText:()=>deferFonts?new Promise(resolve=>fonts.push(resolve)):Promise.resolve(),fitBidSummary(){},fitSeatLines(){},measureNativeSeat:()=>({bodyHeight:43,nameHeight:15}),
    measureSeatActionClearance:()=>14,copy:(_key,fallback)=>fallback,placePlayed(){},errorBox:{textContent:''},
    call:(_path,fields)=>new Promise((resolve,reject)=>requests.push({fields,resolve,reject}))};
  const layout=runInNewContext(source.slice(start,end)+';placeTable',context);
  const result=top=>({board:{seats:seats.map((_,i)=>({top,left:i*100,right:i*100+90,bottom:top+60})),center:{left:10,top:100,right:400,bottom:300}},
    boardHeight:400,hand:{width:200,first:5,advance:40},played:null,bids:null});
  return {layout,context,board,seats,center,fan,requests,fonts,result};
}

test('font completion cannot let an older placement acquire a newer layout generation',async()=>{
  const h=harness({deferFonts:true}),older=h.layout(),newer=h.layout();
  assert.equal(h.fonts.length,2);h.fonts[1]();await settle();
  h.requests[0].resolve(h.result(40));await newer;
  h.fonts[0]();await settle();
  assert.equal(h.requests.length,1,'The superseded font continuation cannot request geometry');
  await older;
  assert.equal(h.seats[0].style.inset,'40px auto auto 0px');
});

test('navigation before fonts complete cannot measure or position a disconnected table',async()=>{
  const h=harness({deferFonts:true}),pending=h.layout();h.board.isConnected=false;
  h.fonts[0]();await settle();assert.equal(h.requests.length,0);await pending;assert.equal(h.seats[0].style.inset,undefined);
});

test('the production trump fitter does not write text after its layout owner expires',async()=>{
  const from=source.indexOf('async function fitTrumpText('),to=source.indexOf('async function placeTable(',from);
  let release,current=true,reads=0;const fits=[];
  const tile={clientWidth:100,children:[{},{}]},content={querySelector:()=>tile};
  const fit=runInNewContext(source.slice(from,to)+';fitTrumpText',{
    trumpFont:undefined,document:{fonts:{load:()=>new Promise(resolve=>release=resolve)}},
    getComputedStyle:()=>{reads++;return {paddingLeft:'0',paddingRight:'0'};},fitHeaderText:line=>fits.push(line)});
  const old=fit(content,()=>current);current=false;release([]);await old;
  assert.equal(reads,0);assert.equal(fits.length,0);
  await fit(content,()=>true);assert.equal(reads,1);assert.deepEqual(fits,tile.children);
});

test('fitting a taller scene cannot change the native policy of the useful viewport',async()=>{
  const h=harness();h.context.stage.clientHeight=1000;h.context.stage.dataset.layoutViewport='568';
  const toggles=[];h.context.stage.firstElementChild.classList.toggle=(name,value)=>toggles.push({name,value});
  const pending=h.layout();await settle();
  assert.equal(h.requests[0].fields.viewport,568,'The Android-style short-window policy uses useful height, not fitted content height');
  assert.deepEqual(toggles,[{name:'compact-table',value:true}]);
  h.requests[0].resolve(h.result(40));await pending;
});

test('only a current native minimum can update the intrinsic scene reservation',async()=>{
  const h=harness(),older=h.layout();await settle();const newer=h.layout();await settle();
  h.requests[1].resolve({...h.result(40),minimumBoardHeight:320});await newer;
  assert.equal(h.context.stage.firstElementChild.dataset.minimumHeight,'520');
  h.requests[0].resolve({...h.result(5),minimumBoardHeight:600});await older;
  assert.equal(h.context.stage.firstElementChild.dataset.minimumHeight,'520','An old allocation cannot inflate the next hand');
});

test('an older layout reply cannot move seats or cards after the latest measurement commits',async()=>{
  const h=harness(),older=h.layout();await settle();const newer=h.layout();await settle();assert.equal(h.requests.length,2);
  h.requests[1].resolve(h.result(40));await newer;
  const committed=JSON.stringify({seats:h.seats.map(n=>n.style),center:h.center.style,fan:h.fan.style,cards:h.fan.children.map(n=>n.style)});
  h.requests[0].resolve(h.result(5));await older;
  assert.equal(JSON.stringify({seats:h.seats.map(n=>n.style),center:h.center.style,fan:h.fan.style,cards:h.fan.children.map(n=>n.style)}),committed);
  assert.equal(h.seats[0].style.inset,'40px auto auto 0px');
  console.log('Layout delivery observations: reversed replies preserve committed seat, center, fan and card styles');
});

test('navigation and a new commit invalidate pending measurements without painting their old geometry',async()=>{
  for(const invalidation of ['detached','new-commit']){
    const h=harness(),pending=h.layout();await settle();
    if(invalidation==='detached')h.board.isConnected=false;else h.context.layoutRequests.set(h.board,{});
    h.requests[0].resolve(h.result(5));await pending;
    assert.equal(h.seats[0].style.inset,undefined);assert.equal(h.fan.children[0].style.left,undefined);
  }
});

test('stale layout failures do not replace the active error or cancel a newer measurement',async()=>{
  const h=harness(),older=h.layout();await settle();const newer=h.layout();await settle();
  h.requests[0].reject(new Error('Old measurement failed'));await older;assert.equal(h.context.errorBox.textContent,'');
  h.requests[1].resolve(h.result(40));await newer;assert.equal(h.seats[0].style.inset,'40px auto auto 0px');
});

test('a current layout failure exposes its original message without applying partial coordinates',async()=>{
  const h=harness(),pending=h.layout();await settle();h.requests[0].reject(new Error('Current measurement failed'));await pending;
  assert.equal(h.context.errorBox.textContent,'Current measurement failed');assert.equal(h.center.style.inset,undefined);
});


test('fractional mobile viewport reaches the integer layout boundary across bidding and play',async()=>{
 const observations=[];
 for(const viewport of [655.3599853515625,626.01,895.9])for(const playerCount of [3,4,5,6])for(const fontSize of ['16px','20.8px']){
  const h=harness({playerCount,fontSize});h.context.stage.dataset.layoutViewport=String(viewport);
  let biddingFrames;
  for(const phase of ['bidding','playing']){
   if(phase==='playing')h.context.stage.dataset.playingSteps='true';
   const pending=h.layout();await settle();const request=h.requests.at(-1);
   if(Number.isInteger(request.fields.viewport)&&request.fields.viewport>0)request.resolve(h.result(40));
   else request.reject(new Error('String contains invalid digits: '+request.fields.viewport));
   await pending;
   assert.equal(h.context.errorBox.textContent,'','A valid fractional mobile measurement must not become a visible game or score error');
   assert.equal(request.fields.viewport,Math.floor(viewport),'Available height is floored once before the integer protocol');
   assert.equal(request.fields.players,playerCount);assert.equal(request.fields.fontScale,parseFloat(fontSize)/16);
   assert.equal(request.fields.bidOptions,phase==='bidding'?2:0);
   const frames=h.seats.map(n=>n.style.inset);if(biddingFrames)assert.deepEqual(frames,biddingFrames);else biddingFrames=frames;
   observations.push({viewport,normalized:request.fields.viewport,playerCount,fontSize,phase,error:h.context.errorBox.textContent,fixedFrames:frames});
  }
 }
 assert.equal(observations.length,48);
 console.log('Fractional viewport observations: '+JSON.stringify(observations));
});

test('malformed viewport still preserves the layout rejection and never paints seat positions',async()=>{
 for(const value of ['NaN','Infinity','-1','0']){
  const h=harness();h.context.stage.dataset.layoutViewport=value;
  const pending=h.layout();await settle();const request=h.requests[0];
  assert.ok(!Number.isInteger(request.fields.viewport)||request.fields.viewport<=0);
  request.reject(new Error('Geometry input out of range'));await pending;
  assert.equal(h.context.errorBox.textContent,'Geometry input out of range');
  assert.equal(h.seats[0].style.inset,undefined,'Invalid input must not silently acquire a zero-sized layout');
 }
 console.log('Invalid viewport observations: malformed and non-positive dimensions preserve the original rejection');
});
