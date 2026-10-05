package org.xrose.feature;

import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.xrose.event.EventTarget;
import org.xrose.event.events.input.KeyboardInputEvent;
import org.xrose.event.events.input.MouseInputEvent;
import org.xrose.event.events.lifecycle.ShutdownEvent;
import org.xrose.feature.impl.combat.AimAssistantFeature;
import org.xrose.feature.impl.combat.AntiBotFeature;
import org.xrose.feature.impl.combat.AuraFeature;
import org.xrose.feature.impl.combat.AutoClickerFeature;
import org.xrose.feature.impl.combat.AutoExplosionFeature;
import org.xrose.feature.impl.combat.AutoMaceFeature;
import org.xrose.feature.impl.combat.AutoTotemFeature;
import org.xrose.feature.impl.combat.BowAimbotFeature;
import org.xrose.feature.impl.combat.CriticalsFeature;
import org.xrose.feature.impl.combat.CrystalAuraFeature;
import org.xrose.feature.impl.combat.HitBoxesFeature;
import org.xrose.feature.impl.combat.HitSoundFeature;
import org.xrose.feature.impl.combat.LungeBoostFeature;
import org.xrose.feature.impl.combat.ReachFeature;
import org.xrose.feature.impl.combat.ShiftTapFeature;
import org.xrose.feature.impl.combat.TriggerBotFeature;
import org.xrose.feature.impl.combat.VelocityFeature;
import org.xrose.feature.impl.misc.AuctionHelperFeature;
import org.xrose.feature.impl.misc.AutoDuelFeature;
import org.xrose.feature.impl.misc.AutoTpaAcceptFeature;
import org.xrose.feature.impl.misc.ChestStealerFeature;
import org.xrose.feature.impl.misc.DeathCoordsFeature;
import org.xrose.feature.impl.misc.ElytraHelperFeature;
import org.xrose.feature.impl.misc.NameProtectFeature;
import org.xrose.feature.impl.misc.NoDelaysFeature;
import org.xrose.feature.impl.misc.ServerHelperFeature;
import org.xrose.feature.impl.misc.UseTrackerFeature;
import org.xrose.feature.impl.misc.XCarryFeature;
import org.xrose.feature.impl.movement.AirStuckFeature;
import org.xrose.feature.impl.movement.AutoJumpFeature;
import org.xrose.feature.impl.movement.ElytraBoosterFeature;
import org.xrose.feature.impl.movement.ElytraMotionFeature;
import org.xrose.feature.impl.movement.ElytraTargetFeature;
import org.xrose.feature.impl.movement.FlyFeature;
import org.xrose.feature.impl.movement.GrimCollideFeature;
import org.xrose.feature.impl.movement.GrimElytraFeature;
import org.xrose.feature.impl.movement.GrimFlyFeature;
import org.xrose.feature.impl.movement.HighJumpFeature;
import org.xrose.feature.impl.movement.InventoryMoveFeature;
import org.xrose.feature.impl.movement.NoFallFeature;
import org.xrose.feature.impl.movement.NoPushFeature;
import org.xrose.feature.impl.movement.NoSlowFeature;
import org.xrose.feature.impl.movement.NoWebFeature;
import org.xrose.feature.impl.movement.ParkourFeature;
import org.xrose.feature.impl.movement.SafeWalkFeature;
import org.xrose.feature.impl.movement.SpeedFeature;
import org.xrose.feature.impl.movement.SprintFeature;
import org.xrose.feature.impl.movement.StrafeFeature;
import org.xrose.feature.impl.movement.TimerFeature;
import org.xrose.feature.impl.movement.WallClimbFeature;
import org.xrose.feature.impl.movement.WaterSpeedFeature;
import org.xrose.feature.impl.player.AutoRespawnFeature;
import org.xrose.feature.impl.player.AutoSwapFeature;
import org.xrose.feature.impl.player.AutoToolFeature;
import org.xrose.feature.impl.player.ClickPearlFeature;
import org.xrose.feature.impl.player.DiscordRPCFeature;
import org.xrose.feature.impl.player.FarmHelperFeature;
import org.xrose.feature.impl.player.FastBreakFeature;
import org.xrose.feature.impl.player.FreeCamFeature;
import org.xrose.feature.impl.player.FreeLookFeature;
import org.xrose.feature.impl.player.FullBrightFeature;
import org.xrose.feature.impl.player.ItemScrollerFeature;
import org.xrose.feature.impl.player.MultiActionFeature;
import org.xrose.feature.impl.player.NoJumpBoostFeature;
import org.xrose.feature.impl.player.WindJumpFeature;
import org.xrose.feature.impl.pve.AntiAfkFeature;
import org.xrose.feature.impl.pve.AppleFarmerFeature;
import org.xrose.feature.impl.pve.AuctionRelistFeature;
import org.xrose.feature.impl.pve.AutoArmorFeature;
import org.xrose.feature.impl.pve.AutoAuthFeature;
import org.xrose.feature.impl.pve.AutoCrafterFeature;
import org.xrose.feature.impl.pve.AutoFishFeature;
import org.xrose.feature.impl.pve.AutoGappleFeature;
import org.xrose.feature.impl.pve.AutoLeaveFeature;
import org.xrose.feature.impl.pve.AutoPotionFeature;
import org.xrose.feature.impl.pve.AutoPottBotFeature;
import org.xrose.feature.impl.pve.AutoTpLootFeature;
import org.xrose.feature.impl.pve.AutoUseFeature;
import org.xrose.feature.impl.pve.ClanInvestFeature;
import org.xrose.feature.impl.pve.GriefJoinerFeature;
import org.xrose.feature.impl.pve.MineHelperFeature;
import org.xrose.feature.impl.pve.PveManagerFeature;
import org.xrose.feature.impl.visual.ArrowsFeature;
import org.xrose.feature.impl.visual.AtmoDawnFogFeature;
import org.xrose.feature.impl.visual.BlockEspFeature;
import org.xrose.feature.impl.visual.BlockOutlineFeature;
import org.xrose.feature.impl.visual.ChamsFeature;
import org.xrose.feature.impl.visual.CrosshairFeature;
import org.xrose.feature.impl.visual.EntityEspFeature;
import org.xrose.feature.impl.visual.HitParticlesFeature;
import org.xrose.feature.impl.visual.HoldMyItemsFeature;
import org.xrose.feature.impl.visual.HudFeature;
import org.xrose.feature.impl.visual.ItemPhysicsFeature;
import org.xrose.feature.impl.visual.JumpCirclesFeature;
import org.xrose.feature.impl.visual.KillEffectFeature;
import org.xrose.feature.impl.visual.NameTagsFeature;
import org.xrose.feature.impl.visual.PopChamsFeature;
import org.xrose.feature.impl.visual.RemovalsFeature;
import org.xrose.feature.impl.visual.SeeInvisibleFeature;
import org.xrose.feature.impl.visual.ShaderHandsFeature;
import org.xrose.feature.impl.visual.SwingAnimationFeature;
import org.xrose.feature.impl.visual.TargetESPFeature;
import org.xrose.feature.impl.visual.TrajectoriesFeature;
import org.xrose.feature.impl.visual.ViewModelFeature;
import org.xrose.feature.impl.visual.WorldParticlesFeature;
import org.xrose.feature.impl.visual.WorldTweaksFeature;
import org.xrose.feature.setting.BindSetting;

