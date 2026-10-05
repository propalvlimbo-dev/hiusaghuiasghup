package com.github.weisj.jsvg.nodes.filter;

import com.github.weisj.jsvg.attributes.filter.TransferFunctionType;
import com.github.weisj.jsvg.nodes.AbstractSVGNode;
import com.github.weisj.jsvg.nodes.animation.Animate;
import com.github.weisj.jsvg.nodes.animation.Set;
import com.github.weisj.jsvg.nodes.prototype.spec.Category;
import com.github.weisj.jsvg.nodes.prototype.spec.ElementCategories;
import com.github.weisj.jsvg.nodes.prototype.spec.PermittedContent;
import com.github.weisj.jsvg.parser.impl.AttributeNode;
import org.jetbrains.annotations.MustBeInvokedByOverriders;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class TransferFunctionElement extends AbstractSVGNode {
   static final byte[] IDENTITY_LOOKUP_TABLE = new byte[256];
   private final TransferFunctionElement.Channel channel;
   private TransferFunctionType type;
   private byte[] lookupTable;

   private TransferFunctionElement(TransferFunctionElement.Channel channel) {
      this.channel = channel;
   }

   public TransferFunctionElement.Channel channel() {
      return this.channel;
   }

   public TransferFunctionType type() {
      return this.type;
   }

   public byte @NotNull [] lookupTable() {
      return this.lookupTable;
   }

   @MustBeInvokedByOverriders
   @Override
   public void build(@NotNull AttributeNode attributeNode) {
      super.build(attributeNode);
      this.type = attributeNode.getEnum("type", TransferFunctionType.Identity);
      byte[] table = createLookupTable(this.type, attributeNode);
      if (table == null) {
         this.type = TransferFunctionType.Identity;
         this.lookupTable = IDENTITY_LOOKUP_TABLE;
      } else {
         this.lookupTable = table;
      }
   }

   private static byte @Nullable [] createLookupTable(TransferFunctionType type, @NotNull AttributeNode attributeNode) {
      switch (type) {
         case Table:
         case Discrete:
            float[] table = attributeNode.getFloatList("tableValues");
            if (table.length == 0) {
               return null;
            }

            int[] intTable = new int[table.length];

            for (int i = 0; i < table.length; i++) {
               intTable[i] = (int)(255.0F * table[i]);
            }

            return createTableBasedLookupTable(type, intTable);
         case Linear:
            float slope = attributeNode.getFloat("slope", 1.0F);
            float intercept = attributeNode.getFloat("intercept", 0.0F);
            if (slope == 1.0F && intercept == 0.0F) {
               return null;
            }

            return createLinearLookupTable(intercept, slope);
         case Gamma:
            float amplitude = attributeNode.getFloat("amplitude", 1.0F);
            float exponent = attributeNode.getFloat("exponent", 1.0F);
            float offset = attributeNode.getFloat("offset", 0.0F);
            if (amplitude == 1.0F && exponent == 1.0F && offset == 0.0F) {
               return null;
            }

            return createGammaLookupTable(amplitude, exponent, offset);
         case Identity:
            return IDENTITY_LOOKUP_TABLE;
         default:
            return null;
      }
   }

   private static byte @Nullable [] createTableBasedLookupTable(TransferFunctionType type, int[] intTable) {
      int n = intTable.length;
      byte[] lookupTable = new byte[256];
      switch (type) {
         case Table:
            for (int j = 0; j <= 255; j++) {
               float fi = j * (n - 1) / 255.0F;
               int k = (int)Math.floor(fi);
               int kNext = Math.min(k + 1, n - 1);
               float r = fi - k;
               int value = (int)(intTable[k] + r * (intTable[kNext] - intTable[k])) & 0xFF;
               lookupTable[j] = (byte)value;
            }
            break;
         case Discrete:
            for (int j = 0; j <= 255; j++) {
               int i = (int)Math.floor(j * n / 255.0F);
               if (i == n) {
                  i = n - 1;
               }

               lookupTable[j] = (byte)(intTable[i] & 0xFF);
            }
            break;
         default:
            return null;
      }

      return lookupTable;
   }

   private static byte @Nullable [] createLinearLookupTable(float intercept, float slope) {
      byte[] table = new byte[256];
      float intIntercept = intercept * 255.0F + 0.5F;

      for (int j = 0; j <= 255; j++) {
         int value = (int)(slope * j + intIntercept);
         value = Math.max(0, Math.min(255, value));
         table[j] = (byte)(0xFF & value);
      }

      return table;
   }

   private static byte @Nullable [] createGammaLookupTable(float amplitude, float exponent, float offset) {
      byte[] table = new byte[256];

      for (int j = 0; j <= 255; j++) {
         int value = (int)Math.round(255.0 * (amplitude * Math.pow(j / 255.0F, exponent) + offset));
         value = Math.max(0, Math.min(255, value));
         table[j] = (byte)(value & 0xFF);
      }

      return table;
   }

   static {
      for (int i = 0; i < 256; i++) {
         IDENTITY_LOOKUP_TABLE[i] = (byte)i;
      }
   }

   public enum Channel {
      Red,
      Green,
      Blue,
      Alpha;

      // $VF: synthetic method
      private static TransferFunctionElement.Channel[] $values() {
         return new TransferFunctionElement.Channel[]{Red, Green, Blue, Alpha};
      }
   }

   @ElementCategories(Category.TransferFunctionElement)
   @PermittedContent(anyOf = {Animate.class, Set.class})
   public static final class FeFuncA extends TransferFunctionElement {
      public static final String TAG = "fefunca";

      public FeFuncA() {
         super(TransferFunctionElement.Channel.Alpha);
      }

      @NotNull
      @Override
      public String tagName() {
         return "fefunca";
      }
   }

   @ElementCategories(Category.TransferFunctionElement)
   @PermittedContent(anyOf = {Animate.class, Set.class})
   public static final class FeFuncB extends TransferFunctionElement {
      public static final String TAG = "fefuncb";

      public FeFuncB() {
         super(TransferFunctionElement.Channel.Blue);
      }

      @NotNull
      @Override
      public String tagName() {
         return "fefuncb";
      }
   }

   @ElementCategories(Category.TransferFunctionElement)
   @PermittedContent(anyOf = {Animate.class, Set.class})
   public static final class FeFuncG extends TransferFunctionElement {
      public static final String TAG = "fefuncg";

      public FeFuncG() {
         super(TransferFunctionElement.Channel.Green);
      }

      @NotNull
      @Override
      public String tagName() {
         return "fefuncg";
      }
   }

   @ElementCategories(Category.TransferFunctionElement)
   @PermittedContent(anyOf = {Animate.class, Set.class})
   public static final class FeFuncR extends TransferFunctionElement {
      public static final String TAG = "fefuncr";

      public FeFuncR() {
         super(TransferFunctionElement.Channel.Red);
      }

      @NotNull
      @Override
      public String tagName() {
         return "fefuncr";
      }
   }
}

