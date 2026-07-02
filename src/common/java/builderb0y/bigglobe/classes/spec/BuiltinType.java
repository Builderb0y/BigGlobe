package builderb0y.bigglobe.classes.spec;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Consumer;
import java.util.random.RandomGenerator;

import org.jetbrains.annotations.UnknownNullability;

import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import builderb0y.autocodec.annotations.MemberUsage;
import builderb0y.autocodec.annotations.UseCoder;
import builderb0y.autocodec.coders.LookupCoder;
import builderb0y.autocodec.common.Case;
import builderb0y.autocodec.data.*;
import builderb0y.autocodec.reflection.reification.ReifiedType;
import builderb0y.autocodec.util.AutoCodecUtil;
import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.classes.BorderedValue;
import builderb0y.bigglobe.classes.Named;
import builderb0y.bigglobe.classes.VoronoiSampler;
import builderb0y.bigglobe.classes.compile.ClassHierarchy;
import builderb0y.bigglobe.classes.compile.ConstantFormatException;
import builderb0y.bigglobe.codecs.BigGlobeAutoCodec;
import builderb0y.bigglobe.codecs.BlockStateCoder;
import builderb0y.bigglobe.columns.scripted.ExternalEnvironmentParams;
import builderb0y.bigglobe.columns.scripted.ScriptedColumn;
import builderb0y.bigglobe.columns.scripted.ScriptedColumn.ColumnUsage;
import builderb0y.bigglobe.columns.scripted.ScriptedColumn.Hints;
import builderb0y.bigglobe.columns.scripted.ScriptedColumnLookup;
import builderb0y.bigglobe.dynamicRegistries.WoodPalette.WoodPaletteType;
import builderb0y.bigglobe.noise.Permuter;
import builderb0y.bigglobe.randomLists.IRandomList;
import builderb0y.bigglobe.randomLists.IRandomList.WeightedIterator;
import builderb0y.bigglobe.randomLists.IRandomList.WeightedListIterator;
import builderb0y.bigglobe.randomLists.RandomList;
import builderb0y.bigglobe.scripting.environments.MinecraftScriptEnvironment;
import builderb0y.bigglobe.scripting.environments.RandomScriptEnvironment;
import builderb0y.bigglobe.scripting.wrappers.*;
import builderb0y.bigglobe.scripting.wrappers.entries.*;
import builderb0y.bigglobe.scripting.wrappers.tags.*;
import builderb0y.bigglobe.util.DelayedEntry;
import builderb0y.bigglobe.util.DelayedEntryList;
import builderb0y.bigglobe.versions.IdentifierVersions;
import builderb0y.scripting.bytecode.FieldInfo;
import builderb0y.scripting.bytecode.MethodInfo;
import builderb0y.scripting.bytecode.TypeInfo;
import builderb0y.scripting.bytecode.tree.ConstantValue;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.bytecode.tree.InsnTree.CastMode;
import builderb0y.scripting.bytecode.tree.instructions.collections.NormalListMapGetterInsnTree;
import builderb0y.scripting.environments.Handlers;
import builderb0y.scripting.environments.JavaUtilScriptEnvironment;
import builderb0y.scripting.environments.MutableScriptEnvironment;
import builderb0y.scripting.environments.MutableScriptEnvironment.*;
import builderb0y.scripting.environments.ScriptEnvironment;
import builderb0y.scripting.environments.ScriptEnvironment.GetFieldMode;
import builderb0y.scripting.environments.ScriptEnvironment.GetMethodMode;
import builderb0y.scripting.environments.ScriptEnvironment.MemberKeywordMode;
import builderb0y.scripting.parsing.ExpressionParser;
import builderb0y.scripting.parsing.ScriptParsingException;
import builderb0y.scripting.parsing.special.ConstantMapSyntax;
import builderb0y.scripting.parsing.special.NamedValues.NamedValue;
import builderb0y.scripting.parsing.special.PrefixedNamedValues;
import builderb0y.scripting.util.TypeInfos;

import static builderb0y.scripting.bytecode.InsnTrees.*;

@UseCoder(name = "CODER", in = BuiltinType.class, usage = MemberUsage.FIELD_CONTAINS_HANDLER)
public abstract class BuiltinType implements Named {

	@UnknownNullability
	public static final LookupCoder<Identifier, BuiltinType> CODER = (
		Export.EXPORTING || JUnit.TESTING
		? null
		: new LookupCoder<>(
			ReifiedType.from(BuiltinType.class),
			BigGlobeAutoCodec.createNamespacedIdentifierCodec(BigGlobeMod.MODID)
		)
	);
	public static final List<BuiltinType> UNIVERSAL = (
		Export.EXPORTING ? null : new ArrayList<>()
	);
	public static final BuiltinType RANDOM;

	public static Consumer<MutableScriptEnvironment> universal() {
		return (MutableScriptEnvironment environment) -> {
			for (BuiltinType type : UNIVERSAL) {
				type.setupEnvironment(environment);
			}
		};
	}

	public final String exposedName;

	public BuiltinType(String exposedName) {
		this.exposedName = exposedName;
	}

	@Override
	public String name() {
		return this.exposedName;
	}

	public Identifier identifier() {
		return CODER.encode.get(this);
	}

	public void setupEnvironment(MutableScriptEnvironment environment) {
		this.setupEnvironment(environment, null);
	}

	public abstract void setupEnvironment(MutableScriptEnvironment environment, UsageCallback callback);

	public void setupEnvironment(ExpressionParser parser, ExternalEnvironmentParams params, Holder<ElementSpec> referencingType) {
		this.setupEnvironment(parser.environment.mutable(), params.dependencyCallback(referencingType));
	}

	public abstract TypeInfo getTypeInfo(BuiltinTypeSpec spec);

	public InsnTree parseConstant(ClassHierarchy hierarchy, BuiltinTypeSpec spec, Data data) throws ConstantFormatException {
		throw new ConstantFormatException("Can't create a constant of type " + this.name() + " (" + this.identifier() + ")");
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

	public static <T> Holder<T> registry(ClassHierarchy hierarchy, Data data, ResourceKey<Registry<T>> registryKey) throws ConstantFormatException {
		return hierarchy.registry.registries.getRegistry(registryKey).getEntry(ResourceKey.create(registryKey, IdentifierVersions.create(asString(data).value)));
	}

	public static <T> DelayedEntryList<T> tag(ClassHierarchy hierarchy, Data data, ResourceKey<Registry<T>> registryKey) throws ConstantFormatException {
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
			BlockStateCoder
			.decodeStateWithMissingErrors(hierarchy.registry.registries.getRegistry(Registries.BLOCK), asString(data).value)
			.unwrapEager(BigGlobeMod.LOGGER::warn, IllegalArgumentException::new)
			.state()
		);
	}

