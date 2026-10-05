package org.xrose.utils.combat;

import it.unimi.dsi.fastutil.objects.Object2DoubleArrayMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.boat.AbstractBoat;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.PowderSnowBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.xrose.mixin.accessor.LivingEntityAccessor;

public class PlayerSimulation {
   public final Player player;
   public final PlayerSimulation.SimulatedPlayerInput input;
   public Vec3 pos;
   public Vec3 velocity;
   public AABB boundingBox;
   public float yaw;
   public float pitch;
   public boolean sprinting;
   public float fallDistance;
   public int jumpingCooldown;
   public boolean isJumping;
   public boolean isFallFlying;
   public boolean onGround;
   public boolean horizontalCollision;
   public boolean verticalCollision;
   public boolean touchingWater;
   public boolean isSwimming;
   public boolean submergedInWater;
   private final Object2DoubleMap<TagKey<Fluid>> fluidHeight;
   private final HashSet<TagKey<Fluid>> submergedFluidTag;
   private int simulatedTicks = 0;
   private boolean clipLedged = false;
   private static final double STEP_HEIGHT = 0.5;

   public PlayerSimulation(
      Player player,
      PlayerSimulation.SimulatedPlayerInput input,
      Vec3 pos,
      Vec3 velocity,
      AABB boundingBox,
      float yaw,
      float pitch,
      boolean sprinting,
      float fallDistance,
      int jumpingCooldown,
      boolean isJumping,
      boolean isFallFlying,
      boolean onGround,
      boolean horizontalCollision,
      boolean verticalCollision,
      boolean touchingWater,
      boolean isSwimming,
      boolean submergedInWater,
      Object2DoubleMap<TagKey<Fluid>> fluidHeight,
      HashSet<TagKey<Fluid>> submergedFluidTag
   ) {
      this.player = player;
      this.input = input;
      this.pos = pos;
      this.velocity = velocity;
      this.boundingBox = boundingBox;
      this.yaw = yaw;
      this.pitch = pitch;
      this.sprinting = sprinting;
      this.fallDistance = fallDistance;
      this.jumpingCooldown = jumpingCooldown;
      this.isJumping = isJumping;
      this.isFallFlying = isFallFlying;
      this.onGround = onGround;
      this.horizontalCollision = horizontalCollision;
      this.verticalCollision = verticalCollision;
      this.touchingWater = touchingWater;
      this.isSwimming = isSwimming;
      this.submergedInWater = submergedInWater;
      this.fluidHeight = fluidHeight;
      this.submergedFluidTag = submergedFluidTag;
   }

   public static PlayerSimulation simulateLocalPlayer(int ticks) {
      LocalPlayer player = Minecraft.getInstance().player;
      if (player == null) {
         return null;
      }

      PlayerSimulation simulatedPlayer = fromClientPlayer(PlayerSimulation.SimulatedPlayerInput.fromClientPlayer(player.input.keyPresses));

      for (int i = 0; i < ticks; i++) {
         simulatedPlayer.tick();
      }

      return simulatedPlayer;
   }

   public static PlayerSimulation fromClientPlayer(PlayerSimulation.SimulatedPlayerInput input) {
      LocalPlayer player = Minecraft.getInstance().player;
      return player == null
         ? null
         : new PlayerSimulation(
            player,
            input,
            player.position(),
            player.getDeltaMovement(),
            player.getBoundingBox(),
            player.getYRot(),
            player.getXRot(),
            player.isSprinting(),
            (float)player.fallDistance,
            ((LivingEntityAccessor)player).getNoJumpDelay(),
            player.isJumping(),
            player.isFallFlying(),
            player.onGround(),
            player.horizontalCollision,
            player.verticalCollision,
            player.isInWater(),
            player.isSwimming(),
            player.isUnderWater(),
            seedFluidHeights(player),
            seedSubmergedFluidTags(player)
         );
   }

