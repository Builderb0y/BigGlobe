package builderb0y.bigglobe.columns.scripted2.entries;

import java.io.BufferedReader;
import java.io.IOException;
import java.lang.invoke.*;
import java.lang.reflect.Modifier;
import java.util.Comparator;
import java.util.Locale;
import java.util.stream.Stream;

import com.google.gson.JsonParser;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleRBTreeMap;

import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.resource.Resource;
import net.minecraft.util.Identifier;
import net.minecraft.util.StringIdentifiable;

import builderb0y.autocodec.annotations.*;
import builderb0y.autocodec.coders.AutoCoder;
import builderb0y.autocodec.decoders.DecodeContext;
import builderb0y.autocodec.decoders.DecodeException;
import builderb0y.autocodec.util.AutoCodecUtil;
import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.codecs.BigGlobeAutoCodec;
import builderb0y.bigglobe.columns.scripted.ScriptedColumn;
import builderb0y.bigglobe.columns.scripted.classes.ElementSpec;
import builderb0y.bigglobe.columns.scripted.classes.TypeSpec;
import builderb0y.bigglobe.columns.scripted.classes.VoronoiBase;
import builderb0y.bigglobe.columns.scripted2.AccessSchema;
import builderb0y.bigglobe.columns.scripted2.ColumnEntryRegistry;
import builderb0y.bigglobe.columns.scripted2.ColumnValueException;
import builderb0y.bigglobe.columns.scripted2.Valid;
import builderb0y.bigglobe.noise.Permuter;
import builderb0y.bigglobe.randomLists.IRandomList;
import builderb0y.bigglobe.randomLists.RandomList;
import builderb0y.bigglobe.settings.VoronoiDiagram2D;
import builderb0y.bigglobe.util.UnregisteredObjectException;
import builderb0y.bigglobe.versions.IdentifierVersions;
import builderb0y.scripting.bytecode.MethodInfo;
import builderb0y.scripting.bytecode.TypeInfo;
import builderb0y.scripting.bytecode.tree.ConstantValue;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.bytecode.tree.instructions.fields.NullableInstanceGetFieldInsnTree;
import builderb0y.scripting.parsing.ScriptParsingException;
import builderb0y.scripting.util.TypeInfos;

import static builderb0y.scripting.bytecode.InsnTrees.*;

@AddPseudoField("decodeContext")
public class VoronoiColumnEntry extends NonConstantColumnEntry {

	public static final MethodHandle RANDOMIZE;
	static {
		try {
			RANDOMIZE = MethodHandles.lookup().findStatic(VoronoiColumnEntry.class, "randomize", MethodType.methodType(VoronoiBase.class, IRandomList.class, long.class, ScriptedColumn.class, VoronoiDiagram2D.Cell.class));
		}
		catch (Exception exception) {
			throw AutoCodecUtil.rethrow(exception);
		}
	}

	public final VoronoiDiagram2D diagram;
	public final RegistryEntry<ElementSpec> base_class;
	public final Identifier classes;
	public final transient Object2DoubleMap<RegistryEntry<ElementSpec>> weightedClasses;

	public VoronoiColumnEntry(
		AccessSchema params,
		@VerifyNullable Valid valid,
		VoronoiDiagram2D diagram,
		RegistryEntry<ElementSpec> base_class,
		Identifier classes,
		DecodeContext<?> decodeContext
	)
	throws DecodeException {
		super(params, valid, true);
		this.diagram = diagram;
		this.base_class = base_class;
		this.classes = classes;
		this.weightedClasses = VoronoiOptions.load(decodeContext.ops, classes);
	}

	//used by pseudo-field.
	public DecodeContext<?> decodeContext() {
		return null;
	}

