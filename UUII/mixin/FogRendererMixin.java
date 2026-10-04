package wtf.expensive.client.mixin;

import net.minecraft.client.renderer.fog.FogData;
import net.minecraft.client.renderer.fog.FogRenderer;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import wtf.expensive.client.managment.Managment;
import wtf.expensive.client.modules.Function;
import wtf.expensive.client.modules.impl.render.CustomWorld;
import wtf.expensive.client.modules.impl.render.NoRenderFunction;
import wtf.expensive.client.util.render.ColorUtil;

@Mixin(FogRenderer.class)
public abstract class FogRendererMixin {
    @Inject(method = "setupFog", at = @At("RETURN"), cancellable = true)
    private void expensive$customFog(CallbackInfoReturnable<FogData> cir) {
        FogData data = cir.getReturnValue();
        if (data == null || Managment.FUNCTION_MANAGER == null) return;

        Function noRenderFunction = Managment.FUNCTION_MANAGER.get("No Render");
        if (noRenderFunction instanceof NoRenderFunction noRender
                && noRender.isState() && noRender.elements.get(7)) {
            data.environmentalStart = 100000f;
            data.renderDistanceStart = 100000f;
            data.environmentalEnd = 100001f;
            data.renderDistanceEnd = 100001f;
            data.skyEnd = 100001f;
            data.cloudEnd = 100001f;
            return;
        }

        Function customWorldFunction = Managment.FUNCTION_MANAGER.get("Custom World");
        if (customWorldFunction instanceof CustomWorld customWorld
                && customWorld.isState() && customWorld.change.get(1)) {
            float distance = Math.max(1.1f, customWorld.fogDistance.getValue().floatValue());
            data.environmentalStart = data.environmentalEnd / distance;
            data.renderDistanceStart = data.renderDistanceEnd / distance;
            int color = customWorld.fogColor.get();
            data.color = new Vector4f(ColorUtil.getRed(color) / 255f, ColorUtil.getGreen(color) / 255f,
                    ColorUtil.getBlue(color) / 255f, 1f);
        }
    }
}
