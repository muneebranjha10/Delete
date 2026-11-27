package dev.s23hype.deletefx;
import android.app.Activity;
import android.os.Bundle;
import android.graphics.*;
import android.graphics.drawable.Drawable;
import android.view.*;
import android.widget.*;
import android.animation.ValueAnimator;
public final class PreviewActivity extends Activity {
 private boolean running;
 private int dp(float x){return Math.round(x*getResources().getDisplayMetrics().density);}
 public void onCreate(Bundle b){super.onCreate(b);
  LinearLayout page=new LinearLayout(this);page.setOrientation(1);page.setGravity(Gravity.CENTER_HORIZONTAL);page.setPadding(dp(24),dp(48),dp(24),dp(24));page.setBackgroundColor(0xff101014);
  TextView title=new TextView(this);title.setText("App Delete FX");title.setTextSize(28);title.setTextColor(0xfff5f4ff);page.addView(title);
  FrameLayout stage=new FrameLayout(this);LinearLayout.LayoutParams sp=new LinearLayout.LayoutParams(-1,dp(300));sp.topMargin=dp(24);page.addView(stage,sp);stage.setBackgroundColor(0xff24252a);
  ImageView icon=new ImageView(this);Drawable sample;
  try{sample=getPackageManager().getApplicationIcon("com.android.settings");}catch(Exception e){sample=getDrawable(R.drawable.ic_module);}
  icon.setImageDrawable(sample);stage.addView(icon,new FrameLayout.LayoutParams(dp(56),dp(56),Gravity.CENTER));
  Button play=new Button(this);play.setText("Preview");page.addView(play,new LinearLayout.LayoutParams(-1,-2));
  View.OnClickListener click=v->{if(running||!ValueAnimator.areAnimatorsEnabled())return;running=true;play.setEnabled(false);
   Bitmap bmp=null;try{bmp=ParticleEffect.sample(icon.getDrawable());if(ParticleEffect.play(stage,bmp,new RectF(icon.getLeft(),icon.getTop(),icon.getRight(),icon.getBottom()))){icon.setAlpha(0);stage.postDelayed(()->{icon.setAlpha(1);running=false;play.setEnabled(true);},750);}else{running=false;play.setEnabled(true);}}catch(RuntimeException e){running=false;play.setEnabled(true);icon.setAlpha(1);}finally{if(bmp!=null)bmp.recycle();}
  };play.setOnClickListener(click);icon.setOnClickListener(click);
  Switch fast=new Switch(this);fast.setText("One-tap uninstall");fast.setTextColor(0xfff5f4ff);
  TextView notice=new TextView(this);notice.setText("When enabled, tapping Uninstall can immediately delete a downloaded app and its local data. Android may still require confirmation. System apps are excluded.");notice.setTextColor(0xffc1c0ce);
  try{
   android.content.SharedPreferences prefs=getSharedPreferences("deletefx",MODE_WORLD_READABLE);
   fast.setChecked(prefs.getBoolean("one_tap",false));
   fast.setOnCheckedChangeListener((button,on)->{if(on){new android.app.AlertDialog.Builder(this).setTitle("Enable one-tap uninstall?").setMessage("Uninstall will remove the selected app and its local data without a second confirmation when Android permits.").setPositiveButton("Enable",(d,w)->prefs.edit().putBoolean("one_tap",true).commit()).setNegativeButton("Cancel",(d,w)->fast.setChecked(false)).setOnCancelListener(d->fast.setChecked(false)).show();}else prefs.edit().putBoolean("one_tap",false).commit();});
  }catch(SecurityException e){fast.setEnabled(false);notice.setText("One-tap requires LSPosed shared preferences. Enable the module, restart the device, then reopen this app.");}
  page.addView(fast);page.addView(notice);
  TextView guide=new TextView(this);guide.setText("LSPosed → One UI Home only → restart launcher.\nExperimental 0.4 alpha");guide.setTextColor(0xffc1c0ce);guide.setPadding(0,dp(20),0,0);page.addView(guide);setContentView(page);
 }
}
