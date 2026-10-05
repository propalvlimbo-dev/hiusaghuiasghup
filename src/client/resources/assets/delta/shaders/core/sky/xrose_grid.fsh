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
// Перенос сетки в духе xrose color_grid.fsh на интерфейс дельтовского sky-шейдера.
float hash21(vec2 v){ v = fract(v*vec2(123.34,345.45)); v += dot(v,v+34.345); return fract(v.x*v.y); }
void main(){
    float depth = texture(DepthTexture, uv).r;
    float skyMask = 1.0 - step(0.00001, depth);
    vec3 scene = texture(ScreenTexture, uv).rgb;
    if(skyMask < 0.5){ fragColor = vec4(scene,1.0); return; }
    float t = Time * max(Speed,0.1);
    float aspect = Resolution.x / max(Resolution.y,1.0);
    vec2 gridUv = uv * vec2(aspect,1.0) * max(Scale,1.0) * 12.0;
    vec2 g = floor(gridUv);
    vec2 f = fract(gridUv);
    float edge = min(min(f.x,1.0-f.x),min(f.y,1.0-f.y));
    float line = 1.0 - smoothstep(0.0,0.06,edge);
    float pulse = 0.5 + 0.5*sin(t*2.0 + hash21(g)*6.28318);
    vec3 tint = max(Color, vec3(0.05));
    vec3 col = tint * (line*(0.35+0.65*pulse) + pulse*0.06);
    fragColor = vec4(mix(scene, col, clamp(Alpha,0.0,1.0)), 1.0);
}
