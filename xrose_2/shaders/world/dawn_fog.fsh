#version 330 core

in vec2 uv;
out vec4 finalColor;

uniform sampler2D SceneSampler;
uniform sampler2D DepthSampler;

layout(std140) uniform DawnFogUniforms {
    mat4 u_InverseProjectionMatrix;
    mat4 u_InverseViewMatrix;
    vec4 u_CameraPosRaw;        // xyz = camera position
    vec4 u_TimeRaw;             // x = time, y = depth is 0..1 (zzero-to-one)
    vec4 u_ResolutionRaw;       // xy = resolution, z = fog density
    vec4 u_SunDirectionRaw;     // xyz = sun direction, w = fog min height
    vec4 u_FogMaxRaw;           // x = fog max height, y = view distance
    vec4 u_PaletteZenithRaw;
    vec4 u_PaletteHorizonWarmRaw;
    vec4 u_PaletteHorizonCoolRaw;
    vec4 u_PaletteFogWarmRaw;
    vec4 u_PaletteFogCoolRaw;
    vec4 u_PaletteRayRaw;
    vec4 u_SunScreenRaw;        // xyz = sun screen pos + visibility, w = rainbow
    vec4 u_RainbowDirRaw;       // xyz = rainbow direction, w = rainbow size
    vec4 u_GodRaysRaw;          // x = god rays, y = softness
    vec4 u_StyleRaw;            // x = stars, y = aurora, z = moon mode, w = sun glow
};

#define u_ScreenTexture SceneSampler
#define u_DepthTexture DepthSampler
#define u_CameraPos u_CameraPosRaw.xyz
#define u_Time u_TimeRaw.x
#define u_ZZeroToOne u_TimeRaw.y
#define u_Resolution u_ResolutionRaw.xy
#define u_FogDensity u_ResolutionRaw.z
#define u_SunDirection u_SunDirectionRaw.xyz
#define u_FogMinHeight u_SunDirectionRaw.w
#define u_FogMaxHeight u_FogMaxRaw.x
#define u_ViewDistance u_FogMaxRaw.y
#define u_Rainbow u_SunScreenRaw.w
#define u_SunScreen u_SunScreenRaw.xyz
#define u_RainbowDir u_RainbowDirRaw.xyz
#define u_RainbowSize u_RainbowDirRaw.w
#define u_GodRays u_GodRaysRaw.x
#define u_Softness u_GodRaysRaw.y
#define u_Stars u_StyleRaw.x
#define u_Aurora u_StyleRaw.y
#define u_MoonMode u_StyleRaw.z
#define u_SunGlow u_StyleRaw.w
#define u_PaletteZenith u_PaletteZenithRaw.xyz
#define u_PaletteHorizonWarm u_PaletteHorizonWarmRaw.xyz
#define u_PaletteHorizonCool u_PaletteHorizonCoolRaw.xyz
#define u_PaletteFogWarm u_PaletteFogWarmRaw.xyz
#define u_PaletteFogCool u_PaletteFogCoolRaw.xyz
#define u_PaletteRay u_PaletteRayRaw.xyz

const float FAR_EPS = 0.0001;

const vec2 POISSON[8] = vec2[8](
    vec2(-0.326, -0.406), vec2(-0.840, -0.074), vec2(-0.696, 0.457), vec2(-0.203, 0.621),
    vec2(0.962, -0.195), vec2(0.473, -0.480), vec2(0.519, 0.767), vec2(0.185, -0.893)
);

float saturate(float v) {
    return clamp(v, 0.0, 1.0);
}

float luminance(vec3 c) {
    return dot(c, vec3(0.2126, 0.7152, 0.0722));
}

float hash12(vec2 p) {
    vec3 p3 = fract(vec3(p.xyx) * 0.1031);
    p3 += dot(p3, p3.yzx + vec3(33.33));
    return fract((p3.x + p3.y) * p3.z);
}

float hash11(float p) {
    p = fract(p * 0.1031);
    p *= p + 33.33;
    p *= p + p;
    return fract(p);
}

