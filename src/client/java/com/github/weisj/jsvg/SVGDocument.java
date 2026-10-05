package com.github.weisj.jsvg;

import com.github.weisj.jsvg.attributes.PreserveAspectRatio;
import com.github.weisj.jsvg.attributes.font.SVGFont;
import com.github.weisj.jsvg.nodes.SVG;
import com.github.weisj.jsvg.paint.SVGPaint;
import com.github.weisj.jsvg.parser.impl.DocumentConstructorAccessor;
import com.github.weisj.jsvg.renderer.MeasureContext;
import com.github.weisj.jsvg.renderer.NullPlatformSupport;
import com.github.weisj.jsvg.renderer.PlatformSupport;
import com.github.weisj.jsvg.renderer.RenderContext;
import com.github.weisj.jsvg.renderer.animation.Animation;
import com.github.weisj.jsvg.renderer.animation.AnimationState;
import com.github.weisj.jsvg.renderer.awt.AwtComponentPlatformSupport;
import com.github.weisj.jsvg.renderer.impl.NodeRenderer;
import com.github.weisj.jsvg.renderer.impl.context.RenderContextAccessor;
import com.github.weisj.jsvg.renderer.output.Output;
import com.github.weisj.jsvg.renderer.output.impl.CurrentColorProvider;
import com.github.weisj.jsvg.renderer.output.impl.ShapeOutput;
import com.github.weisj.jsvg.view.FloatSize;
import com.github.weisj.jsvg.view.ViewBox;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.Path2D.Float;
import javax.swing.JComponent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class SVGDocument {
   private static final boolean DEBUG = false;
   @NotNull
   private final SVG root;
   @NotNull
   private final FloatSize size;

   private SVGDocument(@NotNull SVG root) {
      this.root = root;
      this.size = this.sizeForViewport(null);
   }

   @NotNull
   public FloatSize size() {
      return this.size;
   }

   @NotNull
   public FloatSize sizeForViewport(@Nullable ViewBox viewport) {
      float em = SVGFont.defaultFontSize();
      return this.root.sizeForTopLevel(viewport, em, SVGFont.exFromEm(em));
   }

   @NotNull
   public ViewBox viewBox() {
      return this.root.staticViewBox(this.size());
   }

   @NotNull
   public Shape computeShape() {
      return this.computeShape(null);
   }

   @NotNull
   public Shape computeShape(@Nullable ViewBox viewBox) {
      Area accumulator = new Area(new Float());
      this.renderWithPlatform(NullPlatformSupport.INSTANCE, new ShapeOutput(accumulator), viewBox);
      return accumulator;
   }

   public boolean isAnimated() {
      return this.animation().duration() > 0L;
   }

   @NotNull
   public Animation animation() {
      return this.root.animationPeriod();
   }

   public void render(@Nullable JComponent component, @NotNull Graphics2D g) {
      this.render(component, g, null);
   }

   public void render(@Nullable Component component, @NotNull Graphics2D graphics2D, @Nullable ViewBox bounds) {
      PlatformSupport platformSupport = component != null ? new AwtComponentPlatformSupport(component) : NullPlatformSupport.INSTANCE;
      this.renderWithPlatform(platformSupport, graphics2D, bounds);
   }

   private float computePlatformFontSize(@NotNull PlatformSupport platformSupport, @NotNull Output output) {
      return output.contextFontSize().orElseGet(platformSupport::fontSize);
   }

   public void renderWithPlatform(@NotNull PlatformSupport platformSupport, @NotNull Graphics2D graphics2D, @Nullable ViewBox bounds) {
      Output output = Output.createForGraphics(graphics2D);
      this.renderWithPlatform(platformSupport, output, bounds);
      output.dispose();
   }

   public void renderWithPlatform(@NotNull PlatformSupport platformSupport, @NotNull Output output, @Nullable ViewBox viewportBounds) {
      this.renderWithPlatform(platformSupport, output, viewportBounds, null);
   }

   public void renderWithPlatform(
      @NotNull PlatformSupport platformSupport, @NotNull Output output, @Nullable ViewBox viewportBounds, @Nullable AnimationState animationState
   ) {
      RenderContext context = this.prepareRenderContext(platformSupport, output, viewportBounds, animationState);
      ViewBox rootVieBox = new ViewBox(this.root.size(context));
      if (viewportBounds == null) {
         viewportBounds = rootVieBox;
      }

      AffineTransform rootTransform = PreserveAspectRatio.forDisplay().computeViewportTransform(viewportBounds.size(), rootVieBox);
      RenderContext innerContext = NodeRenderer.setupInnerViewRenderContext(rootVieBox, context, true);
      output.applyClip(viewportBounds);
      innerContext.translate(output, viewportBounds.location());
      innerContext.transform(output, rootTransform);
      RenderContextAccessor.Accessor accessor = RenderContextAccessor.instance();
      accessor.setRootTransform(innerContext, output.transform());
      NodeRenderer.renderRootSVG(this.root, context, output);
   }

   @NotNull
   private RenderContext prepareRenderContext(
      @NotNull PlatformSupport platformSupport, @NotNull Output output, @Nullable ViewBox viewportBounds, @Nullable AnimationState animationState
   ) {
      float defaultEm = this.computePlatformFontSize(platformSupport, output);
      float defaultEx = SVGFont.exFromEm(defaultEm);
      AnimationState animState = animationState != null ? animationState : AnimationState.NO_ANIMATION;
      MeasureContext initialMeasure = viewportBounds != null
         ? MeasureContext.createInitial(viewportBounds.size(), defaultEm, defaultEx, animState)
         : MeasureContext.createInitial(this.root.sizeForTopLevel(null, defaultEm, defaultEx), defaultEm, defaultEx, animState);
      SVGPaint currentColor = null;
      if (output instanceof CurrentColorProvider) {
         currentColor = ((CurrentColorProvider)output).currentColor();
      }

      return RenderContextAccessor.instance().createInitial(currentColor, platformSupport, initialMeasure);
   }

   // $VF: synthetic method
   private static void lambda$renderWithPlatform$0(ViewBox finalBounds, Graphics2D g) {
      g.setColor(Color.RED);
      g.draw(finalBounds);
   }

   static {
      DocumentConstructorAccessor.setDocumentConstructor(SVGDocument::new);
   }
}

