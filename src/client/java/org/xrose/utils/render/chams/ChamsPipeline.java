package org.xrose.utils.render.chams;

import net.minecraft.client.renderer.state.level.LevelRenderState;
import org.xrose.feature.impl.visual.ChamsFeature;
import sdk.api.optimize.optimize;

@optimize
public final class ChamsPipeline {
   private final ChamsMaskRenderer maskRenderer = new ChamsMaskRenderer();
   private final ChamsCompositeEffect composite = new ChamsCompositeEffect();

   public void render(LevelRenderState levelRenderState, ChamsFeature feature) {
      this.maskRenderer.renderGroups(levelRenderState, feature, frame -> this.composite.render(frame, feature));
   }

   public void release() {
      this.maskRenderer.release();
      this.composite.release();
   }
}