public final class FeatureManager {
   public static final FeatureManager INSTANCE = new FeatureManager();
   private final Map<String, Feature> features = new LinkedHashMap<>();
   private final Map<Class<? extends Feature>, Feature> featuresByType = new ConcurrentHashMap<>();
   private final FeatureConfigStore configStore = new FeatureConfigStore();
   private final ScheduledExecutorService configWriter = Executors.newSingleThreadScheduledExecutor(runnable -> {
      Thread thread = new Thread(runnable, "XRose Config Writer");
      thread.setDaemon(true);
      return thread;
   });
   private final Object saveLock = new Object();
   private final Map<Feature, Set<Integer>> activeHoldBinds = new IdentityHashMap<>();
   private ScheduledFuture<?> pendingSave;
   private boolean initialized;

   private FeatureManager() {
   }

   public void initialize() {
      if (!this.initialized) {
         List.of(
               new SprintFeature(),
               new AutoJumpFeature(),
               new AirStuckFeature(),
               new SpeedFeature(),
               new ElytraMotionFeature(),
               new FlyFeature(),
               new NoFallFeature(),
               new NoWebFeature(),
               new StrafeFeature(),
               new HighJumpFeature(),
               new GrimFlyFeature(),
               new InventoryMoveFeature(),
               new NoPushFeature(),
               new NoSlowFeature(),
               new ElytraBoosterFeature(),
               new SafeWalkFeature(),
               new BlockOutlineFeature(),
               new WallClimbFeature(),
               new WaterSpeedFeature(),
               new ParkourFeature(),
               new TimerFeature(),
               new GrimCollideFeature(),
               new GrimElytraFeature(),
               new ElytraTargetFeature(),
               new AuraFeature(),
               new AntiBotFeature(),
               new CriticalsFeature(),
               new TriggerBotFeature(),
               new CrystalAuraFeature(),
               new AutoTotemFeature(),
               new AutoClickerFeature(),
               new AutoMaceFeature(),
               new HitBoxesFeature(),
               new VelocityFeature(),
               new BowAimbotFeature(),
               new AutoExplosionFeature(),
               new HitSoundFeature(),
               new ReachFeature(),
               new ShiftTapFeature(),
               new AimAssistantFeature(),
               new LungeBoostFeature(),
               new HudFeature(),
               new ArrowsFeature(),
               new EntityEspFeature(),
               new NameTagsFeature(),
               new HitParticlesFeature(),
               new JumpCirclesFeature(),
               new KillEffectFeature(),
               new TrajectoriesFeature(),
               new WorldParticlesFeature(),
               new PopChamsFeature(),
               new SeeInvisibleFeature(),
               new RemovalsFeature(),
               new CrosshairFeature(),
               new SwingAnimationFeature(),
               new ViewModelFeature(),
               new ShaderHandsFeature(),
               new BlockEspFeature(),
               new TargetESPFeature(),
               new ChamsFeature(),
               new HoldMyItemsFeature(),
               new ItemPhysicsFeature(),
               new WorldTweaksFeature(),
               new AtmoDawnFogFeature(),
               new NameProtectFeature(),
               new NoDelaysFeature(),
               new DeathCoordsFeature(),
               new XCarryFeature(),
               new AuctionHelperFeature(),
               new ServerHelperFeature(),
               new UseTrackerFeature(),
               new AutoTpaAcceptFeature(),
               new AutoDuelFeature(),
               new ElytraHelperFeature(),
               new ChestStealerFeature(),
               new AutoSwapFeature(),
               new ItemScrollerFeature(),
               new AutoRespawnFeature(),
               new FarmHelperFeature(),
               new DiscordRPCFeature(),
               new FullBrightFeature(),
               new FreeCamFeature(),
               new FreeLookFeature(),
               new NoJumpBoostFeature(),
               new AutoToolFeature(),
               new ClickPearlFeature(),
               new FastBreakFeature(),
               new MultiActionFeature(),
               new WindJumpFeature(),
               PveManagerFeature.INSTANCE,
               new AutoFishFeature(),
               new AutoArmorFeature(),
               new AutoPotionFeature(),
               new AutoUseFeature(),
               new AutoGappleFeature(),
               new AntiAfkFeature(),
               new AutoLeaveFeature(),
               new AutoAuthFeature(),
               new MineHelperFeature(),
               new AutoTpLootFeature(),
               new AppleFarmerFeature(),
               new AutoCrafterFeature(),
               new AuctionRelistFeature(),
               new ClanInvestFeature(),
               new GriefJoinerFeature(),
               new AutoPottBotFeature()
            )
            .forEach(this::register);
         this.configStore.load(this);
         this.initialized = true;
         Runtime.getRuntime().addShutdownHook(new Thread(this::flushPendingSave, "XRose Config Flush"));
      }
   }

