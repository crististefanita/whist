import assert from 'node:assert/strict';
import test from 'node:test';
import {portableContract} from './portable-contract.mjs';
import {MENU_TYPOGRAPHY,surfaceTypography} from '../src/main/web/viewport.mjs';

test('shared Java menu roles preserve Android defaults and the browser compatibility baseline', t => {
  const fixture=portableContract(t,'ro.cardtable.contract.MenuTypographyContract');
  const roles=JSON.parse(fixture.run());
  assert.deepEqual(roles,MENU_TYPOGRAPHY);
  for(const [role,rem] of Object.entries(roles))assert.equal(surfaceTypography(roles)['--menu-'+role+'-size'],rem+'rem');
  assert.equal(roles.title*16,25); assert.equal(roles.selection*16,14); assert.equal(roles.action*16,16);
  console.log('Shared menu role observations: '+JSON.stringify({logicalPixels:Object.fromEntries(Object.entries(roles).map(([key,value])=>[key,value*16])),css:surfaceTypography(roles)}));
});
test('invalid and incomplete roles fail atomically before CSS is applied', () => {
  for(const roles of [{}, {...MENU_TYPOGRAPHY,action:NaN}, {...MENU_TYPOGRAPHY,selection:-1}])
    assert.throws(()=>surfaceTypography(roles),/Invalid shared menu typography/);
});
