import assert from 'node:assert/strict';
import fs from 'node:fs/promises';
import path from 'node:path';
import {fileURLToPath} from 'node:url';
import {withServer,withBrowser,until} from './fixtures.mjs';

// Real JVM engine and rendered DOM; portable/native comparisons remain separate component evidence.
const [classpath,output,executable='C:/Program Files (x86)/Microsoft/Edge/Application/msedge.exe']=process.argv.slice(2);
if(!classpath||!output)throw Error('Expected JVM classpath, new external evidence directory and optional Chromium executable');
await fs.mkdir(output,{recursive:true});
const web=fileURLToPath(new URL('../src/main/web/',import.meta.url)),checks=[];
const footer=fileURLToPath(new URL('../../whist-lab-app/app/src/main/res/drawable-nodpi/teo_dedication.png',import.meta.url));
const scenarios=[['ROMANIAN_WHIST',4],['OH_HELL',3],['REVERSE_OH_HELL',3],['DESCENDING',3],['DOWN_AND_UP',5],['LONG_ROMANIAN',6]];
const profiles=[{width:320,height:568,mobile:true},{width:414,height:896,mobile:true},{width:1280,height:720,mobile:false},{width:393,height:626,mobile:true}];
const overlaps=(a,b)=>a.left<b.right-.5&&a.right>b.left+.5&&a.top<b.bottom-.5&&a.bottom>b.top+.5;
function validate(o,profile,cards){
  assert.deepEqual(o.cards.map(n=>n.face),cards,'The identical hand survives language, text and viewport changes');
  assert.ok(o.extraWidth<=1&&o.stageOverflow<=1,'The logical scene fits without hidden vertical content');
  assert.ok(o.pageHeight<=profile.height+1,'No vertical page scrolling');
  assert.ok(o.frame.width<=profile.width+1&&Math.abs(o.frame.height-profile.height)<1,'The fitted frame uses the available height');
  for(const badge of o.actor){
    assert.ok(badge.inkOverflow<=1,'The complete localized action label fits its frame width');
    assert.ok(badge.top>=o.board.top-1,'Action label remains inside its native-style reserved band');
    assert.ok(!o.controls.some(control=>overlaps(badge,control)),'Action label does not intersect bidding controls');
    assert.ok(!o.seats.some(seat=>overlaps(badge,seat)),'Action label does not intersect player frames');
  }
  for(const region of [o.header,o.trump,o.hand,...o.seats,...o.cards,...o.controls,...o.actor])
    assert.ok(region.top>=o.frame.top-1&&region.bottom<=o.frame.bottom+1,'Every visible table role fits the height budget');
  assert.ok(o.seats.every(n=>n.width>0&&n.left>=o.board.left-1&&n.right<=o.board.right+1),'Seats remain inside the measured board');
  for(const seat of o.typography.seats){
    assert.ok(Math.abs(seat.name-o.typography.root*13/16)<.02&&Math.abs(seat.fact-o.typography.root*11/16)<.02,'Seat text uses the native logical roles before scene zoom');
    assert.match(seat.family,/WhistRoboto/,'Seat text uses the bundled Android reference font');
  }
  assert.ok(o.cards.every(n=>n.left>=o.fan.left-1&&n.right<=o.fan.right+1),'Every dealt card remains inside the native-style fan');
  assert.ok(o.handReach.first&&o.handReach.last,'The first and last card remain reachable through the native horizontal hand gesture');
}
await withServer({classes:classpath,main:'ro.whistlab.browser.WhistBrowser',arguments:[path.join(output,'device-profile'),footer]},web,async url=>{
  for(const file of ['table.mjs','viewport.mjs','seat-presentation.mjs','surface-fit.mjs'])assert.equal((await fetch(new URL(file,url))).status,200,'Production module '+file);
  await withBrowser(executable,path.join(output,'browser-profile'),async({send,evaluate,version})=>{
    const inspect=()=>evaluate(`(()=>{
      const box=n=>{const r=n.getBoundingClientRect();return {left:r.left,top:r.top,width:r.width,height:r.height,right:r.right,bottom:r.bottom};};
      const stage=document.querySelector('#table'),board=document.querySelector('.board'),hand=document.querySelector('.hand');
      return {logicalHeight:stage.clientHeight,stageOverflow:stage.scrollHeight-stage.clientHeight,pageHeight:document.documentElement.scrollHeight,
        typography:{root:parseFloat(getComputedStyle(document.documentElement).fontSize),seats:[...board.querySelectorAll('.seat')].map(n=>({name:parseFloat(getComputedStyle(n.querySelector('strong')).fontSize),fact:parseFloat(getComputedStyle(n.querySelector('.seat-bid')).fontSize),family:getComputedStyle(n).fontFamily}))},
        actor:[...board.querySelectorAll('.seat-turn:not([hidden])')].map(n=>({text:n.textContent,inkOverflow:n.scrollWidth-n.clientWidth,...box(n)})),
        controls:[...board.querySelectorAll('[data-bid],.bid-submit')].filter(n=>n.getClientRects().length).map(box),
        frame:box(document.querySelector('#frame')),board:box(board),hand:box(hand),fan:box(hand.querySelector('.fan')),transform:getComputedStyle(stage).transform,
        cards:[...hand.querySelectorAll('.card')].map(n=>({face:n.dataset.face,...box(n)})),
        seats:[...board.querySelectorAll('.seat')].map(n=>({text:n.textContent,...box(n)})),
        header:box(document.querySelector('.whist-header')),trump:box(document.querySelector('.whist-trump')),extraWidth:stage.scrollWidth-stage.clientWidth};
    })()`);
    const home=async()=>{
      await evaluate("document.querySelector('.whist-navigation summary')?.click();document.querySelector('[data-menu-action=home]')?.click()");
      await until(()=>evaluate('!!document.querySelector(".whist-home button.primary")'));
    };
    async function record(id,profile,language,font,cards,phase,capture){
      await new Promise(resolve=>setTimeout(resolve,80));
      const observed=await inspect();const images=[];
      observed.handReach=await evaluate(`(()=>{
        const hand=document.querySelector('.hand'),cards=[...hand.querySelectorAll('.card')],before=hand.scrollLeft;
        const inside=node=>{const r=node.getBoundingClientRect(),h=hand.getBoundingClientRect();return r.left>=h.left-1&&r.right<=h.right+1;};
        hand.scrollLeft=0;const first=inside(cards[0]);hand.scrollLeft=hand.scrollWidth;const last=inside(cards.at(-1));
        hand.scrollLeft=before;return {first,last,range:hand.scrollWidth-hand.clientWidth};
      })()`);
      try { validate(observed,profile,cards); }
      catch(failure){
        const file=id+'-FAIL.png',png=await send('Page.captureScreenshot',{format:'png'});
        await fs.writeFile(path.join(output,file),Buffer.from(png.data,'base64'));
        checks.push({id,name:`${phase} ${id}`,status:'FAIL',expected:{sameCards:cards,noVerticalScroll:true},observed:{...observed,failure:failure.message},images:[{file,title:id}]});
        await fs.writeFile(path.join(output,'browser-ui-checks.json'),JSON.stringify(checks,null,2));
        throw new Error(id+': '+failure.message,{cause:failure});
      }
      if(capture){const file=id+'.png',png=await send('Page.captureScreenshot',{format:'png'});await fs.writeFile(path.join(output,file),Buffer.from(png.data,'base64'));images.push({file,title:id});}
      checks.push({id,name:`${phase} ${id}`,status:'PASS',expected:{sameCards:cards,noVerticalScroll:true,actionBandClear:true,proportionalFit:true},observed:{version,profile,language,font,...observed},images});
      await fs.writeFile(path.join(output,'browser-ui-checks.json'),JSON.stringify(checks,null,2));
      return observed;
    }
    await send('Page.enable');await send('Emulation.setDeviceMetricsOverride',{width:414,height:896,deviceScaleFactor:1,mobile:true});
    await send('Page.navigate',{url:url.href});await home();
    for(const [mode,players] of scenarios){
      await home();
      await evaluate(`document.querySelector('.whist-home button.primary').click();document.querySelector('select[name=players]').value='${players}';document.querySelector('select[name=players]').dispatchEvent(new Event('change'));document.querySelector('select[name=mode]').value='${mode}';document.querySelector('select[name=mode]').dispatchEvent(new Event('change'));document.querySelector('.setup form').requestSubmit()`);
      await until(()=>evaluate('!!document.querySelector(".board[data-layout=ready] .seat")'));
      const cards=await evaluate("[...document.querySelectorAll('.fan .card')].map(n=>n.dataset.face)");
      if(mode==='DESCENDING')assert.equal(cards.length,17,'Three-player descending exercises the maximum hand');
      let last;
      for(const profile of profiles)for(const language of ['ro','en'])for(const font of [16,20.8]){
        await send('Emulation.setDeviceMetricsOverride',{...profile,deviceScaleFactor:1});
        await send('Emulation.setUserAgentOverride',{userAgent:profile.mobile?'Mozilla/5.0 (Linux; Android 14; Mobile)':'Mozilla/5.0 (Windows NT 10.0; Win64; x64)'});
        await evaluate(`document.documentElement.style.fontSize='${font}px'`);await home();
        await evaluate(`document.querySelector('.whist-language select').value='${language}';document.querySelector('.whist-language select').dispatchEvent(new Event('change'))`);
        await until(()=>evaluate(`document.documentElement.lang==='${language}'&&document.querySelector('#table').getAttribute('aria-busy')==='false'`));
        await evaluate("[...document.querySelectorAll('.whist-home button')].find(n=>/Reia partida|Resume game/.test(n.textContent)).click()");
        await until(()=>evaluate('!!document.querySelector(".board[data-layout=ready] .seat")'));
        last=await record(`GAME-${mode}-${players}-${profile.width}-${language}-${font}`,profile,language,font,cards,'Bidding',profile.width===414&&font===20.8);
      }
      await evaluate("document.querySelector('.bid-submit').click()");
      await until(()=>evaluate('!document.querySelector(".bid-submit")&&document.querySelector("#table").getAttribute("aria-busy")==="false"'),30000);
      const playing=await record(`PLAY-${mode}-${players}`,profiles.at(-1),'en',20.8,cards,'Playing',true);
      assert.equal(playing.actor.length,1,'The active player has one visible play label');
      assert.deepEqual(playing.seats.map(({left,top,width,height})=>({left,top,width,height})),last.seats.map(({left,top,width,height})=>({left,top,width,height})),'Seat frames do not move between bidding and play');

      await send('Emulation.setDeviceMetricsOverride',{...profiles[0],deviceScaleFactor:1});await home();
      await send('Emulation.setUserAgentOverride',{userAgent:'Mozilla/5.0 (Linux; Android 14; Mobile)'});
      await evaluate("document.querySelector('.whist-language select').value='ro';document.querySelector('.whist-language select').dispatchEvent(new Event('change'))");
      await until(()=>evaluate("document.documentElement.lang==='ro'&&document.querySelector('#table').getAttribute('aria-busy')==='false'"));
      await evaluate("[...document.querySelectorAll('.whist-home button')].find(n=>/Reia partida/.test(n.textContent)).click()");
      await until(()=>evaluate('!!document.querySelector(".board[data-layout=ready] .seat")'));
      await record('PLAY-RO-COMPACT-'+mode+'-'+players,profiles[0],'ro',20.8,cards,'Playing',mode==='OH_HELL');
      if(cards.length===1){
        await evaluate("document.querySelector('.fan .card:not([disabled])').click()");
        await until(()=>evaluate('!!document.querySelector(".whist-score")&&document.querySelector("#table").getAttribute("aria-busy")==="false"'),30000);
        async function panel(id,selector,history=false){
          await until(()=>evaluate('!!document.querySelector('+JSON.stringify(selector)+')'));
          await new Promise(resolve=>setTimeout(resolve,100));
          const observed=await evaluate(`(()=>{
            const p=document.querySelector(${JSON.stringify(selector)}),r=p.getBoundingClientRect();
            return {top:r.top,bottom:r.bottom,height:r.height,scroll:p.scrollHeight-p.clientHeight,
              pageHeight:document.documentElement.scrollHeight,historyRange:document.querySelector('#history-data').scrollHeight-document.querySelector('#history-data').clientHeight,
              controls:[...p.querySelectorAll('button')].filter(n=>n.getClientRects().length).map(n=>{const b=n.getBoundingClientRect();return {text:n.textContent,top:b.top,bottom:b.bottom};})};
          })()`);
          assert.ok(observed.top>=-1&&observed.bottom<=profiles[0].height+1,'The complete panel fits the usable height');
          assert.ok(observed.pageHeight<=profiles[0].height+1&&observed.scroll<=1,'Panels do not require vertical page scrolling');
          assert.ok(observed.controls.every(n=>n.top>=-1&&n.bottom<=profiles[0].height+1),'Every panel command is visible');
          if(history)assert.ok(observed.historyRange>0,'Only score history retains internal vertical scrolling');
          const images=[];
          if(mode==='ROMANIAN_WHIST'){const file=id+'.png',png=await send('Page.captureScreenshot',{format:'png'});await fs.writeFile(path.join(output,file),Buffer.from(png.data,'base64'));images.push({file,title:id});}
          checks.push({id,name:id,status:'PASS',expected:{noPageScroll:true,historyException:history},observed,images});
          await fs.writeFile(path.join(output,'browser-ui-checks.json'),JSON.stringify(checks,null,2));
        }
        await panel('SCORE-'+mode,'.whist-score');
        await evaluate("[...document.querySelectorAll('.whist-score button')].find(n=>/Calculul punctajului/.test(n.textContent)).click()");
        await panel('SCORE-RULES-'+mode,'.game-score-rules[open]');
        await evaluate("document.querySelector('.player-dialog[open]').close();[...document.querySelectorAll('.whist-score button')].find(n=>/Istoric/.test(n.textContent)).click()");
        await panel('HISTORY-'+mode,'#history[open]',true);
        await evaluate("document.querySelector('#history').close();[...document.querySelectorAll('.whist-score button')].find(n=>/Meniu principal/.test(n.textContent)).click()");
      }
    }
  });
});
await fs.writeFile(path.join(output,'browser-ui-checks.json'),JSON.stringify(checks,null,2));
console.log(JSON.stringify({status:'PASS',cases:checks.length,source:'six real JVM Whist variants; no native APK rendering claim'}));
