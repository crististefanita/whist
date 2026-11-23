import assert from 'node:assert/strict';
import fs from 'node:fs';
import path from 'node:path';
import test from 'node:test';
import {runInNewContext} from 'node:vm';
import {fileURLToPath,pathToFileURL} from 'node:url';
const web=process.env.ATF_BROWSER_WEB_ROOT??fileURLToPath(new URL('../src/main/web/',import.meta.url));
const {textScale}=await import(pathToFileURL(path.join(web,'viewport.mjs')));
const source=fs.readFileSync(path.join(web,'table.mjs'),'utf8');
const start=source.indexOf('async function placeTable('),end=source.indexOf('function whistHeader(',start);
assert.ok(start>=0&&end>start,'Use the production layout and placement functions');
const settle=()=>new Promise(resolve=>setImmediate(resolve));

function harness(){
  const requests=[],seats=Array.from({length:3},()=>({style:{},offsetHeight:52,dataset:{actorLabel:'PLAY'}}));
  const cards=Array.from({length:2},()=>({style:{}})),fan={children:cards,style:{},querySelectorAll:()=>[]};
  const center={style:{}},played={childElementCount:0},hand={clientWidth:414,querySelector:()=>fan};
  const board={clientWidth:414,clientHeight:400,isConnected:true,style:{minHeight:''},dataset:{},
    querySelectorAll:key=>key==='.seat'?seats:[],querySelector:key=>key==='.center'?center:key==='.cards-on-table'?played:null};
  const content={classList:{toggle(){}},hasAttribute:()=>false,querySelector:key=>key==='.board'?board:key==='.hand'?hand:null};
  const context={stage:{clientHeight:896,dataset:{},firstElementChild:content},state:{view:{header:{mode:'OH_HELL'}}},
    layoutRequests:new WeakMap(),playedLayouts:new WeakMap(),document:{documentElement:{}},getComputedStyle:()=>({fontSize:'16px'}),
    textScale,fitTrumpText:async()=>{},fitBidSummary(){},fitSeatLines(){},measureNativeSeat:()=>({bodyHeight:43,nameHeight:15}),
    measureSeatActionClearance:()=>14,copy:(_key,fallback)=>fallback,placePlayed(){},errorBox:{textContent:''},
    call:(_path,fields)=>new Promise((resolve,reject)=>requests.push({fields,resolve,reject}))};
  const layout=runInNewContext(source.slice(start,end)+';placeTable',context);
  const result=top=>({board:{seats:seats.map((_,i)=>({top,left:i*100,right:i*100+90,bottom:top+60})),center:{left:10,top:100,right:400,bottom:300}},
    boardHeight:400,hand:{width:200,first:5,advance:40},played:null,bids:null});
  return {layout,context,board,seats,center,fan,requests,result};
}

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
