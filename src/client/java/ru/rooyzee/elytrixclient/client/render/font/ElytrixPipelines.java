package ru.rooyzee.elytrixclient.client.render.font;

import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.pipeline.BindGroupLayout;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.CompareOp;
import net.minecraft.client.renderer.BindGroupLayouts;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

import java.lang.reflect.Field;

/**
 * Кастомные RenderPipeline для ElytrixClient.
 * Lazy init — не крашит игру если reflection не сработает.
 */
public final class ElytrixPipelines {

    private static RenderPipeline textPipeline;
    private static boolean initFailed = false;

    public static RenderPipeline getText() {
        if (initFailed) return RenderPipelines.GUI_TEXTURED;
        if (textPipeline != null) return textPipeline;
        try {
            textPipeline = builder("text", true).build();
            System.out.println("[Elytrix] MTSDF pipeline registered OK");
        } catch (Exception e) {
            System.err.println("[Elytrix] MTSDF pipeline failed: " + e);
            initFailed = true;
            return RenderPipelines.GUI_TEXTURED;
        }
        return textPipeline;
    }

    private static RenderPipeline.Snippet getGuiSnippet() {
        try {
            Field f = RenderPipelines.class.getDeclaredField("GUI_SNIPPET");
            f.setAccessible(true);
            return (RenderPipeline.Snippet) f.get(null);
        } catch (Exception e) {
            throw new RuntimeException("Failed to get GUI_SNIPPET via reflection", e);
        }
    }

    private static RenderPipeline.Builder builder(String name, boolean sampled) {
        RenderPipeline.Builder builder = RenderPipeline.builder(getGuiSnippet())
                .withLocation(Identifier.fromNamespaceAndPath("elytrixclient", "pipeline/" + name))
                .withVertexShader(Identifier.fromNamespaceAndPath("elytrixclient", "core/" + name))
                .withFragmentShader(Identifier.fromNamespaceAndPath("elytrixclient", "core/" + name))
                .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
                .withDepthStencilState(new DepthStencilState(CompareOp.ALWAYS_PASS, false))
                .withVertexBinding(0, ElytrixVertexFormats.UI)
                .withPrimitiveTopology(PrimitiveTopology.QUADS)
                .withCull(false);
        if (sampled) {
            builder.withBindGroupLayout(BindGroupLayouts.SAMPLER0);
        }
        return builder;
    }

    private ElytrixPipelines() {}
}