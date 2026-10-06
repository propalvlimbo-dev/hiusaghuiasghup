package org.xrose.utils.render.target;

import net.minecraft.world.entity.LivingEntity;
import org.xrose.feature.impl.visual.TargetESPFeature;
import sdk.api.optimize.optimize;

@optimize
public final class TargetMarkers {
   private final AuraMarkerRenderer marker = new AuraMarkerRenderer();
   private final GhostTargetRenderer ghost = new GhostTargetRenderer();
   private final CircleTargetRenderer circle = new CircleTargetRenderer();
   private final DeadheadTargetRenderer deadhead = new DeadheadTargetRenderer();
   private final CrystalTargetRenderer crystal = new CrystalTargetRenderer();

   public void render(TargetESPFeature esp, float tickDelta) {
      if (esp != null) {
         LivingEntity target = esp.getCurrentTarget();
         int color = esp.getMarkerColor();
         boolean ghosts = esp.usesGhostTargetEsp();
         boolean circles = esp.usesCircleTargetEsp();
         boolean deadheads = esp.usesDeadheadTargetEsp();
         boolean crystals = esp.usesCrystalTargetEsp();
         boolean crystals2 = esp.usesCrystals2TargetEsp();
         boolean markerSuppressed = ghosts || circles || deadheads || crystals || crystals2;
         this.marker.render(markerSuppressed ? null : target, tickDelta, color);
         this.ghost.render(ghosts ? target : null, tickDelta, color);
         this.circle.render(circles ? target : null, tickDelta, color);
         this.deadhead.render(deadheads ? target : null, tickDelta, color);
         if (target != null && (crystals || crystals2)) {
            this.crystal.render(target, tickDelta, color, crystals2);
         }
      }
   }

   public void release() {
      this.marker.release();
      this.deadhead.release();
      this.crystal.release();
   }
}

