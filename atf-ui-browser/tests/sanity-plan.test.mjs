import assert from 'node:assert/strict';
import test from 'node:test';
import {tableSanityPlan,admitSanityContract,executeSanityPlan,verifySanityFrames} from './sanity-plan.mjs';

test('both adapters share ordered roles and stop at the original driver failure',async()=>{
 const roles=[];await executeSanityPlan(tableSanityPlan,role=>roles.push(role));
 assert.deepEqual(roles,tableSanityPlan.steps);console.log(roles.join('|'));
 const failure=new Error('original driver failure'),prefix=[];
 await assert.rejects(executeSanityPlan(tableSanityPlan,role=>{prefix.push(role);throw failure;}),e=>e===failure);
 assert.deepEqual(prefix,['bidding']);
});
test('semantic observations do not consume images; failure remains terminal',()=>{
 const frames=tableSanityPlan.steps.map(id=>({id,capture:tableSanityPlan.captureSteps.includes(id)?id+'.png':null}));
 assert.equal(verifySanityFrames(tableSanityPlan,frames).normalCaptures,4);
 assert.equal(verifySanityFrames(tableSanityPlan,[frames[0],{id:'notice',capture:null},{id:'failure',capture:'failed.png'}],false).failureCaptures,1);
 for(const rows of [frames.slice(1),frames.slice(0,2),[...frames,{id:'notice',capture:'extra.png'}],
   [frames[0],{id:'failure',capture:'failed.png'},{id:'failure',capture:'second.png'}]])
   assert.throws(()=>verifySanityFrames(tableSanityPlan,rows),/E2E_/);
});
test('admission rejects expanded populations and undeclared image roles',()=>{
 for(const delta of [{deals:[1,1,1]},{deals:[1,1.5]},{maximumNormalCaptures:5},{optionalSteps:['failure']},
   {captureSteps:['unknown']},{steps:['bid','bid']},{unknown:true}])
   assert.throws(()=>admitSanityContract({...tableSanityPlan,...delta}),/E2E_/);
});
