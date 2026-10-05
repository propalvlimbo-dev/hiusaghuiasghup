package org.xrose.feature.impl.visual;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.network.protocol.game.ClientboundEntityEventPacket;
import net.minecraft.resources.Identifier;
import org.xrose.context.MinecraftContext;
import org.xrose.event.EventTarget;
import org.xrose.event.events.lifecycle.WorldLeaveEvent;
import org.xrose.event.events.packet.PacketReceiveEvent;
import org.xrose.feature.Feature;
import org.xrose.feature.FeatureCategory;
import org.xrose.feature.FeatureManager;
import org.xrose.feature.setting.BooleanSetting;
import org.xrose.feature.setting.ColorSetting;
import org.xrose.feature.setting.NumberSetting;
import sdk.api.optimize.optimize;

@optimize
public final class PopChamsFeature extends Feature implements MinecraftContext {
   public static final byte USE_TOTEM_STATUS = 35;
   private static final long FALLBACK_ANIMATION_DURATION_NANOS = 1000000000L;
   public final BooleanSetting blending = this.register(new BooleanSetting("Additive Glow", true).configKey("render.popchams.blending"));
   public final BooleanSetting textured = this.register(new BooleanSetting("Textured", false).configKey("render.popchams.textured"));
   public final ColorSetting color = this.register(new ColorSetting("Color", -1).configKey("render.popchams.color"));
   public final NumberSetting glowRadius = this.register(new NumberSetting("Glow Radius", 6.0, 1.0, 12.0, 0.05F, "px").configKey("render.chams.glowRadius"));
   private final List<PopChamsFeature.Snapshot> snapshots = new ArrayList<>();

   public PopChamsFeature() {
      super("PopChams", "Leaves a rising ghost model when a player uses a totem", FeatureCategory.VISUAL, -1);
   }

   public static PopChamsFeature getEnabled() {
      return FeatureManager.INSTANCE.getEnabled(PopChamsFeature.class);
   }

   @EventTarget
   public void onPacketReceive(PacketReceiveEvent event) {
      if (event.getPhase() == PacketReceiveEvent.Phase.PRE && event.getPacket() instanceof ClientboundEntityEventPacket packet && packet.getEventId() == 35) {
         mc.execute(() -> this.capture(packet));
      }
   }

   @EventTarget
   public void onWorldLeave(WorldLeaveEvent event) {
      this.clear();
   }

   @Override
   protected void onDisable() {
      this.clear();
   }

   private void capture(ClientboundEntityEventPacket packet) {
      if (this.isEnabled() && mc.level != null) {
         if (packet.getEntity(mc.level) instanceof AbstractClientPlayer player) {
            Identifier texture = player.getSkin().body().texturePath();
            if (texture != null) {
               this.snapshots
                  .add(
                     new PopChamsFeature.Snapshot(
                        texture,
                        this.color.getValue(),
                        this.textured.getValue(),
                        player.getX(),
                        player.getY(),
                        player.getZ(),
                        player.yBodyRot,
                        player.yHeadRot - player.yHeadRotO,
                        player.getXRot(),
                        player.walkAnimation.position(),
                        player.walkAnimation.speed(),
                        System.nanoTime()
                     )
                  );
            }
         }
      }
   }

   public List<PopChamsFeature.Snapshot> activeSnapshots(long nowNanos) {
      this.snapshots.removeIf(snapshot -> snapshot.expired(nowNanos));
      return this.snapshots.isEmpty() ? List.of() : List.copyOf(this.snapshots);
   }

   public int effectiveGlowRadius() {
      float value = this.glowRadius.getValue().floatValue();
      return value <= 0.0F ? 6 : Math.round(value);
   }

   private void clear() {
      this.snapshots.clear();
   }

   public record Snapshot(
      Identifier texture,
      int baseColor,
      boolean textured,
      double x,
      double y,
      double z,
      float bodyYaw,
      float relativeHeadYaw,
      float pitch,
      float limbProgress,
      float limbSpeed,
      long startedAtNanos
   ) {
      public float animation(long nowNanos) {
         float progress = Math.clamp((float)(nowNanos - this.startedAtNanos) / 1.0E9F, 0.0F, 1.0F);
         return 1.0F - easeOutQuart(progress);
      }

      public boolean expired(long nowNanos) {
         return nowNanos - this.startedAtNanos >= 1000000000L;
      }

      private static float easeOutQuart(float value) {
         float inverse = 1.0F - value;
         return 1.0F - inverse * inverse * inverse * inverse;
      }
   }
}

