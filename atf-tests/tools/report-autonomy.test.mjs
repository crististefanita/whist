import test from 'node:test';
import assert from 'node:assert/strict';
import {admitReview,qualifyReview} from './report-autonomy.mjs';
const original={id:'controlled',sha256:'a'.repeat(64),inlineReferences:1};
const observed={sha256:original.sha256,overview:true,owner:true,lastRow:true,fullBody:true,returned:true,networkBlocked:true,image:true};
test('detached review preserves its qualified identity and every observed comparison',()=>{
 assert.equal(qualifyReview(original,observed).status,'PASS');
 assert.deepEqual(qualifyReview(original,observed).observed,observed);
});
test('a missing interaction foreign hash or absent original image cannot qualify',()=>{
 for(const key of Object.keys(observed))assert.throws(()=>qualifyReview(original,{...observed,[key]:key==='sha256'?'b'.repeat(64):false}));
 assert.equal(qualifyReview({...original,inlineReferences:0},{...observed,image:null}).observed.image,null);
});
test('review cannot omit duplicate or invent a primary producer',()=>{
 const catalog={producers:[{source:'first',role:'primary'},{source:'second',role:'primary'},{source:'old',role:'retired'}]};
 const manifest={schemaVersion:1,reports:['first','second'].map(producer=>({producer,file:producer+'.html',sha256:original.sha256}))};
 assert.equal(admitReview(catalog,manifest).length,2);
 for(const reports of [manifest.reports.slice(1),[manifest.reports[0],manifest.reports[0]],[...manifest.reports,{...manifest.reports[0],producer:'old'}]])
   assert.throws(()=>admitReview(catalog,{...manifest,reports}),/REPORT_REVIEW_PRODUCERS/);
});
