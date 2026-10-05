#version 330 core

in vec2 uv;
out vec4 finalColor;

layout(std140) uniform BlockOutlineStyle {
    vec4 Tint;
    vec4 Params;
    vec4 AuroraAColor;
    vec4 AuroraBColor;
    vec4 StarColor;
};

#moj_import <xrose:block_outline_common.glsl>

vec3 decodeDirection(vec2 encoded) {
    vec2 value = encoded * 2.0 - 1.0;
    value.y = -value.y;
    vec3 direction = vec3(
        value.x,
        value.y,
        1.0 - abs(value.x) - abs(value.y)
    );
    float correction = clamp(-direction.z, 0.0, 1.0);
    direction.x += direction.x >= 0.0 ? -correction : correction;
    direction.y += direction.y >= 0.0 ? -correction : correction;
    return normalize(direction);
}

void main() {
    vec3 direction = decodeDirection(uv);
    float time = Params.z;
    vec3 position = direction * 2.2;

    vec3 color = vec3(0.010, 0.014, 0.030);

    vec3 starTint = StarColor.rgb;
    color += starTint * starGlow(direction, 190.0, 0.10, time);
    color += mix(starTint, vec3(1.0, 0.97, 0.92), 0.35)
            * starGlow(direction * 1.6 + 23.0, 120.0, 0.05, time * 1.35)
            * 1.5;

    float up = clamp(direction.y * 0.5 + 0.5, 0.0, 1.0);
    float drift = time * 0.045;
    float sway = fbm(position * 0.8 + vec3(drift, -drift * 0.6, drift * 0.3));
    float curtainA = smoothstep(0.42, 0.78, sway);
    float swayB = fbm(position * 1.6 + vec3(-drift * 0.7, drift * 0.4, 4.7));
    float curtainB = smoothstep(0.50, 0.85, swayB);

    vec3 auroraA = mix(AuroraAColor.rgb, AuroraBColor.rgb, fbm(position * 0.5 + 11.0));
    vec3 auroraB = mix(AuroraBColor.rgb, AuroraAColor.rgb * 0.6 + AuroraBColor.rgb * 0.4, fbm(position * 0.7 + 29.0));

    float vertical = 0.35 + 0.65 * up;
    color += auroraA * curtainA * vertical * 1.25;
    color += auroraB * curtainB * vertical * 0.85;
    color += AuroraAColor.rgb * pow(sway, 3.0) * 0.18;

    color *= Params.w;
    color = color / (1.0 + color);
    color = pow(color, vec3(0.82)) + ditherRgb(gl_FragCoord.xy);
    finalColor = vec4(clamp(color, 0.0, 1.0) * Tint.rgb, Tint.a);
}
