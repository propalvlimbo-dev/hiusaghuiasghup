package ru.rooyzee.elytrixclient.mixin.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.rooyzee.elytrixclient.client.features.render.Visuals;

/**
 * Хук удара игрока (замена fabric AttackEntityEvents, которого нет в fabric-api 26.2):
 * MultiPlayerGameMode#attack(Player, Entity) — клиентская точка атаки.
 */
@Mixin(MultiPlayerGameMode.class)
public abstract class AttackHookMixin {

    @Inject(method = "attack", at = @At("HEAD"), require = 0)
    private void elytrix$onAttack(Player player, Entity target, CallbackInfo ci) {
        if (player == Minecraft.getInstance().player) {
            Visuals.onAttack(target);
        }
    }
}
