#version 330 core
in vec2 uv;
out vec4 fragColor;
uniform sampler2D ScreenTexture;
uniform sampler2D DepthTexture;
layout(std140) uniform SkyUniforms {
    vec2 Resolution;
    float Time;
    vec3 Color;
    float Alpha;
    float Speed;
    float Scale;
    float Intensity;
    vec2 CameraDir;
    float Fov;
};
// Перенос nebula/plasma из xrose menu_background.fsh на интерфейс дельтовского sky-шейдера.
float hash21(vec2 v){ v = fract(v*vec2(123.34,345.45)); v += dot(v,v+34.345); return fract(v.x*v.y); }
float noise(vec2 v){
    vec2 c = floor(v); vec2 l = fract(v); l = l*l*(3.0-2.0*l);
    float a = hash21(c); float b = hash21(c+vec2(1.0,0.0));
    float c2 = hash21(c+vec2(0.0,1.0)); float d = hash21(c+vec2(1.0,1.0));
    return mix(mix(a,b,l.x),mix(c2,d,l.x),l.y);
}
float fbm(vec2 v){ float r=0.0; float amp=0.5; for(int i=0;i<5;i++){ r+=amp*noise(v); v*=2.02; amp*=0.5; } return r; }
vec3 nebula(vec2 p, float t, vec3 tint){
    vec2 pos = p*vec2(3.0,4.0);
    vec2 warp = vec2(fbm(pos*0.8+vec2(t*0.05,0.0)), fbm(pos*0.8+vec2(5.2,t*0.04)));
    pos += (warp-0.5)*2.2;
    float n1 = fbm(pos*1.2);
    float n2 = fbm(pos*2.6+4.0);
    float density = pow(smoothstep(0.30,0.95,n1*0.7+n2*0.3),1.4);
    float hue = fbm(pos*0.6+9.0);
    vec3 tone = mix(tint*0.55, tint*1.7+0.2, smoothstep(0.18,0.85,hue));
    return tone*density*1.3 + tint*pow(n1,2.0)*0.15;
}
vec3 plasma(vec2 p, float t, vec3 tint){
    vec2 pos = p*vec2(6.0,10.0);
    float w1 = noise(pos*0.6+vec2(t*0.45,-t*0.6));
    float w2 = noise(pos*1.3+w1*2.2+vec2(-t*0.35,t*0.5));
    float val = noise(pos+w2*2.4+vec2(t*0.2,-t*0.7));
    float energy = smoothstep(0.15,0.95,val);
    float vein = pow(energy,2.5);
    vec3 col = mix(tint*0.3,tint*1.7,energy);
    col += tint*vein*0.9;
    return col;
}
void main(){
    float depth = texture(DepthTexture, uv).r;
    float skyMask = 1.0 - step(0.00001, depth);
    vec3 scene = texture(ScreenTexture, uv).rgb;
    if(skyMask < 0.5){ fragColor = vec4(scene,1.0); return; }
    float t = Time * max(Speed,0.1);
    vec3 tint = max(Color, vec3(0.05));
    vec3 col = mix(nebula(uv*max(Scale,0.5), t, tint), plasma(uv*max(Scale,0.5), t, tint), 0.35);
    fragColor = vec4(mix(scene, col, clamp(Alpha,0.0,1.0)), 1.0);
}
