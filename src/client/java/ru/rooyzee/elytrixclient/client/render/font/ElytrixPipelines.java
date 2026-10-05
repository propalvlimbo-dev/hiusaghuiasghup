package ru.rooyzee.elytrixclient.client.render.font;

import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.pipeline.BindGroupLayout;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.CompareOp;
import com.mojang.blaze3d.shaders.UniformType;
import net.minecraft.client.renderer.BindGroupLayouts;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

/**
 * Кастомные RenderPipeline для ElytrixClient.
 * DynamicTransforms = ProjMat + ModelViewMat + ColorModulator.
 */
public final class ElytrixPipelines {

    private static final BindGroupLayout MTSDF_LAYOUT = BindGroupLayout.builder()
            .withUniform("DynamicTransforms", UniformType.UNIFORM_BUFFER)
            .withSampler("Sampler0")
            .build();

    private static RenderPipeline textPipeline;
    private static boolean initFailed = false;

    public static RenderPipeline getText() {
        if (initFailed) return RenderPipelines.GUI_TEXTURED;
        if (textPipeline != null) return textPipeline;
        try {
            textPipeline = RenderPipeline.builder()
                    .withLocation(Identifier.fromNamespaceAndPath("elytrixclient", "pipeline/text"))
                    .withVertexShader(Identifier.fromNamespaceAndPath("elytrixclient", "core/text"))
                    .withFragmentShader(Identifier.fromNamespaceAndPath("elytrixclient", "core/text"))
                    .withBindGroupLayout(MTSDF_LAYOUT)
                    .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
                    .withDepthStencilState(new DepthStencilState(CompareOp.ALWAYS_PASS, false))
                    .withVertexBinding(0, ElytrixVertexFormats.UI)
                    .withPrimitiveTopology(PrimitiveTopology.QUADS)
                    .withCull(false)
                    .build();
            System.out.println("[Elytrix] MTSDF pipeline registered OK");
        } catch (Exception e) {
            System.err.println("[Elytrix] MTSDF pipeline failed: " + e);
            e.printStackTrace();
            initFailed = true;
            return RenderPipelines.GUI_TEXTURED;
        }
        return textPipeline;
    }

    private ElytrixPipelines() {}
}