package builderb0y.bigglobe.columns.scripted.entries;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.function.Supplier;

import org.jetbrains.annotations.MustBeInvokedByOverriders;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.TestOnly;
import org.jetbrains.annotations.UnknownNullability;
import org.objectweb.asm.tree.AnnotationNode;

import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;

import builderb0y.autocodec.annotations.MemberUsage;
import builderb0y.autocodec.annotations.UseCoder;
import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.codecs.CoderRegistry;
import builderb0y.bigglobe.codecs.CoderRegistryTyped;
import builderb0y.bigglobe.columns.scripted.*;
import builderb0y.bigglobe.columns.scripted.AccessSchema.AccessContext;
import builderb0y.bigglobe.columns.scripted.compile.ColumnCompileContext;
import builderb0y.bigglobe.columns.scripted.compile.DataCompileContext;
import builderb0y.bigglobe.columns.scripted.dependencies.DependencyView;
import builderb0y.bigglobe.columns.scripted.tree.*;
import builderb0y.bigglobe.columns.scripted.types.ColumnValueType.TypeContext;
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
public interface ColumnEntry extends CoderRegistryTyped<ColumnEntry>, DependencyView {

	@TestOnly
	public static class Testing {

		public static boolean TESTING = false; //enabled by junit, prevents the registry from being created on class initialization.
	}

	@UnknownNullability
	@SuppressWarnings("TestOnlyProblems")
	public static final CoderRegistry<ColumnEntry> REGISTRY = Testing.TESTING ? null : new CoderRegistry<>(BigGlobeMod.modID("column_value"));
	public static final Object INITIALIZER = new Object() {{
		if (REGISTRY != null) {
			REGISTRY.registerAuto(BigGlobeMod.modID("constant"),          ConstantColumnEntry.class);
			REGISTRY.registerAuto(BigGlobeMod.modID("noise"),                NoiseColumnEntry.class);
			REGISTRY.registerAuto(BigGlobeMod.modID("script"),              ScriptColumnEntry.class);
			REGISTRY.registerAuto(BigGlobeMod.modID("decision_tree"), DecisionTreeColumnEntry.class);
			REGISTRY.registerAuto(BigGlobeMod.modID("voronoi"),            VoronoiColumnEntry.class);
		}
	}};

	public abstract AccessSchema getAccessSchema();

	public abstract boolean hasField();

	public default boolean isSettable() {
		return this.hasField();
	}

	public default void populateField(ColumnEntryMemory memory, DataCompileContext context, FieldCompileContext getterMethod) {}

	@MustBeInvokedByOverriders
	public default void populateGetter(ColumnEntryMemory memory, DataCompileContext context, MethodCompileContext getterMethod) {
		annotateWithAccessorID(memory, ColumnValueGetter.DESCRIPTOR, getterMethod);
	}

	@MustBeInvokedByOverriders
	public default void populateSetter(ColumnEntryMemory memory, DataCompileContext context, MethodCompileContext setterMethod) {
		annotateWithAccessorID(memory, ColumnValueSetter.DESCRIPTOR, setterMethod);
	}

	@MustBeInvokedByOverriders
	public default void populatePreComputer(ColumnEntryMemory memory, DataCompileContext context, MethodCompileContext preComputeMethod) {
		annotateWithAccessorID(memory, ColumnValuePreComputer.DESCRIPTOR, preComputeMethod);
	}

	public static void annotateWithAccessorID(ColumnEntryMemory memory, String annotationDescriptor, MethodCompileContext method) {
		AnnotationNode annotation = new AnnotationNode(annotationDescriptor);
		annotation.values = new ArrayList<>(2);
		annotation.values.add("value");
		annotation.values.add(memory.getTyped(ColumnEntryMemory.ACCESSOR_ID).toString());
		method.node.visibleAnnotations = new ArrayList<>(1);
		method.node.visibleAnnotations.add(annotation);
	}

