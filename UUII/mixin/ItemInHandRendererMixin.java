package wtf.expensive.client.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.world.entity.HumanoidArm;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wtf.expensive.client.managment.Managment;
import wtf.expensive.client.modules.Function;
import wtf.expensive.client.modules.impl.render.SwingAnimationFunction;

@Mixin(ItemInHandRenderer.class)
public abstract class ItemInHandRendererMixin {
    private static SwingAnimationFunction animation() {
        if (Managment.FUNCTION_MANAGER == null) return null;
        Function function = Managment.FUNCTION_MANAGER.get("Swing Animation");
        return function instanceof SwingAnimationFunction animation && animation.isState() ? animation : null;
    }

    @Inject(method = "applyItemArmTransform", at = @At("TAIL"))
    private void expensive$handPosition(PoseStack pose, HumanoidArm arm, float equipProgress, CallbackInfo ci) {
        SwingAnimationFunction animation = animation();
        if (animation == null) return;
        if (arm == HumanoidArm.RIGHT) {
            pose.translate(animation.rightX.getValue().floatValue(), animation.rightY.getValue().floatValue(),
                    animation.rightZ.getValue().floatValue());
        } else {
            pose.translate(animation.leftX.getValue().floatValue(), animation.leftY.getValue().floatValue(),
                    animation.leftZ.getValue().floatValue());
        }
    }

    @Inject(method = "applyItemArmAttackTransform", at = @At("HEAD"), cancellable = true)
    private void expensive$swing(PoseStack pose, HumanoidArm arm, float swingProgress, CallbackInfo ci) {
        SwingAnimationFunction animation = animation();
        if (animation == null || arm != HumanoidArm.RIGHT || animation.mode.is("Smooth")) return;
        float wave = (float) Math.sin(swingProgress * Math.PI);
        if (animation.mode.is("Self")) {
            pose.mulPose(Axis.YP.rotationDegrees(90));
            pose.mulPose(Axis.ZP.rotationDegrees(-70));
            pose.mulPose(Axis.XP.rotationDegrees(-animation.angle.getValue().floatValue()
                    - animation.power.getValue().floatValue() * 10f * wave));
        } else if (animation.mode.is("Block")) {
            pose.mulPose(Axis.YP.rotationDegrees(90));
            pose.mulPose(Axis.ZP.rotationDegrees(-30));
            pose.mulPose(Axis.XP.rotationDegrees(-animation.angle.getValue().floatValue()
                    - animation.power.getValue().floatValue() * 10f * wave));
        } else if (animation.mode.is("Back")) {
            pose.mulPose(Axis.YP.rotationDegrees(90));
            pose.mulPose(Axis.ZP.rotationDegrees(-65));
            pose.mulPose(Axis.XP.rotationDegrees(-65 + animation.power.getValue().floatValue() * 10f * wave));
        } else if (animation.mode.is("Swipe")) {
            pose.mulPose(Axis.YP.rotationDegrees(90));
            pose.mulPose(Axis.ZP.rotationDegrees(-70));
            pose.mulPose(Axis.XP.rotationDegrees(-90 + 80 * wave));
        }
        ci.cancel();
    }
}