   public void save() {
      if (this.initialized) {
         JsonObject snapshot = this.configStore.snapshot(this);
         synchronized (this.saveLock) {
            if (this.pendingSave != null) {
               this.pendingSave.cancel(false);
            }

            this.pendingSave = this.configWriter.schedule(() -> this.configStore.save(snapshot), 2L, TimeUnit.SECONDS);
         }
      }
   }

   private void flushPendingSave() {
      synchronized (this.saveLock) {
         if (this.pendingSave == null) {
            return;
         }

         this.pendingSave.cancel(false);
         this.pendingSave = null;
      }

      this.configStore.save(this.configStore.snapshot(this));
   }

   public boolean saveConfigAs(String name) {
      return this.initialized && this.configStore.saveNamed(name, this.configStore.snapshot(this));
   }

   public boolean loadConfig(String name) {
      if (this.initialized && this.configStore.loadNamed(this, name)) {
         this.save();
         return true;
      } else {
         return false;
      }
   }

   public boolean deleteConfig(String name) {
      return this.configStore.deleteNamed(name);
   }

   public List<String> configNames() {
      return this.configStore.listNamed();
   }

   public void register(Feature feature) {
      String key = this.normalize(feature.getName());
      if (this.features.containsKey(key)) {
         throw new IllegalArgumentException("Duplicate feature name: " + feature.getName());
      }

      feature.setStateListener(this::save);
      this.features.put(key, feature);
      this.featuresByType.put((Class<? extends Feature>)feature.getClass(), feature);
   }

