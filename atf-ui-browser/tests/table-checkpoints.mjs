/** Convert actual adjacent render states into ordered comparisons, without inventing driver actions. */
import {tableContinuity} from './table-continuity.mjs';
export function tableCheckpoints(samples,url,manual=[]){
 if(!Array.isArray(samples)||!samples.length)throw new TypeError('A nonempty observed trace is required');
 const delta=(a,b)=>{if(!Array.isArray(a)||!Array.isArray(b)||a.length!==4||b.length!==4||![...a,...b].every(Number.isFinite))throw new TypeError('Four finite rectangle coordinates required');return Math.max(...a.map((n,i)=>Math.abs(n-b[i])));};
 const equal=(name,expected,observed,passed)=>({name,expected,observed,passed});
 const steps=[],hands=new Set(),removals=new Map();
 tableContinuity(samples,removal=>removals.set(removal.hand+'|'+removal.key,removal));
 const add=(name,action,before,after,source='OBSERVE',images=[])=>{
  if(before.seats.length!==after.seats.length)throw new TypeError('Seat population changed');
  const assertions=before.seats.map((seat,i)=>equal('Seat '+seat.seat+' frame: x, y, width, height',seat.rectangle,after.seats[i].rectangle,
   seat.seat===after.seats[i].seat&&delta(seat.rectangle,after.seats[i].rectangle)<.02));
  let rows=0;for(let i=0;i<before.seats.length;i++){
   const a=before.seats[i].facts,b=after.seats[i].facts;if(!a||!b||a.length!==b.length)throw new TypeError('Seat fact rows are incomplete');
   a.forEach((fact,j)=>{rows=Math.max(rows,delta(fact.rectangle,b[j].rectangle));});
  }
  assertions.push(equal('Name, bid and won-trick rows stay fixed','< 0.02 CSS px',rows,rows<.02));
  for(const key of ['header','.board','.hand'])assertions.push(equal(key+' reserved bounds',before.regions[key],after.regions[key],delta(before.regions[key],after.regions[key])<.02));
  const common=before.played.filter(card=>after.played.some(n=>n.key===card.key));
  const placed=common.reduce((max,card)=>Math.max(max,delta(card.rectangle,after.played.find(n=>n.key===card.key).rectangle)),0);
  assertions.push(equal('Previously deposited faces keep their bounds','< 0.02 CSS px',placed,placed<.02));
  const unchanged=before.handFaces.map(n=>n.face).join('|')===after.handFaces.map(n=>n.face).join('|');
  if(unchanged){
   let hand=0;before.handFaces.forEach((card,i)=>{if(!card.lifting&&!after.handFaces[i].lifting)hand=Math.max(hand,delta(card.rectangle,after.handFaces[i].rectangle));});
   assertions.push(equal('Unchanged private faces stay fixed during AI turns','< 0.02 CSS px',hand,hand<.02));
  }else assertions.push(equal('A committed human removal may recenter the remaining fan',before.handFaces.length-1,after.handFaces.length,after.handFaces.length===before.handFaces.length-1));
  const id='TABLE-STEP-'+String(steps.length+1).padStart(3,'0');
  if(!Number.isFinite(before.at)||!Number.isFinite(after.at)||after.at<before.at)throw new TypeError('An ordered observed time interval is required');
  steps.push({id,name:after.hand+' · '+name,keyword:source==='ACTION'?'WHEN':hands.has(after.hand)?'THEN':'GIVEN',action,url,source,
   durationMillis:Math.round(after.at-before.at),before,after,assertions,images,
   status:assertions.every(row=>row.passed)?'PASS':'FAIL'});
 };
 for(let i=0;i<samples.length;i++){
  const after=samples[i],before=samples[i-1];
  if(!hands.has(after.hand)){add('Bidding table is ready','Observe the new hand',after,after);hands.add(after.hand);continue;}
  if(before.hand!==after.hand)continue;
  const added=after.played.filter(card=>!before.played.some(n=>n.key===card.key));
  if(added.length>1)throw new TypeError('A before/after pair missed an individual card deposit');
  if(added.length){
   const card=added[0],command=manual.find(m=>m.hand===after.hand&&m.key===card.key),removal=removals.get(after.hand+'|'+card.key);
   add('Deposit '+card.key,command?'Click the legal private card '+card.key:'Observe seat/card '+card.key,command?.before??removal?.before??before,command?.after??after,command?'ACTION':'OBSERVE',command?.images??[]);
  }
  const committed=[...removals.values()].find(removal=>removal.after===after);
  if(!before.collecting&&after.collecting)add('Collection starts','Observe collection overlay',committed?.before??before,after);
  if(before.collecting&&!after.collecting)add('Collection completes','Observe the next trick',before,after);
  if(before.actor!==after.actor&&after.actor==='0')add('The human turn becomes available','Observe the actor handoff',before,after);
 }
 return steps;
}
