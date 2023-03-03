package builderb0y.scripting.environments;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class WhitelistClassScriptEnvironment extends ClassScriptEnvironment {

	public WhitelistClassScriptEnvironment(Class<?> clazz) {
		super(clazz);
	}

	public boolean hasWhitelistAnnotation(AnnotatedElement member) {
		return member.isAnnotationPresent(Expose.class) || member.isAnnotationPresent(ExposeAsField.class);
	}

	@Override
	public boolean shouldExposeField(Field field) {
		return super.shouldExposeField(field) && this.hasWhitelistAnnotation(field);
	}

	@Override
	public boolean shouldExposeMethod(Method method) {
		return super.shouldExposeMethod(method) && this.hasWhitelistAnnotation(method);
	}

	@Override
	public boolean shouldExposeConstructor(Constructor<?> constructor) {
		return super.shouldExposeConstructor(constructor) && this.hasWhitelistAnnotation(constructor);
	}

	@Target({ ElementType.FIELD, ElementType.METHOD, ElementType.CONSTRUCTOR })
	@Retention(RetentionPolicy.RUNTIME)
	public static @interface Expose {}
}