	public default void emitFieldGetterAndSetter(ColumnEntryMemory memory, DataCompileContext context) {
		this.setupMemory(memory, context);
		if (this.hasField()) {
			if (this.isSettable()) {
				this.populateField(memory, context, memory.getTyped(ColumnEntryMemory.FIELD));
				this.populateGetter(memory, context, memory.getTyped(ColumnEntryMemory.GETTER));
				this.populatePreComputer(memory, context, memory.getTyped(ColumnEntryMemory.PRE_COMPUTER));
				this.populateSetter(memory, context, memory.getTyped(ColumnEntryMemory.SETTER));
			}
			else {
				this.populateField(memory, context, memory.getTyped(ColumnEntryMemory.FIELD));
				this.populateGetter(memory, context, memory.getTyped(ColumnEntryMemory.GETTER));
				this.populatePreComputer(memory, context, memory.getTyped(ColumnEntryMemory.PRE_COMPUTER));
			}
		}
		else {
			this.populateGetter(memory, context, memory.getTyped(ColumnEntryMemory.GETTER));
		}
	}

	public default void setupMemory(ColumnEntryMemory memory, DataCompileContext context) {
		int uniqueIndex = context.mainClass.memberUniquifier++;
		Identifier accessID = memory.getTyped(ColumnEntryMemory.ACCESSOR_ID);
		String internalName = DataCompileContext.internalName(accessID, uniqueIndex);
		memory.putTyped(ColumnEntryMemory.INTERNAL_NAME, internalName);

		AccessContext accessContext = memory.getTyped(ColumnEntryMemory.ACCESS_CONTEXT);

		if (this.hasField()) {
			int flagIndex = context.flagsIndex++;
			memory.putTyped(ColumnEntryMemory.FLAGS_INDEX, flagIndex);

			FieldCompileContext valueField = context.mainClass.newField(ACC_PUBLIC, internalName, accessContext.fieldType());
			memory.putTyped(ColumnEntryMemory.FIELD, valueField);
			MethodCompileContext getterMethod = context.mainClass.newMethod(ACC_PUBLIC, "get_" + internalName, accessContext.exposedType(), this.getAccessSchema().getterParameters());
			memory.putTyped(ColumnEntryMemory.GETTER, getterMethod);
			MethodCompileContext preComputer = context.mainClass.newMethod(ACC_PUBLIC, "precompute_" + internalName, TypeInfos.VOID);
			memory.putTyped(ColumnEntryMemory.PRE_COMPUTER, preComputer);

			if (this.isSettable()) {
				MethodCompileContext setterMethod = context.mainClass.newMethod(ACC_PUBLIC, "set_" + internalName, TypeInfos.VOID, this.getAccessSchema().setterParameters(context));
				memory.putTyped(ColumnEntryMemory.SETTER, setterMethod);
			}
		}
		else {
			MethodCompileContext getterMethod = context.mainClass.newMethod(ACC_PUBLIC, "get_" + internalName, accessContext.exposedType(), this.getAccessSchema().getterParameters());
			memory.putTyped(ColumnEntryMemory.GETTER, getterMethod);
		}
	}

	public default InsnTree createGenericGetter(ColumnEntryMemory memory, DataCompileContext context) {
		return invokeInstance(
			context.loadSelf(),
			memory.getTyped(ColumnEntryMemory.GETTER).info,
			this.getAccessSchema().is_3d()
			? new InsnTree[] { load("y", TypeInfos.INT) }
			: InsnTree.ARRAY_FACTORY.empty()
		);
	}

	public default InsnTree createGenericSetter(ColumnEntryMemory memory, DataCompileContext context, InsnTree value) {
		return invokeInstance(
			context.loadSelf(),
			memory.getTyped(ColumnEntryMemory.SETTER).info,
			this.getAccessSchema().is_3d()
			? new InsnTree[] { load("y", TypeInfos.INT), value }
			: new InsnTree[] { value }
		);
	}

	public default InsnTree createGenericPreComputer(ColumnEntryMemory memory, DataCompileContext context) {
		if (!this.hasField()) throw new UnsupportedOperationException("Can't pre-compute without field");
		return invokeInstance(context.loadSelf(), memory.getTyped(ColumnEntryMemory.PRE_COMPUTER).info);
	}

