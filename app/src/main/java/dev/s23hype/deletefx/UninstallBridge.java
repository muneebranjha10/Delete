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
 private final ThreadLocal<Boolean> explicitClick=new ThreadLocal<>();
 private boolean directBusy;
 private final ThreadLocal<View> popup=new ThreadLocal<>();
 private UninstallBridge(ClassLoader cl)throws Exception{
  iconClass=Class.forName("com.honeyspace.ui.common.iconview.IconContainer",false,cl);
  utilsClass=Class.forName("com.honeyspace.ui.common.util.PopupAnchorInfoUtils",false,cl);
 }
 static UninstallBridge install(ClassLoader cl)throws Exception{
  UninstallBridge b=new UninstallBridge(cl);
  Class<?> option=Class.forName(QUICK+"Uninstall",false,cl);
  XposedHelpers.findAndHookMethod(option,"onClick",View.class,new XC_MethodHook(){
   protected void beforeHookedMethod(MethodHookParam p){b.explicitClick.set(true);try{b.capture(p.thisObject);}catch(Throwable t){log("capture unavailable: "+t);}}
   protected void afterHookedMethod(MethodHookParam p){b.explicitClick.remove();}
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
     Pending q=b.pending;
     if(q==null||!q.pkg.equals(p.args[1]))return;
     try{if(b.direct((Context)p.args[0],q.component,q.user,true))p.setResult(true);}catch(Throwable t){log("direct uninstall fallback: "+t);}
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
  if(Looper.myLooper()!=Looper.getMainLooper()||!ValueAnimator.areAnimatorsEnabled()||directBusy)return;
  clear(pending);
  Object info=XposedHelpers.callMethod(option,"getItemInfo");
  Object utils=XposedHelpers.getStaticObjectField(utilsClass,"INSTANCE");
  Object component=XposedHelpers.callMethod(utils,"getComponentName",info,false);
  Object user=XposedHelpers.callMethod(utils,"getUserHandle",info,false);
  if(!(component instanceof ComponentName)||!(user instanceof UserHandle))return;
  if(!android.os.Process.myUserHandle().equals(user))return;
  Object item=XposedHelpers.callMethod(info,"getItemInfo");if(item==null)return;
  int id=(Integer)XposedHelpers.callMethod(item,"getId");
  if(id<0)return;
  Object pot=XposedHelpers.callMethod(option,"getHoneyPot");
  Object rootObj=XposedHelpers.callMethod(pot,"getRootView");
  if(!(rootObj instanceof View))return;
  View root=((View)rootObj).getRootView();
  View icon=find(root,id,0,new int[]{3500});if(icon==null){log("selected icon not found: "+id);return;}
  Object drawable=XposedHelpers.callMethod(icon,"getIcon");
  Object rect=XposedHelpers.callMethod(icon,"getIconPosition");
  if(!(drawable instanceof Drawable)||!(rect instanceof Rect))return;
  Object child=XposedHelpers.callMethod(icon,"getIconView");
  View anchor=icon;
  try{Object v=XposedHelpers.callMethod(child,"getView");if(v instanceof View&&((View)v).isAttachedToWindow())anchor=(View)v;}catch(Throwable ignored){}
  RectF box=new RectF((Rect)rect);
  if(box.width()<=0||box.height()<=0||box.width()>anchor.getWidth()+2||box.height()>anchor.getHeight()+2)return;
  int[] at=new int[2],origin=new int[2];anchor.getLocationOnScreen(at);root.getLocationOnScreen(origin);
  box.offset(at[0]-origin[0],at[1]-origin[1]);
  if(!RectF.intersects(box,new RectF(0,0,root.getWidth(),root.getHeight())))return;
  Context app=root.getContext().getApplicationContext();if(app==null)return;
  LauncherApps la=app.getSystemService(LauncherApps.class);if(la==null)return;
  Pending q=new Pending(root,icon,box,ParticleEffect.sample((Drawable)drawable),(ComponentName)component,(UserHandle)user,la);
  pending=q;
  try{la.registerCallback(q.callback,main);q.registered=true;main.postDelayed(q.expire,60000);log("armed for "+q.pkg);}
  catch(Throwable t){clear(q);log("package callback failed: "+t);}
 }
 private boolean direct(Context context,ComponentName component,UserHandle user,boolean pregrantPath)throws Exception{
  Pending q=pending;
  if(q==null||directBusy||context==null||component==null||user==null||!q.component.equals(component)||!q.user.equals(user)||!android.os.Process.myUserHandle().equals(user))return false;
  XSharedPreferences pref=new XSharedPreferences("dev.s23hype.deletefx","deletefx");
  pref.reload();if(!pref.getBoolean("one_tap",false)){log("one-tap disabled or preferences unreadable");return false;}
  Context app=context.getApplicationContext();if(app==null)return false;
  if(app.checkSelfPermission("android.permission.DELETE_PACKAGES")!=PackageManager.PERMISSION_GRANTED){log("DELETE_PACKAGES not granted; stock confirmation");return false;}
  PackageManager pm=app.getPackageManager();ApplicationInfo ai=pm.getApplicationInfo(q.pkg,0);
  if((ai.flags&(ApplicationInfo.FLAG_SYSTEM|ApplicationInfo.FLAG_UPDATED_SYSTEM_APP))!=0||q.pkg.equals(app.getPackageName())||q.pkg.equals("dev.s23hype.deletefx"))return false;
  // Preserve managed-profile and administrator restrictions. PackageInstaller also enforces them.
  UserManager um=app.getSystemService(UserManager.class);
  if(um==null||um.isManagedProfile()||um.hasUserRestriction(UserManager.DISALLOW_UNINSTALL_APPS))return false;
  android.app.admin.DevicePolicyManager dpm=app.getSystemService(android.app.admin.DevicePolicyManager.class);
  if(dpm==null)return false;
  java.util.List<ComponentName> admins=dpm.getActiveAdmins();
  if(admins!=null)for(ComponentName admin:admins)if(q.pkg.equals(admin.getPackageName()))return false;
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
     // Respect the OS if it requires confirmation. Never auto-approve.
     Intent confirm=i.getParcelableExtra(Intent.EXTRA_INTENT);
     if(confirm!=null){try{confirm.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);app.startActivity(confirm);}catch(RuntimeException e){log("OS confirmation unavailable: "+e);clear(q);}}
     else clear(q);
     return;
    }
    main.removeCallbacks(finish);finish.run();
    if(status==PackageInstaller.STATUS_SUCCESS){
     if(pending==q&&!q.confirmed){q.confirmed=true;q.removedAt=SystemClock.uptimeMillis();tryPlay(q);}
     log("direct uninstall succeeded: "+q.pkg);
    }else{clear(q);Toast.makeText(app,"Uninstall was not completed",Toast.LENGTH_SHORT).show();log("uninstall status "+status);}
   }
  };
  try{
   if(Build.VERSION.SDK_INT>=33)app.registerReceiver(holder[0],new IntentFilter(action),Context.RECEIVER_NOT_EXPORTED);
   else app.registerReceiver(holder[0],new IntentFilter(action));
   directBusy=true;main.postDelayed(finish,120000);
   pm.getPackageInstaller().uninstall(q.pkg,pi.getIntentSender());
   log("one-tap request submitted: "+q.pkg);return true;
  }catch(RuntimeException e){main.removeCallbacks(finish);finish.run();log("direct request rejected: "+e);return false;}
 }
 private View find(View v,int id,int depth,int[] budget){
  if(v==null||depth>28||--budget[0]<0||v.getVisibility()!=View.VISIBLE)return null;
  if(iconClass.isInstance(v)&&v.isAttachedToWindow()&&v.isShown()){
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
  final int width,height;boolean registered,confirmed;long removedAt;
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
 private void tryPlay(Pending q){
  if(pending!=q)return;
  try{
   View r=q.root.get(),a=q.anchor.get();long elapsed=SystemClock.uptimeMillis()-q.removedAt;
   if(r==null||!r.isAttachedToWindow()||r.getWidth()!=q.width||r.getHeight()!=q.height||elapsed>1600){clear(q);return;}
   if(!r.hasWindowFocus()||!r.isShown()||(elapsed<250&&a!=null&&a.isAttachedToWindow()&&a.isShown())){main.postDelayed(()->tryPlay(q),16);return;}
   // Do not animate on an icon that is still present: preserve stock behavior.
   if(a!=null&&a.isAttachedToWindow()&&a.isShown()&&a.getAlpha()>.05f){clear(q);log("icon still present; stock fallback");return;}
   if(ParticleEffect.play(r,q.bitmap,q.box,true))log("uninstall particles played");
   clear(q);
  }catch(Throwable t){clear(q);log("uninstall effect fallback: "+t);}
 }
 private void clear(Pending q){
  if(q==null)return;if(pending==q)pending=null;
  main.removeCallbacks(q.expire);
  if(q.registered){q.registered=false;try{q.la.unregisterCallback(q.callback);}catch(Throwable ignored){}}
  if(!q.bitmap.isRecycled())q.bitmap.recycle();
 }
 private static void log(String msg){XposedBridge.log("AppDeleteFX 0.4: "+msg);}
}