vec3 hash33(vec3 p3) {
    p3 = fract(p3 * vec3(0.1031, 0.1030, 0.0973));
    p3 += dot(p3, p3.yxz + 33.33);
    return fract((p3.xxy + p3.yxx) * p3.zyx);
}

vec3 srgbToLinear(vec3 c) {
    return vec3(
        c.r <= 0.04045 ? c.r / 12.92 : pow((c.r + 0.055) / 1.055, 2.4),
        c.g <= 0.04045 ? c.g / 12.92 : pow((c.g + 0.055) / 1.055, 2.4),
        c.b <= 0.04045 ? c.b / 12.92 : pow((c.b + 0.055) / 1.055, 2.4)
    );
}

vec3 linearToSrgb(vec3 c) {
    c = clamp(c, 0.0, 1.0);
    return vec3(
        c.r <= 0.0031308 ? c.r * 12.92 : 1.055 * pow(c.r, 0.4166666666666667) - 0.055,
        c.g <= 0.0031308 ? c.g * 12.92 : 1.055 * pow(c.g, 0.4166666666666667) - 0.055,
        c.b <= 0.0031308 ? c.b * 12.92 : 1.055 * pow(c.b, 0.4166666666666667) - 0.055
    );
}

vec3 linearToOklab(vec3 c) {
    float l = 0.4122214708 * c.r + 0.5363325363 * c.g + 0.0514459929 * c.b;
    float m = 0.2119034982 * c.r + 0.6806995451 * c.g + 0.1073969566 * c.b;
    float s = 0.0883024619 * c.r + 0.2817188376 * c.g + 0.6299787005 * c.b;
    float l_ = sign(l) * pow(abs(l), 0.3333333333333333);
    float m_ = sign(m) * pow(abs(m), 0.3333333333333333);
    float s_ = sign(s) * pow(abs(s), 0.3333333333333333);
    return vec3(
        0.2104542553 * l_ + 0.7936177850 * m_ - 0.0040720468 * s_,
        1.9779984951 * l_ - 2.4285922050 * m_ + 0.4505937099 * s_,
        0.0259040371 * l_ + 0.7827717662 * m_ - 0.8086757660 * s_
    );
}

vec3 oklabToLinear(vec3 c) {
    float l_ = c.x + 0.3963377774 * c.y + 0.2158037573 * c.z;
    float m_ = c.x - 0.1055613458 * c.y - 0.0638541728 * c.z;
    float s_ = c.x - 0.0894841775 * c.y - 1.2914855480 * c.z;
    float l = l_ * l_ * l_;
    float m = m_ * m_ * m_;
    float s = s_ * s_ * s_;
    return vec3(
        4.0767416621 * l - 3.3077115913 * m + 0.2309699292 * s,
        -1.2684380046 * l + 2.6097574011 * m - 0.3413193965 * s,
        -0.0041960863 * l - 0.7034186147 * m + 1.7076147010 * s
    );
}

vec3 oklabMix(vec3 a, vec3 b, float t) {
    vec3 la = linearToOklab(srgbToLinear(a));
    vec3 lb = linearToOklab(srgbToLinear(b));
    return linearToSrgb(oklabToLinear(mix(la, lb, saturate(t))));
}

vec4 permute4(vec4 x) {
    return mod(((x * 34.0) + 1.0) * x, 289.0);
}

vec4 taylorInvSqrt4(vec4 r) {
    return 1.79284291400159 - 0.85373472095314 * r;
}