	public default void setupInternalEnvironment(
		MutableScriptEnvironment environment,
		ColumnEntryMemory memory,
		DataCompileContext context,
		boolean useColumn,
		@Nullable InsnTree loadY,
		MutableDependencyView dependencies,
		@Nullable Identifier callerPrefix
	) {
		Identifier selfID = memory.getTyped(ColumnEntryMemory.ACCESSOR_ID);
		this.implSetupInternalEnvironment(environment, memory, context, useColumn, loadY, dependencies, selfID.toString());
		if (callerPrefix != null && callerPrefix.getNamespace().equals(selfID.getNamespace())) {
			int start = relativize(selfID.getPath(), callerPrefix.getPath());
			if (start >= 0) {
				this.implSetupInternalEnvironment(environment, memory, context, useColumn, loadY, dependencies, selfID.getPath().substring(start));
			}
		}
	}

	public static int relativize(String selfPath, String callerPath) {
		int start = 0;
		while (true) {
			int selfSlash = selfPath.indexOf('/', start);
			int callerSlash = callerPath.indexOf('/', start);
			if (selfSlash >= 0) {
				if (callerSlash >= 0) {
					if (selfSlash == callerSlash && selfPath.regionMatches(start, callerPath, start, selfSlash - start)) {
						start = selfSlash + 1; //a:b/c/... trying to reference a:b/c/...
					}
					else {
						return -1; //a:123/... trying to reference a:456/...
					}
				}
				else {
					return start; //a:b/123 trying to reference a:b/c/...
				}
			}
			else {
				if (callerSlash >= 0) {
					return -1; //a:b/c/... trying to reference a:b/123
				}
				else {
					return start; //a:b/123 trying to reference a:b/456
				}
			}
		}
	}

