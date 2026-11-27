package dev.s23hype.deletefx;
public class ParticleMathTest {
 public static void main(String[] args){
 check(ParticleMath.DURATION_MS==850);check(ParticleMath.spread(0)==0);check(ParticleMath.spread(1)==1);
 check(ParticleMath.fade(0)==1);check(ParticleMath.fade(1)==0);check(ParticleMath.breakup(0)==0);check(ParticleMath.breakup(1)==1);
 float prev=0,alpha=1;for(int i=0;i<=1000;i++){float t=i/1000f;float f=ParticleMath.fade(t),s=ParticleMath.spread(t);check(s>=prev);check(f<=alpha);check(ParticleMath.radius(t)>0);prev=s;alpha=f;}
 check(ParticleMath.collapse(.065f)>1f);check(ParticleMath.collapse(.2f)<1f);
 int purple=ParticleMath.highlight(0xff673ab7);check((purple&255)>((purple>>>16)&255));check(((purple>>>16)&255)>((purple>>>8)&255));
 int red=ParticleMath.highlight(0xffd92d20);check(((red>>>16)&255)>((red>>>8)&255));check(ParticleMath.highlight(0xffffffff)==0xffffffff);
 System.out.println("ParticleMath tests passed");
 }
 static void check(boolean ok){if(!ok)throw new AssertionError();}
}
