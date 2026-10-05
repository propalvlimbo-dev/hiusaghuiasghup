package com.github.weisj.jsvg.parser.impl;

import com.github.weisj.jsvg.nodes.Anchor;
import com.github.weisj.jsvg.nodes.Circle;
import com.github.weisj.jsvg.nodes.ClipPath;
import com.github.weisj.jsvg.nodes.Defs;
import com.github.weisj.jsvg.nodes.Desc;
import com.github.weisj.jsvg.nodes.Ellipse;
import com.github.weisj.jsvg.nodes.Group;
import com.github.weisj.jsvg.nodes.Image;
import com.github.weisj.jsvg.nodes.Line;
import com.github.weisj.jsvg.nodes.LinearGradient;
import com.github.weisj.jsvg.nodes.Marker;
import com.github.weisj.jsvg.nodes.Mask;
import com.github.weisj.jsvg.nodes.Metadata;
import com.github.weisj.jsvg.nodes.Path;
import com.github.weisj.jsvg.nodes.Pattern;
import com.github.weisj.jsvg.nodes.Polygon;
import com.github.weisj.jsvg.nodes.Polyline;
import com.github.weisj.jsvg.nodes.RadialGradient;
import com.github.weisj.jsvg.nodes.Rect;
import com.github.weisj.jsvg.nodes.SVG;
import com.github.weisj.jsvg.nodes.SVGNode;
import com.github.weisj.jsvg.nodes.SolidColor;
import com.github.weisj.jsvg.nodes.Stop;
import com.github.weisj.jsvg.nodes.Style;
import com.github.weisj.jsvg.nodes.Symbol;
import com.github.weisj.jsvg.nodes.Title;
import com.github.weisj.jsvg.nodes.Use;
import com.github.weisj.jsvg.nodes.View;
import com.github.weisj.jsvg.nodes.animation.Animate;
import com.github.weisj.jsvg.nodes.animation.AnimateTransform;
import com.github.weisj.jsvg.nodes.animation.Set;
import com.github.weisj.jsvg.nodes.filter.DummyFilterPrimitive;
import com.github.weisj.jsvg.nodes.filter.FeBlend;
import com.github.weisj.jsvg.nodes.filter.FeColorMatrix;
import com.github.weisj.jsvg.nodes.filter.FeComponentTransfer;
import com.github.weisj.jsvg.nodes.filter.FeComposite;
import com.github.weisj.jsvg.nodes.filter.FeDisplacementMap;
import com.github.weisj.jsvg.nodes.filter.FeDropShadow;
import com.github.weisj.jsvg.nodes.filter.FeFlood;
import com.github.weisj.jsvg.nodes.filter.FeGaussianBlur;
import com.github.weisj.jsvg.nodes.filter.FeMerge;
import com.github.weisj.jsvg.nodes.filter.FeMergeNode;
import com.github.weisj.jsvg.nodes.filter.FeOffset;
import com.github.weisj.jsvg.nodes.filter.FeTurbulence;
import com.github.weisj.jsvg.nodes.filter.Filter;
import com.github.weisj.jsvg.nodes.filter.TransferFunctionElement;
import com.github.weisj.jsvg.nodes.mesh.MeshGradient;
import com.github.weisj.jsvg.nodes.mesh.MeshPatch;
import com.github.weisj.jsvg.nodes.mesh.MeshRow;
import com.github.weisj.jsvg.nodes.text.Text;
import com.github.weisj.jsvg.nodes.text.TextPath;
import com.github.weisj.jsvg.nodes.text.TextSpan;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Supplier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class NodeSupplier {
   private final Map<String, Supplier<SVGNode>> constructorMap;

   public NodeSupplier() {
      this(new TreeMap<>(String.CASE_INSENSITIVE_ORDER));
   }

   public NodeSupplier(@NotNull Map<String, Supplier<SVGNode>> mapImpl) {
      mapImpl.clear();
      this.constructorMap = mapImpl;
      this.constructorMap.put("a", () -> new Anchor());
      this.constructorMap.put("clippath", () -> new ClipPath());
      this.constructorMap.put("defs", () -> new Defs());
      this.constructorMap.put("g", () -> new Group());
      this.constructorMap.put("image", () -> new Image());
      this.constructorMap.put("marker", () -> new Marker());
      this.constructorMap.put("mask", () -> new Mask());
      this.constructorMap.put("svg", () -> new SVG());
      this.constructorMap.put("style", () -> new Style());
      this.constructorMap.put("symbol", () -> new Symbol());
      this.constructorMap.put("use", () -> new Use());
      this.constructorMap.put("view", () -> new View());
      this.populateShapeNodeConstructors();
      this.populatePaintNodeConstructors();
      this.populateTextNodeConstructors();
      this.populateFilterNodeConstructors();
      this.populateAnimationNodeConstructors();
      this.populateMetaNodeConstructors();
      this.populateDummyNodeConstructors();
   }

   @Nullable
   public SVGNode create(@NotNull String tagName) {
      Supplier<SVGNode> supplier = this.constructorMap.get(tagName);
      return supplier == null ? null : supplier.get();
   }

   private void populateShapeNodeConstructors() {
      this.constructorMap.put("circle", () -> new Circle());
      this.constructorMap.put("ellipse", () -> new Ellipse());
      this.constructorMap.put("line", () -> new Line());
      this.constructorMap.put("path", () -> new Path());
      this.constructorMap.put("polygon", () -> new Polygon());
      this.constructorMap.put("polyline", () -> new Polyline());
      this.constructorMap.put("rect", () -> new Rect());
   }

   private void populatePaintNodeConstructors() {
      this.constructorMap.put("lineargradient", () -> new LinearGradient());
      this.constructorMap.put("meshgradient", () -> new MeshGradient());
      this.constructorMap.put("meshpatch", () -> new MeshPatch());
      this.constructorMap.put("meshrow", () -> new MeshRow());
      this.constructorMap.put("pattern", () -> new Pattern());
      this.constructorMap.put("radialgradient", () -> new RadialGradient());
      this.constructorMap.put("solidcolor", () -> new SolidColor());
      this.constructorMap.put("stop", () -> new Stop());
   }

   private void populateTextNodeConstructors() {
      this.constructorMap.put("text", () -> new Text());
      this.constructorMap.put("textpath", () -> new TextPath());
      this.constructorMap.put("tspan", () -> new TextSpan());
   }

   private void populateFilterNodeConstructors() {
      this.constructorMap.put("filter", () -> new Filter());
      this.constructorMap.put("feblend", () -> new FeBlend());
      this.constructorMap.put("fecolormatrix", () -> new FeColorMatrix());
      this.constructorMap.put("fecomposite", () -> new FeComposite());
      this.constructorMap.put("fedisplacementmap", () -> new FeDisplacementMap());
      this.constructorMap.put("fedropshadow", () -> new FeDropShadow());
      this.constructorMap.put("feflood", () -> new FeFlood());
      this.constructorMap.put("fegaussianblur", () -> new FeGaussianBlur());
      this.constructorMap.put("feMerge", () -> new FeMerge());
      this.constructorMap.put("feMergeNode", () -> new FeMergeNode());
      this.constructorMap.put("feturbulence", () -> new FeTurbulence());
      this.constructorMap.put("feOffset", () -> new FeOffset());
      this.constructorMap.put("fecomponenttransfer", () -> new FeComponentTransfer());
      this.constructorMap.put("fefuncb", () -> new TransferFunctionElement.FeFuncB());
      this.constructorMap.put("fefuncg", () -> new TransferFunctionElement.FeFuncG());
      this.constructorMap.put("fefuncr", () -> new TransferFunctionElement.FeFuncR());
      this.constructorMap.put("fefunca", () -> new TransferFunctionElement.FeFuncA());
   }

   private void populateAnimationNodeConstructors() {
      this.constructorMap.put("animate", () -> new Animate());
      this.constructorMap.put("animatetransform", () -> new AnimateTransform());
      this.constructorMap.put("set", () -> new Set());
   }

   private void populateMetaNodeConstructors() {
      this.constructorMap.put("desc", () -> new Desc());
      this.constructorMap.put("metadata", () -> new Metadata());
      this.constructorMap.put("title", () -> new Title());
   }

   private void populateDummyNodeConstructors() {
      this.constructorMap.put("feConvolveMatrix", () -> new DummyFilterPrimitive("feConvolveMatrix"));
      this.constructorMap.put("feDiffuseLightning", () -> new DummyFilterPrimitive("feDiffuseLightning"));
      this.constructorMap.put("feImage", () -> new DummyFilterPrimitive("feImage"));
      this.constructorMap.put("feMorphology", () -> new DummyFilterPrimitive("feMorphology"));
      this.constructorMap.put("feSpecularLighting", () -> new DummyFilterPrimitive("feSpecularLighting"));
      this.constructorMap.put("feTile", () -> new DummyFilterPrimitive("feTile"));
   }
}

