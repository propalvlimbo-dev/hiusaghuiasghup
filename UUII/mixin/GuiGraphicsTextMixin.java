package wtf.expensive.client.mixin;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import wtf.expensive.client.managment.Managment;
import wtf.expensive.client.modules.Function;
import wtf.expensive.client.modules.impl.render.NameProtect;

@Mixin(GuiGraphicsExtractor.class)
public abstract class GuiGraphicsTextMixin {
    @ModifyVariable(method = "text(Lnet/minecraft/client/gui/Font;Ljava/lang/String;IIIZ)V",
            at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private String expensive$string(String text) {
        if (Managment.FUNCTION_MANAGER == null || text == null) return text;
        Function function = Managment.FUNCTION_MANAGER.get("NameProtect");
        return function instanceof NameProtect protect ? protect.patch(text) : text;
    }
}