float snoise(vec3 v) {
    const vec2 C = vec2(1.0 / 6.0, 1.0 / 3.0);
    const vec4 D = vec4(0.0, 0.5, 1.0, 2.0);
    vec3 i = floor(v + dot(v, C.yyy));
    vec3 x0 = v - i + dot(i, C.xxx);
    vec3 g = step(x0.yzx, x0.xyz);
    vec3 l = 1.0 - g;
    vec3 i1 = min(g.xyz, l.zxy);
    vec3 i2 = max(g.xyz, l.zxy);
    vec3 x1 = x0 - i1 + C.xxx;
    vec3 x2 = x0 - i2 + 2.0 * C.xxx;
    vec3 x3 = x0 - 1.0 + 3.0 * C.xxx;
    i = mod(i, 289.0);
    vec4 p = permute4(permute4(permute4(
            i.z + vec4(0.0, i1.z, i2.z, 1.0))
            + i.y + vec4(0.0, i1.y, i2.y, 1.0))
            + i.x + vec4(0.0, i1.x, i2.x, 1.0));
    float n_ = 1.0 / 7.0;
    vec3 ns = n_ * D.wyz - D.xzx;
    vec4 j = p - 49.0 * floor(p * ns.z * ns.z);
    vec4 x_ = floor(j * ns.z);
    vec4 y_ = floor(j - 7.0 * x_);
    vec4 x = x_ * ns.x + ns.yyyy;
    vec4 y = y_ * ns.x + ns.yyyy;
    vec4 h = 1.0 - abs(x) - abs(y);
    vec4 b0 = vec4(x.xy, y.xy);
    vec4 b1 = vec4(x.zw, y.zw);
    vec4 s0 = floor(b0) * 2.0 + 1.0;
    vec4 s1 = floor(b1) * 2.0 + 1.0;
    vec4 sh = -step(h, vec4(0.0));
    vec4 a0 = b0.xzyw + s0.xzyw * sh.xxyy;
    vec4 a1 = b1.xzyw + s1.xzyw * sh.zzww;
    vec3 p0 = vec3(a0.xy, h.x);
    vec3 p1 = vec3(a0.zw, h.y);
    vec3 p2 = vec3(a1.xy, h.z);
    vec3 p3 = vec3(a1.zw, h.w);
    vec4 norm = taylorInvSqrt4(vec4(dot(p0, p0), dot(p1, p1), dot(p2, p2), dot(p3, p3)));
    p0 *= norm.x;
    p1 *= norm.y;
    p2 *= norm.z;
    p3 *= norm.w;
    vec4 m = max(0.6 - vec4(dot(x0, x0), dot(x1, x1), dot(x2, x2), dot(x3, x3)), 0.0);
    m = m * m;
    return 42.0 * dot(m * m, vec4(dot(p0, x0), dot(p1, x1), dot(p2, x2), dot(p3, x3)));
}

float fbm(vec3 p) {
    float sum = 0.6 * snoise(p);
    sum += 0.3 * snoise(p * 2.11 + vec3(19.1, 7.3, 5.2));
    return sum;
}

float starField(vec3 rd, float scale, float density, float time) {
    vec3 p = rd * scale;
    vec3 cell = floor(p);
    float result = 0.0;
    for (int x = -1; x <= 1; x++) {
        for (int y = -1; y <= 1; y++) {
            for (int z = -1; z <= 1; z++) {
                vec3 id = cell + vec3(x, y, z);
                if (length(p - id - 0.5) > 1.35) {
                    continue;
                }
                vec3 h = hash33(id);
                if (h.x > density) {
                    continue;
                }
                vec3 center = id + 0.2 + 0.6 * h;
                float d = length(p - center);
                float brightness = pow(hash11(h.y + 1.7), 4.0);
                float twinkle = 0.6 + 0.4 * sin(time * 2.0 + h.z * 50.0);
                float core = smoothstep(0.06, 0.0, d);
                float halo = exp(-d * 12.0) * 0.5;
                result += (core + halo) * (0.4 + brightness * 2.6) * twinkle;
            }
        }
    }
    return result;
}

vec3 milkyWay(vec3 rd, float time) {
    vec3 plane = normalize(vec3(0.38, 0.82, -0.43));
    float d = dot(rd, plane);
    float band = exp(-d * d * 26.0);
    if (band < 0.004) {
        return vec3(0.0);
    }
    float wisps = fbm(rd * 6.5 + vec3(time * 0.008, 0.0, time * 0.005));
    float lanes = fbm(rd * 14.0 - vec3(0.0, time * 0.01, 0.0));
    float dust = smoothstep(0.30, 0.85, wisps * 0.7 + band * 0.45)
            * (0.35 + 0.65 * smoothstep(0.2, 0.8, lanes));
    vec3 tint = mix(vec3(0.42, 0.50, 0.86), vec3(0.68, 0.58, 0.84), saturate(fbm(rd * 3.0 + 7.0)));
    return tint * band * dust * 0.5;
}

