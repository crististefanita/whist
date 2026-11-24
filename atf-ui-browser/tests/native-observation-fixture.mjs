/** Controlled Android API responses, not a native font or screenshot qualification. */
export const nativeObservationSources = {
 'android/graphics/Rect.java': `package android.graphics; public class Rect { public int left,top,right,bottom; }`,
 'android/os/Looper.java': `package android.os; public class Looper { static final Looper MAIN=new Looper(); public static boolean wrong; public static Looper myLooper(){return wrong?null:MAIN;} public static Looper getMainLooper(){return MAIN;} }`,
 'android/content/res/Resources.java': `package android.content.res; public class Resources { public static class Metrics { public float density=2; } public Metrics getDisplayMetrics(){return new Metrics();} }`,
 'android/view/View.java': `package android.view;
 import android.graphics.Rect;
 public class View {
 public View root=this; public int x=100,y=200,width=80,height=60,reads; public boolean ready=true,pending,shown=true,clipped;
 public Runnable duringRead=()->{};
 public View getRootView(){return root;} public boolean isLaidOut(){return ready;} public boolean isLayoutRequested(){return pending;}
 public boolean isShown(){return shown;} public int getWidth(){return width;} public int getHeight(){return height;}
 public void getLocationOnScreen(int[] xy){xy[0]=x;xy[1]=y;reads++;}
 public boolean getGlobalVisibleRect(Rect r){reads++;duringRead.run();r.left=x;r.top=y;r.right=x+width-(clipped?1:0);r.bottom=y+height;return shown;}
 public android.content.res.Resources getResources(){return new android.content.res.Resources();}
 }`,
 'android/text/Layout.java': `package android.text; public class Layout {
 public float right=74; public int ellipsis,bottom=20; public int getLineCount(){return 1;} public int getLineBaseline(int line){return 15;}
 public int getEllipsisCount(int line){return ellipsis;} public float getLineLeft(int line){return 0;}
 public float getLineRight(int line){return right;} public int getLineTop(int line){return 0;} public int getLineBottom(int line){return bottom;}
 }`,
 'android/widget/TextView.java': `package android.widget; public class TextView extends android.view.View {
 public android.text.Layout layout=new android.text.Layout(); public int length(){return 8;} public android.text.Layout getLayout(){return layout;}
 public int getCompoundPaddingLeft(){return 3;} public int getBaseline(){return 18;} public int getScrollX(){return 0;} public int getScrollY(){return 0;}
 }`,
 'NativeObservationProbe.java': `import java.util.*;
 import android.view.View; import android.widget.TextView;
 import ro.cardtable.ui.observation.*; import ro.cardtable.testing.RenderedSurfaceAudit;
 public class NativeObservationProbe {
 static int rejected;
 static void expect(String code,Runnable action){try{action.run();throw new AssertionError("Missing refusal: "+code);}catch(IllegalStateException|AssertionError failure){if(!failure.getMessage().startsWith(code))throw failure;rejected++;}}
 static AndroidSurfaceObservation.Binding binding(String id,View root,View view){view.root=root;return new AndroidSurfaceObservation.Binding(id,view);}
 public static void main(String[] args){
 var epoch=new SurfaceEpoch(); var ticket=epoch.begin("match-1"); var root=new View();
 var header=new TextView();var fact=new TextView();var trump=new TextView();var card=new View();
 var bindings=List.of(binding("header",root,header),binding("fact:0",root,fact),binding("trump",root,trump),binding("card:0",root,card));
 var initial=AndroidSurfaceObservation.capture(root,bindings,epoch,ticket);RenderedSurfaceAudit.visible(initial,0);
 if(initial.regions().size()!=4||initial.regions().get(1).textLines().size()!=1||initial.scale()!=2)throw new AssertionError("Missing native fields");
 var next=epoch.begin("match-1");var after=AndroidSurfaceObservation.capture(root,bindings,epoch,next);RenderedSurfaceAudit.stable(initial,after,0);
 if(args[0].equals("negative")){
 fact.layout.right=90;expect("CLIPPED_TEXT",()->RenderedSurfaceAudit.visible(AndroidSurfaceObservation.capture(root,bindings,epoch,next),0));fact.layout.right=74;
 fact.layout.ellipsis=1;expect("ELLIPSIZED_TEXT",()->RenderedSurfaceAudit.visible(AndroidSurfaceObservation.capture(root,bindings,epoch,next),0));fact.layout.ellipsis=0;
 fact.layout.bottom=65;expect("CLIPPED_TEXT",()->RenderedSurfaceAudit.visible(AndroidSurfaceObservation.capture(root,bindings,epoch,next),0));fact.layout.bottom=20;
 card.clipped=true;expect("CLIPPED_FRAME",()->RenderedSurfaceAudit.visible(AndroidSurfaceObservation.capture(root,bindings,epoch,next),0));card.clipped=false;
 fact.x++;expect("REGION_DRIFT",()->RenderedSurfaceAudit.stable(initial,AndroidSurfaceObservation.capture(root,bindings,epoch,next),0));fact.x--;
 int reads=fact.reads;expect("STALE_RENDER_OWNER",()->AndroidSurfaceObservation.capture(root,bindings,epoch,ticket));if(reads!=fact.reads)throw new AssertionError("Stale owner measured a view");
 fact.root=new View();expect("FOREIGN_NATIVE_VIEW",()->AndroidSurfaceObservation.capture(root,bindings,epoch,next));fact.root=root;
 root.pending=true;expect("NATIVE_LAYOUT_NOT_READY",()->AndroidSurfaceObservation.capture(root,bindings,epoch,next));root.pending=false;
 fact.layout=null;expect("NATIVE_TEXT_NOT_LAID_OUT",()->AndroidSurfaceObservation.capture(root,bindings,epoch,next));fact.layout=new android.text.Layout();
 android.os.Looper.wrong=true;expect("NATIVE_OBSERVATION_WRONG_THREAD",()->AndroidSurfaceObservation.capture(root,bindings,epoch,next));android.os.Looper.wrong=false;
 fact.duringRead=()->epoch.begin("replacement");expect("STALE_RENDER_OWNER",()->AndroidSurfaceObservation.capture(root,bindings,epoch,next));
 }
 System.out.println("{\\"status\\":\\"PASS\\",\\"regions\\":4,\\"rejected\\":"+rejected+",\\"observed\\":\\""+initial.toString().replace("\\\\","\\\\\\\\").replace("\\\"","\\\\\\\"")+"\\"}");
 }
 }`
};
