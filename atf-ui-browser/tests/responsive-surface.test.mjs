import assert from 'node:assert/strict';
import test from 'node:test';
import fs from 'node:fs/promises';
import os from 'node:os';
import path from 'node:path';
import http from 'node:http';
import {fileURLToPath} from 'node:url';
import {adaptiveViewport, surfaceTypography, MENU_TYPOGRAPHY, viewportGeometry} from '../src/main/web/viewport.mjs';
import {boundedPages} from '../src/main/web/surface-fit.mjs';
import {withBrowser, until} from './fixtures.mjs';

const web=fileURLToPath(new URL('../src/main/web/',import.meta.url));
const profiles=[{width:320,height:568,mobile:true},{width:414,height:896,mobile:true},
  {width:1280,height:720,mobile:false},{width:1600,height:900,mobile:false},{width:420,height:460,mobile:false},
  {width:414,height:300,mobile:true}];

test('height-led dimensions preserve scene proportions and the legacy adapter',()=>{
  for(const {width,height} of [...profiles,{width:220,height:180},{width:1920,height:300}]){
    const frame=adaptiveViewport(width,height);
    assert.equal(frame.height,896);assert.ok(frame.width>=320&&frame.width<=768);
    assert.equal(frame.scale,Math.min(height/896,width/320));
    assert.equal(frame.frameWidth,frame.width*frame.scale);assert.ok(frame.frameWidth<=width+.001);
    assert.ok(frame.frameHeight<=height+.001);assert.equal(frame.frameHeight,896*frame.scale);
  }
  const a={},b={},host={children:[a,b]};
  for(const groups of [[],[[a]],[[a],[a]],[[a,b],[]],[[a],{}]])assert.throws(()=>boundedPages(host,groups),TypeError);
  assert.equal(viewportGeometry(1280,720,false).width,414,'Existing consumers retain the reference-frame adapter');
  assert.equal(surfaceTypography()['--menu-action-size'],'1.5rem');
  for(const role of Object.keys(MENU_TYPOGRAPHY))assert.match(surfaceTypography()['--menu-'+role+'-size'],/^\d+(\.\d+)?rem$/);
  for(const bad of [0,-1,NaN,Infinity,undefined,'414']){
    assert.throws(()=>adaptiveViewport(bad,896),RangeError);assert.throws(()=>adaptiveViewport(414,bad),RangeError);
  }
  console.log('Adaptive policy observations: height-led proportional fit, bounded width, separate rem roles, unchanged legacy adapter; invalid dimensions rejected.');
});

function setup(language){
  const en=language==='en';
  const field=(key,label,initial,options=[])=>({key,label,initial,options});
  return [field('players',en?'Players':'Jucători','4',[3,4,5,6].map(n=>({value:String(n),label:String(n)}))),
    field('mode',en?'Game variant':'Varianta jocului','ROMANIAN_1_8_1',[{value:'ROMANIAN_1_8_1',label:en?'Romanian Whist 1–8–1':'Whist românesc 1–8–1'}]),
    field('difficulty',en?'Opponents level':'Nivel adversari','NORMAL',[{value:'NORMAL',label:'Normal'},{value:'HARD',label:en?'Hard':'Dificil'}]),
    field('series',en?'Series points':'Puncte serie','10',[{value:'5',label:'5'},{value:'10',label:'10'}]),
    field('streaks','Series','yes'),field('zero',en?'Allow playing for zero':'Permite joc la 0','yes')];
}

