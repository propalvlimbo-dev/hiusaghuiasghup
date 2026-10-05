package org.xrose.menu.ui.rows;

import org.xrose.feature.setting.BooleanSetting;
import org.xrose.feature.setting.ButtonSetting;
import org.xrose.feature.setting.ColorSetting;
import org.xrose.feature.setting.InputBindSetting;
import org.xrose.feature.setting.ModeSetting;
import org.xrose.feature.setting.MultiSelectSetting;
import org.xrose.feature.setting.NumberSetting;
import org.xrose.feature.setting.Setting;
import org.xrose.feature.setting.TextSetting;

public final class SettingRows {
   private SettingRows() {
   }

   public static SettingRow create(String featureName, Setting<?> setting) {
      SettingRow row = switch (setting) {
         case BooleanSetting booleanSetting -> new ToggleRow(booleanSetting);
         case ButtonSetting buttonSetting -> new ButtonRow(buttonSetting);
         case NumberSetting numberSetting -> new SliderRow(numberSetting);
         case ModeSetting modeSetting -> new DropdownRow(modeSetting);
         case MultiSelectSetting multiSelectSetting -> new DropdownRow(multiSelectSetting);
         case ColorSetting colorSetting -> new ColorRow(colorSetting);
         case TextSetting textSetting -> new TextRow(textSetting);
         case InputBindSetting inputBindSetting -> new BindChipRow(inputBindSetting);
         default -> null;
      };
      return row == null ? null : row.context(featureName);
   }
}

