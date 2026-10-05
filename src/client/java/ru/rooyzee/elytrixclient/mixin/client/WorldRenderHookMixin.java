package ru.rooyzee.elytrixclient.mixin.client;

import net.minecraft.client.renderer.LevelRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.rooyzee.elytrixclient.client.features.render.Visuals;

/**
 * World-render хук (замена fabric WorldRenderEvents, которого нет в fabric-api 26.2):
 * TAIL LevelRenderer#render — наши визуалы получают контекст каждый кадр.
 */
@Mixin(LevelRenderer.class)
public abstract class WorldRenderHookMixin {

    @Inject(method = "render", at = @At("TAIL"), require = 0)
    private void elytrix$worldRender(CallbackInfo ci) {
        Visuals.renderWorld(this);
    }
}
