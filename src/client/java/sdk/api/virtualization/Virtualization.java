package sdk.api.virtualization;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import sdk.api.enums.VirtualizationMode;
import sdk.api.enums.VirtualizationType;

@Retention(RetentionPolicy.CLASS)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface Virtualization {
   VirtualizationType virtualization();

   VirtualizationMode mode();
}