   private static Object2DoubleArrayMap<TagKey<Fluid>> seedFluidHeights(Player player) {
      Object2DoubleArrayMap<TagKey<Fluid>> map = new Object2DoubleArrayMap(2);
      map.put(FluidTags.WATER, player.getFluidHeight(FluidTags.WATER));
      map.put(FluidTags.LAVA, player.getFluidHeight(FluidTags.LAVA));
      return map;
   }

   private static HashSet<TagKey<Fluid>> seedSubmergedFluidTags(Player player) {
      HashSet<TagKey<Fluid>> set = new HashSet<>();
      if (player.isUnderWater()) {
         set.add(FluidTags.WATER);
      }

      return set;
   }

   public void tick() {
      this.simulatedTicks++;
      this.clipLedged = false;
      if (!(this.pos.y <= -70.0)) {
         this.input.update();
         this.checkWaterState();
         this.updateSubmergedInWaterState();
         this.updateSwimming();
         if (this.jumpingCooldown > 0) {
            this.jumpingCooldown--;
         }

         this.isJumping = this.input.keyPresses.jump();
         double newX = this.velocity.x;
         double newY = this.velocity.y;
         double newZ = this.velocity.z;
         if (Math.abs(this.velocity.x) < 0.003) {
            newX = 0.0;
         }

         if (Math.abs(this.velocity.y) < 0.003) {
            newY = 0.0;
         }

         if (Math.abs(this.velocity.z) < 0.003) {
            newZ = 0.0;
         }

         if (this.onGround) {
            this.isFallFlying = false;
         }

         this.velocity = new Vec3(newX, newY, newZ);
         if (this.isJumping) {
            double fluidLevel = this.isInLava() ? this.getFluidHeight(FluidTags.LAVA) : this.getFluidHeight(FluidTags.WATER);
            boolean inWater = this.isTouchingWater() && fluidLevel > 0.0;
            double swimHeight = this.getSwimHeight();
            if (!inWater || this.onGround && !(fluidLevel > swimHeight)) {
               if (!this.isInLava() || this.onGround && !(fluidLevel > swimHeight)) {
                  if ((this.onGround || inWater && fluidLevel <= swimHeight) && this.jumpingCooldown == 0) {
                     this.jump();
                     if (this.player.equals(Minecraft.getInstance().player)) {
                        this.jumpingCooldown = 10;
                     }
                  }
               } else {
                  this.swimUpward(FluidTags.LAVA);
               }
            } else {
               this.swimUpward(FluidTags.WATER);
            }
         }

         float sidewaysSpeed = this.input.movementSideways * 0.98F;
         float forwardSpeed = this.input.movementForward * 0.98F;
         float upwardsSpeed = 0.0F;
         if (this.hasStatusEffect(MobEffects.SLOW_FALLING) || this.hasStatusEffect(MobEffects.LEVITATION)) {
            this.onLanding();
         }

         this.travel(new Vec3(sidewaysSpeed, upwardsSpeed, forwardSpeed));
      }
   }

