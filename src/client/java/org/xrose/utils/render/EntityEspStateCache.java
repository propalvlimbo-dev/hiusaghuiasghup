package org.xrose.utils.render;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import sdk.api.optimize.optimize;

@optimize
public final class EntityEspStateCache {
   private static volatile List<EntityRenderState> frameStates = Collections.emptyList();

   private EntityEspStateCache() {
   }

   public static void capture(List<EntityRenderState> states) {
      frameStates = states != null && !states.isEmpty() ? new ArrayList<>(states) : Collections.emptyList();
   }

   public static List<EntityRenderState> currentStates() {
      return frameStates;
   }

   public static void clear() {
      frameStates = Collections.emptyList();
   }
}

