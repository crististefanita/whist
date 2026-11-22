import {adaptiveViewport} from './viewport.mjs?v=0.4';

/** Read-only collections use pages; interactive forms keep all their fields together. */
export function boundedPages(host, groups) {
  const nodes=[...host.children],flat=groups.flat();
  if (!groups.length||groups.some(page=>!page.length)||flat.length!==nodes.length
      ||new Set(flat).size!==nodes.length||flat.some(node=>!nodes.includes(node)))
    throw new TypeError('A page must preserve every content node exactly once');
  const en=document.documentElement.lang==='en';
  const nav=document.createElement('nav');nav.className='surface-pages';
  nav.setAttribute('aria-label',en?'Content pages':'Paginile conținutului');
  const previous=document.createElement('button'),next=document.createElement('button'),position=document.createElement('output');
  previous.type=next.type='button';previous.textContent=en?'Previous':'Înapoi';next.textContent=en?'Next':'Înainte';
  position.setAttribute('aria-live','polite');nav.append(previous,position,next);
  let current=0;
  const show=index=>{
    current=index;const active=new Set(groups[current]);
    for(const node of host.children)node.hidden=!active.has(node);
    previous.disabled=current===0;next.disabled=current===groups.length-1;
    position.textContent=`${current+1} / ${groups.length}`;
    host.dataset.page=String(current+1);
    if(host.matches('.ranking-list'))host.style.counterReset='rank '+groups.slice(0,current).flat().length;
  };
  previous.onclick=()=>show(current-1);next.onclick=()=>show(current+1);
  host.classList.add('surface-paged');host.after(nav);nav.hidden=groups.length<2;show(0);
  return {nav,nodes:[...host.children]};
}

/** Fits the delivered DOM; history is deliberately outside the non-scrolling modal contract. */
export function surfaceFitter(stage,profileDialog,resize) {
  let requested=0,lastRoot=stage.firstElementChild;
  const pages=new WeakMap();
  const grouped=host=>{
    const nodes=[...host.children],groups=[];
    if(nodes.some(node=>node.matches('h3'))){
      for(const node of nodes){if(node.matches('h3')||!groups.length)groups.push([]);groups.at(-1).push(node);}
    }else{
      const size=host.matches('.ranking-list,.goals')?5:1;
      for(let i=0;i<nodes.length;i+=size)groups.push(nodes.slice(i,i+size));
    }
    return groups;
  };
  function paginate(host){
    const old=pages.get(host),nodes=[...host.children];
    if(!nodes.length||old&&old.nodes.length===nodes.length&&old.nodes.every((node,i)=>node===nodes[i]))return;
    old?.nav.remove();pages.set(host,boundedPages(host,grouped(host)));
  }
  function prepare(){
    const help=stage.querySelector('.whist-help');
    if(help&&!help.querySelector('.surface-reader')){
      const rules=[...help.querySelectorAll(':scope > .whist-rule')];
      if(rules.length){const body=document.createElement('div');body.className='surface-reader';rules[0].before(body);body.append(...rules);}
    }
    for(const host of stage.querySelectorAll('.surface-reader'))paginate(host);
    if(profileDialog.open)for(const host of profileDialog.querySelectorAll('.goals,.ranking-list,.whist-dialog-copy'))paginate(host);
  }
  function fitDialog(width,height){
    if(!profileDialog.open)return;
    const apply=result=>{
      profileDialog.style.setProperty('--modal-width',result.width*.94+'px');
      profileDialog.style.setProperty('--modal-height',result.height+'px');
      profileDialog.style.setProperty('--modal-scale',result.scale);
    };
    let needed=height;
    for(let pass=0;pass<4;pass++){
      apply(adaptiveViewport(width,height,needed));
      profileDialog.style.height='auto';
      const natural=profileDialog.scrollHeight+2;
      profileDialog.style.removeProperty('height');
      if(natural<=needed)break;
      needed=natural;
    }
    apply(adaptiveViewport(width,height,needed));
  }
  function schedule(){
    if(requested)return;
    requested=requestAnimationFrame(()=>{
      requested=0;
      if(document.documentElement.dataset.surface!=='whist'||stage.querySelector(':scope > [data-preparing]'))return;
      prepare();resize();
    });
  }
  const contentSize=new ResizeObserver(()=>{
    if(stage.scrollHeight>stage.clientHeight+1||profileDialog.open&&profileDialog.scrollHeight>profileDialog.clientHeight+1)schedule();
  });
  const mutations=new MutationObserver(records=>{
    const rootChanged=lastRoot!==stage.firstElementChild;
    const modalChanged=records.some(record=>record.target===profileDialog||profileDialog.contains(record.target));
    if(!rootChanged&&!modalChanged)return;
    lastRoot=stage.firstElementChild;
    contentSize.disconnect();if(stage.firstElementChild)contentSize.observe(stage.firstElementChild);
    if(profileDialog.open)contentSize.observe(profileDialog);schedule();
  });
  mutations.observe(stage,{childList:true});
  mutations.observe(profileDialog,{subtree:true,childList:true,characterData:true,attributes:true,attributeFilter:['open']});
  stage.addEventListener('toggle',schedule,true);
  const changed=event=>{if(event.target.closest('.setup,.whist-home,.whist-help'))schedule();};
  stage.addEventListener('change',changed,true);
  return {fitDialog,schedule,dispose(){
    cancelAnimationFrame(requested);mutations.disconnect();contentSize.disconnect();
    stage.removeEventListener('toggle',schedule,true);stage.removeEventListener('change',changed,true);
  }};
}
