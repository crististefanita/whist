import assert from 'node:assert/strict';
import test from 'node:test';
import path from 'node:path';
import {rendererFixture} from './responsive-fixture.mjs';
import {withBrowser,until} from './fixtures.mjs';

test('native-style seat rows retain their DOM and supplied rectangles through actor and fact updates',async t=>{
  const {work,url}=await rendererFixture(t);
  await withBrowser(process.env.ATF_BROWSER_EXECUTABLE??'C:/Program Files (x86)/Microsoft/Edge/Application/msedge.exe',path.join(work,'profile'),async({send,evaluate})=>{
    await send('Page.enable');await send('Page.navigate',{url});
    await until(()=>evaluate('!!document.querySelector(".whist-home .primary")'));await evaluate('document.fonts.ready');
    const observed=await evaluate(`(async()=>{
      const {commitTableView,updateSeatFact,fitSeatLines}=await import('./seat-presentation.mjs');
      const observations=[];let negativeRejected=false;
      const box=n=>{const r=n.getBoundingClientRect();return [r.left,r.top,r.width,r.height];};
      for(const players of [3,4,5,6])for(const width of [280,393,560])for(const font of [16,20.8])for(const language of ['ro','en']){
        document.documentElement.style.fontSize=font+'px';
        const host=document.createElement('section');host.className='whist-match compact-table';
        host.style.cssText='position:fixed;top:0;left:0;visibility:hidden;width:'+width+'px;height:320px';document.body.append(host);
        const make=step=>{
          const view=document.createElement('section'),board=document.createElement('div');view.className='match whist-match';board.className='board';
          board.style.cssText='position:relative;width:'+width+'px;height:320px';view.append(board);host.append(view);
          for(let seat=0;seat<players;seat++){
            const n=document.createElement('div');n.className='seat'+(seat===step%players?' active':'');n.dataset.seat=String(seat);
            n.style.cssText='left:'+(seat%3*width/3)+'px;top:'+(Math.floor(seat/3)*150)+'px;width:'+Math.floor((width-16)/3)+'px;height:80px;transform:none';
            const name=document.createElement('strong');name.textContent=seat===0?(language==='ro'?'Tu':'You'):'Player '+seat;
            const bid=document.createElement('span'),won=document.createElement('span');bid.className='seat-bid';won.className='seat-won';
            bid.textContent=(language==='ro'?'Licitat: ':'Bid: ')+(step?1:'—');won.textContent=(language==='ro'?'Levate: ':'Won: ')+step;
            n.append(name,bid,won);board.append(n);updateSeatFact(bid,bid.textContent);updateSeatFact(won,won.textContent);
            if(seat===step%players)n.setAttribute('aria-current','true');
          }
          fitSeatLines(board.children);const center=document.createElement('div');center.className='center';board.append(center);return view;
        };
        let current=make(0),frames=[...current.querySelectorAll('.seat')];
        const original=current,board=current.querySelector('.board');
        const refs=frames.map(n=>({frame:n,parent:n.parentNode,rows:[...n.children],ink:[...n.querySelectorAll('.seat-line-ink')],numbers:[...n.querySelectorAll('b')],rectangle:box(n)}));
        let styleChanges=0,unmounts=0;const observer=new MutationObserver(records=>{
          styleChanges+=records.filter(r=>r.attributeName==='style').length;
          for(const r of records)for(const n of r.removedNodes)if(n===original||n===board||refs.some(ref=>ref.frame===n))unmounts++;
        });
        frames.forEach(n=>observer.observe(n,{attributes:true}));
        observer.observe(host,{childList:true});observer.observe(original,{childList:true});observer.observe(board,{childList:true});
        const verify=()=>{
          const actual=[...current.querySelectorAll('.seat')];
          return current===original&&current.querySelector('.board')===board&&actual.every((n,i)=>n===refs[i].frame&&n.parentNode===refs[i].parent&&[...n.children].every((r,j)=>r===refs[i].rows[j])&&
            [...n.querySelectorAll('.seat-line-ink')].every((r,j)=>r===refs[i].ink[j])&&
            [...n.querySelectorAll('b')].every((r,j)=>r===refs[i].numbers[j])&&JSON.stringify(box(n))===JSON.stringify(refs[i].rectangle));
        };
        for(const step of [1,2,3]){
          for(const n of frames)updateSeatFact(n.querySelector('.seat-won'),(language==='ro'?'Levate: ':'Won: ')+step);
          const next=make(step),wanted=[...next.querySelectorAll('.seat')].map(n=>n.textContent);
          current=commitTableView(host,next);frames=[...current.querySelectorAll('.seat')];
          if(!verify())throw Error('An unchanged seat rectangle or fitted row was replaced');
          if(JSON.stringify(frames.map(n=>n.textContent))!==JSON.stringify(wanted))throw Error('Retained text became stale');
          if(frames.some((n,i)=>n.hasAttribute('aria-current')!==(i===step%players)))throw Error('Retained actor accessibility became stale');
          await Promise.resolve();
        }
        if(styleChanges||unmounts)throw Error('Unchanged frame styles were rewritten or the visible board was unmounted');
        // The oracle must detect a fresh row even when its text and rectangle are identical.
        const row=frames[0].firstChild;row.replaceWith(row.cloneNode(true));negativeRejected ||= !verify();
        observer.disconnect();host.remove();observations.push({players,width,font,language,transitions:3,frameStyleRewrites:styleChanges,visibleUnmounts:unmounts,retainedRows:true});
      }
      let fallbacks=0;
      for(const defect of ['no-visible-table','different-seats','missing-center']){
        const stage=document.createElement('main');document.body.append(stage);
        const make=count=>{const view=document.createElement('section');view.className='match';const board=document.createElement('div');board.className='board';view.append(board);for(let i=0;i<count;i++){const seat=document.createElement('div');seat.className='seat';seat.dataset.seat=String(i);board.append(seat);}const center=document.createElement('div');center.className='center';board.append(center);return view;};
        const old=make(3),next=make(defect==='different-seats'?4:3);if(defect!=='no-visible-table')stage.append(old);
        if(defect==='missing-center')next.querySelector('.center').remove();
        if(commitTableView(stage,next)!==next||stage.firstElementChild!==next||old.isConnected)throw Error('An incompatible scene must mount its fresh table');
        stage.remove();fallbacks++;
      }
      return {observations,negativeRejected,fallbacks,boundary:'Real DOM/CSS and production seat adapter with supplied stable rectangles; game legality and native raster are separate contracts'};
    })()`);
    assert.equal(observed.observations.length,48);assert.equal(observed.negativeRejected,true);assert.equal(observed.fallbacks,3);
    console.log('Seat paint observations: '+JSON.stringify(observed));
  });
});
