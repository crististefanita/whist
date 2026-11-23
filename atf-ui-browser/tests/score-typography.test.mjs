import assert from 'node:assert/strict';
import test from 'node:test';
import fs from 'node:fs/promises';
import path from 'node:path';
import {withBrowser,until} from './fixtures.mjs';
import {rendererFixture,scoreProjection} from './responsive-fixture.mjs';
import {MENU_TYPOGRAPHY,SCORE_TABLE_EMPHASIS} from '../src/main/web/viewport.mjs';

test('dropdown and score roles remain balanced without changing native score facts',async t=>{
  const {work,url,requests}=await rendererFixture(t),checks=[];
  const profiles=[{width:320,height:568},{width:414,height:896},{width:420,height:460},{width:1280,height:720},{width:1600,height:900}];
  const capture=async(send,id,enabled)=>{
    if(!process.env.ATF_COMPONENT_CAPTURE_DIR||!enabled)return [];
    await fs.mkdir(process.env.ATF_COMPONENT_CAPTURE_DIR,{recursive:true});
    const file=id+'.png',png=await send('Page.captureScreenshot',{format:'png'});
    await fs.writeFile(path.join(process.env.ATF_COMPONENT_CAPTURE_DIR,file),Buffer.from(png.data,'base64'));
    return [{file,title:id}];
  };
  await withBrowser(process.env.ATF_BROWSER_EXECUTABLE??'C:/Program Files (x86)/Microsoft/Edge/Application/msedge.exe',path.join(work,'profile'),async({send,evaluate,version})=>{
    await send('Page.enable');
    for(const profile of profiles)for(const language of ['ro','en'])for(const font of [16,20.8]){
      await send('Emulation.setDeviceMetricsOverride',{...profile,mobile:profile.width<500,deviceScaleFactor:1});
      await send('Emulation.setUserAgentOverride',{userAgent:profile.width<500?'Mozilla/5.0 (Linux; Android 9; Mobile)':'Mozilla/5.0 (Windows NT 10.0; Win64; x64)'});
      await send('Page.navigate',{url});
      await until(()=>evaluate('!!document.querySelector(".whist-home button.primary")')).catch(async failure=>{
        failure.message+=' | '+JSON.stringify({requests,document:await evaluate('document.body.innerText')});throw failure;
      });
      await evaluate('document.fonts.ready');
      assert.equal(await evaluate('document.fonts.check("16px WhistRoboto")'),true,'The native reference font is loaded locally');
      await evaluate(`document.documentElement.style.fontSize='${font}px';document.querySelector('.whist-language select').value='${language}';document.querySelector('.whist-language select').dispatchEvent(new Event('change'))`);
      await until(()=>evaluate(`document.documentElement.lang==='${language}'&&document.querySelector('#table').getAttribute('aria-busy')==='false'`));
      for(const players of [3,4,5,6]){
        await evaluate("document.querySelector('.whist-home button.primary').click()");
        await until(()=>evaluate("document.querySelector('.setup').getBoundingClientRect().bottom<=innerHeight+1"));
        if(players===3){
          const dropdowns=await evaluate(`(()=>{const size=n=>parseFloat(getComputedStyle(n).fontSize);return [...document.querySelectorAll('.setup select')].map(n=>({name:n.name,value:n.value,size:size(n),label:size(n.closest('label')),options:[...n.querySelectorAll('option,optgroup')].map(size),overflow:n.scrollWidth-n.clientWidth}));})()`);
          assert.ok(dropdowns.length>=5,'Language, players, variant, difficulty and series are exercised');
          for(const control of dropdowns){
            assert.ok(Math.abs(control.size-font*MENU_TYPOGRAPHY.selection)<.02,JSON.stringify(control));
            assert.ok(control.size>=control.label*.85&&control.size<=control.label*1.15,'Selection text stays in proportion to its label');
            assert.ok(control.options.every(n=>Math.abs(n-control.size)<.02),'Options inherit the selected-value size');
            assert.ok(control.overflow<=1,'Complete selection stays inside its control');
          }
          const id=`DROPDOWNS-${profile.width}-${profile.height}-${language}-${font}`;
          checks.push({id,name:id,status:'PASS',expected:{selectionRem:MENU_TYPOGRAPHY.selection,optionsInherit:true},observed:{version,profile,language,font,dropdowns},images:await capture(send,id,profile.width===414&&font===20.8)});
        }
        await evaluate(`document.querySelector('select[name=players]').value='${players}';document.querySelector('select[name=players]').dispatchEvent(new Event('change'));document.querySelector('.setup form').requestSubmit()`);
        await until(()=>evaluate('!!document.querySelector(".whist-score")&&document.querySelector("#table").getAttribute("aria-busy")==="false"'));
        await new Promise(resolve=>setTimeout(resolve,80));
        const observed=await evaluate(`(()=>{
          const stage=document.querySelector('#table'),page=document.querySelector('.whist-score'),size=n=>parseFloat(getComputedStyle(n).fontSize);
          const cells=[...page.querySelectorAll('tbody tr')].map(row=>[...row.cells].map(n=>n.textContent));
          const buttons=[...page.querySelectorAll('button')],physical=n=>{const r=n.getBoundingClientRect();return {top:r.top,bottom:r.bottom};};
          return {column:size(page.querySelector('th')),value:size(page.querySelector('td')),title:size(page.querySelector('h1')),action:size(buttons[0]),
            cells,headings:[...page.querySelectorAll('th')].map(n=>n.textContent),pageHeight:document.documentElement.scrollHeight,stageOverflow:stage.scrollHeight-stage.clientHeight,
            numeric:[...page.querySelectorAll('tbody td:not(:first-child)')].map(n=>{const range=document.createRange();range.selectNodeContents(n);const style=getComputedStyle(n);return {text:n.textContent,ink:range.getBoundingClientRect().width,available:n.clientWidth-parseFloat(style.paddingLeft)-parseFloat(style.paddingRight)};}),
            controls:buttons.map(n=>({label:n.textContent,...physical(n)}))};
        })()`);
        const expected=scoreProjection(players,language);
        assert.deepEqual(observed.cells,expected.score.table.rows,'Native-style hand delta and cumulative Total remain distinct');
        assert.deepEqual(observed.headings,expected.score.table.headings);
        assert.ok(Math.abs(observed.column-font*11/16*SCORE_TABLE_EMPHASIS)<.02);
        assert.ok(Math.abs(observed.value-font*13/16*SCORE_TABLE_EMPHASIS)<.02);
        assert.ok(Math.abs(observed.column/observed.value-11/13)<.01&&observed.value<=observed.title,'Table labels follow the action/title hierarchy');
        assert.ok(observed.pageHeight<=profile.height+1&&observed.stageOverflow<=1,'No vertical page or score scrolling');
        assert.ok(observed.controls.every(n=>n.top>=-1&&n.bottom<=profile.height+1),'Every score command fits the height budget');
        assert.ok(observed.numeric.every(n=>n.ink<=n.available+1),'Numeric values remain complete');
        const id=`SCORE-${players}-${profile.width}-${profile.height}-${language}-${font}`;
        checks.push({id,name:id,status:'PASS',expected:{rows:expected.score.table.rows,columnRem:11/16*SCORE_TABLE_EMPHASIS,valueRem:13/16*SCORE_TABLE_EMPHASIS,noVerticalScroll:true},observed:{version,profile,language,font,...observed},images:await capture(send,id,profile.width===414&&font===20.8&&[3,6].includes(players))});
        await evaluate("document.querySelector('.whist-score button.quiet').click()");
        await until(()=>evaluate('!!document.querySelector(".game-score-rules[open]")'));
        await evaluate('new Promise(resolve=>requestAnimationFrame(()=>requestAnimationFrame(resolve)))');
        const dialog=await evaluate(`(()=>{const p=document.querySelector('.game-score-rules[open]'),size=n=>parseFloat(getComputedStyle(n).fontSize),r=p.getBoundingClientRect();return {body:size(p),title:size(p.querySelector('h2')),action:size(p.querySelector('button')),top:r.top,bottom:r.bottom,scroll:p.scrollHeight-p.clientHeight};})()`);
        assert.ok(Math.abs(dialog.body-font)<.02&&Math.abs(dialog.title-font*1.25)<.02,'The adaptive modal applies text scale once');
        assert.ok(Math.abs(dialog.action-font*.875)<.02&&dialog.scroll<=1&&dialog.top>=-1&&dialog.bottom<=profile.height+1,JSON.stringify({profile,language,font,players,dialog}));
        checks.at(-1).observed.scoringDetails=dialog;
        await evaluate("document.querySelector('.player-dialog[open]').close();[...document.querySelectorAll('.whist-score button')].find(n=>/Istoric|history/i.test(n.textContent)).click()");
        const history=await evaluate(`(()=>{const p=document.querySelector('#history[open]'),r=p.getBoundingClientRect(),body=p.querySelector('.history-data');return {top:r.top,bottom:r.bottom,scroll:p.scrollHeight-p.clientHeight,range:body.scrollHeight-body.clientHeight};})()`);
        assert.ok(history.top>=-1&&history.bottom<=profile.height+1&&history.scroll<=1,'Only the history data region may scroll: '+JSON.stringify(history));
        assert.ok(history.range>0,'Full history retains its internal scroll access');
        checks.at(-1).observed.history=history;
        await evaluate("document.querySelector('#history').close();document.querySelector('.whist-score button.primary').click()");
        await until(()=>evaluate('!!document.querySelector(".whist-home button.primary")'));
        assert.equal(requests.at(-1).pathname,'/api/action');assert.equal(requests.at(-1).fields.action,'next','Next hand emits the native command once');
      }
    }
  });
  if(process.env.ATF_COMPONENT_CAPTURE_DIR)await fs.writeFile(path.join(process.env.ATF_COMPONENT_CAPTURE_DIR,'score-typography-checks.json'),JSON.stringify(checks,null,2));
  console.log('Dropdown and score typography observations: '+JSON.stringify(checks));
});