   private void travel(Vec3 movementInput) {
      if (this.isSwimming && !this.player.isPassenger()) {
         double g = this.getRotationVector().y;
         double h = g < -0.2 ? 0.085 : 0.06;
         BlockPos posAbove = new BlockPos(Mth.floor(this.pos.x), Mth.floor(this.pos.y + 1.0 - 0.1), Mth.floor(this.pos.z));
         if (g <= 0.0 || this.input.keyPresses.jump() || !this.player.level().getBlockState(posAbove).getFluidState().isEmpty()) {
            this.velocity = this.velocity.add(0.0, (g - this.velocity.y) * h, 0.0);
         }
      }

      double beforeTravelVelocityY = this.velocity.y;
      double d = 0.08;
      boolean falling = this.velocity.y <= 0.0;
      if (this.velocity.y <= 0.0 && this.hasStatusEffect(MobEffects.SLOW_FALLING)) {
         d = 0.01;
         this.onLanding();
      }

      if (this.isTouchingWater() && this.player.isAffectedByFluids()) {
         double e = this.pos.y;
         float f = this.isSprinting() ? 0.9F : 0.8F;
         float g = 0.02F;
         float h = (float)this.getAttributeValue(Attributes.WATER_MOVEMENT_EFFICIENCY);
         if (!this.onGround) {
            h *= 0.5F;
         }

         if (h > 0.0F) {
            f += (0.54600006F - f) * h / 3.0F;
            g += (this.getMovementSpeed() - g) * h / 3.0F;
         }

         if (this.hasStatusEffect(MobEffects.DOLPHINS_GRACE)) {
            f = 0.96F;
         }

         this.updateVelocity(g, movementInput);
         this.move(this.velocity);
         Vec3 tempVel = this.velocity;
         if (this.horizontalCollision && this.isClimbing()) {
            tempVel = new Vec3(tempVel.x, 0.2, tempVel.z);
         }

         this.velocity = tempVel.multiply(f, 0.8, f);
         Vec3 vec3d2 = this.player.getFluidFallingAdjustedMovement(d, falling, this.velocity);
         this.velocity = vec3d2;
         if (this.horizontalCollision && this.doesNotCollide(vec3d2.x, vec3d2.y + 0.6 - this.pos.y + e, vec3d2.z)) {
            this.velocity = new Vec3(vec3d2.x, 0.3, vec3d2.z);
         }
      } else if (this.isInLava() && this.player.isAffectedByFluids()) {
         double e = this.pos.y;
         this.updateVelocity(0.02F, movementInput);
         this.move(this.velocity);
         if (this.getFluidHeight(FluidTags.LAVA) <= this.getSwimHeight()) {
            this.velocity = this.velocity.multiply(0.5, 0.8, 0.5);
            this.velocity = this.player.getFluidFallingAdjustedMovement(d, falling, this.velocity);
         } else {
            this.velocity = this.velocity.scale(0.5);
         }

         if (!this.player.isNoGravity()) {
            this.velocity = this.velocity.add(0.0, -d / 4.0, 0.0);
         }

         if (this.horizontalCollision && this.doesNotCollide(this.velocity.x, this.velocity.y + 0.6 - this.pos.y + e, this.velocity.z)) {
            this.velocity = new Vec3(this.velocity.x, 0.3, this.velocity.z);
         }
      } else if (this.isFallFlying) {
         Vec3 e = this.velocity;
         if (e.y > -0.5) {
            this.fallDistance = 1.0F;
         }

         Vec3 vec3d3 = this.getRotationVector();
         float f = this.pitch * (float) (Math.PI / 180.0);
         double g = Math.sqrt(vec3d3.x * vec3d3.x + vec3d3.z * vec3d3.z);
         double horizontalSpeed = this.velocity.horizontalDistance();
         double i = vec3d3.length();
         float j = Mth.cos(f);
         j = (float)(j * (j * Math.min(1.0, i / 0.4)));
         e = this.velocity.add(0.0, d * (-1.0 + j * 0.75), 0.0);
         if (e.y < 0.0 && g > 0.0) {
            double k = e.y * -0.1 * j;
            e = e.add(vec3d3.x * k / g, k, vec3d3.z * k / g);
         }

         if (f < 0.0F && g > 0.0) {
            double k = horizontalSpeed * -Mth.sin(f) * 0.04;
            e = e.add(-vec3d3.x * k / g, k * 3.2, -vec3d3.z * k / g);
         }

         if (g > 0.0) {
            e = e.add((vec3d3.x / g * horizontalSpeed - e.x) * 0.1, 0.0, (vec3d3.z / g * horizontalSpeed - e.z) * 0.1);
         }

         this.velocity = e.multiply(0.99, 0.98, 0.99);
         this.move(this.velocity);
      } else {
         BlockPos blockPos = this.getVelocityAffectingPos();
         float p = this.player.level().getBlockState(blockPos).getBlock().getFriction();
         float f = this.onGround ? p * 0.91F : 0.91F;
         Vec3 vec3d6 = this.applyMovementInput(movementInput, p);
         double q = vec3d6.y;
         if (this.hasStatusEffect(MobEffects.LEVITATION)) {
            MobEffectInstance levitation = this.getStatusEffect(MobEffects.LEVITATION);
            if (levitation != null) {
               q += (0.05 * (levitation.getAmplifier() + 1) - vec3d6.y) * 0.2;
            }
         } else if (this.player.level().isClientSide() && !this.player.level().hasChunkAt(blockPos)) {
            q = this.pos.y > this.player.level().getMinY() ? -0.1 : 0.0;
         } else if (!this.player.isNoGravity()) {
            q -= d;
         }

         if (this.player.shouldDiscardFriction()) {
            this.velocity = new Vec3(vec3d6.x, q, vec3d6.z);
         } else {
            this.velocity = new Vec3(vec3d6.x * f, q * 0.98F, vec3d6.z * f);
         }
      }

      if (this.player.getAbilities().flying && !this.player.isPassenger()) {
         this.velocity = new Vec3(this.velocity.x, beforeTravelVelocityY * 0.6, this.velocity.z);
         this.onLanding();
      }
   }

