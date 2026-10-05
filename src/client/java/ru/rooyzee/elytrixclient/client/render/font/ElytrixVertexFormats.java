package ru.rooyzee.elytrixclient.client.render.font;

import com.mojang.blaze3d.GpuFormat;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;

/**
 * Кастомный vertex format для MTSDF UI рендеринга.
 * Портирован из delta-26.2 DeltaVertexFormats.
 *
 * Position(vec3) + Color(vec4) + UV0(vec2) + UV1(ivec2) + UV2(ivec2) + Normal(vec4) + LineWidth(float)
 */
public final class ElytrixVertexFormats {
    public static final VertexFormat UI = VertexFormat.builder(0)
            .addAttribute(DefaultVertexFormat.POSITION_SEMANTIC_NAME, GpuFormat.RGB32_FLOAT)
            .addAttribute(DefaultVertexFormat.COLOR_SEMANTIC_NAME, GpuFormat.RGBA8_UNORM)
            .addAttribute(DefaultVertexFormat.UV0_SEMANTIC_NAME, GpuFormat.RG32_FLOAT)
            .addAttribute(DefaultVertexFormat.UV1_SEMANTIC_NAME, GpuFormat.RG16_SINT)
            .addAttribute(DefaultVertexFormat.UV2_SEMANTIC_NAME, GpuFormat.RG16_SINT)
            .addAttribute(DefaultVertexFormat.NORMAL_SEMANTIC_NAME, GpuFormat.RGBA8_SNORM)
            .addAttribute(DefaultVertexFormat.LINE_WIDTH_SEMANTIC_NAME, GpuFormat.R32_FLOAT)
            .build();

    private ElytrixVertexFormats() {}
}