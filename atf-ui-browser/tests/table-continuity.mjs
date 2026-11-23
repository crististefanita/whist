/** Trace oracle: accepted human removal may recenter the fan; ordinary AI turns may not. */
export function tableContinuity(samples,onRemoval=()=>{}){
 if(!Array.isArray(samples)||!samples.length)throw new TypeError('A nonempty table trace is required');
 if(typeof onRemoval!=='function')throw new TypeError('The removal observer must be a function');
 const first=samples[0],placed=new Map(),hands=new Map();let maxFrameDelta=0,maxPlacedDelta=0,maxHandDelta=0,acceptedRemovals=0;
 const delta=(a,b)=>{if(a.length!==4||b.length!==4||![...a,...b].every(Number.isFinite))throw new TypeError('Four finite rectangle coordinates are required');return Math.max(...a.map((n,i)=>Math.abs(n-b[i])));};
 for(const [index,sample] of samples.entries()){
  if(sample.seats.length!==first.seats.length)throw new TypeError('A match trace cannot change its seat population');
  sample.seats.forEach((seat,i)=>{maxFrameDelta=Math.max(maxFrameDelta,delta(seat.rectangle,first.seats[i].rectangle));});
  for(const key of Object.keys(first.regions))maxFrameDelta=Math.max(maxFrameDelta,delta(sample.regions[key],first.regions[key]));
  for(const card of sample.played){const key=sample.hand+'/'+card.key,old=placed.get(key);if(old)maxPlacedDelta=Math.max(maxPlacedDelta,delta(card.rectangle,old));else placed.set(key,card.rectangle);}
  let hand=hands.get(sample.hand);if(!hand){hand={baseline:sample.handFaces};hands.set(sample.hand,hand);}
  const before=hand.baseline.map(n=>n.face).join('|'),now=sample.handFaces.map(n=>n.face).join('|');
  if(before!==now){
   hand.beforeRemoval??=samples[index-1]??sample;
   hand.removed??=hand.baseline.find(n=>!sample.handFaces.some(card=>card.face===n.face))?.face;
   if(!hand.removed)throw new TypeError('Unexplained hand composition change');
   if(sample.played.some(card=>card.key==='0/'+hand.removed)){
    onRemoval({hand:sample.hand,key:'0/'+hand.removed,before:hand.beforeRemoval,after:sample});
    hand.baseline=sample.handFaces;hand.removed=undefined;hand.beforeRemoval=undefined;acceptedRemovals++;
   }
  }else sample.handFaces.forEach((card,i)=>{if(!card.lifting&&!hand.baseline[i].lifting)maxHandDelta=Math.max(maxHandDelta,delta(card.rectangle,hand.baseline[i].rectangle));});
 }
 if([...hands.values()].some(hand=>hand.removed))throw new TypeError('An accepted removal lacks its deposited human face');
 return {maxFrameDelta,maxPlacedDelta,maxHandDelta,acceptedRemovals};
}
