package builderb0y.scripting.util;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import builderb0y.autocodec.util.AutoCodecUtil;

@Deprecated //replaced with ReflectionData.
public class UncheckedReflection {

	public static <T> Constructor<T> getDeclaredConstructor(Class<T> clazz, Class<?>... args) {
		try {
			return clazz.getDeclaredConstructor(args);
		}
		catch (NoSuchMethodException exception) {
			throw AutoCodecUtil.rethrow(exception);
		}
	}

	public static Method getDeclaredMethod(Class<?> clazz, String name, Class<?>... args) {
		try {
			return clazz.getDeclaredMethod(name, args);
		}
		catch (NoSuchMethodException exception) {
			throw AutoCodecUtil.rethrow(exception);
		}
	}

	public static Field getDeclaredField(Class<?> clazz, String name) {
		try {
			return clazz.getDeclaredField(name);
		}
		catch (NoSuchFieldException exception) {
			throw AutoCodecUtil.rethrow(exception);
		}
	}

	public static Class<?> findClass(String name) {
		try {
			return switch (name) {
				//these classes don't work in Class.forName().
				case "byte"    ->    byte.class;
				case "short"   ->   short.class;
				case "int"     ->     int.class;
				case "long"    ->    long.class;
				case "float"   ->   float.class;
				case "double"  ->  double.class;
				case "char"    ->    char.class;
				case "boolean" -> boolean.class;
				case "void"    ->    void.class;
				default        -> Class.forName(name);
			};
		}
		catch (ClassNotFoundException exception) {
			throw new TypeNotPresentException(name, exception);
		}
	}
}