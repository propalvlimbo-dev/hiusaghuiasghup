package org.xrose.utils.render.chams;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;
import org.xrose.utils.render.post.PostPipelines;
import sdk.api.optimize.optimize;

@optimize
public final class PopChamsRenderTypes {
   private static final Map<PopChamsRenderTypes.Key, RenderType> CACHE = new HashMap<>();

   private PopChamsRenderTypes() {
   }

   public static RenderType model(Identifier texture, boolean textured, boolean additive) {
      PopChamsRenderTypes.Kind kind;
      if (additive) {
         kind = textured ? PopChamsRenderTypes.Kind.ADDITIVE : PopChamsRenderTypes.Kind.ADDITIVE_SOLID;
      } else {
         kind = textured ? PopChamsRenderTypes.Kind.TRANSLUCENT : PopChamsRenderTypes.Kind.TRANSLUCENT_SOLID;
      }

      return get(texture, kind);
   }

   public static RenderType mask(Identifier texture, boolean textured) {
      return get(texture, textured ? PopChamsRenderTypes.Kind.MASK : PopChamsRenderTypes.Kind.MASK_SOLID);
   }

   private static RenderType get(Identifier texture, PopChamsRenderTypes.Kind kind) {
      return CACHE.computeIfAbsent(new PopChamsRenderTypes.Key(texture, kind), key -> {
         RenderSetup setup = RenderSetup.builder(kind.pipeline).withTexture("Sampler0", texture).createRenderSetup();
         return RenderType.create("xrose_popchams_" + kind.name().toLowerCase(), setup);
      });
   }

   private record Key(Identifier texture, PopChamsRenderTypes.Kind kind) {
   }

   private enum Kind {
      ADDITIVE(PostPipelines.POPCHAMS_ADDITIVE),
      ADDITIVE_SOLID(PostPipelines.POPCHAMS_ADDITIVE_SOLID),
      TRANSLUCENT(PostPipelines.POPCHAMS_TRANSLUCENT),
      TRANSLUCENT_SOLID(PostPipelines.POPCHAMS_TRANSLUCENT_SOLID),
      MASK(PostPipelines.POPCHAMS_MASK),
      MASK_SOLID(PostPipelines.POPCHAMS_MASK_SOLID);

      private final RenderPipeline pipeline;

      Kind(RenderPipeline pipeline) {
         this.pipeline = pipeline;
      }

      // $VF: synthetic method
      private static PopChamsRenderTypes.Kind[] $values() {
         return new PopChamsRenderTypes.Kind[]{ADDITIVE, ADDITIVE_SOLID, TRANSLUCENT, TRANSLUCENT_SOLID, MASK, MASK_SOLID};
      }
   }
}

