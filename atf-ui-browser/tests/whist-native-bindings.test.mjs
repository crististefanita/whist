import assert from 'node:assert/strict';
import fs from 'node:fs';
import os from 'node:os';
import path from 'node:path';
import test from 'node:test';
import {fileURLToPath} from 'node:url';
import {runFixtureProcess} from '../../atf-tests/tools/fixture-process.mjs';
import {nativeObservationSources} from './native-observation-fixture.mjs';

const probe = `import android.view.*; import android.widget.TextView;
import ro.whistlab.app.observation.WhistSurfaceObservation; import ro.cardtable.testing.RenderedSurfaceAudit;
public class WhistBindingProbe {
 static int rejected;
 static void refuse(String code,Runnable run){try{run.run();throw new AssertionError("Missing refusal: "+code);}catch(IllegalStateException|AssertionError error){if(!error.getMessage().startsWith(code))throw error;rejected++;}}
 static <T extends View> T tagged(ViewGroup root,T view,String tag){view.root=root;view.setTag(tag);return view;}
 public static void main(String[] args){
  var root=new ViewGroup();
  for(String id:new String[]{"game-title","game-progress","game-bid-summary","trump-heading","trump-value"})root.children.add(tagged(root,new TextView(),id));
  root.children.add(tagged(root,new View(),"trump-indicator"));
  var hand=tagged(root,new ViewGroup(),"hand-card:AS");hand.children.add(tagged(root,new TextView(),"card-index"));root.children.add(hand);
  var played=tagged(root,new ViewGroup(),"trick-face:2");played.children.add(tagged(root,new TextView(),"card-index"));root.children.add(played);
  var observation=new WhistSurfaceObservation(root,"hand:1/PLAYING");var measured=observation.capture();RenderedSurfaceAudit.visible(measured,0);
  if(measured.regions().size()!=10||measured.regions().stream().filter(region->region.id().endsWith("/index")).count()!=2)throw new AssertionError("Missing mounted content");
  hand.children.clear();root.children.remove(hand);
  if(observation.capture().regions().size()!=8)throw new AssertionError("Detached hand card was retained");
  if(args[0].equals("negative")){
   android.os.Looper.wrong=true;refuse("NATIVE_OBSERVATION_WRONG_THREAD",observation::capture);android.os.Looper.wrong=false;
   var progress=root.children.remove(1);refuse("MISSING_WHIST_BINDING",observation::capture);root.children.add(progress);
   var duplicate=tagged(root,new TextView(),"game-title");root.children.add(duplicate);refuse("DUPLICATE_WHIST_BINDING",observation::capture);root.children.remove(duplicate);
   ((TextView)progress).layout.right=90;refuse("CLIPPED_TEXT",()->RenderedSurfaceAudit.visible(observation.capture(),0));((TextView)progress).layout.right=74;
   progress.duringRead=observation::dispose;refuse("STALE_RENDER_OWNER",observation::capture);progress.duringRead=()->{};
   int reads=progress.reads;refuse("STALE_RENDER_OWNER",observation::capture);if(reads!=progress.reads)throw new AssertionError("Disposed binding read native Views");
  }
  System.out.println("Whist mounted bindings: regions=10; current=8; rejected="+rejected);
 }
}`;

function run(t, mode) {
  const root=fileURLToPath(new URL('../../',import.meta.url));
  const work=fs.mkdtempSync(path.join(os.tmpdir(),'whist-native-bindings-'));
  t.after(()=>fs.rmSync(work,{recursive:true,force:true,maxRetries:10,retryDelay:100}));
  const controlled={...nativeObservationSources}; delete controlled['NativeObservationProbe.java'];
  controlled['android/view/View.java']=controlled['android/view/View.java'].replace('public View root=this;',
    'public Object tag; public Object getTag(){return tag;} public void setTag(Object value){tag=value;} public View root=this;');
  controlled['android/view/ViewGroup.java']='package android.view; public class ViewGroup extends View { public java.util.List<View> children=new java.util.ArrayList<>(); public int getChildCount(){return children.size();} public View getChildAt(int index){return children.get(index);} }';
  controlled['WhistBindingProbe.java']=probe;
  const sources=[];
  for(const [name,text] of Object.entries(controlled)){
    const file=path.join(work,name);fs.mkdirSync(path.dirname(file),{recursive:true});fs.writeFileSync(file,text);sources.push(file);
  }
  const core=path.join(root,'whist-lab-app/card-table-core');
  const closure=['src/main/java/ro/cardtable/ui/observation/RenderedSurface.java',
    'src/main/java/ro/cardtable/ui/observation/SurfaceEpoch.java',
    'src/testFixtures/java/ro/cardtable/testing/RenderedSurfaceAudit.java',
    'src/testFixtures/java/ro/cardtable/testing/SurfaceContinuity.java',
    'src/testFixtures/java/ro/cardtable/testing/SurfaceObservation.java'].map(name=>path.join(core,name));
  const adapter=path.join(root,'whist-lab-app/atf-card-table-ui/src/main/java/ro/cardtable/ui/observation/AndroidSurfaceObservation.java');
  const binding=path.join(root,'whist-lab-app/app/src/main/java/ro/whistlab/app/observation/WhistSurfaceObservation.java');
  const screen=fs.readFileSync(path.join(root,'whist-lab-app/app/src/main/java/ro/whistlab/app/screens/GameScreen.java'),'utf8');
  assert.equal(screen.match(/observe\(page, state\);/g)?.length,2,'Bidding and play install the mounted page owner');
  assert.match(screen,/observation\.dispose\(\)/,'Replacing or leaving the table invalidates the observation owner');
  for(const tag of ['game-progress','game-bid-summary'])assert.ok(screen.includes('setTag("'+tag+'")'));
  const trump=fs.readFileSync(path.join(root,'whist-lab-app/app/src/main/java/ro/whistlab/app/table/TrumpIndicatorView.java'),'utf8');
  for(const tag of ['trump-heading','trump-value'])assert.ok(trump.includes('setTag("'+tag+'")'));
  const argFile=path.join(work,'compile.args');
  fs.writeFileSync(argFile,['-proc:none','-encoding','UTF-8','--release','17','-d',work,...closure,adapter,binding,...sources]
    .map(value=>'"'+value.replaceAll('\\','/')+'"').join('\n'));
  runFixtureProcess('javac',['-J-Xmx128m','@'+argFile],{cwd:root,fixture:'Whist mounted binding compilation'});
  const output=runFixtureProcess('java',['-Xmx128m','-cp',work,'WhistBindingProbe',mode],{cwd:root,fixture:'Whist mounted bindings / '+mode});
  assert.ok(output.includes('regions=10; current=8; rejected='+(mode==='negative'?6:0)),output);
  console.log(output.trim()+'; boundary=production Whist binder with controlled Android View/Layout; screen composition guards; no glyph raster acceptance');
}

test('mounted Whist bindings observe current content without retaining detached cards',t=>run(t,'nominal'));
test('mounted Whist bindings reject missing duplicate clipped and disposed owners',t=>run(t,'negative'));
