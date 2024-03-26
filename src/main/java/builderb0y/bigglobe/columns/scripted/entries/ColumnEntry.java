package builderb0y.bigglobe.columns.scripted.entries;

import java.util.HashMap;
import java.util.function.Supplier;

import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;

import builderb0y.autocodec.annotations.MemberUsage;
import builderb0y.autocodec.annotations.UseCoder;
import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.codecs.CoderRegistry;
import builderb0y.bigglobe.codecs.CoderRegistryTyped;
import builderb0y.bigglobe.columns.scripted.AccessSchema;
import builderb0y.bigglobe.columns.scripted.AccessSchema.AccessContext;
import builderb0y.bigglobe.columns.scripted.ColumnLookupGet3DValueInsnTree;
import builderb0y.bigglobe.columns.scripted.ColumnLookupMutableGet3DValueInsnTree;
import builderb0y.bigglobe.columns.scripted.dependencies.ColumnValueDependencyHolder;
import builderb0y.bigglobe.columns.scripted.compile.ColumnCompileContext;
import builderb0y.bigglobe.columns.scripted.types.ColumnValueType.TypeContext;
import builderb0y.bigglobe.columns.scripted.compile.DataCompileContext;
import builderb0y.bigglobe.util.UnregisteredObjectException;
import builderb0y.scripting.bytecode.FieldCompileContext;
import builderb0y.scripting.bytecode.MethodCompileContext;
import builderb0y.scripting.bytecode.MethodInfo;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.bytecode.tree.InsnTree.CastMode;
import builderb0y.scripting.bytecode.tree.instructions.casting.DirectCastInsnTree;
import builderb0y.scripting.bytecode.tree.instructions.invokers.ArgumentedGetterSetterInsnTree;
import builderb0y.scripting.bytecode.tree.instructions.invokers.GetterSetterInsnTree;
import builderb0y.scripting.environments.MutableScriptEnvironment;
import builderb0y.scripting.environments.MutableScriptEnvironment.*;
import builderb0y.scripting.environments.ScriptEnvironment;
import builderb0y.scripting.environments.ScriptEnvironment.GetFieldMode;
import builderb0y.scripting.environments.ScriptEnvironment.GetMethodMode;
import builderb0y.scripting.parsing.ExpressionParser;
import builderb0y.scripting.parsing.ScriptParsingException;
import builderb0y.scripting.util.TypeInfos;

import static builderb0y.scripting.bytecode.InsnTrees.*;

@UseCoder(name = "REGISTRY", in = ColumnEntry.class, usage = MemberUsage.FIELD_CONTAINS_HANDLER)
public interface ColumnEntry extends CoderRegistryTyped<ColumnEntry>, ColumnValueDependencyHolder {

	public static final CoderRegistry<ColumnEntry> REGISTRY = new CoderRegistry<>(BigGlobeMod.modID("column_value"));
	public static final Object INITIALIZER = new Object() {{
		REGISTRY.registerAuto(BigGlobeMod.modID("constant"),          ConstantColumnEntry.class);
		REGISTRY.registerAuto(BigGlobeMod.modID("noise"),                NoiseColumnEntry.class);
		REGISTRY.registerAuto(BigGlobeMod.modID("script"),              ScriptColumnEntry.class);
		REGISTRY.registerAuto(BigGlobeMod.modID("decision_tree"), DecisionTreeColumnEntry.class);
		REGISTRY.registerAuto(BigGlobeMod.modID("voronoi"),            VoronoiColumnEntry.class);
	}};

	public abstract AccessSchema getAccessSchema();

	public abstract boolean hasField();

	public default boolean isSettable() {
		return this.hasField();
	}

	public default void populateField(ColumnEntryMemory memory, DataCompileContext context, FieldCompileContext getterMethod) {}

	public abstract void populateGetter(ColumnEntryMemory memory, DataCompileContext context, MethodCompileContext getterMethod);

	public abstract void populateSetter(ColumnEntryMemory memory, DataCompileContext context, MethodCompileContext setterMethod);

