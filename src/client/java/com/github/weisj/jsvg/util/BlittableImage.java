package com.github.weisj.jsvg.util;

import com.github.weisj.jsvg.attributes.UnitType;
import com.github.weisj.jsvg.geometry.util.GeometryUtil;
import com.github.weisj.jsvg.nodes.SVGNode;
import com.github.weisj.jsvg.nodes.prototype.Instantiator;
import com.github.weisj.jsvg.renderer.RenderContext;
import com.github.weisj.jsvg.renderer.impl.NodeRenderer;
import com.github.weisj.jsvg.renderer.impl.context.RenderContextAccessor;
import com.github.weisj.jsvg.renderer.output.Output;
import com.github.weisj.jsvg.renderer.output.impl.Graphics2DOutput;
import com.github.weisj.jsvg.renderer.output.impl.GraphicsUtil;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class BlittableImage {
   @NotNull
   private final BufferedImage image;
   @NotNull
   private final RenderContext context;
   @NotNull
   private final Rectangle2D boundsInDeviceSpace;
   @NotNull
   private final Rectangle2D clippedUserBounds;

   private BlittableImage(
      @NotNull BufferedImage image, @NotNull RenderContext context, @NotNull Rectangle2D boundsInDeviceSpace, @NotNull Rectangle2D clippedUserBounds
   ) {
      this.image = image;
      this.context = context;
      this.boundsInDeviceSpace = boundsInDeviceSpace;
      this.clippedUserBounds = clippedUserBounds;
   }

   @Nullable
   public static BlittableImage create(
      @NotNull BlittableImage.BufferSurfaceSupplier bufferSurfaceSupplier,
      @NotNull RenderContext context,
      @Nullable Rectangle2D clipBounds,
      @NotNull Rectangle2D bounds,
      @NotNull Rectangle2D objectBounds,
      @NotNull UnitType contentUnits
   ) {
      RenderContextAccessor.Accessor accessor = RenderContextAccessor.instance();
      RenderContext imageContext = accessor.createInitial(
         accessor.currentColor(context), context.platformSupport(), contentUnits.deriveMeasure(context.measureContext())
      );
      return create(bufferSurfaceSupplier, context, clipBounds, bounds, objectBounds, contentUnits, imageContext);
   }

   @Nullable
   public static BlittableImage create(
      @NotNull BlittableImage.BufferSurfaceSupplier bufferSurfaceSupplier,
      @NotNull RenderContext context,
      @Nullable Rectangle2D clipBounds,
      @NotNull Rectangle2D bounds,
      @NotNull Rectangle2D objectBounds,
      @NotNull UnitType contentUnits,
      @NotNull RenderContext imageContext
   ) {
      Rectangle2D boundsInDeviceSpace = GeometryUtil.userBoundsToDeviceBounds(context, bounds);
      if (clipBounds != null) {
         Rectangle2D clipBoundsInDeviceSpace = GeometryUtil.userBoundsToDeviceBounds(context, clipBounds);
         Rectangle2D.intersect(clipBoundsInDeviceSpace, boundsInDeviceSpace, boundsInDeviceSpace);
      }

      if (ShapeUtil.isInvalidArea(boundsInDeviceSpace)) {
         return null;
      }

      GeometryUtil.adjustForAliasing(boundsInDeviceSpace);
      Rectangle2D adjustedBoundsInUserSpace = GeometryUtil.convertBounds(context, boundsInDeviceSpace, GeometryUtil.Space.DEVICE, GeometryUtil.Space.USER);
      BufferedImage img = bufferSurfaceSupplier.createBufferSurface(null, boundsInDeviceSpace.getWidth(), boundsInDeviceSpace.getHeight());
      AffineTransform rootTransform = new AffineTransform();
      rootTransform.translate(-boundsInDeviceSpace.getX(), -boundsInDeviceSpace.getY());
      rootTransform.concatenate(context.rootTransform());
      AffineTransform userSpaceTransform = new AffineTransform(context.userSpaceTransform());
      if (contentUnits == UnitType.ObjectBoundingBox) {
         userSpaceTransform = new AffineTransform(userSpaceTransform);
         userSpaceTransform.translate(objectBounds.getX(), objectBounds.getY());
         userSpaceTransform.scale(objectBounds.getWidth(), objectBounds.getHeight());
      }

      RenderContextAccessor.instance().setRootTransform(imageContext, rootTransform, userSpaceTransform);
      return new BlittableImage(img, imageContext, boundsInDeviceSpace, adjustedBoundsInUserSpace);
   }

   @NotNull
   public RenderContext context() {
      return this.context;
   }

   @NotNull
   public Rectangle2D imageBoundsInDeviceSpace() {
      return this.boundsInDeviceSpace;
   }

   @NotNull
   public Rectangle2D clippedUserBounds() {
      return this.clippedUserBounds;
   }

   @NotNull
   public BufferedImage image() {
      return this.image;
   }

   @NotNull
   public Graphics2D createGraphics() {
      Graphics2D g = GraphicsUtil.createGraphics(this.image);
      g.transform(this.context.rootTransform());
      g.transform(this.context.userSpaceTransform());
      return g;
   }

   public void renderNode(@NotNull Output parentOutput, @NotNull SVGNode node, @NotNull Instantiator instantiator) {
      this.render(parentOutput, (out, ctx) -> NodeRenderer.renderNode(node, ctx, out, instantiator));
   }

   public void clearBackground(@NotNull Color color) {
      Graphics2D g = this.image.createGraphics();
      g.setColor(color);
      g.fillRect(0, 0, this.image.getWidth(), this.image.getHeight());
      g.dispose();
   }

   public void render(@NotNull Output output, @NotNull Consumer<Graphics2D> painter) {
      Graphics2D imgGraphics = this.createGraphics();
      imgGraphics.setRenderingHints(output.renderingHints());
      painter.accept(imgGraphics);
      imgGraphics.dispose();
   }

   public void render(@NotNull Output output, @NotNull BiConsumer<Output, RenderContext> painter) {
      Graphics2D imgGraphics = this.createGraphics();
      imgGraphics.setRenderingHints(output.renderingHints());
      painter.accept(new Graphics2DOutput(imgGraphics), this.context);
      imgGraphics.dispose();
   }

   public void prepareForBlitting(@NotNull Output output) {
      output.setTransform(AffineTransform.getTranslateInstance(this.boundsInDeviceSpace.getX(), this.boundsInDeviceSpace.getY()));
   }

   public void blitTo(@NotNull Output output) {
      Output out = output.createChild();
      out.setTransform(AffineTransform.getTranslateInstance(this.boundsInDeviceSpace.getX(), this.boundsInDeviceSpace.getY()));
      out.drawImage(this.image);
      out.dispose();
   }

   public void debug(@NotNull Output output) {
      this.debug(output, true);
   }

   public void debug(@NotNull Output output, boolean drawImage) {
      output.debugPaint(g -> {
         g.setComposite(AlphaComposite.SrcOver.derive(0.5F));
         g.setTransform(AffineTransform.getTranslateInstance(this.boundsInDeviceSpace.getX(), this.boundsInDeviceSpace.getY()));
         if (drawImage) {
            g.drawImage(this.image, 0, 0, null);
         }

         g.setColor(Color.MAGENTA);
         g.drawRect(0, 0, this.image.getWidth(), this.image.getHeight());
      });
   }

   @FunctionalInterface
   public interface BufferSurfaceSupplier {
      @NotNull
      BufferedImage createBufferSurface(@Nullable AffineTransform var1, double var2, double var4);
   }
}

