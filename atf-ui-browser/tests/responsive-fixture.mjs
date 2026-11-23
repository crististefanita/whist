import fs from 'node:fs/promises';
import os from 'node:os';
import path from 'node:path';
import http from 'node:http';
import {fileURLToPath} from 'node:url';

function settings(language){
  const en=language==='en',field=(key,label,initial,options=[])=>({key,label,initial,options});
  return [field('players',en?'Players':'Jucători','4',[3,4,5,6].map(n=>({value:String(n),label:String(n)}))),
    field('mode',en?'Game variant':'Varianta jocului','ROMANIAN_1_8_1',[{value:'ROMANIAN_1_8_1',label:en?'Romanian Whist 1–8–1':'Whist românesc 1–8–1'}]),
    field('difficulty',en?'Opponents level':'Nivel adversari','NORMAL',[{value:'NORMAL',label:'Normal'},{value:'HARD',label:en?'Hard':'Dificil'}]),
    field('series',en?'Series points':'Puncte serie','10',[{value:'5',label:'5'},{value:'10',label:'10'}]),
    field('streaks','Series','yes'),field('zero',en?'Allow playing for zero':'Permite joc la 0','yes')];
}

/** Fixed presentation facts, not a scoring oracle: the renderer must preserve each supplied cell. */
export function scoreProjection(players,language){
  const en=language==='en',names=[en?'You':'Tu','Vlad','Eva','Ana','Paul','Dora'];
  const totals=['14','6','0','500','-104','77'],deltas=['+14','-4','-1','+10','-2','+6'];
  const rows=names.slice(0,players).map((name,i)=>[name,i===0?'4':i===1?'2':i===2?'1':'0',i===0?'4':i===1?'6':'0',deltas[i],totals[i]]);
  return {heading:en?'Hand 1 / 19':'Mâna 1 / 19',prompt:en?'Hand completed':'Mână încheiată',
    score:{title:en?'Match score':'Scorul partidei',table:{headings:en?['Player','Bid','Won','Pts.','Total']:['Jucător','Lic.','Lev.','Pct.','Total'],rows}},
    scoreDetails:{summary:en?'Hand 1 / 19 · All bids submitted':'Mâna 1 / 19 · Toate licitațiile depuse',rules:[en?'An exact contract receives the declared bonus.':'Un contract exact primește bonusul declarat.']},
    actions:[{key:'next',label:en?'Next hand':'Mâna următoare',minimum:null,maximum:null,emphasized:true}],
    history:{headings:en?['Cards','Bid','Total']:['Cărți','Licitat','Total'],rows:Array.from({length:25},()=>['1','0','10'])}};
}

/** Only HTTP state is controlled; the browser loads the delivered entry point, CSS and modules. */
export async function rendererFixture(t){
  const work=await fs.mkdtemp(path.join(os.tmpdir(),'whist-responsive-'));
  t.after(()=>fs.rm(work,{recursive:true,force:true,maxRetries:15,retryDelay:200}));
  const web=process.env.ATF_BROWSER_WEB_ROOT??fileURLToPath(new URL('../src/main/web/',import.meta.url));
  const catalogue={summary:'Progress',groups:Array.from({length:3},(_,i)=>({heading:'Group '+(i+1),goals:Array.from({length:5},(_,j)=>({marker:'○',title:'Exact contract '+(j+1),level:'Normal',progress:'0 / 5',description:'Win the declared tricks without receiving duplicate credit.'}))}))};
  const reply={revision:1,view:null,collections:[],profile:{name:'Player',savedName:true,catalogue}};
  const rankings={preferred:'romanian',boards:[{key:'romanian',label:'Romanian Whist',entries:Array.from({length:20},(_,i)=>({label:'Player '+(i+1),score:100-i}))}]};
  const copy={home:'Whist',language:'Language',name:'Your name',newGame:'New game',resume:'Resume game',ranking:'Rankings',rankingHint:'Local top 20 · sample entries are marked · AI scores included',close:'Close',achievements:'Achievements',help:'How to play',setup:'Game settings',start:'Start game',back:'Back',streaks:'Five-hand streaks',rulesAndSchedule:'Rules and schedule',save:'Save',cancel:'Cancel',nameHint:'Maximum 10 characters.',reset:'Reset name',instructionsSubtitle:'How to play',
    scoringDetails:'Scoring details',history:'Score history',mainMenu:'Main menu',
    rules:Array.from({length:4},(_,i)=>({title:'Rule '+(i+1),body:'Follow the lead suit. When void, play trump if available. Each bid is an exact contract.'}))};
  const rules={ROMANIAN_1_8_1:{romanian:true}};
  const palette=[{name:'--whist-paper',color:'#f6f5ed'},{name:'--whist-pine',color:'#1d362b'},{name:'--whist-surface',color:'#fffef8'},{name:'--whist-accent',color:'#306f4e'}];
  const configuration={surface:'whist',title:'Whist',palette,fields:settings('ro'),englishFields:settings('en'),copy:{en:{...copy,modeRules:rules},ro:{close:'Închide',rankingHint:'Top 20 local · exemplele sunt marcate · include scorurile AI',modeRules:rules,rules:Array.from({length:4},(_,i)=>({title:'Regula '+(i+1),body:'Respectă culoarea cerută. În lipsa ei, joacă atu dacă ai. Licitația este un contract exact.'}))}},autoDelayMillis:0};
  let language='ro';const requests=[];
  const server=http.createServer(async(req,res)=>{
    try{
      const pathname=new URL(req.url,'http://localhost').pathname;
      if(pathname.startsWith('/api/')){
        let text='';for await(const chunk of req)text+=chunk;
        const fields=Object.fromEntries(new URLSearchParams(text));requests.push({pathname,fields});
        if(pathname==='/api/language')language=fields.language;
        const body=pathname==='/api/setup'?configuration:pathname==='/api/rankings'?rankings:
          pathname==='/api/start'?{...reply,view:scoreProjection(Number(fields.players),language)}:reply;
        res.setHeader('Content-Type','application/json');res.end(JSON.stringify(body));return;
      }
      const file=pathname==='/'?'index.html':pathname.slice(1);
      if(file.includes('/')||file.includes('..')){res.writeHead(404).end();return;}
      const bytes=await fs.readFile(path.join(web,file));
      res.setHeader('Content-Type',file.endsWith('.mjs')?'text/javascript':file.endsWith('.css')?'text/css':file.endsWith('.html')?'text/html':'application/octet-stream');res.end(bytes);
    }catch{res.writeHead(404).end();}
  });
  await new Promise(resolve=>server.listen(0,'127.0.0.1',resolve));
  t.after(()=>new Promise(resolve=>server.close(resolve)));
  return {work,url:'http://127.0.0.1:'+server.address().port+'/',requests};
}
