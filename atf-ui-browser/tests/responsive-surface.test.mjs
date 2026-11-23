import assert from 'node:assert/strict';
import test from 'node:test';
import fs from 'node:fs/promises';
import path from 'node:path';
import {fileURLToPath,pathToFileURL} from 'node:url';
const web=process.env.ATF_BROWSER_WEB_ROOT??fileURLToPath(new URL('../src/main/web/',import.meta.url));
const {adaptiveViewport, surfaceTypography, MENU_TYPOGRAPHY, ADAPTIVE_SURFACE, viewportGeometry}=await import(pathToFileURL(path.join(web,'viewport.mjs')));
const {boundedPages}=await import(pathToFileURL(path.join(web,'surface-fit.mjs')));
import {withBrowser, until} from './fixtures.mjs';

import {rendererFixture} from './responsive-fixture.mjs';
const profiles=[{width:320,height:568,mobile:true},{width:414,height:896,mobile:true},
  {width:1280,height:720,mobile:false},{width:1600,height:900,mobile:false},{width:420,height:460,mobile:false},
  {width:414,height:300,mobile:true}];

function pagingContrast(button){
  const rgb=value=>value.match(/[\d.]+/g).slice(0,3).map(Number);
  const mix=(ink,paper,opacity)=>ink.map((value,i)=>value*opacity+paper[i]*(1-opacity));
  const light=values=>values.map(v=>v/255).map(v=>v<=.04045?v/12.92:((v+.055)/1.055)**2.4)
    .reduce((sum,v,i)=>sum+v*[.2126,.7152,.0722][i],0);
  const background=rgb(button.paper),foreground=light(mix(rgb(button.ink),background,button.opacity));
  const fill=light(mix(rgb(button.fill),background,button.opacity));
  return (Math.max(foreground,fill)+.05)/(Math.min(foreground,fill)+.05);
}

test('height-led dimensions preserve scene proportions and the legacy adapter',()=>{
  for(const {width,height} of [...profiles,{width:220,height:180},{width:1920,height:300}]){
    const frame=adaptiveViewport(width,height);
    assert.equal(frame.height,ADAPTIVE_SURFACE.height);assert.equal(frame.width,ADAPTIVE_SURFACE.width);
    assert.equal(frame.scale,Math.min(height/ADAPTIVE_SURFACE.height,width/ADAPTIVE_SURFACE.width));
    assert.equal(frame.frameWidth,frame.width*frame.scale);assert.ok(frame.frameWidth<=width+.001);
    assert.ok(frame.frameHeight<=height+.001);assert.equal(frame.frameHeight,ADAPTIVE_SURFACE.height*frame.scale);
  }
  const a={},b={},host={children:[a,b]};
  for(const groups of [[],[[a]],[[a],[a]],[[a,b],[]],[[a],{}]])assert.throws(()=>boundedPages(host,groups),TypeError);
  assert.equal(viewportGeometry(1280,720,false).width,414,'Existing consumers retain the reference-frame adapter');
  assert.equal(surfaceTypography()['--menu-action-size'],'1rem');
  for(const {width,height} of profiles){
    const phone=adaptiveViewport(width,height,undefined,true);
    assert.deepEqual(phone,{width,height,scale:1,frameWidth:width,frameHeight:height},'Phone CSS dimensions match useful native dp before overflow fitting');
  }
  for(const mobile of ['true',1,null])assert.throws(()=>adaptiveViewport(414,896,undefined,mobile),RangeError);
  for(const role of Object.keys(MENU_TYPOGRAPHY))assert.match(surfaceTypography()['--menu-'+role+'-size'],/^\d+(\.\d+)?rem$/);
  for(const bad of [0,-1,NaN,Infinity,undefined,'414']){
    assert.throws(()=>adaptiveViewport(bad,896),RangeError);assert.throws(()=>adaptiveViewport(414,bad),RangeError);
  }
  console.log('Adaptive policy observations: height-led proportional fit, bounded width, separate rem roles, unchanged legacy adapter; invalid dimensions rejected.');
});

