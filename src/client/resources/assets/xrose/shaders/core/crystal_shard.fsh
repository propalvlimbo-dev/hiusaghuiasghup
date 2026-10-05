#version 330 core

in vec2 uv;
in vec4 tint;
out vec4 finalColor;

layout(std140) uniform WorldParticleUniforms {
    vec4 params; // x = brightness multiplier
};

void main() {
    // Solid colored shard geometry (Delta's CRYSTAL_FILLED / CRYSTAL_GLOW look):
    // no texture lookup, plain vertex color with alpha.
    finalColor = vec4(tint.rgb * params.x, tint.a);
}