	public default void emitFieldGetterAndSetter(ColumnEntryMemory memory, DataCompileContext context) {
		int uniqueIndex = context.mainClass.memberUniquifier++;
		int flagIndex = context.flagsIndex++;
		memory.putTyped(ColumnEntryMemory.FLAGS_INDEX, flagIndex);
		Identifier accessID = memory.getTyped(ColumnEntryMemory.ACCESSOR_ID);
		String internalName = DataCompileContext.internalName(accessID, uniqueIndex);
		memory.putTyped(ColumnEntryMemory.INTERNAL_NAME, internalName);

		AccessContext accessContext = memory.getTyped(ColumnEntryMemory.ACCESS_CONTEXT);
		if (this.hasField()) {
			FieldCompileContext valueField = context.mainClass.newField(ACC_PUBLIC, internalName, accessContext.fieldType());
			memory.putTyped(ColumnEntryMemory.FIELD, valueField);
			MethodCompileContext getterMethod = context.mainClass.newMethod(ACC_PUBLIC, "get_" + internalName, accessContext.exposedType(), this.getAccessSchema().getterParameters());
			memory.putTyped(ColumnEntryMemory.GETTER, getterMethod);

			if (this.isSettable()) {
				MethodCompileContext setterMethod = context.mainClass.newMethod(ACC_PUBLIC, "set_" + internalName, TypeInfos.VOID, this.getAccessSchema().setterParameters(context));
				memory.putTyped(ColumnEntryMemory.SETTER, setterMethod);

				this.populateField(memory, context, valueField);
				this.populateGetter(memory, context, getterMethod);
				this.populateSetter(memory, context, setterMethod);
			}
			else {
				this.populateField(memory, context, valueField);
				this.populateGetter(memory, context, getterMethod);
			}
		}
		else {
			MethodCompileContext getterMethod = context.mainClass.newMethod(ACC_PUBLIC, "get_" + internalName, accessContext.exposedType(), this.getAccessSchema().getterParameters());
			memory.putTyped(ColumnEntryMemory.GETTER, getterMethod);

			this.populateGetter(memory, context, getterMethod);
		}
	}

	public default void setupInternalEnvironment(
		MutableScriptEnvironment environment,
		ColumnEntryMemory memory,
		DataCompileContext context,
		boolean useColumn,
		ColumnValueDependencyHolder dependencies
	) {
		String name = memory.getTyped(ColumnEntryMemory.ACCESSOR_ID).toString();
		MethodInfo getter = memory.getTyped(ColumnEntryMemory.GETTER).info;
		RegistryEntry<ColumnEntry> self = memory.getTyped(ColumnEntryMemory.REGISTRY_ENTRY);
		InsnTree loadHolder = useColumn ? context.loadColumn() : context.loadSelf();
		if (getter.paramTypes.length > 0) {
			environment.addFunction(name, new FunctionHandler.Named("functionInvoke: " + getter + " for receiver " + loadHolder.describe(), (ExpressionParser parser, String name1, InsnTree... arguments) -> {
				InsnTree[] castArguments = ScriptEnvironment.castArguments(parser, getter, CastMode.IMPLICIT_NULL, arguments);
				if (castArguments == null) return null;
				dependencies.addDependency(self);
				return new CastResult(invokeInstance(loadHolder, getter, castArguments), castArguments != arguments);
			}));
			environment.addMethod(getter.owner, name, new MethodHandler.Named("methodInvoke: " + getter, (ExpressionParser parser, InsnTree receiver, String name1, GetMethodMode mode, InsnTree... arguments) -> {
				InsnTree[] castArguments = ScriptEnvironment.castArguments(parser, getter, CastMode.IMPLICIT_NULL, arguments);
				if (castArguments == null) return null;
				dependencies.addDependency(self);
				return new CastResult(mode.makeInvoker(parser, receiver, getter, castArguments), castArguments != arguments);
			}));
		}
		else {
			InsnTree tree = invokeInstance(loadHolder, getter);
			environment.addVariable(name, new VariableHandler.Named(tree.describe(), (ExpressionParser parser, String name1) -> {
				dependencies.addDependency(self);
				return tree;
			}));
			environment.addField(getter.owner, name, new FieldHandler.Named("fieldInvoke: " + getter, (ExpressionParser parser, InsnTree receiver, String name1, GetFieldMode mode) -> {
				dependencies.addDependency(self);
				return mode.makeInvoker(parser, receiver, getter);
			}));
		}
	}