test('production menus stay readable and centered in compact and desktop Chromium at RO EN and enlarged text',async t=>{
  const {work,url}=await rendererFixture(t);
  const executable=process.env.ATF_BROWSER_EXECUTABLE??'C:/Program Files (x86)/Microsoft/Edge/Application/msedge.exe';
  const observations=[],checks=[];
  await withBrowser(executable,path.join(work,'profile'),async({send,evaluate,version})=>{
    await send('Page.enable');
    for(const profile of profiles)for(const language of ['ro','en'])for(const font of [16,20.8]){
      await send('Emulation.setDeviceMetricsOverride',{...profile,deviceScaleFactor:1});
      await send('Emulation.setUserAgentOverride',{userAgent:profile.mobile?'Mozilla/5.0 (Linux; Android 14; Mobile)':'Mozilla/5.0 (Windows NT 10.0; Win64; x64)'});
      await send('Page.navigate',{url});
      await until(()=>evaluate('!!document.querySelector(".whist-home button.primary")'));
      await evaluate('document.fonts.ready');
      await evaluate(`document.documentElement.style.fontSize='${font}px';document.querySelector('.whist-language select').value='${language}';document.querySelector('.whist-language select').dispatchEvent(new Event('change'));`);
      await until(()=>evaluate(`document.documentElement.lang==='${language}'&&document.querySelector('#table').getAttribute('aria-busy')==='false'`));
      for(const screen of (profile.height===300?['profile']:['home','setup','profile','help','achievements','rankings'])){
        if(screen==='setup')await evaluate("document.querySelector('.whist-home button.primary').click()");
        if(['profile','help','achievements','rankings'].includes(screen)){
          await evaluate("document.querySelector('.setup > button.quiet')?.click()");
          const match={profile:'/Numele tău|Your name/',help:'/Reguli de joc|How to play/',achievements:'/Realizări|Achievements/',rankings:'/Clasamente|Rankings/'}[screen];
          await evaluate("[...document.querySelectorAll('.whist-home button')].find(n=>"+match+".test(n.textContent)).click()");
        }
        const selector={home:'.whist-home',setup:'.setup',profile:'.profile-editor',help:'.whist-help',achievements:'.achievement-catalogue',rankings:'.game-rankings'}[screen];
        await until(()=>evaluate('!!document.querySelector('+JSON.stringify(selector)+')'));
        await until(()=>evaluate('document.querySelector('+JSON.stringify(selector)+').getBoundingClientRect().bottom<=innerHeight+1'));
        await new Promise(resolve=>setTimeout(resolve,50));
        const measured=await evaluate(`(()=>{
          const frame=document.querySelector('#frame'),stage=document.querySelector('#table'),panel=document.querySelector(${JSON.stringify(selector)}),box=frame.getBoundingClientRect();
          const rect=node=>{const r=node.getBoundingClientRect();return {left:r.left,top:r.top,width:r.width,height:r.height};};
          const controls=[...panel.querySelectorAll('button,select,input')].filter(node=>node.getClientRects().length);
          const panelBox=panel.getBoundingClientRect();const modal=panel.matches('dialog');
          return {frame:rect(frame),stage:rect(stage),transform:getComputedStyle(stage).transform,
            background:getComputedStyle(stage).backgroundColor,foreground:getComputedStyle(stage).color,logicalHeight:stage.clientHeight,scale:box.height/stage.clientHeight,scaleX:new DOMMatrixReadOnly(getComputedStyle(stage).transform).a,scaleY:new DOMMatrixReadOnly(getComputedStyle(stage).transform).d,panel:rect(panel),pageHeight:document.documentElement.scrollHeight,stageOverflow:stage.scrollHeight-stage.clientHeight,panelOverflow:panel.scrollHeight-panel.clientHeight,modal,
            root:parseFloat(getComputedStyle(document.documentElement).fontSize),title:parseFloat(getComputedStyle(panel.querySelector('h1,h2')).fontSize),
            extraWidth:stage.scrollWidth-stage.clientWidth,controls:controls.map(n=>({text:n.textContent,size:parseFloat(getComputedStyle(n).fontSize),
              width:n.getBoundingClientRect().width,height:n.getBoundingClientRect().height,overflow:n.scrollWidth>n.clientWidth+1})),
            reachable:controls.every(n=>{const r=n.getBoundingClientRect();return r.top>=-1&&r.bottom<=innerHeight+1&&r.left>=-1&&r.right<=innerWidth+1;})};
        })()`);
        const expected={height:profile.height,noVerticalScroll:true,proportionalScale:true};
        assert.equal(measured.background,'rgb(246, 245, 237)','The controlled fixture supplies the native Whist paper palette');
        assert.equal(measured.foreground,'rgb(29, 54, 43)','The native pine text remains visible on the paper background');
        assert.ok(measured.frame.width<=profile.width+1);assert.ok(Math.abs(measured.frame.height-profile.height)<1);
        assert.ok(Math.abs(measured.frame.left-(profile.width-measured.frame.width)/2)<1);
        assert.ok(measured.pageHeight<=profile.height+1,JSON.stringify(measured));
        assert.ok(measured.stageOverflow<=1&&measured.panelOverflow<=1,JSON.stringify({screen,profile,measured}));
        if(['home','setup'].includes(screen))assert.ok(Math.abs(measured.title-font*MENU_TYPOGRAPHY.title)<.01);
        assert.ok(Math.abs(measured.scaleX-measured.scaleY)<.00001,'All scene roles use one proportional fit');
        assert.ok(measured.extraWidth<=1,JSON.stringify(measured));
        assert.equal(measured.reachable,true,JSON.stringify({profile,language,font,screen,measured}));
        assert.ok(measured.controls.every(n=>!n.overflow),JSON.stringify({screen,measured}));
        if(screen==='rankings'){
          const inspectSpacing=()=>evaluate(`(()=>{const p=document.querySelector('.game-rankings .rankings > p'),select=document.querySelector('.ranking-options select'),panel=document.querySelector('.game-rankings');const scale=panel.getBoundingClientRect().height/panel.offsetHeight;return {gap:(select.getBoundingClientRect().top-p.getBoundingClientRect().bottom)/scale,margin:parseFloat(getComputedStyle(p).marginBottom)};})()`);
          const spacing=await inspectSpacing();
          assert.ok((await evaluate("document.querySelector('.game-rankings .rankings > p').textContent")).length>20,'Measure the real wrapped explanatory text, not an empty paragraph');
          const verifyGap=value=>assert.ok(value.gap>=font*.74&&value.margin>=font*.74,'Ranking summary must leave a rem-sized gap before its first dropdown: '+JSON.stringify(value));
          verifyGap(spacing);assert.ok(Math.abs(measured.panel.height-profile.height)<1,'Rankings use the available window height without scrolling');
          if(profile.width===320&&language==='ro'&&font===16){
            await evaluate("document.querySelector('.game-rankings .rankings > p').style.marginBottom='0px'");
            const gap=await inspectSpacing();
            assert.throws(()=>verifyGap(gap),/Ranking summary/);
            await evaluate("document.querySelector('.game-rankings .rankings > p').style.removeProperty('margin-bottom')");
          }
          measured.rankingSpacing=spacing;
        }
        const id=`RESPONSIVE-${profile.width}-${profile.height}-${language}-${font}-${screen}`;
        const images=[];
        if(process.env.ATF_COMPONENT_CAPTURE_DIR&&(profile.width===414&&profile.height===896||profile.width===1280&&['home','setup'].includes(screen))){
          await fs.mkdir(process.env.ATF_COMPONENT_CAPTURE_DIR,{recursive:true});await evaluate("document.querySelector('#table').scrollTop=0");
          const file=id+'.png',png=await send('Page.captureScreenshot',{format:'png'});
          await fs.writeFile(path.join(process.env.ATF_COMPONENT_CAPTURE_DIR,file),Buffer.from(png.data,'base64'));images.push({title:id,file});
        }
        if(['achievements','rankings','help'].includes(screen)){
          const close=await evaluate('document.querySelector('+JSON.stringify(selector)+').querySelector(":scope > button").textContent');
          assert.equal(close,language==='en'?'Close':'Închide','Close is distinct from the Previous page command');
          const paging=()=>evaluate(`(()=>{const panel=document.querySelector(${JSON.stringify(selector)});let paper=panel;while(paper&&getComputedStyle(paper).backgroundColor==='rgba(0, 0, 0, 0)')paper=paper.parentElement;return [...panel.querySelectorAll('.surface-pages button')].map(n=>{
            const style=getComputedStyle(n),face=getComputedStyle(n,'::before'),range=document.createRange();range.selectNodeContents(n);const ink=range.getBoundingClientRect(),rect=n.getBoundingClientRect(),scale=rect.height/n.offsetHeight;
            return {text:n.textContent,disabled:n.disabled,ink:style.color,fill:face.content==='none'?style.backgroundColor:face.backgroundColor,paper:paper?getComputedStyle(paper).backgroundColor:'rgb(255, 255, 255)',opacity:Number(style.opacity),targetHeight:n.offsetHeight,targetWidth:n.offsetWidth,face:face.content,
              faceHeight:n.offsetHeight-(parseFloat(face.top)||0)-(parseFloat(face.bottom)||0),faceWidth:n.offsetWidth-(parseFloat(face.left)||0)-(parseFloat(face.right)||0),inkWidth:ink.width/scale,inkHeight:ink.height/scale};});})()`);
          const verify=buttons=>{for(const n of buttons){
            assert.ok(pagingContrast(n)>=(n.disabled?3:4.5),'Readable paging text: '+JSON.stringify({screen,language,font,...n}));
            assert.ok(n.targetHeight>=48,'Keep the native-sized touch region');
            assert.notEqual(n.face,'none','The compact outline is independent of its touch region');
            assert.ok(n.faceHeight<=n.targetHeight-8&&n.faceWidth<=n.targetWidth-4,'The outline stays close to its label');
            assert.ok(n.inkWidth<=n.faceWidth-2&&n.inkHeight<=n.faceHeight-2,'The complete label fits inside its outline');
          }};
          const first=await paging();verify(first);assert.equal(first[0].disabled,true);
          if(screen==='achievements'&&profile.width===320&&font===16&&language==='ro'){
            await evaluate("(()=>{const n=document.querySelector('.achievement-catalogue .surface-pages button:last-child');n.style.setProperty('--page-button-ink','#fff');n.style.setProperty('--page-button-fill','#fff');})()");
            const unreadable=await paging();assert.throws(()=>verify(unreadable),/Readable paging text/);
            await evaluate("(()=>{const n=document.querySelector('.achievement-catalogue .surface-pages button:last-child');n.style.removeProperty('--page-button-ink');n.style.removeProperty('--page-button-fill');})()");
          }
          const count=await evaluate("document.querySelector("+JSON.stringify(selector)+").querySelector('.surface-pages output')?.textContent");
          assert.ok(count?.startsWith('1 / '),'Long content is paginated without vertical scroll');
          await evaluate("document.querySelector("+JSON.stringify(selector)+").querySelector('.surface-pages button:last-child').click()");
          assert.ok((await evaluate("document.querySelector("+JSON.stringify(selector)+").querySelector('.surface-pages output').textContent")).startsWith('2 / '));
          const next=await paging();verify(next);assert.equal(next[0].disabled,false);
          await evaluate("document.querySelector("+JSON.stringify(selector)+").querySelector('.surface-pages button:first-child').click()");
          assert.ok((await evaluate("document.querySelector("+JSON.stringify(selector)+").querySelector('.surface-pages output').textContent")).startsWith('1 / '),'Previous changes the page without dismissing the reader');
          measured.paging={first,next,close};
        }
        if(['achievements','rankings','help'].includes(screen)){
          await evaluate("document.querySelector("+JSON.stringify(selector)+").querySelector(':scope > button').click()");
          const dismissed=measured.modal?selector+'[open]':selector;
          await until(()=>evaluate('!document.querySelector('+JSON.stringify(dismissed)+')'));
          assert.equal(await evaluate('!!document.querySelector("dialog[open]")'),false,'Close dismisses the reader rather than changing its page');
          assert.equal(await evaluate('!!document.querySelector(".whist-home")'),true,'Close returns to the underlying menu');
        }else if(measured.modal)await evaluate("document.querySelector('dialog[open]').close()");
        observations.push({profile,language,font,screen,version,...measured});
        checks.push({id,name:`${screen} ${language} ${profile.width}×${profile.height} text ${font/16}`,status:'PASS',expected,observed:{version,...measured},images});
      }
    }
    // Exercise the production dialog factory and fitter with a controlled six-seat score table.
    // In particular, a legacy 60dvh cap must not introduce an inner scrollbar before fitting.
    for(const profile of profiles.filter(p=>p.height!==300))for(const language of ['ro','en'])for(const font of [16,20.8]){
      await send('Emulation.setDeviceMetricsOverride',{...profile,deviceScaleFactor:1});
      await evaluate(`document.documentElement.style.fontSize='${font}px';document.documentElement.lang='${language}';`);
      const measured=await evaluate(`(async()=>{
        const {createProfileDialogs}=await import('./profile.mjs?v=0.8');
        const {surfaceFitter}=await import('./surface-fit.mjs?v=0.8');
        const element=(tag,text='',kind='')=>{const n=document.createElement(tag);n.textContent=text;n.className=kind;return n;};
        const button=(text,action,kind='')=>{const n=element('button',text,kind);n.onclick=action;return n;};
        const en=document.documentElement.lang==='en';
        const dialogs=createProfileDialogs(element,button,()=>{},(key,fallback)=>en?({close:'Close',history:'Score history'}[key]??fallback):fallback);
        const stage=document.querySelector('#table');
        const fitting=surfaceFitter(stage,dialogs.dialog,()=>fitting.fitDialog(innerWidth,innerHeight,/Android/.test(navigator.userAgent)));
        const table=element('table','','whist-score-grid'),head=element('thead'),body=element('tbody');
        const row=(values,tag)=>{const r=element('tr');for(const value of values)r.append(element(tag,value));return r;};
        head.append(row(en?['Player','Bid','Won','Points']:['Jucător','Licitat','Levate','Puncte'],'th'));
        for(let i=0;i<6;i++)body.append(row(['Player '+(i+1),'2','2',String(20+i)],'td'));
        table.append(head,body);
        dialogs.liveScore({liveTitle:en?'Match score':'Scorul partidei',summary:en?'Hand 4 / 23':'Mâna 4 / 23',pendingBid:en?'Bids submitted 6/6':'Licitații depuse 6/6'},table,()=>{});
        await new Promise(resolve=>requestAnimationFrame(()=>requestAnimationFrame(resolve)));
        const p=dialogs.dialog,r=p.getBoundingClientRect();
        const result={top:r.top,bottom:r.bottom,pageHeight:document.documentElement.scrollHeight,scroll:p.scrollHeight-p.clientHeight,rows:table.tBodies[0].rows.length,
          scrollers:[...p.querySelectorAll('*')].filter(n=>n.getClientRects().length&&/auto|scroll/.test(getComputedStyle(n).overflowY)&&n.scrollHeight>n.clientHeight+1).map(n=>({kind:n.className,content:n.scrollHeight,height:n.clientHeight}))};
        globalThis.disposeScoreFixture=()=>{dialogs.dialog.close();fitting.dispose();dialogs.dialog.remove();};
        return result;
      })()`);
      const id=`LIVE-SCORE-${profile.width}-${profile.height}-${language}-${font}`,images=[];
      assert.equal(measured.rows,6);assert.ok(measured.top>=-1&&measured.bottom<=profile.height+1,JSON.stringify(measured));
      assert.ok(measured.pageHeight<=profile.height+1&&measured.scroll<=1&&!measured.scrollers.length,JSON.stringify({profile,language,font,measured}));
      if(process.env.ATF_COMPONENT_CAPTURE_DIR&&((profile.width===414&&profile.height===896)||profile.height===460)){
        const file=id+'.png',png=await send('Page.captureScreenshot',{format:'png'});
        await fs.writeFile(path.join(process.env.ATF_COMPONENT_CAPTURE_DIR,file),Buffer.from(png.data,'base64'));images.push({file,title:id});
      }
      checks.push({id,name:`Live score ${language} ${profile.width}×${profile.height} text ${font/16}`,status:'PASS',expected:{rows:6,noVerticalScroll:true},observed:{version,...measured},images});
      observations.push({profile,language,font,screen:'live-score',version,...measured});
      await evaluate('disposeScoreFixture();delete globalThis.disposeScoreFixture');
    }
    await send('Emulation.setDeviceMetricsOverride',{width:414,height:896,mobile:true,deviceScaleFactor:1});
    const keyboard=await evaluate(`(async()=>{
      const original=Object.getOwnPropertyDescriptor(window,'visualViewport');
      Object.defineProperty(window,'visualViewport',{configurable:true,value:{width:414,height:300,offsetTop:180,offsetLeft:0}});
      window.dispatchEvent(new Event('resize'));
      [...document.querySelectorAll('.whist-home button')].find(n=>/Numele tău|Your name/.test(n.textContent)).click();
      await new Promise(resolve=>requestAnimationFrame(()=>requestAnimationFrame(resolve)));
      const box=node=>{const r=node.getBoundingClientRect();return {top:r.top,bottom:r.bottom,height:r.height};};
      const result={frame:box(document.querySelector('#frame')),dialog:box(document.querySelector('dialog[open]')),layoutHeight:innerHeight};
      document.querySelector('dialog[open]').close();Object.defineProperty(window,'visualViewport',original);window.dispatchEvent(new Event('resize'));return result;
    })()`);
    assert.ok(Math.abs(keyboard.frame.top-180)<1&&Math.abs(keyboard.frame.height-300)<1,'The panned visual viewport owns the frame');
    assert.ok(keyboard.dialog.top>=179&&keyboard.dialog.bottom<=481,'The modal follows the visual viewport rather than the layout viewport');
    console.log('Visual viewport observations: '+JSON.stringify(keyboard));
    const isolation=await evaluate(`(async()=>{
      const {surfaceFitter}=await import('./surface-fit.mjs?v=0.8');
      const stage=document.createElement('main'),modal=document.createElement('dialog');
      stage.style.cssText='position:fixed;visibility:hidden;width:100px;height:100px';
      let root=document.createElement('section');root.className='whist-match';stage.append(root);document.body.append(stage,modal);
      let fits=0;const fitting=surfaceFitter(stage,modal,()=>fits++);
      const tick=()=>new Promise(resolve=>requestAnimationFrame(()=>requestAnimationFrame(resolve)));
      root=document.createElement('section');root.className='whist-match';stage.replaceChildren(root);await tick();const initial=fits;
      const overlay=document.createElement('div');overlay.className='collection';stage.append(overlay);await tick();overlay.remove();await tick();
      const auto=document.createElement('input');root.append(auto);auto.dispatchEvent(new Event('change',{bubbles:true}));await tick();
      const final=fits;fitting.dispose();stage.replaceChildren(document.createElement('section'));await tick();
      stage.remove();modal.remove();return {initial,final,afterDispose:fits};
    })()`);
    assert.ok(isolation.initial>0);assert.equal(isolation.initial,isolation.final,'Collection overlays and AutoGame changes cannot refit or cancel the scene');
    assert.equal(isolation.afterDispose,isolation.final,'Disposed adapters do not receive late callbacks');
    console.log('Surface lifecycle observations: '+JSON.stringify(isolation));
  });
  if(process.env.ATF_COMPONENT_CAPTURE_DIR)await fs.writeFile(path.join(process.env.ATF_COMPONENT_CAPTURE_DIR,'browser-checks.json'),JSON.stringify(checks,null,2));
  console.log('Responsive renderer observations: '+JSON.stringify(observations));
});
