package com.github.weisj.jsvg.nodes;

import com.github.weisj.jsvg.attributes.MaskType;
import com.github.weisj.jsvg.attributes.UnitType;
import com.github.weisj.jsvg.geometry.util.GeometryUtil;
import com.github.weisj.jsvg.nodes.container.ContainerNode;
import com.github.weisj.jsvg.nodes.prototype.HasShape;
import com.github.weisj.jsvg.nodes.prototype.ShapedContainer;
import com.github.weisj.jsvg.nodes.prototype.spec.Category;
import com.github.weisj.jsvg.nodes.prototype.spec.ElementCategories;
import com.github.weisj.jsvg.nodes.prototype.spec.PermittedContent;
import com.github.weisj.jsvg.nodes.text.Text;
import com.github.weisj.jsvg.paint.impl.MaskedPaint;
import com.github.weisj.jsvg.parser.PaintParser;
import com.github.weisj.jsvg.parser.impl.AttributeNode;
import com.github.weisj.jsvg.renderer.RenderContext;
import com.github.weisj.jsvg.renderer.impl.ElementBounds;
import com.github.weisj.jsvg.renderer.output.Output;
import com.github.weisj.jsvg.util.BlittableImage;
import com.github.weisj.jsvg.util.CachedSurfaceSupplier;
import com.github.weisj.jsvg.util.ImageUtil;
import com.github.weisj.jsvg.util.ShapeUtil;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Shape;
import java.awt.geom.Area;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import org.jetbrains.annotations.NotNull;

@ElementCategories({})
@PermittedContent(categories = {Category.Animation, Category.Descriptive, Category.Shape}, anyOf = {Use.class, Text.class})
public final class ClipPath extends ContainerNode implements ShapedContainer<SVGNode> {
   private static final boolean DEBUG = false;
   public static final String TAG = "clippath";
   private final CachedSurfaceSupplier surfaceSupplier = new CachedSurfaceSupplier(ImageUtil::createLuminosityBuffer);
   private boolean isValid;
   private UnitType clipPathUnits;

   @NotNull
   @Override
   public String tagName() {
      return "clippath";
   }

   public boolean isValid() {
      return this.isValid;
   }

   @Override
   public void build(@NotNull AttributeNode attributeNode) {
      super.build(attributeNode);
      this.isValid = this.checkIsValid();
      this.clipPathUnits = attributeNode.getEnum("clipPathUnits", UnitType.UserSpaceOnUse);
   }

   private boolean checkIsValid() {
      for (SVGNode child : this.children()) {
         if (child instanceof Use) {
            SVGNode referenced = ((Use)child).referencedNode();
            if (referenced != null && !this.isAcceptableType(referenced)) {
               return false;
            }
         }
      }

      return true;
   }

   @NotNull
   public Shape clipShape(@NotNull RenderContext context, @NotNull ElementBounds elementBounds, boolean useSoftClip) {
      Shape shape = ShapedContainer.super.elementShape(context, HasShape.Box.BoundingBox);
      if (!useSoftClip && this.clipPathUnits == UnitType.ObjectBoundingBox) {
         shape = ShapeUtil.transformShape(shape, this.clipPathUnits.viewTransform(elementBounds.boundingBox()));
      }

      Area areaShape = new Area(shape);
      return areaShape.isRectangular() ? areaShape.getBounds2D() : areaShape;
   }

   @NotNull
   public Paint createPaintForSoftClipping(
      @NotNull Output output, @NotNull RenderContext context, @NotNull ElementBounds elementBounds, @NotNull Shape clipShape
   ) {
      Rectangle2D transformedClipBounds = GeometryUtil.containingBoundsAfterTransform(
         this.clipPathUnits.viewTransform(elementBounds.boundingBox()), clipShape.getBounds2D()
      );
      boolean useCache = this.surfaceSupplier.useCache(output, context);
      BlittableImage blitImage = BlittableImage.create(
         this.surfaceSupplier.surfaceSupplier(useCache),
         context,
         output.clipBounds(),
         transformedClipBounds.createIntersection(elementBounds.geometryBox()),
         elementBounds.boundingBox(),
         this.clipPathUnits
      );
      if (blitImage == null) {
         return PaintParser.DEFAULT_COLOR;
      }

      blitImage.clearBackground(Color.BLACK);
      blitImage.render(output, g -> {
         g.setColor(Color.WHITE);
         g.fill(clipShape);
      });
      Point2D offset = GeometryUtil.getLocation(blitImage.imageBoundsInDeviceSpace());
      return new MaskedPaint(
         PaintParser.DEFAULT_COLOR, blitImage.image().getRaster(), offset, this.surfaceSupplier.resourceCleaner(output, useCache), MaskType.Luminance
      );
   }

   public void applyClip(@NotNull Output output, @NotNull RenderContext context, @NotNull ElementBounds elementBounds) {
      if (output.isSoftClippingEnabled()) {
         Rectangle2D bounds = elementBounds.geometryBox();
         if (!bounds.isEmpty()) {
            output.setPaint(() -> {
               Shape childClipShapex = this.clipShape(context, elementBounds, true);
               return this.createPaintForSoftClipping(output, context, elementBounds, childClipShapex);
            });
         }
      } else {
         Shape childClipShape = this.clipShape(context, elementBounds, false);
         output.applyClip(childClipShape);
      }
   }

   // $VF: synthetic method
   private static void lambda$applyClip$2(Shape childClipShape, Graphics2D g) {
      g.setClip(null);
      g.setPaint(Color.MAGENTA);
      g.draw(childClipShape);
   }
}

