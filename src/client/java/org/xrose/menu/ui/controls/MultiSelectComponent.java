package org.xrose.menu.ui.controls;

import java.util.List;
import org.xrose.feature.setting.MultiSelectSetting;
import org.xrose.menu.i18n.MenuText;
import org.xrose.utils.text.StringUtil;

public final class MultiSelectComponent extends DropdownComponent {
   public MultiSelectComponent(MultiSelectSetting setting) {
      super(() -> selectedOptions(setting));
   }

   @Override
   protected String displayValue() {
      return this.value();
   }

   private static String selectedOptions(MultiSelectSetting setting) {
      List<String> selected = setting.getOptions().stream().filter(setting::isSelected).map(MenuText::option).toList();
      return selected.isEmpty() ? MenuText.option("Nothing selected") : StringUtil.joinLimited(selected, 2);
   }
}

