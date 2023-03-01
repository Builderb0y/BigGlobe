package builderb0y.bigglobe.scripting;

import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Opcodes;

import builderb0y.bigglobe.scripting.Wrappers.*;
import builderb0y.scripting.bytecode.CastingSupport.CastProvider;
import builderb0y.scripting.bytecode.CastingSupport.ConstantCaster;
import builderb0y.scripting.bytecode.CastingSupport.LookupCastProvider;
import builderb0y.scripting.bytecode.InsnTrees;
import builderb0y.scripting.bytecode.MethodInfo;
import builderb0y.scripting.bytecode.TypeInfo;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.bytecode.tree.InsnTree.CastMode;
import builderb0y.scripting.bytecode.tree.instructions.InvokeInsnTree;
import builderb0y.scripting.environments.ScriptEnvironment;
import builderb0y.scripting.parsing.ExpressionParser;
import builderb0y.scripting.parsing.ScriptParsingException;
import builderb0y.scripting.util.TypeInfos;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public class MinecraftScriptEnvironment implements ScriptEnvironment {

	public static final CastProvider CAST_PROVIDER = (
		new LookupCastProvider()

		.append(TypeInfos.STRING, BlockTagKey           .TYPE, true, new ConstantCaster(BlockTagKey           .CONSTANT_FACTORY))
		.append(TypeInfos.STRING, BlockWrapper          .TYPE, true, new ConstantCaster(BlockWrapper          .CONSTANT_FACTORY))
		.append(TypeInfos.STRING, BlockStateWrapper     .TYPE, true, new ConstantCaster(BlockStateWrapper     .CONSTANT_FACTORY))
		.append(TypeInfos.STRING, BiomeEntry            .TYPE, true, new ConstantCaster(BiomeEntry            .CONSTANT_FACTORY))
		.append(TypeInfos.STRING, BiomeTagKey           .TYPE, true, new ConstantCaster(BiomeTagKey           .CONSTANT_FACTORY))
		.append(TypeInfos.STRING, ConfiguredFeatureEntry.TYPE, true, new ConstantCaster(ConfiguredFeatureEntry.CONSTANT_FACTORY))
		.append(TypeInfos.STRING, ConfiguredFeatureTag  .TYPE, true, new ConstantCaster(ConfiguredFeatureTag  .CONSTANT_FACTORY))
	);

	public InsnTree loadWorld;

	public MinecraftScriptEnvironment(InsnTree loadWorld) {
		this.loadWorld = loadWorld;
	}

	@Override
	public @Nullable InsnTree getVariable(ExpressionParser parser, String name) throws ScriptParsingException {
		return switch (name) {
			case "worldSeed" -> invokeVirtual(this.loadWorld, WorldWrapper.GET_SEED);
			default -> null;
		};
	}

	@Override
	public @Nullable InsnTree getFunction(ExpressionParser parser, String name, InsnTree... arguments) throws ScriptParsingException {
		return switch (name) {
			case "block"                -> BlockWrapper          .CONSTANT_FACTORY.create(parser, name, arguments);
			case "blockState"           -> BlockStateWrapper     .CONSTANT_FACTORY.create(parser, name, arguments);
			case "blockTag"             -> BlockTagKey           .CONSTANT_FACTORY.create(parser, name, arguments);
			case "biome"                -> BiomeEntry            .CONSTANT_FACTORY.create(parser, name, arguments);
			case "biomeTag"             -> BiomeTagKey           .CONSTANT_FACTORY.create(parser, name, arguments);
			case "configuredFeature"    -> ConfiguredFeatureEntry.CONSTANT_FACTORY.create(parser, name, arguments);
			case "configuredFeatureTag" -> ConfiguredFeatureTag  .CONSTANT_FACTORY.create(parser, name, arguments);

			case "getBlockState"        -> this.makeWorldFunction(parser, WorldWrapper.GET_BLOCK_STATE,   arguments);
			case "setBlockState"        -> this.makeWorldFunction(parser, WorldWrapper.SET_BLOCK_STATE,   arguments);
			case "placeBlockState"      -> this.makeWorldFunction(parser, WorldWrapper.PLACE_BLOCK_STATE, arguments);
			case "fillBlockState"       -> this.makeWorldFunction(parser, WorldWrapper.FILL_BLOCK_STATE,  arguments);
			case "placeFeature"         -> this.makeWorldFunction(parser, WorldWrapper.PLACE_FEATURE,     arguments);
			case "getBiome"             -> this.makeWorldFunction(parser, WorldWrapper.GET_BIOME,         arguments);
			case
				"isValidYLevel",
				"isYLevelValid"         -> this.makeWorldFunction(parser, WorldWrapper.IS_Y_LEVEL_VALID,  arguments);
			case "getBlockData"         -> this.makeWorldFunction(parser, WorldWrapper.GET_BLOCK_DATA,    arguments);
			case "setBlockData"         -> this.makeWorldFunction(parser, WorldWrapper.SET_BLOCK_DATA,    arguments);
			case "mergeBlockData"       -> this.makeWorldFunction(parser, WorldWrapper.MERGE_BLOCK_DATA,  arguments);

			default                     -> null;
		};
	}

	public InsnTree makeWorldFunction(ExpressionParser parser, MethodInfo method, InsnTree[] arguments) {
		arguments = ScriptEnvironment.castArguments(parser, method, CastMode.IMPLICIT_THROW, arguments);
		return new InvokeInsnTree(Opcodes.INVOKEVIRTUAL, this.loadWorld, method, arguments);
	}

	@Override
	public @Nullable TypeInfo getType(ExpressionParser parser, String name) throws ScriptParsingException {
		return switch (name) {
			case "Block"                -> BlockWrapper.TYPE;
			case "BlockState"           -> BlockStateWrapper.TYPE;
			case "BlockTag"             -> BlockTagKey.TYPE;
			case "Biome"                -> BiomeEntry.TYPE;
			case "BiomeTag"             -> BiomeTagKey.TYPE;
			case "ConfiguredFeature"    -> ConfiguredFeatureEntry.TYPE;
			case "ConfiguredFeatureTag" -> ConfiguredFeatureTag.TYPE;
			default                     -> null;
		};
	}

	@Override
	public @Nullable InsnTree getField(ExpressionParser parser, InsnTree receiver, String name) throws ScriptParsingException {
		TypeInfo type = receiver.getTypeInfo();
		if (type.equals(BlockStateWrapper.TYPE)) {
			return invokeStatic(BlockStateWrapper.GET_PROPERTY, receiver, ldc(name));
		}
		else if (type.equals(BiomeEntry.TYPE)) {
			return switch (name) {
				case "temperature"   -> invokeVirtual(receiver, BiomeEntry.GET_TEMPERATURE);
				case "downfall"      -> invokeVirtual(receiver, BiomeEntry.GET_DOWNFALL);
				case "precipitation" -> invokeVirtual(receiver, BiomeEntry.GET_PRECIPITATION);
				default              -> null;
			};
		}
		return null;
	}

	@Override
	public @Nullable InsnTree getMethod(ExpressionParser parser, InsnTree receiver, String name, InsnTree... arguments) throws ScriptParsingException {
		TypeInfo type = receiver.getTypeInfo();
		if (type.equals(BlockTagKey.TYPE)) {
			return switch (name) {
				case "random" -> {
					yield switch (arguments.length) {
						case 0 -> invokeVirtual(receiver, BlockTagKey.RANDOM, InsnTrees.getField(this.loadWorld, WorldWrapper.PERMUTER));
						case 1 -> invokeVirtual(receiver, BlockTagKey.RANDOM, ScriptEnvironment.castArgument(parser, "random", RandomScriptEnvironment.RANDOM_GENERATOR_TYPE, CastMode.IMPLICIT_THROW, arguments));
						default -> throw new ScriptParsingException("BlockTag.random() requires 0 or 1 arguments", parser.input);
					};
				}
				default -> null;
			};
		}
		else if (type.equals(BlockWrapper.TYPE)) {
			return switch (name) {
				case "getDefaultState" -> {
					ScriptEnvironment.checkArgumentCount(parser, name, 0, arguments);
					yield invokeStatic(BlockWrapper.GET_DEFAULT_STATE, receiver);
				}
				case "isIn" -> {
					InsnTree argument = BlockTagKey.CONSTANT_FACTORY.create(parser, name, arguments);
					yield invokeStatic(BlockWrapper.IS_IN, receiver, argument);
				}
				default -> null;
			};
		}
		else if (type.equals(BlockStateWrapper.TYPE)) {
			return switch (name) {
				case "isIn" -> {
					InsnTree argument = BlockTagKey.CONSTANT_FACTORY.create(parser, name, arguments);
					yield invokeStatic(BlockStateWrapper.IS_IN, receiver, argument);
				}
				case
					"getBlock",
					"isAir",
					"isReplaceable",
					"hasWater",
					"hasLava",
					"blocksLight",
					"hasCollision",
					"hasFullCubeCollision"
				-> {
					ScriptEnvironment.checkArgumentCount(parser, name, 0, arguments);
					yield invokeStatic(MethodInfo.findFirstMethod(BlockStateWrapper.class, name), receiver);
				}
				case "rotate" -> {
					InsnTree argument = ScriptEnvironment.castArgument(parser, name, TypeInfos.INT, CastMode.IMPLICIT_THROW, arguments);
					yield invokeStatic(BlockStateWrapper.ROTATE, receiver, argument);
				}
				case "mirror" -> {
					InsnTree argument = ScriptEnvironment.castArgument(parser, name, TypeInfos.STRING, CastMode.IMPLICIT_THROW, arguments);
					yield invokeStatic(BlockStateWrapper.ROTATE, receiver, argument);
				}
				case "with" -> {
					//note: BlockStateWrapper.WITH contains an extra argument for the state, which we don't want.
					InsnTree[] castArguments = ScriptEnvironment.castArguments(parser, "with", new TypeInfo[]{ TypeInfos.STRING, TypeInfos.COMPARABLE }, CastMode.IMPLICIT_THROW, arguments);
					yield invokeStatic(BlockStateWrapper.WITH, receiver, castArguments[0], castArguments[1]);
				}
				case "canPlaceAt" -> {
					//note: BlockStateWrapper.CAN_PLACE_AT contains extra arguments for the world and state, which we don't want.
					InsnTree[] position = ScriptEnvironment.castArguments(parser, "canPlaceAt", types("III"), CastMode.IMPLICIT_THROW, arguments);
					yield invokeStatic(BlockStateWrapper.CAN_PLACE_AT, this.loadWorld, receiver, position[0], position[1], position[2]);
				}
				default -> {
					yield null;
				}
			};
		}
		else if (type.equals(BiomeEntry.TYPE)) {
			return switch (name) {
				case "isIn" -> {
					InsnTree argument = BiomeTagKey.CONSTANT_FACTORY.create(parser, name, arguments);
					yield invokeVirtual(receiver, BiomeEntry.IS_IN, argument);
				}
				default -> null;
			};
		}
		else if (type.equals(BiomeTagKey.TYPE)) {
			return switch (name) {
				case "random" -> {
					yield switch (arguments.length) {
						case 0 -> invokeVirtual(receiver, BiomeTagKey.RANDOM, InsnTrees.getField(this.loadWorld, WorldWrapper.PERMUTER));
						case 1 -> invokeVirtual(receiver, BiomeTagKey.RANDOM, ScriptEnvironment.castArgument(parser, "random", RandomScriptEnvironment.RANDOM_GENERATOR_TYPE, CastMode.IMPLICIT_THROW, arguments));
						default -> throw new ScriptParsingException("BiomeTag.random() requires 0 or 1 arguments", parser.input);
					};
				}
				default -> null;
			};
		}
		else if (type.equals(ConfiguredFeatureTag.TYPE)) {
			return switch (name) {
				case "random" -> {
					yield switch (arguments.length) {
						case 0 -> invokeVirtual(receiver, ConfiguredFeatureTag.RANDOM, InsnTrees.getField(this.loadWorld, WorldWrapper.PERMUTER));
						case 1 -> invokeVirtual(receiver, ConfiguredFeatureTag.RANDOM, ScriptEnvironment.castArgument(parser, "random", RandomScriptEnvironment.RANDOM_GENERATOR_TYPE, CastMode.IMPLICIT_THROW, arguments));
						default -> throw new ScriptParsingException("ConfiguredFeatureTag.random() requires 0 or 1 arguments", parser.input);
					};
				}
				default -> null;
			};
		}
		else {
			return null;
		}
	}
}