	public default void setupExternalEnvironment(MutableScriptEnvironment environment, ColumnEntryMemory memory, ColumnCompileContext context, ExternalEnvironmentParams params) {
		String exposedName = memory.getTyped(ColumnEntryMemory.ACCESSOR_ID).toString();
		MethodInfo getter = memory.getTyped(ColumnEntryMemory.GETTER).info;
		MethodInfo setter = params.mutable && memory.getTyped(ColumnEntryMemory.ENTRY).isSettable() ? memory.getTyped(ColumnEntryMemory.SETTER).info : null;
		RegistryEntry<ColumnEntry> entry = memory.getTyped(ColumnEntryMemory.REGISTRY_ENTRY);
		ColumnValueDependencyHolder caller = params.caller;
		InsnTree loadColumn;
		if (params.loadLookup != null) {
			if (this.getAccessSchema().is_3d()) {
				environment.addFunction(exposedName, new FunctionHandler.Named(getter.toString(), (ExpressionParser parser, String name, InsnTree... arguments) -> {
					InsnTree[] castArguments = ScriptEnvironment.castArguments(parser, name, types("III"), CastMode.IMPLICIT_NULL, arguments);
					if (castArguments == null) return null;
					if (caller != null) caller.addDependency(entry);
					return new CastResult(
						setter != null
						? new ColumnLookupMutableGet3DValueInsnTree(
							params.loadLookup,
							castArguments[0],
							castArguments[1],
							castArguments[2],
							getter,
							setter
						)
						: new ColumnLookupGet3DValueInsnTree(
							params.loadLookup,
							castArguments[0],
							castArguments[1],
							castArguments[2],
							getter
						),
						castArguments != arguments
					);
				}));
			}
			else {
				environment.addFunction(exposedName, new FunctionHandler.Named(getter.toString(), (ExpressionParser parser, String name, InsnTree... arguments) -> {
					InsnTree[] castArguments = ScriptEnvironment.castArguments(parser, name, types("II"), CastMode.IMPLICIT_NULL, arguments);
					if (castArguments == null) return null;
					if (caller != null) caller.addDependency(entry);
					DirectCastInsnTree castColumn = new DirectCastInsnTree(
						invokeInstance(
							params.loadLookup,
							ColumnLookupGet3DValueInsnTree.LOOKUP_COLUMN,
							castArguments
						),
						getter.owner
					);
					return new CastResult(
						setter != null
						? new GetterSetterInsnTree(castColumn, getter, setter)
						: invokeInstance(castColumn, getter),
						castArguments != arguments
					);
				}));
			}
			if (params.loadX != null) {
				loadColumn = new DirectCastInsnTree(
					invokeInstance(
						params.loadLookup,
						ColumnLookupGet3DValueInsnTree.LOOKUP_COLUMN,
						params.loadX,
						params.loadZ
					),
					getter.owner
				);
			}
			else {
				loadColumn = null;
			}
		}
		else {
			loadColumn = params.loadColumn;
		}
		if (loadColumn != null) {
			if (this.getAccessSchema().is_3d()) {
				environment.addFunction(exposedName, new FunctionHandler.Named(getter.toString(), (ExpressionParser parser, String name, InsnTree... arguments) -> {
					InsnTree[] castArguments = ScriptEnvironment.castArguments(parser, getter, CastMode.IMPLICIT_NULL, arguments);
					if (castArguments == null) return null;
					if (caller != null) caller.addDependency(entry);
					return new CastResult(
						setter != null
						? new GetterSetterInsnTree(loadColumn, getter, setter)
						: invokeInstance(loadColumn, getter, castArguments),
						castArguments != arguments
					);
				}));
				InsnTree loadY = params.loadY;
				if (loadY != null) {
					environment.addVariable(exposedName, new VariableHandler.Named(getter.toString(), (ExpressionParser parser, String name) -> {
						if (caller != null) caller.addDependency(entry);
						return (
							setter != null
							? new ArgumentedGetterSetterInsnTree(loadColumn, getter, setter, loadY)
							: invokeInstance(loadColumn, getter, loadY)
						);
					}));
				}
			}
			else {
				environment.addVariable(exposedName, new VariableHandler.Named(getter.toString(), (ExpressionParser parser, String name) -> {
					if (caller != null) caller.addDependency(entry);
					return (
						setter != null
						? new GetterSetterInsnTree(loadColumn, getter, setter)
						: invokeInstance(loadColumn, getter)
					);
				}));
			}
		}
	}

	public static class ExternalEnvironmentParams {

