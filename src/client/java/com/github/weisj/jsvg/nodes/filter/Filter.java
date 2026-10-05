package com.github.weisj.jsvg.nodes.filter;

import com.github.weisj.jsvg.attributes.ColorInterpolation;
import com.github.weisj.jsvg.attributes.UnitType;
import com.github.weisj.jsvg.attributes.filter.DefaultFilterChannel;
import com.github.weisj.jsvg.attributes.filter.LayoutBounds;
import com.github.weisj.jsvg.attributes.value.PercentageDimension;
import com.github.weisj.jsvg.geometry.size.FloatInsets;
import com.github.weisj.jsvg.geometry.size.Length;
import com.github.weisj.jsvg.geometry.size.Unit;
import com.github.weisj.jsvg.geometry.util.GeometryUtil;
import com.github.weisj.jsvg.logging.Logger;
import com.github.weisj.jsvg.logging.impl.LogFactory;
import com.github.weisj.jsvg.nodes.SVGNode;
import com.github.weisj.jsvg.nodes.animation.Animate;
import com.github.weisj.jsvg.nodes.animation.Set;
import com.github.weisj.jsvg.nodes.container.ContainerNode;
import com.github.weisj.jsvg.nodes.prototype.spec.Category;
import com.github.weisj.jsvg.nodes.prototype.spec.ElementCategories;
import com.github.weisj.jsvg.nodes.prototype.spec.PermittedContent;
import com.github.weisj.jsvg.parser.impl.AttributeNode;
import com.github.weisj.jsvg.renderer.RenderContext;
import com.github.weisj.jsvg.renderer.impl.ElementBounds;
import com.github.weisj.jsvg.renderer.output.Output;
import com.github.weisj.jsvg.util.BlittableImage;
import java.awt.geom.Rectangle2D;
import java.awt.geom.Rectangle2D.Double;
import java.awt.image.BufferedImage;
import java.awt.image.ImageProducer;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@ElementCategories({})
@PermittedContent(categories = {Category.Descriptive, Category.FilterPrimitive}, anyOf = {Animate.class, Set.class})
public final class Filter extends ContainerNode {
   private static final Logger LOGGER = LogFactory.createLogger(Filter.class);
   public static final String TAG = "filter";
   private static final Length DEFAULT_FILTER_COORDINATE_X = Unit.PERCENTAGE_WIDTH.valueOf(-10.0F);
   private static final Length DEFAULT_FILTER_COORDINATE_Y = Unit.PERCENTAGE_HEIGHT.valueOf(-10.0F);
   private static final Length DEFAULT_FILTER_WIDTH = Unit.PERCENTAGE_WIDTH.valueOf(120.0F);
   private static final Length DEFAULT_FILTER_HEIGHT = Unit.PERCENTAGE_HEIGHT.valueOf(120.0F);
   private static final Double NO_CLIP_BOUNDS = new Double(-5.992310449541053E307, -5.992310449541053E307, 1.1984620899082105E308, 1.1984620899082105E308);
   private Length x;
   private Length y;
   private Length width;
   private Length height;
   private UnitType filterUnits;
   private UnitType filterPrimitiveUnits;
   private ColorInterpolation colorInterpolation;
   private boolean isValid;

   @NotNull
   @Override
   public String tagName() {
      return "filter";
   }

   public boolean hasEffect() {
      return this.isValid && !this.children().isEmpty();
   }

