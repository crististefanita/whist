import assert from 'node:assert/strict';
import fs from 'node:fs';
import path from 'node:path';
import {test} from 'node:test';
import {portableContract} from './portable-contract.mjs';
import {textScale} from '../src/main/web/viewport.mjs';
import {collectionFrame} from '../src/main/web/collection.mjs';

test('CSS text scale is logical and is independent of desktop device-pixel ratio', () => {
  for (const [text, expected] of [['16px',1],['24px',1.5],['32px',2],['8px',.5],['NaN',1],['0px',1],['Infinity',1]])
    assert.equal(textScale(text),expected,text);
});

test('production Web and native portable adapters share geometry, card marks and gesture transitions', t => {
  const {root,run}=portableContract(t,'ro.cardtable.browser.WhistSurfaceContract');
  assert.ok(fs.readFileSync(path.join(root,'atf-ui-browser/pages/src/main/java/ro/cardtable/browser/PageJson.java'),'utf8').includes('return BrowserLayoutJson.fields(layout);'),'Static pages delegate to the portable geometry projection');
  const text=run(path.join(root,'whist-lab-app/atf-card-table-ui/src/test/resources/hand-feedback.tsv'));
  const result=JSON.parse(text);assert.equal(result.status,'PASS');assert.equal(result.cases.length,4);
  assert.equal(result.cases[0].legacyFrames,24,'Legacy consumers do not inherit Whist native measurement semantics');
  assert.deepEqual(JSON.parse(result.cases[0].minimumProtocol.jvm),JSON.parse(result.cases[0].minimumProtocol.pages),'JVM and compiled-page protocols preserve the same native minimum');
  assert.equal(result.cases[1].faces,52);assert.equal(result.cases[2].commands,8);
  const curve=result.cases[3];assert.equal(curve.samples.length,11);
  for(let i=0;i<curve.samples.length;i++){
    const browser=collectionFrame(100,-60,curve.samples[i]),native=curve.frames[i];
    for(const [web,apk] of [['x','translationX'],['y','translationY'],['scale','scale'],['opacity','alpha']])
      assert.ok(Math.abs(browser[web]-native[apk])<.0001,JSON.stringify({i,web,apk,browser,native}));
  }
  console.log('Whist adapter observations: '+JSON.stringify(result));
});
