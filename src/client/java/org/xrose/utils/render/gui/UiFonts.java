package org.xrose.utils.render.gui;

import sdk.api.optimize.optimize;

@optimize
public final class UiFonts {
   private UiFonts() {
   }

   public static MsdfFontFamily googleSansFlex() {
      return UiFonts.Holder.GOOGLE_SANS_FLEX;
   }

   public static MsdfFont googleSansFlex(int weight) {
      return UiFonts.Holder.GOOGLE_SANS_FLEX.resolve(weight);
   }

   public static MsdfFontFamily googleSans() {
      return UiFonts.Holder.GOOGLE_SANS;
   }

   public static MsdfFont googleSans(int weight) {
      return UiFonts.Holder.GOOGLE_SANS.resolve(weight);
   }

   public static MsdfFontFamily sfPro() {
      return UiFonts.Holder.SF_PRO;
   }

   public static MsdfFont sfPro(int weight) {
      return UiFonts.Holder.SF_PRO.resolve(weight);
   }

   public static MsdfFont sfProDisplay() {
      return UiFonts.Holder.SF_PRO.regular();
   }

   public static MsdfFont getFontForText(String text, int weight) {
      if (text != null) {
         for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c >= 1024 && c <= 1279) {
               return googleSans(weight);
            }
         }
      }

      return googleSansFlex(weight);
   }

   private static final class Holder {
      private static final MsdfFontFamily GOOGLE_SANS_FLEX = MsdfFontFamily.builder()
         .variant(400, "xrose:fonts/google_sans_flex_regular.json")
         .variant(500, "xrose:fonts/google_sans_flex_medium.json")
         .variant(600, "xrose:fonts/google_sans_flex_semibold.json")
         .variant(700, "xrose:fonts/google_sans_flex_bold.json")
         .build();
      private static final MsdfFontFamily GOOGLE_SANS = MsdfFontFamily.builder()
         .variant(400, "xrose:fonts/google_sans_regular.json")
         .variant(500, "xrose:fonts/google_sans_medium.json")
         .variant(600, "xrose:fonts/google_sans_semibold.json")
         .variant(700, "xrose:fonts/google_sans_bold.json")
         .build();
      private static final MsdfFontFamily SF_PRO = MsdfFontFamily.builder()
         .variant(400, "xrose:fonts/sf_pro_regular.json")
         .variant(500, "xrose:fonts/sf_pro_medium.json")
         .variant(600, "xrose:fonts/sf_pro_semibold.json")
         .variant(700, "xrose:fonts/sf_pro_bold.json")
         .build();
   }
}

