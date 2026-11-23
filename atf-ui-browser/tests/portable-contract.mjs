import fs from 'node:fs';
import os from 'node:os';
import path from 'node:path';
import {execFileSync} from 'node:child_process';
import {fileURLToPath} from 'node:url';

/** Compile the same portable source closure, without Gradle, an Android SDK or an emulator. */
export function portableContract(t,main,{testSources=false}={}){
 const root=fileURLToPath(new URL('../../',import.meta.url)),work=fs.mkdtempSync(path.join(os.tmpdir(),'whist-contract-'));
 t.after(()=>fs.rmSync(work,{recursive:true,force:true,maxRetries:15,retryDelay:200}));
 const roots=['atf-ui-browser/src/main/java','whist-lab-app/atf-card-table-ui/src/main/java',
  'whist-lab-app/card-table-names/src/main/java','whist-lab-app/game-core/src/main/java',
  'whist-lab-app/app/src/main/java','atf-ui-browser/whist/src/main/java',
  ...(testSources?['whist-lab-app/app/src/test/java']:[])].map(n=>path.join(root,n));
 const fixture=path.join(root,'atf-ui-browser/tests',...main.split('.'))+'.java';
 const sources=[fixture,...fs.readdirSync(roots[0]+'/ro/cardtable/browser').filter(n=>n.endsWith('.java')).map(n=>roots[0]+'/ro/cardtable/browser/'+n)];
 const args=['-proc:none','-encoding','UTF-8','-sourcepath',roots.join(path.delimiter),'-d',work,...sources];
 const argFile=path.join(work,'javac.args');fs.writeFileSync(argFile,args.map(n=>'"'+n.replaceAll('\\','/')+'"').join('\n'));
 execFileSync('javac',['-J-Xmx384m','@'+argFile],{encoding:'utf8',timeout:60000,windowsHide:true});
 return {root,run:(...args)=>execFileSync('java',['-Xmx384m','-Dfile.encoding=UTF-8','-Dstdout.encoding=UTF-8','-cp',work,main,...args],
  {encoding:'utf8',timeout:90000,windowsHide:true})};
}
