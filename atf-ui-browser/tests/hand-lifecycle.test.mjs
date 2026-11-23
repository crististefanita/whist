import assert from 'node:assert/strict';
import test from 'node:test';
import path from 'node:path';
import {fileURLToPath,pathToFileURL} from 'node:url';
const web=process.env.ATF_BROWSER_WEB_ROOT??fileURLToPath(new URL('../src/main/web/',import.meta.url));
const {bindHandFeedback}=await import(pathToFileURL(path.join(web,'hand-feedback.mjs')));
const settle=()=>new Promise(resolve=>setImmediate(resolve));

function hand(){
 const listeners=new Set(),classes=new Set(),effects=[];
 const card={dataset:{face:'A♠',command:'play:new'},disabled:false,
  classList:{toggle:(n,on)=>on?classes.add(n):classes.delete(n),add:n=>classes.add(n),remove:n=>classes.delete(n)},
  closest(){return this;},animate(){let finish;const finished=new Promise(r=>finish=r);const effect={finished,cancel:finish,finish};effects.push(effect);return effect;}};
 const fan={dataset:{},style:{setProperty(){}},querySelectorAll:()=>[card],
  addEventListener:(_event,callback)=>listeners.add(callback),removeEventListener:(_event,callback)=>listeners.delete(callback)};
 return {fan,card,listeners,classes,effects,click:()=>[...listeners].forEach(callback=>callback({target:card}))};
}
const timing={restLayer:1,liftLayer:-1,progress:[0,1],liftDp:20,liftScale:1.12,liftMillis:150,hintMillis:2000};
function bind(h,request,play=()=>{}){return bindHandFeedback(h.fan,{timing,request,play,active:()=>true,failed:error=>{throw error;}});}

test('a retained fan gets one fresh owner and ignores a pending reply from its disposed owner',async()=>{
 const h=hand(),calls=[],plays=[];let release;
 const old=bind(h,()=>new Promise(resolve=>release=resolve),key=>plays.push('old:'+key));h.click();await settle();
 old.dispose();old.dispose();assert.equal(h.listeners.size,0);
 const current=bind(h,fields=>{calls.push(fields);return Promise.resolve({action:'NONE',suggestions:[]});},key=>plays.push(key));
 release({action:'HINT',suggestions:['A♠'],revision:1});await settle();assert.equal(h.classes.has('suggested-card'),false);
 h.click();await settle();assert.equal(calls.length,1);assert.equal(h.listeners.size,1);assert.deepEqual(plays,[]);
 current.dispose();h.click();await settle();assert.equal(calls.length,1);
 console.log('Hand lifecycle observations: retained fan, one current listener, disposed reply ignored, no duplicate command');
});

test('disposing a lifted hand cancels its effect and cannot submit the old command',async t=>{
 const original=globalThis.matchMedia;globalThis.matchMedia=()=>({matches:false});t.after(()=>original?globalThis.matchMedia=original:delete globalThis.matchMedia);
 const h=hand(),plays=[],adapter=bind(h,async()=>({action:'PLAY',suggestions:[]}),key=>plays.push(key));
 h.click();await settle();assert.equal(h.effects.length,1);assert.equal(h.classes.has('lifting-card'),true);
 adapter.dispose();h.effects[0].finish();await settle();assert.deepEqual(plays,[]);assert.equal(h.classes.has('lifting-card'),false);
});

test('cancellation keeps the current click binding reusable while disposal releases it',async()=>{
 const h=hand(),calls=[],adapter=bind(h,async fields=>{calls.push(fields);return {action:'NONE',suggestions:[]};});
 adapter.cancel();h.click();await settle();assert.equal(calls.length,1);assert.equal(h.listeners.size,1);
 adapter.dispose();assert.equal(h.listeners.size,0);
});
