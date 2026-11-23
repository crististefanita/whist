import assert from 'node:assert/strict';
import test from 'node:test';
import fs from 'node:fs';
import path from 'node:path';
import {fileURLToPath,pathToFileURL} from 'node:url';
import {createContext,runInContext} from 'node:vm';
const web=process.env.ATF_BROWSER_WEB_ROOT??fileURLToPath(new URL('../src/main/web/',import.meta.url));
const {autoGame}=await import(pathToFileURL(path.join(web,'auto-game.mjs')));
const source=fs.readFileSync(path.join(web,'table.mjs'),'utf8');
const settle=()=>new Promise(resolve=>setImmediate(resolve));
const deferred=()=>{let resolve,reject;const promise=new Promise((a,b)=>{resolve=a;reject=b;});return {promise,resolve,reject};};

// Run the production command owner and availability gate; only transport and presentation are controlled.
function harness(shell=source){
  const requests=[],presentations=[],waits=[],selections=[];
  const stage={dataset:{},setAttribute(){},querySelector:key=>key==='.board'||key==='.whist-match'?{}:null};
  const c=createContext({pending:false,stage,animation:undefined,selectedBid:undefined,
    state:{revision:0,view:{actions:[{key:'auto-step'}]}},errorBox:{textContent:''},
    history:{open:false},profileDialogs:{dialog:{open:false},notice(){}},rankingsOpening:false,handFeedback:undefined,
    copy:(_key,fallback)=>fallback,transport:{remember(){}},showStorageIssue(){},rememberLanguage(){},
    call:(route,fields)=>{const d=deferred();requests.push({route,fields,...d});return d.promise;},
    playCards:()=>{const d=deferred();presentations.push(d);return d.promise;},render:async()=>{}});
  const section=(start,end)=>{const from=shell.indexOf(start),to=shell.indexOf(end,from);assert.ok(from>=0&&to>from);return shell.slice(from,to);};
  runInContext(section('function dialogBlocksPlay()', 'history.addEventListener('),c);
  runInContext(section('async function issue(', 'async function playCards('),c);
  c.automation=autoGame({available:()=>!c.pending&&c.state.view.actions.some(n=>n.key==='auto-step'),
    wait:signal=>{const d=deferred();signal.addEventListener('abort',d.resolve,{once:true});waits.push({...d,signal});return d.promise;},
    step:async()=>!await c.issue('/api/action',{action:'auto-step'}),changed:value=>selections.push(value)});
  const reply=(revision,terminal=false,playback=false)=>({revision,collections:[],playback:playback?[{}]:[],view:{actions:terminal?[]:[{key:'auto-step'}]}});
  return {c,requests,presentations,waits,selections,reply};
}

async function pendingSelection(h){
  const manual=h.c.issue('/api/action',{action:'play:legal'});assert.equal(h.c.pending,true);
  h.c.automation.set(true);await settle();
  assert.equal(h.c.automation.selected(),true,'Keep selection during an owned manual request');
  assert.equal(h.waits.length,0);h.requests[0].resolve(h.reply(1,false,true));await settle();
  assert.equal(h.presentations.length,1);assert.equal(h.c.automation.selected(),true,'Keep selection through playback and collection');
  assert.equal(h.waits.length,0);return {manual};
}

test('selecting AutoGame during a manual request waits for presentation then submits once',async()=>{
  const h=harness();const {manual}=await pendingSelection(h);
  h.presentations[0].resolve();await manual;await settle();assert.equal(h.waits.length,1);
  h.waits[0].resolve();await settle();assert.equal(h.requests.length,2);
  assert.equal(h.requests[1].fields.action,'auto-step');h.requests[1].resolve(h.reply(2,true));await settle();
  assert.equal(h.c.automation.selected(),false);assert.equal(h.waits.length,1);
  console.log('AutoGame handoff observations: manual request and presentation retained selection; exactly one following command; terminal score reset');
});

test('deselection during an owned manual presentation prevents its next automated decision',async()=>{
  const h=harness();const {manual}=await pendingSelection(h);h.c.automation.set(false);
  h.presentations[0].resolve();await manual;await settle();assert.equal(h.waits.length,0);assert.equal(h.requests.length,1);
});

test('overlapping dialogs retain intent and resume only after the command and dialog both finish',async()=>{
  const h=harness();const {manual}=await pendingSelection(h);h.c.profileDialogs.dialog.open=true;h.c.refreshAutomationGate();
  h.presentations[0].resolve();await manual;await settle();assert.equal(h.waits.length,0);assert.equal(h.c.automation.selected(),true);
  h.c.profileDialogs.dialog.open=false;h.c.refreshAutomationGate();await settle();assert.equal(h.waits.length,1);
  h.c.automation.set(false);await settle();assert.equal(h.requests.length,1);
});

test('a rejected automated command stops without retries even when busy ownership aborted its wait',async()=>{
  const h=harness();h.c.automation.set(true);h.waits[0].resolve();await settle();assert.equal(h.requests.length,1);
  const original=Object.assign(new Error('Rejected move'),{status:400});h.requests[0].reject(original);await settle();
  assert.equal(h.c.errorBox.textContent,original.message);assert.equal(h.c.pending,false);
  assert.equal(h.c.automation.selected(),false);assert.equal(h.waits.length,1);assert.equal(h.requests.length,1);
});

test('rapid deselection and reselection cannot duplicate a committed command',async()=>{
  const h=harness();h.c.automation.set(true);h.waits[0].resolve();await settle();
  h.c.automation.set(false);h.c.automation.set(true);assert.equal(h.requests.length,1);
  h.requests[0].resolve(h.reply(1));await settle();assert.equal(h.waits.length,2);assert.equal(h.requests.length,1);
  h.c.automation.set(false);await settle();
});

test('the handoff oracle rejects the former gate that ignores pending command ownership',async()=>{
  const broken=source.replace('const blocked = pending || dialogBlocksPlay();','const blocked = dialogBlocksPlay();');
  assert.notEqual(broken,source);const h=harness(broken);
  await assert.rejects(pendingSelection(h),/Keep selection during an owned manual request/);
  h.requests[0].resolve(h.reply(1,true));await settle();
});
