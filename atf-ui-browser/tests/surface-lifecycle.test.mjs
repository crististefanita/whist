import assert from 'node:assert/strict';
import test from 'node:test';
import path from 'node:path';
import {fileURLToPath,pathToFileURL} from 'node:url';
const web=process.env.ATF_BROWSER_WEB_ROOT??fileURLToPath(new URL('../src/main/web/',import.meta.url));
const {boundedPages,surfaceFitter}=await import(pathToFileURL(path.join(web,'surface-fit.mjs')));
const {measureSeatActionClearance}=await import(pathToFileURL(path.join(web,'seat-presentation.mjs')));

function environment(t){
  const observers=[],frames=new Map(),delivered=[],created=[];let sequence=0;
  const element=tag=>({tag,children:[],dataset:{},style:{setProperty(name,value){this[name]=value;},removeProperty(name){delete this[name];}},
    classList:{add(){}},setAttribute(name,value){this[name]=value;},append(...nodes){this.children.push(...nodes);},
    matches(selector){return selector.split(',').includes('.'+this.className);},after(node){this.navigation=node;},remove(){this.removed=true;}});
  class Observer{
    constructor(callback){this.callback=callback;this.targets=[];this.observations=0;observers.push(this);}
    observe(target){this.targets.push(target);this.observations++;}
    disconnect(){this.targets=[];}
  }
  const document={documentElement:{lang:'en',dataset:{surface:'whist'}},createElement(tag){const n=element(tag);created.push(n);return n;}};
  const replacements={document,ResizeObserver:Observer,MutationObserver:Observer,
    requestAnimationFrame(callback){frames.set(++sequence,callback);delivered.push(callback);return sequence;},
    cancelAnimationFrame(id){frames.delete(id);}};
  for(const [key,value] of Object.entries(replacements)){
    const descriptor=Object.getOwnPropertyDescriptor(globalThis,key);
    Object.defineProperty(globalThis,key,{configurable:true,writable:true,value});
    t.after(()=>descriptor?Object.defineProperty(globalThis,key,descriptor):delete globalThis[key]);
  }
  const stage={firstElementChild:{},scrollHeight:100,clientHeight:100,events:new Map(),
    querySelector(){return null;},querySelectorAll(){return [];},
    addEventListener(name,callback){this.events.set(name,callback);},removeEventListener(name){this.events.delete(name);}};
  const dialog={...element('dialog'),open:false,scrollHeight:600,clientHeight:600,querySelectorAll(){return [];},contains(target){return target===this.child;}};
  const flush=()=>{const pending=[...frames.values()];frames.clear();pending.forEach(callback=>callback());};
  return {element,document,stage,dialog,observers,frames,delivered,created,flush};
}

test('page boundaries preserve every row and the ranking counter in RO and EN',t=>{
  const e=environment(t);
  for(const language of ['ro','en']){
    e.document.documentElement.lang=language;
    const nodes=Array.from({length:7},()=>e.element('li')),host=e.element('ol');host.className='ranking-list';host.children=nodes;
    const page=boundedPages(host,[nodes.slice(0,5),nodes.slice(5)]),[previous,position,next]=page.nav.children;
    assert.equal(position.textContent,'1 / 2');assert.equal(previous.disabled,true);assert.equal(host.style.counterReset,'rank 0');
    previous.onclick();assert.equal(host.dataset.page,'1','A delayed boundary event cannot hide all content');
    next.onclick();assert.equal(host.style.counterReset,'rank 5');assert.deepEqual(nodes.map(n=>n.hidden),[true,true,true,true,true,false,false]);
    next.onclick();assert.equal(position.textContent,'2 / 2');assert.equal(next.disabled,true);
    previous.onclick();assert.equal(position.textContent,'1 / 2');assert.equal(nodes.filter(n=>!n.hidden).length,5);
    assert.equal(next.textContent,language==='en'?'Next':'Înainte');assert.equal(new Set(page.nodes).size,7);
  }
});

test('invalid page populations fail before creating controls or hiding rows',t=>{
  const e=environment(t),a=e.element('li'),b=e.element('li'),host=e.element('ol');host.children=[a,b];
  for(const pages of [undefined,[],[[a]],[[a],[a]],[[a,b],[]],[[a],{}],[[a],[e.element('li')]]]){
    assert.throws(()=>boundedPages(host,pages),TypeError);
    assert.equal(e.created.length,0);assert.equal(a.hidden,undefined);assert.equal(b.hidden,undefined);
  }
});

test('replaced pagination rejects captured callbacks from its previous owner',t=>{
  const e=environment(t),host=e.element('div');host.children=[e.element('p'),e.element('p')];
  const old=boundedPages(host,host.children.map(node=>[node])),stale=old.nav.children[2].onclick;
  old.dispose();old.dispose();host.children=[e.element('p')];
  const replacement=boundedPages(host,[[host.children[0]]]);stale();
  assert.equal(host.dataset.page,'1');assert.equal(host.children[0].hidden,false);assert.equal(old.nav.removed,true);
  assert.equal(replacement.nav.hidden,true,'A single page does not add unnecessary navigation');
});

