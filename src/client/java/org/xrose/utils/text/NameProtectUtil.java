package org.xrose.utils.text;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import org.xrose.feature.FeatureManager;
import org.xrose.feature.impl.misc.NameProtectFeature;

public final class NameProtectUtil {
   /** Обновляется каждый тик: активны ли NameProtect / StreamerMode / AutoRegister. */
   public static volatile boolean hooksActive = false;
   private static final String FEATURE_NAME = "NameProtect";

   private NameProtectUtil() {
   }

   public static String protect(String text) {
      if (text != null && !text.isEmpty()) {
         if (!hooksActive) {
            return text;
         }
         try {
            ru.rooyzee.elytrixclient.client.features.misc.AutoRegister.onDrawnText(text);
         } catch (Throwable ignored) {
         }
         String realName = realName();
         String fakeName = fakeName();
         if (blurMode()) {
            fakeName = blurOf(realName);
         }
         String result = !realName.isEmpty() && !fakeName.isEmpty() && !realName.equals(fakeName) && text.contains(realName) ? text.replace(realName, fakeName) : text;
         try {
            platform.api.event.events.client.TextVisitEvent event = new platform.api.event.events.client.TextVisitEvent(result);
            platform.api.event.EventManager.a((platform.api.event.interfaces.IEvent) event);
            return event.b();
         } catch (Throwable ignored) {
            return result;
         }
      } else {
         return text;
      }
   }

   private static boolean blurMode() {
      NameProtectFeature feature = (NameProtectFeature) FeatureManager.INSTANCE.getFeatures(org.xrose.feature.FeatureCategory.MISC)
         .stream().filter(f -> f instanceof NameProtectFeature).findFirst().orElse(null);
      return feature != null && feature.isEnabled() && "Blur".equals(feature.mode.getValue());
   }

   private static String blurOf(String name) {
      if (name == null || name.isEmpty()) return name;
      return "\u2588".repeat(name.length());
   }

   public static Component protect(Component component) {
      if (component == null) {
         return null;
      }

      String protectedText = protect(component.getString());
      return (Component)(protectedText.equals(component.getString()) ? component : Component.literal(protectedText).withStyle(component.getStyle()));
   }

   public static FormattedText protect(FormattedText text) {
      if (text == null) {
         return null;
      } else if (text instanceof Component component) {
         return protect(component);
      } else {
         String protectedText = protect(text.getString());
         return protectedText.equals(text.getString()) ? text : FormattedText.of(protectedText);
      }
   }

   public static FormattedCharSequence protect(FormattedCharSequence sequence) {
      if (sequence == null) {
         return null;
      }

      StringBuilder text = new StringBuilder();
      sequence.accept((index, style, codePoint) -> {
         text.appendCodePoint(codePoint);
         return true;
      });
      String original = text.toString();
      String protectedText = protect(original);
      return protectedText.equals(original) ? sequence : FormattedCharSequence.forward(protectedText, Style.EMPTY);
   }

   private static String realName() {
      Minecraft minecraft = Minecraft.getInstance();
      if (minecraft == null) {
         return "";
      } else {
         return minecraft.getUser() != null && minecraft.getUser().getName() != null ? minecraft.getUser().getName() : "";
      }
   }

   private static String fakeName() {
      if (FeatureManager.INSTANCE.getFeature("NameProtect") instanceof NameProtectFeature nameProtect && nameProtect.isEnabled()) {
         String value = nameProtect.name.getValue();
         return value == null ? "" : value.trim();
      } else {
         return "";
      }
   }
}

