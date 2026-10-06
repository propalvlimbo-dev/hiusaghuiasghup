package ru.rooyzee.elytrixclient.mixin.client;

import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ru.rooyzee.elytrixclient.client.ElytrixclientClient;

/**
 * Оптимизация ботов: свои боты (ник с префиксом из настроек) не отрисовываются
 * у тебя вообще — рендер не проседает даже с сотнями ботов. На сервере они есть.
 */
@Mixin(EntityRenderer.class)
public abstract class BotCullMixin {

    @Inject(method = "shouldRender", at = @At("HEAD"), cancellable = true)
    private void cullOwnBots(Entity entity, Frustum frustum, double camX, double camY, double camZ,
            CallbackInfoReturnable<Boolean> cir) {
        if (!ElytrixclientClient.CONFIG.botCull || !(entity instanceof Player)) {
            return;
        }
        String prefix = ElytrixclientClient.CONFIG.ownBotPrefix;
        if (prefix != null && !prefix.isEmpty()
                && entity.getName().getString().startsWith(prefix)) {
            cir.setReturnValue(false);
        }
    }
}
