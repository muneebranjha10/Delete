package dev.s23hype.deletefx;
final class ParticleMath {
 static final long DURATION_MS=850;
 static float clamp(float t){return Math.max(0f,Math.min(1f,t));}
 static float progress(float t,float delay){return clamp((t-delay)/(1f-delay));}
 static float spread(float t){float q=1-clamp(t);return 1-q*q;}
 static float fade(float t){float q=clamp((t-.32f)/.68f);return 1-q*q*(3-2*q);}
 static float radius(float t){return 1f-.36f*clamp(t);}
 static float collapse(float t){return 1f-.12f*clamp(t/.08f);}
 static float breakup(float t){return clamp((t-.025f)/.10f);}
 private ParticleMath(){}
}
