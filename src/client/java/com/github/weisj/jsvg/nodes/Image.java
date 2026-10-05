package com.github.weisj.jsvg.nodes;

import com.github.weisj.jsvg.attributes.Overflow;
import com.github.weisj.jsvg.attributes.PreserveAspectRatio;
import com.github.weisj.jsvg.attributes.value.PercentageDimension;
import com.github.weisj.jsvg.geometry.size.Length;
import com.github.weisj.jsvg.logging.Logger;
import com.github.weisj.jsvg.logging.impl.LogFactory;
import com.github.weisj.jsvg.nodes.prototype.spec.Category;
import com.github.weisj.jsvg.nodes.prototype.spec.ElementCategories;
import com.github.weisj.jsvg.nodes.prototype.spec.PermittedContent;
import com.github.weisj.jsvg.parser.impl.AttributeNode;
import com.github.weisj.jsvg.parser.impl.Url;
import com.github.weisj.jsvg.parser.resources.RenderableResource;
import com.github.weisj.jsvg.parser.resources.ResourceSupplier;
import com.github.weisj.jsvg.parser.resources.impl.MissingImageResource;
import com.github.weisj.jsvg.parser.resources.impl.ValueResourceSupplier;
import com.github.weisj.jsvg.renderer.MeasureContext;
import com.github.weisj.jsvg.renderer.RenderContext;
import com.github.weisj.jsvg.renderer.output.Output;
import com.github.weisj.jsvg.view.FloatSize;
import com.github.weisj.jsvg.view.ViewBox;
import java.awt.geom.AffineTransform;
import java.io.IOException;
import java.net.URI;
import java.util.Optional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@ElementCategories({Category.Graphic, Category.GraphicsReferencing})
@PermittedContent(categories = {Category.Animation, Category.Descriptive})
public final class Image extends RenderableSVGNode {
   private static final Logger LOGGER = LogFactory.createLogger(Image.class);
   public static final String TAG = "image";
   private Length x;
   private Length y;
   private Length width;
   private Length height;
   private PreserveAspectRatio preserveAspectRatio;
   private Overflow overflow;
   private ResourceSupplier<RenderableResource> imgResource;

   @NotNull
   @Override
   public String tagName() {
      return "image";
   }

   @Override
   public boolean isVisible(@NotNull RenderContext context) {
      return this.imgResource != null && super.isVisible(context);
   }

   @Override
   public void build(@NotNull AttributeNode attributeNode) {
      super.build(attributeNode);
      this.x = attributeNode.getLength("x", PercentageDimension.WIDTH, 0.0F);
      this.y = attributeNode.getLength("y", PercentageDimension.HEIGHT, 0.0F);
      this.width = attributeNode.getLength("width", PercentageDimension.WIDTH, Length.UNSPECIFIED);
      this.height = attributeNode.getLength("height", PercentageDimension.HEIGHT, Length.UNSPECIFIED);
      this.preserveAspectRatio = PreserveAspectRatio.parse(attributeNode.getValue("preserveAspectRatio"), attributeNode.parser());
      this.overflow = attributeNode.getEnum("overflow", Overflow.Hidden);
      Url url = Url.parse(attributeNode.getHref(), Url.RequireFragment.NO);
      if (url != null) {
         URI resolvedUri = attributeNode.resolveResourceURI(url.rawUrl());
         if (resolvedUri != null) {
            try {
               this.imgResource = attributeNode.resourceLoader().loadImage(attributeNode.document(), resolvedUri);
            } catch (IOException e) {
               LOGGER.log(Logger.Level.INFO, e.getMessage(), e);
               this.imgResource = null;
            }
         }
      }
   }

   @Nullable
   private RenderableResource fetchImage(@NotNull RenderContext context) {
      if (this.imgResource == null) {
         return null;
      }

      if (this.imgResource instanceof ValueResourceSupplier) {
         return ((ValueResourceSupplier<RenderableResource>)this.imgResource).get();
      }

      Optional<RenderableResource> optionalResource = this.imgResource.get(context.platformSupport());
      if (!optionalResource.isPresent()) {
         return null;
      }

      RenderableResource resource = optionalResource.get();
      this.imgResource = new ValueResourceSupplier<>(resource);
      return resource;
   }

   @Override
   public void render(@NotNull RenderContext context, @NotNull Output output) {
      RenderableResource resource = this.fetchImage(context);
      if (resource == null) {
         resource = new MissingImageResource();
      }

      MeasureContext measure = context.measureContext();
      FloatSize intrinsicResourceSize = resource.intrinsicSize(context);
      float resourceWidth = intrinsicResourceSize.width;
      float resourceHeight = intrinsicResourceSize.height;
      if (resourceWidth != 0.0F && resourceHeight != 0.0F) {
         float viewWidth = this.width.orElseIfUnspecified(resourceWidth).resolve(measure);
         float viewHeight = this.height.orElseIfUnspecified(resourceHeight).resolve(measure);
         output.translate(this.x.resolve(measure), this.y.resolve(measure));
         if (this.overflow.establishesClip()) {
            output.applyClip(new ViewBox(viewWidth, viewHeight));
         }

         AffineTransform imgTransform = this.preserveAspectRatio
            .computeViewportTransform(new FloatSize(viewWidth, viewHeight), new ViewBox(resourceWidth, resourceHeight));
         resource.render(output, context, imgTransform);
      }
   }
}

