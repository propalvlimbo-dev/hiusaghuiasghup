package org.xrose.utils.render.world;

import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import sdk.api.optimize.optimize;

@optimize
public record WorldEffectContext(LevelRenderState levelRenderState, CameraRenderState cameraRenderState, float tickDelta) {
}