   @Override
   public void build(@NotNull AttributeNode attributeNode) {
      super.build(attributeNode);
      this.isValid = true;

      for (SVGNode child : this.children()) {
         FilterPrimitive filterPrimitive = (FilterPrimitive)child;
         if (!filterPrimitive.isValid()) {
            this.isValid = false;
            break;
         }
      }

      this.filterUnits = attributeNode.getEnum("filterUnits", UnitType.ObjectBoundingBox);
      this.filterPrimitiveUnits = attributeNode.getEnum("primitiveUnits", UnitType.UserSpaceOnUse);
      this.colorInterpolation = attributeNode.getEnum("color-interpolation-filters", ColorInterpolation.LinearRGB);
      this.x = attributeNode.getLength("x", PercentageDimension.WIDTH, DEFAULT_FILTER_COORDINATE_X)
         .coercePercentageToCorrectUnit(this.filterUnits, PercentageDimension.WIDTH);
      this.y = attributeNode.getLength("y", PercentageDimension.HEIGHT, DEFAULT_FILTER_COORDINATE_Y)
         .coercePercentageToCorrectUnit(this.filterUnits, PercentageDimension.HEIGHT);
      this.width = attributeNode.getLength("width", PercentageDimension.WIDTH, DEFAULT_FILTER_WIDTH)
         .coercePercentageToCorrectUnit(this.filterUnits, PercentageDimension.WIDTH);
      this.height = attributeNode.getLength("height", PercentageDimension.HEIGHT, DEFAULT_FILTER_HEIGHT)
         .coercePercentageToCorrectUnit(this.filterUnits, PercentageDimension.HEIGHT);
   }

   @Nullable
   public Filter.FilterBounds createFilterBounds(@Nullable Output output, @NotNull RenderContext context, @NotNull ElementBounds elementBounds) {
      Double filterRegion = this.filterUnits.computeViewBounds(context.measureContext(), elementBounds.boundingBox(), this.x, this.y, this.width, this.height);
      Rectangle2D graphicsClipBounds = output != null ? output.clipBounds() : NO_CLIP_BOUNDS.getBounds2D();
      FilterLayoutContext filterLayoutContext = new FilterLayoutContext(this.filterPrimitiveUnits, elementBounds.boundingBox(), graphicsClipBounds);
      Rectangle2D clippedElementBounds = elementBounds.geometryBox().createIntersection(graphicsClipBounds);
      Rectangle2D effectiveFilterRegion = filterRegion.createIntersection(graphicsClipBounds);
      if (effectiveFilterRegion.isEmpty()) {
         return null;
      }

      LayoutBounds elementLayoutBounds = new LayoutBounds(effectiveFilterRegion, new FloatInsets());
      LayoutBounds clippedElementLayoutBounds = new LayoutBounds(clippedElementBounds, new FloatInsets());
      LayoutBounds sourceDependentBounds = elementLayoutBounds.transform(
         (data, flags) -> flags.operatesOnWholeFilterRegion ? data : clippedElementLayoutBounds.resolve(flags)
      );
      filterLayoutContext.resultChannels().addResult(DefaultFilterChannel.LastResult, elementLayoutBounds);
      filterLayoutContext.resultChannels().addResult(DefaultFilterChannel.SourceGraphic, sourceDependentBounds);
      filterLayoutContext.resultChannels().addResult(DefaultFilterChannel.SourceAlpha, sourceDependentBounds);

      for (SVGNode child : this.children()) {
         try {
            FilterPrimitive filterPrimitive = (FilterPrimitive)child;
            filterPrimitive.layoutFilter(context, filterLayoutContext);
         } catch (IllegalFilterStateException var15) {
         }
      }

      LayoutBounds.Data clipHeuristic = filterLayoutContext.resultChannels().get(DefaultFilterChannel.LastResult).resolve(LayoutBounds.ComputeFlags.INITIAL);
      FloatInsets insets = clipHeuristic.clipBoundsEscapeInsets();
      Rectangle2D clipHeuristicBounds = clipHeuristic.bounds().createIntersection(GeometryUtil.grow(graphicsClipBounds, insets));
      GeometryUtil.adjustForAliasing(clipHeuristicBounds);
      return new Filter.FilterBounds(elementBounds.boundingBox(), filterRegion, clipHeuristicBounds);
   }

