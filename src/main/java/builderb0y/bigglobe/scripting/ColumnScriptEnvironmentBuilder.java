package builderb0y.bigglobe.scripting;

import java.lang.invoke.MethodHandles;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.jetbrains.annotations.Nullable;

import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.util.Identifier;

import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.columns.Column;
import builderb0y.bigglobe.columns.ColumnValue;
import builderb0y.bigglobe.columns.WorldColumn;
import builderb0y.scripting.bytecode.FieldInfo;
import builderb0y.scripting.bytecode.MethodInfo;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.bytecode.tree.InsnTree.CastMode;
import builderb0y.scripting.environments.MutableScriptEnvironment;
import builderb0y.scripting.environments.MutableScriptEnvironment.CastResult;
import builderb0y.scripting.environments.MutableScriptEnvironment.FunctionHandler;
import builderb0y.scripting.environments.MutableScriptEnvironment.VariableHandler;
import builderb0y.scripting.environments.ScriptEnvironment;
import builderb0y.scripting.util.TypeInfos;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public class ColumnScriptEnvironmentBuilder {

	public static final MethodInfo
		COLUMN_VALUE_GET_VALUE           = MethodInfo.getMethod(ColumnValue.class, "getValue"),
		COLUMN_VALUE_GET_VALUE_WITHOUT_Y = MethodInfo.getMethod(ColumnValue.class, "getValueWithoutY"),
		INVOKE_GET_VALUE                 = MethodInfo.getMethod(ColumnScriptEnvironmentBuilder.class, "invokeGetValue"),
		INVOKE_GET_VALUE_WITHOUT_Y       = MethodInfo.getMethod(ColumnScriptEnvironmentBuilder.class, "invokeGetValueWithoutY"),
		GET_VALUE_FROM_LOOKUP            = MethodInfo.getMethod(ColumnScriptEnvironmentBuilder.class, "getValueFromLookup"),
		GET_VALUE_FROM_LOOKUP_WITHOUT_Y  = MethodInfo.getMethod(ColumnScriptEnvironmentBuilder.class, "getValueFromLookupWithoutY");
	public static final FieldInfo
		SEED                             =  FieldInfo.getField (WorldColumn.class, "seed");

	public MutableScriptEnvironment mutable = new MutableScriptEnvironment();
	public @Nullable InsnTree loadColumn;
	public @Nullable InsnTree loadY;
	public Set<ColumnValue<?>> usedValues;

	public ColumnScriptEnvironmentBuilder(@Nullable InsnTree loadColumn, @Nullable InsnTree loadY) {
		this.loadColumn = loadColumn;
		this.loadY = loadY;
	}

	public MutableScriptEnvironment build() {
		return this.mutable;
	}

	public static String[] getNames(RegistryKey<ColumnValue<?>> key) {
		Identifier identifier = key.getValue();
		return (
			identifier.getNamespace().equals(BigGlobeMod.MODID)
			? new String[] { identifier.toString(), identifier.getPath() }
			: new String[] { identifier.toString() }
		);
	}

	public static void addTree(ColumnScriptEnvironmentBuilder environment, String name, ColumnValue<?> value, InsnTree tree) {
		environment
		.mutable
		.addVariable(name, new VariableHandler.Named(name, (parser, name1) -> {
			if (environment.usedValues != null) environment.usedValues.add(value);
			return tree;
		}))
		.addFunction(name, new FunctionHandler.Named(name + "()", (parser, name1, arguments) -> {
			if (arguments.length != 0) return null;
			if (environment.usedValues != null) environment.usedValues.add(value);
			return new CastResult(tree, false);
		}));
	}

	public static ColumnScriptEnvironmentBuilder createFixedXYZ(
		Registry<ColumnValue<?>> registry,
		InsnTree loadColumn,
		InsnTree loadY
	) {
		ColumnScriptEnvironmentBuilder environment = new ColumnScriptEnvironmentBuilder(loadColumn, loadY);
		for (Map.Entry<RegistryKey<ColumnValue<?>>, ColumnValue<?>> entry : registry.getEntrySet()) {
			ColumnValue<?> value = entry.getValue();
			if (value == ColumnValue.Y) continue;
			for (String name : getNames(entry.getKey())) {
				InsnTree tree = (
					value.dependsOnY()
					? invokeInstance(
						COLUMN_VALUE_CONSTANT_FACTORY.create(constant(name)),
						COLUMN_VALUE_GET_VALUE,
						loadColumn,
						loadY
					)
					: invokeInstance(
						COLUMN_VALUE_CONSTANT_FACTORY.create(constant(name)),
						COLUMN_VALUE_GET_VALUE_WITHOUT_Y,
						loadColumn
					)
				);
				addTree(environment, name, value, tree);
			}
		}
		return environment;
	}

	public static ColumnScriptEnvironmentBuilder createFixedXZVariableY(
		Registry<ColumnValue<?>> registry,
		InsnTree loadColumn,
		@Nullable InsnTree defaultY
	) {
		ColumnScriptEnvironmentBuilder environment = new ColumnScriptEnvironmentBuilder(loadColumn, defaultY);
		for (Map.Entry<RegistryKey<ColumnValue<?>>, ColumnValue<?>> entry : registry.getEntrySet()) {
			ColumnValue<?> value = entry.getValue();
			if (value == ColumnValue.Y) continue;
			for (String name : getNames(entry.getKey())) {
				InsnTree tree;
				if (value.dependsOnY()) {
					environment.mutable.addFunction(name, new FunctionHandler.Named(name + "(double y)", (parser, name1, arguments) -> {
						InsnTree castArgument = ScriptEnvironment.castArgument(parser, name1, TypeInfos.DOUBLE, CastMode.IMPLICIT_NULL, arguments);
						if (castArgument == null) return null;
						if (environment.usedValues != null) environment.usedValues.add(value);
						return new CastResult(
							invokeInstance(
								COLUMN_VALUE_CONSTANT_FACTORY.create(constant(name1)),
								COLUMN_VALUE_GET_VALUE,
								loadColumn,
								castArgument
							),
							castArgument != arguments[0]
						);
					}));
					if (defaultY != null) {
						tree = invokeInstance(
							COLUMN_VALUE_CONSTANT_FACTORY.create(constant(name)),
							COLUMN_VALUE_GET_VALUE,
							loadColumn,
							defaultY
						);
					}
					else {
						tree = null;
					}
				}
				else {
					tree = invokeInstance(
						COLUMN_VALUE_CONSTANT_FACTORY.create(constant(name)),
						COLUMN_VALUE_GET_VALUE_WITHOUT_Y,
						loadColumn
					);
				}
				if (tree != null) {
					addTree(environment, name, value, tree);
				}
			}
		}
		return environment;
	}

	public static ColumnScriptEnvironmentBuilder createVariableXYZ(
		Registry<ColumnValue<?>> registry,
		InsnTree loadColumn
	) {
		ColumnScriptEnvironmentBuilder environment = new ColumnScriptEnvironmentBuilder(loadColumn, null);
		for (Map.Entry<RegistryKey<ColumnValue<?>>, ColumnValue<?>> entry : registry.getEntrySet()) {
			ColumnValue<?> value = entry.getValue();
			if (value == ColumnValue.Y) continue;
			for (String name : getNames(entry.getKey())) {
				if (value.dependsOnY()) {
					environment.mutable.addFunction(name, new FunctionHandler.Named(name + "(int x, double y, int z)", (parser, name1, arguments) -> {
						InsnTree[] castArguments = ScriptEnvironment.castArguments(parser, name1, types("IDI"), CastMode.IMPLICIT_NULL, arguments);
						if (castArguments == null) return null;
						if (environment.usedValues != null) environment.usedValues.add(value);
						return new CastResult(
							invokeStatic(
								INVOKE_GET_VALUE,
								loadColumn,
								COLUMN_VALUE_CONSTANT_FACTORY.create(constant(name1)),
								castArguments[0],
								castArguments[1],
								castArguments[2]
							),
							castArguments != arguments
						);
					}));
				}
				else {
					environment.mutable.addFunction(name, new FunctionHandler.Named(name + "(int x, int z)", (parser, name1, arguments) -> {
						InsnTree[] castArguments = ScriptEnvironment.castArguments(parser, name1, types("II"), CastMode.IMPLICIT_NULL, arguments);
						if (castArguments == null) return null;
						if (environment.usedValues != null) environment.usedValues.add(value);
						return new CastResult(
							invokeStatic(
								INVOKE_GET_VALUE_WITHOUT_Y,
								loadColumn,
								COLUMN_VALUE_CONSTANT_FACTORY.create(constant(name1)),
								castArguments[0],
								castArguments[1]
							),
							castArguments != arguments
						);
					}));
				}
			}
		}
		return environment;
	}

	public static record DefaultLookupPosition(InsnTree x, InsnTree y, InsnTree z) {}

	public static ColumnScriptEnvironmentBuilder createFromLookup(
		Registry<ColumnValue<?>> registry,
		InsnTree loadLookup,
		@Nullable DefaultLookupPosition defaultPosition
	) {
		ColumnScriptEnvironmentBuilder environment = new ColumnScriptEnvironmentBuilder(null, null);
		for (Map.Entry<RegistryKey<ColumnValue<?>>, ColumnValue<?>> entry : registry.getEntrySet()) {
			ColumnValue<?> value = entry.getValue();
			if (value == ColumnValue.Y) continue;
			for (String name : getNames(entry.getKey())) {
				if (value.dependsOnY()) {
					environment.mutable.addFunction(name, new FunctionHandler.Named(name + "(int x, double y, int z)", (parser, name1, arguments) -> {
						InsnTree[] castArguments = ScriptEnvironment.castArguments(parser, name1, types("IDI"), CastMode.IMPLICIT_NULL, arguments);
						if (castArguments == null) return null;
						if (environment.usedValues != null) environment.usedValues.add(value);
						return new CastResult(
							invokeStatic(
								GET_VALUE_FROM_LOOKUP,
								loadLookup,
								COLUMN_VALUE_CONSTANT_FACTORY.create(constant(name1)),
								castArguments[0],
								castArguments[1],
								castArguments[2]
							),
							castArguments != arguments
						);
					}));
					if (defaultPosition != null) {
						environment.mutable.addVariable(name, new VariableHandler.Named(name, (parser, name1) -> {
							return invokeStatic(
								GET_VALUE_FROM_LOOKUP,
								loadLookup,
								COLUMN_VALUE_CONSTANT_FACTORY.create(constant(name1)),
								defaultPosition.x,
								defaultPosition.y,
								defaultPosition.z
							);
						}));
					}
				}
				else {
					environment.mutable.addFunction(name, new FunctionHandler.Named(name + "(int x, int z)", (parser, name1, arguments) -> {
						InsnTree[] castArguments = ScriptEnvironment.castArguments(parser, name1, types("II"), CastMode.IMPLICIT_NULL, arguments);
						if (castArguments == null) return null;
						if (environment.usedValues != null) environment.usedValues.add(value);
						return new CastResult(
							invokeStatic(
								GET_VALUE_FROM_LOOKUP_WITHOUT_Y,
								loadLookup,
								COLUMN_VALUE_CONSTANT_FACTORY.create(constant(name1)),
								castArguments[0],
								castArguments[1]
							),
							castArguments != arguments
						);
					}));
					if (defaultPosition != null) {
						environment.mutable.addVariable(name, new VariableHandler.Named(name, (parser, name1) -> {
							return invokeStatic(
								GET_VALUE_FROM_LOOKUP_WITHOUT_Y,
								loadLookup,
								COLUMN_VALUE_CONSTANT_FACTORY.create(constant(name1)),
								defaultPosition.x,
								defaultPosition.z
							);
						}));
					}
				}
			}
		}
		return environment;
	}

	public ColumnScriptEnvironmentBuilder trackUsedValues() {
		this.usedValues = new HashSet<>(8);
		return this;
	}

	public ColumnScriptEnvironmentBuilder addY(String nameY) {
		if (this.loadY == null) throw new IllegalStateException("Y not specified.");
		this.mutable.addVariable(nameY, this.loadY);
		return this;
	}

	public ColumnScriptEnvironmentBuilder addXZ(String nameX, String nameZ) {
		if (this.loadColumn == null) throw new IllegalStateException("Column not specified.");
		this
		.mutable
		.addVariableRenamedGetField(this.loadColumn, nameX, Column.class, "x")
		.addVariableRenamedGetField(this.loadColumn, nameZ, Column.class, "z")
		;
		return this;
	}

	public ColumnScriptEnvironmentBuilder addSeed(String name) {
		if (this.loadColumn == null) throw new IllegalStateException("Column not specified.");
		this.mutable.addVariableRenamedGetField(this.loadColumn, name, SEED);
		return this;
	}

	public static final ConstantFactory COLUMN_VALUE_CONSTANT_FACTORY = new ConstantFactory(ColumnScriptEnvironmentBuilder.class, "getColumnValueRuntime", String.class, ColumnValue.class);

	public static ColumnValue<?> getColumnValueRuntime(MethodHandles.Lookup caller, String name, Class<?> type, String id) {
		return getColumnValueRuntime(id);
	}

	public static ColumnValue<?> getColumnValueRuntime(String id) {
		if (id == null) return null;
		return ColumnValue.get(id);
	}

	/**
	enforces that x, y, and z are evaluated in that exact order.
	cause column.setPos(x, z),, value.getValue(column, y) would be a different order,
	which could matter if evaluating x, y, or z contain side effects.
	*/
	public static double invokeGetValue(WorldColumn column, ColumnValue<?> value, int x, double y, int z) {
		column.setPos(x, z);
		return value.getValue(column, y);
	}

	public static double invokeGetValueWithoutY(WorldColumn column, ColumnValue<?> value, int x, int z) {
		column.setPos(x, z);
		return value.getValueWithoutY(column);
	}

	public static double getValueFromLookup(ColumnLookup lookup, ColumnValue<?> value, int x, double y, int z) {
		return value.getValue(lookup.lookupColumn(x, z), y);
	}

	public static double getValueFromLookupWithoutY(ColumnLookup lookup, ColumnValue<?> value, int x, int z) {
		return value.getValueWithoutY(lookup.lookupColumn(x, z));
	}

	@FunctionalInterface
	public static interface ColumnLookup {

		public abstract WorldColumn lookupColumn(int x, int z);
	}
}