   private Vec3 applyMovementInput(Vec3 movementInput, float slipperiness) {
      this.updateVelocity(this.getMovementSpeed(slipperiness), movementInput);
      this.velocity = this.applyClimbingSpeed(this.velocity);
      this.move(this.velocity);
      Vec3 result = this.velocity;
      BlockPos posBlock = this.posToBlockPos(this.pos);
      BlockState state = this.getState(posBlock);
      if ((this.horizontalCollision || this.isJumping)
         && (this.isClimbing() || state != null && state.is(Blocks.POWDER_SNOW) && PowderSnowBlock.canEntityWalkOnPowderSnow(this.player))) {
         result = new Vec3(result.x, 0.2, result.z);
      }

      return result;
   }

   private void updateVelocity(float speed, Vec3 movementInput) {
      Vec3 vec = movementInputToVelocity(movementInput, speed, this.yaw);
      this.velocity = this.velocity.add(vec);
   }

   private static Vec3 movementInputToVelocity(Vec3 movementInput, float speed, float yaw) {
      double lengthSquared = movementInput.lengthSqr();
      if (lengthSquared < 1.0E-7) {
         return Vec3.ZERO;
      }

      Vec3 normalized = lengthSquared > 1.0 ? movementInput.normalize() : movementInput;
      Vec3 scaled = normalized.scale(speed);
      float yawRad = yaw * (float) (Math.PI / 180.0);
      float sin = Mth.sin(yawRad);
      float cos = Mth.cos(yawRad);
      return new Vec3(scaled.x * cos - scaled.z * sin, scaled.y, scaled.z * cos + scaled.x * sin);
   }

   private float getMovementSpeed(float slipperiness) {
      return this.onGround ? this.getMovementSpeed() * (0.21600002F / (slipperiness * slipperiness * slipperiness)) : this.getAirStrafingSpeed();
   }

   private float getAirStrafingSpeed() {
      float speed = 0.02F;
      return this.input.keyPresses.sprint() ? speed + 0.006F : speed;
   }

   private float getMovementSpeed() {
      return 0.1F;
   }

   private void move(Vec3 movement) {
      Vec3 modifiedMovement = movement;
      modifiedMovement = this.adjustMovementForSneaking(modifiedMovement);
      Vec3 adjustedMovement = this.adjustMovementForCollisions(modifiedMovement);
      if (adjustedMovement.lengthSqr() > 1.0E-7) {
         this.pos = this.pos.add(adjustedMovement);
         this.boundingBox = this.player.getDimensions(this.player.getPose()).makeBoundingBox(this.pos);
      }

      boolean xCollision = !Mth.equal(movement.x, adjustedMovement.x);
      boolean zCollision = !Mth.equal(movement.z, adjustedMovement.z);
      this.horizontalCollision = xCollision || zCollision;
      this.verticalCollision = movement.y != adjustedMovement.y;
      this.onGround = this.verticalCollision && movement.y < 0.0;
      if (!this.isTouchingWater()) {
         this.checkWaterState();
      }

      if (this.onGround) {
         this.onLanding();
      } else if (movement.y < 0.0) {
         this.fallDistance = this.fallDistance - (float)movement.y;
      }

      Vec3 currentVel = this.velocity;
      if (this.horizontalCollision || this.verticalCollision) {
         this.velocity = new Vec3(xCollision ? 0.0 : currentVel.x, this.onGround ? 0.0 : currentVel.y, zCollision ? 0.0 : currentVel.z);
      }
   }

