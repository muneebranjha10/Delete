package dev.s23hype.deletefx;
import android.animation.*;
import android.graphics.*;
import android.graphics.drawable.Drawable;
import android.os.Looper;
import android.view.View;
import java.lang.reflect.Method;
import de.robv.android.xposed.*;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
public final class LauncherHook implements IXposedHookLoadPackage {
 private static final String PKG="com.sec.android.app.launcher",ICON="com.honeyspace.ui.common.iconview.IconContainer";
 private static boolean installed,warned;
 private static UninstallBridge uninstall;
 public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpp){
  if(!PKG.equals(lpp.packageName)||!PKG.equals(lpp.processName)||installed)return;
  XposedBridge.log("AppDeleteFX 0.6: launcher loaded "+lpp.processName);
  try{uninstall=UninstallBridge.install(lpp.classLoader);}catch(Throwable t){XposedBridge.log("AppDeleteFX uninstall hook unavailable: "+t);}
  try{
   Class<?> cls=Class.forName(ICON,false,lpp.classLoader),callback=Class.forName("kotlin.jvm.functions.Function0",false,lpp.classLoader);
   Method remove=cls.getDeclaredMethod("getRemoveAnimation",callback);
   if(!View.class.isAssignableFrom(cls)||!AnimatorSet.class.isAssignableFrom(remove.getReturnType()))return;
   Method getIcon=cls.getMethod("getIcon"),getPosition=cls.getMethod("getIconPosition"),getChild=cls.getMethod("getIconView");
   XposedBridge.hookMethod(remove,new XC_MethodHook(){
    protected void afterHookedMethod(MethodHookParam p){
     if(p.hasThrowable()||!(p.getResult() instanceof AnimatorSet)||Looper.myLooper()!=Looper.getMainLooper())return;
     try{
      View container=(View)p.thisObject;
      if(uninstall!=null&&uninstall.owns(container))return;
      AnimatorSet original=(AnimatorSet)p.getResult();
      original.addListener(new AnimatorListenerAdapter(){
       private boolean played; private View hidden;private float oldAlpha;
       public void onAnimationStart(Animator a){
        if(played)return;played=true;
        if(uninstall!=null&&uninstall.owns(container))return;
        try{
         View child=render(container,getIcon,getPosition,getChild);
         if(child!=null&&child!=container){oldAlpha=child.getAlpha();hidden=child;child.setAlpha(0f);}
        }catch(Throwable t){warn(t);}
       }
       private void restore(){if(hidden!=null){if(hidden.getAlpha()==0f)hidden.setAlpha(oldAlpha);hidden=null;}}
       public void onAnimationCancel(Animator a){restore();}
       public void onAnimationEnd(Animator a){restore();}
      });
     }catch(Throwable t){warn(t);}
    }
   });
   installed=true;XposedBridge.log("AppDeleteFX 0.6: installed IconContainer removal hook");
  }catch(Throwable t){warn(t);}
 }
 private static View render(View container,Method getIcon,Method getPosition,Method getChild)throws Exception{
  if(!container.isAttachedToWindow()||!container.isShown()||!ValueAnimator.areAnimatorsEnabled())return null;
  Object item=getIcon.invoke(container);if(!(item instanceof Drawable))return null;
  Object child=getChild.invoke(container);View anchor=container;
  try{Object v=XposedHelpers.callMethod(child,"getView");if(v instanceof View&&((View)v).isAttachedToWindow())anchor=(View)v;}catch(Throwable ignored){}
  Object r=getPosition.invoke(container);if(!(r instanceof Rect))return null;
  Rect pos=new Rect((Rect)r);
  if(pos.width()<=0||pos.height()<=0||pos.width()>anchor.getWidth()+2||pos.height()>anchor.getHeight()+2)return null;
  View root=container.getRootView();if(root==null||root==container)return null;
  int[] at=new int[2],origin=new int[2];anchor.getLocationOnScreen(at);root.getLocationOnScreen(origin);
  RectF box=new RectF(pos);box.offset(at[0]-origin[0],at[1]-origin[1]);
  if(!RectF.intersects(box,new RectF(0,0,root.getWidth(),root.getHeight())))return null;
  Bitmap bmp=ParticleEffect.sample((Drawable)item);
  try{return ParticleEffect.play(root,bmp,box)?anchor:null;}finally{bmp.recycle();}
 }
 private static void warn(Throwable t){if(!warned){warned=true;XposedBridge.log("AppDeleteFX: stock animation fallback: "+t);}}
}