	public default void implSetupInternalEnvironment(
		MutableScriptEnvironment environment,
		ColumnEntryMemory memory,
		DataCompileContext context,
		boolean useColumn,
		@Nullable InsnTree loadY,
		MutableDependencyView dependencies,
		String name
	) {
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
			if (loadY != null) {
				environment.addVariable(name, new VariableHandler.Named("functionInvoke: " + getter + " for receiver " + loadHolder.describe() + " at Y level " + loadY.describe(), (ExpressionParser parser, String name1) -> {
					dependencies.addDependency(self);
					return invokeInstance(loadHolder, getter, loadY);
				}));
				environment.addField(getter.owner, name, new FieldHandler.Named("methodInvoke: " + getter + " at Y level " + loadY.describe(), (ExpressionParser parser, InsnTree receiver, String name1, GetFieldMode mode) -> {
					dependencies.addDependency(self);
					return mode.makeInvoker(parser, receiver, getter, loadY);
				}));
			}
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
		MutableDependencyView caller = params.dependencies;
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
						? new ArgumentedGetterSetterInsnTree(loadColumn, getter, setter, arguments[0])
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
		public MutableDependencyView dependencies;

		public ExternalEnvironmentParams withColumn(InsnTree loadColumn) { this.loadColumn = loadColumn; return this; }
		public ExternalEnvironmentParams withLookup(InsnTree loadLookup) { this.loadLookup = loadLookup; return this; }
		public ExternalEnvironmentParams withXZ    (InsnTree loadX, InsnTree loadZ) { this.loadX = loadX; this.loadZ = loadZ; return this; }
		public ExternalEnvironmentParams withY     (InsnTree loadY     ) { this.loadY      = loadY     ; return this; }
		public ExternalEnvironmentParams mutable   (boolean  mutable   ) { this.mutable    = mutable   ; return this; }
		public ExternalEnvironmentParams mutable   (                   ) { this.mutable    = true      ; return this; }
		public ExternalEnvironmentParams trackDependencies(MutableDependencyView dependencies) { this.dependencies = dependencies; return this; }

		public boolean requiresNoArguments(boolean is3D) {
			return (!is3D || this.loadY != null) && (this.loadColumn != null || (this.loadX != null && this.loadZ != null));
		}

		public String getPossibleArguments(boolean is3D) {
			return (
				'(' +
				(this.loadColumn != null ? "forbidden" : this.loadX != null ? "optional" : "required") + " int x, " +
				(!is3D                   ? "forbidden" : this.loadY != null ? "optional" : "required") + " int y, " +
				(this.loadColumn != null ? "forbidden" : this.loadX != null ? "optional" : "required") + " int z" +
				')'
			);
		}

		public static StringBuilder appendIfMissing(StringBuilder builder, String columnValueName, InsnTree tree, String componentName) {
			return tree == null ? (builder == null ? new StringBuilder(columnValueName).append(" requires ") : builder.append(", ")).append(componentName) : builder;
		}

		//todo: make use of this in more places.
		public CastResult resolveColumn(
			ExpressionParser parser,
			String name,
			boolean is3D,
			boolean hasTraits,
			MethodInfo valueGetter,
			InsnTree... arguments
		)
		throws ScriptParsingException {
			if (!is3D && (arguments.length & 1) != 0) {
				throw new ScriptParsingException("Invalid number of arguments for 2D column value " + name, parser.input);
			}
			if (this.loadColumn != null && arguments.length >= 2) {
				throw new ScriptParsingException("x and z are hard-coded in this context and cannot be manually specified.", parser.input);
			}
			InsnTree x, y, z;
			switch (arguments.length) {
				case 0 -> {
					x = this.loadX;
					y = this.loadY;
					z = this.loadZ;
				}
				case 1 -> {
					x = this.loadX;
					y = arguments[0];
					z = this.loadZ;
				}
				case 2 -> {
					x = arguments[0];
					y = this.loadY;
					z = arguments[1];
				}
				case 3 -> {
					x = arguments[0];
					y = arguments[1];
					z = arguments[2];
				}
				default -> {
					throw new ScriptParsingException("Too many arguments for column value " + name, parser.input);
				}
			}
			boolean requiredCasting = false;
			if (x != null && x != (x = x.cast(parser, TypeInfos.INT, CastMode.IMPLICIT_THROW))) requiredCasting = true;
			if (y != null && x != (y = y.cast(parser, TypeInfos.INT, CastMode.IMPLICIT_THROW))) requiredCasting = true;
			if (z != null && x != (z = z.cast(parser, TypeInfos.INT, CastMode.IMPLICIT_THROW))) requiredCasting = true;

			StringBuilder error = null;
			if (this.loadColumn == null) error = appendIfMissing(error, name, x, "x");
			if (is3D                   ) error = appendIfMissing(error, name, y, "y");
			if (this.loadColumn == null) error = appendIfMissing(error, name, z, "z");
			if (error != null) throw new ScriptParsingException(error.toString(), parser.input);

			InsnTree result;
			if (is3D) {
				if (this.loadColumn != null) {
					if (hasTraits) {
						result = new ColumnGet3DValueWithTraitsInsnTree(this.loadColumn, y, valueGetter);
					}
					else {
						result = invokeInstance(this.loadColumn.cast(parser, valueGetter.owner, CastMode.EXPLICIT_THROW), valueGetter, y);
					}
				}
				else {
					if (hasTraits) {
						result = new ColumnLookupGet3DValueFromTraitsInsnTree(this.loadLookup, x, y, z, valueGetter);
					}
					else {
						result = new ColumnLookupGet3DValueInsnTree(this.loadLookup, x, y, z, valueGetter);
					}
				}
			}
			else {
				if (this.loadColumn != null) {
					if (hasTraits) {
						result = new ColumnGet2DValueWithTraitsInsnTree(this.loadColumn, x, z, valueGetter);
					}
					else {
						result = invokeInstance(this.loadColumn.cast(parser, valueGetter.owner, CastMode.EXPLICIT_THROW), valueGetter);
					}
				}
				else {
					if (hasTraits) {
						result = new ColumnLookupGet2DValueFromTraitsInsnTree(this.loadLookup, x, z, valueGetter);
					}
					else {
						result = new ColumnLookupGet2DValueInsnTree(this.loadLookup, x, z, valueGetter);
					}
				}
			}
			return new CastResult(result, requiredCasting);
		}
	}

	public abstract void emitComputer(ColumnEntryMemory memory, DataCompileContext context) throws ScriptParsingException;

	/**
	a quick-and-dirty way of transferring information between
	{@link #emitFieldGetterAndSetter(ColumnEntryMemory, DataCompileContext)},
	{@link #setupInternalEnvironment(MutableScriptEnvironment, ColumnEntryMemory, DataCompileContext, boolean, InsnTree, MutableDependencyView, Identifier)},
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
			COMPUTE_TEST = new Key<>("computeTest"),
			COMPUTE_NO_TEST = new Key<>("computeNoTest"),
			PRE_COMPUTER = new Key<>("preComputer"),
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