	static {

		/*
		//for copy-pasting:
		register("", new Typed("", .class) {

			@Override
			public void setupEnvironment(MutableScriptEnvironment environment, UsageCallback callback) {

			}
		});
		*/

		abstract class Typed extends BuiltinType {

			public final TypeInfo type;

			public Typed(String exposedName, TypeInfo type) {
				super(exposedName);
				this.type = type;
			}

			public Typed(String exposedName, Class<?> clazz) {
				this(exposedName, type(clazz));
			}

			@Override
			public void setupEnvironment(MutableScriptEnvironment environment) {
				environment.addType(this.exposedName, this.type);
				super.setupEnvironment(environment);
			}

			@Override
			public void setupEnvironment(ExpressionParser parser, ExternalEnvironmentParams params, Holder<ElementSpec> referencingType) {
				UsageCallback callback = params.dependencyCallback(referencingType);
				MutableScriptEnvironment environment = parser.environment.mutable();
				environment.addType(this.exposedName, callback, this.type);
				this.setupEnvironment(environment, callback);
			}

			@Override
			public TypeInfo getTypeInfo(BuiltinTypeSpec spec) {
				return this.type;
			}
		}

		//primitive

		abstract class Primitive extends BuiltinType {

			public final TypeInfo type;

			public Primitive(String exposedName, TypeInfo type) {
				super(exposedName);
				this.type = type;
			}

			@Override
			public void setupEnvironment(MutableScriptEnvironment environment) {

			}

			@Override
			public void setupEnvironment(MutableScriptEnvironment environment, UsageCallback callback) {
				environment.types.put(this.exposedName, new TypeHandler.Named(this.exposedName, this.type.toString(), callback, (ExpressionParser parser, String name_) -> this.type));
			}

			@Override
			public TypeInfo getTypeInfo(BuiltinTypeSpec spec) {
				return this.type;
			}

			@Override
			public abstract InsnTree parseConstant(ClassHierarchy hierarchy, BuiltinTypeSpec spec, Data data) throws ConstantFormatException;
		}
		registerUniversal("byte", new Primitive("byte", TypeInfos.BYTE) {

			@Override
			public InsnTree parseConstant(ClassHierarchy hierarchy, BuiltinTypeSpec spec, Data data) throws ConstantFormatException {
				return ldc(asNumber(data).byteValue());
			}
		});
		registerUniversal("short", new Primitive("short", TypeInfos.SHORT) {

			@Override
			public InsnTree parseConstant(ClassHierarchy hierarchy, BuiltinTypeSpec spec, Data data) throws ConstantFormatException {
				return ldc(asNumber(data).shortValue());
			}
		});
		registerUniversal("int", new Primitive("int", TypeInfos.INT) {

			@Override
			public InsnTree parseConstant(ClassHierarchy hierarchy, BuiltinTypeSpec spec, Data data) throws ConstantFormatException {
				return ldc(asNumber(data).intValue());
			}
		});
		registerUniversal("long", new Primitive("long", TypeInfos.LONG) {

			@Override
			public InsnTree parseConstant(ClassHierarchy hierarchy, BuiltinTypeSpec spec, Data data) throws ConstantFormatException {
				return ldc(asNumber(data).longValue());
			}
		});
		registerUniversal("float", new Primitive("float", TypeInfos.FLOAT) {

			@Override
			public InsnTree parseConstant(ClassHierarchy hierarchy, BuiltinTypeSpec spec, Data data) throws ConstantFormatException {
				return ldc(asNumber(data).floatValue());
			}
		});
		registerUniversal("double", new Primitive("double", TypeInfos.DOUBLE) {

			@Override
			public InsnTree parseConstant(ClassHierarchy hierarchy, BuiltinTypeSpec spec, Data data) throws ConstantFormatException {
				return ldc(asNumber(data).doubleValue());
			}
		});
		registerUniversal("boolean", new Primitive("boolean", TypeInfos.BOOLEAN) {

			@Override
			public InsnTree parseConstant(ClassHierarchy hierarchy, BuiltinTypeSpec spec, Data data) throws ConstantFormatException {
				return ldc(asBoolean(data).value);
			}
		});
		registerUniversal("void", new Primitive("void", TypeInfos.VOID) {

			@Override
			public InsnTree parseConstant(ClassHierarchy hierarchy, BuiltinTypeSpec spec, Data data) throws ConstantFormatException {
				throw new ConstantFormatException("Can't create a constant of type " + this.name() + " (" + this.identifier() + ")");
			}
		});

		//todo: boxed classes.

		//java.lang

		registerUniversal("object", new BuiltinType("Object") {

			@Override
			public void setupEnvironment(MutableScriptEnvironment environment, UsageCallback callback) {
				addMethods(environment, callback, Object.class, "toString", "equals", "hashCode");
			}

			@Override
			public TypeInfo getTypeInfo(BuiltinTypeSpec spec) {
				return TypeInfos.OBJECT;
			}
		});
		registerUniversal("string", new BuiltinType("String") {

			@Override
			public void setupEnvironment(MutableScriptEnvironment environment, UsageCallback callback) {

			}

			@Override
			public InsnTree parseConstant(ClassHierarchy hierarchy, BuiltinTypeSpec spec, Data data) throws ConstantFormatException {
				return ldc(asString(data).value);
			}

			@Override
			public TypeInfo getTypeInfo(BuiltinTypeSpec spec) {
				return TypeInfos.STRING;
			}
		});

		//java.util

		//iterators

		registerUniversal("iterator", new Typed("Iterator", Iterator.class) {

			@Override
			public void setupEnvironment(MutableScriptEnvironment environment, UsageCallback callback) {
				addMethods(environment, callback, Iterator.class, "hasNext", "next", "remove");
			}
		});
		registerUniversal("list_iterator", new Typed("ListIterator", ListIterator.class) {

			@Override
			public void setupEnvironment(MutableScriptEnvironment environment, UsageCallback callback) {
				addMethods(environment, callback, ListIterator.class, "hasPrevious", "previous", "nextIndex", "previousIndex", "set", "add");
			}
		});

		//iterables/collections

		registerUniversal("iterable", new Typed("Iterable", Iterable.class) {

			@Override
			public void setupEnvironment(MutableScriptEnvironment environment, UsageCallback callback) {
				environment
				.addMethod(method(Iterable.class, "iterator", callback))
				;
			}
		});
		registerUniversal("collection", new Typed("Collection", Collection.class) {

			@Override
			public void setupEnvironment(MutableScriptEnvironment environment, UsageCallback callback) {
				environment
				.addField(Handlers.methodWithReceiver(Collection.class, "size").onUsed(callback).buildField())
				.addField(Handlers.methodWithReceiver(Collection.class, "isEmpty").onUsed(callback).buildField())
				.addMethod(Handlers.methodWithReceiver(Collection.class, "remove").onUsed(callback).exposedName("removeElement").buildMethod());
				addMethods(environment, callback, Collection.class, "size", "isEmpty", "contains", "containsAll", "add", "addAll", "removeAll", "retainAll", "clear");
			}
		});
		registerUniversal("sequenced_collection", new Typed("SequencedCollection", SequencedCollection.class) {

			@Override
			public void setupEnvironment(MutableScriptEnvironment environment, UsageCallback callback) {
				addMethods(environment, callback, SequencedCollection.class, "reversed", "getFirst", "getLast", "addFirst", "addLast", "removeFirst", "removeLast");
			}
		});

		//lists

		registerUniversal("list", new Typed("List", List.class) {

			public static final MethodInfo
				LIST_GET = MethodInfo.getMethod(List.class, "get"),
				LIST_SET = MethodInfo.getMethod(List.class, "set");

			@Override
			public void setupEnvironment(MutableScriptEnvironment environment, UsageCallback callback) {
				environment
				.addMethod(Handlers.methodBuilder(List.class, "add").onUsed(callback).addReceiverArgument(this.type).addArguments(int.class, Object.class).buildMethod())
				.addMethod(Handlers.methodBuilder(Collections.class, "swap").onUsed(callback).addReceiverArgument(this.type).addArguments("II").buildMethod())
				.addMethod(Handlers.methodBuilder(Collections.class, "reverse").onUsed(callback).addReceiverArgument(this.type).buildMethod())
				.addMethod(Handlers.methodBuilder(Collections.class, "shuffle").onUsed(callback).addReceiverArgument(this.type).addRequiredArgument(RandomGenerator.class).buildMethod())
				.addMethod(Handlers.methodBuilder(Collections.class, "shuffle").onUsed(callback).addReceiverArgument(this.type).addImportedArgument(RandomGenerator.class).buildMethod())
				.addMethod(Handlers.methodBuilder(JavaUtilScriptEnvironment.class, "shuffle").onUsed(callback).addReceiverArgument(this.type).addRequiredArgument(long.class).buildMethod())
				.addMethod(Handlers.methodBuilder(List.class, "remove").onUsed(callback).addReceiverArgument(this.type).addRequiredArgument(int.class).exposedName("removeIndex").buildMethod())
				.addMethod(Handlers.methodBuilder(List.class, "remove").onUsed(callback).addReceiverArgument(this.type).addRequiredArgument(Object.class).exposedName("removeElement").buildMethod())
				.addMethod(Handlers.methodBuilder(List.class, "listIterator").onUsed(callback).addReceiverArgument(this.type).buildMethod())
				.addMethod(Handlers.methodBuilder(List.class, "listIterator").onUsed(callback).addReceiverArgument(this.type).addRequiredArgument(int.class).buildMethod())
				.addMethod(new MethodHandler.Named(
					type(List.class),
					"",
					"list.(index)",
					callback,
					(ExpressionParser parser, InsnTree receiver, String name, GetMethodMode mode, InsnTree... arguments) -> {
						InsnTree index = ScriptEnvironment.castArgument(parser, "", TypeInfos.INT, CastMode.IMPLICIT_THROW, arguments);
						return new CastResult(
							NormalListMapGetterInsnTree.from(receiver, LIST_GET, index, LIST_SET, "List", mode),
							index != arguments[0]
						);
					}
				))
				;
				addMethods(environment, callback, List.class, "get", "getFirst", "getLast", "indexOf", "lastIndexOf", "subList", "set");
			}
		});
		registerUniversal("linked_list", new Typed("LinkedList", LinkedList.class) {

			@Override
			public void setupEnvironment(MutableScriptEnvironment environment, UsageCallback callback) {
				environment
				.addQualifiedFunction(this.type, Handlers.constructorBuilder(LinkedList.class).onUsed(callback).buildFunction())
				.addQualifiedFunction(this.type, Handlers.constructorBuilder(LinkedList.class).onUsed(callback).addRequiredArgument(Collection.class).buildFunction())
				;
			}
		});
		registerUniversal("array_list", new Typed("ArrayList", ArrayList.class) {

			@Override
			public void setupEnvironment(MutableScriptEnvironment environment, UsageCallback callback) {
				environment
				.addQualifiedFunction(this.type, Handlers.constructorBuilder(ArrayList.class).onUsed(callback).buildFunction())
				.addQualifiedFunction(this.type, Handlers.constructorBuilder(ArrayList.class).onUsed(callback).addRequiredArgument(int.class).buildFunction())
				.addQualifiedFunction(this.type, Handlers.constructorBuilder(ArrayList.class).onUsed(callback).addRequiredArgument(Collection.class).buildFunction())
				;
				addMethods(environment, callback, ArrayList.class, "trimToSize", "ensureCapacity");
			}
		});
		registerUniversal("constant_list", new Typed("ConstantList", ArrayWrapper.class) {

			@Override
			public void setupEnvironment(MutableScriptEnvironment environment, UsageCallback callback) {
				environment
				.addQualifiedFunction(
					type(ArrayWrapper.class),
					new FunctionHandler.Named(
						"new",
						"ConstantList.new(element1, element2, ...)",
						callback,
						(ExpressionParser parser, String name, InsnTree... arguments) -> {
							int argumentCount = arguments.length;
							ConstantValue[] constants = new ConstantValue[argumentCount];
							for (int index = 0; index < argumentCount; index++) {
								if (!(constants[index] = arguments[index].getConstantValue()).isConstantOrDynamic()) {
									throw new ScriptParsingException("Argument " + index + " is not a constant value: " + arguments[index].describe(), parser.input);
								}
							}
							return new CastResult(ldc(JavaUtilScriptEnvironment.CONSTANT_LIST, JavaUtilScriptEnvironment.inflate(constants)), false);
						}
					)
				);
			}
		});
		registerUniversal("random_list", new Typed("RandomList", IRandomList.class) {

			@Override
			public void setupEnvironment(MutableScriptEnvironment environment, UsageCallback callback) {
				environment
				.addMethod(Handlers.methodBuilder(IRandomList.class, "getRandomIndex").onUsed(callback).addReceiverArgument(this.type).addRequiredArgument(RandomGenerator.class).buildMethod())
				.addMethod(Handlers.methodBuilder(IRandomList.class, "getRandomIndex").onUsed(callback).addReceiverArgument(this.type).addRequiredArgument(long.class).buildMethod())
				.addMethod(Handlers.methodBuilder(IRandomList.class, "getRandomElement").onUsed(callback).addReceiverArgument(this.type).addRequiredArgument(RandomGenerator.class).buildMethod())
				.addMethod(Handlers.methodBuilder(IRandomList.class, "getRandomElement").onUsed(callback).addReceiverArgument(this.type).addRequiredArgument(long.class).buildMethod())
				.addMethod(Handlers.methodBuilder(IRandomList.class, "add").onUsed(callback).addReceiverArgument(this.type).addArguments(Object.class, double.class).buildMethod())
				.addMethod(Handlers.methodBuilder(IRandomList.class, "add").onUsed(callback).addReceiverArgument(this.type).addArguments(int.class, Object.class, double.class).buildMethod())
				.addMethod(Handlers.methodBuilder(IRandomList.class, "iterator").onUsed(callback).resultClass(WeightedIterator.class).addReceiverArgument(this.type).buildMethod())
				.addMethod(Handlers.methodBuilder(IRandomList.class, "listIterator").onUsed(callback).resultClass(WeightedListIterator.class).addReceiverArgument(this.type).buildMethod())
				.addMethod(Handlers.methodBuilder(IRandomList.class, "listIterator").onUsed(callback).resultClass(WeightedListIterator.class).addReceiverArgument(this.type).addRequiredArgument(int.class).buildMethod())
				.addMethod(Handlers.methodBuilder(IRandomList.class, "subList").onUsed(callback).resultClass(IRandomList.class).addReceiverArgument(this.type).addArguments("II").buildMethod())
				;
				addMethods(environment, callback, IRandomList.class, "getWeight", "setWeight", "set");
			}
		});
		registerUniversal("random_array_list", new Typed("RandomArrayList", RandomList.class) {

			@Override
			public void setupEnvironment(MutableScriptEnvironment environment, UsageCallback callback) {
				environment
				.addQualifiedFunction(this.type, Handlers.constructorBuilder(RandomList.class).onUsed(callback).buildFunction())
				.addQualifiedFunction(this.type, Handlers.constructorBuilder(RandomList.class).onUsed(callback).addRequiredArgument(int.class).buildFunction())
				.addQualifiedFunction(this.type, Handlers.constructorBuilder(RandomList.class).onUsed(callback).addRequiredArgument(IRandomList.class).buildFunction())
				.addQualifiedFunction(this.type, Handlers.constructorBuilder(RandomList.class).onUsed(callback).addRequiredArgument(RandomList.class).buildFunction())
				;
			}
		});

		//sets

		registerUniversal("set", new Typed("Set", Set.class) {

			@Override
			public void setupEnvironment(MutableScriptEnvironment environment, UsageCallback callback) {
			}
		});
		registerUniversal("sequenced_set", new Typed("SequencedSet", SequencedSet.class) {

			@Override
			public void setupEnvironment(MutableScriptEnvironment environment, UsageCallback callback) {
				environment.addMethod(Handlers.methodBuilder(SequencedSet.class, "reversed").onUsed(callback).resultClass(SequencedSet.class).addReceiverArgument(this.type).buildMethod());
			}
		});
		registerUniversal("sorted_set", new Typed("SortedSet", SortedSet.class) {

			@Override
			public void setupEnvironment(MutableScriptEnvironment environment, UsageCallback callback) {
				addMethods(environment, callback, SortedSet.class, "subSet", "headSet", "tailSet", "first", "last");
			}
		});
		registerUniversal("navigable_set", new Typed("NavigableSet", NavigableSet.class) {

			@Override
			public void setupEnvironment(MutableScriptEnvironment environment, UsageCallback callback) {
				environment
				.addMethod(Handlers.methodBuilder(NavigableSet.class,  "subSet").onUsed(callback).addReceiverArgument(this.type).addArguments(Object.class, boolean.class, Object.class, boolean.class).buildMethod())
				.addMethod(Handlers.methodBuilder(NavigableSet.class, "headSet").onUsed(callback).addReceiverArgument(this.type).addArguments(Object.class, boolean.class).buildMethod())
				.addMethod(Handlers.methodBuilder(NavigableSet.class, "tailSet").onUsed(callback).addReceiverArgument(this.type).addArguments(Object.class, boolean.class).buildMethod())
				.addMethod(Handlers.methodBuilder(NavigableSet.class, "reversed").onUsed(callback).addReceiverArgument(this.type).resultClass(NavigableSet.class).buildMethod())
				;
				addMethods(environment, callback, NavigableSet.class, "lower", "floor", "ceiling", "higher", "descendingSet", "descendingIterator", "pollFirst", "pollLast");
			}
		});
		registerUniversal("tree_set", new Typed("TreeSet", TreeSet.class) {

			@Override
			public void setupEnvironment(MutableScriptEnvironment environment, UsageCallback callback) {
				environment
				.addQualifiedFunction(this.type, Handlers.constructorBuilder(TreeSet.class).onUsed(callback).buildFunction())
				.addQualifiedFunction(this.type, Handlers.constructorBuilder(TreeSet.class).onUsed(callback).addRequiredArgument(SortedSet.class).buildFunction())
				.addQualifiedFunction(this.type, Handlers.constructorBuilder(TreeSet.class).onUsed(callback).addRequiredArgument(Collection.class).buildFunction())
				;
			}
		});
		registerUniversal("hash_set", new Typed("HashSet", HashSet.class) {

			@Override
			public void setupEnvironment(MutableScriptEnvironment environment, UsageCallback callback) {
				environment
				.addQualifiedFunction(this.type, Handlers.constructorBuilder(HashSet.class).onUsed(callback).buildFunction())
				.addQualifiedFunction(this.type, Handlers.constructorBuilder(HashSet.class).onUsed(callback).addRequiredArgument(Collection.class).buildFunction())
				.addQualifiedFunction(this.type, Handlers.constructorBuilder(HashSet.class).onUsed(callback).addRequiredArgument(int.class).buildFunction())
				.addQualifiedFunction(this.type, Handlers.constructorBuilder(HashSet.class).onUsed(callback).addArguments(int.class, float.class).buildFunction())
				;
			}
		});
		registerUniversal("linked_hash_set", new Typed("LinkedHashSet", LinkedHashSet.class) {

			@Override
			public void setupEnvironment(MutableScriptEnvironment environment, UsageCallback callback) {
				environment
				.addQualifiedFunction(this.type, Handlers.constructorBuilder(LinkedHashSet.class).onUsed(callback).buildFunction())
				.addQualifiedFunction(this.type, Handlers.constructorBuilder(LinkedHashSet.class).onUsed(callback).addRequiredArgument(Collection.class).buildFunction())
				.addQualifiedFunction(this.type, Handlers.constructorBuilder(LinkedHashSet.class).onUsed(callback).addArguments("I").buildFunction())
				.addQualifiedFunction(this.type, Handlers.constructorBuilder(LinkedHashSet.class).onUsed(callback).addArguments("IF").buildFunction())
				;
			}
		});
		registerUniversal("constant_set", new Typed("ConstantSet", ConstantSet.class) {

			@Override
			public void setupEnvironment(MutableScriptEnvironment environment, UsageCallback callback) {
				environment
				.addQualifiedFunction(
					type(ConstantSet.class),
					new FunctionHandler.Named(
						"new",
						"ConstantSet.new(element1, element2, ...)",
						callback,
						(ExpressionParser parser, String name, InsnTree... arguments) -> {
							int elementCount = arguments.length;
							ConstantValue[] constants = new ConstantValue[elementCount];
							for (int index = 0; index < elementCount; index++) {
								if (!(constants[index] = arguments[index].getConstantValue()).isConstantOrDynamic()) {
									throw new ScriptParsingException("Argument " + index + " is not a constant value: " + arguments[index].describe(), parser.input);
								}
							}
							return new CastResult(ldc(JavaUtilScriptEnvironment.CONSTANT_SET, JavaUtilScriptEnvironment.inflate(constants)), false);
						}
					)
				);
			}
		});

		//maps

		registerUniversal("map", new Typed("Map", Map.class) {

			public static final MethodInfo
				MAP_GET = MethodInfo.getMethod(Map.class, "get"),
				MAP_PUT = MethodInfo.getMethod(Map.class, "put");

			@Override
			public void setupEnvironment(MutableScriptEnvironment environment, UsageCallback callback) {
				environment
				.addField(Handlers.methodWithReceiver(Map.class, "size").onUsed(callback).buildField())
				.addField(Handlers.methodWithReceiver(Map.class, "isEmpty").onUsed(callback).buildField())
				.addMethod(Handlers.methodBuilder(Map.class, "remove").onUsed(callback).addReceiverArgument(this.type).addRequiredArgument(Object.class).buildMethod())
				.addMethod(Handlers.methodBuilder(Map.class, "remove").onUsed(callback).addReceiverArgument(this.type).addArguments(Object.class, Object.class).buildMethod())
				.addMethod(Handlers.methodBuilder(Map.class, "replace").onUsed(callback).addReceiverArgument(this.type).addArguments(Object.class, Object.class).buildMethod())
				.addMethod(Handlers.methodBuilder(Map.class, "replace").onUsed(callback).addReceiverArgument(this.type).addArguments(Object.class, Object.class, Object.class).buildMethod())
				.addMethod(new MethodHandler.Named(
					type(Map.class),
					"",
					"map.(key)",
					callback,
					(ExpressionParser parser, InsnTree receiver, String name, GetMethodMode mode, InsnTree... arguments) -> {
						InsnTree key = ScriptEnvironment.castArgument(parser, "", TypeInfos.OBJECT, CastMode.IMPLICIT_THROW, arguments);
						return new CastResult(
							NormalListMapGetterInsnTree.from(receiver, MAP_GET, key, MAP_PUT, "Map", mode),
							key != arguments[0]
						);
					}
				))
				;
				addMethods(environment, callback, Map.class, "size", "isEmpty", "containsKey", "containsValue", "get", "getOrDefault", "keySet", "values", "entrySet", "put", "putIfAbsent", "putAll", "clear");
			}
		});
		registerUniversal("map_entry", new Typed("MapEntry", Map.Entry.class) {

			public static final MethodInfo
				MAP_ENTRY_GET_VALUE = MethodInfo.getMethod(Map.Entry.class, "getValue"),
				MAP_ENTRY_SET_VALUE = MethodInfo.getMethod(JavaUtilScriptEnvironment.class, "setEntryValue");

			@Override
			public void setupEnvironment(MutableScriptEnvironment environment, UsageCallback callback) {
				environment
				.addField(Handlers.methodWithReceiver(Map.Entry.class, "getKey").onUsed(callback).exposedName("key").buildField())
				.addField(
					new FieldHandler.Named(
						type(Map.Entry.class),
						"value",
						"mapEntry.value",
						callback,
						(ExpressionParser parser, InsnTree receiver, String name, GetFieldMode mode) -> {
							return mode.makeGetterSetter(parser, receiver, MAP_ENTRY_GET_VALUE, MAP_ENTRY_SET_VALUE);
						}
					)
				)
				;
			}
		});
		registerUniversal("sequenced_map", new Typed("SequencedMap", SequencedMap.class) {

			@Override
			public void setupEnvironment(MutableScriptEnvironment environment, UsageCallback callback) {
				addMethods(environment, callback, SequencedMap.class, "reversed", "firstEntry", "lastEntry", "pollFirstEntry", "pollLastEntry", "putFirst", "putLast", "sequencedKeySet", "sequencedValues", "sequencedEntrySet");
			}
		});
		registerUniversal("sorted_map", new Typed("SortedMap", SortedMap.class) {

			@Override
			public void setupEnvironment(MutableScriptEnvironment environment, UsageCallback callback) {
				addMethods(environment, callback, SortedMap.class, "firstKey", "lastKey", "headMap", "tailMap", "subMap");
			}
		});
		registerUniversal("navigable_map", new Typed("NavigableMap", NavigableMap.class) {

			@Override
			public void setupEnvironment(MutableScriptEnvironment environment, UsageCallback callback) {
				environment
				.addMethod(Handlers.methodBuilder(NavigableMap.class, "subMap").onUsed(callback).addReceiverArgument(this.type).addArguments(Object.class, boolean.class, Object.class, boolean.class).buildMethod())
				.addMethod(Handlers.methodBuilder(NavigableMap.class, "headMap").onUsed(callback).addReceiverArgument(this.type).addArguments(Object.class, boolean.class).buildMethod())
				.addMethod(Handlers.methodBuilder(NavigableMap.class, "tailMap").onUsed(callback).addReceiverArgument(this.type).addArguments(Object.class, boolean.class).buildMethod())
				.addMethod(Handlers.methodBuilder(NavigableMap.class, "reversed").onUsed(callback).addReceiverArgument(this.type).resultClass(NavigableMap.class).buildMethod())
				;
				addMethods(environment, callback, NavigableMap.class, "lowerKey", "higherKey", "floorKey", "ceilingKey", "lowerEntry", "higherEntry", "floorEntry", "ceilingEntry", "firstEntry", "lastEntry", "navigableKeySet", "descendingKeySet", "pollFirstEntry", "pollLastEntry");
			}
		});
		registerUniversal("tree_map", new Typed("TreeMap", TreeMap.class) {

			@Override
			public void setupEnvironment(MutableScriptEnvironment environment, UsageCallback callback) {
				environment
				.addQualifiedFunction(this.type, Handlers.constructorBuilder(TreeMap.class).onUsed(callback).addRequiredArgument(SortedMap.class).buildFunction())
				.addQualifiedFunction(this.type, Handlers.constructorBuilder(TreeMap.class).onUsed(callback).addRequiredArgument(Map.class).buildFunction())
				.addQualifiedFunction(this.type, Handlers.constructorBuilder(TreeMap.class).onUsed(callback).buildFunction())
				;
			}
		});
		registerUniversal("hash_map", new Typed("HashMap", HashMap.class) {

			@Override
			public void setupEnvironment(MutableScriptEnvironment environment, UsageCallback callback) {
				environment
				.addQualifiedFunction(this.type, Handlers.constructorBuilder(HashMap.class).onUsed(callback).addArguments("IF").buildFunction())
				.addQualifiedFunction(this.type, Handlers.constructorBuilder(HashMap.class).onUsed(callback).addRequiredArgument(int.class).buildFunction())
				.addQualifiedFunction(this.type, Handlers.constructorBuilder(HashMap.class).onUsed(callback).buildFunction())
				.addQualifiedFunction(this.type, Handlers.constructorBuilder(HashMap.class).onUsed(callback).addRequiredArgument(Map.class).buildFunction())
				;
			}
		});
		registerUniversal("linked_hash_map", new Typed("LinkedHashMap", LinkedHashMap.class) {

			@Override
			public void setupEnvironment(MutableScriptEnvironment environment, UsageCallback callback) {
				environment
				.addQualifiedFunction(this.type, Handlers.constructorBuilder(LinkedHashMap.class).onUsed(callback).addArguments("IFZ").buildFunction())
				.addQualifiedFunction(this.type, Handlers.constructorBuilder(LinkedHashMap.class).onUsed(callback).addArguments("IF").buildFunction())
				.addQualifiedFunction(this.type, Handlers.constructorBuilder(LinkedHashMap.class).onUsed(callback).addArguments("I").buildFunction())
				.addQualifiedFunction(this.type, Handlers.constructorBuilder(LinkedHashMap.class).onUsed(callback).buildFunction())
				.addQualifiedFunction(this.type, Handlers.constructorBuilder(LinkedHashMap.class).onUsed(callback).addArguments(Map.class).buildFunction())
				;
			}
		});
		registerUniversal("constant_map", new Typed("ConstantMap", ConstantMap.class) {

			@Override
			public void setupEnvironment(MutableScriptEnvironment environment, UsageCallback callback) {
				environment
				.addMemberKeyword(new MemberKeywordHandler.Named(
					TypeInfos.CLASS,
					"new",
					"ConstantMap.new(key1: value1, key2: value2, ...)",
					callback,
					(ExpressionParser parser, InsnTree receiver, String name, MemberKeywordMode mode) -> {
						if (receiver.getConstantValue().isConstant() && receiver.getConstantValue().asJavaObject().equals(type(ConstantMap.class))) {
							return ldc(JavaUtilScriptEnvironment.CONSTANT_MAP, JavaUtilScriptEnvironment.inflate(ConstantMapSyntax.parse(parser).keysAndValues()));
						}
						return null;
					}
				))
				;
			}
		});

		//misc

		registerUniversal("queue", new Typed("Queue", Queue.class) {

			@Override
			public void setupEnvironment(MutableScriptEnvironment environment, UsageCallback callback) {
				addMethods(environment, callback, Queue.class, "element", "peek", "offer", "remove", "poll");
			}
		});
		registerUniversal("deque", new Typed("Deque", Deque.class) {

			@Override
			public void setupEnvironment(MutableScriptEnvironment environment, UsageCallback callback) {
				addMethods(environment, callback, Deque.class,  "peekFirst", "peekLast", "offerFirst", "offerLast", "pollFirst", "pollLast", "removeFirstOccurrence", "removeLastOccurrence", "push", "pop");
			}
		});
		registerUniversal("array_deque", new Typed("ArrayDeque", ArrayDeque.class) {

			@Override
			public void setupEnvironment(MutableScriptEnvironment environment, UsageCallback callback) {
				environment
				.addQualifiedFunction(this.type, Handlers.constructorBuilder(ArrayDeque.class).onUsed(callback).buildFunction())
				.addQualifiedFunction(this.type, Handlers.constructorBuilder(ArrayDeque.class).onUsed(callback).addRequiredArgument(int.class).buildFunction())
				.addQualifiedFunction(this.type, Handlers.constructorBuilder(ArrayDeque.class).onUsed(callback).addRequiredArgument(Collection.class).buildFunction())
				;
			}
		});
		registerUniversal("priority_queue", new Typed("PriorityQueue", PriorityQueue.class) {

			@Override
			public void setupEnvironment(MutableScriptEnvironment environment, UsageCallback callback) {
				environment
				.addQualifiedFunction(this.type, Handlers.constructorBuilder(PriorityQueue.class).onUsed(callback).buildFunction())
				.addQualifiedFunction(this.type, Handlers.constructorBuilder(PriorityQueue.class).onUsed(callback).addRequiredArgument(int.class).buildFunction())
				.addQualifiedFunction(this.type, Handlers.constructorBuilder(PriorityQueue.class).onUsed(callback).addRequiredArgument(Collection.class).buildFunction())
				.addQualifiedFunction(this.type, Handlers.constructorBuilder(PriorityQueue.class).onUsed(callback).addRequiredArgument(PriorityQueue.class).buildFunction())
				.addQualifiedFunction(this.type, Handlers.constructorBuilder(PriorityQueue.class).onUsed(callback).addRequiredArgument(SortedSet.class).buildFunction())
				;
			}
		});
		registerUniversal("random", RANDOM = new Typed("Random", RandomGenerator.class) {

			public static final MethodInfo PERMUTER_CONSTRUCTOR = MethodInfo.findConstructor(Permuter.class, long.class);

			@Override
			public void setupEnvironment(MutableScriptEnvironment environment, UsageCallback callback) {
				environment
				.addQualifiedFunction(
					type(RandomGenerator.class),
					new FunctionHandler.Named(
						"new",
						"Random.new(long seed [, int salt1, int salt2, ...])",
						callback,
						(ExpressionParser parser, String name, InsnTree... arguments) -> {
							if (arguments.length == 0) return null;
							CastResult seed = RandomScriptEnvironment.createSeed(parser, arguments);
							return new CastResult(newInstance(PERMUTER_CONSTRUCTOR, seed.tree()), seed.requiredCasting());
						}
					)
				)
				.addMethod(Handlers.methodBuilder(RandomGenerator.class, "nextInt").onUsed(callback).addReceiverArgument(this.type).buildMethod())
				.addMethod(Handlers.methodBuilder(RandomGenerator.class, "nextInt").onUsed(callback).addReceiverArgument(this.type).addArguments("I").buildMethod())
				.addMethod(Handlers.methodBuilder(RandomGenerator.class, "nextInt").onUsed(callback).addReceiverArgument(this.type).addArguments("II").buildMethod())
				.addMethod(Handlers.methodBuilder(RandomGenerator.class, "nextLong").onUsed(callback).addReceiverArgument(this.type).buildMethod())
				.addMethod(Handlers.methodBuilder(RandomGenerator.class, "nextLong").onUsed(callback).addReceiverArgument(this.type).addArguments("J").buildMethod())
				.addMethod(Handlers.methodBuilder(RandomGenerator.class, "nextLong").onUsed(callback).addReceiverArgument(this.type).addArguments("JJ").buildMethod())
				.addMethod(Handlers.methodBuilder(RandomGenerator.class, "nextFloat").onUsed(callback).addReceiverArgument(this.type).buildMethod())
				.addMethod(Handlers.methodBuilder(RandomGenerator.class, "nextFloat").onUsed(callback).addReceiverArgument(this.type).addArguments("F").buildMethod())
				.addMethod(Handlers.methodBuilder(RandomGenerator.class, "nextFloat").onUsed(callback).addReceiverArgument(this.type).addArguments("FF").buildMethod())
				.addMethod(Handlers.methodBuilder(RandomGenerator.class, "nextDouble").onUsed(callback).addReceiverArgument(this.type).buildMethod())
				.addMethod(Handlers.methodBuilder(RandomGenerator.class, "nextDouble").onUsed(callback).addReceiverArgument(this.type).addArguments("D").buildMethod())
				.addMethod(Handlers.methodBuilder(RandomGenerator.class, "nextDouble").onUsed(callback).addReceiverArgument(this.type).addArguments("DD").buildMethod())
				.addMethod(Handlers.methodBuilder(RandomGenerator.class, "nextGaussian").onUsed(callback).addReceiverArgument(this.type).buildMethod())
				.addMethod(Handlers.methodBuilder(RandomGenerator.class, "nextGaussian").onUsed(callback).addReceiverArgument(this.type).addArguments("DD").buildMethod())
				.addMethod(Handlers.methodBuilder(RandomGenerator.class, "nextExponential").onUsed(callback).addReceiverArgument(this.type).buildMethod())
				.addMethod(Handlers.methodBuilder(RandomGenerator.class, "nextBoolean").onUsed(callback).addReceiverArgument(this.type).buildMethod())
				.addMethod(Handlers.methodBuilder(Permuter.class, "nextChancedBoolean").onUsed(callback).exposedName("nextBoolean").addReceiverArgument(this.type).addArguments("F").buildMethod())
				.addMethod(Handlers.methodBuilder(Permuter.class, "nextChancedBoolean").onUsed(callback).exposedName("nextBoolean").addReceiverArgument(this.type).addArguments("D").buildMethod())
				.addMethod(Handlers.methodBuilder(Permuter.class, "roundRandomlyI").onUsed(callback).exposedName("roundInt").addReceiverArgument(this.type).addArguments("F").buildMethod())
				.addMethod(Handlers.methodBuilder(Permuter.class, "roundRandomlyI").onUsed(callback).exposedName("roundInt").addReceiverArgument(this.type).addArguments("D").buildMethod())
				.addMethod(Handlers.methodBuilder(Permuter.class, "roundRandomlyL").onUsed(callback).exposedName("roundLong").addReceiverArgument(this.type).addArguments("F").buildMethod())
				.addMethod(Handlers.methodBuilder(Permuter.class, "roundRandomlyL").onUsed(callback).exposedName("roundLong").addReceiverArgument(this.type).addArguments("D").buildMethod())
				.addMethod(Handlers.methodBuilder(Permuter.class, "choose").onUsed(callback).addReceiverArgument(this.type).addRequiredArgument(List.class).buildMethod())
				.addMemberKeyword(new MemberKeywordHandler.Named(
					type(RandomGenerator.class),
					"if",
					"random.if ([chance: ] body)",
					callback,
					(ExpressionParser parser, InsnTree receiver, String name, MemberKeywordMode mode) -> {
						return RandomScriptEnvironment.wrapRandomIf(parser, receiver, false, mode);
					}
				))
				.addMemberKeyword(new MemberKeywordHandler.Named(
					type(RandomGenerator.class),
					"unless",
					"random.unless ([chance: ] body)",
					callback,
					(ExpressionParser parser, InsnTree receiver, String name, MemberKeywordMode mode) -> {
						return RandomScriptEnvironment.wrapRandomIf(parser, receiver, true, mode);
					}
				))
				.addMemberKeyword(new MemberKeywordHandler.Named(
					type(RandomGenerator.class),
					"switch",
					"random.switch (case1, case2, ...) or random.switch(weight1: case1, weight2: case2, ...)",
					callback,
					RandomScriptEnvironment.randomSwitch()
				))
				.addMemberKeyword(new MemberKeywordHandler.Named(
					type(RandomGenerator.class),
					"nextBetween",
					"random.nextBetween[min, max)",
					callback,
					RandomScriptEnvironment.nextBetween()
				))
				;
			}
		});

		//minecraft

		register("minecraft_object", new BuiltinType("MCObject") {

			@Override
			public void setupEnvironment(MutableScriptEnvironment environment, UsageCallback callback) {
				//don't add type.
				environment.addField(Handlers.methodWithReceiver(EntryWrapper.class, "id").onUsed(callback).buildField());
			}

			@Override
			public TypeInfo getTypeInfo(BuiltinTypeSpec spec) {
				return EntryWrapper.TYPE;
			}
		});
		register("minecraft_tag", new BuiltinType("MCTag") {

			@Override
			public void setupEnvironment(MutableScriptEnvironment environment, UsageCallback callback) {
				//don't add type.
				environment
				.addField(Handlers.methodWithReceiver(TagWrapper.class, "size").onUsed(callback).buildField())
				.addField(Handlers.methodWithReceiver(TagWrapper.class, "isEmpty").onUsed(callback).buildField());
			}

			@Override
			public TypeInfo getTypeInfo(BuiltinTypeSpec spec) {
				return TagWrapper.TYPE;
			}
		});
		register("block", new Typed("Block", BlockWrapper.TYPE) {

			@Override
			public void setupEnvironment(MutableScriptEnvironment environment, UsageCallback callback) {
				environment
				.addField(Handlers.methodWithReceiver(BlockWrapper.class, "id").onUsed(callback).buildField())
				.addMethod(Handlers.methodWithReceiver(BlockWrapper.class, "getDefaultState").onUsed(callback).buildMethod())
				.addMethod(Handlers.methodBuilder(BlockWrapper.class, "getRandomState").onUsed(callback).addReceiverArgument(this.type).addImportedArgument(RandomGenerator.class).buildMethod())
				.addMethod(Handlers.methodBuilder(BlockWrapper.class, "getRandomState").onUsed(callback).addReceiverArgument(this.type).addRequiredArgument(RandomGenerator.class).buildMethod())
				.addMethod(Handlers.methodBuilder(BlockWrapper.class, "getRandomState").onUsed(callback).addReceiverArgument(this.type).addRequiredArgument(long.class).buildMethod())
				.addCastConstant(BlockWrapper.CONSTANT_FACTORY, true)
				;
			}

			@Override
			public InsnTree parseConstant(ClassHierarchy hierarchy, BuiltinTypeSpec spec, Data data) throws ConstantFormatException {
				return ldc(registry(hierarchy, data, Registries.BLOCK).value(), BlockWrapper.TYPE);
			}
		});
		register("block_tag", new Typed("BlockTag", BlockTag.TYPE) {

			@Override
			public void setupEnvironment(MutableScriptEnvironment environment, UsageCallback callback) {
				environment
				.addMethod(Handlers.methodBuilder(BlockTag.class, "random").onUsed(callback).resultClass(Block.class).addReceiverArgument(this.type).addImportedArgument(RandomGenerator.class).buildMethod())
				.addMethod(Handlers.methodBuilder(BlockTag.class, "random").onUsed(callback).resultClass(Block.class).addReceiverArgument(this.type).addRequiredArgument(RandomGenerator.class).buildMethod())
				.addMethod(Handlers.methodBuilder(BlockTag.class, "random").onUsed(callback).resultClass(Block.class).addReceiverArgument(this.type).addRequiredArgument(long.class).buildMethod())
				;
				BlockTag.PARSER.configure(environment, callback);
			}

			@Override
			public InsnTree parseConstant(ClassHierarchy hierarchy, BuiltinTypeSpec spec, Data data) throws ConstantFormatException {
				return ldc(new BlockTag(tag(hierarchy, data, Registries.BLOCK)), BlockTag.TYPE);
			}
		});
		register("block_state", new Typed("BlockState", BlockStateWrapper.TYPE) {

			@Override
			public void setupEnvironment(MutableScriptEnvironment environment, UsageCallback callback) {
				environment
				.addField(new FieldHandler.Named(
					this.type,
					null,
					"state.propertyName",
					callback,
					(ExpressionParser parser, InsnTree receiver, String name, GetFieldMode mode) -> {
						return mode.makeInvoker(parser, receiver, BlockStateWrapper.GET_PROPERTY, ldc(name));
					}
				))
				.addMethod(Handlers.methodBuilder(BlockStateWrapper.class, "canPlaceAt").onUsed(callback).addReceiverArgument(this.type).addImportedArgument(WorldWrapper.class).addArguments("III").buildMethod())
				.addMethod(Handlers.methodBuilder(BlockStateWrapper.class, "canPlaceAt").onUsed(callback).addReceiverArgument(this.type).addRequiredArgument(WorldWrapper.class).addArguments("III").buildMethod())
				.addMethod(Handlers.methodBuilder(BlockStateWrapper.class, "canStayAt").onUsed(callback).addReceiverArgument(this.type).addImportedArgument(WorldWrapper.class).addArguments("III").buildMethod())
				.addMethod(Handlers.methodBuilder(BlockStateWrapper.class, "canStayAt").onUsed(callback).addReceiverArgument(this.type).addRequiredArgument(WorldWrapper.class).addArguments("III").buildMethod())
				.addCastConstant(BlockStateWrapper.CONSTANT_FACTORY, true)
				.addMethod(BlockStateWrapper.TAG_PARSER.makeIsIn(callback))
				.addKeyword(MinecraftScriptEnvironment.blockStateKeyword(callback))
				;
				addMethods(environment, callback, BlockStateWrapper.class, "getBlock", "isAir", "isReplaceable", "hasWater", "hasLava", "hasSoulLava", "hasFluid", "blocksLight", "hasCollision", "hasFullCubeCollision", "hasFullCubeOutline", "rotate", "mirror", "with");
			}

			@Override
			public InsnTree parseConstant(ClassHierarchy hierarchy, BuiltinTypeSpec spec, Data data) throws ConstantFormatException {
				return ldc(blockState(hierarchy, data), BlockStateWrapper.TYPE);
			}
		});
		register("biome", new Typed("Biome", BiomeEntry.TYPE) {

			@Override
			public void setupEnvironment(MutableScriptEnvironment environment, UsageCallback callback) {
				environment
				.addField(Handlers.methodWithReceiver(BiomeEntry.class, "temperature").onUsed(callback).buildField())
				.addField(Handlers.methodWithReceiver(BiomeEntry.class, "downfall").onUsed(callback).buildField())
				.addCastConstant(BiomeEntry.CONSTANT_FACTORY, true)
				;
			}

			@Override
			public InsnTree parseConstant(ClassHierarchy hierarchy, BuiltinTypeSpec spec, Data data) throws ConstantFormatException {
				return ldc(BiomeEntry.of(asString(data).value, hierarchy.registry.constantFlags()), BiomeEntry.TYPE);
			}
		});
		register("biome_tag", new Typed("BiomeTag", BiomeTag.TYPE) {

			@Override
			public void setupEnvironment(MutableScriptEnvironment environment, UsageCallback callback) {
				environment
				.addMethod(Handlers.methodBuilder(BiomeTag.class, "random").onUsed(callback).resultClass(BiomeEntry.class).addReceiverArgument(this.type).addImportedArgument(RandomGenerator.class).buildMethod())
				.addMethod(Handlers.methodBuilder(BiomeTag.class, "random").onUsed(callback).resultClass(BiomeEntry.class).addReceiverArgument(this.type).addRequiredArgument(RandomGenerator.class).buildMethod())
				.addMethod(Handlers.methodBuilder(BiomeTag.class, "random").onUsed(callback).resultClass(BiomeEntry.class).addReceiverArgument(this.type).addRequiredArgument(long.class).buildMethod())
				;
				BiomeTag.PARSER.configure(environment, callback);
			}

			@Override
			public InsnTree parseConstant(ClassHierarchy hierarchy, BuiltinTypeSpec spec, Data data) throws ConstantFormatException {
				return ldc(BiomeTag.of(hierarchy.registry.constantFlags(), asString(data).value), BiomeTag.TYPE);
			}
		});
		register("configured_feature", new Typed("ConfiguredFeature", ConfiguredFeatureEntry.TYPE) {

			@Override
			public void setupEnvironment(MutableScriptEnvironment environment, UsageCallback callback) {
				environment.addCastConstant(ConfiguredFeatureEntry.CONSTANT_FACTORY, true);
			}

			@Override
			public InsnTree parseConstant(ClassHierarchy hierarchy, BuiltinTypeSpec spec, Data data) throws ConstantFormatException {
				return ldc(ConfiguredFeatureEntry.of(asString(data).value, hierarchy.registry.constantFlags()), ConfiguredFeatureEntry.TYPE);
			}
		});
		register("configured_feature_tag", new Typed("ConfiguredFeatureTag", ConfiguredFeatureTag.TYPE) {

			@Override
			public void setupEnvironment(MutableScriptEnvironment environment, UsageCallback callback) {
				environment
				.addMethod(Handlers.methodBuilder(ConfiguredFeatureTag.class, "random").onUsed(callback).resultClass(ConfiguredFeatureEntry.class).addReceiverArgument(this.type).addImportedArgument(RandomGenerator.class).buildMethod())
				.addMethod(Handlers.methodBuilder(ConfiguredFeatureTag.class, "random").onUsed(callback).resultClass(ConfiguredFeatureEntry.class).addReceiverArgument(this.type).addRequiredArgument(RandomGenerator.class).buildMethod())
				.addMethod(Handlers.methodBuilder(ConfiguredFeatureTag.class, "random").onUsed(callback).resultClass(ConfiguredFeatureEntry.class).addReceiverArgument(this.type).addRequiredArgument(long.class).buildMethod())
				;
				ConfiguredFeatureTag.PARSER.configure(environment, callback);
			}

			@Override
			public InsnTree parseConstant(ClassHierarchy hierarchy, BuiltinTypeSpec spec, Data data) throws ConstantFormatException {
				return ldc(ConfiguredFeatureTag.of(hierarchy.registry.constantFlags(), asString(data).value), ConfiguredFeatureTag.TYPE);
			}
		});
		register("entity_type", new Typed("EntityType", EntityTypeEntry.TYPE) {

			@Override
			public void setupEnvironment(MutableScriptEnvironment environment, UsageCallback callback) {
				environment.addCastConstant(EntityTypeEntry.CONSTANT_FACTORY, true);
			}

			@Override
			public InsnTree parseConstant(ClassHierarchy hierarchy, BuiltinTypeSpec spec, Data data) throws ConstantFormatException {
				return ldc(EntityTypeEntry.of(asString(data).value, hierarchy.registry.constantFlags()), EntityTypeEntry.TYPE);
			}
		});
		register("entity_type_tag", new Typed("EntityTypeTag", EntityTypeTag.TYPE) {

			@Override
			public void setupEnvironment(MutableScriptEnvironment environment, UsageCallback callback) {
				environment
				.addMethod(Handlers.methodBuilder(EntityTypeTag.class, "random").onUsed(callback).resultClass(EntityTypeEntry.class).addReceiverArgument(this.type).addImportedArgument(RandomGenerator.class).buildMethod())
				.addMethod(Handlers.methodBuilder(EntityTypeTag.class, "random").onUsed(callback).resultClass(EntityTypeEntry.class).addReceiverArgument(this.type).addRequiredArgument(RandomGenerator.class).buildMethod())
				.addMethod(Handlers.methodBuilder(EntityTypeTag.class, "random").onUsed(callback).resultClass(EntityTypeEntry.class).addReceiverArgument(this.type).addRequiredArgument(long.class).buildMethod())
				;
				EntityTypeTag.PARSER.configure(environment, callback);
			}

			@Override
			public InsnTree parseConstant(ClassHierarchy hierarchy, BuiltinTypeSpec spec, Data data) throws ConstantFormatException {
				return ldc(EntityTypeTag.of(hierarchy.registry.constantFlags(), asString(data).value), EntityTypeTag.TYPE);
			}
		});
		register("spawn_tweaker", new Typed("SpawnTweaker", SpawnTweakerEntry.TYPE) {

			@Override
			public void setupEnvironment(MutableScriptEnvironment environment, UsageCallback callback) {
				environment.addCastConstant(SpawnTweakerEntry.CONSTANT_FACTORY, true);
			}

			@Override
			public InsnTree parseConstant(ClassHierarchy hierarchy, BuiltinTypeSpec spec, Data data) throws ConstantFormatException {
				return ldc(SpawnTweakerEntry.of(asString(data).value, hierarchy.registry.constantFlags()), SpawnTweakerEntry.TYPE);
			}
		});
		register("spawn_tweaker_tag", new Typed("SpawnTweakerTag", SpawnTweakerTag.TYPE) {

			@Override
			public void setupEnvironment(MutableScriptEnvironment environment, UsageCallback callback) {
				environment
				.addMethod(Handlers.methodBuilder(SpawnTweakerTag.class, "random").onUsed(callback).resultClass(SpawnTweakerEntry.class).addReceiverArgument(this.type).addImportedArgument(RandomGenerator.class).buildMethod())
				.addMethod(Handlers.methodBuilder(SpawnTweakerTag.class, "random").onUsed(callback).resultClass(SpawnTweakerEntry.class).addReceiverArgument(this.type).addRequiredArgument(RandomGenerator.class).buildMethod())
				.addMethod(Handlers.methodBuilder(SpawnTweakerTag.class, "random").onUsed(callback).resultClass(SpawnTweakerEntry.class).addReceiverArgument(this.type).addRequiredArgument(long.class).buildMethod())
				;
				SpawnTweakerTag.PARSER.configure(environment, callback);
			}

			@Override
			public InsnTree parseConstant(ClassHierarchy hierarchy, BuiltinTypeSpec spec, Data data) throws ConstantFormatException {
				return ldc(SpawnTweakerTag.of(hierarchy.registry.constantFlags(), asString(data).value), SpawnTweakerTag.TYPE);
			}
		});
		register("wood_palette", new Typed("WoodPalette", WoodPaletteEntry.INFO.type) {

			@Override
			public void setupEnvironment(MutableScriptEnvironment environment, UsageCallback callback) {
				environment
				.addField(Handlers.methodWithReceiver(WoodPaletteEntry.class, "features").onUsed(callback).buildField())
				.addCastConstant(WoodPaletteEntry.CONSTANT_FACTORY, true);
				for (WoodPaletteType type : WoodPaletteType.VALUES) {
					String baseName = Case.CAMEL_CASE.apply(type.lowerCaseName);
					InsnTree loadType = getStatic(FieldInfo.getField(WoodPaletteType.class, type.name()));
					environment
					.addField(Handlers.methodBuilder(WoodPaletteEntry.INFO.getBlocks).onUsed(callback).exposedName(baseName + "Blocks").addReceiverArgument(this.type).addImplicitArgument(loadType).buildField())
					.addField(Handlers.methodBuilder(WoodPaletteEntry.INFO.getRandomBlock).onUsed(callback).exposedName(baseName + "Block").addReceiverArgument(this.type).addImportedArgument(RandomGenerator.class).addImplicitArgument(loadType).buildField())
					.addMethod(Handlers.methodBuilder(WoodPaletteEntry.INFO.getRandomBlock).onUsed(callback).exposedName(baseName + "Block").addReceiverArgument(this.type).addRequiredArgument(RandomGenerator.class).buildMethod())
					.addMethod(Handlers.methodBuilder(WoodPaletteEntry.INFO.getSeededBlock).onUsed(callback).exposedName(baseName + "Block").addReceiverArgument(this.type).addRequiredArgument(long.class).buildMethod())
					.addMemberKeyword(
						new MemberKeywordHandler.Named(
							type(WoodPaletteEntry.class),
							baseName + "State",
							"palette." + baseName + "State(optional Random random or long seed, property1: value1, property2: value2, ...)",
							callback,
							(ExpressionParser parser, InsnTree receiver, String name, MemberKeywordMode mode) -> {
								return mode.apply(receiver, (InsnTree actualReceiver) -> {
									PrefixedNamedValues namedValues = PrefixedNamedValues.parse(parser, null, TypeInfos.COMPARABLE, null);
									InsnTree loadRandomOrSeed = namedValues.prefix();
									if (loadRandomOrSeed == null) {
										loadRandomOrSeed = parser.environment.getImportedObject(parser, type(RandomGenerator.class));
										if (loadRandomOrSeed == null) {
											throw new ScriptParsingException("Implicit random is not available. Specify your own random or seed.", parser.input);
										}
									}
									InsnTree tree;
									if (loadRandomOrSeed.getTypeInfo().equals(TypeInfos.LONG)) {
										tree = invokeInstance(actualReceiver, WoodPaletteEntry.INFO.getSeededState, loadRandomOrSeed, loadType);
									}
									else if (loadRandomOrSeed.getTypeInfo().extendsOrImplements(type(RandomGenerator.class))) {
										tree = invokeInstance(actualReceiver, WoodPaletteEntry.INFO.getRandomState, loadRandomOrSeed, loadType);
									}
									else {
										throw new ScriptParsingException("Expected long or Random, got " + loadRandomOrSeed.getTypeInfo(), parser.input);
									}
									for (NamedValue value : namedValues.values()) {
										tree = invokeStatic(BlockStateWrapper.WITH, tree, ldc(value.name()), value.value());
									}
									return namedValues.maybeWrap(tree);
								});
							}
						)
					);
				}
			}

			@Override
			public InsnTree parseConstant(ClassHierarchy hierarchy, BuiltinTypeSpec spec, Data data) throws ConstantFormatException {
				return ldc(WoodPaletteEntry.of(asString(data).value, hierarchy.registry.constantFlags()), WoodPaletteEntry.INFO.type);
			}
		});
		register("wood_palette_tag", new Typed("WoodPaletteTag", WoodPaletteTag.TYPE) {

			@Override
			public void setupEnvironment(MutableScriptEnvironment environment, UsageCallback callback) {
				environment
				.addMethod(Handlers.methodBuilder(WoodPaletteTag.class, "random").onUsed(callback).resultClass(WoodPaletteEntry.class).addReceiverArgument(this.type).addImportedArgument(RandomGenerator.class).buildMethod())
				.addMethod(Handlers.methodBuilder(WoodPaletteTag.class, "random").onUsed(callback).resultClass(WoodPaletteEntry.class).addReceiverArgument(this.type).addRequiredArgument(RandomGenerator.class).buildMethod())
				.addMethod(Handlers.methodBuilder(WoodPaletteTag.class, "random").onUsed(callback).resultClass(WoodPaletteEntry.class).addReceiverArgument(this.type).addRequiredArgument(long.class).buildMethod())
				;
				WoodPaletteTag.PARSER.configure(environment, callback);
			}

			@Override
			public InsnTree parseConstant(ClassHierarchy hierarchy, BuiltinTypeSpec spec, Data data) throws ConstantFormatException {
				return ldc(WoodPaletteTag.of(hierarchy.registry.constantFlags(), asString(data).value), WoodPaletteTag.TYPE);
			}
		});

		//columns

		register("column_storage", new BuiltinType("ColumnStorage") {

			@Override
			public void setupEnvironment(MutableScriptEnvironment environment, UsageCallback callback) {
				throw new UnsupportedOperationException("Call the other overload instead.");
			}

			@Override
			public void setupEnvironment(ExpressionParser parser, ExternalEnvironmentParams params, Holder<ElementSpec> referencingType) {
				UsageCallback callback = params.dependencyCallback(referencingType);
				MutableScriptEnvironment environment = parser.environment.mutable();
				environment
				.addType("ColumnStorage", callback, ((BuiltinTypeSpec)(referencingType.value())).columnType)
				.addField(Handlers.methodBuilder(ScriptedColumn.class, "baseSeed").onUsed(callback).exposedName("worldSeed").addReceiverArgument(ScriptedColumn.INFO.type).buildField())
				.addMethod(Handlers.methodBuilder(ScriptedColumn.class, "saltedBaseSeed").onUsed(callback).exposedName("worldSeed").addReceiverArgument(ScriptedColumn.INFO.type).addRequiredArgument(long.class).buildMethod())
				.addField(Handlers.methodBuilder(ScriptedColumn.class, "positionedSeed").onUsed(callback).exposedName("columnSeed").addReceiverArgument(ScriptedColumn.INFO.type).buildField())
				.addMethod(Handlers.methodBuilder(ScriptedColumn.class, "saltedPositionedSeed").onUsed(callback).exposedName("columnSeed").addReceiverArgument(ScriptedColumn.INFO.type).addRequiredArgument(long.class).buildMethod())
				.addField(Handlers.methodBuilder(ScriptedColumn.class, "minY").onUsed(callback).exposedName("minCachedYLevel").addReceiverArgument(ScriptedColumn.INFO.type).buildField())
				.addField(Handlers.methodBuilder(ScriptedColumn.class, "maxY").onUsed(callback).exposedName("maxCachedYLevel").addReceiverArgument(ScriptedColumn.INFO.type).buildField())
				;
				addFields(environment, callback, ScriptedColumn.class, "x", "z");
			}

			@Override
			public TypeInfo getTypeInfo(BuiltinTypeSpec spec) {
				return spec.columnType;
			}
		});
		register("hints", new Typed("Hints", Hints.class) {

			@Override
			public void setupEnvironment(MutableScriptEnvironment environment, UsageCallback callback) {
				environment
				.addField(Handlers.methodWithReceiver(ScriptedColumn.class, "hints").onUsed(callback).buildField())
				.addField(Handlers.methodWithReceiver(ScriptedColumnLookup.HINTS).onUsed(callback).exposedName("hints").buildField())
				.addCastConstant(ColumnUsage.CONSTANT_FACTORY, true)
				;
				addFields(environment, callback, Hints.class, "fill", "carve", "isLod", "distanceBetweenColumns", "lod", "usage", "decorate");
			}
		});
		register("column_lookup", new Typed("ColumnLookup", ScriptedColumnLookup.class) {

			@Override
			public void setupEnvironment(MutableScriptEnvironment environment, UsageCallback callback) {
				throw new UnsupportedOperationException("Call the other overload instead.");
			}

			@Override
			public void setupEnvironment(ExpressionParser parser, ExternalEnvironmentParams params, Holder<ElementSpec> referencingType) {
				UsageCallback callback = params.dependencyCallback(referencingType);
				parser
				.environment
				.mutable()
				.addType(this.exposedName, callback, this.type)
				.addMethod(Handlers.methodBuilder(ScriptedColumnLookup.LOOKUP_COLUMN).onUsed(callback).exposedName("columnAt").addReceiverArgument(this.type).addArguments("II").explicitCast(((BuiltinTypeSpec)(referencingType.value())).columnType).buildMethod());
			}
		});
		register("voronoi", new Typed("VoronoiCell", VoronoiSampler.class) {

			@Override
			public void setupEnvironment(MutableScriptEnvironment environment, UsageCallback callback) {
				throw new UnsupportedOperationException("Call the other overload instead.");
			}

			@Override
			public void setupEnvironment(ExpressionParser parser, ExternalEnvironmentParams params, Holder<ElementSpec> referencingType) {
				UsageCallback callback = params.dependencyCallback(referencingType);
				MutableScriptEnvironment environment = parser.environment.mutable();
				environment
				.addType("VoronoiCell", callback, this.type)
				.addField(Handlers.methodWithReceiver(VoronoiSampler.class, "centerColumn").onUsed(callback).explicitCast(((BuiltinTypeSpec)(referencingType.value())).columnType).buildField());
				addFields(environment, callback, VoronoiSampler.class, "cellX", "cellZ", "centerX", "centerZ", "softDistanceSquared", "dxSoftDistanceSquared", "dzSoftDistanceSquared", "softDistance", "dxSoftDistance", "dzSoftDistance", "hardDistanceSquared", "hardDistance", "euclideanDistanceSquared", "euclideanDistance");
			}
		});
		register("bordered_value", new Typed("BorderedValue", BorderedValue.class) {

			@Override
			public void setupEnvironment(MutableScriptEnvironment environment, UsageCallback callback) {
				environment.addField(Handlers.fieldBuilder(BorderedValue.class, "border").onUsed(callback).addReceiverArgument(this.type).buildField());
			}
		});
		register("read_only_world", new Typed("ReadOnlyWorld", ReadOnlyWorldWrapper.class) {

			@Override
			public void setupEnvironment(MutableScriptEnvironment environment, UsageCallback callback) {
				environment
				.addField(Handlers.methodWithReceiver(ReadOnlyWorldWrapper.class, "seed").onUsed(callback).exposedName("worldSeed").buildField())
				.addField(Handlers.methodWithReceiver(ReadOnlyWorldWrapper.class, "minValidYLevel").onUsed(callback).buildField())
				.addField(Handlers.methodWithReceiver(ReadOnlyWorldWrapper.class, "maxValidYLevel").onUsed(callback).buildField())
				;
				addMethods(environment, callback, ReadOnlyWorldWrapper.class, "getBlockState", "isYLevelValid");
			}
		});
		register("world", new Typed("World", WorldWrapper.class) {

			@Override
			public void setupEnvironment(MutableScriptEnvironment environment, UsageCallback callback) {
				environment
				.addMethod(Handlers.methodBuilder(WorldWrapper.class, "transformX").onUsed(callback).addReceiverArgument(this.type).addArguments("III").buildMethod())
				.addMethod(Handlers.methodBuilder(WorldWrapper.class, "transformY").onUsed(callback).addReceiverArgument(this.type).addArguments("III").buildMethod())
				.addMethod(Handlers.methodBuilder(WorldWrapper.class, "transformZ").onUsed(callback).addReceiverArgument(this.type).addArguments("III").buildMethod())
				.addMethod(Handlers.methodBuilder(WorldWrapper.class, "transformX").onUsed(callback).addReceiverArgument(this.type).addArguments("DDD").buildMethod())
				.addMethod(Handlers.methodBuilder(WorldWrapper.class, "transformY").onUsed(callback).addReceiverArgument(this.type).addArguments("DDD").buildMethod())
				.addMethod(Handlers.methodBuilder(WorldWrapper.class, "transformZ").onUsed(callback).addReceiverArgument(this.type).addArguments("DDD").buildMethod())
				.addMethod(Handlers.methodBuilder(WorldWrapper.class, "summon").onUsed(callback).addReceiverArgument(this.type).addArguments("DDD", String.class).buildMethod())
				.addMethod(Handlers.methodBuilder(WorldWrapper.class, "summon").onUsed(callback).addReceiverArgument(this.type).addArguments("DDD", String.class, CompoundTag.class).buildMethod())
				;
				addMethods(environment, callback, WorldWrapper.class, "setBlockState", "setBlockStateReplaceable", "setBlockStateNonReplaceable", "updateBlockState", "placeBlockState", "fillBlockState", "fillBlockStateReplaceable", "fillBlockStateNonReplaceable", "updateBlockStates", "placeFeature", "isPositionValid", "getBlockData", "setBlockData", "mergeBlockData");
			}
		});

		//todo: NBT data.
	}

