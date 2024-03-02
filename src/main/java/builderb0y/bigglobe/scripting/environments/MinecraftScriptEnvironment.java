package builderb0y.bigglobe.scripting.environments;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.random.RandomGenerator;
import java.util.stream.Collectors;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.Property;
import net.minecraft.util.Identifier;

import builderb0y.bigglobe.scripting.wrappers.*;
import builderb0y.bigglobe.versions.RegistryVersions;
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

	public static MutableScriptEnvironment create() {
		return (
			new MutableScriptEnvironment()
			.addType("Block",                BlockWrapper           .TYPE)
			.addType("BlockTag",             BlockTagKey            .TYPE)
			.addType("BlockState",           BlockStateWrapper      .TYPE)
			.addType("Biome",                BiomeEntry             .TYPE)
			.addType("BiomeTag",             BiomeTagKey            .TYPE)
			.addType("ConfiguredFeature",    ConfiguredFeatureEntry .TYPE)
			.addType("ConfiguredFeatureTag", ConfiguredFeatureTagKey.TYPE)
			.addType("Tag", TagWrapper.TYPE)
			.addFieldInvokeStatic(BlockWrapper.class, "id")
			.addFieldInvoke(TagWrapper.class, "id")
			.addFieldInvoke(EntryWrapper.class, "id")
			.addFieldInvokes(BiomeEntry.class, "temperature", "downfall")
			.addMethodInvokeStatics(BlockWrapper.class, "getDefaultState", "getRandomState", "isIn")
			.addMethodInvokeSpecific(BlockTagKey.class, "random", Block.class, RandomGenerator.class)
			.addMethodInvokeSpecific(BlockTagKey.class, "random", Block.class, long.class)
			.addMethodInvokeStatics(BlockStateWrapper.class, "isIn", "getBlock", "isAir", "isReplaceable", "hasWater", "hasLava", "hasSoulLava", "hasFluid", "blocksLight", "hasCollision", "hasFullCubeCollision", "hasFullCubeOutline", "rotate", "mirror", "with")
			.addField(BlockStateWrapper.TYPE, null, new FieldHandler.Named("<property getter>", (ExpressionParser parser, InsnTree receiver, String name, GetFieldMode mode) -> {
				return mode.makeInvoker(parser, receiver, BlockStateWrapper.GET_PROPERTY, ldc(name));
			}))
			.addMethodInvokeSpecific(BiomeEntry.class, "isIn", boolean.class, BiomeTagKey.class)
			.addMethodInvokeSpecific(BiomeTagKey.class, "random", BiomeEntry.class, RandomGenerator.class)
			.addMethodInvokeSpecific(BiomeTagKey.class, "random", BiomeEntry.class, long.class)
			.addMethodInvokeSpecific(ConfiguredFeatureEntry.class, "isIn", boolean.class, ConfiguredFeatureTagKey.class)
			.addMethodInvokeSpecific(ConfiguredFeatureTagKey.class, "random", ConfiguredFeatureEntry.class, RandomGenerator.class)
			.addMethodInvokeSpecific(ConfiguredFeatureTagKey.class, "random", ConfiguredFeatureEntry.class, long.class)

			//casting

			.addCastConstant(BlockWrapper           .CONSTANT_FACTORY, true)
			.addCastConstant(BlockStateWrapper      .CONSTANT_FACTORY, true)
			.addCastConstant(BlockTagKey            .CONSTANT_FACTORY, true)
			.addCastConstant(BiomeEntry             .CONSTANT_FACTORY, true)
			.addCastConstant(BiomeTagKey            .CONSTANT_FACTORY, true)
			.addCastConstant(ConfiguredFeatureEntry .CONSTANT_FACTORY, true)
			.addCastConstant(ConfiguredFeatureTagKey.CONSTANT_FACTORY, true)

			.addKeyword("BlockState", blockStateKeyword())
		);
	}

	public static MutableScriptEnvironment createWithRandom(InsnTree loadRandom) {
		return (
			create()
			.addMethod(BlockWrapper.TYPE, "getRandomState", Handlers.builder(BlockWrapper.class, "getRandomState").addReceiverArgument(BlockWrapper.TYPE).addImplicitArgument(loadRandom).buildMethod())
			.addMethod(BlockTagKey.TYPE, "random", tagRandom(loadRandom, BlockTagKey.class, Block.class))
			.addMethod(BiomeTagKey.TYPE, "random", tagRandom(loadRandom, BiomeTagKey.class, BiomeEntry.class))
			.addMethod(ConfiguredFeatureTagKey.TYPE, "random", tagRandom(loadRandom, ConfiguredFeatureTagKey.class, ConfiguredFeatureEntry.class))
		);
	}

	public static MutableScriptEnvironment createWithWorld(InsnTree loadWorld) {
		InsnTree loadRandom = getField(loadWorld, FieldInfo.getField(WorldWrapper.class, "random"));

		return (
			createWithRandom(loadRandom)
			.addVariable("worldSeed", WorldWrapper.INFO.seed(loadWorld))
			.addFunctionInvokes(loadWorld, WorldWrapper.class, "getBlockState", "setBlockState", "setBlockStateReplaceable", "setBlockStateNonReplaceable", "placeBlockState", "fillBlockState", "fillBlockStateReplaceable", "fillBlockStateNonReplaceable", "placeFeature", /* "getBiome", */ "isYLevelValid", "isPositionValid", "getBlockData", "setBlockData", "mergeBlockData")
			.addVariableInvokes(loadWorld, WorldWrapper.class, "minValidYLevel", "maxValidYLevel")
			.addFunctionMultiInvoke(loadWorld, WorldWrapper.class, "summon")
			.addMethod(BlockStateWrapper.TYPE, "canPlaceAt", Handlers.builder(BlockStateWrapper.class, "canPlaceAt").addImplicitArgument(loadWorld).addReceiverArgument(BlockStateWrapper.TYPE).addArguments("III").buildMethod())
			.addMethod(BlockStateWrapper.TYPE, "canStayAt", Handlers.builder(BlockStateWrapper.class, "canStayAt").addImplicitArgument(loadWorld).addReceiverArgument(BlockStateWrapper.TYPE).addArguments("III").buildMethod())
		);
	}

	public static KeywordHandler blockStateKeyword() {
		return (ExpressionParser parser, String name) -> {
			if (parser.input.peekAfterWhitespace() != '(') return null;
			parser.beginCodeBlock();
			InsnTree state = parser.nextScript();
			if (parser.input.hasOperatorAfterWhitespace(",")) {
				//BlockState(?, b: ?)
				ConstantValue constantBlock = state.getConstantValue();
				if (constantBlock.isConstant() && constantBlock.getTypeInfo().equals(TypeInfos.STRING)) {
					//BlockState('a', b: ?)
					String blockName = (String)(constantBlock.asJavaObject());
					Identifier identifier = new Identifier(blockName);
					if (RegistryVersions.block().containsId(identifier)) {
						Block block = RegistryVersions.block().get(identifier);
						Set<String> properties = block.getStateManager().getProperties().stream().map(Property::getName).collect(Collectors.toSet());
						List<ConstantValue> constantProperties = new ArrayList<>(16);
						constantProperties.add(constantBlock);
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
								nonConstantProperties.add(new NonConstantProperty(property, value.cast(parser, TypeInfos.COMPARABLE, CastMode.IMPLICIT_THROW)));
							}
						}
						while (parser.input.hasOperatorAfterWhitespace(","));
						//System.out.println("[MinecraftScriptEnvironment]:\nConstant properties: " + constantProperties + "\nNon-constant properties: " + nonConstantProperties + "\nMissing properties: " + properties);
						if (constantProperties.size() > 1) {
							state = ldc(BOOTSTRAP_CONSTANT_STATE, constantProperties.toArray(ConstantValue.ARRAY_FACTORY));
						}
						else {
							state = BlockStateWrapper.DEFAULT_CONSTANT_FACTORY.create(parser, state, true).tree();
						}
						for (NonConstantProperty nonConstantProperty : nonConstantProperties) {
							state = invokeStatic(BlockStateWrapper.WITH, state, ldc(nonConstantProperty.name), nonConstantProperty.value);
						}
					}
					else {
						throw new ScriptParsingException("Unknown block: " + identifier, parser.input);
					}
				}
				else {
					//BlockState(name, b: c)
					state = invokeStatic(
						BlockWrapper.GET_DEFAULT_STATE,
						BlockWrapper.CONSTANT_FACTORY.create(parser, state, false).tree()
					);
					Set<String> properties = new HashSet<>(8);
					do {
						String property = parser.input.expectIdentifierAfterWhitespace();
						if (!properties.add(property)) {
							throw new ScriptParsingException("Duplicate property: " + property, parser.input);
						}
						parser.input.expectOperatorAfterWhitespace(":");
						InsnTree value = parser.nextScript().cast(parser, TypeInfos.COMPARABLE, CastMode.IMPLICIT_THROW);
						state = invokeStatic(BlockStateWrapper.WITH, state, ldc(property), value);
					}
					while (parser.input.hasOperatorAfterWhitespace(","));
				}
			}
			else {
				//BlockState('a[b=c]')
				state = BlockStateWrapper.CONSTANT_FACTORY.create(parser, state, false).tree();
			}
			parser.endCodeBlock();
			return state;
		};
	}

	public static MethodHandler tagRandom(InsnTree loadRandom, Class<?> owner, Class<?> returnType) {
		return Handlers.builder(owner, "random").returnClass(returnType).addReceiverArgument(owner).addImplicitArgument(loadRandom).buildMethod();
	}

	public static final MethodInfo BOOTSTRAP_CONSTANT_STATE = MethodInfo.getMethod(MinecraftScriptEnvironment.class, "bootstrapConstantState");

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static BlockState bootstrapConstantState(MethodHandles.Lookup caller, String name, Class<?> type, String id, Object... properties) {
		int length = properties.length;
		if ((length & 1) != 0) throw new IllegalArgumentException("properties array length must be even.");
		BlockState state = BlockStateWrapper.getDefaultState(id);
		StateManager<Block, BlockState> manager = state.getBlock().getStateManager();
		for (int index = 0; index < length; index += 2) {
			Property<?> property = manager.getProperty((String)(properties[index]));
			if (property == null) throw new IllegalArgumentException("Cannot set property " + properties[index] + " as it does not exist in " + state.getBlock());
			Comparable<?> value = (Comparable<?>)(properties[index + 1]);
			if (value instanceof String string) {
				value = property.parse(string).orElse(null);
			}
			else if (value instanceof Integer integer && property.getType() == Boolean.class) {
				value = integer.intValue() != 0;
			}
			if (!property.getType().isInstance(value)) {
				throw new IllegalArgumentException("Cannot set property " + property + " to " + properties[index + 1] + " on " + state.getBlock() + ", it is not an allowed value");
			}
			state = state.with((Property)(property), (Comparable)(value));
		}
		return state;
	}
}