package org.xrose.feature;

public final class FeatureEnableRejectedException extends RuntimeException {
   public FeatureEnableRejectedException(String message) {
      super(message, null, false, false);
   }
}

