package com.github.weisj.jsvg.parser;

public class DocumentLimits {
   public static final int DEFAULT_MAX_USE_NESTING_DEPTH = 15;
   public static final int DEFAULT_MAX_NESTING_DEPTH = 30;
   public static final int DEFAULT_MAX_PATH_COUNT = 2000;
   public static final DocumentLimits DEFAULT = new DocumentLimits(30, 15, 2000);
   private final int maxNestingDepth;
   private final int maxUseNestingDepth;
   private final int maxPathCount;

   public DocumentLimits(int maxNestingDepth, int maxUseNestingDepth, int maxPathCount) {
      this.maxNestingDepth = maxNestingDepth;
      this.maxUseNestingDepth = maxUseNestingDepth;
      this.maxPathCount = maxPathCount;
   }

   public int maxNestingDepth() {
      return this.maxNestingDepth;
   }

   public int maxUseNestingDepth() {
      return this.maxUseNestingDepth;
   }

   public int maxPathCount() {
      return this.maxPathCount;
   }
}

