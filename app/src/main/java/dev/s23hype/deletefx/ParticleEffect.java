package dev.s23hype.deletefx;
import android.animation.*;
import android.graphics.*;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.Choreographer;
import android.os.SystemClock;
import java.util.WeakHashMap;
import android.view.animation.LinearInterpolator;
import java.util.Random;
final class ParticleEffect extends Drawable {
 private static final int GRID=36,MAX=GRID*GRID;
 private static final WeakHashMap<View,Recent> recent=new WeakHashMap<>();
 private static final class Recent {final RectF box;final long at;Recent(RectF b){box=new RectF(b);at=SystemClock.uptimeMillis();}}
 static boolean alreadyPlayed(View root,RectF box){Recent r=recent.get(root);return r!=null&&SystemClock.uptimeMillis()-r.at<2200&&Math.abs(r.box.centerX()-box.centerX())<box.width()*.6f&&Math.abs(r.box.centerY()-box.centerY())<box.height()*.6f;}
 private final float[] x=new float[MAX],y=new float[MAX],vx=new float[MAX],vy=new float[MAX],radius=new float[MAX],delay=new float[MAX],phase=new float[MAX],life=new float[MAX];
 private final int[] colors=new int[MAX];
 private final boolean[] spark=new boolean[MAX];
 private final int[] highlights=new int[MAX];
 private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);
 private final RectF box;
 private final Bitmap image;
 private final float size,cx,cy;
 private int count;
 private float time;
 private boolean burstOnly;
 private ParticleEffect(Bitmap sampled,RectF bounds){
  box=new RectF(bounds);size=Math.min(box.width(),box.height());cx=box.centerX();cy=box.centerY();
  image=sampled.copy(Bitmap.Config.ARGB_8888,false);
  Random rand=new Random(1907L);
  int[] pixels=new int[GRID*GRID];image.getPixels(pixels,0,GRID,0,0,GRID,GRID);
  for(int gy=0;gy<GRID;gy++)for(int gx=0;gx<GRID;gx++){
   int color=pixels[gy*GRID+gx];if(Color.alpha(color)<55)continue;
   if(rand.nextFloat()>.62f)continue;
   int i=count++;
   float px=(gx+.15f+rand.nextFloat()*.7f)/GRID,py=(gy+.15f+rand.nextFloat()*.7f)/GRID;
   x[i]=(px-.5f)*box.width();y[i]=(py-.5f)*box.height();
   float a=(float)Math.atan2(y[i],x[i])+(rand.nextFloat()-.5f)*1.85f;
   float speed=size*(.18f+1.58f*(float)Math.pow(rand.nextFloat(),1.35));
   vx[i]=(float)Math.cos(a)*speed;vy[i]=(float)Math.sin(a)*speed;
   radius[i]=Math.max(.42f,size*(.0045f+.010f*(float)Math.pow(rand.nextFloat(),2)));
   float edge=Math.min(1f,(float)Math.hypot(x[i],y[i])/(size*.71f));
   delay[i]=.065f+(1-edge)*.035f+rand.nextFloat()*.03f;phase[i]=rand.nextFloat()*6.283f;
   life[i]=.85f+.15f*rand.nextFloat();colors[i]=color;highlights[i]=ParticleMath.highlight(color);spark[i]=rand.nextFloat()<.30f;
  }
 }
 static Bitmap sample(Drawable icon){
  Bitmap bmp=Bitmap.createBitmap(GRID,GRID,Bitmap.Config.ARGB_8888);
  Drawable.ConstantState cs=icon.getConstantState();Drawable d=cs==null?icon:cs.newDrawable().mutate();
  Rect old=new Rect(d.getBounds());
  try{d.setBounds(0,0,GRID,GRID);d.draw(new Canvas(bmp));return bmp;}
  catch(RuntimeException|Error e){bmp.recycle();throw e;}finally{d.setBounds(old);}
 }
 static boolean play(View root,Bitmap sampled,RectF box){return play(root,sampled,box,false);}
 static boolean play(View root,Bitmap sampled,RectF box,boolean burstOnly){
  if(!root.isAttachedToWindow()||root.getWidth()<=0||root.getHeight()<=0||!ValueAnimator.areAnimatorsEnabled())return false;
  if(alreadyPlayed(root,box))return false;
  ParticleEffect fx=new ParticleEffect(sampled,box);fx.burstOnly=burstOnly;
  if(fx.count==0){fx.image.recycle();return false;}
  fx.setBounds(0,0,root.getWidth(),root.getHeight());
  // Real elapsed frame time, independent of nonzero system animator-scale settings.
  final class Playback implements Choreographer.FrameCallback,View.OnAttachStateChangeListener {
   long first=-1;boolean done;
   public void doFrame(long nanos){
    if(done)return;
    if(!root.isAttachedToWindow()){finish();return;}
    if(first<0)first=nanos;
    fx.time=ParticleMath.clamp((nanos-first)/(ParticleMath.DURATION_MS*1000000f));fx.invalidateSelf();
    if(fx.time>=1f)finish();else Choreographer.getInstance().postFrameCallback(this);
   }
   public void onViewAttachedToWindow(View v){}
   public void onViewDetachedFromWindow(View v){finish();}
   void finish(){if(done)return;done=true;Choreographer.getInstance().removeFrameCallback(this);root.getOverlay().remove(fx);root.removeOnAttachStateChangeListener(this);}
  }
  Playback playback=new Playback();
  try{root.getOverlay().add(fx);root.addOnAttachStateChangeListener(playback);recent.put(root,new Recent(box));Choreographer.getInstance().postFrameCallback(playback);return true;}
  catch(RuntimeException e){recent.remove(root);playback.finish();throw e;}
 }
 @Override public void draw(Canvas c){
  float breakup=ParticleMath.breakup(time),collapse=ParticleMath.collapse(time);
  if(!burstOnly&&breakup<1){
   paint.setColor(Color.WHITE);paint.setAlpha(Math.round(255*(1-breakup)));
   c.save();c.scale(collapse,collapse,cx,cy);c.drawBitmap(image,null,box,paint);c.restore();
  }
  for(int i=0;i<count;i++){
   if(time<delay[i])continue;
   float p=ParticleMath.progress(time,delay[i]);float age=ParticleMath.clamp(p/life[i]);if(age>=1)continue;
   float travel=ParticleMath.spread(p);
   float xx=cx+x[i]*.96f+vx[i]*travel+(float)Math.sin(phase[i]+p*4)*size*.025f*p;
   float yy=cy+y[i]*.96f+vy[i]*travel+size*.035f*p*p;
   float r=radius[i]*ParticleMath.radius(age);
   int alpha=Math.round(Color.alpha(colors[i])*ParticleMath.fade(age)*breakup);
   // Every mote keeps its source pixel hue, including dark and white icon regions.
   // Small round highlights mimic a popping bubble without a white flash.
   paint.setColor(colors[i]);paint.setAlpha(alpha);
   c.drawCircle(xx,yy,r,paint);
   if(spark[i]&&r>.65f){
    float twinkle=.55f+.45f*(float)Math.abs(Math.sin(phase[i]+p*16));
    paint.setColor(highlights[i]);paint.setAlpha(Math.round(alpha*twinkle*.8f));
    c.drawCircle(xx-r*.24f,yy-r*.28f,Math.max(.28f,r*.45f),paint);
   }
  }
 }
 public void setAlpha(int a){} public void setColorFilter(ColorFilter f){} public int getOpacity(){return PixelFormat.TRANSLUCENT;}
}