   public Collection<Feature> getFeatures() {
      return Collections.unmodifiableCollection(this.features.values());
   }

   public List<Feature> getFeatures(FeatureCategory category) {
      List<Feature> result = new ArrayList<>();

      for (Feature feature : this.features.values()) {
         if (feature.getCategory() == category) {
            result.add(feature);
         }
      }

      return result;
   }

   public Feature getFeature(String name) {
      return this.features.get(this.normalize(name));
   }

   public <T extends Feature> T getFeature(Class<T> type) {
      return type.cast(this.featuresByType.get(type));
   }

   public <T extends Feature> T getEnabled(Class<T> type) {
      T feature = this.getFeature(type);
      return feature != null && feature.isEnabled() ? feature : null;
   }

   @EventTarget
   public void onKeyboardInput(KeyboardInputEvent event) {
      Minecraft mc = Minecraft.getInstance();
      if (mc.gui.screen() == null || mc.gui.screen() instanceof AbstractContainerScreen) {
         this.dispatchBind(BindSetting.key(event.getKey()), event.getAction());
      }
   }

   @EventTarget
   public void onMouseInput(MouseInputEvent event) {
      Minecraft mc = Minecraft.getInstance();
      if (mc.gui.screen() == null || mc.gui.screen() instanceof AbstractContainerScreen) {
         this.dispatchBind(BindSetting.mouse(event.getButton()), event.getAction());
      }
   }

   @EventTarget
   public void onShutdown(ShutdownEvent event) {
      synchronized (this.saveLock) {
         if (this.pendingSave != null) {
            this.pendingSave.cancel(false);
            this.pendingSave = null;
         }
      }

      this.configStore.save(this.configStore.snapshot(this));
      this.configWriter.shutdown();
      this.initialized = false;
   }

   private String normalize(String value) {
      return value.toLowerCase(Locale.ROOT);
   }

   private void dispatchBind(int bindCode, int action) {
      if (action == 1 || action == 0) {
         for (Feature feature : this.features.values()) {
            if (feature.supportsBinds() && feature.isToggleable() && feature.getBind().matchesCode(bindCode)) {
               this.handleBindInput(feature, bindCode, action);
            }
         }
      }
   }

   private void handleBindInput(Feature feature, int bindCode, int action) {
      if (feature.getBindModeForCode(bindCode) == BindMode.HOLD) {
         Set<Integer> activeCodes = this.activeHoldBinds.computeIfAbsent(feature, ignored -> new LinkedHashSet<>());
         if (action == 1) {
            if (activeCodes.add(bindCode)) {
               feature.setEnabled(true);
            }
         } else {
            activeCodes.remove(bindCode);
            if (activeCodes.isEmpty()) {
               this.activeHoldBinds.remove(feature);
               feature.setEnabled(false);
            }
         }
      } else {
         if (action == 1) {
            feature.toggle();
         }
      }
   }
}

