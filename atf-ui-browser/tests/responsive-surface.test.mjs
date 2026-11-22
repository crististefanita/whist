import assert from 'node:assert/strict';
import test from 'node:test';
import fs from 'node:fs/promises';
import os from 'node:os';
import path from 'node:path';
import http from 'node:http';
import {fileURLToPath} from 'node:url';
import {adaptiveViewport, surfaceTypography, MENU_TYPOGRAPHY, viewportGeometry} from '../src/main/web/viewport.mjs';
import {withBrowser, until} from './fixtures.mjs';

const web=fileURLToPath(new URL('../src/main/web/',import.meta.url));
const profiles=[{width:320,height:568,mobile:true},{width:414,height:896,mobile:true},
  {width:1280,height:720,mobile:false},{width:1600,height:900,mobile:false},{width:420,height:460,mobile:false},
  {width:414,height:300,mobile:true}];

test('adaptive dimensions preserve CSS pixels and text roles without changing the legacy adapter',()=>{
  for(const {width,height} of [...profiles,{width:220,height:180},{width:1920,height:300}]){
    const frame=adaptiveViewport(width,height);
    assert.equal(frame.scale,1);assert.equal(frame.width,Math.min(width,768));assert.equal(frame.height,height);
    assert.equal(frame.frameWidth,frame.width);assert.equal(frame.frameHeight,height);
    for(const font of [16,20.8,32])assert.deepEqual(adaptiveViewport(width,height),frame,'Text zoom is not a scene scale');
  }
  assert.equal(viewportGeometry(1280,720,false).width,414,'Existing consumers retain the reference-frame adapter');
  assert.equal(surfaceTypography()['--menu-action-size'],'1.5rem');
  for(const role of Object.keys(MENU_TYPOGRAPHY))assert.match(surfaceTypography()['--menu-'+role+'-size'],/^\d+(\.\d+)?rem$/);
  for(const bad of [0,-1,NaN,Infinity,undefined,'414']){
    assert.throws(()=>adaptiveViewport(bad,896),RangeError);assert.throws(()=>adaptiveViewport(414,bad),RangeError);
  }
  console.log('Adaptive policy observations: unscaled bounded width, usable height, independent rem roles, unchanged legacy adapter; invalid dimensions rejected.');
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
  const reply={revision:1,view:null,collections:[],profile:{name:'Player',savedName:true}};
  const copy={home:'Whist',language:'Language',name:'Your name',newGame:'New game',resume:'Resume game',ranking:'Rankings',
    achievements:'Achievements',help:'How to play',setup:'Game settings',start:'Start game',back:'Back',streaks:'Five-hand streaks',rulesAndSchedule:'Rules and schedule'};
  const rules={ROMANIAN_1_8_1:{romanian:true}};
  const configuration={surface:'whist',title:'Whist',fields:setup('ro'),englishFields:setup('en'),copy:{en:{...copy,modeRules:rules},ro:{modeRules:rules}},autoDelayMillis:0};
  const server=http.createServer(async(req,res)=>{
    try{
      const pathname=new URL(req.url,'http://localhost').pathname;
      if(pathname.startsWith('/api/')){
        res.setHeader('Content-Type','application/json');res.end(JSON.stringify(pathname==='/api/setup'?configuration:reply));return;
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
      for(const screen of ['home','setup']){
        if(screen==='setup')await evaluate("document.querySelector('.whist-home button.primary').click()");
        await until(()=>evaluate(`!!document.querySelector('.${screen==='home'?'whist-home':'setup'} h1')`));
        const measured=await evaluate(`(()=>{
          const frame=document.querySelector('#frame'),stage=document.querySelector('#table'),box=frame.getBoundingClientRect();
          const rect=node=>{const r=node.getBoundingClientRect();return {left:r.left,top:r.top,width:r.width,height:r.height};};
          const controls=[...stage.querySelectorAll('button,select')].filter(node=>node.getClientRects().length);
          return {frame:rect(frame),stage:rect(stage),transform:getComputedStyle(stage).transform,
            root:parseFloat(getComputedStyle(document.documentElement).fontSize),title:parseFloat(getComputedStyle(stage.querySelector('h1')).fontSize),
            extraWidth:stage.scrollWidth-stage.clientWidth,controls:controls.map(n=>({text:n.textContent,size:parseFloat(getComputedStyle(n).fontSize),
              width:n.getBoundingClientRect().width,height:n.getBoundingClientRect().height,overflow:n.scrollWidth>n.clientWidth+1})),
            reachable:controls.every(n=>{n.scrollIntoView({block:'center'});const r=n.getBoundingClientRect();return r.top>=box.top-1&&r.bottom<=box.bottom+1;})};
        })()`);
        const expected={width:Math.min(profile.width,768),height:profile.height,scale:1,title:font*2.34};
        assert.ok(Math.abs(measured.frame.width-expected.width)<1);assert.ok(Math.abs(measured.frame.height-expected.height)<1);
        assert.ok(Math.abs(measured.frame.left-(profile.width-expected.width)/2)<1);assert.equal(measured.transform,'none');
        assert.ok(Math.abs(measured.title-expected.title)<.01);assert.ok(measured.extraWidth<=1,JSON.stringify(measured));
        assert.equal(measured.reachable,true,JSON.stringify({profile,language,font,screen,measured}));
        assert.ok(measured.controls.every(n=>n.height>=48&&!n.overflow),JSON.stringify({profile,language,font,screen,measured}));
        const id=`RESPONSIVE-${profile.width}-${profile.height}-${language}-${font}-${screen}`;
        const images=[];
        if(process.env.ATF_COMPONENT_CAPTURE_DIR&&[414,1280].includes(profile.width)){
          await fs.mkdir(process.env.ATF_COMPONENT_CAPTURE_DIR,{recursive:true});await evaluate("document.querySelector('#table').scrollTop=0");
          const file=id+'.png',png=await send('Page.captureScreenshot',{format:'png'});
          await fs.writeFile(path.join(process.env.ATF_COMPONENT_CAPTURE_DIR,file),Buffer.from(png.data,'base64'));images.push({title:id,file});
        }
        observations.push({profile,language,font,screen,version,...measured});
        checks.push({id,name:`${screen} ${language} ${profile.width}×${profile.height} text ${font/16}`,status:'PASS',expected,observed:{version,...measured},images});
      }
    }
  });
  if(process.env.ATF_COMPONENT_CAPTURE_DIR)await fs.writeFile(path.join(process.env.ATF_COMPONENT_CAPTURE_DIR,'browser-checks.json'),JSON.stringify(checks,null,2));
  console.log('Responsive renderer observations: '+JSON.stringify(observations));
});
