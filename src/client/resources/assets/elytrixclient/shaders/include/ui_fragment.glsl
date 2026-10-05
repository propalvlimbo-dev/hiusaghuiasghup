// xrose 2D UI fragment helpers.

// Screen-pixel range for MTSDF rendering.
float ui_screenPxRange(vec2 uv, vec2 texSize, float pxRange) {
    vec2 unitRange = vec2(pxRange) / texSize;
    vec2 screenTexSize = vec2(1.0) / fwidth(uv);
    return max(0.5 * dot(unitRange, screenTexSize), 1.0);
}