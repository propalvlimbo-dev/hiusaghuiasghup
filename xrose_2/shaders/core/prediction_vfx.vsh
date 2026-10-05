#version 330 core

layout(location = 0) in vec3 Position;
layout(location = 1) in vec2 UV0;
layout(location = 2) in vec4 Color;
layout(location = 3) in vec3 Normal;

layout(std140) uniform Projection {
    mat4 ProjMat;
};

out vec3 vViewPos;
out vec3 vNormal;
out vec2 vUv;
out vec4 vColor;

void main() {
    vViewPos = Position;
    vUv = UV0;
    vColor = Color;
    vNormal = normalize(Normal);
    gl_Position = ProjMat * vec4(Position, 1.0);
}