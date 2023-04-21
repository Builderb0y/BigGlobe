package builderb0y.bigglobe.scripting;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.random.RandomGenerator;
import java.util.stream.Collectors;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.registry.Registries;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.Property;
import net.minecraft.util.Identifier;

import builderb0y.bigglobe.noise.Permuter;
import builderb0y.bigglobe.scripting.wrappers.*;
import builderb0y.scripting.bytecode.InsnTrees;
import builderb0y.scripting.bytecode.MethodInfo;
import builderb0y.scripting.bytecode.tree.ConstantValue;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.bytecode.tree.InsnTree.CastMode;
import builderb0y.scripting.environments.MutableScriptEnvironment;
import builderb0y.scripting.environments.ScriptEnvironment;
import builderb0y.scripting.parsing.ScriptParsingException;
import builderb0y.scripting.util.TypeInfos;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public class MinecraftScriptEnvironment extends MutableScriptEnvironment {

	public MinecraftScriptEnvironment(InsnTree loadWorld) {
		InsnTree loadRandom = InsnTrees.getField(loadWorld, field(ACC_PUBLIC | ACC_FINAL, WorldWrapper.class, "permuter", Permuter.class));

		this
		.addVariableRenamedInvoke(loadWorld, "worldSeed", method(ACC_PUBLIC | ACC_PURE, WorldWrapper.TYPE, "getSeed", TypeInfos.LONG))
		.addFunctionInvokes(loadWorld, WorldWrapper.class, "getBlockState", "setBlockState", "placeBlockState", "fillBlockState", "placeFeature", "getBiome", "isYLevelValid", "isPositionValid", "getBlockData", "setBlockData", "mergeBlockData")
		.addType("Block",                BlockWrapper           .TYPE)
		.addType("BlockTag",             BlockTagKey            .TYPE)
		.addType("BlockState",           BlockStateWrapper      .TYPE)
		.addType("Biome",                BiomeEntry             .TYPE)
		.addType("BiomeTag",             BiomeTagKey            .TYPE)
		.addType("ConfiguredFeature",    ConfiguredFeatureEntry .TYPE)
		.addType("ConfiguredFeatureTag", ConfiguredFeatureTagKey.TYPE)
		.addFieldInvokes(BiomeEntry.class, "temperature", "downfall")
		.addMethodInvokeStatics(BlockWrapper.class, "getDefaultState", "isIn")
		.addMethodInvokeSpecific(BlockTagKey.class, "random", Block.class, RandomGenerator.class)
		.addMethod(BlockTagKey.TYPE, "random", randomFromWorld(loadRandom, BlockTagKey.class, Block.class))
		.addMethodInvokeStatics(BlockStateWrapper.class, "isIn", "getBlock", "isAir", "isReplaceable", "hasWater", "hasLava", "blocksLight", "hasCollision", "hasFullCubeCollision", "hasFullCubeOutline", "rotate", "mirror", "with")
		.addMethod(BlockStateWrapper.TYPE, "canPlaceAt", (parser, receiver, name, arguments) -> {
			InsnTree[] position = ScriptEnvironment.castArguments(parser, "canPlaceAt", types("III"), CastMode.IMPLICIT_NULL, arguments);
			return position == null ? null : new CastResult(invokeStatic(MethodInfo.getMethod(BlockStateWrapper.class, "canPlaceAt"), loadWorld, receiver, position[0], position[1], position[2]), position != arguments);
		})
		.addMethodInvoke(BiomeEntry.class, "isIn")
		.addMethodInvokeSpecific(BiomeTagKey.class, "random", BiomeEntry.class, RandomGenerator.class)
		.addMethod(BiomeTagKey.TYPE, "random", randomFromWorld(loadRandom, BiomeTagKey.class, BiomeEntry.class))
		.addMethodInvoke(ConfiguredFeatureEntry.class, "isIn")
		.addMethodInvokeSpecific(ConfiguredFeatureTagKey.class, "random", ConfiguredFeatureEntry.class, RandomGenerator.class)
		.addMethod(ConfiguredFeatureTagKey.TYPE, "random", randomFromWorld(loadRandom, ConfiguredFeatureTagKey.class, ConfiguredFeatureEntry.class))

		//casting

		.addCastConstant(BlockWrapper           .CONSTANT_FACTORY, "Block",                true)
		.addCastConstant(BlockStateWrapper      .CONSTANT_FACTORY, "BlockState",           true)
		.addCastConstant(BlockTagKey            .CONSTANT_FACTORY, "BlockTag",             true)
		.addCastConstant(BiomeEntry             .CONSTANT_FACTORY, "Biome",                true)
		.addCastConstant(BiomeTagKey            .CONSTANT_FACTORY, "BiomeTag",             true)
		.addCastConstant(ConfiguredFeatureEntry .CONSTANT_FACTORY, "ConfiguredFeature",    true)
		.addCastConstant(ConfiguredFeatureTagKey.CONSTANT_FACTORY, "ConfiguredFeatureTag", true)

		.addKeyword("BlockState", (parser, name) -> {
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
					if (Registries.BLOCK.containsId(identifier)) {
						Block block = Registries.BLOCK.get(identifier);
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
								//BlockState('a': b: true)
								constantProperties.add(constant(property));
								constantProperties.add(constantValue);
							}
							else {
								//BlockState('a': b: c)
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
						BlockWrapper.CONSTANT_FACTORY.create(parser, state, true).tree()
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
		})

		;
	}

	public static MethodHandler randomFromWorld(InsnTree loadRandom, Class<?> owner, Class<?> returnType) {
		MethodInfo randomFunction = MethodInfo.findMethod(owner, "random", returnType, RandomGenerator.class);
		return (parser, receiver, name, arguments) -> {
			if (arguments.length == 0) {
				return new CastResult(invokeVirtual(receiver, randomFunction, loadRandom), false);
			}
			return null;
		};
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