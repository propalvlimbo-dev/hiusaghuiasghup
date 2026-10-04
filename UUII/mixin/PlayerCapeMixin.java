package wtf.expensive.client.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.core.ClientAsset;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.PlayerSkin;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import wtf.expensive.client.Expensive;
import wtf.expensive.client.util.ClientUtil;

@Mixin(AbstractClientPlayer.class)
public abstract class PlayerCapeMixin {
    @Unique
    private static final ClientAsset.Texture EXPENSIVE_CAPE = new ClientAsset.ResourceTexture(
            Identifier.fromNamespaceAndPath(Expensive.MOD_ID, "cape"),
            Identifier.fromNamespaceAndPath(Expensive.MOD_ID, "textures/images/cape.png"));

    @Inject(method = "getSkin", at = @At("RETURN"), cancellable = true)
    private void expensive$cape(CallbackInfoReturnable<PlayerSkin> cir) {
        Minecraft minecraft = Minecraft.getInstance();

        if ((Object) this != minecraft.player || ClientUtil.legitMode) {
            return;
        }
        PlayerSkin skin = cir.getReturnValue();
        if (skin == null) {
            return;
        }
        cir.setReturnValue(new PlayerSkin(skin.body(), EXPENSIVE_CAPE, skin.elytra(),
                skin.model(), skin.secure()));
    }
}
