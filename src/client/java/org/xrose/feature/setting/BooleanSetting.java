package org.xrose.feature.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

public final class BooleanSetting extends Setting<Boolean> {
   public BooleanSetting(String name, boolean defaultValue) {
      super(name, defaultValue);
   }

   public void toggle() {
      this.setValue(!this.getValue());
   }

   protected JsonElement writeValue(Boolean value) {
      return new JsonPrimitive(value);
   }

   protected Boolean readValue(JsonElement element) {
      return element.getAsBoolean();
   }
}

