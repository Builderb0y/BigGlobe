package builderb0y.bigglobe.scripting;

import java.lang.invoke.MethodHandles;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import org.jetbrains.annotations.Nullable;

import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.util.Identifier;

import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.columns.*;
import builderb0y.bigglobe.columns.OverworldColumn.CaveCell;
import builderb0y.bigglobe.columns.OverworldColumn.CavernCell;
import builderb0y.bigglobe.columns.OverworldColumn.SkylandCell;
import builderb0y.bigglobe.scripting.wrappers.NetherBiomeSettingsEntry;
import builderb0y.bigglobe.scripting.wrappers.OverworldCaveSettingsEntry;
import builderb0y.bigglobe.scripting.wrappers.OverworldCavernSettingsEntry;
import builderb0y.bigglobe.scripting.wrappers.OverworldSkylandSettingsEntry;
import builderb0y.bigglobe.settings.NetherSettings.NetherCaveSettings;
import builderb0y.bigglobe.settings.NetherSettings.NetherCavernSettings;
import builderb0y.scripting.bytecode.ConstantFactory;
import builderb0y.scripting.bytecode.FieldInfo;
import builderb0y.scripting.bytecode.MethodInfo;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.environments.Handlers;
import builderb0y.scripting.environments.MutableScriptEnvironment;
import builderb0y.scripting.environments.MutableScriptEnvironment.CastResult;
import builderb0y.scripting.environments.MutableScriptEnvironment.FunctionHandler;
import builderb0y.scripting.environments.MutableScriptEnvironment.VariableHandler;
import builderb0y.scripting.parsing.ExpressionParser;
import builderb0y.scripting.util.TypeInfos;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public class ColumnScriptEnvironmentBuilder {

	public static final MethodInfo
		COLUMN_VALUE_GET_VALUE            = MethodInfo.getMethod(ColumnValue.class, "getValue"),
		COLUMN_VALUE_GET_VALUE_WITHOUT_Y  = MethodInfo.getMethod(ColumnValue.class, "getValueWithoutY"),
		GET_VALUE_FROM_LOOKUP             = MethodInfo.getMethod(ColumnScriptEnvironmentBuilder.class, "getValueFromLookup"),
		GET_VALUE_FROM_LOOKUP_WITHOUT_Y   = MethodInfo.getMethod(ColumnScriptEnvironmentBuilder.class, "getValueFromLookupWithoutY"),
		LOOKUP_COLUMN                     = MethodInfo.getMethod(ColumnLookup.class, "lookupColumn");
	public static final FieldInfo
		SEED                              =  FieldInfo.getField (WorldColumn.class, "seed");
	public static final SpecialGetter[] SPECIAL_GETTERS = {
		new SpecialGetter(   "overworld/cave_settings",    "getOverworldCaveSettings"),
		new SpecialGetter( "overworld/cavern_settings",  "getOverworldCavernSettings"),
		new SpecialGetter("overworld/skyland_settings", "getOverworldSkylandSettings"),
		new SpecialGetter(     "nether/biome_settings",      "getNetherBiomeSettings"),
	};

	public static record SpecialGetter(
		String[] exposedNames,
		String fixedPositionInternalName,
		String variablePositionInternalName,
		MethodInfo fixedPositionGetter,
		MethodInfo variablePositionGetter
	) {

		public SpecialGetter(String exposedName, String internalName) {
			this(
				new String[] { exposedName, BigGlobeMod.MODID + ':' + exposedName },
				internalName,
				internalName + "At",
				MethodInfo.getMethod(ColumnScriptEnvironmentBuilder.class, internalName),
				MethodInfo.getMethod(ColumnScriptEnvironmentBuilder.class, internalName + "At")
			);
		}
	}

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

	public static Consumer<CastResult> addToUsed(ColumnScriptEnvironmentBuilder environment, ColumnValue<?> value) {
		return (CastResult result) -> {
			if (environment.usedValues != null) {
				environment.usedValues.add(value);
			}
		};
	}

	public static VariableHandler makeVariableAdder(ColumnScriptEnvironmentBuilder environment, ColumnValue<?> value, String name, InsnTree tree) {
		return new VariableHandler.Named(name, (ExpressionParser parser, String name1) -> {
			if (environment.usedValues != null) {
				environment.usedValues.add(value);
			}
			return tree;
		});
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
		return environment.addVoronoiTypesAndMethods().addFixedPositionVoronoiGetters();
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
					environment.mutable.addFunction(
						name,
						Handlers
						.builder(ColumnValue.class, "getValue")
						.addImplicitArgument(COLUMN_VALUE_CONSTANT_FACTORY.create(constant(name)))
						.addImplicitArgument(loadColumn)
						.addRequiredArgument(TypeInfos.DOUBLE)
						.also(addToUsed(environment, value))
						.buildFunction()
					);
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
		return environment.addVoronoiTypesAndMethods().addFixedPositionVoronoiGetters();
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
					environment.mutable.addFunction(
						name,
						Handlers
						.inCaller("invokeGetValue")
						.addArguments(loadColumn, COLUMN_VALUE_CONSTANT_FACTORY.create(constant(name)), "IDI")
						.also(addToUsed(environment, value))
						.buildFunction()
					);
				}
				else {
					environment.mutable.addFunction(
						name,
						Handlers
						.inCaller("invokeGetValueWithoutY")
						.addArguments(loadColumn, COLUMN_VALUE_CONSTANT_FACTORY.create(constant(name)), "II")
						.also(addToUsed(environment, value))
						.buildFunction()
					);
				}
			}
		}
		return environment.addVoronoiTypesAndMethods().addVaryingPositionVoronoiGetters();
	}

	public ColumnScriptEnvironmentBuilder addVoronoiTypesAndMethods() {
		this
		.mutable
		.addType("OverworldCaveSettings",       OverworldCaveSettingsEntry.TYPE)
		.addType("OverworldCavernSettings",   OverworldCavernSettingsEntry.TYPE)
		.addType("OverworldSkylandSettings", OverworldSkylandSettingsEntry.TYPE)
		.addType("NetherBiomeSettings",           NetherBiomeSettingsEntry.TYPE)
		.addType("NetherCaveSettings",             NetherCaveSettings     .class)
		.addType("NetherCavernSettings",         NetherCavernSettings     .class)
		.addCastConstant(   OverworldCaveSettingsEntry.CONSTANT_FACTORY, true)
		.addCastConstant( OverworldCavernSettingsEntry.CONSTANT_FACTORY, true)
		.addCastConstant(OverworldSkylandSettingsEntry.CONSTANT_FACTORY, true)
		.addCastConstant(     NetherBiomeSettingsEntry.CONSTANT_FACTORY, true)

		.addFieldInvoke(   OverworldCaveSettingsEntry.class, "weight")
		.addFieldInvoke(   OverworldCaveSettingsEntry.class, "depth")
		.addFieldInvoke( OverworldCavernSettingsEntry.class, "weight")
		.addFieldInvoke( OverworldCavernSettingsEntry.class, "padding")
		.addFieldInvoke( OverworldCavernSettingsEntry.class, "fluid")
		.addFieldInvoke( OverworldCavernSettingsEntry.class, "has_ancient_cities")
		.addFieldInvoke(OverworldSkylandSettingsEntry.class, "weight")
		.addFieldInvoke(     NetherBiomeSettingsEntry.class, "weight")
		.addFieldInvoke(     NetherBiomeSettingsEntry.class, "biome")
		.addFieldInvoke(     NetherBiomeSettingsEntry.class, "caves")
		.addFieldInvoke(     NetherBiomeSettingsEntry.class, "caverns")
		.addFieldInvoke(      NetherCaveSettings     .class, "lower_padding")
		.addFieldInvoke(    NetherCavernSettings     .class, "min_y")
		.addFieldInvoke(    NetherCavernSettings     .class, "max_y")
		.addFieldInvoke(    NetherCavernSettings     .class, "lower_padding")
		.addFieldInvoke(    NetherCavernSettings     .class, "upper_padding")
		.addFieldInvoke(    NetherCavernSettings     .class, "edge_padding")
		;

		return this;
	}

	public ColumnScriptEnvironmentBuilder addFixedPositionVoronoiGetters() {
		if (this.loadColumn == null) throw new IllegalStateException("Column not specified.");

		for (SpecialGetter getter : SPECIAL_GETTERS) {
			for (String name : getter.exposedNames) {
				this.mutable.addVariableRenamedInvoke(
					this.loadColumn,
					name,
					getter.fixedPositionGetter
				);
			}
		}

		return this;
	}

	public ColumnScriptEnvironmentBuilder addVaryingPositionVoronoiGetters() {
		if (this.loadColumn == null) throw new IllegalStateException("Column not specified.");

		for (SpecialGetter getter : SPECIAL_GETTERS) {
			for (String name : getter.exposedNames) {
				this.mutable.addFunction(
					name,
					Handlers.inCaller(getter.variablePositionInternalName)
					.addArguments(this.loadColumn, int.class, int.class)
					.buildFunction()
				);
			}
		}

		return this;
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
					environment.mutable.addFunction(
						name,
						Handlers
						.inCaller("getValueFromLookup")
						.addArguments(loadLookup, COLUMN_VALUE_CONSTANT_FACTORY.create(constant(name)), "IDI")
						.also(addToUsed(environment, value))
						.buildFunction()
					);
					if (defaultPosition != null) {
						environment.mutable.addVariable(
							name,
							makeVariableAdder(
								environment,
								value,
								name,
								invokeStatic(
									GET_VALUE_FROM_LOOKUP,
									loadLookup,
									COLUMN_VALUE_CONSTANT_FACTORY.create(constant(name)),
									defaultPosition.x,
									defaultPosition.y,
									defaultPosition.z
								)
							)
						);
					}
				}
				else {
					environment.mutable.addFunction(
						name,
						Handlers.inCaller("getValueFromLookupWithoutY")
						.addArguments(
							loadLookup,
							COLUMN_VALUE_CONSTANT_FACTORY.create(constant(name)),
							"II"
						)
						.buildFunction()
					);
					if (defaultPosition != null) {
						environment.mutable.addVariable(
							name,
							makeVariableAdder(
								environment,
								value,
								name,
								invokeStatic(
									GET_VALUE_FROM_LOOKUP_WITHOUT_Y,
									loadLookup,
									COLUMN_VALUE_CONSTANT_FACTORY.create(constant(name)),
									defaultPosition.x,
									defaultPosition.z
								)
							)
						);
					}
				}
			}
		}
		for (SpecialGetter getter : SPECIAL_GETTERS) {
			for (String name : getter.exposedNames) {
				environment.mutable.addFunction(
					name,
					Handlers
					.inCaller(getter.fixedPositionInternalName)
					.addNestedArgument(
						Handlers
						.builder(ColumnLookup.class, "lookupColumn")
						.addImplicitArgument(loadLookup)
						.addArguments("II")
					)
					.buildFunction()
				);
				if (defaultPosition != null) {
					environment.mutable.addVariable(
						name,
						invokeStatic(
							getter.fixedPositionGetter,
							invokeInstance(
								loadLookup,
								LOOKUP_COLUMN,
								defaultPosition.x,
								defaultPosition.z
							)
						)
					);
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

	public static @Nullable OverworldCaveSettingsEntry getOverworldCaveSettings(WorldColumn column) {
		if (column instanceof OverworldColumn overworld) {
			CaveCell cell = overworld.getCaveCell();
			if (cell != null) {
				return new OverworldCaveSettingsEntry(cell.entry);
			}
		}
		return null;
	}

	public static @Nullable OverworldCavernSettingsEntry getOverworldCavernSettings(WorldColumn column) {
		if (column instanceof OverworldColumn overworld) {
			CavernCell cell = overworld.getCavernCell();
			if (cell != null) {
				return new OverworldCavernSettingsEntry(cell.entry);
			}
		}
		return null;
	}

	public static @Nullable OverworldSkylandSettingsEntry getOverworldSkylandSettings(WorldColumn column) {
		if (column instanceof OverworldColumn overworld) {
			SkylandCell cell = overworld.getSkylandCell();
			if (cell != null) {
				return new OverworldSkylandSettingsEntry(cell.entry);
			}
		}
		return null;
	}

	public static @Nullable NetherBiomeSettingsEntry getNetherBiomeSettings(WorldColumn column) {
		if (column instanceof NetherColumn nether) {
			return new NetherBiomeSettingsEntry(nether.getLocalCell().entry);
		}
		return null;
	}

	public static @Nullable OverworldCaveSettingsEntry getOverworldCaveSettingsAt(WorldColumn column, int x, int z) {
		if (column instanceof OverworldColumn overworld) {
			overworld.setPos(x, z);
			CaveCell cell = overworld.getCaveCell();
			if (cell != null) {
				return new OverworldCaveSettingsEntry(cell.entry);
			}
		}
		return null;
	}

	public static @Nullable OverworldCavernSettingsEntry getOverworldCavernSettingsAt(WorldColumn column, int x, int z) {
		if (column instanceof OverworldColumn overworld) {
			overworld.setPos(x, z);
			CavernCell cell = overworld.getCavernCell();
			if (cell != null) {
				return new OverworldCavernSettingsEntry(cell.entry);
			}
		}
		return null;
	}

	public static @Nullable OverworldSkylandSettingsEntry getOverworldSkylandSettingsAt(WorldColumn column, int x, int z) {
		if (column instanceof OverworldColumn overworld) {
			overworld.setPos(x, z);
			SkylandCell cell = overworld.getSkylandCell();
			if (cell != null) {
				return new OverworldSkylandSettingsEntry(cell.entry);
			}
		}
		return null;
	}

	public static @Nullable NetherBiomeSettingsEntry getNetherBiomeSettingsAt(WorldColumn column, int x, int z) {
		if (column instanceof NetherColumn nether) {
			nether.setPos(x, z);
			return new NetherBiomeSettingsEntry(nether.getLocalCell().entry);
		}
		return null;
	}

	@FunctionalInterface
	public static interface ColumnLookup {

		public abstract WorldColumn lookupColumn(int x, int z);
	}
}