   @NotNull
   public BufferedImage applyFilter(@NotNull Output output, @NotNull RenderContext context, @NotNull Filter.FilterInfo filterInfo) {
      ImageProducer producer = filterInfo.blittableImage.image().getSource();
      FilterContext filterContext = new FilterContext(filterInfo, this.filterPrimitiveUnits, this.colorInterpolation, output.renderingHints());
      Channel sourceChannel = new ImageProducerChannel(producer);
      filterContext.resultChannels().addResult(DefaultFilterChannel.SourceGraphic, sourceChannel);
      filterContext.resultChannels().addResult(DefaultFilterChannel.LastResult, sourceChannel);
      filterContext.resultChannels().addResult(DefaultFilterChannel.SourceAlpha, () -> new Filter.SourceAlphaChannel(sourceChannel.alphaChannel().producer()));

      for (SVGNode child : this.children()) {
         try {
            FilterPrimitive filterPrimitive = (FilterPrimitive)child;
            filterPrimitive.applyFilter(context, filterContext);
         } catch (IllegalFilterStateException e) {
            LOGGER.log(Logger.Level.INFO, "Exception during filter", e);
         }
      }

      Channel result = Objects.requireNonNull(filterContext.getChannel(DefaultFilterChannel.LastResult));
      return result.toBufferedImageNonAliased(context);
   }

   @Override
   protected boolean acceptChild(@Nullable String id, @NotNull SVGNode node) {
      return node instanceof FilterPrimitive && super.acceptChild(id, node);
   }

   public static final class FilterBounds {
      @NotNull
      private final Rectangle2D elementBounds;
      @NotNull
      private final Rectangle2D filterRegion;
      @NotNull
      private final Rectangle2D effectiveFilterArea;

      private FilterBounds(@NotNull Rectangle2D elementBounds, @NotNull Rectangle2D filterRegion, @NotNull Rectangle2D effectiveFilterArea) {
         this.elementBounds = elementBounds;
         this.filterRegion = filterRegion;
         this.effectiveFilterArea = effectiveFilterArea;
      }

      @NotNull
      public Rectangle2D elementBounds() {
         return this.elementBounds;
      }

      @NotNull
      public Rectangle2D filterRegion() {
         return this.filterRegion;
      }

      @NotNull
      public Rectangle2D effectiveFilterArea() {
         return this.effectiveFilterArea;
      }
   }

   public static final class FilterInfo {
      public final int imageWidth;
      public final int imageHeight;
      @NotNull
      private final Filter.FilterBounds filterBounds;
      @NotNull
      private final BlittableImage blittableImage;
      @NotNull
      private final Output imageOutput;

      public FilterInfo(@NotNull BlittableImage blittableImage, @NotNull Output imageOutput, @NotNull Filter.FilterBounds filterBounds) {
         BufferedImage image = blittableImage.image();
         this.imageWidth = image.getWidth();
         this.imageHeight = image.getHeight();
         this.blittableImage = blittableImage;
         this.filterBounds = filterBounds;
         this.imageOutput = imageOutput;
      }

      @NotNull
      public Rectangle2D imageBounds() {
         return this.blittableImage.clippedUserBounds();
      }

      @NotNull
      public Rectangle2D filterRegion() {
         return this.filterBounds.filterRegion();
      }

      @NotNull
      public Rectangle2D elementBounds() {
         return this.filterBounds.elementBounds();
      }

      @NotNull
      public Output output() {
         return this.imageOutput;
      }

      @NotNull
      public Rectangle2D tile() {
         Rectangle2D elementBounds = this.elementBounds();
         Rectangle2D imageBounds = this.imageBounds();
         return new Double(
            imageBounds.getX() - elementBounds.getX(), imageBounds.getY() - elementBounds.getY(), imageBounds.getWidth(), imageBounds.getHeight()
         );
      }
   }

   private static final class SourceAlphaChannel extends ImageProducerChannel {
      public SourceAlphaChannel(@NotNull ImageProducer producer) {
         super(producer);
      }
   }
}