   private Vec3 adjustMovementForCollisions(Vec3 movement) {
      AABB box = new AABB(-0.3, 0.0, -0.3, 0.3, 1.8, 0.3).move(this.pos);
      List<VoxelShape> collisionShapes = Collections.emptyList();
      Vec3 adjusted;
      if (movement.lengthSqr() == 0.0) {
         adjusted = movement;
      } else {
         adjusted = Entity.collideBoundingBox(this.player, movement, box, this.player.level(), collisionShapes);
      }

      boolean xCollide = movement.x != adjusted.x;
      boolean yCollide = movement.y != adjusted.y;
      boolean zCollide = movement.z != adjusted.z;
      boolean stepPossible = this.onGround || yCollide && movement.y < 0.0;
      if (this.player.maxUpStep() > 0.0F && stepPossible && (xCollide || zCollide)) {
         Vec3 stepAdjust = Entity.collideBoundingBox(
            this.player, new Vec3(movement.x, this.player.maxUpStep(), movement.z), box, this.player.level(), collisionShapes
         );
         Vec3 stepOffset = Entity.collideBoundingBox(
            this.player, new Vec3(0.0, this.player.maxUpStep(), 0.0), box.expandTowards(movement.x, 0.0, movement.z), this.player.level(), collisionShapes
         );
         Vec3 combined = Entity.collideBoundingBox(
               this.player, new Vec3(movement.x, 0.0, movement.z), box.move(stepOffset), this.player.level(), collisionShapes
            )
            .add(stepOffset);
         if (stepOffset.y < this.player.maxUpStep() && combined.horizontalDistanceSqr() > stepAdjust.horizontalDistanceSqr()) {
            stepAdjust = combined;
         }

         if (stepAdjust.horizontalDistanceSqr() > adjusted.horizontalDistanceSqr()) {
            return stepAdjust.add(
               Entity.collideBoundingBox(
                  this.player, new Vec3(0.0, -stepAdjust.y + movement.y, 0.0), box.move(stepAdjust), this.player.level(), collisionShapes
               )
            );
         }
      }

      return adjusted;
   }

   private void onLanding() {
      this.fallDistance = 0.0F;
   }

   public void jump() {
      this.velocity = this.velocity.add(0.0, this.getJumpVelocity() - this.velocity.y, 0.0);
      if (this.isSprinting()) {
         float rad = (float)Math.toRadians(this.yaw);
         this.velocity = this.velocity.add(-Mth.sin(rad) * 0.2, 0.0, Mth.cos(rad) * 0.2);
      }
   }

   private Vec3 applyClimbingSpeed(Vec3 motion) {
      if (!this.isClimbing()) {
         return motion;
      }

      this.onLanding();
      double clampedX = Mth.clamp(motion.x, -0.15F, 0.15F);
      double clampedZ = Mth.clamp(motion.z, -0.15F, 0.15F);
      double clampedY = Math.max(motion.y, -0.15F);
      if (clampedY < 0.0 && !this.getState(this.posToBlockPos(this.pos)).is(Blocks.SCAFFOLDING) && this.player.isSuppressingSlidingDownLadder()) {
         clampedY = 0.0;
      }

      return new Vec3(clampedX, clampedY, clampedZ);
   }

