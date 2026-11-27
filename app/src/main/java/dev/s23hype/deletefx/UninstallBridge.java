package dev.s23hype.deletefx;
import android.animation.ValueAnimator;
import android.app.PendingIntent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInstaller;
import android.content.pm.PackageManager;
import android.widget.Toast;
import java.util.UUID;
import android.content.*;
import android.content.pm.LauncherApps;
import android.graphics.*;
import android.graphics.drawable.Drawable;
import android.os.*;
import android.view.*;
import java.lang.ref.WeakReference;
import de.robv.android.xposed.*;
final class UninstallBridge {
 private static final String QUICK="com.honeyspace.ui.common.quickoption.";
 private final Class<?> iconClass,utilsClass;
 private final Handler main=new Handler(Looper.getMainLooper());
 private Pending pending;
 private TouchSnapshot touch;
 private final class TouchSnapshot {
  final WeakReference<View> root,anchor;final int id;final RectF box;final Bitmap bitmap;final long at;
  TouchSnapshot(View r,View a,int i,RectF b,Bitmap m){root=new WeakReference<>(r);anchor=new WeakReference<>(a);id=i;box=b;bitmap=m;at=SystemClock.uptimeMillis();}
 }
 private void forgetTouch(){if(touch!=null){touch.bitmap.recycle();touch=null;}}
 private void remember(View v){
  if(!v.isAttachedToWindow()||!v.isShown())return;
  int id=(Integer)XposedHelpers.callMethod(v,"getItemId");if(id<0)return;
  Object child=XposedHelpers.callMethod(v,"getIconView");Object d=XposedHelpers.callMethod(v,"getIcon");Object r=XposedHelpers.callMethod(v,"getIconPosition");
  if(!(d instanceof Drawable)||!(r instanceof Rect))return;
  View a=v;try{Object av=XposedHelpers.callMethod(child,"getView");if(av instanceof View)a=(View)av;}catch(Throwable ignored){}
  View root=v.getRootView();RectF box=new RectF((Rect)r);if(box.isEmpty())return;
  int[] xy=new int[2],origin=new int[2];a.getLocationOnScreen(xy);root.getLocationOnScreen(origin);box.offset(xy[0]-origin[0],xy[1]-origin[1]);
  if(!RectF.intersects(box,new RectF(0,0,root.getWidth(),root.getHeight())))return;
  forgetTouch();TouchSnapshot snap=new TouchSnapshot(root,v,id,box,ParticleEffect.sample((Drawable)d));touch=snap;
  main.postDelayed(()->{if(touch==snap)forgetTouch();},45000);
 }

