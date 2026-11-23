import assert from 'node:assert/strict';
import fs from 'node:fs';
import path from 'node:path';
import test from 'node:test';
import {fileURLToPath} from 'node:url';

test('the entrypoint and imported UI modules share a cache revision',()=>{
 const web=process.env.ATF_BROWSER_WEB_ROOT??fileURLToPath(new URL('../src/main/web/',import.meta.url));
 const html=fs.readFileSync(path.join(web,'index.html'),'utf8'),table=fs.readFileSync(path.join(web,'table.mjs'),'utf8');
 const revision=/table\.mjs\?v=([0-9.]+)/.exec(html)?.[1];assert.ok(revision);
 assert.ok(html.includes('table.css?v='+revision));
 for(const file of ['profile.mjs','viewport.mjs','surface-fit.mjs','seat-presentation.mjs','hand-feedback.mjs'])assert.ok(table.includes('./'+file+'?v='+revision),file);
 assert.ok(fs.readFileSync(path.join(web,'surface-fit.mjs'),'utf8').includes('./viewport.mjs?v='+revision));
 console.log('Asset revision observations: shell, fitted surface and delegated hand use one source revision');
});
