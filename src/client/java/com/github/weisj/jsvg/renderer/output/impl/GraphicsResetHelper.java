package com.github.weisj.jsvg.renderer.output.impl;

import com.github.weisj.jsvg.renderer.output.Output;
import java.awt.Composite;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Stroke;
import java.awt.geom.AffineTransform;
import org.jetbrains.annotations.NotNull;

public class GraphicsResetHelper implements Output.SafeState {
   private final Graphics2D graphics;
   private final Composite originalComposite;
   private final Paint originalPaint;
   private final Stroke originalStroke;
   private final AffineTransform originalTransform;

   public GraphicsResetHelper(@NotNull Graphics2D graphics) {
      this.graphics = graphics;
      this.originalComposite = graphics.getComposite();
      this.originalPaint = graphics.getPaint();
      this.originalStroke = graphics.getStroke();
      this.originalTransform = graphics.getTransform();
   }

   @NotNull
   public Graphics2D graphics() {
      return this.graphics;
   }

   @Override
   public void restore() {
      this.graphics.setComposite(this.originalComposite);
      this.graphics.setPaint(this.originalPaint);
      this.graphics.setStroke(this.originalStroke);
      this.graphics.setTransform(this.originalTransform);
   }
}

