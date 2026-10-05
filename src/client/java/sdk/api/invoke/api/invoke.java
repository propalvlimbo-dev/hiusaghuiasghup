package sdk.api.invoke.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import sdk.api.invoke.impl.PackerMode;
import sdk.api.invoke.impl.PackerType;

@Retention(RetentionPolicy.CLASS)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface invoke {
   PackerType ivirtualiz();

   PackerMode imode();
}
