package org.xrose.mixin.accessor;

import com.mojang.authlib.yggdrasil.ProfileResult;
import java.util.concurrent.CompletableFuture;
import net.minecraft.client.Minecraft;
import net.minecraft.client.User;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Minecraft.class)
public interface MinecraftAccessor {
   @Accessor("rightClickDelay")
   void setRightClickDelay(int var1);

   @Accessor("user")
   @Mutable
   void setUser(User var1);

   @Accessor("profileFuture")
   @Mutable
   void setProfileFuture(CompletableFuture<ProfileResult> var1);
}
