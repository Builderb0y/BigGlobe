package builderb0y.bigglobe.columns.scripted.classes;

import java.util.*;

import org.jetbrains.annotations.Nullable;

import net.minecraft.block.BlockState;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.StringIdentifiable;

import builderb0y.autocodec.data.*;
import builderb0y.autocodec.util.AutoCodecUtil;
import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.codecs.BlockStateCoder;
import builderb0y.bigglobe.columns.scripted.ScriptedColumn;
import builderb0y.bigglobe.dynamicRegistries.BigGlobeDynamicRegistries;
import builderb0y.bigglobe.randomLists.IRandomList;
import builderb0y.bigglobe.randomLists.RandomList;
import builderb0y.bigglobe.scripting.wrappers.*;
import builderb0y.bigglobe.scripting.wrappers.entries.BiomeEntry;
import builderb0y.bigglobe.scripting.wrappers.entries.ConfiguredFeatureEntry;
import builderb0y.bigglobe.scripting.wrappers.entries.WoodPaletteEntry;
import builderb0y.bigglobe.scripting.wrappers.tags.*;
import builderb0y.bigglobe.util.DelayedEntry;
import builderb0y.bigglobe.util.DelayedEntryList;
import builderb0y.bigglobe.versions.IdentifierVersions;
import builderb0y.scripting.bytecode.TypeInfo;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.environments.MutableScriptEnvironment;
import builderb0y.scripting.util.TypeInfos;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public class BuiltinTypeSpec extends TypeSpec {

	public final BuiltinJavaType java_type;
	public TypeInfo columnType;

	public BuiltinTypeSpec(BuiltinJavaType java_type) {
		this.java_type = java_type;
	}

	@Override
	public void createTypeInfo(ClassHierarchy hierarchy, LinkedHashSet<RegistryEntry<ElementSpec>> cyclicDetector) throws CustomClassFormatException {
		super.createTypeInfo(hierarchy, cyclicDetector);
		this.columnType = hierarchy.registry.columnCompileContext.columnTypeInfo();
	}

	@Override
	public TypeInfo getTypeInfo() {
		if (this.columnType == null) {
			throw new IllegalStateException("Haven't created scripted column type info yet!");
		}
		if (this.java_type == BuiltinJavaType.COLUMN) {
			return this.columnType;
		}
		return this.java_type.typeInfo;
	}

	@Override
	public boolean isFinal() {
		//prevent custom classes extending builtin classes,
		//even if that class isn't actually final.
		return true;
	}

	@Override
	public @Nullable OverrideTracker getOverrideTracker() {
		return null;
	}

	@Override
	public void setupEnvironment(MutableScriptEnvironment environment, @Nullable InsnTree loadCustomClass) {
		//no-op. these types are already provided by BuiltinScriptEnvironment, JavaUtilScriptEnvironment, and MinecraftScriptEnvironment.
	}

	@Override
	public String name() {
		return this.java_type.lowerCaseName;
	}

	@Override
	public InsnTree parseConstant(ClassHierarchy hierarchy, Data data, InsnTree loadColumn) throws ConstantFormatException {
		if (data.isEmpty()) return ldc(null, this.getTypeInfo());
		return switch (this.java_type) {
			case BYTE -> ldc(asNumber(data).byteValue());
			case SHORT -> ldc(asNumber(data).shortValue());
			case INT -> ldc(asNumber(data).intValue());
			case LONG -> ldc(asNumber(data).longValue());
			case FLOAT -> ldc(asNumber(data).floatValue());
			case DOUBLE -> ldc(asNumber(data).doubleValue());
			case BOOLEAN -> ldc(asBoolean(data).value);

			case BLOCK -> ldc(registry(hierarchy, data, RegistryKeys.BLOCK).value(), BlockWrapper.TYPE);
			case BLOCK_TAG -> ldc(new BlockTag(tag(hierarchy, data, RegistryKeys.BLOCK)), BlockTag.TYPE);
			case BLOCK_STATE -> ldc(blockState(hierarchy, data), BlockStateWrapper.TYPE);
			case BIOME -> ldc(new BiomeEntry(registry(hierarchy, data, RegistryKeys.BIOME)), BiomeEntry.TYPE);
			case BIOME_TAG -> ldc(new BiomeTag(tag(hierarchy, data, RegistryKeys.BIOME)), BiomeTag.TYPE);
			case CONFIGURED_FEATURE -> ldc(new ConfiguredFeatureEntry(registry(hierarchy, data, RegistryKeys.CONFIGURED_FEATURE)), ConfiguredFeatureEntry.TYPE);
			case CONFIGURED_FEATURE_TAG -> ldc(new ConfiguredFeatureTag(tag(hierarchy, data, RegistryKeys.CONFIGURED_FEATURE)), ConfiguredFeatureTag.TYPE);
			case WOOD_PALETTE -> ldc(new WoodPaletteEntry(registry(hierarchy, data, BigGlobeDynamicRegistries.WOOD_PALETTE_REGISTRY_KEY)), WoodPaletteEntry.INFO.type);
			case WOOD_PALETTE_TAG -> ldc(new WoodPaletteTag(tag(hierarchy, data, BigGlobeDynamicRegistries.WOOD_PALETTE_REGISTRY_KEY)), WoodPaletteTag.TYPE);

			default -> throw new ConstantFormatException("Can't create a constant of type " + this.java_type.lowerCaseName);
		};
	}

	public static AbstractNumberData asNumber(Data data) throws ConstantFormatException {
		AbstractNumberData number = data.tryAsNumber();
		if (number != null) return number;
		else throw new ConstantFormatException("Not a number: " + data);
	}

	public static BooleanData asBoolean(Data data) throws ConstantFormatException {
		BooleanData result = data.tryAsBoolean();
		if (result != null) return result;
		else throw new ConstantFormatException("Not a boolean: " + data);
	}

	public static StringData asString(Data data) throws ConstantFormatException {
		StringData result = data.tryAsString();
		if (result != null) return result;
		else throw new ConstantFormatException("Not a string: " + data);
	}

	public static <T> RegistryEntry<T> registry(ClassHierarchy hierarchy, Data data, RegistryKey<Registry<T>> registryKey) throws ConstantFormatException {
		return hierarchy.registry.registries.getRegistry(registryKey).getEntry(RegistryKey.of(registryKey, IdentifierVersions.create(asString(data).value)));
	}

	public static <T> DelayedEntryList<T> tag(ClassHierarchy hierarchy, Data data, RegistryKey<Registry<T>> registryKey) throws ConstantFormatException {
		ListData list = data.tryAsList();
		DelayedEntryList<T> result;
		if (list != null) {
			result = new DelayedEntryList<>(
				hierarchy.registry.registries.getRegistry(registryKey),
				list
				.value
				.stream()
				.map((Data data_) -> {
					try {
						return asString(data_);
					}
					catch (ConstantFormatException exception) {
						throw AutoCodecUtil.rethrow(exception);
					}
				})
				.map((StringData string) -> string.value)
				.map(DelayedEntry::new)
				.toList()
			);
		}
		else {
			result = DelayedEntryList.create(
				hierarchy.registry.registries.getRegistry(registryKey),
				asString(data).value
			);
		}
		result.delay();
		return result;
	}

	public static BlockState blockState(ClassHierarchy hierarchy, Data data) throws ConstantFormatException {
		return (
			BlockStateCoder.decodeStateWithMissingErrors(hierarchy.registry.registries.getRegistry(RegistryKeys.BLOCK), asString(data).value)
			.unwrapEager(BigGlobeMod.LOGGER::warn, IllegalArgumentException::new)
			.state()
		);
	}

	public static enum BuiltinJavaType implements StringIdentifiable {

		//primitives
		BYTE(TypeInfos.BYTE),
		SHORT(TypeInfos.SHORT),
		INT(TypeInfos.INT),
		LONG(TypeInfos.LONG),
		FLOAT(TypeInfos.FLOAT),
		DOUBLE(TypeInfos.DOUBLE),
		BOOLEAN(TypeInfos.BOOLEAN),
		VOID(TypeInfos.VOID),

		//java.util
		ITERATOR(Iterator.class),
		LIST_ITERATOR(ListIterator.class),
		MAP(Map.class),
		MAP_ENTRY(Map.Entry.class),
		SORTED_MAP(SortedMap.class),
		NAVIGABLE_MAP(NavigableMap.class),
		TREE_MAP(TreeMap.class),
		HASH_MAP(HashMap.class),
		LINKED_HASH_MAP(LinkedHashMap.class),
		CONSTANT_MAP(ConstantMap.class),
		ITERABLE(Iterable.class),
		COLLECTION(Collection.class),
		SET(Set.class),
		SORTED_SET(SortedSet.class),
		NAVIGABLE_SET(NavigableSet.class),
		TREE_SET(TreeSet.class),
		HASH_SET(HashSet.class),
		LINKED_HASH_SET(LinkedHashSet.class),
		CONSTANT_SET(ConstantSet.class),
		LIST(List.class),
		LINKED_LIST(LinkedList.class),
		ARRAY_LIST(ArrayList.class),
		CONSTANT_LIST(ArrayWrapper.class),
		QUEUE(Queue.class),
		DEQUE(Deque.class),
		ARRAY_DEQUE(ArrayDeque.class),
		PRIORITY_QUEUE(PriorityQueue.class),
		RANDOM_LIST(IRandomList.class),
		RANDOM_ARRAY_LIST(RandomList.class),

		//minecraft
		BLOCK(BlockWrapper.TYPE),
		BLOCK_TAG(BlockTag.TYPE),
		BLOCK_STATE(BlockStateWrapper.TYPE),
		BIOME(BiomeEntry.TYPE),
		BIOME_TAG(BiomeTag.TYPE),
		CONFIGURED_FEATURE(ConfiguredFeatureEntry.TYPE),
		CONFIGURED_FEATURE_TAG(ConfiguredFeatureTag.TYPE),
		WOOD_PALETTE(WoodPaletteEntry.INFO.type),
		WOOD_PALETTE_TAG(WoodPaletteTag.TYPE),
		TAG(TagWrapper.TYPE),

		//columns
		COLUMN(ScriptedColumn.INFO.type),
		VORONOI(VoronoiSampler.INFO.type),
		;

		public static final BuiltinJavaType[] VALUES = values();

		public final String lowerCaseName = this.name().toLowerCase(Locale.ROOT);
		public final TypeInfo typeInfo;

		BuiltinJavaType(TypeInfo typeInfo) {
			this.typeInfo = typeInfo;
		}

		BuiltinJavaType(Class<?> clazz) {
			this.typeInfo = TypeInfo.of(clazz);
		}

		@Override
		public String asString() {
			return this.lowerCaseName;
		}
	}
}