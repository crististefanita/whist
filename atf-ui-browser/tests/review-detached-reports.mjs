import {readFile,writeFile,mkdir,copyFile} from 'node:fs/promises';
import {resolve,join} from 'node:path';
import {pathToFileURL} from 'node:url';
import {execFileSync} from 'node:child_process';
import {createHash} from 'node:crypto';
import {withBrowser,until} from './fixtures.mjs';
import {admitReview,qualifyReview} from '../../atf-tests/tools/report-autonomy.mjs';

// Producer-specific tests supply the original seals; this reader never republishes or repairs their HTML.
const [manifestPath,output,browser,java,classpath]=process.argv.slice(2);
if(!classpath)throw new Error('Use <manifest> <new-output> <browser> <java> <qualified-classpath>');
const manifest=JSON.parse(await readFile(manifestPath,'utf8'));
const catalog=JSON.parse(await readFile(new URL('../../config/report-producers.json',import.meta.url),'utf8'));
const reports=admitReview(catalog,manifest);
await mkdir(output);await mkdir(join(output,'detached'));
const originals=[];
for(let i=0;i<reports.length;i++) {
 const report=reports[i],file=join(output,'detached',i+'.html');
 await copyFile(resolve(report.file),file);
 const original=JSON.parse(execFileSync(java,['-Dstdout.encoding=UTF-8','-Dstderr.encoding=UTF-8','-cp',classpath,'atf.tests.reporting.autonomy.StandaloneReportContract',file,report.sha256],{encoding:'utf8',windowsHide:true}));
 originals.push({...original,id:String(i+1),producer:report.producer,file});
}
const checks=[];
await withBrowser(browser,join(output,'fresh-profile'),async({send,evaluate,version})=>{
 await send('Page.enable');await send('Network.enable');
 await send('Network.setBlockedURLs',{urls:['http://*','https://*']});
 await send('Network.emulateNetworkConditions',{offline:true,latency:0,downloadThroughput:0,uploadThroughput:0});
 await send('Emulation.setDeviceMetricsOverride',{width:1664,height:916,mobile:false,deviceScaleFactor:1});
 for(const original of originals) {
  await send('Page.navigate',{url:pathToFileURL(original.file).href});
  await until(()=>evaluate('document.readyState==="complete"&&!!document.querySelector("#dashboard-view")'),20000);
  const overview=await evaluate(`(()=>{document.querySelector('.side-nav a[href="#dashboard-view"]').click();const v=document.querySelector('#dashboard-view');return v.getBoundingClientRect().width>0&&!!v.querySelector('.row');})()`);
  await evaluate(`document.querySelector('.side-nav a[href="#test-view"]').click()`);
  await evaluate(`document.querySelector('li.test-item[test-id="${original.ownerId}"] .test-detail').click()`);
  await evaluate(`(()=>{const p=[...document.querySelectorAll('.test-content-detail')].find(n=>n.getBoundingClientRect().width);p.querySelectorAll('.card-header').forEach(n=>n.click());p.querySelectorAll('details:not([open])>summary').forEach(n=>n.click());})()`);
  await until(()=>evaluate(`(()=>{const p=[...document.querySelectorAll('.test-content-detail')].find(n=>n.getBoundingClientRect().width);return [...p.querySelectorAll('table.helm-validation-table tbody tr')].some(n=>n.children[0]?.textContent.replace(/\\s+/g,' ').trim()===${JSON.stringify(original.lastCaption)}&&n.getBoundingClientRect().height>0);})()`));
  const observed=await evaluate(`(()=>{const p=[...document.querySelectorAll('.test-content-detail')].find(n=>n.getBoundingClientRect().width);const rows=[...p.querySelectorAll('table.helm-validation-table tbody tr')];const row=rows.findLast(n=>n.children[0]?.textContent.replace(/\\s+/g,' ').trim()===${JSON.stringify(original.lastCaption)});row?.scrollIntoView({block:'center'});return {owner:p.textContent.includes(${JSON.stringify(original.ownerName)}),lastRow:!!row&&row.getBoundingClientRect().height>0,fullBody:[...p.querySelectorAll('pre')].some(n=>n.textContent.includes(${JSON.stringify(original.bodyTail)})&&n.getBoundingClientRect().height>0)};})()`);
  let image=null,returned=true;
  if(original.inlineReferences>0) {
   await evaluate(`(()=>{const p=[...document.querySelectorAll('.test-content-detail')].find(n=>n.getBoundingClientRect().width);const a=[...p.querySelectorAll('a[data-atf-screenshot-ref],a[data-atf-screenshot=base64]')].find(n=>n.getBoundingClientRect().width);if(!a)throw new Error('REPORT_VISIBLE_IMAGE_REQUIRED');a.scrollIntoView({block:'center'});a.click();})()`);
   image=await until(()=>evaluate(`[...document.querySelectorAll('img')].some(n=>n.getBoundingClientRect().width>0&&n.naturalWidth>0&&n.naturalHeight>0&&n.src.startsWith('data:image/png;base64,'))`));
   await send('Input.dispatchKeyEvent',{type:'keyDown',key:'Escape',code:'Escape',windowsVirtualKeyCode:27});
   await send('Input.dispatchKeyEvent',{type:'keyUp',key:'Escape',code:'Escape',windowsVirtualKeyCode:27});
   returned=await until(()=>evaluate(`![...document.querySelectorAll('img')].some(n=>n.getBoundingClientRect().width>0&&n.src.startsWith('data:image/png;base64,'))&&[...document.querySelectorAll('.test-content-detail')].some(n=>n.getBoundingClientRect().width&&n.textContent.includes(${JSON.stringify(original.ownerName)}))`));
  }
  const sha256=createHash('sha256').update(await readFile(original.file)).digest('hex');
  const result={...observed,overview,image,returned,sha256,networkBlocked:true,browser:version,producer:original.producer};
  await writeFile(join(output,original.id+'-observations.json'),JSON.stringify({original,result},null,2));
  checks.push(qualifyReview(original,result));
  await writeFile(join(output,'browser-checks.json'),JSON.stringify(checks,null,2));
  console.log('PASS detached producer: '+original.producer);
 }
});