		/**
		invariants:
		must specify loadColumn or loadLookup, but not both.
		if loadColumn is specified, then loadX and loadZ are ignored.
		must specify both loadX and loadZ, or neither.
		*/
		public InsnTree loadColumn, loadLookup, loadX, loadY, loadZ;
		public boolean mutable;
		public ColumnValueDependencyHolder caller;

		public ExternalEnvironmentParams withColumn(InsnTree    loadColumn) { this.loadColumn = loadColumn; return this; }
		public ExternalEnvironmentParams withLookup(InsnTree    loadLookup) { this.loadLookup = loadLookup; return this; }
		public ExternalEnvironmentParams withX     (InsnTree    loadX     ) { this.loadX      = loadX     ; return this; }
		public ExternalEnvironmentParams withY     (InsnTree    loadY     ) { this.loadY      = loadY     ; return this; }
		public ExternalEnvironmentParams withZ     (InsnTree    loadZ     ) { this.loadZ      = loadZ     ; return this; }
		public ExternalEnvironmentParams mutable   (boolean     mutable   ) { this.mutable    = mutable   ; return this; }
		public ExternalEnvironmentParams mutable   (                      ) { this.mutable    = true      ; return this; }
		public ExternalEnvironmentParams withCaller(ColumnEntry caller    ) { this.caller     = caller    ; return this; }
	}

	public abstract void emitComputer(ColumnEntryMemory memory, DataCompileContext context) throws ScriptParsingException;

	/**
	a quick-and-dirty way of transferring information between
	{@link #emitFieldGetterAndSetter(ColumnEntryMemory, DataCompileContext)},
	{@link #setupInternalEnvironment(MutableScriptEnvironment, ColumnEntryMemory, DataCompileContext, boolean, ColumnValueDependencyHolder)},
	and {@link #emitComputer(ColumnEntryMemory, DataCompileContext)}.
	*/
	public static class ColumnEntryMemory extends HashMap<ColumnEntryMemory.Key<?>, Object> {

		public static final Key<RegistryEntry<ColumnEntry>>
			REGISTRY_ENTRY = new Key<>("registryEntry");
		public static final Key<ColumnEntry>
			ENTRY = new Key<>("entry");
		public static final Key<Identifier>
			ACCESSOR_ID = new Key<>("accessorID");
		public static final Key<Integer>
			FLAGS_INDEX = new Key<>("flagsIndex");
		public static final Key<String>
			INTERNAL_NAME = new Key<>("internalName");
		public static final Key<FieldCompileContext>
			FIELD = new Key<>("field");
		public static final Key<MethodCompileContext>
			GETTER = new Key<>("getter"),
			SETTER = new Key<>("setter"),
			COMPUTER = new Key<>("computer"),
			VALID_WHERE = new Key<>("validWhere");
		public static final Key<TypeContext>
			TYPE_CONTEXT = new Key<>("typeContext");
		public static final Key<AccessContext>
			ACCESS_CONTEXT = new Key<>("accessContext");

		public ColumnEntryMemory(RegistryEntry<ColumnEntry> entry) {
			this.putTyped(REGISTRY_ENTRY, entry);
			this.putTyped(ACCESSOR_ID, UnregisteredObjectException.getID(entry));
			this.putTyped(ENTRY, entry.value());
		}

		public <T> T getTyped(Key<T> key) {
			@SuppressWarnings("unchecked")
			T result = (T)(this.get(key));
			if (result != null) return result;
			else throw new IllegalArgumentException("Key " + key + " not in " + this);
		}

		public <T> void putTyped(Key<T> key, T value) {
			if (this.putIfAbsent(key, value) != null) {
				throw new IllegalStateException(key + " already present in " + this);
			}
		}

		@SuppressWarnings("unchecked")
		public <T> T addOrGet(Key<T> key, Supplier<T> valueSupplier) {
			return (T)(this.computeIfAbsent(key, k -> valueSupplier.get()));
		}

		public <T> void replaceTyped(Key<T> key, T oldValue, T newValue) {
			if (!this.replace(key, oldValue, newValue)) {
				throw new IllegalStateException(key + " was bound to " + this.get(key) + " when trying to replace " + oldValue + " with " + newValue + " in " + this);
			}
		}

		public static record Key<T>(String name) {

			@Override
			public boolean equals(Object obj) {
				return this == obj;
			}

			@Override
			public int hashCode() {
				return System.identityHashCode(this);
			}

			@Override
			public String toString() {
				return this.name;
			}
		}
	}
}