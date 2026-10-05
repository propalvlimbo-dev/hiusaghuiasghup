package com.github.weisj.jsvg.nodes.prototype.spec;

import com.github.weisj.jsvg.nodes.SVGNode;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface PermittedContent {
   Category[] categories() default {};

   Class<? extends SVGNode>[] anyOf() default {};

   boolean any() default false;

   boolean charData() default false;
}
