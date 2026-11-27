package dev.s23hype.deletefx;
final class ParticleMath {
 static final long DURATION_MS=850;
 static float clamp(float t){return Math.max(0f,Math.min(1f,t));}
 static float progress(float t,float delay){return clamp((t-delay)/(1f-delay));}
 static float spread(float t){float q=1-clamp(t);return 1-q*q;}
 static float fade(float t){float q=clamp((t-.32f)/.68f);return 1-q*q*(3-2*q);}
 static float radius(float t){return 1f-.36f*clamp(t);}
 static float collapse(float t){return t<.065f?1f+.045f*(float)Math.sin(clamp(t/.065f)*Math.PI*.5f):1.045f-.115f*clamp((t-.065f)/.09f);}
 static float breakup(float t){return clamp((t-.065f)/.10f);}
 // Tint a highlight toward white without destroying the source hue.
 static int highlight(int argb){int a=argb>>>24,r=(argb>>>16)&255,g=(argb>>>8)&255,b=argb&255;int max=Math.max(r,Math.max(g,b)),min=Math.min(r,Math.min(g,b));float mix=max-min>45?.24f:.46f;return (a<<24)|(Math.round(r+(255-r)*mix)<<16)|(Math.round(g+(255-g)*mix)<<8)|Math.round(b+(255-b)*mix);}
 private ParticleMath(){}
}