test('production menus stay readable and centered in compact and desktop Chromium at RO EN and enlarged text',async t=>{
  const work=await fs.mkdtemp(path.join(os.tmpdir(),'whist-responsive-'));
  t.after(()=>fs.rm(work,{recursive:true,force:true,maxRetries:15,retryDelay:200}));
  const catalogue={summary:'Progress',groups:Array.from({length:3},(_,i)=>({heading:'Group '+(i+1),goals:Array.from({length:5},(_,j)=>({marker:'○',title:'Exact contract '+(j+1),level:'Normal',progress:'0 / 5',description:'Win the declared tricks without receiving duplicate credit.'}))}))};
  const reply={revision:1,view:null,collections:[],profile:{name:'Player',savedName:true,catalogue}};
  const rankings={preferred:'romanian',boards:[{key:'romanian',label:'Romanian Whist',entries:Array.from({length:20},(_,i)=>({label:'Player '+(i+1),score:100-i}))}]};
  const copy={home:'Whist',language:'Language',name:'Your name',newGame:'New game',resume:'Resume game',ranking:'Rankings',
    achievements:'Achievements',help:'How to play',setup:'Game settings',start:'Start game',back:'Back',streaks:'Five-hand streaks',rulesAndSchedule:'Rules and schedule',save:'Save',cancel:'Cancel',nameHint:'Maximum 10 characters.',reset:'Reset name',instructionsSubtitle:'How to play',rules:Array.from({length:4},(_,i)=>({title:'Rule '+(i+1),body:'Follow the lead suit. When void, play trump if available. Each bid is an exact contract.'}))};
  const rules={ROMANIAN_1_8_1:{romanian:true}};
  const palette=[{name:'--whist-paper',color:'#f6f5ed'},{name:'--whist-pine',color:'#1d362b'},{name:'--whist-surface',color:'#fffef8'},{name:'--whist-accent',color:'#306f4e'}];
  const configuration={surface:'whist',title:'Whist',palette,fields:setup('ro'),englishFields:setup('en'),copy:{en:{...copy,modeRules:rules},ro:{modeRules:rules,rules:Array.from({length:4},(_,i)=>({title:'Regula '+(i+1),body:'Respectă culoarea cerută. În lipsa ei, joacă atu dacă ai. Licitația este un contract exact.'}))}},autoDelayMillis:0};
  const server=http.createServer(async(req,res)=>{
    try{
      const pathname=new URL(req.url,'http://localhost').pathname;
      if(pathname.startsWith('/api/')){
        res.setHeader('Content-Type','application/json');res.end(JSON.stringify(pathname==='/api/setup'?configuration:pathname==='/api/rankings'?rankings:reply));return;
      }
      const file=pathname==='/'?'index.html':pathname.slice(1);
      if(file.includes('/')||file.includes('..')){res.writeHead(404).end();return;}
      const bytes=await fs.readFile(path.join(web,file));
      res.setHeader('Content-Type',file.endsWith('.mjs')?'text/javascript':file.endsWith('.css')?'text/css':file.endsWith('.html')?'text/html':'application/octet-stream');res.end(bytes);
    }catch{res.writeHead(404).end();}
  });
  await new Promise(resolve=>server.listen(0,'127.0.0.1',resolve));
  t.after(()=>new Promise(resolve=>server.close(resolve)));
  const url='http://127.0.0.1:'+server.address().port+'/';
  const executable=process.env.ATF_BROWSER_EXECUTABLE??'C:/Program Files (x86)/Microsoft/Edge/Application/msedge.exe';
  const observations=[],checks=[];
  await withBrowser(executable,path.join(work,'profile'),async({send,evaluate,version})=>{
    await send('Page.enable');
    for(const profile of profiles)for(const language of ['ro','en'])for(const font of [16,20.8]){
      await send('Emulation.setDeviceMetricsOverride',{...profile,deviceScaleFactor:1});
      await send('Emulation.setUserAgentOverride',{userAgent:profile.mobile?'Mozilla/5.0 (Linux; Android 14; Mobile)':'Mozilla/5.0 (Windows NT 10.0; Win64; x64)'});
      await send('Page.navigate',{url});
      await until(()=>evaluate('!!document.querySelector(".whist-home button.primary")'));
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
        if(['home','setup'].includes(screen))assert.ok(Math.abs(measured.title-font*2.34)<.01);
        assert.ok(Math.abs(measured.scaleX-measured.scaleY)<.00001,'All scene roles use one proportional fit');
        assert.ok(measured.extraWidth<=1,JSON.stringify(measured));
        assert.equal(measured.reachable,true,JSON.stringify({profile,language,font,screen,measured}));
        assert.ok(measured.controls.every(n=>!n.overflow),JSON.stringify({screen,measured}));
        const id=`RESPONSIVE-${profile.width}-${profile.height}-${language}-${font}-${screen}`;
        const images=[];
        if(process.env.ATF_COMPONENT_CAPTURE_DIR&&profile.width===414&&profile.height===896){
          await fs.mkdir(process.env.ATF_COMPONENT_CAPTURE_DIR,{recursive:true});await evaluate("document.querySelector('#table').scrollTop=0");
          const file=id+'.png',png=await send('Page.captureScreenshot',{format:'png'});
          await fs.writeFile(path.join(process.env.ATF_COMPONENT_CAPTURE_DIR,file),Buffer.from(png.data,'base64'));images.push({title:id,file});
        }
        if(['achievements','rankings','help'].includes(screen)){
          const count=await evaluate("document.querySelector("+JSON.stringify(selector)+").querySelector('.surface-pages output')?.textContent");
          assert.ok(count?.startsWith('1 / '),'Long content is paginated without vertical scroll');
          await evaluate("document.querySelector("+JSON.stringify(selector)+").querySelector('.surface-pages button:last-child').click()");
          assert.ok((await evaluate("document.querySelector("+JSON.stringify(selector)+").querySelector('.surface-pages output').textContent")).startsWith('2 / '));
        }
        if(measured.modal)await evaluate("document.querySelector('dialog[open]').close()");
        if(screen==='help')await evaluate("document.querySelector('.whist-help > button').click()");
        observations.push({profile,language,font,screen,version,...measured});
        checks.push({id,name:`${screen} ${language} ${profile.width}×${profile.height} text ${font/16}`,status:'PASS',expected,observed:{version,...measured},images});
      }
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
      const {surfaceFitter}=await import('./surface-fit.mjs?v=0.4');
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
