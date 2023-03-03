package builderb0y.scripting.environments;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import builderb0y.scripting.environments.ClassScriptEnvironment.ExposeAsField;
import builderb0y.scripting.environments.WhitelistClassScriptEnvironment.Expose;

public class WrapperScriptEnvironment implements ScriptEnvironment {

	public final Class<?> wrapperClass;
	public final Class<?> wrappedClass;

	public WrapperScriptEnvironment(Class<?> wrapperClass, Class<?> wrappedClass) {
		this.wrapperClass = wrapperClass;
		this.wrappedClass = wrappedClass;
		for (Method method : wrapperClass.getDeclaredMethods()) {
			if (this.shouldExposeMethod(method)) {

			}
		}
	}

	public boolean hasExposeAnnotation(AnnotatedElement element) {
		return element.isAnnotationPresent(Expose.class) || element.isAnnotationPresent(ExposeAsField.class);
	}

	public boolean shouldExposeMethod(Method method) {
		return Modifier.isPublic(method.getModifiers()) && this.hasExposeAnnotation(method);
	}
}