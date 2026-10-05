package org.xrose.mixin.input;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.ClientInput;
import net.minecraft.client.player.KeyboardInput;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.phys.Vec2;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.xrose.event.EventManager;
import org.xrose.event.events.input.PlayerInputEvent;
import org.xrose.feature.impl.movement.InventoryMoveFeature;
import org.xrose.utils.combat.aura.Angle;
import org.xrose.utils.combat.aura.AngleConnection;
import org.xrose.utils.combat.aura.AngleConstructor;
import org.xrose.utils.inventory.InventoryFlowManager;

@Mixin(KeyboardInput.class)
public abstract class KeyboardInputMixin extends ClientInput {
   @Inject(method = "tick", at = @At("TAIL"))
   private void onPlayerInputTick(CallbackInfo ci) {
      PlayerInputEvent event = new PlayerInputEvent((KeyboardInput)(Object)this, this.keyPresses, this.getMoveVector());
      EventManager.call(event);
      InventoryFlowManager.input(event);
      this.keyPresses = this.xrose$transformInput(event.getKeyPresses());
      this.moveVector = xrose$computeMoveVector(this.keyPresses);
   }

   @Unique
   private Input xrose$transformInput(Input input) {
      Minecraft client = Minecraft.getInstance();
      Angle angle = AngleConnection.INSTANCE.getCurrentAngle();
      AngleConstructor plan = AngleConnection.INSTANCE.getCurrentRotationPlan();
      if (client.player != null
         && !this.xrose$shouldPreserveInventoryMovement()
         && angle != null
         && plan != null
         && plan.isMoveCorrection()
         && plan.isFreeCorrection()) {
         float deltaYaw = client.player.getYRot() - angle.getYaw();
         float z = getMovementMultiplier(input.forward(), input.backward());
         float x = getMovementMultiplier(input.left(), input.right());
         float radians = deltaYaw * (float) (Math.PI / 180.0);
         float cos = Mth.cos(radians);
         float sin = Mth.sin(radians);
         float newX = x * cos - z * sin;
         float newZ = z * cos + x * sin;
         int movementSideways = Math.round(newX);
         int movementForward = Math.round(newZ);
         return new Input(movementForward > 0, movementForward < 0, movementSideways > 0, movementSideways < 0, input.jump(), input.shift(), input.sprint());
      } else {
         return input;
      }
   }

   @Unique
   private boolean xrose$shouldPreserveInventoryMovement() {
      InventoryMoveFeature inventoryMove = InventoryMoveFeature.getEnabled();
      return inventoryMove != null && inventoryMove.shouldPreserveInventoryMovementInput();
   }

   @Unique
   private static Vec2 xrose$computeMoveVector(Input keyPresses) {
      float z = getMovementMultiplier(keyPresses.forward(), keyPresses.backward());
      float x = getMovementMultiplier(keyPresses.left(), keyPresses.right());
      return new Vec2(x, z).normalized();
   }

   private static float getMovementMultiplier(boolean positive, boolean negative) {
      if (positive == negative) {
         return 0.0F;
      } else {
         return positive ? 1.0F : -1.0F;
      }
   }
}