	@Override
	public void verify(ColumnEntryRegistry registry) throws ColumnValueException {
		super.verify(registry);
		if (this.params.is_3d()) {
			throw new ColumnValueException("3D voronoi column values are not yet supported.");
		}
		TypeInfo baseTypeInfo = ElementSpec.asType(this.base_class).getTypeInfo();
		if (!baseTypeInfo.extendsOrImplements(VoronoiBase.INFO.type)) {
			throw new ColumnValueException(UnregisteredObjectException.getID(registry.entryOf(this)) + " uses non-voronoi base_class " + UnregisteredObjectException.getID(this.base_class));
		}
		for (RegistryEntry<ElementSpec> entry : this.weightedClasses.keySet()) {
			TypeSpec type = ElementSpec.asType(entry);
			if (!type.getTypeInfo().extendsOrImplements(baseTypeInfo)) {
				throw new ColumnValueException(UnregisteredObjectException.getID(registry.entryOf(this)) + " includes " + UnregisteredObjectException.getID(entry) + " in its classes tag, but that class does not extend the base_class " + UnregisteredObjectException.getID(this.base_class));
			}
			if (type.isAbstract()) {
				throw new ColumnValueException(UnregisteredObjectException.getID(registry.entryOf(this)) + " includes " + UnregisteredObjectException.getID(entry) + " in its classes tag, but that class is abstract.");
			}
		}
	}

	@Override
	public InsnTree makeComputer(ColumnEntryRegistry registry, NonConstantColumnEntryContext context) throws ScriptParsingException {
		InsnTree loadColumn = registry.columnCompileContext.loadColumn();
		return invokeDynamic(
			MethodInfo.inCaller("createRandomizer"),
			new MethodInfo(
				ACC_PUBLIC | ACC_STATIC,
				TypeInfos.OBJECT, //ignored.
				"randomize",
				ElementSpec.asType(this.base_class).getTypeInfo(),
				type(ScriptedColumn.class),
				type(VoronoiDiagram2D.class)
			),
			Stream.concat(
				Stream.of(constant(Permuter.permute(0L, UnregisteredObjectException.getID(registry.entryOf(this))))),
				this.weightedClasses.object2DoubleEntrySet().stream().flatMap(
					(Object2DoubleMap.Entry<RegistryEntry<ElementSpec>> entry) -> Stream.of(
						constant(ElementSpec.asType(entry.getKey()).getTypeInfo()),
						constant(entry.getDoubleValue())
					)
				)
			)
			.toArray(ConstantValue.ARRAY_FACTORY),
			new InsnTree[] {
				loadColumn,
				invokeInstance(
					ldc(this.diagram, type(VoronoiDiagram2D.class)),
					MethodInfo.findMethod(VoronoiDiagram2D.class, "getNearestCell", VoronoiDiagram2D.Cell.class, int.class, int.class, VoronoiDiagram2D.Cell.class),
					invokeInstance(loadColumn, ScriptedColumn.INFO.x),
					invokeInstance(loadColumn, ScriptedColumn.INFO.z),
					new NullableInstanceGetFieldInsnTree(
						getField(loadColumn, context.valueField.info),
						VoronoiBase.INFO.$cell
					)
				)
			}
		);
	}