vec3 auroraCurtains(vec3 rd, float time) {
    float up = rd.y;
    float env = smoothstep(0.05, 0.30, up) * (1.0 - smoothstep(0.40, 0.88, up));
    if (env < 0.003) {
        return vec3(0.0);
    }
    vec2 h = rd.xz * 2.3;
    float sway = snoise(vec3(h.x, time * 0.07, h.y));
    float curtain = snoise(vec3(h.x * 1.7 + sway, time * 0.045, h.y * 1.7 + sway * 0.6));
    float strands = snoise(vec3(h.x * 3.1 + sway * 1.8, up * 12.0 - time * 0.24, h.y * 3.1 + sway * 1.2));
    float energy = saturate(curtain * 0.55 + strands * 0.70 + 0.18);
    float rays = pow(energy, 2.4);
    float pulse = 0.78 + 0.22 * sin(time * 0.7 + sway * 3.0);
    vec3 col = mix(vec3(0.10, 0.92, 0.46), vec3(0.26, 0.60, 1.0), saturate(up * 1.9));
    col = mix(col, vec3(0.70, 0.32, 0.94), saturate((up - 0.38) * 2.1));
    return col * env * rays * pulse;
}

vec3 spectral(float t) {
    t = clamp(t, 0.0, 1.0);
    vec3 c = mix(vec3(0.54, 0.30, 0.89), vec3(0.25, 0.50, 0.95), saturate(t * 4.0));
    c = mix(c, vec3(0.25, 0.85, 0.45), saturate(t * 4.0 - 1.0));
    c = mix(c, vec3(0.98, 0.90, 0.30), saturate(t * 4.0 - 2.0));
    c = mix(c, vec3(0.95, 0.28, 0.22), saturate(t * 4.0 - 3.0));
    return c;
}

vec3 worldPosFromDepth(vec2 uv, float depth) {
    float clipZ = u_ZZeroToOne > 0.5 ? depth : depth * 2.0 - 1.0;
    vec4 clipSpace = vec4(uv * 2.0 - 1.0, clipZ, 1.0);
    vec4 viewSpace = u_InverseProjectionMatrix * clipSpace;
    float invW = abs(viewSpace.w) > 0.000001 ? 1.0 / viewSpace.w : 1.0;
    viewSpace *= invW;
    return (u_InverseViewMatrix * viewSpace).xyz;
}

float heightIntegral(vec3 ro, vec3 rd, float dist) {
    float thickness = max(u_FogMaxHeight - u_FogMinHeight, 1.0);
    float k = 2.0 / thickness;
    float rdy = abs(rd.y) < 0.00002 ? (rd.y < 0.0 ? -0.00002 : 0.00002) : rd.y;
    float base = exp(-clamp(k * max(ro.y - u_FogMinHeight, 0.0), 0.0, 60.0));
    float travel = 1.0 - exp(clamp(-k * rdy * dist, -60.0, 60.0));
    return clamp(base * travel / (k * rdy), 0.0, dist);
}

float henyeyGreenstein(float mu, float g) {
    float g2 = g * g;
    return (1.0 - g2) / pow(max(1.0 + g2 - 2.0 * g * mu, 0.0001), 1.5);
}

float godRaysMask(vec2 uv, vec2 sunUv, float jitter) {
    vec2 stepUv = (sunUv - uv) / 16.0;
    vec2 pos = uv + stepUv * jitter;
    float sum = 0.0;
    float weight = 1.0;
    float total = 0.0;
    for (int i = 0; i < 16; i++) {
        vec2 s = clamp(pos, vec2(0.001), vec2(0.999));
        sum += (1.0 - step(FAR_EPS, texture(u_DepthTexture, s).r)) * weight;
        total += weight;
        weight *= 0.925;
        pos += stepUv;
    }
    return sum / max(total, 0.0001);
}

