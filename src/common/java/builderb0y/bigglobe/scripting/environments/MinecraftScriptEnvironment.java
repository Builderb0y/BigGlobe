package builderb0y.bigglobe.scripting.environments;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.random.RandomGenerator;
import java.util.stream.Collectors;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.Property;
import builderb0y.bigglobe.scripting.wrappers.BlockStateWrapper;
import builderb0y.bigglobe.scripting.wrappers.BlockWrapper;
import builderb0y.bigglobe.scripting.wrappers.WorldWrapper;
import builderb0y.bigglobe.scripting.wrappers.entries.BiomeEntry;
import builderb0y.bigglobe.scripting.wrappers.entries.ConfiguredFeatureEntry;
import builderb0y.bigglobe.scripting.wrappers.entries.EntryWrapper;
import builderb0y.bigglobe.scripting.wrappers.tags.BiomeTag;
import builderb0y.bigglobe.scripting.wrappers.tags.BlockTag;
import builderb0y.bigglobe.scripting.wrappers.tags.ConfiguredFeatureTag;
import builderb0y.bigglobe.scripting.wrappers.tags.TagWrapper;
import builderb0y.bigglobe.versions.IdentifierVersions;
import builderb0y.scripting.bytecode.AbstractConstantFactory;
import builderb0y.scripting.bytecode.FieldInfo;
import builderb0y.scripting.bytecode.MethodInfo;
import builderb0y.scripting.bytecode.tree.ConstantValue;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.bytecode.tree.InsnTree.CastMode;
import builderb0y.scripting.environments.Handlers;
import builderb0y.scripting.environments.MutableScriptEnvironment;
import builderb0y.scripting.environments.MutableScriptEnvironment.FieldHandler;
import builderb0y.scripting.environments.MutableScriptEnvironment.KeywordHandler;
import builderb0y.scripting.environments.MutableScriptEnvironment.MethodHandler;
import builderb0y.scripting.environments.ScriptEnvironment.GetFieldMode;
import builderb0y.scripting.parsing.ExpressionParser;
import builderb0y.scripting.parsing.ScriptParsingException;
import builderb0y.scripting.util.TypeInfos;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public class MinecraftScriptEnvironment {

	public static final MutableScriptEnvironment BASE = (
		new MutableScriptEnvironment()
		.addType("Block", BlockWrapper.TYPE)
		.addType("BlockTag", BlockTag.TYPE)
		.addType("BlockState", BlockStateWrapper.TYPE)
		.addType("Biome", BiomeEntry.TYPE)
		.addType("BiomeTag", BiomeTag.TYPE)
		.addType("ConfiguredFeature", ConfiguredFeatureEntry.TYPE)
		.addType("ConfiguredFeatureTag", ConfiguredFeatureTag.TYPE)
		.addType("Tag", TagWrapper.TYPE)
		.addFieldInvokes(TagWrapper.class, "size", "isEmpty")
		.addFieldInvokeStatic(BlockWrapper.class, "id")
		.addFieldInvoke(EntryWrapper.class, "id")
		.addFieldInvokes(BiomeEntry.class, "temperature", "downfall")
		.addMethodInvokeStatics(BlockWrapper.class, "getDefaultState")
		.addMethodMultiInvokeStatic(BlockWrapper.class, "getRandomState")
		.addMethodInvokeSpecific(BlockTag.class, "random", Block.class, RandomGenerator.class)
		.addMethodInvokeSpecific(BlockTag.class, "random", Block.class, long.class)
		.addMethodInvokeStatics(
			BlockStateWrapper.class,
			"getBlock",
			"isAir",
			"isReplaceable",
			"hasWater",
			"hasLava",
			"hasSoulLava",
			"hasFluid",
			"blocksLight",
			"hasCollision",
			"hasFullCubeCollision",
			"hasFullCubeOutline",
			"rotate",
			"mirror",
			"with"
		)
		.addField(
			BlockStateWrapper.TYPE, null, new FieldHandler.Named(
				"<property getter>",
				(ExpressionParser parser, InsnTree receiver, String name, GetFieldMode mode) -> {
					return mode.makeInvoker(parser, receiver, BlockStateWrapper.GET_PROPERTY, ldc(name));
				}
			)
		)
		.addMethodInvokeSpecific(BiomeTag.class, "random", BiomeEntry.class, RandomGenerator.class)
		.addMethodInvokeSpecific(BiomeTag.class, "random", BiomeEntry.class, long.class)
		.addMethodInvokeSpecific(ConfiguredFeatureTag.class, "random", ConfiguredFeatureEntry.class, RandomGenerator.class)
		.addMethodInvokeSpecific(ConfiguredFeatureTag.class, "random", ConfiguredFeatureEntry.class, long.class)

		//casting

		.addCastConstant(BlockWrapper.CONSTANT_FACTORY, true)
		.addCastConstant(BlockStateWrapper.CONSTANT_FACTORY, true)
		.addCastConstant(BiomeEntry.CONSTANT_FACTORY, true)
		.addCastConstant(ConfiguredFeatureEntry.CONSTANT_FACTORY, true)
		.configure(BlockTag.PARSER)
		.addMethod(BlockStateWrapper.TYPE, "isIn", BlockStateWrapper.TAG_PARSER.makeIsIn())
		.configure(BiomeTag.PARSER)
		.configure(ConfiguredFeatureTag.PARSER)

		.addKeyword("BlockState", blockStateKeyword())
	);

	public static Consumer<MutableScriptEnvironment> create() {
		return (MutableScriptEnvironment environment) -> environment.addAll(BASE);
	}

	public static Consumer<MutableScriptEnvironment> createWithRandom(InsnTree loadRandom) {
		return (MutableScriptEnvironment environment) -> {
			environment
			.configure(create())
			.addMethod(BlockWrapper.TYPE, "getRandomState", Handlers.builder(BlockWrapper.class, "getRandomState").addReceiverArgument(BlockWrapper.TYPE).addImplicitArgument(loadRandom).buildMethod())
			.addMethod(BlockTag.TYPE, "random", tagRandom(loadRandom, BlockTag.class, Block.class))
			.addMethod(BiomeTag.TYPE, "random", tagRandom(loadRandom, BiomeTag.class, BiomeEntry.class))
			.addMethod(ConfiguredFeatureTag.TYPE, "random", tagRandom(loadRandom, ConfiguredFeatureTag.class, ConfiguredFeatureEntry.class))
			;
		};
	}

	public static Consumer<MutableScriptEnvironment> createWithWorld(InsnTree loadWorld) {
		InsnTree loadRandom = getField(loadWorld, FieldInfo.getField(WorldWrapper.class, "random"));

		return (MutableScriptEnvironment environment) -> {
			environment
			.configure(createWithRandom(loadRandom))
			.addVariable("worldSeed", WorldWrapper.INFO.seed(loadWorld))
			.addFunctionInvokes(
				loadWorld,
				WorldWrapper.class,
				"getBlockState",
				"setBlockState",
				"setBlockStateReplaceable",
				"setBlockStateNonReplaceable",
				"updateBlockState",
				"placeBlockState",
				"fillBlockState",
				"fillBlockStateReplaceable",
				"fillBlockStateNonReplaceable",
				"updateBlockStates",
				"placeFeature",
				//"getBiome",
				"isYLevelValid",
				"isPositionValid",
				"getBlockData",
				"setBlockData",
				"mergeBlockData"
			)
			.addFunctionMultiInvokes(
				loadWorld,
				WorldWrapper.class,
				"transformX",
				"transformY",
				"transformZ"
			)
			.addVariableInvokes(loadWorld, WorldWrapper.class, "minValidYLevel", "maxValidYLevel")
			.addFunctionMultiInvoke(loadWorld, WorldWrapper.class, "summon")
			.addMethod(BlockStateWrapper.TYPE, "canPlaceAt", Handlers.builder(BlockStateWrapper.class, "canPlaceAt").addImplicitArgument(loadWorld).addReceiverArgument(BlockStateWrapper.TYPE).addArguments("III").buildMethod())
			.addMethod(BlockStateWrapper.TYPE, "canStayAt", Handlers.builder(BlockStateWrapper.class, "canStayAt").addImplicitArgument(loadWorld).addReceiverArgument(BlockStateWrapper.TYPE).addArguments("III").buildMethod())
			;
		};
	}

	public static KeywordHandler.Named blockStateKeyword() {
		return new KeywordHandler.Named(
			"BlockState(block, property1: value1, property2: value2, ...)", (ExpressionParser parser, String name) -> {
			boolean nullable = parser.input.hasOperatorAfterWhitespace("?");
			if (parser.input.peekAfterWhitespace() != '(') {
				if (nullable) throw new ScriptParsingException("'BlockState?' must be followed by parentheses for nullable cast. If a nullable cast was not intended, remove the question mark.", parser.input);
				return null;
			}
			parser.beginCodeBlock();
			InsnTree state = parser.nextScript();
			if (parser.input.hasOperatorAfterWhitespace(",")) {
				//BlockState(?, b: ?)
				ConstantValue constantBlock = state.getConstantValue();
				if (constantBlock.isConstant() && constantBlock.getTypeInfo().equals(TypeInfos.STRING)) {
					//BlockState('a', b: ?)
					String blockName = (String)(constantBlock.asJavaObject());
					Identifier identifier = IdentifierVersions.create(blockName);
					if (BuiltInRegistries.BLOCK.containsKey(identifier)) {
						Block block = BuiltInRegistries.BLOCK.getValue(identifier);
						Set<String> properties = block.getStateDefinition().getProperties().stream().map(Property::getName).collect(Collectors.toSet());
						List<ConstantValue> constantProperties = new ArrayList<>(16);
						constantProperties.add(constantBlock);
						constantProperties.add(constant(AbstractConstantFactory.flags(parser, nullable)));
						record NonConstantProperty(String name, InsnTree value) {}
						List<NonConstantProperty> nonConstantProperties = new ArrayList<>(8);
						do {
							String property = parser.input.expectIdentifierAfterWhitespace();
							if (!properties.remove(property)) {
								throw new ScriptParsingException("Duplicate or unknown property: " + property, parser.input);
							}
							parser.input.expectOperatorAfterWhitespace(":");
							InsnTree value = parser.nextScript();
							ConstantValue constantValue = value.getConstantValue();
							if (constantValue.isConstantOrDynamic()) {
								//BlockState('a', b: true)
								constantProperties.add(constant(property));
								constantProperties.add(constantValue);
							}
							else {
								//BlockState('a', b: c)
								nonConstantProperties.add(new NonConstantProperty(property, value.cast(parser, TypeInfos.COMPARABLE, CastMode.IMPLICIT_THROW, false)));
							}
						}
						while (parser.input.hasOperatorAfterWhitespace(","));
						//System.out.println("[MinecraftScriptEnvironment]:\nConstant properties: " + constantProperties + "\nNon-constant properties: " + nonConstantProperties + "\nMissing properties: " + properties);
						if (constantProperties.size() > 1) {
							state = ldc(BOOTSTRAP_CONSTANT_STATE, constantProperties.toArray(ConstantValue.ARRAY_FACTORY));
						}
						else {
							state = BlockStateWrapper.DEFAULT_CONSTANT_FACTORY.create(parser, state, true, nullable).tree();
						}
						for (NonConstantProperty nonConstantProperty : nonConstantProperties) {
							state = invokeStatic(nullable ? BlockStateWrapper.WITH_NULLABLE : BlockStateWrapper.WITH, state, ldc(nonConstantProperty.name), nonConstantProperty.value);
						}
					}
					else {
						throw new ScriptParsingException("Unknown block: " + identifier, parser.input);
					}
				}
				else {
					//BlockState(name, b: c)
					state = invokeStatic(
						nullable ? BlockWrapper.GET_DEFAULT_STATE_NULLABLE : BlockWrapper.GET_DEFAULT_STATE,
						BlockWrapper.CONSTANT_FACTORY.create(parser, state, false, nullable).tree()
					);
					Set<String> properties = new HashSet<>(8);
					do {
						String property = parser.input.expectIdentifierAfterWhitespace();
						if (!properties.add(property)) {
							throw new ScriptParsingException("Duplicate property: " + property, parser.input);
						}
						parser.input.expectOperatorAfterWhitespace(":");
						InsnTree value = parser.nextScript().cast(parser, TypeInfos.COMPARABLE, CastMode.IMPLICIT_THROW, false);
						state = invokeStatic(nullable ? BlockStateWrapper.WITH_NULLABLE : BlockStateWrapper.WITH, state, ldc(property), value);
					}
					while (parser.input.hasOperatorAfterWhitespace(","));
				}
			}
			else {
				//BlockState('a[b=c]')
				state = BlockStateWrapper.CONSTANT_FACTORY.create(parser, state, false, nullable).tree();
			}
			parser.endCodeBlock();
			return state;
		}
		);
	}

	public static MethodHandler.Named tagRandom(InsnTree loadRandom, Class<?> owner, Class<?> returnType) {
		return Handlers.builder(owner, "random").returnClass(returnType).addReceiverArgument(owner).addImplicitArgument(loadRandom).buildMethod();
	}

	public static final MethodInfo BOOTSTRAP_CONSTANT_STATE = MethodInfo.getMethod(MinecraftScriptEnvironment.class, "bootstrapConstantState");

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static BlockState bootstrapConstantState(MethodHandles.Lookup caller, String name, Class<?> type, String id, int flags, Object... properties) {
		int length = properties.length;
		if ((length & 1) != 0) throw new IllegalArgumentException("properties array length must be even.");
		BlockState state = BlockStateWrapper.getDefaultState(id, flags);
		StateDefinition<Block, BlockState> manager = state.getBlock().getStateDefinition();
		for (int index = 0; index < length; index += 2) {
			Property<?> property = manager.getProperty((String)(properties[index]));
			if (property == null) throw new IllegalArgumentException("Cannot set property " + properties[index] + " as it does not exist in " + state.getBlock());
			Comparable<?> value = (Comparable<?>)(properties[index + 1]);
			if (value instanceof String string) {
				value = property.getValue(string).orElse(null);
			}
			else if (value instanceof Integer integer && property.getValueClass() == Boolean.class) {
				value = integer.intValue() != 0;
			}
			if (!property.getValueClass().isInstance(value)) {
				throw new IllegalArgumentException("Cannot set property " + property + " to " + properties[index + 1] + " on " + state.getBlock() + ", it is not an allowed value");
			}
			state = state.setValue((Property)(property), (Comparable)(value));
		}
		return state;
	}
}