import assert from 'node:assert/strict';
import fs from 'node:fs';
import path from 'node:path';
import os from 'node:os';
import test from 'node:test';
import {fileURLToPath} from 'node:url';
import {runFixtureProcess} from '../../atf-tests/tools/fixture-process.mjs';

const probe=`import android.os.Bundle;
import ro.whistlab.app.achievements.presentation.WhistAchievementScreenOwner;
public class AchievementOwnerProbe {
 static void check(boolean valid,String message){if(!valid)throw new AssertionError(message);}
 static Bundle inspect(WhistAchievementScreenOwner owner){var saved=new Bundle();owner.state().save(saved);return saved;}
 public static void main(String[] args){
  int roundTrips=0,refusals=0;
  for(String language:new String[]{"RO","EN"}){
   var owner=new WhistAchievementScreenOwner();var row=new Bundle();row.putInt("achievementPosition",48);row.putString("achievementSelected","exact_small_rate");owner.state().restore(row);
   var activity=new Bundle();activity.putString("unrelatedGameState","unchanged");owner.save(activity,true);
   row.putInt("achievementPosition",1);row.putString("achievementSelected",null);owner.state().restore(row);
   var recreated=new WhistAchievementScreenOwner();recreated.restore(activity);var observed=inspect(recreated);
   check(recreated.reopen(),"Visible achievement route was lost");
   check(observed.getInt("achievementPosition")==48&&"exact_small_rate".equals(observed.getString("achievementSelected")),"Recreation lost the saved row or stable goal ID");
   check("unchanged".equals(activity.getString("unrelatedGameState")),"Navigation snapshot rewrote game state");
   roundTrips++;
   if(args[0].equals("negative")){
    var home=new Bundle();recreated.save(home,false);var next=new WhistAchievementScreenOwner();next.restore(home);check(!next.reopen(),"A hidden route reopened on recreation");refusals++;
    var absent=new WhistAchievementScreenOwner();absent.restore(null);check(!absent.reopen()&&inspect(absent).getInt("achievementPosition")==0,"Missing checkpoint fabricated navigation");refusals++;
    var negative=new Bundle();negative.putInt("achievementPosition",-20);next.state().restore(negative);check(inspect(next).getInt("achievementPosition")==0,"Shared screen failed to normalize an invalid scroll position");refusals++;
   }
  }
  System.out.println("Achievement owner observations: roundTrips="+roundTrips+"; refusals="+refusals+"; stableGoal=exact_small_rate; row=48");
 }
}`;

function run(t,mode){
  const root=fileURLToPath(new URL('../../',import.meta.url));
  const sdk=process.env.ANDROID_SDK_ROOT||process.env.ANDROID_HOME;
  assert.ok(sdk,'An explicit Android SDK is required for the real shared-screen API compilation; no APK or emulator is used');
  const android=path.join(sdk,'platforms/android-36/android.jar');assert.ok(fs.existsSync(android),'Android API 36 is required');
  const main=fs.readFileSync(path.join(root,'whist-lab-app/app/src/main/java/ro/whistlab/app/MainActivity.java'),'utf8');
  assert.match(main,/achievementScreen\.restore\(state\)/);
  assert.match(main,/achievementScreen\.save\(state, navigation\.screen\(\) == AppNavigation\.Screen\.ACHIEVEMENTS\)/);
  assert.match(main,/if \(achievementScreen\.reopen\(\)\) showAchievements\(\)/);
  assert.match(main,/case ACHIEVEMENTS -> showAchievements\(\)/);
  assert.ok(!main.includes('new ro.cardtable.ui.achievements.AchievementScreen.State()'),'Every opening reuses its activity-owned state');
  const sources={'AchievementOwnerProbe.java':probe,
    'android/os/Bundle.java':`package android.os; public class Bundle {
      private final java.util.Map<String,Object> values=new java.util.HashMap<>();
      public void putInt(String key,int value){values.put(key,value);} public int getInt(String key){return values.get(key) instanceof Integer value?value:0;}
      public void putString(String key,String value){values.put(key,value);} public String getString(String key){return (String)values.get(key);}
      public void putBoolean(String key,boolean value){values.put(key,value);} public boolean getBoolean(String key,boolean fallback){return values.get(key) instanceof Boolean value?value:fallback;}
      public void putBundle(String key,Bundle value){values.put(key,value);} public Bundle getBundle(String key){return (Bundle)values.get(key);}
    }`};
  const work=fs.mkdtempSync(path.join(os.tmpdir(),'achievement-owner-'));
  t.after(()=>fs.rmSync(work,{recursive:true,force:true,maxRetries:10,retryDelay:100}));
  const generated=[];for(const [name,text] of Object.entries(sources)){const file=path.join(work,name);fs.mkdirSync(path.dirname(file),{recursive:true});fs.writeFileSync(file,text);generated.push(file);}
  const files=['whist-lab-app/app/src/main/java/ro/whistlab/app/achievements/presentation/WhistAchievementScreenOwner.java',
    'whist-lab-app/atf-card-table-ui/src/main/java/ro/cardtable/ui/achievements/AchievementScreen.java'].map(name=>path.join(root,name));
  const roots=['whist-lab-app/atf-card-table-ui/src/main/java','whist-lab-app/card-table-core/src/main/java','whist-lab-app/card-table-names/src/main/java'].map(name=>path.join(root,name));
  const args=path.join(work,'compile.args');
  fs.writeFileSync(args,['-proc:none','-encoding','UTF-8','--release','17','-d',work,'-classpath',android,'-sourcepath',roots.join(path.delimiter),...files,...generated]
    .map(value=>'"'+value.replaceAll('\\','/')+'"').join('\n'));
  runFixtureProcess('javac',['-J-Xmx256m','@'+args],{cwd:root,fixture:'Shared achievement state SDK API compilation'});
  const output=runFixtureProcess('java',['-Xmx128m','-cp',[work,android].join(path.delimiter),'AchievementOwnerProbe',mode],{cwd:root,fixture:'Achievement activity owner'});
  assert.ok(output.includes('roundTrips=2; refusals='+(mode==='negative'?6:0)),output);
  console.log(output.trim()+'; boundary=actual Whist owner and shared State codec with controlled Bundle; source composition guards; no native recreation, scroll pixels or translated glyph acceptance');
}
test('achievement route restores the shared scroll and stable goal across recreation',t=>run(t,'nominal'));
test('achievement route refuses hidden or absent restoration and stale snapshot mutation',t=>run(t,'negative'));
