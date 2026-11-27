package dev.s23hype.deletefx;
public class ParticleMathTest {
 public static void main(String[] args){
 check(ParticleMath.DURATION_MS==480);check(ParticleMath.spread(0)==0);check(ParticleMath.spread(1)==1);
 check(ParticleMath.fade(0)==1);check(ParticleMath.fade(1)==0);check(ParticleMath.breakup(0)==0);check(ParticleMath.breakup(1)==1);
 float prev=0,alpha=1;for(int i=0;i<=1000;i++){float t=i/1000f;float f=ParticleMath.fade(t),s=ParticleMath.spread(t);check(s>=prev);check(f<=alpha);check(ParticleMath.radius(t)>0);prev=s;alpha=f;}
 System.out.println("ParticleMath tests passed");
 }
 static void check(boolean ok){if(!ok)throw new AssertionError();}
}
