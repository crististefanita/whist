import assert from 'node:assert/strict';
import test from 'node:test';
import path from 'node:path';
import {fileURLToPath,pathToFileURL} from 'node:url';
const web=process.env.ATF_BROWSER_WEB_ROOT??fileURLToPath(new URL('../src/main/web/',import.meta.url));
const {intrinsicTableHeight}=await import(pathToFileURL(path.join(web,'viewport.mjs')));

test('intrinsic scene height excludes previously allocated board slack and absolute overflow',()=>{
 for(const allocated of [300,500,1000,2000])assert.equal(intrinsicTableHeight(allocated+200,allocated,300),500);
 assert.equal(intrinsicTableHeight(637,403.65625,359),593);
 assert.equal(intrinsicTableHeight(1000,766.65625,359),593);
 console.log('Intrinsic table observations: native minimum plus non-board flow; allocated slack cannot enlarge the next hand');
});
test('invalid intrinsic dimensions are rejected before changing scene geometry',()=>{
 for(const bad of [0,-1,NaN,Infinity,'600',undefined]){
  assert.throws(()=>intrinsicTableHeight(bad,400,300),RangeError);assert.throws(()=>intrinsicTableHeight(600,bad,300),RangeError);assert.throws(()=>intrinsicTableHeight(600,400,bad),RangeError);
 }assert.throws(()=>intrinsicTableHeight(300,400,200),RangeError);
});