	public static void register(String name, BuiltinType type) {
		register(name, type, false);
	}

	public static void registerUniversal(String name, BuiltinType type) {
		register(name, type, true);
	}

	public static void register(String name, BuiltinType type, boolean universal) {
		if (!Export.EXPORTING) {
			if (!JUnit.TESTING) CODER.add(BigGlobeMod.modID(name), type);
			if (universal) UNIVERSAL.add(type);
		}
		else {
			Export.toDelete.remove(name + ".json");
			File file = new File("src/common/resources/data/bigglobe/bigglobe/custom_class".replace('/', File.separatorChar), name + ".json");
			try (FileWriter writer = new FileWriter(file, StandardCharsets.UTF_8)) {
				writer.write("{\n\t\"element_type\": \"class/builtin\",\n\t\"java_type\": \"" + name + "\"\n}");
			}
			catch (IOException exception) {
				exception.printStackTrace();
			}
		}
	}

	public static MethodHandler.Named method(Class<?> in, String name, UsageCallback callback) {
		return Handlers.methodWithReceiver(in, name).onUsed(callback).buildMethod();
	}

	public static void addMethods(MutableScriptEnvironment environment, UsageCallback callback, Class<?> in, String... names) {
		for (String name : names) {
			environment.addMethod(method(in, name, callback));
		}
	}

	public static void addFields(MutableScriptEnvironment environment, UsageCallback callback, Class<?> in, String... names) {
		for (String name : names) {
			environment.addField(Handlers.methodWithReceiver(in, name).onUsed(callback).buildField());
		}
	}

	public static void clinit() {}

	public static class Export {

		public static boolean EXPORTING = false;
		public static Set<String> toDelete;

		public static void main() {
			EXPORTING = true;
			toDelete = new TreeSet<>(Arrays.asList(new File("src/common/resources/data/bigglobe/bigglobe/custom_class".replace('/', File.separatorChar)).list((File dir, String name) -> name.endsWith(".json"))));
			BuiltinType.clinit();
			if (!toDelete.isEmpty()) {
				System.out.println("Leftover files:");
				toDelete.forEach(System.out::println);
			}
		}
	}

	public static class JUnit {

		public static boolean TESTING = false;
	}
}