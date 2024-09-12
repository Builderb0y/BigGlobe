package builderb0y.bigglobe.columns.scripted.tree;

import java.lang.invoke.*;

import builderb0y.bigglobe.columns.scripted.ScriptedColumn;
import builderb0y.bigglobe.columns.scripted.traits.WorldTraits;
import builderb0y.scripting.bytecode.MethodInfo;

public class BootstrapTraitsMethods {

	public static final MethodInfo COLUMN_Y_TO_VALUE_VIA_TRAITS = MethodInfo.inCaller("columnYToValueViaTraits");

	//receives: (WorldTraits$GeneratedBase_123, ScriptedColumn$Generated_123, int) -> value
	//returns: (ScriptedColumn$Generated_123, int) -> value
	public static CallSite columnYToValueViaTraits(
		MethodHandles.Lookup caller,
		String name,
		//type: (ScriptedColumn$Generated_123, int) -> value
		MethodType type,
		//getter: (WorldTraits$GeneratedBase_123, ScriptedColumn$Generated_123, int) -> value
		MethodHandle getter
	)
	throws Exception {
		if (
			type.parameterCount() != 2 ||
			!ScriptedColumn.class.isAssignableFrom(type.parameterType(0)) ||
			type.parameterType(1) != int.class ||
			type.returnType() == void.class
		) {
			throw new IllegalArgumentException("Invalid method type: " + type);
		}
		if (
			getter.type().parameterCount() != 3 ||
			!WorldTraits.class.isAssignableFrom(getter.type().parameterType(0)) ||
			!ScriptedColumn.class.isAssignableFrom(getter.type().parameterType(1)) ||
			getter.type().parameterType(2) != int.class ||
			getter.type().returnType() == void.class
		) {
			throw new IllegalArgumentException("Invalid getter: " + getter.type());
		}
		if (type.parameterType(0) != getter.type().parameterType(1)) {
			throw new IllegalArgumentException("Mismatched type " + type + " and getter " + getter.type());
		}

		Class<?> traitsClass = getter.type().parameterType(0);
		Class<?> columnClass = getter.type().parameterType(1);

		//getTraits: (ScriptedColumn$Generated_123) -> WorldTraits
		MethodHandle getTraits = caller.findVirtual(columnClass, "worldTraits", MethodType.methodType(WorldTraits.class));
		//typedTraits: (ScriptedColumn$Generated_123) -> WorldTraits$GeneratedBase_123
		MethodHandle typedTraits = getTraits.asType(getTraits.type().changeReturnType(traitsClass));
		//filtered: (ScriptedColumn$Generated_123, ScriptedColumn$Generated_123, int) -> value
		MethodHandle filtered = MethodHandles.filterArguments(getter, 0, typedTraits);
		//merged: (ScriptedColumn$Generated_123, int) -> value
		MethodHandle merged = MethodHandles.permuteArguments(filtered, type, 0, 0, 1);

		return new ConstantCallSite(merged);
	}

	public static final MethodInfo COLUMN_Y_VALUE_SETTER_VIA_TRAITS = MethodInfo.inCaller("columnYValueSetterViaTraits");

	public static CallSite columnYValueSetterViaTraits(
		MethodHandles.Lookup caller,
		String name,
		//type: (ScriptedColumn$Generated_123, int, value) -> void
		MethodType type,
		//setter: (WorldTraits$GeneratedBase_123, ScriptedColumn$Generated_123, int, value) -> void
		MethodHandle setter
	)
	throws Exception {
		if (
			type.parameterCount() != 3 ||
			!ScriptedColumn.class.isAssignableFrom(type.parameterType(0)) ||
			type.parameterType(1) != int.class ||
			type.returnType() != void.class
		) {
			throw new IllegalArgumentException("Invalid method type: " + type);
		}
		if (
			setter.type().parameterCount() != 4 ||
			!WorldTraits.class.isAssignableFrom(setter.type().parameterType(0)) ||
			!ScriptedColumn.class.isAssignableFrom(setter.type().parameterType(1)) ||
			setter.type().parameterType(2) != int.class ||
			setter.type().returnType() != void.class
		) {
			throw new IllegalArgumentException("Invalid setter: " + setter.type());
		}
		if (type.parameterType(0) != setter.type().parameterType(1)) {
			throw new IllegalArgumentException("Mismatched type " + type + " and getter " + setter.type());
		}

		Class<?> traitsClass = setter.type().parameterType(0).asSubclass(WorldTraits.class);
		Class<?> columnClass = setter.type().parameterType(1).asSubclass(ScriptedColumn.class);

		//getTraits: (ScriptedColumn$Generated_123) -> WorldTraits
		MethodHandle getTraits = caller.findVirtual(columnClass, "worldTraits", MethodType.methodType(WorldTraits.class));
		//typedTraits: (ScriptedColumn$Generated_123) -> WorldTraits$GeneratedBase_123
		MethodHandle typedTraits = getTraits.asType(getTraits.type().changeReturnType(traitsClass));
		//filtered: (ScriptedColumn$Generated_123, ScriptedColumn$Generated_123, int, value) -> void
		MethodHandle filtered = MethodHandles.filterArguments(setter, 0, typedTraits);
		//merged: (ScriptedColumn$Generated_123, int, value) -> void
		MethodHandle merged = MethodHandles.permuteArguments(filtered, type, 0, 0, 1, 2);

		return new ConstantCallSite(merged);
	}
}