	/**
	@param methodType (ScriptedColumn, VoronoiDiagram2D.Cell) -> VoronoiBase
	*/
	public static CallSite createRandomizer(MethodHandles.Lookup caller, String name, MethodType methodType, long seed, Object... options) throws Throwable {
		if (!VoronoiBase.class.isAssignableFrom(methodType.returnType())) {
			throw new IllegalArgumentException("Invalid super class: " + methodType.returnType().getSuperclass());
		}
		if ((options.length & 1) != 0) {
			throw new IllegalArgumentException("Options array is odd-length");
		}
		if (options.length == 0) {
			if (Modifier.isAbstract(methodType.returnType().getModifiers())) {
				throw new IllegalArgumentException("No options");
			}
			else {
				return new ConstantCallSite(caller.findConstructor(methodType.returnType(), methodType.changeReturnType(void.class)));
			}
		}
		MethodType constructorType = methodType.changeReturnType(void.class);
		MethodType factoryType = methodType.changeReturnType(VoronoiBase.class);
		RandomList<VoronoiBase.Factory> list = new RandomList<>(options.length >> 1);
		for (int baseIndex = 0; baseIndex < options.length; baseIndex += 2) {
			Class<?> clazz = ((Class<?>)(options[baseIndex])).asSubclass(methodType.returnType());
			if (Modifier.isAbstract(clazz.getModifiers())) {
				throw new IllegalArgumentException(clazz + " is abstract.");
			}
			double weight = (double)(options[baseIndex | 1]);
			VoronoiBase.Factory factory = (VoronoiBase.Factory)(
				LambdaMetafactory.metafactory(
					caller,
					"create",
					MethodType.methodType(VoronoiBase.Factory.class),
					factoryType,
					caller.findConstructor(clazz, constructorType),
					methodType
				)
				.getTarget()
				.invokeExact()
			);
			list.add(factory, weight);
		}
		return new ConstantCallSite(
			MethodHandles
			.insertArguments(RANDOMIZE, 0, list.optimize(), seed)
			.asType(methodType)
		);
	}

	public static VoronoiBase randomize(IRandomList<VoronoiBase.Factory> factories, long baseSeed, ScriptedColumn column, VoronoiDiagram2D.Cell cell) {
		return factories.getRandomElement(cell.center.getSeed(baseSeed)).create(column, cell);
	}

	@Override
	public InsnTree makeBulkComputer(ColumnEntryRegistry registry, NonConstantColumnEntryContext context) throws ScriptParsingException {
		throw new UnsupportedOperationException();
	}

	public static record VoronoiOptions(@DefaultBoolean(false) boolean replace, VoronoiOption @DefaultEmpty [] values) {

		public static final AutoCoder<VoronoiOptions> CODER = BigGlobeAutoCodec.AUTO_CODEC.createCoder(VoronoiOptions.class);

		public static <T> Object2DoubleMap<RegistryEntry<ElementSpec>> load(DynamicOps<T> ops, Identifier tagID) throws DecodeException {
			Object2DoubleMap<RegistryEntry<ElementSpec>> result = new Object2DoubleRBTreeMap<>(
				Comparator.comparing(UnregisteredObjectException::getID)
			);
			for (Resource resource : BigGlobeMod.getResourceManager().getAllResources(IdentifierVersions.create(tagID.getNamespace(), "worldgen/bigglobe_voronoi_options/" + tagID.getPath() + ".json"))) {
				try (BufferedReader reader = resource.getReader()) {
					BigGlobeAutoCodec.AUTO_CODEC.decode(CODER, JsonOps.INSTANCE.convertTo(ops, JsonParser.parseReader(reader)), ops).addTo(result);
				}
				catch (IOException exception) {
					throw new DecodeException(exception);
				}
			}
			return result;
		}

		public void addTo(Object2DoubleMap<RegistryEntry<ElementSpec>> classes) {
			if (this.replace) classes.clear();
			for (VoronoiOption value : this.values) {
				value.addTo(classes);
			}
		}
	}

	public static record VoronoiOption(
		@DefaultString("add") Operation operation,
		@UseName("class") RegistryEntry<ElementSpec> clazz,
		@DefaultDouble(1.0D) double weight
	) {

		public void addTo(Object2DoubleMap<RegistryEntry<ElementSpec>> classes) {
			switch (this.operation) {
				case ADD -> {
					classes.put(this.clazz, this.weight);
				}
				case REMOVE -> {
					classes.removeDouble(this.clazz);
				}
			}
		}

		public enum Operation implements StringIdentifiable {
			ADD,
			REMOVE;

			public final String lowerCaseName = this.name().toLowerCase(Locale.ROOT);

			@Override
			public String asString() {
				return this.lowerCaseName;
			}
		}
	}
}