package builderb0y.bigglobe.columns.scripted.tree;

import java.lang.invoke.*;

import builderb0y.bigglobe.columns.scripted.ScriptedColumn;
import builderb0y.bigglobe.columns.scripted.traits.WorldTraits;
import builderb0y.scripting.bytecode.MethodInfo;

public class BootstrapTraitsMethods {

	public static final MethodInfo COLUMN_VALUE_SETTER_VIA_TRAITS = MethodInfo.inCaller("columnValueSetterViaTraits");

	public static CallSite columnValueSetterViaTraits(
		MethodHandles.Lookup caller,
		String name,
		//(ScriptedColumn$Generated_123, value) -> void
		MethodType type,
		//(WorldTraits$GeneratedBase_123, ScriptedColumn$Generated_123, value) -> void
		MethodHandle setter
	)
	throws Exception {
		if (!checkTypes(type, void.class, ScriptedColumn.class, null)) {
			throw new IllegalArgumentException("Invalid method type: " + type);
		}
		if (!checkTypes(setter.type(), void.class, WorldTraits.class, ScriptedColumn.class, null)) {
			throw new IllegalArgumentException("Invalid setter: " + setter.type());
		}
		if (type.parameterType(0) != setter.type().parameterType(1)) {
			throw new IllegalArgumentException("Mismatched type " + type + " and setter " + setter.type());
		}

		Class<?> traitsClass = setter.type().parameterType(0);
		Class<?> columnClass = setter.type().parameterType(1);

		//(ScriptedColumn$Generated_123) -> WorldTraits
		MethodHandle getTraits = caller.findVirtual(columnClass, "worldTraits", MethodType.methodType(WorldTraits.class));
		//(ScriptedColumn$Generated_123) -> WorldTraits$GeneratedBase_123
		MethodHandle typedTraits = getTraits.asType(getTraits.type().changeReturnType(traitsClass));
		//(ScriptedColumn$Generated_123, ScriptedColumn$Generated_123, value) -> void
		MethodHandle filtered = MethodHandles.filterArguments(setter, 0, typedTraits);
		//(ScriptedColumn$Generated_123, value) -> void
		MethodHandle merged = MethodHandles.permuteArguments(filtered, type, 0, 0, 1);

		return new ConstantCallSite(merged);
	}

	public static final MethodInfo COLUMN_Y_TO_VALUE_VIA_TRAITS = MethodInfo.inCaller("columnYToValueViaTraits");

	public static CallSite columnYToValueViaTraits(
		MethodHandles.Lookup caller,
		String name,
		//(ScriptedColumn$Generated_123, int) -> value
		MethodType type,
		//(WorldTraits$GeneratedBase_123, ScriptedColumn$Generated_123, int) -> value
		MethodHandle getter
	)
	throws Exception {
		if (!checkTypes(type, null, ScriptedColumn.class, int.class)) {
			throw new IllegalArgumentException("Invalid method type: " + type);
		}
		if (!checkTypes(getter.type(), null, WorldTraits.class, ScriptedColumn.class, int.class)) {
			throw new IllegalArgumentException("Invalid getter: " + getter.type());
		}
		if (type.parameterType(0) != getter.type().parameterType(1)) {
			throw new IllegalArgumentException("Mismatched type " + type + " and getter " + getter.type());
		}

		Class<?> traitsClass = getter.type().parameterType(0);
		Class<?> columnClass = getter.type().parameterType(1);

		//(ScriptedColumn$Generated_123) -> WorldTraits
		MethodHandle getTraits = caller.findVirtual(columnClass, "worldTraits", MethodType.methodType(WorldTraits.class));
		//(ScriptedColumn$Generated_123) -> WorldTraits$GeneratedBase_123
		MethodHandle typedTraits = getTraits.asType(getTraits.type().changeReturnType(traitsClass));
		//(ScriptedColumn$Generated_123, ScriptedColumn$Generated_123, int) -> value
		MethodHandle filtered = MethodHandles.filterArguments(getter, 0, typedTraits);
		//(ScriptedColumn$Generated_123, int) -> value
		MethodHandle merged = MethodHandles.permuteArguments(filtered, type, 0, 0, 1);

		return new ConstantCallSite(merged);
	}

	public static final MethodInfo COLUMN_Y_VALUE_SETTER_VIA_TRAITS = MethodInfo.inCaller("columnYValueSetterViaTraits");

	public static CallSite columnYValueSetterViaTraits(
		MethodHandles.Lookup caller,
		String name,
		//(ScriptedColumn$Generated_123, int, value) -> void
		MethodType type,
		//(WorldTraits$GeneratedBase_123, ScriptedColumn$Generated_123, int, value) -> void
		MethodHandle setter
	)
	throws Exception {
		if (!checkTypes(type, void.class, ScriptedColumn.class, int.class, null)) {
			throw new IllegalArgumentException("Invalid method type: " + type);
		}
		if (!checkTypes(setter.type(), void.class, WorldTraits.class, ScriptedColumn.class, int.class, null)) {
			throw new IllegalArgumentException("Invalid setter: " + setter.type());
		}
		if (type.parameterType(0) != setter.type().parameterType(1)) {
			throw new IllegalArgumentException("Mismatched type " + type + " and getter " + setter.type());
		}

		Class<?> traitsClass = setter.type().parameterType(0).asSubclass(WorldTraits.class);
		Class<?> columnClass = setter.type().parameterType(1).asSubclass(ScriptedColumn.class);

		//(ScriptedColumn$Generated_123) -> WorldTraits
		MethodHandle getTraits = caller.findVirtual(columnClass, "worldTraits", MethodType.methodType(WorldTraits.class));
		//(ScriptedColumn$Generated_123) -> WorldTraits$GeneratedBase_123
		MethodHandle typedTraits = getTraits.asType(getTraits.type().changeReturnType(traitsClass));
		//(ScriptedColumn$Generated_123, ScriptedColumn$Generated_123, int, value) -> void
		MethodHandle filtered = MethodHandles.filterArguments(setter, 0, typedTraits);
		//(ScriptedColumn$Generated_123, int, value) -> void
		MethodHandle merged = MethodHandles.permuteArguments(filtered, type, 0, 0, 1, 2);

		return new ConstantCallSite(merged);
	}

	public static boolean checkTypes(MethodType type, Class<?> returnType, Class<?>... paramTypes) {
		if (type.parameterCount() != paramTypes.length) {
			return false;
		}
		for (int index = 0, length = paramTypes.length; index < length; index++) {
			if (paramTypes[index] != null && !paramTypes[index].isAssignableFrom(type.parameterType(index))) {
				return false;
			}
		}
		if (returnType == null) {
			if (type.returnType() == void.class) {
				return false;
			}
		}
		else {
			if (!returnType.isAssignableFrom(type.returnType())) {
				return false;
			}
		}
		return true;
	}
}