   public boolean isClimbing() {
      BlockPos posBlock = this.posToBlockPos(this.pos);
      BlockState state = this.getState(posBlock);
      return state.is(BlockTags.CLIMBABLE) ? true : state.getBlock() instanceof TrapDoorBlock && this.canEnterTrapdoor(posBlock, state);
   }

   private boolean canEnterTrapdoor(BlockPos pos, BlockState state) {
      if (!(Boolean)state.getValue(TrapDoorBlock.OPEN)) {
         return false;
      }

      BlockState below = this.player.level().getBlockState(pos.below());
      return below.is(Blocks.LADDER) && ((Direction)below.getValue(LadderBlock.FACING)).equals(state.getValue(TrapDoorBlock.FACING));
   }

   private Vec3 adjustMovementForSneaking(Vec3 movement) {
      if (movement.y <= 0.0 && this.method_30263()) {
         double dx = movement.x;
         double dz = movement.z;

         double step;
         for (step = 0.05; dx != 0.0 && this.player.level().noCollision(this.player, this.boundingBox.move(dx, -0.5, 0.0)); dx += dx > 0.0 ? -step : step) {
            if (dx < step && dx >= -step) {
               dx = 0.0;
               break;
            }
         }

         while (dz != 0.0 && this.player.level().noCollision(this.player, this.boundingBox.move(0.0, -0.5, dz))) {
            if (dz < step && dz >= -step) {
               dz = 0.0;
               break;
            }

            dz += dz > 0.0 ? -step : step;
         }

         while (dx != 0.0 && dz != 0.0 && this.player.level().noCollision(this.player, this.boundingBox.move(dx, -0.5, dz))) {
            dx = dx < step && dx >= -step ? 0.0 : (dx > 0.0 ? dx - step : dx + step);
            if (dz < step && dz >= -step) {
               dz = 0.0;
               break;
            }

            dz += dz > 0.0 ? -step : step;
         }

         if (movement.x != dx || movement.z != dz) {
            this.clipLedged = true;
         }

         if (this.shouldClipAtLedge()) {
            movement = new Vec3(dx, movement.y, dz);
         }
      }

      return movement;
   }

   protected boolean shouldClipAtLedge() {
      return this.input.keyPresses.shift() || this.input.forceSafeWalk;
   }

   private boolean method_30263() {
      return this.onGround
         || this.fallDistance < 0.5 && !this.player.level().noCollision(this.player, this.boundingBox.move(0.0, this.fallDistance - 0.5, 0.0));
   }

   public boolean isSprinting() {
      return this.sprinting;
   }

   private float getJumpVelocity() {
      return 0.42F * this.getJumpVelocityMultiplier() + this.getJumpBoostVelocityModifier();
   }

   private float getJumpBoostVelocityModifier() {
      if (this.hasStatusEffect(MobEffects.JUMP_BOOST)) {
         MobEffectInstance boost = this.getStatusEffect(MobEffects.JUMP_BOOST);
         return 0.1F * (boost.getAmplifier() + 1);
      } else {
         return 0.0F;
      }
   }

   private float getJumpVelocityMultiplier() {
      float multiplier1 = 0.0F;
      Block block = this.getState(this.posToBlockPos(this.pos)).getBlock();
      if (block != null) {
         multiplier1 = block.getJumpFactor();
      }

      float multiplier2 = 0.0F;
      Block block2 = this.getState(this.getVelocityAffectingPos()).getBlock();
      if (block2 != null) {
         multiplier2 = block2.getJumpFactor();
      }

      return multiplier1 == 1.0F ? multiplier2 : multiplier1;
   }

   private boolean doesNotCollide(double offsetX, double offsetY, double offsetZ) {
      return this.doesNotCollide(this.boundingBox.move(offsetX, offsetY, offsetZ));
   }

   private boolean doesNotCollide(AABB box) {
      return this.player.level().noCollision(this.player, box) && !this.player.level().containsAnyLiquid(box);
   }

   private void swimUpward(TagKey<Fluid> fluidTag) {
      this.velocity = this.velocity.add(0.0, 0.04F, 0.0);
   }

