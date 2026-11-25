import assert from 'node:assert/strict';
import {createHash} from 'node:crypto';
import fs from 'node:fs';
import os from 'node:os';
import path from 'node:path';
import {fileURLToPath} from 'node:url';
import test from 'node:test';
import {runFixtureProcess} from '../../atf-tests/tools/fixture-process.mjs';

const source='whist-lab-app/game-core/src/main/java/ro/whistlab/core/ai/engine/inference/BidSampleEvidence.java';
const probe=`import java.util.*; import java.math.*;
import ro.whistlab.core.ai.engine.inference.BidSampleEvidence;
import ro.whistlab.core.rules.GameRules; import ro.whistlab.core.rules.GameMode;
public class BidEvidenceProbe {
 static int refusals;
 static void check(boolean valid,String message){if(!valid)throw new AssertionError(message);}
 static void near(double expected,double actual){check(Math.abs(expected-actual)<1e-12,"Independent weighted mean mismatch: "+expected+" / "+actual);}
 static void rejected(BidSampleEvidence state,Runnable invalid){
  var before=state.options();int count=state.samples();double ess=state.effectiveSamples();int best=state.best();boolean enough=state.enough();
  try{invalid.run();throw new AssertionError("Invalid row accepted");}catch(IllegalArgumentException expected){}
  check(before.equals(state.options()),"Rejected row changed accumulated options");
  check(count==state.samples()&&ess==state.effectiveSamples()&&best==state.best()&&enough==state.enough(),"Rejected row changed evidence metadata");
  refusals++;
 }
 public static void main(String[] args){
  if(args[0].equals("negative")){
   var state=new BidSampleEvidence(List.of(0,1),2,true);state.add(new double[]{5,-1},new double[]{1,0},2);
   rejected(state,()->state.add(new double[]{100,Double.NaN},new double[]{0,0},1));
   for(double weight:new double[]{0,-1,Double.NaN,Double.POSITIVE_INFINITY,Double.MAX_VALUE,Double.MIN_VALUE})
    rejected(state,()->state.add(new double[]{5,-1},new double[]{1,0},weight));
   rejected(state,()->state.add(new double[]{1,2},new double[]{0,1.1},1));
   rejected(state,()->state.add(new double[]{1,2},new double[]{0,-.1},1));
   rejected(state,()->state.add(new double[]{1},new double[]{0,1},1));
   rejected(state,()->state.add(null,new double[]{0,1},1));
   rejected(state,()->state.add(new double[]{1,2},null,1));
   rejected(state,()->state.add(new double[]{Double.MAX_VALUE,2},new double[]{0,1},4));
   var points=new BidSampleEvidence(List.of(0,1),2,false);points.add(new double[]{1,2},new double[]{0,1});
   rejected(points,()->points.add(new double[]{1e200,-1e200},new double[]{0,1}));
   int domains=0;for(var bids:List.of(List.of(0,0),List.of(-1),List.of(2)))
    try{new BidSampleEvidence(bids,2,true);throw new AssertionError("Invalid bid domain accepted");}catch(IllegalArgumentException expected){domains++;}
   var fresh=new BidSampleEvidence(List.of(0,1),2,true);fresh.add(new double[]{5,-1},new double[]{1,0},2);
   state.add(new double[]{-1,6},new double[]{0,1},3);fresh.add(new double[]{-1,6},new double[]{0,1},3);
   check(state.options().equals(fresh.options()),"Valid follow-up differs after rejected rows");
   System.out.println("Bid evidence rejection: rows="+refusals+"; domains="+domains+"; continuation=unchanged");return;
  }
  int contexts=0,scales=0;double maxError=0;
  for(int size=2;size<=8;size++)for(int seed=0;seed<16;seed++)for(boolean safety:new boolean[]{false,true}){
   var bids=new ArrayList<Integer>();for(int bid=0;bid<=size;bid++)bids.add(bid);
   var random=new Random(0x511eedL+size*1009L+seed);int[] taken=new int[20],weights=new int[20];long total=0,squares=0;
   for(int index=0;index<20;index++){taken[index]=random.nextInt(size+1);weights[index]=1+random.nextInt(5);total+=weights[index];squares+=weights[index]*weights[index];}
   for(double scale:new double[]{.25,1,4}){
    var state=new BidSampleEvidence(bids,size+1,safety);
    for(int index=0;index<20;index++){double[] scores=new double[size+1],exact=new double[size+1];
     for(int bid:bids){exact[bid]=taken[index]==bid?1:0;scores[bid]=GameRules.scoreDelta(taken[index],bid,GameMode.ROMANIAN_WHIST);}
     state.add(scores,exact,weights[index]*scale);
    }
    for(var actual:state.options()){
     long exactWeight=0,pointWeight=0;for(int index=0;index<20;index++){
      if(taken[index]==actual.bid())exactWeight+=weights[index];
      pointWeight+=(taken[index]==actual.bid()?5+taken[index]:-Math.abs(taken[index]-actual.bid()))*weights[index];
     }
     double expected=new BigDecimal(exactWeight).divide(new BigDecimal(total),MathContext.DECIMAL128).doubleValue();
     near(expected,actual.exactContractProbability());maxError=Math.max(maxError,Math.abs(expected-actual.exactContractProbability()));
     near(new BigDecimal(pointWeight).divide(new BigDecimal(total),MathContext.DECIMAL128).doubleValue(),actual.expectedPoints());
     check(actual.samples()==20,"Raw count is not effective sample size");
    }
    near((double)total*total/squares,state.effectiveSamples());scales++;
   }contexts++;
  }
  System.out.println("Bid evidence finite-model oracle: contexts="+contexts+"; scales="+scales+"; maxFrequencyError="+maxError);
 }
}`;

