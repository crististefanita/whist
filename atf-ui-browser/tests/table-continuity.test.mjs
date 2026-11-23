import assert from 'node:assert/strict';
import test from 'node:test';
import {tableContinuity} from './table-continuity.mjs';
const rectangle=[10,20,50,70],card=(face,x=10)=>({face,lifting:false,rectangle:[x,20,50,70]});
const sample=()=>({hand:'Hand 1',regions:{header:rectangle,'.board':rectangle,'.hand':rectangle},seats:[{rectangle}],handFaces:[card('A♠'),card('K♥',40)],played:[]});

test('an accepted card removal may recenter the remaining fan only at its deposited-face boundary',()=>{
 const a=sample(),b=sample(),c=sample(),d=sample();
 b.handFaces=[card('K♥',40)];c.handFaces=[card('K♥',25)];c.played=[{key:'0/A♠',rectangle}];d.handFaces=[card('K♥',25)];
 assert.deepEqual(tableContinuity([a,b,c,d]),{maxFrameDelta:0,maxPlacedDelta:0,maxHandDelta:0,acceptedRemovals:1});
 console.log('Table continuity oracle observations: explicit removal boundary permits recentering while AI drift remains measurable');
});
test('ordinary AI changes cannot hide one unit of hand, frame or deposited-card drift',()=>{
 for(const role of ['hand','frame','played']){
  const a=sample(),b=sample();a.played=[{key:'1/Q♦',rectangle}];b.played=[{key:'1/Q♦',rectangle:[...rectangle]}];
  if(role==='hand')b.handFaces[0].rectangle[0]++;if(role==='frame')b.seats=[{rectangle:[11,20,50,70]}];if(role==='played')b.played[0].rectangle[0]++;
  const result=tableContinuity([a,b]);assert.equal(result[role==='hand'?'maxHandDelta':role==='frame'?'maxFrameDelta':'maxPlacedDelta'],1);
 }
});
test('empty, malformed and unconfirmed-removal traces cannot pass silently',()=>{
 assert.throws(()=>tableContinuity([]),TypeError);const a=sample(),b=sample();b.handFaces=[card('K♥',25)];
 assert.throws(()=>tableContinuity([a,b]),/lacks its deposited/);b.handFaces=sample().handFaces;b.seats=[{rectangle:[null,20,50,70]}];assert.throws(()=>tableContinuity([a,b]),/finite rectangle/);
});