   private BlockPos getVelocityAffectingPos() {
      return BlockPos.containing(this.pos.x, this.boundingBox.minY - 0.5000001, this.pos.z);
   }

   private double getSwimHeight() {
      return this.player.getEyeHeight() < 0.4 ? 0.0 : 0.4;
   }

   public boolean isTouchingWater() {
      return this.touchingWater;
   }

   public boolean isInLava() {
      return this.fluidHeight.getDouble(FluidTags.LAVA) > 0.0;
   }

   private void checkWaterState() {
      if (this.player.getVehicle() instanceof AbstractBoat boat && !boat.isUnderWater()) {
         this.touchingWater = false;
      } else {
         if (this.updateMovementInFluid(FluidTags.WATER, 0.014)) {
            this.onLanding();
            this.touchingWater = true;
         } else {
            this.touchingWater = false;
         }
      }
   }

   private void updateSwimming() {
      if (this.isSwimming) {
         this.isSwimming = this.isSprinting() && this.isTouchingWater() && !this.player.isPassenger();
      } else {
         this.isSwimming = this.isSprinting()
            && this.isSubmergedInWater()
            && !this.player.isPassenger()
            && this.player.level().getFluidState(this.posToBlockPos(this.pos)).is(FluidTags.WATER);
      }
   }

   private void updateSubmergedInWaterState() {
      this.submergedInWater = this.submergedFluidTag.contains(FluidTags.WATER);
      this.submergedFluidTag.clear();
      double eyeLevel = this.getEyeY() - 0.11111111F;
      if (!(
         this.player.getVehicle() instanceof AbstractBoat boat
            && !boat.isUnderWater()
            && boat.getBoundingBox().maxY >= eyeLevel
            && boat.getBoundingBox().minY <= eyeLevel
      )) {
         BlockPos posEye = BlockPos.containing(this.pos.x, eyeLevel, this.pos.z);
         FluidState fluidState = this.player.level().getFluidState(posEye);
         double height = posEye.getY() + fluidState.getHeight(this.player.level(), posEye);
         if (height > eyeLevel) {
            this.submergedFluidTag.addAll(fluidState.typeHolder().tags().toList());
         }
      }
   }

   private double getEyeY() {
      return this.pos.y + this.player.getEyeHeight();
   }

   public boolean isSubmergedInWater() {
      return this.submergedInWater && this.isTouchingWater();
   }

   private double getFluidHeight(TagKey<Fluid> tag) {
      return this.fluidHeight.getDouble(tag);
   }

   private boolean updateMovementInFluid(TagKey<Fluid> tag, double speed) {
      if (this.isRegionUnloaded()) {
         return false;
      }

      AABB box = this.boundingBox.deflate(0.001);
      int i = Mth.floor(box.minX);
      int j = Mth.ceil(box.maxX);
      int k = Mth.floor(box.minY);
      int l = Mth.ceil(box.maxY);
      int m = Mth.floor(box.minZ);
      int n = Mth.ceil(box.maxZ);
      double d = 0.0;
      boolean pushedByFluids = true;
      boolean foundFluid = false;
      Vec3 fluidVelocity = Vec3.ZERO;
      int count = 0;
      MutableBlockPos mutable = new MutableBlockPos();

      for (int p = i; p < j; p++) {
         for (int q = k; q < l; q++) {
            for (int r = m; r < n; r++) {
               mutable.set(p, q, r);
               FluidState fluidState = this.player.level().getFluidState(mutable);
               if (fluidState.is(tag)) {
                  double e = q + fluidState.getHeight(this.player.level(), mutable);
                  if (e >= box.minY) {
                     foundFluid = true;
                     d = Math.max(e - box.minY, d);
                     if (pushedByFluids) {
                        Vec3 vel = fluidState.getFlow(this.player.level(), mutable);
                        if (d < 0.4) {
                           vel = vel.scale(d);
                        }

                        fluidVelocity = fluidVelocity.add(vel);
                        count++;
                     }
                  }
               }
            }
         }
      }

      if (fluidVelocity.length() > 0.0) {
         if (count > 0) {
            fluidVelocity = fluidVelocity.scale(1.0 / count);
         }

         fluidVelocity = fluidVelocity.scale(speed);
         if (Math.abs(this.velocity.x) < 0.003 && Math.abs(this.velocity.z) < 0.003 && fluidVelocity.length() < 0.0045) {
            fluidVelocity = fluidVelocity.normalize().scale(0.0045);
         }

         this.velocity = this.velocity.add(fluidVelocity);
      }

      this.fluidHeight.put(tag, d);
      return foundFluid;
   }

