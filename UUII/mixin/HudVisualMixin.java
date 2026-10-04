package wtf.expensive.client.mixin;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wtf.expensive.client.managment.Managment;
import wtf.expensive.client.modules.Function;
import wtf.expensive.client.modules.impl.render.NoRenderFunction;

@Mixin(Hud.class)
public abstract class HudVisualMixin {
    private static boolean hidden(int option) {
        if (Managment.FUNCTION_MANAGER == null) return false;
        Function function = Managment.FUNCTION_MANAGER.get("No Render");
        return function instanceof NoRenderFunction noRender && noRender.isState() && noRender.elements.get(option);
    }

    @Inject(method = "extractBossOverlay", at = @At("HEAD"), cancellable = true)
    private void expensive$bossBar(GuiGraphicsExtractor graphics, DeltaTracker delta, CallbackInfo ci) {
        if (hidden(2)) ci.cancel();
    }

    @Inject(method = "extractScoreboardSidebar", at = @At("HEAD"), cancellable = true)
    private void expensive$scoreboard(GuiGraphicsExtractor graphics, DeltaTracker delta, CallbackInfo ci) {
        if (hidden(3)) ci.cancel();
    }

    @Inject(method = "extractTitle", at = @At("HEAD"), cancellable = true)
    private void expensive$titles(GuiGraphicsExtractor graphics, DeltaTracker delta, CallbackInfo ci) {
        if (hidden(4)) ci.cancel();
    }

    @Inject(method = "extractTabList", at = @At("HEAD"), cancellable = true)
    private void expensive$tabList(GuiGraphicsExtractor graphics, DeltaTracker delta, CallbackInfo ci) {
        if (hidden(3)) ci.cancel();
    }

    @Inject(method = "extractCrosshair", at = @At("HEAD"), cancellable = true)
    private void expensive$vanillaCrosshair(GuiGraphicsExtractor graphics, DeltaTracker delta, CallbackInfo ci) {
        Function crosshair = Managment.FUNCTION_MANAGER == null ? null : Managment.FUNCTION_MANAGER.get("Crosshair");
        if (crosshair != null && crosshair.isState()) ci.cancel();
    }
}
