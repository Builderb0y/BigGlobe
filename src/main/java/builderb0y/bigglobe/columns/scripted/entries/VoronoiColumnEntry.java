package builderb0y.bigglobe.columns.scripted.entries;

import java.lang.invoke.*;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import net.minecraft.registry.entry.RegistryEntry;

import builderb0y.autocodec.annotations.VerifyNullable;
import builderb0y.autocodec.decoders.DecodeContext;
import builderb0y.autocodec.util.AutoCodecUtil;
import builderb0y.autocodec.verifiers.VerifyContext;
import builderb0y.autocodec.verifiers.VerifyException;
import builderb0y.bigglobe.columns.scripted.*;
import builderb0y.bigglobe.columns.scripted.ScriptedColumn.VoronoiDataBase;
import builderb0y.bigglobe.columns.scripted.compile.ColumnCompileContext;
import builderb0y.bigglobe.columns.scripted.compile.DataCompileContext;
import builderb0y.bigglobe.columns.scripted.compile.VoronoiBaseCompileContext;
import builderb0y.bigglobe.columns.scripted.compile.VoronoiImplCompileContext;
import builderb0y.bigglobe.columns.scripted.dependencies.ColumnValueDependencyHolder;
import builderb0y.bigglobe.columns.scripted.types.VoronoiColumnValueType;
import builderb0y.bigglobe.noise.Permuter;
import builderb0y.bigglobe.randomLists.IRandomList;
import builderb0y.bigglobe.randomLists.RandomList;
import builderb0y.bigglobe.settings.VoronoiDiagram2D;
import builderb0y.bigglobe.settings.VoronoiDiagram2D.Cell;
import builderb0y.scripting.bytecode.*;
import builderb0y.scripting.bytecode.tree.ConstantValue;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.bytecode.tree.InsnTree.CastMode;
import builderb0y.scripting.bytecode.tree.instructions.fields.NullableInstanceGetFieldInsnTree;
import builderb0y.scripting.bytecode.tree.instructions.invokers.ArgumentedGetterSetterInsnTree;
import builderb0y.scripting.environments.MutableScriptEnvironment;
import builderb0y.scripting.environments.MutableScriptEnvironment.CastResult;
import builderb0y.scripting.environments.MutableScriptEnvironment.FieldHandler;
import builderb0y.scripting.environments.MutableScriptEnvironment.MethodHandler;
import builderb0y.scripting.environments.ScriptEnvironment;
import builderb0y.scripting.environments.ScriptEnvironment.GetFieldMode;
import builderb0y.scripting.environments.ScriptEnvironment.GetMethodMode;
import builderb0y.scripting.parsing.ExpressionParser;
import builderb0y.scripting.parsing.ScriptParsingException;
import builderb0y.scripting.util.TypeInfos;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public class VoronoiColumnEntry extends AbstractColumnEntry {

	public static final MethodHandle RANDOMIZE;
	static {
		try {
			RANDOMIZE = MethodHandles.lookup().findStatic(VoronoiColumnEntry.class, "randomize", MethodType.methodType(VoronoiDataBase.class, IRandomList.class, long.class, ScriptedColumn.class, Cell.class));
		}
		catch (Exception exception) {
			throw AutoCodecUtil.rethrow(exception);
		}
	}

	public final VoronoiDiagram2D diagram;

	public VoronoiColumnEntry(
		VoronoiDiagram2D diagram,
		AccessSchema params,
		@VerifyNullable Valid valid,
		DecodeContext<?> decodeContext
	) {
		super(params, valid, true, decodeContext);
		this.diagram = diagram;
		if (!(params.type() instanceof VoronoiColumnValueType)) {
			throw new IllegalArgumentException("params.type must be 'bigglobe:voronoi' when column value type is 'bigglobe:voronoi'");
		}
		if (params.is_3d()) {
			throw new IllegalArgumentException("params.is_3d must be false when column value is 'bigglobe:voronoi'");
		}
	}

	public static CallSite createRandomizer(MethodHandles.Lookup lookup, String name, MethodType methodType, Class<?>... options) throws Throwable {
		if (methodType.returnType().getSuperclass() != VoronoiDataBase.class) {
			throw new IllegalArgumentException("Invalid super class: " + methodType.returnType().getSuperclass());
		}
		if (options.length == 0) {
			if (Modifier.isAbstract(methodType.returnType().getModifiers())) {
				throw new IllegalArgumentException("No options");
			}
			else {
				return new ConstantCallSite(lookup.findConstructor(methodType.returnType(), methodType.changeReturnType(void.class)));
			}
		}
		long seed = methodType.returnType().getDeclaredField("SEED").getLong(null);
		MethodType constructorType = methodType.changeReturnType(void.class);
		MethodType factoryType = methodType.changeReturnType(VoronoiDataBase.class);
		RandomList<VoronoiDataBase.Factory> list = new RandomList<>(options.length);
		for (Class<?> option : options) {
			option.asSubclass(VoronoiDataBase.class);
			VoronoiDataBase.Factory factory = (VoronoiDataBase.Factory)(
				LambdaMetafactory.altMetafactory(
					lookup,
					"create",
					MethodType.methodType(VoronoiDataBase.Factory.class),
					factoryType,
					lookup.findConstructor(option, constructorType),
					MethodType.methodType(option, methodType.parameterType(0), VoronoiDiagram2D.Cell.class),
					LambdaMetafactory.FLAG_BRIDGES,
					1,
					MethodType.methodType(VoronoiDataBase.class, ScriptedColumn.class, VoronoiDiagram2D.Cell.class)
				)
				.getTarget()
				.invokeExact()
			);
			double weight = option.getDeclaredField("WEIGHT").getDouble(null);
			list.add(factory, weight);
		}
		return new ConstantCallSite(
			MethodHandles
			.insertArguments(RANDOMIZE, 0, list.optimize(), seed)
			.asType(methodType)
		);
	}

	public static VoronoiDataBase randomize(IRandomList<VoronoiDataBase.Factory> factories, long baseSeed, ScriptedColumn column, VoronoiDiagram2D.Cell cell) {
		return factories.getRandomElement(cell.center.getSeed(baseSeed)).create(column, cell);
	}

	@Override
	public boolean hasField() {
		return true;
	}

	@Override
	public boolean isSettable() {
		return false;
	}

	public VoronoiColumnValueType voronoiType() {
		return (VoronoiColumnValueType)(this.params.type());
	}

	public Map<String, AccessSchema> exports() {
		return this.voronoiType().exports;
	}

	@Override
	public void populateSetter(ColumnEntryMemory memory, DataCompileContext context, MethodCompileContext setterMethod) {

	}

	@Override
	public void emitFieldGetterAndSetter(ColumnEntryMemory memory, DataCompileContext context) {
		super.emitFieldGetterAndSetter(memory, context);

		VoronoiManager voronoiManager = context.root().registry.voronoiManager;
		VoronoiBaseCompileContext voronoiBaseContext = voronoiManager.getBaseContextFor(this);
		voronoiBaseContext.mainClass.newField(ACC_PUBLIC | ACC_STATIC | ACC_FINAL, "SEED", TypeInfos.LONG).node.value = Permuter.permute(0L, memory.getTyped(ColumnEntryMemory.ACCESSOR_ID));

		for (Map.Entry<String, AccessSchema> entry : this.exports().entrySet()) {
			voronoiBaseContext.mainClass.newMethod(ACC_PUBLIC | ACC_ABSTRACT, "get_" + entry.getKey(), context.root().getTypeContext(entry.getValue().type()).type(), entry.getValue().getterParameters());
			voronoiBaseContext.mainClass.newMethod(ACC_PUBLIC | ACC_ABSTRACT, "set_" + entry.getKey(), TypeInfos.VOID, entry.getValue().setterParameters(context));
		}
		for (RegistryEntry<VoronoiSettings> voronoiEntry : voronoiManager.getOptionsFor(this)) {
			VoronoiImplCompileContext implContext = voronoiManager.getImplContextFor(voronoiEntry.value());
			implContext.mainClass.newField(ACC_PUBLIC | ACC_STATIC | ACC_FINAL, "WEIGHT", TypeInfos.DOUBLE).node.value = voronoiEntry.value().weight();

			for (Map.Entry<String, RegistryEntry<ColumnEntry>> export : voronoiEntry.value().exports().entrySet()) {
				AccessSchema schema = export.getValue().value().getAccessSchema();
				MethodCompileContext getterDelegator = implContext.mainClass.newMethod(ACC_PUBLIC, "get_" + export.getKey(), implContext.root().getTypeContext(schema.type()).type(), schema.getterParameters());
				LazyVarInfo self = new LazyVarInfo("this", getterDelegator.clazz.info);
				LazyVarInfo loadY = schema.is_3d() ? new LazyVarInfo("y", TypeInfos.INT) : null;
				ColumnEntryMemory exportMemory = implContext.getMemories().get(export.getValue().value());
				return_(
					invokeInstance(
						load(self),
						exportMemory.getTyped(ColumnEntryMemory.GETTER).info,
						loadY != null ? new InsnTree[] { load(loadY) } : InsnTree.ARRAY_FACTORY.empty()
					)
				)
				.emitBytecode(getterDelegator);
				getterDelegator.endCode();

				LazyVarInfo value = new LazyVarInfo("value", implContext.root().getAccessContext(schema).exposedType());
				MethodCompileContext setterDelegator = implContext.mainClass.newMethod(ACC_PUBLIC, "set_" + export.getKey(), TypeInfos.VOID, schema.setterParameters(implContext));
				if (exportMemory.containsKey(ColumnEntryMemory.SETTER)) {
					return_(
						invokeInstance(
							load(self),
							exportMemory.getTyped(ColumnEntryMemory.SETTER).info,
							loadY != null ? new InsnTree[] { load(loadY), load(value) } : new InsnTree[] { load(value) }
						)
					)
					.emitBytecode(setterDelegator);
				}
				else {
					return_(noop).emitBytecode(setterDelegator);
				}
				setterDelegator.endCode();
			}
		}
	}

	@Override
	public void setupExternalEnvironment(MutableScriptEnvironment environment, ColumnEntryMemory memory, ColumnCompileContext context, ExternalEnvironmentParams params) {
		super.setupExternalEnvironment(environment, memory, context, params);
		ColumnValueDependencyHolder dependencies = params.caller;
		List<RegistryEntry<VoronoiSettings>> options = context.registry.voronoiManager.getOptionsFor(this);
		DataCompileContext selfContext = context.root().getAccessContext(this.getAccessSchema()).context();
		for (Map.Entry<String, AccessSchema> export : this.exports().entrySet()) {
			Runnable dependencyTrigger;
			if (dependencies != null) {
				@SuppressWarnings("unchecked")
				RegistryEntry<ColumnEntry>[] triggers = (
					options
					.stream()
					.map(RegistryEntry::value)
					.map(VoronoiSettings::exports)
					.map((Map<String, RegistryEntry<ColumnEntry>> exports) -> exports.get(export.getKey()))
					.peek(Objects::requireNonNull)
					.toArray(RegistryEntry[]::new)
				);
				dependencyTrigger = () -> {
					for (RegistryEntry<ColumnEntry> trigger : triggers) {
						dependencies.addDependency(trigger);
					}
				};
			}
			else {
				dependencyTrigger = null;
			}
			MethodInfo getter = export.getValue().getterDescriptor(ACC_PUBLIC | ACC_ABSTRACT, "get_" + export.getKey(), selfContext);
			MethodInfo setter = export.getValue().setterDescriptor(ACC_PUBLIC | ACC_ABSTRACT, "set_" + export.getKey(), selfContext);
			if (export.getValue().is_3d()) {
				if (params.mutable) {
					environment.addMethod(getter.owner, export.getKey(), (ExpressionParser parser, InsnTree receiver, String name, GetMethodMode mode, InsnTree... arguments) -> {
						InsnTree[] castArguments = ScriptEnvironment.castArguments(parser, getter, CastMode.IMPLICIT_NULL, arguments);
						if (castArguments == null) return null;
						if (dependencyTrigger != null) dependencyTrigger.run();
						return new CastResult(new ArgumentedGetterSetterInsnTree(receiver, getter, setter, castArguments[0]), castArguments != arguments);
					});
				}
				else {
					environment.addMethod(getter.owner, export.getKey(), new MethodHandler.Named("methodInvoke: " + getter, (ExpressionParser parser, InsnTree receiver, String name1, GetMethodMode mode, InsnTree... arguments) -> {
						InsnTree[] castArguments = ScriptEnvironment.castArguments(parser, getter, CastMode.IMPLICIT_NULL, arguments);
						if (castArguments == null) return null;
						if (dependencyTrigger != null) dependencyTrigger.run();
						return new CastResult(mode.makeInvoker(parser, receiver, getter, castArguments), castArguments != arguments);
					}));
				}
			}
			else {
				if (params.mutable) {
					environment.addField(getter.owner, export.getKey(), (ExpressionParser parser, InsnTree receiver, String name, GetFieldMode mode) -> {
						if (dependencyTrigger != null) dependencyTrigger.run();
						return mode.makeGetterSetter(parser, receiver, getter, setter);
					});
				}
				else {
					environment.addField(getter.owner, export.getKey(), new FieldHandler.Named("fieldInvoke: " + getter, (ExpressionParser parser, InsnTree receiver, String name1, GetFieldMode mode) -> {
						if (dependencyTrigger != null) dependencyTrigger.run();
						return mode.makeInvoker(parser, receiver, getter);
					}));
				}
			}
		}
	}

	@Override
	public void populateCompute2D(ColumnEntryMemory memory, DataCompileContext context, MethodCompileContext computeMethod) throws ScriptParsingException {
		ConstantValue diagram = ConstantValue.ofManual(this.diagram, type(VoronoiDiagram2D.class));
		FieldCompileContext valueField = memory.getTyped(ColumnEntryMemory.FIELD);
		FieldInfo cellField = FieldInfo.getField(VoronoiDataBase.class, "cell");
		return_(
			invokeDynamic(
				MethodInfo.getMethod(VoronoiColumnEntry.class, "createRandomizer"),
				new MethodInfo(
					ACC_PUBLIC | ACC_STATIC,
					TypeInfos.OBJECT, //ignored.
					"randomize",
					memory.getTyped(ColumnEntryMemory.ACCESS_CONTEXT).exposedType(),
					context.root().columnType(),
					type(VoronoiDiagram2D.Cell.class)
				),
				context.root().registry.voronoiManager.getImplContextsFor(this).stream().map((VoronoiImplCompileContext impl) -> constant(impl.mainClass.info)).toArray(ConstantValue[]::new),
				new InsnTree[] {
					context.loadColumn(),
					invokeInstance(
						ldc(diagram),
						MethodInfo.findMethod(VoronoiDiagram2D.class, "getNearestCell", VoronoiDiagram2D.Cell.class, int.class, int.class, VoronoiDiagram2D.Cell.class),
						ScriptedColumn.INFO.x(context.loadColumn()),
						ScriptedColumn.INFO.z(context.loadColumn()),
						new NullableInstanceGetFieldInsnTree(
							getField(
								context.loadSelf(),
								valueField.info
							),
							cellField
						)
					)
				}
			)
		)
		.emitBytecode(computeMethod);
		computeMethod.endCode();
	}

	@Override
	public void populateCompute3D(ColumnEntryMemory memory, DataCompileContext context, MethodCompileContext computeMethod) throws ScriptParsingException {
		throw new UnsupportedOperationException();
	}

	public static <T> void checkNotReserved(VerifyContext<T, String> context) throws VerifyException {
		String name = context.object;
		if (name != null) switch (name) {
			case
				"cell_x",
				"cell_z",
				"center_x",
				"center_z",
				"soft_distance_squared",
				"soft_distance",
				"hard_distance",
				"hard_distance_squared",
				"euclidean_distance_squared",
				"euclidean_distance"
			-> {
				throw new VerifyException(() -> "Export name " + name + " is built-in, and cannot be overridden.");
			}
		}
	}
}