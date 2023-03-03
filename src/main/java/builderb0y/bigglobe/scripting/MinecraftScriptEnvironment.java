package builderb0y.bigglobe.scripting;

import java.util.HashMap;
import java.util.Map;

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
		.append(TypeInfos.STRING, ConfiguredFeatureTagKey.TYPE, true, new ConstantCaster(ConfiguredFeatureTagKey.CONSTANT_FACTORY))
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
			case "configuredFeatureTag" -> ConfiguredFeatureTagKey.CONSTANT_FACTORY.create(parser, name, arguments);

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
			case "BlockTag"             -> BlockTagKey.TYPE;
			case "BlockState"           -> BlockStateWrapper.TYPE;
			case "Biome"                -> BiomeEntry.TYPE;
			case "BiomeTag"             -> BiomeTagKey.TYPE;
			case "ConfiguredFeature"    -> ConfiguredFeatureEntry.TYPE;
			case "ConfiguredFeatureTag" -> ConfiguredFeatureTagKey.TYPE;
			case "StructureStart"       -> StructureStartWrapper.TYPE;
			case "StructurePiece"       -> StructurePieceWrapper.TYPE;
			case "Structure"            -> StructureEntry.TYPE;
			case "StructureTag"         -> StructureTagKey.TYPE;
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
		else if (type.equals(StructureStartWrapper.TYPE)) {
			return switch (name) {
				case "minX", "minY", "minZ", "maxX", "maxY", "maxZ", "structure", "pieces" -> invokeVirtual(receiver, MethodInfo.findFirstMethod(StructureStartWrapper.class, name));
				default -> null;
			};
		}
		else if (type.equals(StructurePieceWrapper.TYPE)) {
			return switch (name) {
				case "minX", "minY", "minZ", "maxX", "maxY", "maxZ" -> invokeVirtual(receiver, MethodInfo.findFirstMethod(StructurePieceWrapper.class, name));
				default -> null;
			};
		}
		return null;
	}

	@Override
	public @Nullable InsnTree getMethod(ExpressionParser parser, InsnTree receiver, String name, InsnTree... arguments) throws ScriptParsingException {
		enum SwitchHelper {
			BLOCK,
			BLOCK_TAG,
			BLOCK_STATE,
			BIOME,
			BIOME_TAG,
			CONFIGURED_FEATURE,
			CONFIGURED_FEATURE_TAG,
			STRUCTURE_START,
			STRUCTURE_PIECE,
			STRUCTURE,
			STRUCTURE_TAG;

			public static final Map<TypeInfo, SwitchHelper> MAP = new HashMap<>(STRUCTURE_TAG.ordinal() + 1);
			static {
				MAP.put(BlockWrapper           .TYPE, BLOCK                 );
				MAP.put(BlockTagKey            .TYPE, BLOCK_TAG             );
				MAP.put(BlockStateWrapper      .TYPE, BLOCK_STATE           );
				MAP.put(BiomeEntry             .TYPE, BIOME                 );
				MAP.put(BiomeTagKey            .TYPE, BIOME_TAG             );
				MAP.put(ConfiguredFeatureEntry .TYPE, CONFIGURED_FEATURE    );
				MAP.put(ConfiguredFeatureTagKey.TYPE, CONFIGURED_FEATURE_TAG);
				MAP.put(StructureStartWrapper  .TYPE, STRUCTURE_START       );
				MAP.put(StructurePieceWrapper  .TYPE, STRUCTURE_PIECE       );
				MAP.put(StructureEntry         .TYPE, STRUCTURE             );
				MAP.put(StructureTagKey        .TYPE, STRUCTURE_TAG         );
			}
		}
		return switch (SwitchHelper.MAP.get(receiver.getTypeInfo())) {
			case BLOCK -> switch (name) {
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
			case BLOCK_TAG -> switch (name) {
				case "random" -> {
					yield switch (arguments.length) {
						case 0 -> invokeVirtual(receiver, BlockTagKey.RANDOM, InsnTrees.getField(this.loadWorld, WorldWrapper.PERMUTER));
						case 1 -> invokeVirtual(receiver, BlockTagKey.RANDOM, ScriptEnvironment.castArgument(parser, "random", RandomScriptEnvironment.RANDOM_GENERATOR_TYPE, CastMode.IMPLICIT_THROW, arguments));
						default -> throw new ScriptParsingException("BlockTag.random() requires 0 or 1 arguments", parser.input);
					};
				}
				default -> null;
			};
			case BLOCK_STATE -> switch (name) {
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
			case BIOME -> switch (name) {
				case "isIn" -> {
					InsnTree argument = BiomeTagKey.CONSTANT_FACTORY.create(parser, name, arguments);
					yield invokeVirtual(receiver, BiomeEntry.IS_IN, argument);
				}
				default -> null;
			};
			case BIOME_TAG -> switch (name) {
				case "random" -> {
					yield switch (arguments.length) {
						case 0 -> invokeVirtual(receiver, BiomeTagKey.RANDOM, InsnTrees.getField(this.loadWorld, WorldWrapper.PERMUTER));
						case 1 -> invokeVirtual(receiver, BiomeTagKey.RANDOM, ScriptEnvironment.castArgument(parser, "random", RandomScriptEnvironment.RANDOM_GENERATOR_TYPE, CastMode.IMPLICIT_THROW, arguments));
						default -> throw new ScriptParsingException("BiomeTag.random() requires 0 or 1 arguments", parser.input);
					};
				}
				default -> null;
			};
			case CONFIGURED_FEATURE -> switch (name) {
				case "isIn" -> {
					InsnTree argument = ConfiguredFeatureTagKey.CONSTANT_FACTORY.create(parser, name, arguments);
					yield invokeVirtual(receiver, ConfiguredFeatureEntry.IS_IN, argument);
				}
				default -> null;
			};
			case CONFIGURED_FEATURE_TAG -> switch (name) {
				case "random" -> switch (arguments.length) {
					case 0 -> invokeVirtual(receiver, ConfiguredFeatureTagKey.RANDOM, InsnTrees.getField(this.loadWorld, WorldWrapper.PERMUTER));
					case 1 -> invokeVirtual(receiver, ConfiguredFeatureTagKey.RANDOM, ScriptEnvironment.castArgument(parser, "random", RandomScriptEnvironment.RANDOM_GENERATOR_TYPE, CastMode.IMPLICIT_THROW, arguments));
					default -> throw new ScriptParsingException("ConfiguredFeatureTag.random() requires 0 or 1 arguments", parser.input);
				};
				default -> null;
			};
			case STRUCTURE_START -> null;
			case STRUCTURE_PIECE -> null;
			case STRUCTURE -> switch (name) {
				case "isIn" -> {
					InsnTree argument = StructureTagKey.CONSTANT_FACTORY.create(parser, name, arguments);
					yield invokeVirtual(receiver, StructureEntry.IS_IN, argument);
				}
				default -> null;
			};
			case STRUCTURE_TAG -> switch (name) {
				case "random" -> switch (arguments.length) {
					case 0 -> invokeVirtual(receiver, StructureTagKey.RANDOM, InsnTrees.getField(this.loadWorld, WorldWrapper.PERMUTER));
					case 1 -> invokeVirtual(receiver, StructureTagKey.RANDOM, ScriptEnvironment.castArgument(parser, "random", RandomScriptEnvironment.RANDOM_GENERATOR_TYPE, CastMode.IMPLICIT_THROW, arguments));
					default -> throw new ScriptParsingException("StructureTag.random() requires 0 or 1 arguments", parser.input);
				};
				default -> null;
			};
			default -> null;
		};
	}
}