   private boolean isRegionUnloaded() {
      AABB box = this.boundingBox.inflate(1.0);
      int i = Mth.floor(box.minX);
      int j = Mth.ceil(box.maxX);
      int k = Mth.floor(box.minZ);
      int l = Mth.ceil(box.maxZ);
      return !this.player.level().hasChunksAt(i, k, j, l);
   }

   private Vec3 getRotationVector() {
      return this.getRotationVector(this.pitch, this.yaw);
   }

   private Vec3 getRotationVector(float pitch, float yaw) {
      float f = (float)(pitch * Math.PI / 180.0);
      float g = (float)(-yaw * Math.PI / 180.0);
      float h = Mth.cos(g);
      float i = Mth.sin(g);
      float j = Mth.cos(f);
      float k = Mth.sin(f);
      return new Vec3(i * j, -k, h * j);
   }

   public boolean hasStatusEffect(Holder<MobEffect> effect) {
      MobEffectInstance instance = this.player.getEffect(effect);
      return instance != null && instance.getDuration() >= this.simulatedTicks;
   }

   private MobEffectInstance getStatusEffect(Holder<MobEffect> effect) {
      MobEffectInstance instance = this.player.getEffect(effect);
      return instance != null && instance.getDuration() >= this.simulatedTicks ? instance : null;
   }

   public double getAttributeValue(Holder<Attribute> attribute) {
      return this.player.getAttributes().getValue(attribute);
   }

   public BlockPos posToBlockPos(Vec3 pos) {
      return new BlockPos(Mth.floor(pos.x), Mth.floor(pos.y), Mth.floor(pos.z));
   }

   public BlockState getState(BlockPos pos) {
      return this.player.level().getBlockState(pos);
   }

   public boolean isInWater() {
      return this.fluidHeight.getDouble(FluidTags.WATER) > 0.0;
   }

   public boolean isSwimming() {
      return this.isSwimming;
   }

   public Vec3 getMovementVector() {
      return this.velocity;
   }

   public boolean onGround() {
      return this.onGround;
   }

   public boolean isUnderWater() {
      return this.submergedInWater;
   }

   public boolean isFallFlying() {
      return this.isFallFlying;
   }

   public float getFallDistance() {
      return this.fallDistance;
   }

   public int getJumpingCooldown() {
      return this.jumpingCooldown;
   }

   public static class SimulatedPlayerInput {
      public Input keyPresses;
      public boolean forceSafeWalk = false;
      public float movementForward;
      public float movementSideways;

      public void update() {
         if (this.keyPresses.forward() != this.keyPresses.backward()) {
            this.movementForward = this.keyPresses.forward() ? 1.0F : -1.0F;
         } else {
            this.movementForward = 0.0F;
         }

         if (this.keyPresses.left() == this.keyPresses.right()) {
            this.movementSideways = 0.0F;
         } else {
            this.movementSideways = this.keyPresses.left() ? 1.0F : -1.0F;
         }

         if (this.keyPresses.shift()) {
            this.movementSideways *= 0.3F;
            this.movementForward *= 0.3F;
         }
      }

      public static PlayerSimulation.SimulatedPlayerInput fromClientPlayer(Input input) {
         PlayerSimulation.SimulatedPlayerInput simulatedInput = new PlayerSimulation.SimulatedPlayerInput();
         simulatedInput.keyPresses = input;
         return simulatedInput;
      }
   }
}

