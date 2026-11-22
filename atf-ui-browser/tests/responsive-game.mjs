import assert from 'node:assert/strict';
import fs from 'node:fs/promises';
import path from 'node:path';
import {fileURLToPath} from 'node:url';
import {withServer,withBrowser,until} from './fixtures.mjs';

// Explicit rendered acceptance against the real JVM engine, separate from controlled component fixtures.
const [classpath,output,executable='C:/Program Files (x86)/Microsoft/Edge/Application/msedge.exe']=process.argv.slice(2);
if(!classpath||!output)throw Error('Expected Whist JVM classpath, new external evidence directory and optional Chromium executable');
await fs.mkdir(output,{recursive:true});
const web=fileURLToPath(new URL('../src/main/web/',import.meta.url)),checks=[];
const footer=fileURLToPath(new URL('../../whist-lab-app/app/src/main/res/drawable-nodpi/teo_dedication.png',import.meta.url));
await withServer({classes:classpath,main:'ro.whistlab.browser.WhistBrowser',arguments:[path.join(output,'device-profile'),footer]},web,async url=>{
  for(const file of ['table.mjs','viewport.mjs','seat-presentation.mjs'])assert.equal((await fetch(new URL(file,url))).status,200,'Production static module: '+file);
  await withBrowser(executable,path.join(output,'browser-profile'),async({send,evaluate,version})=>{
    await send('Page.enable');
    await send('Emulation.setDeviceMetricsOverride',{width:414,height:896,deviceScaleFactor:1,mobile:true});
    await send('Page.navigate',{url:url.href});
    try { await until(()=>evaluate('!!document.querySelector(".whist-home button.primary")')); }
    catch(failure) { console.error('Browser startup observation: '+await evaluate('document.body.innerText')); throw failure; }
    await evaluate("document.querySelector('.whist-home button.primary').click();document.querySelector('select[name=mode]').value='ROMANIAN_WHIST';document.querySelector('select[name=mode]').dispatchEvent(new Event('change'));document.querySelector('.setup form').requestSubmit()");
    await until(()=>evaluate('!!document.querySelector(".board[data-layout=ready] .seat")'));
    const cards=await evaluate("[...document.querySelectorAll('.fan .card')].map(n=>n.dataset.face)");
    for(const profile of [{width:320,height:568,mobile:true},{width:414,height:896,mobile:true},{width:1280,height:720,mobile:false}])
      for(const language of ['ro','en'])for(const font of [16,20.8]){
        await send('Emulation.setDeviceMetricsOverride',{...profile,deviceScaleFactor:1});
        await send('Emulation.setUserAgentOverride',{userAgent:profile.mobile?'Mozilla/5.0 (Linux; Android 14; Mobile)':'Mozilla/5.0 (Windows NT 10.0; Win64; x64)'});
        await evaluate(`document.documentElement.style.fontSize='${font}px';document.querySelector('.whist-navigation summary').click();document.querySelector('[data-menu-action=home]').click()`);
        await until(()=>evaluate('!!document.querySelector(".whist-home button.primary")'));
        await evaluate(`document.querySelector('.whist-language select').value='${language}';document.querySelector('.whist-language select').dispatchEvent(new Event('change'))`);
        await until(()=>evaluate(`document.documentElement.lang==='${language}'&&document.querySelector('#table').getAttribute('aria-busy')==='false'`));
        await evaluate("[...document.querySelectorAll('.whist-home button')].find(n=>/Reia partida|Resume game/.test(n.textContent)).click()");
        await until(()=>evaluate('!!document.querySelector(".board[data-layout=ready] .seat")'));
        const observed=await evaluate(`(()=>{
          const box=n=>{const r=n.getBoundingClientRect();return {left:r.left,top:r.top,width:r.width,height:r.height,right:r.right,bottom:r.bottom};};
          const stage=document.querySelector('#table'),board=document.querySelector('.board'),hand=document.querySelector('.hand');
          return {frame:box(document.querySelector('#frame')),board:box(board),hand:box(hand),transform:getComputedStyle(stage).transform,
            cards:[...hand.querySelectorAll('.card')].map(n=>({face:n.dataset.face,...box(n)})),
            seats:[...board.querySelectorAll('.seat')].map(n=>({text:n.textContent,...box(n)})),
            header:box(document.querySelector('.whist-header')),trump:box(document.querySelector('.whist-trump')),extraWidth:stage.scrollWidth-stage.clientWidth};
        })()`);
        assert.deepEqual(observed.cards.map(n=>n.face),cards,'The same saved hand must survive language, text and viewport changes');
        assert.equal(observed.transform,'none');assert.ok(observed.extraWidth<=1);
        assert.ok(Math.abs(observed.frame.width-Math.min(profile.width,768))<1);
        assert.ok(observed.seats.every(n=>n.width>0&&n.height>0&&n.left>=observed.board.left-1&&n.right<=observed.board.right+1),'Seats remain inside the actual board');
        assert.ok(observed.cards.every(n=>n.left>=observed.hand.left-1&&n.right<=observed.hand.right+1),'Every dealt card remains horizontally reachable');
        const id=`GAME-${profile.width}-${language}-${font}`,image=id+'.png';
        await evaluate("document.querySelector('#table').scrollTop=0");
        const png=await send('Page.captureScreenshot',{format:'png'});await fs.writeFile(path.join(output,image),Buffer.from(png.data,'base64'));
        checks.push({id,name:`Real Whist hand ${language} ${profile.width}×${profile.height} text ${font/16}`,status:'PASS',
          expected:{sameCards:cards,transform:'none',frameWidth:Math.min(profile.width,768),seatsWithinBoard:true},
          observed:{version,profile,language,font,...observed},images:[{file:image,title:id}]});
      }
  });
});
await fs.writeFile(path.join(output,'browser-ui-checks.json'),JSON.stringify(checks,null,2));
console.log(JSON.stringify({status:'PASS',cases:checks.length,source:'real JVM Whist engine; no native APK comparison'}));