function fixture(t,baseline) {
  const root=fileURLToPath(new URL('../../',import.meta.url));
  const work=fs.mkdtempSync(path.join(os.tmpdir(),'bid-evidence-'));
  t.after(()=>fs.rmSync(work,{recursive:true,force:true,maxRetries:10,retryDelay:100}));
  const sources={'BidEvidenceProbe.java':probe};
  if(baseline)sources['ro/whistlab/core/ai/engine/inference/BidSampleEvidence.java']=baseline;
  const files=[];for(const [name,text] of Object.entries(sources)){const file=path.join(work,name);fs.mkdirSync(path.dirname(file),{recursive:true});fs.writeFileSync(file,text);files.push(file);}
  const args=path.join(work,'compile.args');
  fs.writeFileSync(args,['-proc:none','-encoding','UTF-8','--release','17','-d',work,'-sourcepath',path.join(root,'whist-lab-app/game-core/src/main/java'),
    ...(baseline?[]:[path.join(root,source)]),...files].map(value=>'"'+value.replaceAll('\\','/')+'"').join('\n'));
  runFixtureProcess('javac',['-J-Xmx256m','@'+args],{cwd:root,fixture:'Weighted evidence source compilation'});
  return {root,run:(...values)=>runFixtureProcess('java',['-Xmx128m','-cp',work,...values],{cwd:root,fixture:'Weighted evidence oracle'})};
}

test('finite weighted contract frequencies stay distinct from points for two to eight cards',t=>{
  const actual=fixture(t).run('BidEvidenceProbe','nominal');
  assert.ok(actual.includes('contexts=224; scales=672'),actual);
  console.log(actual.trim()+'; distribution=declared finite teaching population; not posterior calibration or new match-strength evidence');
});
test('invalid rows are atomic and a nontransactional mutant is caught by the same oracle',t=>{
  const current=fixture(t),actual=current.run('BidEvidenceProbe','negative');
  assert.ok(actual.includes('rows=14; domains=3; continuation=unchanged'),actual);
  const original=fs.readFileSync(path.join(current.root,source),'utf8');
  const mutant=original.replace('validate(scores, exact, weight);','if (!Double.isFinite(weight) || weight <= 0) throw new IllegalArgumentException("Invalid sample weight");')
    .replace('double value = safety ? exact[bid] : scores[bid];','if (!Double.isFinite(scores[bid]) || !Double.isFinite(exact[bid])) throw new IllegalArgumentException("Invalid sampled utility");\n            double value = safety ? exact[bid] : scores[bid];');
  assert.notEqual(mutant,original,'The controlled mutant restores the original partial-mutation defect');
  const previous=fixture(t,mutant);let refusal;
  try{previous.run('BidEvidenceProbe','negative');}catch(error){refusal=error;}
  assert.ok(refusal&&refusal.code==='FIXTURE-CHILD-EXIT'&&refusal.status===1
    &&String(refusal.stderr).includes('Rejected row changed accumulated options'),refusal?.message);
  console.log(actual.trim()+'; controlledMutantSha256='+createHash('sha256').update(mutant).digest('hex')+'; original refusal='+refusal.stderr);
});