 private final ThreadLocal<Boolean> explicitClick=new ThreadLocal<>();
 private boolean directBusy;
 private ComponentName selected;
 private UserHandle selectedUser;
 private final ThreadLocal<View> popup=new ThreadLocal<>();
 private UninstallBridge(ClassLoader cl)throws Exception{
  iconClass=Class.forName("com.honeyspace.ui.common.iconview.IconContainer",false,cl);
  utilsClass=Class.forName("com.honeyspace.ui.common.util.PopupAnchorInfoUtils",false,cl);
 }
 static UninstallBridge install(ClassLoader cl)throws Exception{
  UninstallBridge b=new UninstallBridge(cl);
  try{XposedHelpers.findAndHookMethod(b.iconClass,"onTouchEvent",MotionEvent.class,new XC_MethodHook(){
   protected void beforeHookedMethod(MethodHookParam p){if(((MotionEvent)p.args[0]).getActionMasked()==MotionEvent.ACTION_DOWN){try{b.remember((View)p.thisObject);}catch(Throwable t){log("touch capture: "+t);}}}
  });log("early icon capture installed");}catch(Throwable t){log("early icon capture unavailable: "+t);}
  Class<?> option=Class.forName(QUICK+"Uninstall",false,cl);
  XposedHelpers.findAndHookMethod(option,"onClick",View.class,new XC_MethodHook(){
   protected void beforeHookedMethod(MethodHookParam p){b.explicitClick.set(true);b.selected=null;b.selectedUser=null;log("Uninstall.onClick");try{b.capture(p.thisObject);}catch(Throwable t){log("capture unavailable: "+t);}}
   protected void afterHookedMethod(MethodHookParam p){b.explicitClick.remove();b.selected=null;b.selectedUser=null;}
  });
  // Only replace Samsung's final intent launch after its existing eligibility checks.
  // Never grant privileges, use root, or bypass a required OS policy decision.
  try{
   Class<?> pkgUtils=Class.forName("com.honeyspace.ui.common.util.PackageUtils",false,cl);
   Class<?> dexInfo=Class.forName("com.honeyspace.common.interfaces.CombinedDexInfo",false,cl);
   XposedHelpers.findAndHookMethod(pkgUtils,"startUninstallActivity",Context.class,ComponentName.class,UserHandle.class,dexInfo,new XC_MethodHook(){
    protected void beforeHookedMethod(MethodHookParam p){
     if(!Boolean.TRUE.equals(b.explicitClick.get()))return;
     try{if(b.direct((Context)p.args[0],(ComponentName)p.args[1],(UserHandle)p.args[2],false))p.setResult(true);}catch(Throwable t){log("direct uninstall fallback: "+t);}
    }
   });
   XposedHelpers.findAndHookMethod(pkgUtils,"uninstallPackage",Context.class,String.class,new XC_MethodHook(){
    protected void beforeHookedMethod(MethodHookParam p){
     if(!Boolean.TRUE.equals(b.explicitClick.get()))return;
     if(b.selected==null||!b.selected.getPackageName().equals(p.args[1]))return;
     try{if(b.direct((Context)p.args[0],b.selected,b.selectedUser,true))p.setResult(true);}catch(Throwable t){log("direct uninstall fallback: "+t);}
    }
   });
  }catch(Throwable t){log("direct path unavailable: "+t);}
  // Only the launcher-owned uninstall window; do not alter system installer windows.
  try{
   XposedHelpers.findAndHookMethod(option,"setLayoutParams",new XC_MethodHook(){
    protected void beforeHookedMethod(MethodHookParam p){try{b.popup.set((View)XposedHelpers.getObjectField(p.thisObject,"uninstallPopup"));}catch(Throwable ignored){b.popup.remove();}}
    protected void afterHookedMethod(MethodHookParam p){b.popup.remove();}
   });
   Class<?> wm=Class.forName("android.view.WindowManagerImpl");
   XposedHelpers.findAndHookMethod(wm,"addView",View.class,ViewGroup.LayoutParams.class,new XC_MethodHook(){
    protected void beforeHookedMethod(MethodHookParam p){
     if(b.popup.get()!=p.args[0]||b.popup.get()==null||!(p.args[1] instanceof WindowManager.LayoutParams))return;
     WindowManager.LayoutParams lp=(WindowManager.LayoutParams)p.args[1];
     lp.dimAmount=0f;lp.flags&=~WindowManager.LayoutParams.FLAG_DIM_BEHIND;
     log("launcher uninstall popup: dim removed");
    }
   });
  }catch(Throwable t){log("launcher popup dim hook unavailable: "+t);}
  log("uninstall path installed");return b;
 }
 boolean owns(View view){
  Pending q=pending;if(q==null)return false;
  try{return q.anchor.get()==view;}catch(Throwable ignored){return false;}
 }
 private void capture(Object option){
  if(Looper.myLooper()!=Looper.getMainLooper()||directBusy)return;
  clear(pending);
  Object info=XposedHelpers.callMethod(option,"getItemInfo");
  Object utils=XposedHelpers.getStaticObjectField(utilsClass,"INSTANCE");
  Object component=XposedHelpers.callStaticMethod(utilsClass,"getComponentName$default",utils,info,false,2,null);
  Object user=XposedHelpers.callStaticMethod(utilsClass,"getUserHandle$default",utils,info,false,2,null);
  if(!(component instanceof ComponentName)||!(user instanceof UserHandle))return;
  if(!android.os.Process.myUserHandle().equals(user))return;
  selected=(ComponentName)component;selectedUser=(UserHandle)user;
  Object item=XposedHelpers.callMethod(info,"getItemInfo");if(item==null)return;
  int id=(Integer)XposedHelpers.callMethod(item,"getId");
  if(id<0)return;
  View root=null,icon=null;RectF box=null;Bitmap bitmap=null;
  TouchSnapshot snap=touch;
  if(snap!=null&&snap.id==id&&SystemClock.uptimeMillis()-snap.at<45000){
   root=snap.root.get();icon=snap.anchor.get();
   if(root!=null&&root.isAttachedToWindow()&&icon!=null){
    box=new RectF(snap.box);bitmap=snap.bitmap.copy(Bitmap.Config.ARGB_8888,false);log("using early touch snapshot: "+id);
   }
  }
  if(bitmap==null){
   Object pot=XposedHelpers.callMethod(option,"getHoneyPot");
   Object rootObj=XposedHelpers.callMethod(pot,"getRootView");
   if(!(rootObj instanceof View)){log("no launcher root");return;}
   root=((View)rootObj).getRootView();
   icon=find(root,id,0,new int[]{5000});if(icon==null){log("selected icon not found: "+id);return;}
   Object drawable=XposedHelpers.callMethod(icon,"getIcon"),rect=XposedHelpers.callMethod(icon,"getIconPosition");
   if(!(drawable instanceof Drawable)||!(rect instanceof Rect)){log("no drawable or bounds");return;}
   Object child=XposedHelpers.callMethod(icon,"getIconView");View anchor=icon;
   try{Object v=XposedHelpers.callMethod(child,"getView");if(v instanceof View&&((View)v).isAttachedToWindow())anchor=(View)v;}catch(Throwable ignored){}
   box=new RectF((Rect)rect);if(box.isEmpty()){log("empty icon bounds");return;}
   int[] at=new int[2],origin=new int[2];anchor.getLocationOnScreen(at);root.getLocationOnScreen(origin);
   box.offset(at[0]-origin[0],at[1]-origin[1]);
   if(!RectF.intersects(box,new RectF(0,0,root.getWidth(),root.getHeight()))){log("icon outside root");return;}
   bitmap=ParticleEffect.sample((Drawable)drawable);
  }
  Context app=root.getContext().getApplicationContext();if(app==null){bitmap.recycle();return;}
  LauncherApps la=app.getSystemService(LauncherApps.class);if(la==null){bitmap.recycle();return;}
  Pending q=new Pending(root,icon,box,bitmap,(ComponentName)component,(UserHandle)user,la);
  pending=q;
  try{la.registerCallback(q.callback,main);q.registered=true;
   IntentFilter removed=new IntentFilter(Intent.ACTION_PACKAGE_REMOVED);removed.addDataScheme("package");
   try{if(Build.VERSION.SDK_INT>=33)app.registerReceiver(q.removalReceiver,removed,Context.RECEIVER_EXPORTED);else app.registerReceiver(q.removalReceiver,removed);q.receiverContext=app;}catch(RuntimeException e){log("broadcast backup unavailable: "+e);}
   main.postDelayed(q.expire,60000);log("armed for "+q.pkg);}
  catch(Throwable t){clear(q);log("package callback failed: "+t);}
 }
 private boolean direct(Context context,ComponentName component,UserHandle user,boolean pregrantPath)throws Exception{
  Pending q=pending;
  if(directBusy||context==null||component==null||user==null||!component.equals(selected)||!user.equals(selectedUser)||!android.os.Process.myUserHandle().equals(user))return false;
  if(q!=null&&(!q.component.equals(component)||!q.user.equals(user)))return false;
  final String target=component.getPackageName();
  XSharedPreferences pref=new XSharedPreferences("dev.s23hype.deletefx","deletefx");
  pref.reload();if(!pref.getBoolean("one_tap",false)){log("one-tap disabled or preferences unreadable");return false;}
  Context app=context.getApplicationContext();if(app==null)return false;
  if(app.checkSelfPermission("android.permission.DELETE_PACKAGES")!=PackageManager.PERMISSION_GRANTED){log("DELETE_PACKAGES not granted; stock confirmation");return false;}
  PackageManager pm=app.getPackageManager();ApplicationInfo ai=pm.getApplicationInfo(target,0);
  if((ai.flags&(ApplicationInfo.FLAG_SYSTEM|ApplicationInfo.FLAG_UPDATED_SYSTEM_APP))!=0||target.equals(app.getPackageName())||target.equals("dev.s23hype.deletefx"))return false;
  // Preserve managed-profile and administrator restrictions. PackageInstaller also enforces them.
  UserManager um=app.getSystemService(UserManager.class);
  if(um==null||um.isManagedProfile()||um.hasUserRestriction(UserManager.DISALLOW_UNINSTALL_APPS))return false;
  android.app.admin.DevicePolicyManager dpm=app.getSystemService(android.app.admin.DevicePolicyManager.class);
  if(dpm==null)return false;
  java.util.List<ComponentName> admins=dpm.getActiveAdmins();
  if(admins!=null)for(ComponentName admin:admins)if(target.equals(admin.getPackageName()))return false;
  String action=app.getPackageName()+".DELETE_FX."+UUID.randomUUID().toString();
  Intent result=new Intent(action).setPackage(app.getPackageName());
  int flags=PendingIntent.FLAG_UPDATE_CURRENT|(Build.VERSION.SDK_INT>=31?PendingIntent.FLAG_MUTABLE:0);
  PendingIntent pi=PendingIntent.getBroadcast(app,0,result,flags);
  final BroadcastReceiver[] holder=new BroadcastReceiver[1];
  final Runnable finish=()->{directBusy=false;try{app.unregisterReceiver(holder[0]);}catch(RuntimeException ignored){}pi.cancel();};
  holder[0]=new BroadcastReceiver(){
   public void onReceive(Context c,Intent i){
    if(!action.equals(i.getAction()))return;
    int status=i.getIntExtra(PackageInstaller.EXTRA_STATUS,PackageInstaller.STATUS_FAILURE);
    if(status==PackageInstaller.STATUS_PENDING_USER_ACTION){
     if(q!=null){q.osConfirmation=true;restoreVisual(q);}

     // Respect the OS if it requires confirmation. Never auto-approve.
     Intent confirm=i.getParcelableExtra(Intent.EXTRA_INTENT);
     if(confirm!=null){try{confirm.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);app.startActivity(confirm);}catch(RuntimeException e){log("OS confirmation unavailable: "+e);clear(q);}}
     else clear(q);
     return;
    }
    main.removeCallbacks(finish);finish.run();
    if(status==PackageInstaller.STATUS_SUCCESS){
     if(q!=null&&pending==q&&!q.confirmed){q.confirmed=true;q.removedAt=SystemClock.uptimeMillis();tryPlay(q);}
     log("direct uninstall succeeded: "+target);
    }else{clear(q);Toast.makeText(app,"Uninstall was not completed",Toast.LENGTH_SHORT).show();log("uninstall status "+status);}
   }
  };
  try{
   if(Build.VERSION.SDK_INT>=33)app.registerReceiver(holder[0],new IntentFilter(action),Context.RECEIVER_NOT_EXPORTED);
   else app.registerReceiver(holder[0],new IntentFilter(action));
   directBusy=true;main.postDelayed(finish,120000);
   pm.getPackageInstaller().uninstall(target,pi.getIntentSender());
   if(q!=null){q.submittedAt=SystemClock.uptimeMillis();main.post(()->startImmediate(q));}
   log("one-tap request submitted: "+target);return true;
  }catch(RuntimeException e){main.removeCallbacks(finish);finish.run();log("direct request rejected: "+e);return false;}
 }
 private View find(View v,int id,int depth,int[] budget){
  if(v==null||depth>28||--budget[0]<0)return null;
  if(iconClass.isInstance(v)&&v.isAttachedToWindow()){
   try{if(((Integer)XposedHelpers.callMethod(v,"getItemId"))==id)return v;}catch(Throwable ignored){}
  }
  if(v instanceof ViewGroup){
   ViewGroup g=(ViewGroup)v;
   for(int i=0;i<g.getChildCount();i++){View r=find(g.getChildAt(i),id,depth+1,budget);if(r!=null)return r;}
  }return null;
 }
 private final class Pending{
  final WeakReference<View> root,anchor;final RectF box;final Bitmap bitmap;
  final String pkg;final ComponentName component;final UserHandle user;final LauncherApps la;
  final int width,height;boolean registered,confirmed,played,osConfirmation;long removedAt,submittedAt;Context receiverContext;
  WeakReference<View> hidden;float originalAlpha;int hiddenId=-1;
  final BroadcastReceiver removalReceiver=new BroadcastReceiver(){public void onReceive(Context c,Intent i){if(!Intent.ACTION_PACKAGE_REMOVED.equals(i.getAction())||i.getBooleanExtra(Intent.EXTRA_REPLACING,false)||i.getData()==null||!pkg.equals(i.getData().getSchemeSpecificPart())||pending!=Pending.this||confirmed)return;try{c.getPackageManager().getApplicationInfo(pkg,0);return;}catch(PackageManager.NameNotFoundException expected){} confirmed=true;removedAt=SystemClock.uptimeMillis();log("confirmed package removal broadcast: "+pkg);tryPlay(Pending.this);}};
  final Runnable expire=()->clear(this);
  final LauncherApps.Callback callback=new LauncherApps.Callback(){
   public void onPackageRemoved(String name,UserHandle u){
    if(pending!=Pending.this||confirmed||!pkg.equals(name)||!user.equals(u))return;
    confirmed=true;removedAt=SystemClock.uptimeMillis();log("confirmed uninstall: "+pkg);tryPlay(Pending.this);
   }
   public void onPackageAdded(String name,UserHandle u){}
   public void onPackageChanged(String name,UserHandle u){}
   public void onPackagesAvailable(String[] names,UserHandle u,boolean replacing){}
   public void onPackagesUnavailable(String[] names,UserHandle u,boolean replacing){}
  };
  Pending(View r,View a,RectF box,Bitmap bmp,ComponentName p,UserHandle u,LauncherApps l){
   root=new WeakReference<>(r);anchor=new WeakReference<>(a);this.box=box;bitmap=bmp;component=p;pkg=p.getPackageName();user=u;la=l;width=r.getWidth();height=r.getHeight();
  }
 }
 private void startImmediate(Pending q){
  if(pending!=q||q.played||q.osConfirmation||q.confirmed||!ValueAnimator.areAnimatorsEnabled())return;
  try{
   View r=q.root.get();
   if(r==null||!r.isAttachedToWindow()||r.getWidth()!=q.width||r.getHeight()!=q.height)return;
   if(!r.hasWindowFocus()||!r.isShown()){
    if(SystemClock.uptimeMillis()-q.submittedAt<200)main.postDelayed(()->startImmediate(q),16);
    return;
   }
   playNow(q,r,false);
  }catch(Throwable t){restoreVisual(q);log("immediate visual fallback: "+t);}
 }
 private void playNow(Pending q,View r,boolean confirmed){
  if(q.played)return;
  View a=q.anchor.get();
  if(a!=null&&a.isAttachedToWindow()&&a.getRootView()==r&&a.isShown()){
   try{
    int id=(Integer)XposedHelpers.callMethod(a,"getItemId");
    Object child=XposedHelpers.callMethod(a,"getIconView");
    Object v=XposedHelpers.callMethod(child,"getView");
    if(v instanceof View&&((View)v).isAttachedToWindow()){
     View h=(View)v;q.hidden=new WeakReference<>(h);q.hiddenId=id;q.originalAlpha=h.getAlpha();h.setAlpha(0f);
    }
   }catch(Throwable ignored){}
  }
  if(ParticleEffect.play(r,q.bitmap,q.box,confirmed)){
   q.played=true;log(confirmed?"confirmed uninstall particles played":"immediate uninstall transition started");
   main.postDelayed(()->restoreVisual(q),ParticleMath.DURATION_MS+40);
  }else restoreVisual(q);
 }
 private void restoreVisual(Pending q){
  if(q==null||q.hidden==null)return;
  View h=q.hidden.get();q.hidden=null;
  if(h!=null&&h.getAlpha()==0f)h.setAlpha(q.originalAlpha);
 }
 private void tryPlay(Pending q){
  if(pending!=q)return;
  try{
   if(q.played){clear(q);return;}
   View r=q.root.get();long elapsed=SystemClock.uptimeMillis()-q.removedAt;
   if(r==null||!r.isAttachedToWindow()||r.getWidth()!=q.width||r.getHeight()!=q.height||elapsed>4000){log("root unavailable/timeout");clear(q);return;}
   if(!r.hasWindowFocus()||!r.isShown()){main.postDelayed(()->tryPlay(q),16);return;}
   playNow(q,r,true);clear(q);
  }catch(Throwable t){clear(q);log("uninstall effect fallback: "+t);}
 }
 private void clear(Pending q){
  if(q==null)return;if(pending==q)pending=null;
  if(!q.confirmed)restoreVisual(q);
  main.removeCallbacks(q.expire);
  if(q.registered){q.registered=false;try{q.la.unregisterCallback(q.callback);}catch(Throwable ignored){}}
  if(q.receiverContext!=null){try{q.receiverContext.unregisterReceiver(q.removalReceiver);}catch(RuntimeException ignored){}q.receiverContext=null;}
  if(!q.bitmap.isRecycled())q.bitmap.recycle();
 }
 private static void log(String msg){XposedBridge.log("AppDeleteFX 0.6: "+msg);}
}