vec3 sampleSceneSoft(vec2 uv, float radiusPx, float rotation) {
    vec3 center = texture(u_ScreenTexture, uv).rgb;
    if (radiusPx < 0.5) {
        return center;
    }
    vec2 px = vec2(radiusPx) / max(u_Resolution, vec2(1.0));
    float ca = cos(rotation);
    float sa = sin(rotation);
    mat2 basis = mat2(ca, sa, -sa, ca);
    vec3 acc = center * 0.2;
    for (int i = 0; i < 8; i++) {
        vec2 s = clamp(uv + basis * POISSON[i] * px, vec2(0.001), vec2(0.999));
        acc += texture(u_ScreenTexture, s).rgb * 0.1;
    }
    return acc;
}

vec3 atmoSky(vec3 rd, float mu) {
    float up = saturate(rd.y);
    float below = saturate(-rd.y);
    vec3 grad = oklabMix(u_PaletteHorizonCool, u_PaletteZenith, pow(up, 0.58));
    float band = pow(1.0 - up, 3.2);
    float sunNear = pow(saturate(mu * 0.5 + 0.5), 3.0);
    grad = oklabMix(grad, u_PaletteHorizonWarm, saturate(sunNear * (0.30 + 0.70 * band)));
    grad += u_PaletteRay * henyeyGreenstein(mu, 0.62) * 0.045;
    grad = mix(grad, u_PaletteHorizonCool * 0.9, below * 0.8);
    return grad;
}

