package com.github.weisj.jsvg.nodes.filter;

import com.github.weisj.jsvg.attributes.filter.LayoutBounds;
import com.github.weisj.jsvg.geometry.noise.PerlinTurbulence;
import com.github.weisj.jsvg.geometry.size.FloatInsets;
import com.github.weisj.jsvg.nodes.animation.Animate;
import com.github.weisj.jsvg.nodes.animation.Set;
import com.github.weisj.jsvg.nodes.prototype.spec.Category;
import com.github.weisj.jsvg.nodes.prototype.spec.ElementCategories;
import com.github.weisj.jsvg.nodes.prototype.spec.PermittedContent;
import com.github.weisj.jsvg.parser.impl.AttributeNode;
import com.github.weisj.jsvg.renderer.RenderContext;
import com.github.weisj.jsvg.util.ImageUtil;
import java.awt.color.ColorSpace;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.DirectColorModel;
import java.awt.image.FilteredImageSource;
import java.awt.image.ImageFilter;
import java.awt.image.ImageProducer;
import java.awt.image.WritableRaster;
import org.jetbrains.annotations.NotNull;

@ElementCategories(Category.FilterPrimitive)
@PermittedContent(anyOf = {Animate.class, Set.class})
public final class FeTurbulence extends AbstractFilterPrimitive {
   public static final String TAG = "feturbulence";
   private float seed;
   private float[] baseFrequency;
   private int numOctaves;
   private FeTurbulence.Type type;

   @NotNull
   @Override
   public String tagName() {
      return "feturbulence";
   }

   @Override
   public void build(@NotNull AttributeNode attributeNode) {
      super.build(attributeNode);
      this.seed = attributeNode.getFloat("seed", 0.0F);
      this.baseFrequency = attributeNode.getFloatList("baseFrequency");
      if (this.baseFrequency.length == 0) {
         this.baseFrequency = new float[]{0.0F};
      }

      this.numOctaves = attributeNode.getInt("numOctaves", 1);
      this.numOctaves = Math.min(this.numOctaves, 8);
      this.type = attributeNode.getEnum("type", FeTurbulence.Type.fractalNoise);
   }

   @Override
   public void layoutFilter(@NotNull RenderContext context, @NotNull FilterLayoutContext filterLayoutContext) {
      this.impl()
         .saveLayoutResult(new LayoutBounds(filterLayoutContext.filterPrimitiveRegion(context.measureContext(), this), new FloatInsets()), filterLayoutContext);
   }

   @Override
   public void applyFilter(@NotNull RenderContext context, @NotNull FilterContext filterContext) {
      Filter.FilterInfo info = filterContext.info();
      Channel turbulenceChannel = new FeTurbulence.TurbulenceChannel(
         info.imageBounds(),
         info.imageWidth,
         info.imageHeight,
         this.seed,
         this.numOctaves,
         this.baseFrequency[0],
         this.baseFrequency.length > 1 ? this.baseFrequency[1] : this.baseFrequency[0],
         this.type
      );
      this.impl().saveResult(turbulenceChannel, filterContext);
   }

   public static final class TurbulenceChannel implements Channel, PixelProvider {
      private final PerlinTurbulence perlinTurbulence;
      private final double[] channels = new double[4];
      private final int imageWidth;
      private final int imageHeight;
      private final FeTurbulence.Type type;
      private final Rectangle2D tileBounds;
      private BufferedImage bufferedImage;

      public TurbulenceChannel(
         @NotNull Rectangle2D tileBounds,
         int imageWidth,
         int imageHeight,
         float seed,
         int octaves,
         double xFrequency,
         double yFrequency,
         FeTurbulence.Type type
      ) {
         this.tileBounds = tileBounds;
         this.imageWidth = imageWidth;
         this.imageHeight = imageHeight;
         this.type = type;
         this.perlinTurbulence = new PerlinTurbulence((int)seed, octaves, xFrequency, yFrequency);
      }

      @NotNull
      private BufferedImage ensureImageBackingStore() {
         if (this.bufferedImage == null) {
            ColorSpace cs = ColorSpace.getInstance(1004);
            ColorModel cm = new DirectColorModel(cs, 32, 16711680, 65280, 255, -16777216, false, 3);
            WritableRaster dest = cm.createCompatibleWritableRaster(this.imageWidth, this.imageHeight);
            this.bufferedImage = new BufferedImage(cm, dest, false, null);
            int w = dest.getWidth();
            int h = dest.getHeight();
            double scaleX = this.tileBounds.getWidth() / w;
            double scaleY = this.tileBounds.getHeight() / h;
            double startX = this.tileBounds.getX();
            double startY = this.tileBounds.getY();
            boolean fractalNoise = this.type == FeTurbulence.Type.fractalNoise;
            int[] destPixels = ImageUtil.getINT_RGBA_DataBank(dest);
            int dstAdjust = ImageUtil.getINT_RGBA_DataAdjust(dest);
            int dp = ImageUtil.getINT_RGBA_DataOffset(dest);
            double point1 = startY;

            for (int i = 0; i < h; i++) {
               double point0 = startX;

               for (int end = dp + w; dp < end; dp++) {
                  this.perlinTurbulence.turbulence(this.channels, point0, point1, fractalNoise, null, null);
                  destPixels[dp] = cm.getRGB(channelsToRGB(this.channels));
                  point0 += scaleX;
               }

               point1 += scaleY;
               dp += dstAdjust;
            }
         }

         return this.bufferedImage;
      }

      @NotNull
      @Override
      public ImageProducer producer() {
         return this.ensureImageBackingStore().getSource();
      }

      @NotNull
      @Override
      public BufferedImage toBufferedImageNonAliased(@NotNull RenderContext context) {
         BufferedImage img = this.ensureImageBackingStore();
         ColorModel cm = img.getColorModel();
         WritableRaster raster = img.copyData(null);
         return new BufferedImage(cm, raster, cm.isAlphaPremultiplied(), null);
      }

      @NotNull
      @Override
      public Channel applyFilter(@NotNull ImageFilter filter) {
         return new ImageProducerChannel(new FilteredImageSource(this.producer(), filter));
      }

      @NotNull
      @Override
      public PixelProvider pixels(@NotNull RenderContext context) {
         return this;
      }

      @Override
      public int pixelAt(double x, double y) {
         this.perlinTurbulence.turbulence(this.channels, x, y, this.type == FeTurbulence.Type.fractalNoise, null, null);
         return channelsToRGB(this.channels);
      }

      private static int channelsToRGB(double[] channels) {
         int i = (int)channels[0];
         int j;
         if ((i & -256) == 0) {
            j = i << 16;
         } else {
            j = (i & -2147483648) != 0 ? 0 : 16711680;
         }

         i = (int)channels[1];
         if ((i & -256) == 0) {
            j |= i << 8;
         } else {
            j |= (i & -2147483648) != 0 ? 0 : 65280;
         }

         i = (int)channels[2];
         if ((i & -256) == 0) {
            j |= i;
         } else {
            j |= (i & -2147483648) != 0 ? 0 : 255;
         }

         i = (int)channels[3];
         if ((i & -256) == 0) {
            j |= i << 24;
         } else {
            j |= (i & -2147483648) != 0 ? 0 : -16777216;
         }

         return j;
      }
   }

   public enum Type {
      fractalNoise,
      Turbulence;

      // $VF: synthetic method
      private static FeTurbulence.Type[] $values() {
         return new FeTurbulence.Type[]{fractalNoise, Turbulence};
      }
   }
}

