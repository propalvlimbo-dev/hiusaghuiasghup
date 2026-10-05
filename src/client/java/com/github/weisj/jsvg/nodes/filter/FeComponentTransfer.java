package com.github.weisj.jsvg.nodes.filter;

import com.github.weisj.jsvg.attributes.ColorInterpolation;
import com.github.weisj.jsvg.attributes.filter.LayoutBounds;
import com.github.weisj.jsvg.attributes.filter.TransferFunctionType;
import com.github.weisj.jsvg.geometry.size.Length;
import com.github.weisj.jsvg.nodes.animation.Animate;
import com.github.weisj.jsvg.nodes.animation.Set;
import com.github.weisj.jsvg.nodes.container.ContainerNode;
import com.github.weisj.jsvg.nodes.prototype.spec.Category;
import com.github.weisj.jsvg.nodes.prototype.spec.ElementCategories;
import com.github.weisj.jsvg.nodes.prototype.spec.PermittedContent;
import com.github.weisj.jsvg.parser.impl.AttributeNode;
import com.github.weisj.jsvg.renderer.RenderContext;
import com.github.weisj.jsvg.util.ColorUtil;
import java.awt.image.BufferedImageFilter;
import java.awt.image.ByteLookupTable;
import java.awt.image.ImageFilter;
import java.awt.image.LookupOp;
import java.awt.image.LookupTable;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@ElementCategories(Category.FilterPrimitive)
@PermittedContent(categories = Category.TransferFunctionElement, anyOf = {Animate.class, Set.class})
public class FeComponentTransfer extends ContainerNode implements FilterPrimitive {
   public static final String TAG = "fecomponenttransfer";
   private FilterPrimitiveBase filterPrimitiveBase;
   private ByteLookupTable sRGBlookupTable;
   private ByteLookupTable linearRGBlookupTable;

   @NotNull
   @Override
   public String tagName() {
      return "fecomponenttransfer";
   }

   @Override
   public void build(@NotNull AttributeNode attributeNode) {
      super.build(attributeNode);
      this.filterPrimitiveBase = new FilterPrimitiveBase(attributeNode);
      List<TransferFunctionElement> nodes = this.childrenOfType(TransferFunctionElement.class);
      boolean redValid = false;
      boolean greenValid = false;
      boolean blueValid = false;
      boolean alphaValid = false;
      byte[][] tables = new byte[][]{
         TransferFunctionElement.IDENTITY_LOOKUP_TABLE,
         TransferFunctionElement.IDENTITY_LOOKUP_TABLE,
         TransferFunctionElement.IDENTITY_LOOKUP_TABLE,
         TransferFunctionElement.IDENTITY_LOOKUP_TABLE
      };

      for (TransferFunctionElement node : nodes) {
         switch (node.channel()) {
            case Red:
               redValid = node.type() != TransferFunctionType.Identity;
               tables[0] = node.lookupTable();
               break;
            case Green:
               greenValid = node.type() != TransferFunctionType.Identity;
               tables[1] = node.lookupTable();
               break;
            case Blue:
               blueValid = node.type() != TransferFunctionType.Identity;
               tables[2] = node.lookupTable();
               break;
            case Alpha:
               alphaValid = node.type() != TransferFunctionType.Identity;
               tables[3] = node.lookupTable();
         }
      }

      if (redValid || greenValid || blueValid || alphaValid) {
         this.sRGBlookupTable = new ByteLookupTable(0, tables);
      }

      this.children().clear();
   }

   @NotNull
   @Override
   public Length x() {
      return this.filterPrimitiveBase.x;
   }

   @NotNull
   @Override
   public Length y() {
      return this.filterPrimitiveBase.y;
   }

   @NotNull
   @Override
   public Length width() {
      return this.filterPrimitiveBase.width;
   }

   @NotNull
   @Override
   public Length height() {
      return this.filterPrimitiveBase.height;
   }

   @Override
   public ColorInterpolation colorInterpolation(@NotNull FilterContext filterContext) {
      return this.filterPrimitiveBase.colorInterpolation(filterContext);
   }

   @Override
   public void layoutFilter(@NotNull RenderContext context, @NotNull FilterLayoutContext filterLayoutContext) {
      LayoutBounds bounds = this.filterPrimitiveBase.layoutInput(filterLayoutContext).withFlags(new LayoutBounds.ComputeFlags(true));
      this.filterPrimitiveBase.saveLayoutResult(bounds, filterLayoutContext);
   }

   @Nullable
   private LookupTable lookupTable(@NotNull FilterContext filterContext) {
      if (this.sRGBlookupTable == null) {
         return null;
      }

      if (this.filterPrimitiveBase.colorInterpolation(filterContext) != ColorInterpolation.LinearRGB) {
         return this.sRGBlookupTable;
      }

      if (this.linearRGBlookupTable == null) {
         byte[][] tables = this.sRGBlookupTable.getTable();

         for (int j = 0; j < tables.length; j++) {
            byte[] table = tables[j];
            if (table != TransferFunctionElement.IDENTITY_LOOKUP_TABLE) {
               byte[] lRGBtable = new byte[table.length];

               for (int i = 0; i < table.length; i++) {
                  lRGBtable[i] = (byte)ColorUtil.linearRGBtoSRGBBand(table[ColorUtil.sRGBtoLinearRGBBand(i)] & 255);
               }

               tables[j] = lRGBtable;
            }
         }

         this.linearRGBlookupTable = new ByteLookupTable(0, tables);
      }

      return this.linearRGBlookupTable;
   }

   @Override
   public void applyFilter(@NotNull RenderContext context, @NotNull FilterContext filterContext) {
      LookupTable lookup = this.lookupTable(filterContext);
      if (lookup == null) {
         this.filterPrimitiveBase.noop(filterContext);
      } else {
         ImageFilter f = new BufferedImageFilter(new LookupOp(lookup, filterContext.renderingHints()));
         this.filterPrimitiveBase.saveResult(this.filterPrimitiveBase.inputChannel(filterContext).applyFilter(f), filterContext);
      }
   }
}

