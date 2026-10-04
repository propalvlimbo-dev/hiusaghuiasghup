package wtf.expensive.client.mixin;

import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import wtf.expensive.client.events.EventManager;
import wtf.expensive.client.events.impl.player.EventObsidianPlace;
import wtf.expensive.client.events.impl.player.EventPlaceAnchorByPlayer;
import wtf.expensive.client.managment.Managment;
import wtf.expensive.client.modules.Function;
import wtf.expensive.client.modules.impl.player.NoInteractFunction;

@Mixin(MultiPlayerGameMode.class)
public abstract class BlockInteractionMixin {
    private static NoInteractFunction expensive$noInteract() {
        if (Managment.FUNCTION_MANAGER == null) return null;
        Function function = Managment.FUNCTION_MANAGER.get("NoInteract");
        return function instanceof NoInteractFunction noInteract && noInteract.isState() ? noInteract : null;
    }

    @Inject(method = "useItemOn", at = @At("HEAD"), cancellable = true)
    private void expensive$noInteract(LocalPlayer player, InteractionHand hand, BlockHitResult hit,
                                      CallbackInfoReturnable<InteractionResult> cir) {
        NoInteractFunction noInteract = expensive$noInteract();
        if (player.level() == null || noInteract == null) return;
        if (noInteract.shouldBlock(player.level().getBlockState(hit.getBlockPos()))) {
            cir.setReturnValue(InteractionResult.PASS);
        }
    }

    @Inject(method = "interact", at = @At("HEAD"), cancellable = true)
    private void expensive$noInteractEntity(Player player, Entity entity, EntityHitResult hit,
                                            InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        NoInteractFunction noInteract = expensive$noInteract();
        if (noInteract != null && noInteract.shouldBlock(entity)) {
            cir.setReturnValue(InteractionResult.PASS);
        }
    }

    @Inject(method = "useItemOn", at = @At("RETURN"))
    private void expensive$placedBlock(LocalPlayer player, InteractionHand hand, BlockHitResult hit,
                                       CallbackInfoReturnable<InteractionResult> cir) {
        if (!cir.getReturnValue().consumesAction()
                || !(player.getItemInHand(hand).getItem() instanceof BlockItem blockItem)) {
            return;
        }
        BlockPlaceContext context = new BlockPlaceContext(player, hand, player.getItemInHand(hand), hit);
        var position = context.getClickedPos();
        if (blockItem.getBlock() == Blocks.OBSIDIAN) {
            EventManager.call(new EventObsidianPlace(Blocks.OBSIDIAN, position));
        } else if (blockItem.getBlock() == Blocks.RESPAWN_ANCHOR) {
            EventManager.call(new EventPlaceAnchorByPlayer(Blocks.RESPAWN_ANCHOR, position));
        }
    }
}
