import test from 'node:test';
import assert from 'node:assert/strict';
import {tableCheckpoints} from './table-checkpoints.mjs';
const rect=[10,20,50,70];
const state=()=>({hand:'Hand 1',at:100,actor:'1',collecting:false,regions:{header:rect,'.board':rect,'.hand':rect},
 seats:[{seat:'0',rectangle:[...rect],facts:[{text:'Bid: 1',rectangle:[...rect]}]}],handFaces:[{face:'A♠',rectangle:[...rect],lifting:false}],played:[]});
test('each observed deposit retains its original before/after, per-field assertions and observation ownership',()=>{
 const a=state(),b=state();b.at=137;b.played=[{key:'1/K♥',rectangle:rect}];
 const steps=tableCheckpoints([a,b],'https://game.invalid/');assert.equal(steps.length,2);
 const step=steps[1];assert.equal(step.source,'OBSERVE');assert.equal(step.before,a);assert.equal(step.after,b);assert.equal(step.durationMillis,37);
 assert.ok(step.assertions.length>=7);assert.ok(step.assertions.every(n=>n.passed));assert.equal(step.status,'PASS');
 console.log('Table checkpoint observations: individual before/after deposits, field assertions and observed-vs-driver ownership');
});
test('one-unit frame, fact, deposited-face or unchanged-hand drift becomes an explicit failed assertion',()=>{
 for(const role of ['frame','fact','placed','hand']){
  const a=state(),b=structuredClone(a);a.played=[{key:'2/2♣',rectangle:rect}];b.played=[...structuredClone(a.played),{key:'1/K♥',rectangle:rect}];
  if(role==='frame')b.seats[0].rectangle[0]++;if(role==='fact')b.seats[0].facts[0].rectangle[0]++;
  if(role==='placed')b.played[0].rectangle[0]++;if(role==='hand')b.handFaces[0].rectangle[0]++;
  assert.equal(tableCheckpoints([a,b],'https://game.invalid/')[1].status,'FAIL',role);
 }
});
test('manual deposits keep real command snapshots, while their permitted fan recenter does not move frames',()=>{
 const a=state(),b=state();a.handFaces.push({face:'K♣',rectangle:[40,20,50,70]});b.handFaces=[{face:'K♣',rectangle:[25,20,50,70]}];b.played=[{key:'0/A♠',rectangle:rect}];
 const manual={hand:a.hand,key:'0/A♠',before:a,after:b,images:[{file:'before.png',title:'Before'}]};
 const step=tableCheckpoints([a,b],'https://game.invalid/',[manual])[1];assert.equal(step.source,'ACTION');assert.equal(step.status,'PASS');assert.deepEqual(step.images,manual.images);
});
test('a transient pre-deposit removal is paired with its original full hand, including simultaneous collection',()=>{
 const a=state(),b=state(),c=state();a.handFaces.push({face:'K♣',rectangle:[40,20,50,70]});
 b.handFaces=[{face:'K♣',rectangle:[40,20,50,70]}];c.handFaces=[{face:'K♣',rectangle:[25,20,50,70]}];
 c.played=[{key:'0/A♠',rectangle:rect}];c.collecting=true;
 const steps=tableCheckpoints([a,b,c],'https://game.invalid/');
 assert.ok(steps.every(n=>n.status==='PASS'));assert.equal(steps[1].before,a);assert.equal(steps[2].before,a);
 assert.equal(steps[1].assertions.at(-1).observed,1);
});
test('collection boundaries remain ordered, and missing deposits or invalid measurements cannot qualify',()=>{
 const a=state(),b=state(),c=state();b.collecting=true;c.at=140;
 assert.deepEqual(tableCheckpoints([a,b,c],'https://game.invalid/').map(n=>n.name.split(' · ')[1]),['Bidding table is ready','Collection starts','Collection completes']);
 b.played=[{key:'1/K♥',rectangle:rect},{key:'2/Q♦',rectangle:rect}];assert.throws(()=>tableCheckpoints([a,b],'https://game.invalid/'),/missed an individual/);
 b.played=[];b.seats[0].rectangle=[NaN,20,50,70];assert.throws(()=>tableCheckpoints([b],'https://game.invalid/'),/finite rectangle/);
});
