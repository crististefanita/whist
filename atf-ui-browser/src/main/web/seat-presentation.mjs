/** Fit measured ink horizontally; the outer line keeps its typography and height across turns. */
export function fitSeatLine(line) {
  line.classList.add("seat-line");
  let ink = line.querySelector(":scope > .seat-line-ink");
  if (!ink) {
    ink = line.ownerDocument.createElement("span");
    ink.className = "seat-line-ink";
    ink.append(...line.childNodes);
    line.append(ink);
  }
  const available = line.clientWidth;
  if (available <= 0) return;
  const natural = Math.max(ink.scrollWidth, ink.offsetWidth);
  const scale = natural <= 0 ? 1 : Math.min(1, Math.max(0, available - 1) / natural);
  const transform=`scaleX(${scale})`;
  if(ink.style.transform!==transform)ink.style.transform=transform;
}

/** Update the native-style fact without replacing its fitted line or numeric ink. */
export function updateSeatFact(line,content){
  if(line.textContent===content&&line.querySelector(':scope > .seat-line-ink'))return;
  let ink=line.querySelector(':scope > .seat-line-ink');
  const separator=content.lastIndexOf(' ');
  const prefix=separator<0?content:content.slice(0,separator+1),value=content.slice(separator+1);
  if(ink&&separator>=0&&ink.childNodes.length===2&&ink.firstChild.nodeType===3&&ink.lastChild.nodeName==='B'){
    if(ink.firstChild.data!==prefix)ink.firstChild.data=prefix;
    const number=ink.lastChild;
    if(number.childNodes.length===1&&number.firstChild.nodeType===3){if(number.firstChild.data!==value)number.firstChild.data=value;}
    else number.textContent=value;
  }else{
    const target=ink??line;
    if(separator<0)target.textContent=content;
    else{const number=line.ownerDocument.createElement('b');number.textContent=value;target.replaceChildren(prefix,number);}
  }
  fitSeatLine(line);
}

export function fitSeatLines(seats) {
  for (const seat of seats) {
    for (const line of seat.querySelectorAll(":scope > strong, :scope > .seat-bid, :scope > .seat-won")) fitSeatLine(line);
  }
}

/** Measure the external action line in CSS layout units, including when its real badge is hidden. */
export function measureSeatActionClearance(seat, labels) {
  const badge = seat.querySelector(".seat-turn");
  if (!badge) return 0;
  const probe = badge.cloneNode(true);
  probe.hidden = false;
  probe.style.visibility = "hidden";
  seat.append(probe);
  try {
    let clearance=0;
    for(const label of labels??[badge.textContent||"M"]){
      probe.textContent=label;
      clearance=Math.max(clearance,-probe.offsetTop);
    }
    return clearance;
  }
  finally { probe.remove(); }
}

/** Measure intrinsic content, independently of the height assigned by the previous layout. */
export function measureNativeSeat(seat){
  const probe=seat.cloneNode(true);
  Object.assign(probe.style,{height:'auto',minHeight:'0',width:seat.offsetWidth+'px',visibility:'hidden',pointerEvents:'none',transform:'none',left:'0',top:'0'});
  seat.append(probe);
  try{
    const style=getComputedStyle(probe),name=probe.querySelector(':scope > strong');
    return {bodyHeight:Math.max(1,Math.round(probe.offsetHeight-parseFloat(style.borderTopWidth)-parseFloat(style.borderBottomWidth))),nameHeight:name.offsetHeight};
  }finally{probe.remove();}
}

/** Commit measured content without replacing the existing per-seat frame objects. */
export function retainSeatFrames(previous, next) {
  if (!previous || previous === next) return 0;
  const pairs=compatibleSeats(previous,next);
  if(!pairs)return 0;
  for(const [frame,measured] of pairs){
    synchronizeAttributes(frame,measured);
    synchronizeChildren(frame,measured);
    measured.replaceWith(frame);
  }
  return pairs.length;
}

/** Commit a measured view without unmounting a compatible visible board or its seats. */
export function commitTableView(stage,next){
  const previous=stage.querySelector(':scope > .match:not([data-preparing])');
  const pairs=previous&&previous!==next?compatibleSeats(previous,next):null;
  if(!pairs){stage.replaceChildren(next);return next;}
  const board=previous.querySelector(':scope > .board'),prepared=next.querySelector(':scope > .board');
  const center=board.querySelector(':scope > .center'),nextCenter=prepared.querySelector(':scope > .center');
  if(!center||!nextCenter){stage.replaceChildren(next);return next;}
  for(const [frame,measured] of pairs){synchronizeAttributes(frame,measured);synchronizeChildren(frame,measured);}
  synchronizeAttributes(board,prepared);
  synchronizeAttributes(center,nextCenter);
  center.replaceChildren(...nextCenter.childNodes);
  synchronizeAttributes(previous,next);
  replaceAroundBoard(previous,next,board,prepared);
  next.remove();
  return previous;
}

function replaceAroundBoard(current,next,board,prepared){
  let cursor=current.firstChild;
  for(const child of [...next.childNodes]){
    if(child===prepared){
      while(cursor&&cursor!==board){const after=cursor.nextSibling;cursor.remove();cursor=after;}
      cursor=board.nextSibling;
    }else if(cursor===board){current.insertBefore(child,cursor);}
    else if(cursor){const after=cursor.nextSibling;cursor.replaceWith(child);cursor=after;}
    else current.append(child);
  }
  while(cursor){const after=cursor.nextSibling;cursor.remove();cursor=after;}
}

function compatibleSeats(previous,next){
  const before = [...(previous?.querySelectorAll(":scope > .board > .seat") ?? [])];
  const after = [...next.querySelectorAll(":scope > .board > .seat")];
  if (!before.length || before.length !== after.length) return null;
  const keys = nodes => nodes.map(node => node.dataset.seat);
  const oldKeys = keys(before), newKeys = keys(after);
  if (oldKeys.some(key => !key) || new Set(oldKeys).size !== before.length
      || newKeys.some(key => !key) || new Set(newKeys).size !== after.length
      || newKeys.some(key => !oldKeys.includes(key))) return null;
  const frames = new Map(before.map(node => [node.dataset.seat, node]));
  return after.map(measured=>[frames.get(measured.dataset.seat),measured]);
}

function synchronizeAttributes(current,next){
  const wanted=new Map([...next.attributes].map(n=>[n.name,n.value]));
  const existing=new Map([...current.attributes].map(n=>[n.name,n.value]));
  for(const name of existing.keys())if(!wanted.has(name))current.removeAttribute(name);
  for(const [name,value] of wanted)if(existing.get(name)!==value)current.setAttribute(name,value);
}

function synchronizeChildren(current,next){
  const existing=[...current.childNodes],wanted=[...next.childNodes];
  if(existing.length!==wanted.length||existing.some((n,i)=>!n?.nodeType||n.nodeType!==wanted[i]?.nodeType||n.nodeName!==wanted[i].nodeName)){
    current.replaceChildren(...wanted);return;
  }
  for(const [i,node] of existing.entries()){
    if(node.nodeType===3){if(node.data!==wanted[i].data)node.data=wanted[i].data;}
    else if(node.nodeType===1){synchronizeAttributes(node,wanted[i]);synchronizeChildren(node,wanted[i]);}
  }
}
