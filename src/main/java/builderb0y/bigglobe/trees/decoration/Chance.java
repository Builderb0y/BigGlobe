package builderb0y.bigglobe.trees.decoration;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import builderb0y.autocodec.annotations.Mirror;
import builderb0y.autocodec.annotations.VerifyFloatRange;

@VerifyFloatRange(min = 0.0F, minInclusive = false, max = 1.0F, maxInclusive = true)
@Mirror(VerifyFloatRange.class)
@Target(ElementType.TYPE_USE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Chance {}