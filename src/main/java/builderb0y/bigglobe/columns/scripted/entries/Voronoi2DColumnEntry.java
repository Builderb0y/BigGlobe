package builderb0y.bigglobe.columns.scripted.entries;

import java.lang.invoke.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.entry.RegistryEntryList;
import net.minecraft.registry.tag.TagKey;

import builderb0y.autocodec.annotations.DefaultEmpty;
import builderb0y.autocodec.annotations.MemberUsage;
import builderb0y.autocodec.annotations.UseVerifier;
import builderb0y.autocodec.annotations.VerifyNullable;
import builderb0y.autocodec.util.AutoCodecUtil;
import builderb0y.autocodec.verifiers.VerifyContext;
import builderb0y.autocodec.verifiers.VerifyException;
import builderb0y.bigglobe.columns.scripted.AccessSchema;
import builderb0y.bigglobe.columns.scripted.AccessSchemas.Voronoi2DAccessSchema;
import builderb0y.bigglobe.columns.scripted.DataCompileContext;
import builderb0y.bigglobe.columns.scripted.DataCompileContext.VoronoiBaseCompileContext;
import builderb0y.bigglobe.columns.scripted.DataCompileContext.VoronoiImplCompileContext;
import builderb0y.bigglobe.columns.scripted.ScriptedColumn.VoronoiDataBase;
import builderb0y.bigglobe.columns.scripted.Valids._2DValid;
import builderb0y.bigglobe.columns.scripted.VoronoiSettings;
import builderb0y.bigglobe.noise.Permuter;
import builderb0y.bigglobe.randomLists.RandomList;
import builderb0y.bigglobe.settings.VoronoiDiagram2D;
import builderb0y.bigglobe.settings.VoronoiDiagram2D.Cell;
import builderb0y.bigglobe.util.UnregisteredObjectException;
import builderb0y.scripting.bytecode.*;
import builderb0y.scripting.bytecode.tree.ConstantValue;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.bytecode.tree.instructions.fields.NullableInstanceGetFieldInsnTree;
import builderb0y.scripting.parsing.GenericScriptTemplate.GenericScriptTemplateUsage;
import builderb0y.scripting.parsing.ScriptParsingException;
import builderb0y.scripting.parsing.ScriptUsage;
import builderb0y.scripting.util.TypeInfos;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public class Voronoi2DColumnEntry extends Basic2DColumnEntry {

	public static final ColumnEntryMemory.Key<Map<RegistryKey<VoronoiSettings>, VoronoiImplCompileContext>>
		VORONOI_CONTEXT_MAP = new ColumnEntryMemory.Key<>("voronoiContextMap");
	public static final MethodHandle RANDOMIZE;
	static {
		try {
			RANDOMIZE = MethodHandles.lookup().findStatic(Voronoi2DColumnEntry.class, "randomize", MethodType.methodType(VoronoiDataBase.class, RandomList.class, long.class, Cell.class));
		}
		catch (Exception exception) {
			throw AutoCodecUtil.rethrow(exception);
		}
	}

	public final VoronoiDiagram2D value;
	public final TagKey<VoronoiSettings> values;
	public final @DefaultEmpty Map<@UseVerifier(name = "checkNotReserved", in = Voronoi2DColumnEntry.class, usage = MemberUsage.METHOD_IS_HANDLER) String, AccessSchema> exports;
	public final @VerifyNullable Valid valid;
	public static record Valid(ScriptUsage<GenericScriptTemplateUsage> where) implements _2DValid {

		@Override
		public ConstantValue getFallback(TypeInfo type) {
			return ConstantValue.of(null, type);
		}
	}

	public Voronoi2DColumnEntry(
		VoronoiDiagram2D value,
		TagKey<VoronoiSettings> values,
		Map<String, AccessSchema> exports,
		@VerifyNullable Valid valid
	) {
		this.value   = value;
		this.values  = values;
		this.exports = exports;
		this.valid   = valid;
	}

	@Override
	public _2DValid valid() {
		return this.valid;
	}

	public static CallSite createRandomizer(MethodHandles.Lookup lookup, String name, MethodType methodType, Class<?>... options) throws Throwable {
		if (methodType.returnType().getSuperclass() != VoronoiDataBase.class) {
			throw new IllegalArgumentException("Invalid super class: " + methodType.returnType().getSuperclass());
		}
		long seed = methodType.returnType().getDeclaredField("SEED").getLong(null);
		RandomList<VoronoiDataBase.Factory> list = new RandomList<>(options.length);
		for (Class<?> option : options) {
			option.asSubclass(VoronoiDataBase.class);
			VoronoiDataBase.Factory factory = (VoronoiDataBase.Factory)(
				LambdaMetafactory.metafactory(
					lookup,
					"create",
					MethodType.methodType(VoronoiDataBase.Factory.class),
					MethodType.methodType(VoronoiDataBase.class, VoronoiDiagram2D.Cell.class),
					lookup.findConstructor(option, MethodType.methodType(void.class, VoronoiDiagram2D.Cell.class)),
					MethodType.methodType(option, VoronoiDiagram2D.Cell.class)
				)
				.getTarget()
				.invokeExact()
			);
			double weight = option.getDeclaredField("WEIGHT").getDouble(null);
			list.add(factory, weight);
		}
		return new ConstantCallSite(
			MethodHandles
			.insertArguments(RANDOMIZE, 0, list, seed)
			.asType(methodType)
		);
	}

	public static VoronoiDataBase randomize(RandomList<VoronoiDataBase.Factory> factories, long baseSeed, VoronoiDiagram2D.Cell cell) {
		return factories.isEmpty() ? null : factories.getRandomElement(cell.center.getSeed(baseSeed)).create(cell);
	}

	@Override
	public boolean hasField() {
		return true;
	}

	@Override
	public AccessSchema getAccessSchema() {
		return new Voronoi2DAccessSchema(this.exports);
	}

	@Override
	public void populateSetter(ColumnEntryMemory memory, DataCompileContext context, MethodCompileContext setterMethod) {

	}

	@Override
	public void emitFieldGetterAndSetter(ColumnEntryMemory memory, DataCompileContext context) {
		super.emitFieldGetterAndSetter(memory, context);

		RegistryEntryList<VoronoiSettings> voronoiTag = context.registry.voronois.getOrCreateTag(this.values);
		//sanity check that all implementations of this class export the same values we do.
		for (RegistryEntry<VoronoiSettings> preset : voronoiTag) {
			Map<String, AccessSchema> expected = preset.value().exports().entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().value().getAccessSchema()));
			if (!this.exports.equals(expected)) {
				throw new IllegalStateException("Export mismatch between column value " + memory.getTyped(ColumnEntryMemory.ACCESSOR_ID) + ' ' + this.exports + " and voronoi settings " + UnregisteredObjectException.getID(preset) + ' ' + expected);
			}
		}

		TypeContext baseType = context.getSchemaType(this.getAccessSchema());
		VoronoiBaseCompileContext voronoiBaseContext = (VoronoiBaseCompileContext)(Objects.requireNonNull(baseType.context()));
		voronoiBaseContext.mainClass.newField(ACC_PUBLIC | ACC_STATIC | ACC_FINAL, "SEED", TypeInfos.LONG).node.value = Permuter.permute(0L, memory.getTyped(ColumnEntryMemory.ACCESSOR_ID));

		Map<RegistryKey<VoronoiSettings>, VoronoiImplCompileContext> voronoiContextMap = new HashMap<>();
		memory.putTyped(VORONOI_CONTEXT_MAP, voronoiContextMap);
		for (Map.Entry<String, AccessSchema> entry : this.exports.entrySet()) {
			voronoiBaseContext.mainClass.newMethod(entry.getValue().getterDescriptor(ACC_PUBLIC | ACC_ABSTRACT, "get_" + entry.getKey(), voronoiBaseContext));
		}
		for (RegistryEntry<VoronoiSettings> entry : voronoiTag) {
			VoronoiImplCompileContext implContext = new VoronoiImplCompileContext(voronoiBaseContext);
			voronoiContextMap.put(UnregisteredObjectException.getKey(entry), implContext);
			implContext.mainClass.newField(ACC_PUBLIC | ACC_STATIC | ACC_FINAL, "WEIGHT", TypeInfos.DOUBLE).node.value = entry.value().weight();

			for (RegistryEntry<ColumnEntry> enable : entry.value().enables()) {
				ColumnEntryMemory enabledMemory = Objects.requireNonNull(
					context.registry.memories.get(
						UnregisteredObjectException.getID(entry)
					)
				);
				enable.value().emitFieldGetterAndSetter(enabledMemory, implContext);
			}

			for (Map.Entry<String, RegistryEntry<ColumnEntry>> export : entry.value().exports().entrySet()) {
				ColumnEntryMemory exportMemory = Objects.requireNonNull(
					context.registry.memories.get(
						UnregisteredObjectException.getID(export.getValue())
					)
				);
				AccessSchema schema = export.getValue().value().getAccessSchema();
				implContext.mainClass.newMethod(schema.getterDescriptor(ACC_PUBLIC, "get_" + export.getKey(), context)).scopes.withScope((MethodCompileContext delegator) -> {
					VarInfo self = delegator.addThis();
					VarInfo loadY = schema.requiresYLevel() ? delegator.newParameter("y", TypeInfos.INT) : null;
					return_(
						invokeInstance(
							load(self),
							exportMemory.getTyped(ColumnEntryMemory.GETTER).info,
							loadY != null ? new InsnTree[] { load(loadY) } : InsnTree.ARRAY_FACTORY.empty()
						)
					)
					.emitBytecode(delegator);
				});
			}
		}
	}

	@Override
	public void setupEnvironment(ColumnEntryMemory memory, DataCompileContext context) {
		super.setupEnvironment(memory, context);
		for (Map.Entry<String, AccessSchema> entry : this.exports.entrySet()) {
			context.environment.addVariableRenamedInvoke(context.loadSelf(), entry.getKey(), entry.getValue().getterDescriptor(ACC_PUBLIC | ACC_ABSTRACT, "get_" + entry.getKey(), context));
		}
		RegistryEntryList<VoronoiSettings> voronoiTag = context.registry.voronois.getOrCreateTag(this.values);
		Map<RegistryKey<VoronoiSettings>, VoronoiImplCompileContext> voronoiContextMap = memory.getTyped(VORONOI_CONTEXT_MAP);
		for (RegistryEntry<VoronoiSettings> entry : voronoiTag) {
			VoronoiImplCompileContext implContext = Objects.requireNonNull(voronoiContextMap.get(UnregisteredObjectException.getKey(entry)));
			for (RegistryEntry<ColumnEntry> enable : entry.value().enables()) {
				ColumnEntryMemory enabledMemory = Objects.requireNonNull(
					context.registry.memories.get(
						UnregisteredObjectException.getID(entry)
					)
				);
				enable.value().setupEnvironment(enabledMemory, implContext);
			}
		}
	}

	@Override
	public void populateCompute(ColumnEntryMemory memory, DataCompileContext context, MethodCompileContext computeMethod) throws ScriptParsingException {
		ConstantValue diagram = context.mainClass.newConstant(this.value, type(VoronoiDiagram2D.class));
		FieldCompileContext valueField = memory.getTyped(ColumnEntryMemory.FIELD);
		FieldInfo cellField = FieldInfo.getField(VoronoiDataBase.class, "cell");
		memory.getTyped(ColumnEntryMemory.COMPUTER).scopes.withScope((MethodCompileContext compute) -> {
			VarInfo self = compute.addThis();
			return_(
				invokeDynamic(
					MethodInfo.getMethod(Voronoi2DColumnEntry.class, "createRandomizer"),
					MethodInfo.getMethod(Voronoi2DColumnEntry.class, "randomize"),
					memory.getTyped(VORONOI_CONTEXT_MAP).values().stream().map((DataCompileContext impl) -> constant(impl.mainClass.info)).toArray(ConstantValue[]::new),
					new InsnTree[] {
						invokeInstance(
							ldc(diagram),
							MethodInfo.findMethod(VoronoiDiagram2D.class, "getNearestCell", VoronoiDiagram2D.Cell.class, int.class, int.class, VoronoiDiagram2D.Cell.class),
							new NullableInstanceGetFieldInsnTree(
								getField(
									load(self),
									valueField.info
								),
								cellField
							)
						)
					}
				)
			)
			.emitBytecode(compute);
		});

		RegistryEntryList<VoronoiSettings> voronoiTag = context.registry.voronois.getOrCreateTag(this.values);
		Map<RegistryKey<VoronoiSettings>, VoronoiImplCompileContext> voronoiContextMap = memory.getTyped(VORONOI_CONTEXT_MAP);
		for (RegistryEntry<VoronoiSettings> entry : voronoiTag) {
			VoronoiImplCompileContext implContext = Objects.requireNonNull(voronoiContextMap.get(UnregisteredObjectException.getKey(entry)));
			for (RegistryEntry<ColumnEntry> enable : entry.value().enables()) {
				ColumnEntryMemory enabledMemory = Objects.requireNonNull(
					context.registry.memories.get(
						UnregisteredObjectException.getID(entry)
					)
				);
				enable.value().emitComputer(enabledMemory, implContext);
			}
		}
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
				"hard_distance_squared"
			-> {
				throw new VerifyException(() -> "Export name " + name + " is built-in, and cannot be overridden.");
			}
		}
	}
}