void main() {
    vec2 uv = gl_FragCoord.xy / max(u_Resolution, vec2(1.0));
    float depth = texture(u_DepthTexture, uv).r;
    float validDepth = step(FAR_EPS, depth);
    float skyMask = 1.0 - validDepth;

    vec3 ro = u_CameraPos;
    vec3 farPoint = worldPosFromDepth(uv, FAR_EPS);
    vec3 rd = normalize(farPoint - ro + vec3(0.0, 0.0000001, 0.0));
    vec3 surfacePos = worldPosFromDepth(uv, max(depth, FAR_EPS));
    float sceneDist = mix(2600.0, length(surfacePos - ro), validDepth);

    float edgeFade = smoothstep(0.0, 7.5, sceneDist);

    float midY = (u_FogMinHeight + u_FogMaxHeight) * 0.5;
    float toMid = abs(midY - ro.y) / max(abs(rd.y), 0.04);
    float probeDist = clamp(min(toMid, sceneDist * 0.5), 6.0, 220.0);
    float caveFade = smoothstep(u_FogMinHeight - 40.0, u_FogMinHeight - 12.0, ro.y);
    float densityMod = 1.0;
    if (caveFade > 0.0001) {
        vec3 probe = ro + rd * probeDist;
        vec3 drift = vec3(u_Time * 1.05, u_Time * 0.16, u_Time * 0.58);
        float macro = fbm(probe * 0.045 + drift * 0.6);
        float sheets = snoise(vec3(probe.x * 0.052, probe.y * 0.155, probe.z * 0.052) + drift * 0.35);
        vec3 probeNear = ro + rd * min(probeDist * 0.45, 60.0);
        float detail = snoise(probeNear * 0.21 - drift * 0.5);
        float turbulence = macro * 0.62 + sheets * 0.28 + detail * 0.10;
        densityMod = mix(0.55, 1.50, saturate(turbulence * 0.5 + 0.5));
        densityMod = mix(densityMod, 1.0, smoothstep(120.0, 220.0, probeDist));
    }
    float sigma = u_FogDensity * 0.058 * densityMod * caveFade;
    float fogGeom = saturate((1.0 - exp(-sigma * heightIntegral(ro, rd, min(sceneDist, 900.0)))) * edgeFade);
    float fogSky = saturate(1.0 - exp(-sigma * heightIntegral(ro, rd, 4000.0)));
    float fogAmount = mix(fogSky, fogGeom, validDepth);

    vec3 sun = normalize(u_SunDirection);
    float mu = dot(rd, sun);
    float lowSun = saturate(1.0 - abs(sun.y) * 2.2);
    float horizonGlow = pow(saturate(1.0 - abs(rd.y) * 1.30), 2.3);

    vec3 skyCol = atmoSky(rd, mu);

    float sunward = pow(saturate(mu * 0.5 + 0.5), 3.4);
    float vert = saturate(rd.y * 1.9 + 0.55);
    vec3 shadowSide = mix(u_PaletteFogCool * 0.92, u_PaletteFogCool * 1.10, vert);
    vec3 dawnSide = mix(u_PaletteFogWarm, u_PaletteHorizonWarm, pow(saturate(mu), 2.4));
    float bleed = saturate(sunward * (0.30 + 0.70 * lowSun));
    vec3 fogTint = oklabMix(shadowSide, dawnSide, bleed);
    float forward = henyeyGreenstein(mu, 0.56) * 0.085;
    vec3 bloomGlow = u_PaletteRay * forward * (0.35 + 0.65 * lowSun) * horizonGlow;
    fogTint += bloomGlow * 0.85;
    float tintLum = luminance(fogTint);
    fogTint /= 1.0 + max(tintLum - 0.865, 0.0) * 2.4;

    float viewRange = max(u_ViewDistance, 64.0);
    float farHaze = smoothstep(viewRange * 0.45, viewRange * 0.92, sceneDist) * validDepth;
    farHaze *= mix(0.75, 1.0, pow(1.0 - saturate(rd.y), 3.2));

    float skyBlend = saturate(0.42 + 0.58 * pow(1.0 - saturate(rd.y), 2.6));

    float blurDrive = max(fogAmount, max(farHaze * 0.7, skyMask * skyBlend * 0.45));
    float blurRadius = u_Softness * 7.5 * blurDrive;
    float rotation = hash12(gl_FragCoord.xy) * 6.2831853;
    vec3 scene = sampleSceneSoft(uv, blurRadius, rotation);

    vec3 color = mix(scene, skyCol, skyMask * skyBlend);
    vec3 veiled = color * mix(vec3(1.0), vec3(0.90, 0.94, 1.03), fogAmount * 0.30);
    color = mix(veiled, fogTint, fogAmount);
    color = mix(color, skyCol, farHaze * 0.92);

    float rays = 0.0;
    if (u_GodRays > 0.001 && u_SunScreen.z > 0.001) {
        float jitter = hash12(gl_FragCoord.yx + vec2(17.0, 59.0));
        float mask = godRaysMask(uv, u_SunScreen.xy, jitter);
        float angular = pow(saturate(mu * 0.5 + 0.5), 3.0);
        rays = mask * angular * u_SunScreen.z * u_GodRays;
        rays *= 0.45 + 0.55 * saturate(fogAmount + skyMask * 0.85 + farHaze);
        rays *= mix(1.0, 0.6, u_MoonMode);
    }
    vec3 rayGlow = u_PaletteRay * rays * 0.85;

    float canvas = saturate(skyMask + farHaze);
    vec3 celestial = vec3(0.0);
    if (u_SunScreen.z > 0.001 && canvas > 0.002) {
        float vis = u_SunScreen.z * (0.35 + 0.65 * lowSun);
        float angSq = max(2.0 * (1.0 - mu), 0.0);
        if (u_MoonMode > 0.5) {
            float chord = sqrt(angSq);
            float discR = 0.055;
            float limbFade = 1.0 - smoothstep(discR * 0.82, discR, chord);
            if (limbFade > 0.002) {
                vec3 axis = sun;
                vec3 t1 = normalize(cross(axis, vec3(0.0, 1.0, 0.0)));
                vec3 t2 = cross(axis, t1);
                vec2 local = vec2(dot(rd, t1), dot(rd, t2)) / max(cos(chord), 0.25) / discR;
                float rLocal = length(local);
                float mare = smoothstep(0.40, 0.75, fbm(vec3(local * 1.5, 3.71)));
                float crater = fbm(vec3(local * 3.2, 8.13));
                float shade = mix(1.0, 0.80, mare) * mix(1.0, 0.90, crater);
                float limbDark = mix(1.0, 0.86, smoothstep(0.35, 1.0, rLocal));
                celestial += vec3(0.93, 0.95, 1.02) * shade * limbDark * limbFade * limbFade * 1.15;
            }
            float haloIn = exp(-angSq * 300.0);
            float haloMid = exp(-angSq * 52.0);
            float haloWide = exp(-angSq * 7.5);
            celestial += vec3(0.64, 0.72, 0.98)
                    * (haloIn * 0.50 + haloMid * 0.16 + haloWide * 0.05) * (0.55 + 0.45 * vis);
        } else {
            float dy = rd.y - sun.y;
            float dx2 = max(angSq - dy * dy, 0.0);
            float core = exp(-angSq * 9500.0);
            float disc = exp(-angSq * 1350.0);
            float inner = exp(-angSq * 250.0);
            float halo = exp(-angSq * 42.0);
            float streakH = exp(-dy * dy * 2800.0) * exp(-dx2 * 26.0);
            float g = u_SunGlow * vis;
            celestial += vec3(1.0, 0.99, 0.95) * core * 1.4 * g;
            celestial += (u_PaletteRay * 1.15 + vec3(0.22)) * (disc * 0.9 + inner * 0.42) * g;
            celestial += u_PaletteRay * (halo * 0.15 + streakH * 0.32) * g;
        }
    }
    celestial *= canvas;

    vec3 nightSky = vec3(0.0);
    float nightCanvas = u_MoonMode * canvas;
    if (nightCanvas > 0.002 && (u_Stars > 0.001 || u_Aurora > 0.001)) {
        float starVis = nightCanvas * (1.0 - fogAmount * 0.85);
        if (u_Stars > 0.001 && starVis > 0.004) {
            float twinkleTime = u_Time * 1.35;
            float st = starField(rd, 210.0, 0.055, twinkleTime)
                    + starField(rd * 1.7 + 11.0, 130.0, 0.032, twinkleTime * 1.3) * 0.85;
            nightSky += vec3(0.88, 0.93, 1.08) * min(st, 2.2) * u_Stars * starVis;
            nightSky += milkyWay(rd, u_Time) * u_Stars * nightCanvas;
        }
        if (u_Aurora > 0.001 && rd.y > 0.02) {
            nightSky += auroraCurtains(rd, u_Time) * u_Aurora * nightCanvas;
        }
    }

    color = 1.0 - (1.0 - clamp(color, vec3(0.0), vec3(1.0)))
            * (1.0 - clamp(rayGlow + celestial, vec3(0.0), vec3(1.2)));
    color += nightSky;

    if (u_Rainbow > 0.001) {
        float cosA = dot(rd, normalize(u_RainbowDir));
        float ang = degrees(acos(clamp(cosA, -1.0, 1.0)));
        float radius = clamp(u_RainbowSize, 40.0, 64.0);
        float scale = radius / 42.0;
        float width = 3.4 * scale;
        float bandLo = radius - width;
        float rbCanvas = saturate(skyMask + farHaze * 0.8);
        float lift = smoothstep(-0.02, 0.06, rd.y);
        float presence = rbCanvas * lift * (1.0 - fogAmount * 0.9) * u_Rainbow;
        if (presence > 0.001) {
            float inner = 1.0 - smoothstep(bandLo - 6.5, bandLo + 0.4, ang);
            color += vec3(0.95, 0.97, 1.0) * inner * 0.05 * presence;
            float tP = (ang - bandLo) / width;
            if (tP > 0.0 && tP < 1.0) {
                float band = sin(tP * 3.14159265);
                color += mix(spectral(tP), vec3(1.0), 0.20) * band * band * 0.70 * presence;
            }
            float sn = (bandLo - ang) / scale;
            if (sn > 0.0 && sn < 3.6) {
                float fringe = sin(sn * 2.618);
                color += mix(spectral(0.22), vec3(1.0), 0.35) * fringe * fringe * exp(-sn * 1.1) * 0.16 * presence;
            }
        }
    }

    float outLum = luminance(color);
    color /= 1.0 + max(outLum - 0.985, 0.0) * 1.6;
    color += vec3((hash12(gl_FragCoord.xy + vec2(311.7, 74.3)) - 0.5) * 0.0078);

    finalColor = vec4(clamp(color, 0.0, 1.0), 1.0);
}
