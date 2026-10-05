package com.github.weisj.jsvg.ui;

import com.github.weisj.jsvg.animation.AnimationPeriod;
import com.github.weisj.jsvg.renderer.animation.Animation;
import com.github.weisj.jsvg.renderer.animation.AnimationState;
import javax.swing.Timer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class AnimationPlayer {
   private static final AnimationPeriod NO_ANIMATION = new AnimationPeriod(0L, 0L, false);
   private final Timer animationTimer = new Timer(0, e -> this.tick());
   @NotNull
   private final AnimationPlayer.FrameAction action;
   @NotNull
   private Animation animation = NO_ANIMATION;
   private long startTime;
   private long elapsedTime;

   public AnimationPlayer(@NotNull AnimationPlayer.FrameAction action) {
      this.action = action;
      this.animationTimer.setCoalesce(true);
      this.animationTimer.setRepeats(true);
   }

   public void setAnimation(@Nullable Animation animation) {
      this.animation = animation != null ? animation : NO_ANIMATION;
      this.animationTimer.setInitialDelay((int)this.animation.startTime());
   }

   @NotNull
   public AnimationState animationState() {
      return new AnimationState(0L, this.elapsedTime());
   }

   public boolean isRunning() {
      return this.animationTimer.isRunning();
   }

   public void start() {
      this.elapsedTime = 0L;
      this.resume();
   }

   public void stop() {
      this.pause();
      this.elapsedTime = 0L;
   }

   public void pause() {
      this.animationTimer.stop();
      this.elapsedTime = this.elapsedTime + (System.currentTimeMillis() - this.startTime);
      this.action.runFrame(this.elapsedTime());
   }

   public void resume() {
      if (this.elapsedTime() < this.animation.duration()) {
         this.startTime = System.currentTimeMillis();
         this.animationTimer.start();
      }
   }

   private long elapsedTime() {
      if (!this.animationTimer.isRunning()) {
         return this.elapsedTime;
      }

      long currentTime = System.currentTimeMillis();
      return currentTime - this.startTime + this.elapsedTime;
   }

   private void tick() {
      long time = this.elapsedTime();
      long maxTime = this.animation.endTime();
      if (time >= maxTime) {
         this.animationTimer.stop();
         time = maxTime;
      }

      this.action.runFrame(time);
   }

   @FunctionalInterface
   public interface FrameAction {
      void runFrame(long var1);
   }
}

