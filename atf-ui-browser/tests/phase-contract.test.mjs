import assert from 'node:assert/strict';
import test from 'node:test';
import {portableContract} from './portable-contract.mjs';

test('all legal Whist deals and every seat use one native/Web phase contract',t=>{
 const fixture=portableContract(t,'ro.cardtable.browser.TablePhaseAdapters',{testSources:true});
 const observed=JSON.parse(fixture.run());
 assert.equal(observed.status,'PASS');assert.equal(observed.observed.modes.length,6);
 assert.ok(observed.observed.configurations>10000);assert.ok(observed.observed.seatComparisons>100000);
 console.log('Shared table phase observations: '+JSON.stringify(observed));
});

test('shifted or missing seats are rejected by the same native/Web oracle',t=>{
 const fixture=portableContract(t,'ro.cardtable.browser.TablePhaseAdapters',{testSources:true});
 const negative=JSON.parse(fixture.run('negative'));assert.equal(negative.status,'PASS');assert.equal(negative.rejected,76);
 console.log('Shared table phase negative controls: '+JSON.stringify(negative));
});