test('resize requests coalesce while collection and AutoGame retain scene ownership',t=>{
  const e=environment(t);let fits=0;
  const fitting=surfaceFitter(e.stage,e.dialog,()=>fits++),[resize,mutations]=e.observers;
  fitting.schedule();fitting.schedule();assert.equal(e.frames.size,1);e.flush();assert.equal(fits,1);
  mutations.callback([{target:e.stage}]);assert.equal(e.frames.size,0,'Appending a collection overlay preserves the root');
  e.stage.events.get('change')({target:{closest:()=>null}});assert.equal(e.frames.size,0,'AutoGame does not refit');
  e.stage.firstElementChild={};mutations.callback([{target:e.stage}]);e.flush();assert.equal(fits,2);
  assert.deepEqual(resize.targets,[e.stage.firstElementChild]);
  e.stage.scrollHeight=150;resize.callback();resize.callback();assert.equal(e.frames.size,1);e.flush();assert.equal(fits,3);
  e.stage.events.get('change')({target:{closest:()=>({})}});e.flush();assert.equal(fits,4,'Settings changes do refit');
  fitting.dispose();
});

test('disposal rejects late animation frames observers and retained form events',t=>{
  const e=environment(t);let fits=0;
  const fitting=surfaceFitter(e.stage,e.dialog,()=>fits++),[resize,mutations]=e.observers;
  const oldForm=e.stage.events.get('change');fitting.schedule();const late=e.delivered[0];
  fitting.dispose();fitting.dispose();assert.equal(e.frames.size,0);assert.equal(e.stage.events.size,0);
  e.stage.firstElementChild={};e.stage.scrollHeight=150;
  mutations.callback([{target:e.stage}]);resize.callback();oldForm({target:{closest:()=>({})}});late();fitting.schedule();
  assert.equal(fits,0);assert.equal(e.frames.size,0);assert.equal(resize.observations,0,'Late mutation delivery cannot reconnect observers');
  e.dialog.open=true;assert.doesNotThrow(()=>fitting.fitDialog(0,0));assert.equal(e.dialog.style['--modal-scale'],undefined);
});

test('closed and legacy surfaces do not consume a fitting frame',t=>{
  const e=environment(t);let fits=0;const fitting=surfaceFitter(e.stage,e.dialog,()=>fits++);
  assert.doesNotThrow(()=>fitting.fitDialog(0,0));
  e.document.documentElement.dataset.surface='legacy';fitting.schedule();e.flush();assert.equal(fits,0);
  e.document.documentElement.dataset.surface='whist';e.stage.querySelector=()=>({});fitting.schedule();e.flush();assert.equal(fits,0,'A staged render does not expose intermediate geometry');
  fitting.dispose();
});

test('modal geometry fits its content once and removes temporary measurement styles',t=>{
  const e=environment(t);e.dialog.open=true;e.dialog.scrollHeight=1000;
  const fitting=surfaceFitter(e.stage,e.dialog,()=>{});fitting.fitDialog(414,896);
  const height=parseFloat(e.dialog.style['--modal-height']),scale=e.dialog.style['--modal-scale'];
  assert.equal(height,1002);assert.ok(Math.abs(height*scale-896)<.001);
  assert.ok(parseFloat(e.dialog.style['--modal-width'])*scale<=414);assert.equal(e.dialog.style.height,undefined);
  fitting.dispose();
});

test('action bands measure both phase labels and always remove their probe',()=>{
  let probe;const badge={hidden:true,textContent:'Original',cloneNode(){return probe={style:{},hidden:true,textContent:'',get offsetTop(){return this.textContent==='PLAY NOW'?-28:-14;},remove(){this.removed=true;}};}};
  const seat={querySelector:()=>badge,append(){}};
  for(const labels of [['BID','PLAY NOW'],['PLAY NOW','BID']]){
    assert.equal(measureSeatActionClearance(seat,labels),28);assert.equal(probe.removed,true);assert.equal(badge.textContent,'Original');
  }
  const failure=new Error('Font measurement failed');
  badge.cloneNode=()=>probe={style:{},set textContent(value){throw failure;},remove(){this.removed=true;}};
  assert.throws(()=>measureSeatActionClearance(seat,['PLAY NOW']),error=>error===failure);assert.equal(probe.removed,true);
});

test('short native dialogs retain natural height while catalogues and rankings fill the scene',t=>{
  const e=environment(t);e.dialog.open=true;e.dialog.scrollHeight=176;
  const fitting=surfaceFitter(e.stage,e.dialog,()=>{});
  const scene={width:393,height:626,scale:720/626};
  fitting.fitDialog(1280,720,false,scene);
  assert.equal(e.dialog.style['--modal-height'],'178px');
  assert.equal(e.dialog.style['--modal-scale'],scene.scale,'Short dialogs do not magnify independently');
  for(const kind of ['achievement-catalogue','game-rankings']){
    e.dialog.className=kind;fitting.fitDialog(1280,720,false,scene);
    assert.equal(e.dialog.style['--modal-height'],'626px','Long read-only dialogs retain their full scene');
  }
  assert.equal(e.dialog.style.height,undefined);
  fitting.dispose();
});
