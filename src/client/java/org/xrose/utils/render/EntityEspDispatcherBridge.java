package org.xrose.utils.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import sdk.api.optimize.optimize;

@optimize
public interface EntityEspDispatcherBridge {
   <S extends EntityRenderState> void submitForGlow(
      S var1, CameraRenderState var2, double var3, double var5, double var7, PoseStack var9, SubmitNodeCollector var10
   );
}
