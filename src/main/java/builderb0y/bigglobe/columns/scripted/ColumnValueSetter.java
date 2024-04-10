package builderb0y.bigglobe.columns.scripted;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ColumnValueSetter {

	public static final String DESCRIPTOR = 'L' + ColumnValueSetter.class.getName().replace('.', '/') + ';';

	/** the ID of the ColumnEntry which created the method annotated by this annotation. */
	public abstract String value();
}