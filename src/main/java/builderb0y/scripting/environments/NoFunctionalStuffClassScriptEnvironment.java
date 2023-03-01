package builderb0y.scripting.environments;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;

public class NoFunctionalStuffClassScriptEnvironment extends ClassScriptEnvironment {

	public static final Set<Package> FUNCTIONAL_PACKAGES = new HashSet<>(Set.of(
		Function.class.getPackage(),
		Stream.class.getPackage()
	));

	public NoFunctionalStuffClassScriptEnvironment(Class<?> clazz) {
		super(clazz);
	}

	@Override
	public boolean shouldExposeField(Field field) {
		return (
			super.shouldExposeField(field) &&
			!FUNCTIONAL_PACKAGES.contains(field.getType().getPackage())
		);
	}

	@Override
	public boolean shouldExposeMethod(Method method) {
		if (!super.shouldExposeMethod(method)) return false;
		if (FUNCTIONAL_PACKAGES.contains(method.getReturnType().getPackage())) return false;
		for (Class<?> parameterType : method.getParameterTypes()) {
			if (FUNCTIONAL_PACKAGES.contains(parameterType.getPackage())) return false;
		}
		return true;
	}

	@Override
	public boolean shouldExposeConstructor(Constructor<?> constructor) {
		if (!super.shouldExposeConstructor(constructor)) return false;
		for (Class<?> parameterType : constructor.getParameterTypes()) {
			if (FUNCTIONAL_PACKAGES.contains(parameterType.getPackage())) return false;
		}
		return true;
	}
}