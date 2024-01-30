package builderb0y.scripting.util;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Field;

import builderb0y.autocodec.util.AutoCodecUtil;
import builderb0y.scripting.bytecode.FieldInfo;
import builderb0y.scripting.bytecode.MethodInfo;
import builderb0y.scripting.bytecode.TypeInfo;

public class InfoHolder {

	public TypeInfo type;

	public InfoHolder() {
		this.init(this.getClass().getDeclaringClass());
	}

	public InfoHolder(Class<?> targetClass) {
		this.init(targetClass);
	}

	public void init(Class<?> targetClass) {
		this.type = TypeInfo.of(targetClass);
		ReflectionData reflectionData = ReflectionData.forClass(targetClass);
		RuntimeException exception = null;
		for (Field field : ReflectionData.forClass(this.getClass()).getDeclaredFields()) {
			try {
				if (field.getType() == FieldInfo.class) {
					field.set(this, FieldInfo.forField(reflectionData.getDeclaredField(field.getName())));
				}
				else if (field.getType() == MethodInfo.class) {
					Disambiguate annotation = field.getDeclaredAnnotation(Disambiguate.class);
					if (annotation != null) {
						if (annotation.name().equals("new")) {
							field.set(this, MethodInfo.forConstructor(reflectionData.findConstructor(annotation.paramTypes())));
						}
						else {
							field.set(this, MethodInfo.forMethod(reflectionData.findDeclaredMethod(annotation.name(), annotation.returnType(), annotation.paramTypes())));
						}
					}
					else {
						field.set(this, MethodInfo.forMethod(reflectionData.getDeclaredMethod(field.getName())));
					}
				}
			}
			catch (Throwable throwable) {
				if (exception == null) exception = new RuntimeException("Failed to initialize " + this.getClass());
				exception.addSuppressed(throwable);
			}
		}
		if (exception != null) throw exception;
	}

	@Target(ElementType.FIELD)
	@Retention(RetentionPolicy.RUNTIME)
	public static @interface Disambiguate {

		public abstract String name();

		public abstract Class<?> returnType();

		public abstract Class<?>[] paramTypes();
	}
}