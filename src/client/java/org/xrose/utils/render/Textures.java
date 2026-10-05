package org.xrose.utils.render;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.resources.Identifier;
import sdk.api.optimize.optimize;

@optimize
public final class Textures {
   private static final List<Identifier> ALL = new ArrayList<>();
   public static final Identifier TARGET = texture("target.png");

   private Textures() {
   }

   public static List<Identifier> all() {
      touch(Textures.Logos.BOOT, Textures.Hud.ARROW_OUTLINE, Textures.Shader.BLOOM, Textures.Header.SEARCH, Textures.Icons.BOXES);
      synchronized (ALL) {
         return List.copyOf(ALL);
      }
   }

   private static void touch(Identifier... constants) {
   }

   private static Identifier svg(String menuPath) {
      return texture("menu/" + menuPath + ".svg");
   }

   private static Identifier texture(String path) {
      return register(Identifier.parse("xrose:textures/" + path));
   }

   private static Identifier register(Identifier id) {
      synchronized (ALL) {
         ALL.add(id);
         return id;
      }
   }

   public static final class Header {
      public static final Identifier SEARCH = Textures.svg("header/search");
      public static final Identifier CHEVRON_LEFT = Textures.svg("header/chevron_left");
      public static final Identifier CHEVRON_RIGHT = Textures.svg("header/chevron_right");
      public static final Identifier SETTINGS = Textures.svg("header/settings");
      public static final Identifier FRIENDS = Textures.svg("header/friends");
      public static final Identifier PROFILE_ADD = Textures.svg("header/profile_add");
      public static final Identifier DOCUMENT = Textures.svg("header/document");
      public static final Identifier DEV_AVATAR = Textures.texture("menu/header/dev.png");
      public static final Identifier PROFILE_AVATAR = Textures.texture("menu/header/avatar_gray.png");

      private Header() {
      }
   }

   public static final class Hud {
      public static final Identifier ARROW_OUTLINE = Textures.texture("hud/arrow.png");
      public static final Identifier ARROW_FILLED = Textures.texture("hud/filled_arrow.png");

      private Hud() {
      }
   }

   public static final class Icons {
      public static final Identifier ACTIVITY = Textures.svg("icons/activity");
      public static final Identifier BOXES = Textures.svg("icons/boxes");
      public static final Identifier BRAIN = Textures.svg("icons/brain");
      public static final Identifier CHEVRON_DOWN = Textures.svg("icons/chevron_down");
      public static final Identifier CHEVRONS_LEFT_RIGHT = Textures.svg("icons/chevrons_left_right");
      public static final Identifier CLOCK = Textures.svg("icons/clock");
      public static final Identifier CHEVRONS_LEFT_RIGHT_ELLIPSIS = Textures.svg("icons/chevrons_left_right_ellipsis");
      public static final Identifier CIRCLE_PLUS = Textures.svg("icons/circle_plus");
      public static final Identifier COMMAND = Textures.svg("icons/command");
      public static final Identifier DELETE = Textures.svg("icons/delete");
      public static final Identifier DELETE_LEFT = Textures.svg("icons/delete_left");
      public static final Identifier DICES = Textures.svg("icons/dices");
      public static final Identifier EYE = Textures.svg("icons/eye");
      public static final Identifier GAMEPAD = Textures.svg("icons/gamepad_2");
      public static final Identifier GIFT = Textures.svg("icons/gift");
      public static final Identifier HARD_DRIVE = Textures.svg("icons/hard_drive");
      public static final Identifier KEYBOARD = Textures.svg("icons/keyboard");
      public static final Identifier MOVE_3D = Textures.svg("icons/move_3d");
      public static final Identifier OPTION = Textures.svg("icons/option");
      public static final Identifier PERSON_STANDING = Textures.svg("icons/person_standing");
      public static final Identifier PIN = Textures.svg("icons/pin");
      public static final Identifier PLUS = Textures.svg("icons/plus");
      public static final Identifier REFRESH_CCW = Textures.svg("icons/refresh_ccw");
      public static final Identifier SCAN_HEART = Textures.svg("icons/scan_heart");
      public static final Identifier SIGNAL = Textures.svg("icons/signal");
      public static final Identifier SPARKLES = Textures.svg("icons/sparkles");
      public static final Identifier SWORDS = Textures.svg("icons/swords");
      public static final Identifier TRIANGLE_ALERT = Textures.svg("icons/triangle_alert");
      public static final Identifier USER_ROUND = Textures.svg("icons/user_round");
      public static final Identifier USER_ROUND_PLUS = Textures.svg("icons/user_round_plus");
      public static final Identifier ZAP = Textures.svg("icons/zap");

      private Icons() {
      }
   }

   public static final class Logos {
      public static final Identifier BOOT = Textures.texture("menu/logo_boot.svg");
      public static final Identifier BOLT = Textures.texture("hud/logo_bolt.svg");
      public static final Identifier ROSE = Textures.texture("hud/logo_rose.png");
      public static final Identifier CORE_MARK = Textures.texture("hud/core_mark.png");
      public static final Identifier WATERMARK = Textures.texture("hud/watermark_logo.png");

      private Logos() {
      }
   }

   public static final class Shader {
      public static final Identifier BLOOM = Textures.texture("shader/bloom.png");
      public static final Identifier PARTICLE_GLOW = Textures.texture("particles/glow.png");
      public static final Identifier JUMP_FREQUENCY = Textures.texture("shader/jump_frequency.png");

      private Shader() {
      }
   }
}

