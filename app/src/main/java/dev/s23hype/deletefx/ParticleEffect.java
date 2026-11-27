package dev.s23hype.deletefx;
import android.animation.*;
import android.graphics.*;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.animation.LinearInterpolator;
import java.util.Random;
final class ParticleEffect extends Drawable {
 private static final int GRID=34,MAX=GRID*GRID;
 private final float[] x=new float[MAX],y=new float[MAX],vx=new float[MAX],vy=new float[MAX],radius=new float[MAX],delay=new float[MAX],phase=new float[MAX],life=new float[MAX];
 private final int[] colors=new int[MAX];
 private final boolean[] spark=new boolean[MAX];
 private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);
 private final RectF box;
 private final Bitmap image;
 private final float size,cx,cy;
 private int count;
 private float time;
 private ParticleEffect(Bitmap sampled,RectF bounds){
  box=new RectF(bounds);size=Math.min(box.width(),box.height());cx=box.centerX();cy=box.centerY();
  image=sampled.copy(Bitmap.Config.ARGB_8888,false);
  Random rand=new Random(1907L);
  int[] pixels=new int[GRID*GRID];image.getPixels(pixels,0,GRID,0,0,GRID,GRID);
  for(int gy=0;gy<GRID;gy++)for(int gx=0;gx<GRID;gx++){
   int color=pixels[gy*GRID+gx];if(Color.alpha(color)<55)continue;
   int i=count++;
   float px=(gx+.15f+rand.nextFloat()*.7f)/GRID,py=(gy+.15f+rand.nextFloat()*.7f)/GRID;
   x[i]=(px-.5f)*box.width();y[i]=(py-.5f)*box.height();
   float a=(float)Math.atan2(y[i],x[i])+(rand.nextFloat()-.5f)*2.9f;
   float speed=size*(.12f+1.78f*(float)Math.pow(rand.nextFloat(),1.5));
   vx[i]=(float)Math.cos(a)*speed;vy[i]=(float)Math.sin(a)*speed;
   radius[i]=Math.max(.38f,size*(.003f+.008f*(float)Math.pow(rand.nextFloat(),2)));
   delay[i]=.035f+rand.nextFloat()*.085f;phase[i]=rand.nextFloat()*6.283f;
   life[i]=.60f+.40f*rand.nextFloat();colors[i]=color;spark[i]=rand.nextFloat()<.18f;
  }
 }
 static Bitmap sample(Drawable icon){
  Bitmap bmp=Bitmap.createBitmap(GRID,GRID,Bitmap.Config.ARGB_8888);
  Drawable.ConstantState cs=icon.getConstantState();Drawable d=cs==null?icon:cs.newDrawable().mutate();
  Rect old=new Rect(d.getBounds());
  try{d.setBounds(0,0,GRID,GRID);d.draw(new Canvas(bmp));return bmp;}
  catch(RuntimeException|Error e){bmp.recycle();throw e;}finally{d.setBounds(old);}
 }
 static boolean play(View root,Bitmap sampled,RectF box){
  if(!root.isAttachedToWindow()||root.getWidth()<=0||root.getHeight()<=0||!ValueAnimator.areAnimatorsEnabled())return false;
  ParticleEffect fx=new ParticleEffect(sampled,box);
  if(fx.count==0){fx.image.recycle();return false;}
  fx.setBounds(0,0,root.getWidth(),root.getHeight());
  ValueAnimator anim=ValueAnimator.ofFloat(0,1);anim.setDuration(ParticleMath.DURATION_MS);anim.setInterpolator(new LinearInterpolator());
  View.OnAttachStateChangeListener attach=new View.OnAttachStateChangeListener(){
   public void onViewAttachedToWindow(View v){}
   public void onViewDetachedFromWindow(View v){anim.cancel();}
  };
  anim.addUpdateListener(a->{fx.time=(Float)a.getAnimatedValue();fx.invalidateSelf();});
  anim.addListener(new AnimatorListenerAdapter(){public void onAnimationEnd(Animator a){root.getOverlay().remove(fx);root.removeOnAttachStateChangeListener(attach);}});
  root.getOverlay().add(fx);root.addOnAttachStateChangeListener(attach);
  try{anim.start();return true;}catch(RuntimeException e){root.getOverlay().remove(fx);root.removeOnAttachStateChangeListener(attach);throw e;}
 }
 @Override public void draw(Canvas c){
  float breakup=ParticleMath.breakup(time),collapse=ParticleMath.collapse(time);
  if(breakup<1){
   paint.setColor(Color.WHITE);paint.setAlpha(Math.round(255*(1-breakup)));
   c.save();c.scale(collapse,collapse,cx,cy);c.drawBitmap(image,null,box,paint);c.restore();
  }
  for(int i=0;i<count;i++){
   float p=ParticleMath.progress(time,delay[i]);float age=ParticleMath.clamp(p/life[i]);if(age>=1)continue;
   float travel=ParticleMath.spread(p);
   float xx=cx+x[i]*collapse+vx[i]*travel+(float)Math.sin(phase[i]+p*4)*size*.022f*p;
   float yy=cy+y[i]*collapse+vy[i]*travel+size*.028f*p*p;
   float r=radius[i]*ParticleMath.radius(age);
   int alpha=Math.round(Color.alpha(colors[i])*ParticleMath.fade(age)*breakup);
   paint.setColor(colors[i]);paint.setAlpha(alpha);
   if((i&3)==0)c.drawRect(xx-r,yy-r,xx+r,yy+r,paint);else c.drawCircle(xx,yy,r,paint);
   if(spark[i]&&p>.045f){
    float twinkle=.30f+.70f*(float)Math.abs(Math.sin(phase[i]+p*22));
    paint.setColor(Color.WHITE);paint.setAlpha((int)(alpha*twinkle*.82f));c.drawCircle(xx,yy,Math.max(.33f,r*.65f),paint);
   }
  }
 }
 public void setAlpha(int a){} public void setColorFilter(ColorFilter f){} public int getOpacity(){return PixelFormat.TRANSLUCENT;}
}
