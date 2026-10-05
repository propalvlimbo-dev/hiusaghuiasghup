package org.xrose.feature.impl.visual;

import java.util.Locale;
import org.xrose.feature.Feature;
import org.xrose.feature.FeatureCategory;
import org.xrose.feature.setting.ModeSetting;
import sdk.api.optimize.optimize;

@optimize
public final class CapeFeature extends Feature {
   public final ModeSetting mode = this.register(new ModeSetting("Style", "Glass", "AppIcon", "ASCII", "Barcode", "Glass", "MinimalPattern", "Motto", "Vandal"));

   public CapeFeature() {
      super("Capes", "Custom cosmetic capes visible on you", FeatureCategory.VISUAL, -1);
   }

   public CapeFeature.CapeStyle currentStyle() {
      try {
         return CapeFeature.CapeStyle.valueOf(this.mode.getValue().toUpperCase(Locale.ROOT));
      } catch (IllegalArgumentException e) {
         return CapeFeature.CapeStyle.GLASS;
      }
   }

   public enum CapeStyle {
      APPICON("appicon.png"),
      ASCII("ascii.png"),
      BARCODE("barcode.png"),
      GLASS("glass.png"),
      MINIMALPATTERN("minimalpatern.png"),
      MOTTO("motto.png"),
      VANDAL("vandal.png");

      private final String fileName;

      CapeStyle(String fileName) {
         this.fileName = fileName;
      }

      public String getFileName() {
         return this.fileName;
      }

      // $VF: synthetic method
      private static CapeFeature.CapeStyle[] $values() {
         return new CapeFeature.CapeStyle[]{APPICON, ASCII, BARCODE, GLASS, MINIMALPATTERN, MOTTO, VANDAL};
      }
   }
}

