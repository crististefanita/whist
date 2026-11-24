import assert from 'node:assert/strict';
import test from 'node:test';
import path from 'node:path';
import {withBrowser,until} from './fixtures.mjs';
import {rendererFixture} from './responsive-fixture.mjs';

test('home setup and score share command and supporting roles with isolated callbacks',async t=>{
  const {work,url,requests}=await rendererFixture(t),observations=[];
  await withBrowser(process.env.ATF_BROWSER_EXECUTABLE??'C:/Program Files (x86)/Microsoft/Edge/Application/msedge.exe',
    path.join(work,'profile'),async({send,evaluate,version})=>{
    await send('Page.enable');
    const measure=()=>evaluate(`(()=>{
      const page=document.querySelector('.whist-home,.setup,.whist-score');
      return [...page.querySelectorAll('button')].map(n=>{
        const s=getComputedStyle(n),r=n.getBoundingClientRect();
        return {label:n.textContent,size:parseFloat(s.fontSize),family:s.fontFamily,weight:s.fontWeight,
          height:n.offsetHeight,overflow:n.scrollWidth>n.clientWidth+1,visible:r.top>=-1&&r.bottom<=innerHeight+1,
          supporting:(page.classList.contains('whist-home')&&n.classList.contains('quiet'))||n.classList.contains('score-explanation')||!!(n.closest('.setup form')&&n.classList.contains('quiet'))};
      });
    })()`);
    for(const profile of [{width:414,height:896},{width:320,height:568}])
      for(const language of ['ro','en'])for(const font of [16,20.8]){
      await send('Emulation.setDeviceMetricsOverride',{...profile,mobile:true,deviceScaleFactor:1});
      await send('Page.navigate',{url});
      await until(()=>evaluate('!!document.querySelector(".whist-home button.primary")'));
      await evaluate(`document.documentElement.style.fontSize='${font}px';document.querySelector('.whist-language select').value='${language}';document.querySelector('.whist-language select').dispatchEvent(new Event('change'))`);
      await until(()=>evaluate(`document.documentElement.lang==='${language}'&&document.querySelector('#table').getAttribute('aria-busy')==='false'`));
      await evaluate('document.fonts.ready');
      const home=await measure();
      await evaluate("document.querySelector('.whist-home button.primary').click()");
      await until(()=>evaluate('!!document.querySelector(".setup form")'));
      const setup=await measure();
      const setupRequestCount=requests.filter(r=>r.pathname==='/api/start').length;
      await evaluate("document.querySelector('.setup > button.quiet').click()");
      await until(()=>evaluate('!!document.querySelector(".whist-home button.primary")'));
      assert.equal(requests.filter(r=>r.pathname==='/api/start').length,setupRequestCount,'Back never starts a game');
      await evaluate("document.querySelector('.whist-home button.primary').click()");
      await until(()=>evaluate('!!document.querySelector(".setup form")'));
      await evaluate("document.querySelector('.setup form').requestSubmit()");
      await until(()=>evaluate('!!document.querySelector(".whist-score")'));
      const score=await measure();
      assert.equal(home.length,6,'Home commands, profile and three supporting actions');
      assert.equal(setup.length,3,'Setup start, back and rules');
      assert.equal(score.length,4,'Score explanation and three commands');
      const commands=[...home,...setup,...score].filter(n=>!n.supporting);
      const supporting=[...home,...setup,...score].filter(n=>n.supporting);
      for(const [size,buttons] of [[font,commands],[font*14/16,supporting]])for(const button of buttons){
        assert.ok(Math.abs(button.size-size)<.02,JSON.stringify({profile,language,font,expected:size,button}));
        assert.equal(button.family,home[0].family,'One font family across screen adapters');
        assert.equal(button.weight,home[0].weight,'One label weight across screen adapters');
        assert.ok(button.height>=48,'The text size does not shrink the touch target');
        assert.equal(button.overflow,false,'Complete action label');
        assert.equal(button.visible,true,'Action fits the actual viewport');
      }
      await evaluate("document.querySelector('.whist-score button.primary').click()");
      await until(()=>evaluate('!!document.querySelector(".whist-home button.primary")'));
      assert.equal(requests.filter(r=>r.pathname==='/api/start').length,setupRequestCount+1,'One explicit start');
      assert.equal(requests.at(-1).fields.action,'next','Next hand dispatches its original command');
      observations.push({profile,language,font,version,home,setup,score});
    }
  });
  console.log('Shared action typography observations: '+JSON.stringify(observations));
});
